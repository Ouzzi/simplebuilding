"""early_access.py (N108): launch version public at once, afterwards platforms one version behind; publish is a dry run
and refuses real uploads without the owner's go; no network."""
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import early_access as ea  # noqa: E402


class EarlyAccessScriptTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.d = self.tmp / "early"

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def jar(self, v):
        p = self.tmp / f"simplelib-fabric-{v}.jar"
        p.write_bytes(b"PK" + v.encode())
        return p

    def test_one_version_behind_after_launch(self):
        ea.stage(self.d, "1.0.0", [self.jar("1.0.0")], launch=True, today="2026-12-01")
        self.assertEqual(ea.due(ea.load(self.d)), [("modrinth", "1.0.0"), ("curseforge", "1.0.0")], "release: everywhere at once")
        ea.stage(self.d, "1.1.0", [self.jar("1.1.0")], launch=False)
        data = ea.load(self.d)
        self.assertEqual([(r["version"], r["public"]) for r in data["releases"]], [("1.1.0", False), ("1.0.0", True)])
        self.assertNotIn(("modrinth", "1.1.0"), ea.due(data))
        ea.stage(self.d, "1.2.0", [self.jar("1.2.0")], launch=False)
        self.assertIn(("modrinth", "1.1.0"), ea.due(ea.load(self.d)), "1.1.0 goes public with the next update")
        self.assertTrue((self.d / "files" / "simplelib-fabric-1.2.0.jar").is_file())

    def test_publish_dry_run_and_refusals(self):
        ea.stage(self.d, "1.0.0", [self.jar("1.0.0")], launch=True)
        sent, lines = [], []
        rc = ea.publish(self.d, "modrinth", False, False, ["26.3"], ["fabric"], env={}, sender=lambda *a: sent.append(a), out=lines.append)
        self.assertEqual((rc, sent), (0, []))
        self.assertIn("dry run", lines[0])
        rc = ea.publish(self.d, "modrinth", True, False, ["26.3"], ["fabric"], env={"MODRINTH_TOKEN": "t"}, sender=lambda *a: sent.append(a), out=lines.append)
        self.assertEqual((rc, sent), (2, []))
        rc = ea.publish(self.d, "curseforge", True, True, ["26.3"], ["fabric"], env={}, sender=lambda *a: sent.append(a), out=lines.append)
        self.assertEqual((rc, sent), (2, []), "no token")
        self.assertFalse(any("t" == x for x in lines))

    def test_publish_with_approval_marks_published(self):
        ea.stage(self.d, "1.0.0", [self.jar("1.0.0")], launch=True)
        sent = []
        rc = ea.publish(self.d, "modrinth", True, True, ["26.3"], ["fabric"], env={"MODRINTH_TOKEN": "tok", "MODRINTH_PROJECT": "abc"},
                        sender=lambda req, d, token: sent.append((req["url"], token)), out=lambda *_: None)
        self.assertEqual(rc, 0)
        self.assertEqual(sent, [("https://api.modrinth.com/v2/version", "tok")])
        self.assertEqual(ea.due(ea.load(self.d)), [("curseforge", "1.0.0")])

    def test_service_reads_the_index(self):
        ea.stage(self.d, "1.0.0", [self.jar("1.0.0")], launch=True)
        sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "livemap-web"))
        from smweb.early import EarlyMixin

        class S(EarlyMixin):
            cfg = {"early_access": {"dir": str(self.d)}}
        rel = S().early_releases()
        self.assertEqual(rel[0]["files"][0]["name"], "simplelib-fabric-1.0.0.jar")


if __name__ == "__main__":
    unittest.main()
