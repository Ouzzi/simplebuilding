"""editor_save.py: path whitelist, limits and the plumbing commit of "Save to repo" (TX22-5, PLAN-A-WIKI24).

Run: python3 -m unittest discover -s tools/textures/tests
"""
import base64
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import editor_save as es  # noqa: E402
import layers as ly  # noqa: E402

SRC = "modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block/test_tile.png"
ENTRY = {"id": "simpleblocks:block/test_tile", "source": SRC, "w": 2, "h": 2}
DERIVED = {"id": "simplebuilding:block/cracked_x", "source": "build/vanilla-derived/resources/assets/simplebuilding/textures/block/cracked_x.png", "w": 2, "h": 2}
WL = {ENTRY["id"]: ENTRY, DERIVED["id"]: DERIVED}


def layers_doc(color="#ff0000ff", w=2, h=2):
    return {"format": "sb-layers", "version": 1, "width": w, "height": h, "digits": 2, "palette": [color],
            "layers": [{"name": "Base", "visible": True, "opacity": 100, "blend": "normal", "rows": ["00" * w] * h}]}


def payload(**kw):
    p = {"id": ENTRY["id"], "source": SRC, "layers": ly.dumps(layers_doc()), "comment": "heller"}
    p.update(kw)
    return p


class Whitelist(unittest.TestCase):
    def test_safe_sources(self):
        for ok in (SRC, "src/main/resources/assets/simplebuilding/textures/item/iron_chisel.png",
                   "mc26_3/overlay/resources/assets/minecraft/textures/block/x.png",
                   "modules/simplelib/fabric/src/main/resources/assets/simplelib/textures/item/a.png"):
            self.assertTrue(es.safe_source(ok), ok)
        for bad in ("../src/main/resources/assets/a/textures/b.png", "/etc/passwd",
                    "src/main/resources/assets/a/textures/../../../../x.png", "src/main/resources/assets/a/textures/b.txt",
                    "src\\main\\resources\\assets\\a\\textures\\b.png", "build/vanilla-derived/resources/assets/a/textures/b.png",
                    "src/main/java/com/x/Evil.png", "src/main/resources/assets/a/textures//b.png",
                    "src/main/resources/assets/a/lang/en_us.png", ".git/hooks/pre-commit.png", None, 5):
            self.assertFalse(es.safe_source(bad), bad)

    def test_every_listed_own_texture_is_allowed_and_no_derived_one(self):
        wl = es.load_whitelist()
        self.assertGreater(len(wl), 100)
        for t in wl.values():
            self.assertEqual(es.safe_source(t["source"]), not t["source"].startswith("build/"), t["source"])

    def test_accepts_a_listed_texture(self):
        save = es.check_save(payload(), WL)
        self.assertEqual(save["source"], SRC)
        self.assertEqual(save["comment"], "heller")
        self.assertEqual(json.loads(save["layers"])["palette"], ["#ff0000ff"])

    def test_rejects_unknown_id_foreign_source_and_derived(self):
        with self.assertRaisesRegex(es.SaveError, "unknown"):
            es.check_save(payload(id="simpleblocks:block/nope"), WL)
        with self.assertRaisesRegex(es.SaveError, "source"):
            es.check_save(payload(source="src/main/resources/assets/simpleblocks/textures/block/other.png"), WL)
        with self.assertRaisesRegex(es.SaveError, "source"):
            es.check_save(payload(source="modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block/../../../../../../build.gradle"), WL)
        with self.assertRaisesRegex(es.SaveError, "source"):
            es.check_save(payload(id=DERIVED["id"], source=DERIVED["source"]), WL)

    def test_limits_and_layers(self):
        with self.assertRaisesRegex(es.SaveError, "size"):
            es.check_save(payload(layers=ly.dumps(layers_doc(w=4, h=4))), WL)
        with self.assertRaisesRegex(es.SaveError, "too large"):
            es.check_save(payload(layers="x" * (es.MAX_LAYERS + 1)), WL)
        with self.assertRaisesRegex(es.SaveError, "layers"):
            es.check_save(payload(layers='{"format": "other"}'), WL)
        big = dict(ENTRY, id="a:big", w=600, h=600)
        with self.assertRaisesRegex(es.SaveError, "larger"):
            es.check_save(payload(id="a:big", layers=json.dumps(layers_doc(w=600, h=600))), {"a:big": big})
        with self.assertRaisesRegex(es.SaveError, "comment"):
            es.check_save(payload(comment="x" * (es.MAX_COMMENT + 1)), WL)
        self.assertEqual(es.check_save(payload(comment="a\x00b\x1b[31m"), WL)["comment"], "ab[31m")

    def test_png_must_match_the_layers(self):
        good = base64.b64encode(ly.png_write(2, 2, ly.compose(layers_doc()))).decode()
        es.check_save(payload(png=good), WL)
        other = base64.b64encode(ly.png_write(2, 2, ly.compose(layers_doc("#00ff00ff")))).decode()
        with self.assertRaisesRegex(es.SaveError, "differs"):
            es.check_save(payload(png=other), WL)
        with self.assertRaisesRegex(es.SaveError, "unreadable"):
            es.check_save(payload(png=base64.b64encode(b"\x89PNG\r\n\x1a\nbroken").decode()), WL)
        with self.assertRaisesRegex(es.SaveError, "too large"):
            es.check_save(payload(png="A" * (es.MAX_PNG * 2)), WL)


def run(*args, cwd=None):
    return subprocess.run(args, cwd=cwd, check=True, capture_output=True, text=True).stdout.strip()


class Apply(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        tmp = Path(self.tmp.name)
        work = tmp / "work"
        work.mkdir()
        (work / SRC).parent.mkdir(parents=True)
        (work / SRC).write_bytes(ly.png_write(2, 2, bytes(16)))
        (work / "wiki/data").mkdir(parents=True)
        (work / es.TEXTURES_JS).write_text("window.WIKI_TEXTURES = " + json.dumps({"textures": [ENTRY, DERIVED]}) + ";\n")
        env = {"GIT_AUTHOR_NAME": "t", "GIT_AUTHOR_EMAIL": "t@t", "GIT_COMMITTER_NAME": "t", "GIT_COMMITTER_EMAIL": "t@t"}
        os.environ.update(env)
        run("git", "init", "-q", "-b", "claude-wave1", cwd=work)
        run("git", "add", "-A", cwd=work)
        run("git", "commit", "-qm", "base", cwd=work)
        self.hub = tmp / "hub.git"
        run("git", "clone", "-q", "--bare", str(work), str(self.hub))

    def tearDown(self):
        self.tmp.cleanup()

    def test_commits_png_and_layers_on_a_new_branch(self):
        branch = es.apply_save(payload(), "1761234567", self.hub, "claude-wave1")
        self.assertEqual(branch, "tex-test_tile-1761234567")
        g = ["git", "--git-dir", str(self.hub)]
        files = run(*g, "diff", "--name-only", "claude-wave1", branch).splitlines()
        self.assertEqual(sorted(files), sorted([SRC, SRC[:-4] + ".layers.json"]))
        png = subprocess.run(g + ["show", f"{branch}:{SRC}"], capture_output=True, check=True).stdout
        w, h, rgba = ly.png_read(png)
        self.assertEqual((w, h, bytes(rgba[:4])), (2, 2, b"\xff\x00\x00\xff"))
        msg = run(*g, "log", "-1", "--format=%an%n%B", branch)
        self.assertIn("SB Wiki-Pixeleditor", msg)
        self.assertIn("heller", msg)
        self.assertEqual(run(*g, "rev-parse", "claude-wave1"), run(*g, "rev-parse", f"{branch}^"))
        with self.assertRaisesRegex(es.SaveError, "update-ref"):
            es.apply_save(payload(), "1761234567", self.hub, "claude-wave1")  # never overwrites

    def test_rejects_bad_ids_and_untracked_targets(self):
        with self.assertRaisesRegex(es.SaveError, "save id"):
            es.apply_save(payload(), "../x", self.hub, "claude-wave1")
        with self.assertRaisesRegex(es.SaveError, "source"):
            es.apply_save(payload(id=DERIVED["id"], source=DERIVED["source"]), "1761234568", self.hub, "claude-wave1")

    def test_pickup_marks_ready_and_failed(self):
        store = Path(self.tmp.name) / "api.json"
        good = dict(payload(), sid="1761234570", state="new", texture=ENTRY["id"])
        bad = dict(payload(id="simpleblocks:block/nope"), sid="1761234571", state="new", texture="simpleblocks:block/nope")
        store.write_text(json.dumps({"1761234570": good, "1761234571": bad}))
        fake = Path(self.tmp.name) / "fake_api.py"
        fake.write_text(
            "import json,sys\n"
            f"p={str(store)!r}; d=json.load(open(p)); m,path=sys.argv[2],sys.argv[3]\n"
            "if path=='textures': print(json.dumps([{'sid':k,'state':v['state']} for k,v in d.items()]))\n"
            "elif m=='GET': print(json.dumps(d[path.split('/')[1]]))\n"
            "else:\n b=json.loads(sys.argv[4]); d[path.split('/')[1]].update(b); json.dump(d,open(p,'w')); print('{}')\n")
        n = es.pickup([sys.executable, str(fake)], self.hub, "claude-wave1", log=lambda *_: None)
        self.assertEqual(n, 2)
        d = json.loads(store.read_text())
        self.assertEqual((d["1761234570"]["state"], d["1761234570"]["branch"]), ("ready", "tex-test_tile-1761234570"))
        self.assertEqual(d["1761234571"]["state"], "failed")
        self.assertIn("unknown", d["1761234571"]["note"])
        self.assertEqual(es.pickup([sys.executable, str(fake)], self.hub, "claude-wave1", log=lambda *_: None), 0)


if __name__ == "__main__":
    unittest.main()
