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
ASRC = "modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block/test_anim.png"
ANIM = {"id": "simpleblocks:block/test_anim", "source": ASRC, "w": 2, "h": 6,
        "anim": {"w": 2, "h": 2, "cols": 1, "count": 3, "frametime": 4}, "mcmeta": {"animation": {"frametime": 4}}}
MCMETA = b'{"animation": {"frametime": 4}}\n'
WL = {ENTRY["id"]: ENTRY, DERIVED["id"]: DERIVED, ANIM["id"]: ANIM}


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

    def test_animated_sheet_keeps_its_frames_and_mcmeta(self):
        """a-wikiux06: a frame edit is a whole-sheet save of the same size; .mcmeta never comes along."""
        frames = layers_doc(w=2, h=6)
        save = es.check_save(payload(id=ANIM["id"], source=ASRC, layers=ly.dumps(frames)), WL)
        self.assertEqual(json.loads(save["layers"])["height"], 6)
        with self.assertRaisesRegex(es.SaveError, "size"):  # one frame alone is not a sheet
            es.check_save(payload(id=ANIM["id"], source=ASRC, layers=ly.dumps(layers_doc(w=2, h=2))), WL)
        with self.assertRaisesRegex(es.SaveError, "mcmeta"):
            es.check_save(payload(id=ANIM["id"], source=ASRC, layers=ly.dumps(frames), mcmeta={"animation": {}}), WL)
        broken = dict(ANIM, anim=dict(ANIM["anim"], count=4))
        with self.assertRaisesRegex(es.SaveError, "frames"):
            es.check_save(payload(id=ANIM["id"], source=ASRC, layers=ly.dumps(frames)), {ANIM["id"]: broken})

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
        (work / ASRC).write_bytes(ly.png_write(2, 6, bytes(48)))
        (work / (ASRC + ".mcmeta")).write_bytes(MCMETA)
        (work / "wiki/data").mkdir(parents=True)
        (work / es.TEXTURES_JS).write_text("window.WIKI_TEXTURES = " + json.dumps({"textures": [ENTRY, DERIVED, ANIM]}) + ";\n")
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

    def test_animated_commit_leaves_the_mcmeta_alone(self):
        branch = es.apply_save(payload(id=ANIM["id"], source=ASRC, layers=ly.dumps(layers_doc(w=2, h=6))), "1761234580",
                               self.hub, "claude-wave1")
        g = ["git", "--git-dir", str(self.hub)]
        files = run(*g, "diff", "--name-only", "claude-wave1", branch).splitlines()
        self.assertEqual(sorted(files), sorted([ASRC, ASRC[:-4] + ".layers.json"]))
        meta = subprocess.run(g + ["show", f"{branch}:{ASRC}.mcmeta"], capture_output=True, check=True).stdout
        self.assertEqual(meta, MCMETA)
        w, h, _ = ly.png_read(subprocess.run(g + ["show", f"{branch}:{ASRC}"], capture_output=True, check=True).stdout)
        self.assertEqual((w, h), (2, 6))  # frames stay stacked: height = 3 x frame

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

    # ---------------------------------------------------------------- b-texed16: read-only pull + adopted_in
    def _remote(self, records):
        """A web host stand-in: a directory with <sid>.json read by `ls`/`cat` through a fake command prefix."""
        d = Path(self.tmp.name) / "srv"
        d.mkdir(exist_ok=True)
        for sid, rec in records.items():
            (d / f"{sid}.json").write_text(json.dumps(rec))
        fake = Path(self.tmp.name) / "fake_remote.py"
        fake.write_text("import subprocess,sys\nsys.exit(subprocess.run(sys.argv[1:]).returncode)\n")
        return [sys.executable, str(fake)], str(d)

    def test_pull_is_read_only_idempotent_and_newest_wins(self):
        rec = lambda sid, color, state="new": dict(sid=sid, texture=ENTRY["id"], source=ENTRY["source"], state=state,
                                                   layers=ly.dumps(layers_doc(color)), comment="", by="owner@x")
        cmd, d = self._remote({"1761234580": rec("1761234580", "#00ff00ff"), "1761234581": rec("1761234581", "#0000ffff"),
                               "1761234582": dict(rec("1761234582", "#ffffffff"), texture="simpleblocks:block/nope"),
                               "1761234583": dict(rec("1761234583", "#ffffffff", "failed"), texture="other:x")})
        before = {p.name: p.read_text() for p in Path(d).iterdir()}
        report, state = Path(self.tmp.name) / "fertig.md", Path(self.tmp.name) / "state.json"
        made = es.pull(cmd, d, self.hub, "claude-wave1", report, state, log=lambda *_: None)
        self.assertEqual(made, ["tex-test_tile-1761234581"])          # older save of the same texture superseded
        self.assertEqual({p.name: p.read_text() for p in Path(d).iterdir()}, before, "nothing written on the web host")
        line = report.read_text()
        self.assertIn("tex-test_tile-1761234581", line)
        self.assertIn("Save-ID 1761234581", line)
        self.assertEqual(run("git", "--git-dir", str(self.hub), "cat-file", "-t", f"tex-test_tile-1761234581:{SRC}"), "blob")
        st = json.loads(state.read_text())["failed"]
        self.assertIn("1761234582", st)                                  # unknown texture: remembered, not retried
        self.assertIn("superseded", st["1761234580"])
        # second run: nothing new, no new line
        self.assertEqual(es.pull(cmd, d, self.hub, "claude-wave1", report, state, log=lambda *_: None), [])
        self.assertEqual(report.read_text(), line)

    def test_pull_skips_saves_taken_over_by_hand(self):
        cmd, d = self._remote({"1761234590": dict(sid="1761234590", texture=ENTRY["id"], source=ENTRY["source"], state="new",
                                                  layers=ly.dumps(layers_doc()), comment="")})
        work = Path(self.tmp.name) / "work"
        run("git", "commit", "-q", "--allow-empty", "-m", "tex: by hand\n\nSave-IDs 1761234590", cwd=work)
        run("git", "push", "-q", str(self.hub), "claude-wave1:b-hand", cwd=work)
        self.assertEqual(es.pull(cmd, d, self.hub, "claude-wave1", None, None, log=lambda *_: None), [])

    def test_adopted_in_finds_commit_and_bundle(self):
        work = Path(self.tmp.name) / "work"
        run("git", "checkout", "-q", "-b", "tex-x", cwd=work)
        run("git", "commit", "-q", "--allow-empty", "-m", "tex(x): Editor\n\nSave-ID: 1761234600", cwd=work)
        tex = run("git", "rev-parse", "--short=9", "HEAD", cwd=work)
        run("git", "checkout", "-q", "claude-wave1", cwd=work)
        run("git", "commit", "-q", "--allow-empty", "-m", "other", cwd=work)
        run("git", "merge", "-q", "--no-ff", "-m", "Merge tex-x into bundle 170", "tex-x", cwd=work)
        got = es.adopted_in(work, ["1761234600", "1761234601", "../x"])
        self.assertEqual(set(got), {"1761234600"})
        self.assertTrue(tex.startswith(got["1761234600"]["commit"]) or got["1761234600"]["commit"].startswith(tex[:7]))
        self.assertEqual(got["1761234600"]["bundle"], "b-bundle170")
        self.assertEqual(es.adopted_in(work, []), {})
        self.assertEqual(es.adopted_in(Path(self.tmp.name) / "missing", ["1761234600"]), {})


if __name__ == "__main__":
    unittest.main()
