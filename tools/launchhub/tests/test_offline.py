"""Offline-Testlauf (hub/offline.py): parsing, lock and process identity, start command without a real start.
python -m unittest discover tools/launchhub/tests"""

import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

HUB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HUB))

from hub import offline, paths  # noqa: E402
from hub.api import Hub, HubError  # noqa: E402

MD = """# offline
- 2026-10-02 19:24  **Lauf gestartet** (offline, Profil all): 1d6a2ae6
- 2026-10-02 19:24  1d6a2ae6  check: **FEHLER** (C:\\x\\.ai-runs\\offline-logs\\1d6a2ae6-check.log)
- 2026-10-02 19:27  1d6a2ae6  kern-263: gruen 2500/2500
- 2026-10-02 19:32  1d6a2ae6  module-263: **ROT** -   ROT module-x-fabric-263 simpletweaks:claims_a |   NICHT gruen: 518/519 bestanden, 1 rot (C:\\x\\offline-logs\\1d6a2ae6-module-263.log)
- 2026-10-02 19:32  1d6a2ae6  linie-262: **ROT** -   FEHLER Fabric - MC 26.2: Gradle brach ab\r |   NICHT gruen: 0/0 bestanden, 0 rot (C:\\x\\1d6a2ae6-linie-262.log)
- 2026-10-02 19:32  1d6a2ae6  **VERDICT 26.3: RED**
- 2026-10-02 20:19  **Lauf gestartet** (online, Profil 263): 9b00b80a, deadbeef
- 2026-10-02 20:19  deadbeef  **FEHLER**: SHA nicht gefunden (vorher fetchen/committen)
- 2026-10-02 20:40  9b00b80a  kern-263: gruen 3/3
- 2026-10-02 20:41  9b00b80a  **VERDICT 26.3: GREEN ohne Forge (forge-263 online nachholen, ~10 min)**
"""

GATE = {"pid": 4242, "name": "pwsh.exe", "created": "2026-10-02T18:00:00.0000000Z",
        "commandLine": '"C:\\pwsh.exe" -NoProfile -File C:\\repo\\tools\\testrunner\\offline_gate.ps1 -Refs abc'}
OTHER = {"pid": 4242, "name": "python.exe", "created": "2026-10-02T18:00:00Z", "commandLine": "python -m http.server 8765"}


def json_doc(status="running", pid=4242):
    return json.dumps({"schemaVersion": 1, "runs": [
        {"id": "old", "started": "2026-10-01T10:00:00Z", "finished": "2026-10-01T11:00:00Z", "mode": "online",
         "profile": "all", "refs": ["a"], "pid": 1, "status": "done", "shas": []},
        {"id": "new", "started": "2026-10-02T10:00:00Z", "finished": None, "mode": "offline", "profile": "263",
         "refs": ["9b00b80a"], "pid": pid, "status": status, "shas": [
             {"ref": "9b00b80a", "sha": "9b00b80a", "error": None, "notes": [], "verdict": None, "groups": [
                 {"name": "check", "kind": "check", "ok": True, "log": "9b00b80a-check.log"},
                 {"name": "kern-263", "kind": "tests", "ok": False, "passed": 4, "total": 5, "failed": 1,
                  "red": ["ROT fabric-263 simplebuilding:x"], "log": "9b00b80a-kern-263.log"}]}]}]})


class ParsingTests(unittest.TestCase):
    def test_json_newest_first(self):
        runs = offline.parse_results_json(json_doc())
        self.assertEqual([r["id"] for r in runs], ["new", "old"])
        self.assertEqual(runs[0]["shas"][0]["groups"][1]["failed"], 1)

    def test_broken_json_is_empty(self):
        self.assertEqual(offline.parse_results_json("{nope"), [])
        self.assertEqual(offline.parse_results_json(None), [])
        self.assertEqual(offline.parse_results_json('{"runs": 3}'), [])

    def test_markdown(self):
        runs = offline.parse_results_md(MD)
        self.assertEqual(len(runs), 2)
        newest, oldest = runs
        self.assertEqual((newest["mode"], newest["profile"], newest["refs"]), ("online", "263", ["9b00b80a", "deadbeef"]))
        bad = next(s for s in newest["shas"] if s["sha"] == "deadbeef")
        self.assertEqual(bad["verdict"], "error")
        good = next(s for s in newest["shas"] if s["sha"] == "9b00b80a")
        self.assertEqual(good["verdict"], "green-no-forge")
        self.assertEqual(good["groups"][0]["passed"], 3)
        sha = oldest["shas"][0]
        self.assertEqual(sha["verdict"], "red")
        check, kern, mods, line262 = sha["groups"]
        self.assertEqual((line262["total"], line262["log"]), (0, "1d6a2ae6-linie-262.log"))  # stray CR mid-line
        self.assertEqual(line262["red"], ["FEHLER Fabric - MC 26.2: Gradle brach ab"])
        self.assertFalse(check["ok"])
        self.assertEqual(check["log"], "1d6a2ae6-check.log")
        self.assertTrue(kern["ok"])
        self.assertEqual((mods["passed"], mods["total"], mods["failed"]), (518, 519, 1))
        self.assertEqual(mods["red"], ["ROT module-x-fabric-263 simpletweaks:claims_a"])
        self.assertEqual(mods["log"], "1d6a2ae6-module-263.log")

    def test_process_key(self):
        own = offline.process_key(os.getpid())
        self.assertTrue(own and own.startswith(str(os.getpid())))
        self.assertEqual(offline.process_key(os.getpid()), own)  # stable for the same process
        self.assertIsNone(offline.process_key(4_000_000_000 // 4 * 4 - 4))

    def test_lock_text(self):
        self.assertEqual(offline.parse_lock("1234\r\n"), 1234)
        self.assertIsNone(offline.parse_lock(""))
        self.assertIsNone(offline.parse_lock("abc"))

    def test_identity(self):
        self.assertTrue(offline.is_gate_process(GATE))
        self.assertTrue(offline.is_gate_process(dict(GATE, commandLine="pwsh -File .ai-runs\\offline-gate.ps1")))
        self.assertFalse(offline.is_gate_process(OTHER))
        self.assertFalse(offline.is_gate_process(dict(GATE, commandLine="pwsh -File other.ps1")))
        self.assertFalse(offline.is_gate_process(None))

    def test_queue_refs(self):
        self.assertEqual(Hub._queue_refs("# head\nabc1234  # first\n\ndef5678, 0123abc\n"), ["abc1234", "def5678", "0123abc"])


class HubOfflineTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.runs = Path(self.tmp) / "ai-runs"
        self.runs.mkdir()
        self.env = mock.patch.dict(os.environ, {"SB_OFFLINE_RUNS_DIR": str(self.runs), "SB_HUB_DRY_RUN": "1"})
        self.env.start()
        self.key = mock.patch.object(offline, "process_key", return_value="4242-1")  # the fake PID "exists"
        self.key.start()
        self.addCleanup(self.key.stop)
        self.hub = Hub(logs_dir=Path(self.tmp) / "logs", data_dir=Path(self.tmp) / "data")
        self.head = self.hub.head()

    def tearDown(self):
        self.env.stop()
        shutil.rmtree(self.tmp, ignore_errors=True)

    def lock(self, pid=4242):
        (self.runs / "offline-gate.lock").write_text(f"{pid}\n", encoding="utf-8")

    def test_dir_follows_override(self):
        self.assertEqual(self.hub.offline_dir(), self.runs.resolve())

    def test_state_marks_dead_running_run_aborted(self):
        (self.runs / "offline-results.json").write_text(json_doc(), encoding="utf-8")
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=None):
            state = self.hub.offline_state()
        self.assertFalse(state["running"])
        self.assertTrue(state["lock"]["stale"])
        self.assertEqual(state["runs"][0]["status"], "aborted")
        self.assertEqual(state["source"], "json")

    def test_state_live_run_stays_running(self):
        (self.runs / "offline-results.json").write_text(json_doc(), encoding="utf-8")
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=GATE):
            state = self.hub.offline_state()
        self.assertTrue(state["running"])
        self.assertEqual(state["runs"][0]["status"], "running")

    def test_state_falls_back_to_markdown(self):
        (self.runs / "offline-results.md").write_text(MD, encoding="utf-8")
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=GATE):
            state = self.hub.offline_state()
        self.assertEqual(state["source"], "md")
        self.assertEqual(len(state["runs"]), 2)
        self.assertEqual(state["runs"][0]["status"], "done")  # finished runs stay finished
        (self.runs / "offline-results.md").write_text(MD + "- 2026-10-02 21:00  **Lauf gestartet** (offline, Profil all): abc\n", encoding="utf-8")
        self.hub._cache.clear()
        with mock.patch.object(offline, "process_info", return_value=GATE):
            self.assertEqual(self.hub.offline_state()["runs"][0]["status"], "running")
        self.assertTrue(state["master"])

    def test_pid_reuse_is_not_a_running_gate(self):
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=OTHER):
            self.assertFalse(self.hub.offline_lock()["alive"])

    def test_dry_run_start_builds_command_and_spawns_nothing(self):
        with mock.patch.object(offline, "spawn_detached", side_effect=AssertionError("started")):
            result = self.hub.offline_start({"refs": f"{self.head[:10]}, {self.head}", "profile": "263", "online": True})
        self.assertTrue(result["dryRun"])
        self.assertFalse(result["started"])
        argv = result["argv"]
        self.assertEqual(argv[argv.index("-File") + 1], str(self.hub.offline_script()))
        self.assertEqual(argv[argv.index("-Refs") + 1], self.head[:12])  # deduplicated after rev-parse
        self.assertEqual(argv[argv.index("-Profile") + 1], "263")
        self.assertIn("-Online", argv)
        self.assertEqual(result["refs"], [self.head])

    def test_offline_default_has_no_online_switch(self):
        result = self.hub.offline_start({"refs": [self.head], "profile": "all"})
        self.assertNotIn("-Online", result["argv"])

    def test_start_validation(self):
        for body in ({"refs": "deadbeefdeadbeef"}, {"refs": "--exec=calc"}, {"refs": ""}, {"refs": [3]},
                     {"refs": self.head, "profile": "x"}, {"refs": self.head, "online": "yes"},
                     {"refs": ",".join(["HEAD"] * 21)}):
            with self.subTest(body=body), self.assertRaises(HubError) as ctx:
                self.hub.offline_start(body)
            self.assertEqual(ctx.exception.status, 400)

    def test_double_start_refused(self):
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=GATE), self.assertRaises(HubError) as ctx:
            self.hub.offline_start({"refs": self.head})
        self.assertEqual(ctx.exception.status, 409)

    def test_queue_start_needs_entries_and_passes_no_refs(self):
        with self.assertRaises(HubError):
            self.hub.offline_start({"useQueue": True})
        (self.runs / "offline-queue.txt").write_text(f"# x\n{self.head[:8]}\n", encoding="utf-8")
        result = self.hub.offline_start({"useQueue": True, "profile": "all"})
        self.assertNotIn("-Refs", result["argv"])

    def test_stop_refuses_foreign_process(self):
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=OTHER), \
                mock.patch.object(subprocess, "run", side_effect=AssertionError("killed")), self.assertRaises(HubError) as ctx:
            self.hub.offline_stop({})
        self.assertEqual(ctx.exception.status, 409)

    def test_stop_dry_run_names_the_gate(self):
        self.lock()
        with mock.patch.object(offline, "process_info", return_value=GATE), \
                mock.patch.object(subprocess, "run", side_effect=AssertionError("killed")):
            result = self.hub.offline_stop({})
        self.assertEqual((result["dryRun"], result["pid"]), (True, 4242))

    def test_stop_real_checks_identity_again(self):
        self.lock()
        changed = dict(GATE, created="2026-10-02T19:00:00Z")
        with mock.patch.dict(os.environ, {"SB_HUB_DRY_RUN": ""}), \
                mock.patch.object(offline, "process_info", side_effect=[GATE, changed]), \
                mock.patch.object(subprocess, "run", side_effect=AssertionError("killed")), self.assertRaises(HubError):
            self.hub.offline_stop({})

    def test_stop_without_lock(self):
        with self.assertRaises(HubError) as ctx:
            self.hub.offline_stop({})
        self.assertEqual(ctx.exception.status, 409)

    def test_queue_save(self):
        with self.assertRaises(HubError):
            self.hub.offline_queue_save({"text": "nichtda123\n"})
        self.assertTrue(self.hub.offline_queue_save({"text": f"# a\n{self.head[:8]}\n"})["dryRun"])
        with mock.patch.dict(os.environ, {"SB_HUB_DRY_RUN": ""}), mock.patch("hub.settings.load", return_value={"dryRun": False}):
            self.assertTrue(self.hub.offline_queue_save({"text": f"{self.head[:8]}\r\n"})["saved"])
        self.assertEqual((self.runs / "offline-queue.txt").read_text(encoding="utf-8"), f"{self.head[:8]}\n")

    def test_log_reading_is_confined(self):
        (self.runs / "offline-logs").mkdir()
        (self.runs / "offline-logs" / "abc-check.log").write_text("hallo", encoding="utf-8")
        self.assertEqual(self.hub.offline_log({"name": ["abc-check.log"]}), "hallo")
        for name in ("../offline-gate.lock", "..\\x.log", "C:x.log", "x.txt", ""):
            with self.subTest(name=name), self.assertRaises(HubError):
                self.hub.offline_log({"name": [name]})

    def test_routes(self):
        self.assertEqual(self.hub.route_get("/api/offline", {})[0], "json")
        with self.assertRaises(HubError):
            self.hub.route_post("/api/offline/stop", {})


@unittest.skipUnless(shutil.which("pwsh"), "pwsh not installed")
class ScriptTests(unittest.TestCase):
    def test_script_parses(self):
        script = paths.REPO / "tools" / "testrunner" / "offline_gate.ps1"
        code = ("$e=$null; [void][System.Management.Automation.Language.Parser]::ParseFile('%s',[ref]$null,[ref]$e); $e.Count"
                % str(script).replace("'", "''"))
        done = subprocess.run(["pwsh", "-NoProfile", "-Command", code], capture_output=True, text=True, timeout=60)
        self.assertEqual(done.stdout.strip(), "0", done.stdout + done.stderr)


if __name__ == "__main__":
    unittest.main()
