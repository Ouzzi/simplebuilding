"""O6b: the cross-mod index data/index.js (wiki/wiki_index.py) and per-mod lazy loading (assets/module-loader.js).

Run from the repository root: python -m unittest discover -s wiki/tests -p test_wiki_index.py
The browser part lives in wiki/tests/o6b_lazy_core.cjs (run here when node is there); live check on the dev wiki:
/root/n74dev/o6b_measure.cjs and the click test of O6b.
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

WIKI = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(WIKI))
import wiki_index as wi  # noqa: E402


class IndexTests(unittest.TestCase):
    def test_committed_index_is_current_and_small(self):
        self.assertEqual(wi.sync(WIKI, check=True), [])
        self.assertLess((WIKI / wi.TARGET).stat().st_size, 1_500_000)

    def test_every_catalogue_mod_and_id(self):
        index = wi.read(WIKI)
        self.assertIsNotNone(index)
        for m in wi.catalogue(WIKI):
            path = WIKI / "data" / (m["id"] + ".json")
            if not path.is_file():
                continue
            ix = index["mods"].get(m["id"])
            self.assertIsNotNone(ix, m["id"])
            data = json.loads(path.read_text(encoding="utf-8"))
            have = {e[0] for e in ix["items"] + ix["blocks"]}
            ids = {e["id"] for k in ("items", "blocks") for e in data.get(k) or [] if isinstance(e, dict) and e.get("id")}
            self.assertEqual(ids - have, set(), m["id"])
            self.assertEqual(ix["counts"], wi.counts(data), m["id"])
            self.assertEqual(ix["head"].get("mod"), data.get("mod"), m["id"])

    def test_public_prune_rules(self):
        """The Python copy of pruneHidden (module-loader.js): needsMods, result of a hidden mod, a slot only it fills."""
        d = {"recipes": [{"id": "x:a", "result": {"id": "x:a"}, "slots": {"i": ["h:p", "minecraft:stone"]}},
                         {"id": "x:b", "result": {"id": "x:b"}, "key": {"A": ["h:p"]}},
                         {"id": "x:c", "result": {"id": "h:q"}},
                         {"id": "x:d", "result": {"id": "x:d"}, "needsMods": ["h"]}],
             "items": [{"id": "x:a"}, {"id": "x:e", "needsMods": ["h"]}],
             "blocks": [{"id": "x:f", "needsMods": ["h"], "lootTable": "x:blocks/f"}, {"id": "x:g"}],
             "lootTables": [{"id": "x:blocks/f"}, {"id": "x:blocks/g"}]}
        p = wi.pruned(d, {"h"})
        self.assertEqual([r["id"] for r in p["recipes"]], ["x:a"])
        self.assertEqual([e["id"] for e in p["items"]], ["x:a"])
        self.assertEqual([e["id"] for e in p["blocks"]], ["x:g"])
        self.assertEqual([t["id"] for t in p["lootTables"]], ["x:blocks/g"])
        self.assertIs(wi.pruned(d, {"other"}), d)

    def test_check_reports_a_stale_index(self):
        with tempfile.TemporaryDirectory() as tmp:
            wiki = Path(tmp)
            (wiki / "data").mkdir()
            (wiki / "data/modules.js").write_text('window.WIKI_MODULES = [{"id": "a"}, {"id": "b", "release": false}];\n', encoding="utf-8")
            (wiki / "data/a.json").write_text(json.dumps({"mod": {"id": "a"}, "items": [{"id": "a:x", "name": {"en_us": "X", "de_de": "Y"}}],
                                                           "recipes": [{"id": "a:r", "result": {"id": "b:z"}}]}), encoding="utf-8")
            self.assertEqual(len(wi.sync(wiki, check=True)), 1)
            self.assertEqual(wi.sync(wiki), [])
            self.assertEqual(wi.sync(wiki, check=True), [])
            ix = wi.read(wiki)["mods"]["a"]
            self.assertEqual(ix["items"], [["a:x", "X", "Y"]])
            self.assertEqual(ix["recipes"], ["r"])
            self.assertEqual(ix["counts"]["recipes"], 1)
            self.assertEqual(ix["countsPublic"]["recipes"], 0)

    @unittest.skipUnless(shutil.which("node"), "node fehlt")
    def test_loader_core(self):
        r = subprocess.run(["node", str(Path(__file__).with_name("o6b_lazy_core.cjs"))], capture_output=True, text=True, timeout=300)
        out = json.loads((r.stdout.strip().splitlines() or ["{}"])[-1])
        self.assertEqual(out.get("failures"), [], r.stderr[-2000:])
        self.assertGreater(out.get("checks", 0), 100)


if __name__ == "__main__":
    unittest.main()
