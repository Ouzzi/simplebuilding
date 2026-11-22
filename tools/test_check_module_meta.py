"""Unit tests of check_module_meta.py (O6a): requires/description must equal fabric.mod.json, descriptions are player text."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_module_meta as c  # noqa: E402


class ModuleMetaTest(unittest.TestCase):
    def tree(self, entry, fmj):
        root = Path(tempfile.mkdtemp())
        (root / "modules").mkdir()
        (root / "modules/modules.json").write_text(json.dumps({"modules": [entry]}))
        path = root / f"modules/{entry['id']}/fabric/src/main/resources/fabric.mod.json"
        path.parent.mkdir(parents=True)
        path.write_text(json.dumps(fmj))
        return root

    def test_agreeing_metadata_passes(self):
        root = self.tree({"id": "simplex", "requires": ["simplelib", "cloth_config"], "description": "Adds lamps."},
                         {"depends": {"minecraft": "~26.3", "fabric-api": "*", "simplelib": "*", "cloth-config": "*"}, "description": "Adds lamps."})
        self.assertEqual(c.problems(root), [])

    def test_requires_and_description_mismatch(self):
        root = self.tree({"id": "simplex", "requires": ["cloth_config"], "description": "Adds lamps."},
                         {"depends": {"simplelib": "*"}, "description": "Adds lamps and panes."})
        found = "\n".join(c.problems(root))
        self.assertIn("not needed by the jar: cloth_config", found)
        self.assertIn("missing: simplelib", found)
        self.assertIn("description differs", found)

    def test_history_codes_and_british_wording(self):
        for text, hit in (("Light panes, moved from SimpleBuilding (ids migrate).", "moved from"), ("Since stage 2 the pistons.", "Since stage"),
                          ("Barrels (WH26-6).", "WH26-6"), ("Grass takes the biome colour.", "colour"), ("Bundles (8c-1).", "(8c-1)")):
            root = self.tree({"id": "simplex", "requires": [], "description": text}, {"depends": {}, "description": text})
            self.assertTrue(any(hit in p for p in c.problems(root)), (text, c.problems(root)))

    def test_example_modules_are_skipped(self):
        root = self.tree({"id": "simplex", "example": True, "requires": ["x"], "description": "Example (N12)."}, {"depends": {}})
        self.assertEqual(c.problems(root), [])


if __name__ == "__main__":
    unittest.main()
