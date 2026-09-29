"""Java-Leser, JSON-Chirurgie und Markdown - die kleinen Bausteine."""

import json
import unittest

import helpers  # noqa: F401  (setzt den Suchpfad)
from sbdev import docs, javasrc, jsonedit


class JavaSourceTests(unittest.TestCase):
    def test_comments_are_blanked_but_offsets_stay(self):
        src = 'int a = 1; // x = 2;\n/* int b = 3; */ String s = "// nicht weg";\n'
        out = javasrc.blank_comments(src)
        self.assertEqual(len(out), len(src))
        self.assertNotIn("x = 2", out)
        self.assertNotIn("int b", out)
        self.assertIn('"// nicht weg"', out)
        self.assertEqual(out.count("\n"), src.count("\n"))

    def test_evaluate_constant_expressions(self):
        env = {"BASE": (4, True), "OFFSET": (0.4, False), "MAX": (1024, True), "PEARLS": (16, True)}
        self.assertEqual(javasrc.evaluate("190 * BASE", env), (760, True))
        value, is_int = javasrc.evaluate("-3.0f - OFFSET", env)
        self.assertAlmostEqual(value, -3.4)
        self.assertFalse(is_int)
        self.assertEqual(javasrc.evaluate("MAX / PEARLS", env), (64, True))
        self.assertEqual(javasrc.evaluate("7 / 2", env), (3, True))  # Java-Ganzzahldivision
        self.assertEqual(javasrc.evaluate("-7 / 2", env), (-3, True))  # Richtung 0
        self.assertEqual(javasrc.evaluate("131_072", env), (131072, True))
        self.assertEqual(javasrc.evaluate("(int) 2.9", env), (2, True))
        with self.assertRaises(javasrc.ExprError):
            javasrc.evaluate("ninthOf(ModToolMaterials.X.durability())", env)
        with self.assertRaises(javasrc.ExprError):
            javasrc.evaluate("UNKNOWN * 2", env)

    def test_split_args_keeps_positions(self):
        text = "f(a, g(b, c), 0.5f)"
        args = javasrc.split_args(text, 2, len(text) - 1)
        self.assertEqual([a[0] for a in args], ["a", "g(b, c)", "0.5f"])
        for token, start, end in args:
            self.assertEqual(text[start:end], token)

    def test_doc_before_reads_javadoc_and_line_comments(self):
        raw = "/** Eisenkern pro {@code Kiste}. */\npublic static final float X = 0.1f;\n// zwei\n// Zeilen\nint Y = 2;\n"
        self.assertEqual(javasrc.doc_before(raw, raw.index("public")), "Eisenkern pro Kiste.")
        self.assertEqual(javasrc.doc_before(raw, raw.index("int Y")), "zwei Zeilen")


class JsonEditTests(unittest.TestCase):
    SRC = ('{\r\n  "wants": { "id": "minecraft:emerald", "count": 32 },\r\n  "gives": { "id": "x" },\r\n'
           '  "max_uses": 1,\r\n  "reputation_discount": 0.1\r\n}\r\n')

    def test_replace_changes_exactly_one_token(self):
        out = jsonedit.replace(self.SRC, ("wants", "count"), 40)
        self.assertEqual(out, self.SRC.replace('"count": 32', '"count": 40'))
        out = jsonedit.replace(self.SRC, ("reputation_discount",), 0.25)
        self.assertEqual(json.loads(out)["reputation_discount"], 0.25)
        self.assertIn("\r\n", out)  # Zeilenenden bleiben

    def test_insert_before_keeps_layout_and_line_endings(self):
        out = jsonedit.insert_before(self.SRC, (), "merchant_predicate",
                                     {"condition": "minecraft:random_chance", "chance": 0.5}, "max_uses")
        data = json.loads(out)
        self.assertEqual(data["merchant_predicate"]["chance"], 0.5)
        self.assertIn('  "merchant_predicate": { "condition": "minecraft:random_chance", "chance": 0.5 },\r\n  "max_uses": 1', out)
        self.assertNotRegex(out, r"(?<!\r)\n")  # keine LF-Zeile in einer CRLF-Datei

    def test_missing_path_and_containers_are_refused(self):
        with self.assertRaises(jsonedit.JsonEditError):
            jsonedit.replace(self.SRC, ("nope",), 1)
        with self.assertRaises(jsonedit.JsonEditError):
            jsonedit.replace(self.SRC, ("wants",), 1)
        with self.assertRaises(jsonedit.JsonEditError):
            jsonedit.replace(self.SRC, ("max_uses",), float("nan"))

    def test_scan_positions(self):
        spans = jsonedit.scan(self.SRC)
        span = spans[("wants", "count")]
        self.assertEqual(self.SRC[span["start"]:span["end"]], "32")
        self.assertEqual(jsonedit.line_of(self.SRC, span["start"]), 2)


class MarkdownTests(unittest.TestCase):
    def test_tables_lists_code_and_escaping(self):
        md = "# Titel\n\nText mit **fett** und `code <x>`.\n\n| a | b |\n|---|---|\n| 1 | <b>2</b> |\n\n- eins\n  - zwei\n- drei\n\n```\n<script>\n```\n"
        out = docs.render(md)
        self.assertIn('<h1 id="titel">Titel</h1>', out)
        self.assertIn("<strong>fett</strong>", out)
        self.assertIn("<code>code &lt;x&gt;</code>", out)
        self.assertIn("<td>&lt;b&gt;2&lt;/b&gt;</td>", out)
        self.assertIn("<ul><li>eins<ul><li>zwei</li></ul></li><li>drei</li></ul>", out)
        self.assertIn("&lt;script&gt;", out)
        self.assertNotIn("<script>", out)

    def test_real_docs_render_and_wiki_docs_stay_out(self):
        names = [d["name"] for d in docs.listing(helpers.REPO)]
        self.assertIn("KERNE-SELTENHEIT.md", names)
        self.assertNotIn("WIKI-HOSTING.md", names)
        page = docs.render_file(helpers.REPO, "KERNE-SELTENHEIT.md")
        self.assertIn("<table>", page["html"])
        with self.assertRaises(FileNotFoundError):
            docs.render_file(helpers.REPO, "../readme.md")


if __name__ == "__main__":
    unittest.main()
