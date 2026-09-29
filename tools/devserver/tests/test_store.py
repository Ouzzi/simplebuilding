"""Die Ablage: Versionen, Konflikte, Wiederherstellung, gleichzeitiges Schreiben - kein Wert geht verloren."""

import json
import os
import threading
import time
import unittest

import helpers
from sbdev import store as store_mod
from sbdev.store import Store, StoreError


def change(vid, new, mod=0):
    return {"id": vid, "new": new, "mod": mod}


class StoreTests(unittest.TestCase):
    def setUp(self):
        self.root = helpers.temp_dir("bz-store-")
        self.store = Store(self.root)

    def test_empty_store_is_version_zero(self):
        state = self.store.state()
        self.assertEqual(state["version"], 0)
        self.assertEqual(state["entries"], {})
        self.assertFalse((self.root / "balance.json").exists())  # Lesen schreibt nichts

    def test_every_save_is_a_new_immutable_version(self):
        self.store.commit(0, [change("a", 1)], "eins")
        self.store.commit(1, [change("b", 2)], "zwei")
        self.store.commit(2, [change("a", 3)], "drei")
        state = self.store.state()
        self.assertEqual(state["version"], 3)
        self.assertEqual(state["entries"]["a"]["value"], 3)
        self.assertEqual(state["entries"]["a"]["origin"], 0)  # Ursprungswert bleibt
        self.assertEqual([h["version"] for h in self.store.history()], [0, 1, 2, 3])
        self.assertEqual(self.store.version(1)["entries"], {"a": {"value": 1, "mod": 0, "origin": 0}})
        self.assertEqual(self.store.version(2)["changes"][0]["id"], "b")

    def test_stale_base_version_is_a_conflict_and_writes_nothing(self):
        self.store.commit(0, [change("a", 1)], "")
        before = sorted(os.listdir(self.root / "versions"))
        with self.assertRaises(StoreError) as ctx:
            self.store.commit(0, [change("a", 2)], "")
        self.assertEqual(ctx.exception.status, 409)
        self.assertEqual(sorted(os.listdir(self.root / "versions")), before)
        self.assertEqual(self.store.state()["entries"]["a"]["value"], 1)

    def test_empty_or_noop_changes_are_refused(self):
        with self.assertRaises(StoreError):
            self.store.commit(0, [], "")
        self.store.commit(0, [change("a", 1)], "")
        with self.assertRaises(StoreError):
            self.store.commit(1, [change("a", 1)], "")  # gleicher Stand
        self.assertEqual(self.store.state()["version"], 1)

    def test_before_write_can_abort_cleanly(self):
        def refuse(_record):
            raise StoreError(409, "Datei geaendert")
        with self.assertRaises(StoreError):
            self.store.commit(0, [change("a", 1)], "", before_write=refuse)
        self.assertEqual(self.store.state()["version"], 0)
        self.assertFalse((self.root / "versions").exists() and os.listdir(self.root / "versions"))

    def test_crash_between_version_and_main_file_is_recovered(self):
        self.store.commit(0, [change("a", 1)], "")
        self.store.commit(1, [change("a", 2)], "")
        # Absturz simulieren: balance.json steht noch auf v1
        v1 = self.store.version(1)
        (self.root / "balance.json").write_text(json.dumps({"schema": 1, "version": 1, "entries": v1["entries"]}), encoding="utf-8")
        fresh = Store(self.root)
        state = fresh.state()
        self.assertEqual(state["version"], 2)
        self.assertEqual(state["entries"]["a"]["value"], 2)
        self.assertTrue(any("wiederhergestellt" in w for w in fresh.warnings))

    def test_corrupt_main_file_is_kept_aside_and_recovered(self):
        self.store.commit(0, [change("a", 1)], "")
        (self.root / "balance.json").write_text("{ kaputt", encoding="utf-8")
        fresh = Store(self.root)
        self.assertEqual(fresh.state()["entries"]["a"]["value"], 1)
        corrupt = [p for p in os.listdir(self.root) if p.startswith("balance.json.corrupt-")]
        self.assertEqual(len(corrupt), 1)
        self.assertEqual((self.root / corrupt[0]).read_text(encoding="utf-8"), "{ kaputt")

    def test_missing_main_file_is_recovered_from_versions(self):
        self.store.commit(0, [change("a", 1)], "")
        os.remove(self.root / "balance.json")
        self.assertEqual(Store(self.root).state()["version"], 1)

    def test_corrupt_version_number_is_never_reused(self):
        self.store.commit(0, [change("a", 1)], "")
        (self.root / "versions" / "v0002.json").write_text("halb geschrieben", encoding="utf-8")
        fresh = Store(self.root)
        state = fresh.state()
        self.assertEqual(state["version"], 1)
        fresh.commit(1, [change("a", 5)], "")
        self.assertEqual(fresh.state()["version"], 3)
        self.assertEqual((self.root / "versions" / "v0002.json").read_text(encoding="utf-8"), "halb geschrieben")
        self.assertTrue(any(h.get("corrupt") for h in fresh.history()))

    def test_existing_version_file_is_never_overwritten(self):
        self.store.versions.mkdir(parents=True)
        path = self.store.versions / "v0001.json"
        path.write_text("fremd", encoding="utf-8")
        with self.assertRaises(StoreError):
            store_mod._create_exclusive(path, "neu")
        self.assertEqual(path.read_text(encoding="utf-8"), "fremd")

    def test_concurrent_saves_on_the_same_base_one_wins(self):
        results = []
        barrier = threading.Barrier(8)

        def worker(i):
            s = Store(self.root)  # eigenes Objekt je Thread: wie getrennte Prozesse, nur Sperrdatei schuetzt
            barrier.wait()
            try:
                s.commit(0, [change("a", i)], f"t{i}")
                results.append(("ok", i))
            except StoreError as err:
                results.append((err.status, i))

        threads = [threading.Thread(target=worker, args=(i,)) for i in range(8)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        oks = [r for r in results if r[0] == "ok"]
        self.assertEqual(len(oks), 1, results)
        self.assertTrue(all(r[0] == 409 for r in results if r[0] != "ok"))
        state = Store(self.root).state()
        self.assertEqual(state["version"], 1)
        self.assertEqual(state["entries"]["a"]["value"], oks[0][1])

    def test_concurrent_saves_that_reread_lose_nothing(self):
        def worker(i):
            s = Store(self.root)
            for _ in range(200):
                base = s.state()["version"]
                try:
                    s.commit(base, [change(f"k{i}", i)], "")
                    return
                except StoreError as err:
                    if err.status not in (409, 423):
                        raise
                    time.sleep(0.01)
            raise AssertionError("kam nie dran")

        threads = [threading.Thread(target=worker, args=(i,)) for i in range(6)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        state = Store(self.root).state()
        self.assertEqual(state["version"], 6)
        self.assertEqual({k: v["value"] for k, v in state["entries"].items()}, {f"k{i}": i for i in range(6)})
        self.assertEqual(len([p for p in os.listdir(self.root / "versions") if p.endswith(".json")]), 6)

    def test_fresh_lock_blocks_and_stale_lock_is_broken(self):
        self.root.mkdir(parents=True, exist_ok=True)
        (self.root / ".lock").write_text("123", encoding="utf-8")
        blocked = Store(self.root, lock_timeout=0.3)
        with self.assertRaises(StoreError) as ctx:
            blocked.commit(0, [change("a", 1)], "")
        self.assertEqual(ctx.exception.status, 423)
        self.assertIn("gesperrt", ctx.exception.message)
        self.assertEqual(blocked.state()["version"], 0)
        old = time.time() - 120
        os.utime(self.root / ".lock", (old, old))
        s = Store(self.root)
        s.commit(0, [change("a", 1)], "")
        self.assertEqual(s.state()["version"], 1)
        self.assertFalse((self.root / ".lock").exists())


if __name__ == "__main__":
    unittest.main()
