"""fill_legal.py: fills {{betreiber.*}}/{{kontakt.*}} and blocks from a FAKE operator file (temp dir), never prints values,
fails on missing keys or leftovers, refuses an output folder inside the repo; renders the real docs/legal texts."""
from __future__ import annotations

import json
import shutil
import sys
import unittest
from pathlib import Path

from helpers import ROOT, temp

sys.path.insert(0, str(ROOT))
import fill_legal  # noqa: E402

FAKE = {"name": "Erika Testbetrieb", "anschrift": "Musterweg 1\n12345 Musterstadt", "email": "erika@example.org", "kleinunternehmer": True,
        "telefon": "", "ustid": ""}


class FillLegalTests(unittest.TestCase):
    def setUp(self):
        self.dir = Path(temp())
        self.op = self.dir / "betreiber.json"
        self.op.write_text(json.dumps(FAKE))
        self.src = self.dir / "src"
        self.src.mkdir()
        for name in fill_legal.FILES:
            (self.src / name).write_text("<!-- intern: ungeprüft – vor Zahlungsstart anwaltlich prüfen. -->\n# T\n{{betreiber.name}}\n{{betreiber.anschrift}}\n"
                                         "Mail {{kontakt.allgemein}} / {{kontakt.melden}}\n{{#wenn betreiber.telefon}}Tel {{betreiber.telefon}}{{/wenn}}\n"
                                         "{{#wenn betreiber.kleinunternehmer}}§ 19 UStG{{#sonst}}inkl. USt.{{/wenn}}\n")
        self.lines = []

    def tearDown(self):
        shutil.rmtree(self.dir, ignore_errors=True)

    def run_fill(self, *extra, src=None):
        return fill_legal.main(["--operator", str(self.op), "--src", str(src or self.src), "--out", str(self.dir / "out"), *extra], out=self.lines.append)

    def test_fills_and_never_prints_values(self):
        self.assertEqual(self.run_fill(), 0, self.lines)
        text = (self.dir / "out" / "impressum.md").read_text()
        self.assertIn("Erika Testbetrieb\nMusterweg 1\n12345 Musterstadt", text)
        self.assertIn("Mail erika@example.org / erika@example.org", text)
        self.assertIn("§ 19 UStG", text)
        self.assertNotIn("Tel", text)
        self.assertNotIn("{{", text)
        joined = "\n".join(self.lines)
        for value in ("Erika", "Musterweg", "erika@example.org"):
            self.assertNotIn(value, joined)

    def test_contact_file_and_vat_switch_and_strip(self):
        self.op.write_text(json.dumps({**FAKE, "kleinunternehmer": False, "telefon": "0123"}))
        c = self.dir / "contact.json"
        c.write_text(json.dumps({"allgemein": "kontakt@example.net", "datenschutz": "ds@example.net", "melden": "melden@example.net"}))
        self.assertEqual(self.run_fill("--contact-file", str(c), "--strip-internal"), 0, self.lines)
        text = (self.dir / "out" / "agb.md").read_text()
        self.assertIn("Mail kontakt@example.net / melden@example.net", text)
        self.assertIn("inkl. USt.", text)
        self.assertIn("Tel 0123", text)
        self.assertNotIn("intern:", text)

    def test_fails_on_missing_key_or_leftover(self):
        self.op.write_text(json.dumps({k: v for k, v in FAKE.items() if k != "anschrift"}))
        self.assertEqual(self.run_fill(), 1)
        self.assertIn("anschrift", self.lines[-1])
        self.op.write_text(json.dumps(FAKE))
        (self.src / "agb.md").write_text("{{betreiber.geburtstag}}")
        self.assertEqual(self.run_fill(), 1)
        self.assertFalse((self.dir / "out").exists(), "nothing written on failure")

    def test_refuses_output_in_repo(self):
        rc = fill_legal.main(["--operator", str(self.op), "--src", str(self.src), "--out", str(fill_legal.REPO / "tmp-legal")], out=self.lines.append)
        self.assertEqual(rc, 2)

    def test_real_texts_render_completely(self):
        src = fill_legal.REPO / "docs" / "legal"
        if not all((src / n).exists() for n in fill_legal.FILES):
            self.skipTest("legal texts not complete")
        self.assertEqual(self.run_fill(src=src), 0, self.lines)
        for n in fill_legal.FILES:
            text = (self.dir / "out" / n).read_text()
            self.assertNotIn("⟦", text, n)
            self.assertIn("Erika Testbetrieb", (self.dir / "out" / "impressum.md").read_text())


if __name__ == "__main__":
    unittest.main()
