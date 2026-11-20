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
        # N119: a development status left in a player text (simpleblocks Pulse Shapecutter page)
        self.assertEqual(kinds("x", "en", "The textures are a preview waiting for the owner's choice."), ["history"])
        self.assertEqual(kinds("x", "de", "Die Texturen warten auf die Wahl des Besitzers."), ["history"])
        # N120: a design rule of the owner named in a player text (simplestorage pipe filter)
        self.assertEqual(kinds("x", "en", "Filter (the owner's filter principle, as with the hoppers):"), ["history"])
        self.assertEqual(kinds("x", "de", "Filter (Filter-Prinzip des Besitzers, wie bei den Trichtern):"), ["history"])
        self.assertEqual(kinds("x", "en", "On the owner's own land everybody is reached."), [])  # the block owner in play
        self.assertEqual(kinds("x", "en", "The restart was cancelled; it stays centred."), ["british", "british"])
        self.assertEqual(kinds("x", "de", "Farbe, cancelled"), [])  # British spelling only counts in English text

    def test_real_names_and_versions_pass(self):
        self.assertEqual(kinds("x", "de", "Nur für die Ports 26.2/1.21.11. Ab 26.3 hält der Löwenzahn"), [])  # not a date
        self.assertEqual(kinds("simplebuilding", "en", "P1"), [])
        self.assertEqual(kinds("simpleearth", "en", "V25"), [])
        self.assertEqual(kinds("x", "en", "Press F3, color #FF0000, OP4"), [])


class UmlautTest(unittest.TestCase):
    """N115: German umlauts/ß written as ASCII (curated TRANSLIT list), German text only."""

    def toks(self, text, lang="de"):
        return [tok for k, tok in c.findings("x", lang, text) if k == "umlaut"]

    def test_transliterations_found(self):
        self.assertEqual(self.toks("Das Gewoelbe hat 54 persoenliche Plaetze."), ["Gewoelbe", "persoenliche", "Plaetze"])
        self.assertEqual(self.toks("fuer alle, ueberall, koennen und muessen"), ["fuer", "ueberall", "koennen", "muessen"])
        self.assertEqual(self.toks("Die Groesse, so gross wie, Menues, Oeffnen"), ["Groesse", "gross", "Menues", "Oeffnen"])
        # inside compounds: unambiguous stems match anywhere in the word
        self.assertEqual(self.toks("Endertruhenplaetze, Wasserfuellung, Nihil-Gewoelbe, Rezeptplaetze 9"),
                         ["Endertruhenplaetze", "Wasserfuellung", "Gewoelbe", "Rezeptplaetze"])
        self.assertEqual(self.toks("In Simple QoL gebuendelt (frueher Simple QoL)"), ["gebuendelt", "frueher"])

    def test_real_words_pass(self):
        real = ("Feuer, Lagerfeuer, Abenteuer, neue, Quelle, aktuell, Steuerung, Mauer, Dauer, Wasser, muss, lassen, "
                "Blaueis, Zuschauer, Questionary, true, value, Schloss, Kessel, Messing, Rossharnisch, bauen, treue, "
                "zuerst, Vertrauen, Bauer, Schlüssel, Plätze, Gewölbe, für, über, Größe, groß, Tür, Türkis")
        self.assertEqual(self.toks(real), [])
        self.assertEqual(self.toks("Fuer Elise", lang="en"), [])        # English text is not checked
        self.assertEqual(self.toks("Beispiel fuer_elise"), [])          # ALLOW_TRANSLIT: ids
        self.assertEqual(self.toks("enableHoeHarvest, fuelEfficiency"), [])  # config keys


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

    def test_german_sources_checked_for_umlauts(self):
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/lang/de_de.json", {"a": "fuer dich"})
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/questionary/de_de.json",
                   {"x": {"lore": ["Ein Gewoelbe"]}})
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/questionary/en_us.json",
                   {"x": {"lore": ["Grosse grey"]}})               # English questionary: no umlaut check
        self.write("wiki/manual.json", {"features": [{"en": {"title": "Menue"}, "de": {"title": "Menue"}}]})
        rc, out = self.run_main()
        self.assertEqual(rc, 1, out)
        self.assertIn("lang/de_de.json a: umlaut 'fuer'", out)
        self.assertIn("questionary/de_de.json .x.lore[0]: umlaut 'Gewoelbe'", out)
        self.assertIn("wiki/manual.json de.features[0].de.title: umlaut 'Menue'", out)
        self.assertNotIn("en.title", out)
        self.assertNotIn("umlaut 'Grosse'", out)

    def test_english_questionary_checked_for_british(self):
        """N116: questionary/en_us.json gets the British check (colour, grey, centre, mouldy ...), all bases."""
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/questionary/en_us.json",
                   {"x": {"lore": ["A grey, mouldy block"], "use": ["Pick a colour; fills from the centre"]},
                    "family:y": {"secret": ["Coloured outlines"]}})
        self.write("mc26_3/overlay/resources/assets/simplebuilding/questionary/en_us.json", {"z": {"lore": ["colours"]}})
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/questionary/de_de.json",
                   {"x": {"lore": ["grey colour"]}})                            # German text: no British check
        rc, out = self.run_main()
        self.assertEqual(rc, 1, out)
        for tok in (".x.lore[0]: british 'grey'", ".x.lore[0]: british 'mouldy'", ".x.use[0]: british 'colour'",
                    ".x.use[0]: british 'centre'", ".family:y.secret[0]: british 'Coloured'"):
            self.assertIn("questionary/en_us.json " + tok, out)
        self.assertIn("mc26_3/overlay/resources/assets/simplebuilding/questionary/en_us.json .z.lore[0]: british 'colours'", out)
        self.assertNotIn("de_de.json", out)

    def test_english_questionary_us_spelling_passes(self):
        self.write("modules/simplefoo/shared/resources/assets/simplefoo/questionary/en_us.json",
                   {"x": {"lore": ["A gray, moldy block"], "use": ["Pick a color; fills from the center, colored"]}})
        self.assertEqual(self.run_main()[0], 0)


if __name__ == "__main__":
    unittest.main()
