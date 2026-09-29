"""Pure-logic tests of the Launch- und Testzentrale. Nothing is launched: run with
python -m unittest discover tools/launchhub/tests"""

import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

HUB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HUB))

from hub import ai, paths, runs, settings, targets, worktrees  # noqa: E402
from server import host_ok  # noqa: E402


def make_record(run_id, commit="a" * 40, tests=None, target="fabric-263", filt=None, trigger="hub", dirty=False, error=None, ok=None):
    tests = tests or []
    failed = sum(1 for t in tests if t[1] == "failed")
    return runs.slim({
        "runId": run_id, "startedAt": run_id[:10] + "T10:00:" + run_id[17:19] + "Z",
        "finishedAt": None, "durationMs": 1000, "trigger": trigger, "filter": filt,
        "git": {"commit": commit, "short": commit[:7], "branch": "master", "dirty": dirty},
        "totals": {"total": len(tests), "passed": len(tests) - failed, "failed": failed},
        "ok": (failed == 0 and not error) if ok is None else ok,
        "targets": [{"id": target, "label": target, "selected": True, "exitCode": 0, "durationMs": 500,
                     "counts": {"total": len(tests), "passed": len(tests) - failed, "failed": failed},
                     "missing": [], "tests": [{"id": t[0], "status": t[1], "message": t[2] if len(t) > 2 else None} for t in tests],
                     "error": error, "kind": "server"}],
    })


class RunRecordTests(unittest.TestCase):
    def test_verdict_uses_the_record_not_an_exit_code(self):
        green = make_record("2026-09-29T10-00-01Z-aaaa", tests=[("simplebuilding:a", "passed")])
        red = make_record("2026-09-29T10-00-02Z-bbbb", tests=[("simplebuilding:a", "failed", "boom")])
        broken = make_record("2026-09-29T10-00-03Z-cccc", tests=[], error="no report")
        self.assertEqual(runs.verdict(green), "green")
        self.assertEqual(runs.verdict(red), "red")
        self.assertEqual(runs.verdict(broken), "red")  # zero tests is never green

    def test_latest_state_and_first_failed(self):
        recs = [  # newest first
            make_record("2026-09-29T10-00-03Z-cccc", tests=[("simplebuilding:t", "failed", "x")]),
            make_record("2026-09-29T10-00-02Z-bbbb", tests=[("simplebuilding:t", "failed", "x")]),
            make_record("2026-09-29T10-00-01Z-aaaa", tests=[("simplebuilding:t", "passed")]),
        ]
        self.assertEqual(runs.latest_states(recs)["fabric-263"]["simplebuilding:t"]["status"], "failed")
        seq = runs.history(recs, "fabric-263", "simplebuilding:t")
        self.assertEqual(runs.first_failed(seq)["runId"], "2026-09-29T10-00-02Z-bbbb")

    def test_flaky_confirmed_needs_same_clean_commit(self):
        same = [{"status": "passed", "commit": "c1", "dirty": False}, {"status": "failed", "commit": "c1", "dirty": False}]
        self.assertEqual(runs.flaky_kind(same), "confirmed")
        fixed = [{"status": "failed", "commit": "c1", "dirty": False}, {"status": "passed", "commit": "c2", "dirty": False}]
        self.assertIsNone(runs.flaky_kind(fixed))  # regression + fix is not flakiness
        dirty = [{"status": "passed", "commit": "c1", "dirty": True}, {"status": "failed", "commit": "c1", "dirty": True}]
        self.assertIsNone(runs.flaky_kind(dirty))
        flips = [{"status": s, "commit": f"c{i}", "dirty": False} for i, s in enumerate(["passed", "failed", "passed", "failed"])]
        self.assertEqual(runs.flaky_kind(flips), "suspect")

    def test_mutation_runs_are_excluded(self):
        with tempfile.TemporaryDirectory() as tmp:
            for name, trig in (("2026-09-29T10-00-01Z-aaaa", "cli"), ("2026-09-29T10-00-02Z-bbbb", "mutation-round-1")):
                (Path(tmp) / f"{name}.json").write_text(json.dumps({"runId": name, "startedAt": "2026-09-29T10:00:00Z", "trigger": trig, "targets": [], "git": {}}))
            self.assertEqual(len(runs.load_records(Path(tmp), 10)), 1)
            self.assertEqual(len(runs.load_records(Path(tmp), 10, include_mutations=True)), 2)

    def test_compare(self):
        a = make_record("2026-09-29T10-00-01Z-aaaa", tests=[("simplebuilding:x", "failed"), ("simplebuilding:y", "passed")])
        b = make_record("2026-09-29T10-00-02Z-bbbb", tests=[("simplebuilding:x", "passed"), ("simplebuilding:y", "failed")])
        result = runs.compare(a, b)
        self.assertEqual([i["id"] for i in result["newlyFailing"]], ["simplebuilding:y"])
        self.assertEqual([i["id"] for i in result["newlyFixed"]], ["simplebuilding:x"])

    def test_prepush_needs_check_and_suite_for_exact_head(self):
        head = "b" * 40
        recs = [make_record("2026-09-29T10-00-05Z-dddd", commit=head, tests=[("simplebuilding:a", "passed")], target="fabric-263")]
        self.assertFalse(runs.prepush(head, False, recs, [])["ready"])  # only one of the two targets
        both = make_record("2026-09-29T10-00-06Z-eeee", commit=head, tests=[("simplebuilding:a", "passed")], target="fabric-263")
        both["targets"].append(dict(both["targets"][0], id="neoforge-263"))
        self.assertFalse(runs.prepush(head, False, [both], [])["ready"])  # no check yet
        check = [{"commit": head, "exitCode": 0, "at": "x"}]
        self.assertTrue(runs.prepush(head, False, [both], check)["ready"])
        self.assertFalse(runs.prepush(head, True, [both], check)["ready"])  # dirty tree
        self.assertFalse(runs.prepush("c" * 40, False, [both], check)["ready"])  # other commit
        self.assertFalse(runs.prepush(head, False, [both], [{"commit": head, "exitCode": 0, "dry": True}])["ready"])

    def test_log_excerpt(self):
        text = "noise\n\x1b[31mFAILED simplebuilding:foo_bar\x1b[0m\nat line1\nat line2\n"
        excerpt = runs.log_excerpt(text, "simplebuilding:foo_bar")
        self.assertIn("at line1", excerpt)
        self.assertNotIn("\x1b", excerpt)

    def test_sync_runs_copies_only_new_files(self):
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            (Path(a) / "r.json").write_text("{}")
            (Path(a) / "ignore.txt").write_text("x")
            self.assertEqual(runs.sync_runs(Path(a), Path(b), 0), ["r.json"])
            self.assertEqual(runs.sync_runs(Path(a), Path(b), 0), [])


class TargetTests(unittest.TestCase):
    def test_catalogue_matches_runner_ids(self):
        run = targets.runner()
        data = targets.load_launch()
        for entry in targets.loaders(data).values():
            self.assertIn(entry["testTarget"], run.BY_ID)
            if entry["clientTestTarget"]:
                self.assertIn(entry["clientTestTarget"], run.BY_ID)

    def test_launch_command_is_argv_list(self):
        entry = targets.find_loader("fabric-264")
        argv = targets.launch_command(entry, "client", Path("/ws"), program_args="--quickPlaySingleplayer W")
        self.assertIsInstance(argv, list)
        self.assertIn("-Pmc264=true", argv)
        self.assertIn(":mc26_4:fabric:runClient", argv)
        self.assertEqual(argv[-1], "--args=--quickPlaySingleplayer W")

    def test_forge_args_expand(self):
        argv = targets.launch_command(targets.find_loader("forge-262"), "server", Path("/ws"))
        self.assertIn("-Pforge_runs=true", argv)
        self.assertNotIn("@forge", argv)

    def test_unknown_ids_are_refused(self):
        with self.assertRaises(targets.TargetError):
            targets.find_loader("evil; rm -rf")
        with self.assertRaises(targets.TargetError):
            targets.validate_targets(["fabric-263", "nope"])
        with self.assertRaises(targets.TargetError):
            targets.launch_command(targets.find_loader("fabric-263"), "shell", Path("/ws"))

    def test_filter_validation(self):
        self.assertEqual(targets.validate_filter("simplebuilding:hammer_*"), "simplebuilding:hammer_*")
        self.assertIsNone(targets.validate_filter(""))
        for bad in ("a,b", "a b", "a;b", "$(x)", "a&b", "x" * 400):
            with self.assertRaises(targets.TargetError):
                targets.validate_filter(bad)

    def test_test_argv(self):
        argv = targets.test_argv(Path("/ws"), ["fabric-263", "neoforge-263"], "simplebuilding:x*")
        self.assertIn("fabric-263,neoforge-263", argv)
        self.assertIn("--filter=simplebuilding:x*", argv)

    def test_build_patterns_folds_only_when_exact(self):
        cat = [{"id": f"simplebuilding:hammer_game_test_{n}", "testClass": "HammerTests"} for n in ("a", "b", "c")]
        cat.append({"id": "simplebuilding:other_game_test_z", "testClass": "OtherTests"})
        ids = [c["id"] for c in cat[:3]]
        self.assertEqual(targets.build_patterns(ids, cat), ["simplebuilding:hammer_game_test_*"])
        two = ids[:2]
        self.assertEqual(targets.build_patterns(two, cat), two)  # wildcard would also hit the third
        for p in targets.build_patterns(ids + [cat[3]["id"]], cat):
            self.assertNotIn(",", p)


class PathSafetyTests(unittest.TestCase):
    def test_safe_join(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(paths.safe_join(tmp, "a", "b"), (Path(tmp) / "a" / "b").resolve())
            for bad in ("..", "/etc", "C:\\x", "a\x00b"):
                with self.assertRaises(ValueError):
                    paths.safe_join(tmp, bad)
            with self.assertRaises(ValueError):
                paths.safe_join(tmp, "a", "..", "..", "x")

    def test_is_within(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertTrue(paths.is_within(Path(tmp) / "x", tmp))
            self.assertFalse(paths.is_within(Path(tmp).parent, tmp))

    def test_host_header(self):
        for ok in ("127.0.0.1:8771", "localhost:8771", "[::1]:8771", "localhost"):
            self.assertTrue(host_ok(ok), ok)
        for bad in ("evil.com", "127.0.0.1.evil.com:80", "192.168.1.5:8771", "", None):
            self.assertFalse(host_ok(bad), bad)

    def test_world_path_stays_inside_workspace(self):
        with tempfile.TemporaryDirectory() as tmp:
            entry = targets.find_loader("neoforge-263")
            self.assertTrue(paths.is_within(targets.world_path(entry, Path(tmp)), tmp))


class AiTests(unittest.TestCase):
    def test_render_template_reports_unknown(self):
        text, unknown = ai.render_template("a {{x}} b {{y}}", {"x": "1"})
        self.assertEqual(text, "a 1 b {{y}}")
        self.assertEqual(unknown, ["y"])

    def test_shipped_template_placeholders_are_all_filled(self):
        row = {"target": "fabric-263", "id": "simplebuilding:t", "status": "failed", "runId": "r1", "message": "boom", "flaky": None}
        text, unknown = ai.build_prompt(ai.read_template(), [row], {}, {"worktree": "/w", "branch": "hub-fix/x", "head": "h" * 40, "runCommits": ["c" * 40]}, "hints", "rerun")
        self.assertEqual(unknown, [])
        for needle in ("boom", "AGENTS.md", "docs/HANDOFF.md", "hub-fix/x", "one pattern per call", "Push nothing", "26.3", "American English"):
            self.assertIn(needle, text)
        self.assertIn("older commit", text)

    def test_command_rendering_has_no_shell_and_substitutes_per_argument(self):
        import shutil
        exe = Path(sys.executable).name
        conf = {"template": f"{exe} -c {{prompt}} --wt {{worktree}}", "stdin": False}
        real = shutil.which
        try:
            shutil.which = lambda name: sys.executable if name == exe else None
            cmd = ai.render_command(conf, "hello; rm -rf /", "pf", "/wt dir", "br")
        finally:
            shutil.which = real
        self.assertEqual(cmd["argv"], [sys.executable, "-c", "hello; rm -rf /", "--wt", "/wt dir"])
        self.assertIsNone(cmd["stdin_text"])

    def test_missing_cli_is_a_clear_error(self):
        with self.assertRaises(ai.ProviderError) as ctx:
            ai.render_command({"template": "definitely-not-installed-cli -p", "install": "npm i x"}, "p", "f", "w", "b")
        self.assertIn("npm i x", str(ctx.exception))

    def test_branch_names(self):
        self.assertTrue(ai.valid_branch("hub-fix/20260929-1"))
        for bad in ("-x", "a..b", "a b", "x.lock", "a;b"):
            self.assertFalse(ai.valid_branch(bad), bad)


class SettingsAndWorktreeTests(unittest.TestCase):
    def test_settings_validation(self):
        base = settings.load()
        self.assertEqual(settings.merge(base, {"minFreeGb": 12})["minFreeGb"], 12)
        for bad in ({"minFreeGb": 0}, {"nope": 1}, {"testWorkspace": "x"}, {"dryRun": "yes"}, {"baseRef": "--evil"},
                    {"providers": {"claude": {"template": "a\nb"}}}, {"providers": {"gpt": {}}}):
            with self.assertRaises(settings.SettingsError):
                settings.merge(base, bad)

    def test_parse_porcelain(self):
        text = "worktree /a\nHEAD abc\nbranch refs/heads/master\n\nworktree /b\nHEAD def\ndetached\n"
        items = worktrees.parse_porcelain(text)
        self.assertEqual(items[0]["branch"], "master")
        self.assertTrue(items[1]["detached"])


if __name__ == "__main__":
    unittest.main()
