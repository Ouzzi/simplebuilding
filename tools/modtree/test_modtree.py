import sys, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import modtree


class ModtreeTest(unittest.TestCase):
    def test_structure(self):
        mods = {"a": dict(id="a", name="A", desc="d", req=["simplelib"], opt=[], parent=None, bundle=None, status="done", loaders=[]),
                "b": dict(id="b", name="B", desc="d", req=[], opt=["a"], parent="a", bundle="optional", status="new", loaders=[]),
                "simplelib": dict(id="simplelib", name="Lib", desc="d", req=[], opt=[], parent=None, bundle=None, status="done", loaders=[])}
        sup, rest = modtree.roots(mods)
        self.assertEqual([m["id"] for m in sup], ["a"])
        self.assertEqual(len(rest), 0)
        h = modtree.render_html(mods)
        self.assertIn("edge-opt", h)
        self.assertIn("<code>b</code>", h)
        self.assertIn("`b` (optional) [neu]", modtree.render_md(mods))

    def test_real_data_every_module_listed(self):
        mods = modtree.load()
        h = modtree.render_html(mods)
        for mid in mods:
            self.assertIn(f"<code>{mid}</code>", h)


if __name__ == "__main__":
    unittest.main()
