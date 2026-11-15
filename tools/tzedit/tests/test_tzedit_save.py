"""N50 TZEDIT50: rules of a testcentre editor save (tools/tzedit/tzedit_save.py), the devqueue endpoint and the hub commit."""
import http.client
import json
import subprocess
import sys
import tempfile
import threading
import unittest
from pathlib import Path

HERE = Path(__file__).resolve()
sys.path.insert(0, str(HERE.parents[1]))
sys.path.insert(0, str(HERE.parents[2] / "zentrale-web"))
import tzedit_save as tz  # noqa: E402

GOOD = {"variant": "lines", "area": "metals-excerpt", "columns": ["button", "plate"],
        "rows": [{"key": "copper", "label": "copper", "cells": ["copper_button", None]},
                 {"key": "diamond", "label": "diamond", "cells": ["#red", "simplelib:diamond_pressure_plate"]}],
        "comment": "test"}


def bad(**change):
    d = json.loads(json.dumps(GOOD))
    d.update(change)
    return d


class CheckSave(unittest.TestCase):
    def test_good_is_cleaned(self):
        s = tz.check_save(dict(GOOD, extra="dropped"))
        self.assertNotIn("extra", s)
        self.assertEqual(tz.layout_path(s), tz.LAYOUT_DIR + "/layout_lines_metals-excerpt.json")
        self.assertEqual(json.loads(tz.layout_text(s))["rows"][1]["cells"][0], "#red")

    def test_rejects(self):
        for payload in (bad(variant="../x"), bad(area="A B"), bad(columns=[]), bad(columns=["ok", "Bad"]),
                        bad(rows=[{"key": "a", "cells": ["x"]}]),                      # wrong length
                        bad(rows=[{"key": "a", "cells": ["x", "y"]}, {"key": "a", "cells": [None, None]}]),  # duplicate key
                        bad(rows=[{"key": "a", "cells": ["<script>", None]}]),
                        bad(rows=[{"key": "a", "cells": ["#blue", None]}]),
                        bad(excluded=["ok", 3]), bad(comment="x" * 600), [], "x"):
            with self.assertRaises(tz.SaveError, msg=str(payload)[:80]):
                tz.check_save(payload)


    def test_tops(self):
        rows = [{"key": "iron", "label": "Iron", "cells": ["minecraft:iron_block", None],
                 "tops": {"plate": "simpleoctets:x", "button": "simpleoctets:iron_block_octet"}}]
        s = tz.check_save(bad(rows=rows))
        self.assertEqual(list(s["rows"][0]["tops"]), ["button", "plate"])     # column order, stable text
        self.assertEqual(json.loads(tz.layout_text(s))["rows"][0]["tops"]["button"], "simpleoctets:iron_block_octet")
        self.assertNotIn("tops", tz.check_save(GOOD)["rows"][0])
        for tops in ({"nope": "a:b"}, {"button": "<x>"}, ["button"]):
            with self.assertRaises(tz.SaveError):
                tz.check_save(bad(rows=[dict(rows[0], tops=tops)]))


class Export(unittest.TestCase):
    """export_layout.py: the integration dump tables -> one checked config per area."""

    def test_dump_to_configs(self):
        import export_layout as ex
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            rows = ["#lineZ\t9", "#band\t2\t3\t0\t7\t9\t11\t4"]
            for x, group, kind, side in ((2, "iron", "METAL", "A"), (3, "oak", "WOOD", "A"), (4, "andesite", "STONE", "A"),
                                         (2, "stone", "STONE", "B")):
                rows += [f"{group}\t{kind}\t{group}\t{side}\t{x}\t7\t0\tBUTTON\tminecraft:{group}_button\t",
                         f"{group}\t{kind}\t{group}\t{side}\t{x}\t6\t1\tPLATE\tMISSING\t",
                         f"{group}\t{kind}\t{group}\t{side}\t{x}\t5\t2\thollow_{{m}}_log\tNONE\tminecraft:octet",
                         f"{group}\t{kind}\t{group}\t{side}\t{x}\t4\t3\tspecial\tminecraft:{group}_x\t"]
            rows.append("#label\tiron\t2\t7\tIron Ingot")
            (d / "lines-rows.tsv").write_text("\n".join(rows) + "\n")
            (d / "compact-cells.tsv").write_text("simplefun\t0\tsimplefun:a\t3\t0\t3\tplace\n"
                                                 "simplefun\t1\tsimplefun:b\t2\t0\t4\tplace\n")
            saves = {s["area"]: s for s in ex.build(d)}
            self.assertEqual(list(saves), ["metals", "wood", "stone", "compact"])
            iron = saves["metals"]["rows"][0]
            self.assertEqual(saves["metals"]["columns"], ["button", "plate", "hollow_m_log", "special_1"])
            self.assertEqual(iron["cells"], ["minecraft:iron_button", "#red", "#light", "minecraft:iron_x"])
            self.assertEqual((iron["label"], iron["tops"]), ("Iron Ingot", {"hollow_m_log": "minecraft:octet"}))
            self.assertEqual([r["key"] for r in saves["stone"]["rows"]], ["stone", "andesite"])  # side B first
            self.assertEqual(saves["compact"]["rows"][0]["cells"], [None, "simplefun:a"])   # c0 = the mod sign
            self.assertEqual(saves["compact"]["rows"][1]["cells"], ["simplefun:b", None])


class Endpoint(unittest.TestCase):
    def setUp(self):
        from devqueue import DevQueueServer
        self.tmp = tempfile.TemporaryDirectory()
        self.server = DevQueueServer(("127.0.0.1", 0), Path(self.tmp.name))
        threading.Thread(target=self.server.serve_forever, daemon=True).start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def call(self, method, path, body=None, user="u@test"):
        c = http.client.HTTPConnection("127.0.0.1", self.server.server_address[1])
        headers = {"Content-Type": "application/json"}
        if user:
            headers["Cf-Access-Authenticated-User-Email"] = user
        c.request(method, path, body=json.dumps(body) if body is not None else None, headers=headers)
        r = c.getresponse()
        return r.status, json.loads(r.read() or b"null")

    def test_save_list_get(self):
        self.assertEqual(self.call("POST", "/api/testcentre", GOOD, user=None)[0], 403)
        self.assertEqual(self.call("POST", "/api/testcentre", bad(area="A B"))[0], 400)
        status, rec = self.call("POST", "/api/testcentre", GOOD)
        self.assertEqual(status, 201)
        self.assertEqual((rec["by"], rec["state"]), ("u@test", "new"))
        status, items = self.call("GET", "/api/testcentre")
        self.assertEqual((status, items[0]["sid"], items[0]["rowCount"]), (200, rec["sid"], 2))
        status, full = self.call("GET", "/api/testcentre/" + rec["sid"])
        self.assertEqual(full["rows"][0]["cells"], ["copper_button", None])
        self.assertEqual(self.call("GET", "/api/testcentre/../x")[0], 404)


class HubCommit(unittest.TestCase):
    def test_apply_creates_branch_and_never_overwrites(self):
        with tempfile.TemporaryDirectory() as tmp:
            hub, work = Path(tmp) / "hub.git", Path(tmp) / "w"
            run = lambda *a, cwd=None: subprocess.run(a, cwd=cwd, check=True, capture_output=True)
            run("git", "init", "-q", "--bare", str(hub))
            run("git", "init", "-q", "-b", "claude-wave1", str(work))
            (work / "README").write_text("x")
            run("git", "-C", str(work), "add", "README")
            run("git", "-C", str(work), "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-qm", "base")
            run("git", "-C", str(work), "push", "-q", str(hub), "claude-wave1")
            branch = tz.apply_save(GOOD, "1234567", hub, "claude-wave1", "u@test")
            self.assertEqual(branch, "tz-layout-lines-metals-excerpt-1234567")
            text = run("git", "--git-dir", str(hub), "show", f"{branch}:{tz.LAYOUT_DIR}/layout_lines_metals-excerpt.json").stdout
            self.assertEqual(json.loads(text)["rows"][0]["key"], "copper")
            with self.assertRaises(tz.SaveError):
                tz.apply_save(GOOD, "1234567", hub, "claude-wave1")


if __name__ == "__main__":
    unittest.main()
