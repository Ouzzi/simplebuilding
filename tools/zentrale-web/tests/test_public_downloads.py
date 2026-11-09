"""W12: public release index for simplemods.de/downloads/ (public_downloads.py)."""
import hashlib
import io
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HERE))

import public_downloads as pd  # noqa: E402


def jar(mod, depends=None, suggests=None, nested=()):
    b = io.BytesIO()
    with zipfile.ZipFile(b, "w") as z:
        z.writestr("fabric.mod.json", json.dumps({
            "schemaVersion": 1, "id": mod, "version": "1.0.0", "depends": depends or {}, "suggests": suggests or {},
            "jars": [{"file": f"META-INF/jars/{n}"} for n in nested]}))
    return b.getvalue()


class PublicDownloadsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.out = Path(self.tmp.name)
        files = {
            "simplelib-26.3-fabric-0.4.0.jar": ("simplelib", "fabric", jar("simplelib", {"fabric-api": "*", "minecraft": "~26.3"})),
            "simpleores-26.3-fabric-0.1.0.jar": ("simpleores", "fabric", jar(
                "simpleores", {"simplelib": ">=0.4", "fabric-api": "*", "java": ">=25"},
                {"jei": "*", "simplebuilding": "*", "claimchunk": "*"}, ["simplelib-26.3-fabric-0.4.0.jar"])),
            "simplebuilding-26.3-2.0.0.jar": ("simplebuilding", "fabric", jar(
                "simplebuilding", {"cloth_config": "*", "fabric-api": "*", "simpleores": "*"}, {},
                ["simplelib-26.3-fabric-0.4.0.jar", "simpleores-26.3-fabric-0.1.0.jar"])),
            "simplelib-26.3-neoforge-0.4.0.jar": ("simplelib", "neoforge", b"PK\x05\x06" + b"\0" * 18),
        }
        good = []
        for name, (mod, loader, data) in files.items():
            p = self.out / "files/aaaaaaaaaa" / loader / "26.3" / name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(data)
            good.append({"module": mod, "name": mod, "loader": loader, "minecraft": "26.3", "version": "1.0.0",
                         "file": name, "size": len(data), "sha256": hashlib.sha256(data).hexdigest(),
                         "url": f"files/aaaaaaaaaa/{loader}/26.3/{name}"})
        partial = [dict(good[2], url="files/bbbbbbbbbb/fabric/26.3/simplebuilding-26.3-2.0.0.jar")]
        (self.out / "index.json").write_text(json.dumps({"schemaVersion": 1, "builds": [
            {"commit": "b" * 40, "short": "bbbbbbbbbb", "date": "2026-11-09T05:00:00+00:00", "files": partial},
            {"commit": "a" * 40, "short": "aaaaaaaaaa", "date": "2026-11-09T02:00:00+00:00", "verdict": "VERDICT: GREEN",
             "files": good}]}))

    def tearDown(self):
        self.tmp.cleanup()

    def test_complete_build_only_public_loaders(self):
        p = pd.build(self.out)
        self.assertEqual(p["release"]["commit"], "aaaaaaaaaa")  # newest build lacks SimpleLib -> skipped
        self.assertEqual(p["loaders"], ["fabric"])
        self.assertEqual({m["loader"] for m in p["mods"]}, {"fabric"})
        self.assertNotIn("builds", p)
        on_disk = sorted(x.name for x in (self.out / "public/files/fabric/26.3").iterdir())
        self.assertEqual(on_disk, sorted(m["file"] for m in p["mods"]))
        self.assertFalse((self.out / "public/files/neoforge").exists())
        for m in p["mods"]:
            data = (self.out / "public" / m["url"]).read_bytes()
            self.assertEqual(hashlib.sha256(data).hexdigest(), m["sha256"])
        self.assertEqual(json.loads((self.out / "public/index.json").read_text()), p)

    def test_dependencies_contains_and_external_links(self):
        m = {x["module"]: x for x in pd.build(self.out)["mods"]}
        self.assertEqual(m["simplebuilding"]["contains"], ["simplelib", "simpleores"])
        self.assertEqual(m["simplebuilding"]["external"], ["cloth-config", "fabric-api"])
        self.assertIn("simplelib", m["simpleores"]["requires"])
        self.assertEqual(m["simpleores"]["externalOptional"], ["jei"])  # Simple mods and compat hooks are no download hint
        p = json.loads((self.out / "public/index.json").read_text())
        self.assertTrue(p["externalMods"]["fabric-api"]["url"].startswith("https://modrinth.com/"))

    def test_no_complete_build(self):
        idx = json.loads((self.out / "index.json").read_text())
        idx["builds"] = idx["builds"][:1]
        (self.out / "index.json").write_text(json.dumps(idx))
        self.assertIsNone(pd.build(self.out))
        self.assertFalse((self.out / "public").exists())

    def test_rebuild_replaces_old_files(self):
        pd.build(self.out)
        (self.out / "public/files/fabric/26.3/stale.jar").write_bytes(b"x")
        pd.build(self.out)
        self.assertFalse((self.out / "public/files/fabric/26.3/stale.jar").exists())


if __name__ == "__main__":
    unittest.main()
