"""Unit tests of check_player_texts.py (N114: every lang value, SimpleBuilding's own lang + wiki/manual.json, version numbers)."""
import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_player_texts as c  # noqa: E402


def kinds(mod, lang, text):
    return [k for k, _ in c.findings(mod, lang, text)]


class FindingsTest(unittest.TestCase):
    def test_codes_history_british(self):
        self.assertEqual(kinds("simpleearth", "en", "E9: other players appear"), ["code"])
        self.assertEqual(kinds("x", "en", "Coated kinds (PAL14):"), ["code"])
        self.assertEqual(kinds("x", "de", "seit dem 27.08.2026 zurückgestellt"), ["history"])
        self.assertEqual(kinds("x", "en", "Enderite 2.0 (3.5 until 2026-09)."), ["history"])
        self.assertEqual(kinds("x", "en", "26.2 keeps it until the port run."), ["history"])
        self.assertEqual(kinds("x", "en", "The restart was cancelled; it stays centred."), ["british", "british"])
        self.assertEqual(kinds("x", "de", "Farbe, cancelled"), [])  # British spelling only counts in English text

    def test_real_names_and_versions_pass(self):
        self.assertEqual(kinds("x", "de", "Nur für die Ports 26.2/1.21.11. Ab 26.3 hält der Löwenzahn"), [])  # not a date
        self.assertEqual(kinds("simplebuilding", "en", "P1"), [])
        self.assertEqual(kinds("simpleearth", "en", "V25"), [])
        self.assertEqual(kinds("x", "en", "Press F3, color #FF0000, OP4"), [])


class RepoScanTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.saved = c.R
        c.R = Path(self.tmp.name)

    def tearDown(self):
        c.R = self.saved
        self.tmp.cleanup()

    def write(self, rel, data):
        p = c.R / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(data), encoding="utf-8")

    def run_main(self):
        out = io.StringIO()
        with redirect_stdout(out):
            rc = c.main(["--list"])
        return rc, out.getvalue()

    def test_clean_tree_passes(self):
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/lang/en_us.json", {"item.simplefoo.a": "A color"})
        self.write("wiki/manual.json", {"features": [{"en": {"title": "Test center"}, "de": {"title": "Testzentrale"}}]})
        self.assertEqual(self.run_main()[0], 0)

    def test_every_source_is_checked(self):
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/lang/en_us.json",
                   {"text.autoconfig.simplefoo.option.a.@Tooltip": "Next colour."})          # a non-guide module value
        self.write("src/main/resources/assets/simplebuilding/lang/en_us.json", {"a": "grey line"})
        self.write("mc26_3/overlay/resources/assets/simplebuilding/lang/de_de.json", {"b": "seit N49"})
        self.write("wiki/manual.json", {"features": [{"en": {"title": "Test centre"}, "sources": ["N12"]}]})
        rc, out = self.run_main()
        self.assertEqual(rc, 1, out)
        for where in ("modules/simplefoo/", "src/main/resources/", "mc26_3/overlay/", "wiki/manual.json"):
            self.assertIn(where, out)
        self.assertNotIn("N12", out)  # "sources" are not player text


if __name__ == "__main__":
    unittest.main()
