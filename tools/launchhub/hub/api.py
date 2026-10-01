"""The hub's service layer: every endpoint is a method here, the HTTP layer (server.py) only routes.

GET handlers never start, stop or change anything. Every POST validates its input against
allow-lists (launch target ids, test target ids, records currently known) before an argv list is built.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from datetime import datetime, timezone
from pathlib import Path

from . import ai, paths, processes, runs, settings, targets, worktrees

RUN_ID_RE = re.compile(r"^[0-9A-Za-z\-]{10,60}$")
JOB_ID_RE = re.compile(r"^[a-z]+-\d{8}-\d{6}-[0-9a-f]{4}$")
WORKSPACES = ("repo", "gate")


class HubError(Exception):
    def __init__(self, status: int, message: str, **extra):
        super().__init__(message)
        self.status = status
        self.message = message
        self.extra = extra


def _need(body: dict, key: str, kind=str):
    value = body.get(key)
    if not isinstance(value, kind) or (kind is str and not value):
        raise HubError(400, f"missing or invalid field: {key}")
    return value


from .mods import ModsMixin


class Hub(ModsMixin):
    def __init__(self, repo: Path = paths.REPO, logs_dir: Path = paths.LOGS_DIR, data_dir: Path = paths.DATA_DIR):
        self.repo = repo
        self.data_dir = data_dir
        self.manager = processes.Manager(logs_dir)
        self._cache: dict[str, tuple[float, object]] = {}
        self._analysis_cache: tuple | None = None
        self._lock = threading.Lock()
        data_dir.mkdir(parents=True, exist_ok=True)

    # ------------------------------------------------------------------ helpers
    def cached(self, key: str, ttl: float, fn):
        with self._lock:
            hit = self._cache.get(key)
            if hit and time.time() - hit[0] < ttl:
                return hit[1]
        value = fn()
        with self._lock:
            self._cache[key] = (time.time(), value)
        return value

    def runs_dir(self) -> Path:
        return paths.runs_dir(self.repo)

    def workspace(self, name: str) -> Path:
        if name == "repo":
            return self.repo
        if name == "gate":
            return paths.gate_dir()
        raise HubError(400, "workspace must be 'repo' or 'gate'")

    def head(self) -> str:
        code, out = paths.git(["rev-parse", "HEAD"], self.repo)
        return out if code == 0 else ""

    def require_disk(self, *locations: Path) -> float:
        minimum = settings.load()["minFreeGb"]
        worst = min((paths.free_gb(p) for p in (locations or (self.repo,))), default=-1)
        if 0 <= worst < minimum:
            raise HubError(409, f"Only {worst} GB free; the hub refuses to start builds below {minimum} GB "
                                "(Gradle output, run directories and worktrees need several GB each). "
                                "Free space or lower 'minimum free disk' in Settings.", freeGb=worst, minFreeGb=minimum)
        return worst

    # ------------------------------------------------------------------ state
    def git_state(self) -> dict:
        def read():
            code, branch = paths.git(["rev-parse", "--abbrev-ref", "HEAD"], self.repo)
            head = self.head()
            code, dirty = paths.git(["status", "--porcelain"], self.repo, timeout=30)
            info = {"branch": branch, "head": head, "short": head[:8], "dirty": bool(dirty.strip()),
                    "dirtyCount": len([l for l in dirty.splitlines() if l.strip()]), "ahead": None, "behind": None,
                    "upstream": None, "isWorktree": self.repo != paths.main_repo_root(self.repo)}
            for upstream in ("@{upstream}", "origin/master"):
                code, out = paths.git(["rev-list", "--left-right", "--count", f"HEAD...{upstream}"], self.repo)
                if code == 0 and len(out.split()) == 2:
                    info["ahead"], info["behind"] = (int(x) for x in out.split())
                    info["upstream"] = "upstream" if upstream.startswith("@") else "origin/master"
                    break
            code, subject = paths.git(["log", "-1", "--format=%s"], self.repo)
            info["subject"] = subject
            return info
        return self.cached("git", 10, read)

    def state(self) -> dict:
        s = settings.load()
        active = self.manager.active()
        newest = runs.load_records(self.runs_dir(), 1, include_mutations=True)
        age = None
        if newest:
            moment = runs.parse_iso(newest[0]["startedAt"])
            age = int((datetime.now(timezone.utc) - moment).total_seconds()) if moment else None
        gate = paths.gate_dir()
        return {
            "repo": str(self.repo), "runsDir": str(self.runs_dir()), "gateDir": str(gate), "gateExists": gate.exists(),
            "disk": {"freeGb": paths.free_gb(self.repo), "gateFreeGb": paths.free_gb(gate), "minFreeGb": s["minFreeGb"]},
            "dryRun": settings.dry_run(), "git": self.git_state(),
            "processes": {"active": len(active), "clients": len([j for j in active if j.meta.get("action", "").startswith("client") and j.kind == "launch"]),
                          "servers": len([j for j in active if j.meta.get("action") == "server"]),
                          "tests": len([j for j in active if j.kind == "test"]),
                          "ai": len([j for j in active if j.kind == "ai"])},
            "lastRun": (runs.brief(newest[0]) | {"ageSeconds": age}) if newest else None,
            "settings": {k: s[k] for k in ("testWorkspace", "launchWorkspace", "defaultProvider", "includeMutations")},
            "now": processes.now_iso(),
        }

    def get_targets(self) -> dict:
        data = targets.load_launch()
        s = settings.load()
        lines = []
        for line in data["lines"]:
            entries = []
            for entry in line["loaders"]:
                entries.append(dict(entry, gradleArgs=[a for a in entry["gradleArgs"]],
                                    tasks={k: entry["prefix"] + v for k, v in data["tasks"].items()}))
            lines.append(dict(line, loaders=entries))
        return {"lines": lines, "worldName": data["worldName"], "serverPort": data["serverPort"],
                "testTargets": targets.test_targets(), "presets": targets.test_presets(),
                "defaults": {"testWorkspace": s["testWorkspace"], "launchWorkspace": s["launchWorkspace"]}}

    # ------------------------------------------------------------------ processes
    def list_processes(self) -> dict:
        return {"processes": [j.to_dict() for j in self.manager.list()]}

    def get_process(self, job_id: str, after: int = 0) -> dict:
        job = self._job(job_id)
        lines, total = job.tail(after)
        data = job.to_dict()
        data.update({"tail": lines, "next": total})
        if job.kind == "ai" and job.meta.get("promptFile"):
            data["hasPrompt"] = True
        return data

    def _job(self, job_id: str) -> processes.Job:
        if not JOB_ID_RE.match(job_id or ""):
            raise HubError(400, "invalid job id")
        job = self.manager.get(job_id)
        if not job:
            raise HubError(404, "unknown job")
        return job

    def job_log_text(self, job_id: str) -> str:
        return self.manager.read_full_log(self._job(job_id))

    def stop_process(self, body: dict) -> dict:
        job = self._job(_need(body, "id"))
        mode = body.get("mode", "stop")
        if mode not in ("stop", "kill", "graceful"):
            raise HubError(400, "mode must be stop, kill or graceful")
        if mode == "graceful":
            ok = self.manager.graceful_stop(job.id)
            if not ok:
                raise HubError(409, "no server console to talk to (not a running server started by the hub)")
        elif not self.manager.stop(job.id, force=(mode == "kill")):
            raise HubError(409, "the process is not running")
        return {"ok": True, "id": job.id}

    # ------------------------------------------------------------------ launch
    def _lock_warnings(self, workspace: str) -> list[str]:
        warnings = []
        for job in self.manager.active_where(workspace=workspace):
            young = time.time() - job.started_ts < 120
            if job.kind in ("test", "check", "ai") or (job.kind == "launch" and young):
                warnings.append(f"Another Gradle build is active in this workspace ({job.label}); "
                                "concurrent builds can wait on the Gradle lock.")
        return warnings

    def _dry_note(self, warnings: list[str]) -> list[str]:
        if settings.dry_run():
            warnings.insert(0, "Dry run: nothing is started.")
        return warnings

    def _gate_step(self, workspace: str) -> list[dict]:
        if workspace != "gate":
            return []
        head = self.head()
        gate = paths.gate_dir()
        return [{"label": f"prepare gate worktree at {head[:8]}",
                 "call": lambda log: worktrees.ensure_gate(self.repo, gate, head, log)}]

    def launch(self, body: dict) -> dict:
        data = targets.load_launch()
        entry = targets.find_loader(_need(body, "target"), data)
        action = _need(body, "action")
        if action not in ("client", "server", "client_fresh"):
            raise HubError(400, "action must be client, server or client_fresh")
        s = settings.load()
        workspace = body.get("workspace") or s["launchWorkspace"]
        ws = self.workspace(workspace)
        kind = "server" if action == "server" else "client"
        if self.manager.active_where(target=entry["id"], workspace=workspace, kind_of=kind):
            raise HubError(409, f"{entry['id']} {kind} already runs from this workspace (same run directory).")
        if kind == "server" and self.manager.active_where(kind_of="server"):
            raise HubError(409, f"A server is already running; all servers use port {data['serverPort']}. Stop it first.")
        self.require_disk(self.repo, ws)
        warnings = self._lock_warnings(workspace)
        world_mode = body.get("world", "rebuild")
        if world_mode not in ("rebuild", "recreate"):
            raise HubError(400, "world must be rebuild or recreate")
        if workspace == "gate" and not ws.exists() and not settings.dry_run():
            warnings.append("The gate worktree does not exist yet; it is created first.")

        steps = self._gate_step(workspace)
        program_args = None
        run_dir_rel = entry["runDir"]
        if action == "client_fresh":
            try:
                world = targets.world_path(entry, ws, data)
            except ValueError as error:
                raise HubError(400, str(error)) from error
            will_exist = world.exists() and world_mode == "rebuild"
            steps.append({"label": f"prepare world {data['worldName']} ({world_mode})",
                          "call": lambda log: self._prepare_world(world, world_mode, log, data["worldName"])})
            if will_exist and s["quickPlay"]:
                program_args = f"--quickPlaySingleplayer {data['worldName']}"
            elif not world.exists() or world_mode == "recreate":
                warnings.append(f"World '{data['worldName']}' will not exist: create it once (flat, creative, cheats on); "
                                "the test centre then builds itself on first join.")
        argv = targets.launch_command(entry, "server" if kind == "server" else "client", ws, data, program_args)
        if entry['id'] in ('fabric-263', 'neoforge-263', 'forge-263'):
            steps.append(self.selection_step(ws))
            argv.append('-Phub_mod_selection=true')
        steps.append({"label": f"{entry['id']} {kind}", "argv": argv, "cwd": str(ws)})
        label = {"client": "Client", "server": "Server", "client_fresh": "Client + fresh test centre"}[action]
        job = self.manager.start(
            "launch", f"{label} {entry['id']}", steps,
            meta={"target": entry["id"], "action": action, "kind_of": kind, "workspace": workspace,
                  "runDir": run_dir_rel, "line": entry["line"]},
            keep_stdin=(kind == "server"))
        return {"job": job.to_dict(), "warnings": self._dry_note(warnings)}

    @staticmethod
    def _prepare_world(world: Path, mode: str, log, world_name: str) -> bool:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        if not world.exists():
            log(f"[hub] world '{world_name}' does not exist yet - create it in the client: flat, creative, cheats on")
            return True
        marker = paths.safe_join(world, "simplebuilding_testcentre.txt")
        if mode == "rebuild":
            if marker.exists():
                marker.unlink()
                log("[hub] removed simplebuilding_testcentre.txt: the test centre rebuilds itself on the first join")
            else:
                log("[hub] no fingerprint file: the test centre builds itself on the first join")
            return True
        parking = paths.safe_join(world.parent.parent, "hub-old-worlds")
        parking.mkdir(exist_ok=True)
        target = parking / f"{world_name}-{stamp}"
        world.rename(target)
        log(f"[hub] moved the old world to {target} (delete it yourself when you no longer need it)")
        return True

    # ------------------------------------------------------------------ tests
    def _analysis(self) -> dict:
        """Records and rows of the newest state, recomputed only when the runs folder changed."""
        s = settings.load()
        rdir = self.runs_dir()
        try:
            names = sorted(n for n in os.listdir(rdir) if n.endswith(".json"))
        except OSError:
            names = []
        signature = (str(rdir), len(names), names[-1] if names else "", s["stateDepth"], s["includeMutations"])
        with self._lock:
            if self._analysis_cache and self._analysis_cache[0] == signature:
                return self._analysis_cache[1]
        records = runs.load_records(rdir, s["stateDepth"], s["includeMutations"])
        run = targets.runner()
        catalogue = self._catalogue()
        class_of = {e["id"]: e["testClass"] for entries in catalogue.values() for e in entries}
        known = {}
        for tid, target in run.BY_ID.items():
            if target.kind == "server":
                known[tid] = {e["id"] for e in catalogue.get(target.mc_line, [])}
        rows = runs.rows(records, class_of, known)
        result = {"records": records, "rows": rows, "class_of": class_of, "catalogue": catalogue,
                  "states": runs.latest_states(records)}
        with self._lock:
            self._analysis_cache = (signature, result)
        return result

    def _catalogue(self) -> dict:
        path = paths.REPO / "common" / "src" / "shared" / "java" / "com" / "simplebuilding" / "gametest" / "SimpleBuildingGameTests.java"
        mtime = path.stat().st_mtime if path.exists() else 0
        return self.cached(f"catalogue-{mtime}", 3600, lambda: targets.runner().read_catalogue())

    def _run_steps(self, ws: Path, groups: list[tuple[list[str], str | None]], env: dict | None) -> list[dict]:
        steps = []
        for ids, pattern in groups:
            argv = targets.test_argv(ws, ids, pattern)
            label = f"tests {','.join(ids)}" + (f" filter {pattern}" if pattern else "")
            steps.append({"label": label, "argv": argv, "cwd": str(ws), "env": env or {}, "continue": True})
        return steps

    def run_tests(self, body: dict) -> dict:
        s = settings.load()
        workspace = body.get("workspace") or s["testWorkspace"]
        ws = self.workspace(workspace)
        if workspace == "repo" and not body.get("force"):
            running = self.manager.active_where(workspace="repo", kind="launch")
            if running:
                raise HubError(409, "A client/server started from this repository is running. Building here while it runs "
                                    "can break the running game (NoClassDefFoundError, AGENTS.md rule 3). "
                                    "Use the gate worktree, or confirm to run here anyway.", needsForce=True)
        self.require_disk(self.repo, ws)
        analysis = None
        groups: list[tuple[list[str], str | None]] = []
        env: dict = {}
        label = "Tests"
        mode = body.get("mode", "targets")
        if mode == "targets":
            ids = targets.validate_targets(body.get("targets") if body.get("targets") else self._preset_ids(body.get("preset")))
            pattern = targets.validate_filter(body.get("filter"))
            client = [i for i in ids if i.startswith("client-")]
            if client and pattern:
                raise HubError(400, "a test filter means nothing for client suites (they are whole scenes)")
            if client:
                entries = targets.validate_client_entries(body.get("clientEntries"))
                if entries:
                    env["SIMPLEBUILDING_CLIENT_ONLY"] = ",".join(entries)
                if self.manager.active_where(kind="test", suite="client"):
                    raise HubError(409, "A client suite is already running; client suites run one at a time.")
            groups = [(ids, pattern)]
            label = f"Tests {','.join(ids)}" + (f" [{pattern}]" if pattern else "")
        elif mode in ("tests", "failed"):
            analysis = self._analysis()
            picked = self._pick_tests(analysis, body, failed_only=(mode == "failed"))
            groups = self._patterns_for(analysis, picked)
            if not groups:
                raise HubError(400, "nothing to run: no failing tests in the latest state" if mode == "failed" else "no tests selected")
            label = f"Re-run {len(picked)} test(s)"
        else:
            raise HubError(400, "mode must be targets, tests or failed")
        warnings = self._lock_warnings(workspace)
        client_suite = any(i.startswith("client-") for ids, _ in groups for i in ids)
        if client_suite:
            warnings.append("Client suites steer mouse and focus of their own Minecraft window; close owner clients first.")
        steps = self._gate_step(workspace) + self._run_steps(ws, groups, env)
        since = time.time() - 1
        job = self.manager.start(
            "test", label, steps,
            meta={"workspace": workspace, "suite": "client" if client_suite else "server", "targets": sorted({i for ids, _ in groups for i in ids}),
                  "patterns": [p for _, p in groups if p], "since": since},
            on_finish=lambda j: self._after_tests(j, ws, since))
        return {"job": job.to_dict(), "warnings": self._dry_note(warnings), "runsQueued": len(groups)}

    def _preset_ids(self, preset) -> list[str]:
        presets = targets.test_presets()
        if preset not in presets:
            raise HubError(400, "unknown preset")
        return presets[preset]["targets"]

    def _pick_tests(self, analysis: dict, body: dict, failed_only: bool) -> list[dict]:
        rows = analysis["rows"]
        index = {(r["target"], r["id"]): r for r in rows}
        wanted = body.get("tests")
        if failed_only and not wanted:
            limit = body.get("targets")
            limit = targets.validate_targets(limit) if limit else None
            return [r for r in rows if r["status"] == "failed" and (limit is None or r["target"] in limit)]
        if not isinstance(wanted, list) or not wanted:
            raise HubError(400, "tests must be a non-empty list of {target, id}")
        out = []
        for item in wanted[:400]:
            key = (item.get("target"), item.get("id")) if isinstance(item, dict) else (None, None)
            if key not in index:
                raise HubError(400, f"unknown test: {key[1]!r} on {key[0]!r}")
            out.append(index[key])
        return out

    def _patterns_for(self, analysis: dict, rows: list[dict]) -> list[tuple[list[str], str | None]]:
        """Sequential single-pattern runs; targets that need the same pattern share one run.py call."""
        run = targets.runner()
        per_target: dict[str, list[str]] = {}
        for row in rows:
            per_target.setdefault(row["target"], []).append(row["id"])
        by_pattern: dict[str, list[str]] = {}
        clients: list[str] = []
        for tid, ids in per_target.items():
            if tid.startswith("client-"):
                clients.append(tid)
                continue
            entries = analysis["catalogue"].get(run.BY_ID[tid].mc_line, [])
            for pattern in targets.build_patterns(ids, entries):
                by_pattern.setdefault(pattern, []).append(tid)
        groups: list[tuple[list[str], str | None]] = [(tids, pattern) for pattern, tids in by_pattern.items()]
        if clients:
            groups.append((clients, None))
        return groups

    def _after_tests(self, job: processes.Job, ws: Path, since: float) -> None:
        if job.dry:
            job.meta["verdict"] = "dry"
            return
        rdir = self.runs_dir()
        copied = runs.sync_runs(ws / "testing" / "runs", rdir, since)
        if copied:
            job.log(f"[hub] copied {len(copied)} record file(s) from {ws} into {rdir}")
        ids = []
        try:
            for name in sorted(os.listdir(rdir)):
                if name.endswith(".json") and (rdir / name).stat().st_mtime >= since:
                    ids.append(name[:-5])
        except OSError:
            pass
        verdicts = []
        for rid in ids:
            record = runs.load_one(rdir, rid)
            if record and record["trigger"] == "hub":
                verdicts.append(runs.verdict(record))
        job.meta["runs"] = ids
        # The runner exits 0 even when tests are red: only the records decide.
        job.meta["verdict"] = "none" if not verdicts else ("green" if all(v == "green" for v in verdicts) else "red")
        job.log(f"[hub] verdict from the record(s): {job.meta['verdict']}" + (" (alles gruen)" if job.meta["verdict"] == "green" else ""))

    # ------------------------------------------------------------------ check (Gradle)
    def run_check(self, body: dict) -> dict:
        s = settings.load()
        workspace = body.get("workspace") or s["testWorkspace"]
        ws = self.workspace(workspace)
        if workspace == "repo" and not body.get("force") and self.manager.active_where(workspace="repo", kind="launch"):
            raise HubError(409, "A client/server started from this repository is running - use the gate worktree, "
                                "or confirm to run here anyway.", needsForce=True)
        self.require_disk(self.repo, ws)
        head = self.head()
        steps = self._gate_step(workspace)
        steps.append({"label": "gradlew check", "argv": targets.check_argv(ws), "cwd": str(ws)})
        job = self.manager.start("check", "Gradle check", steps, meta={"workspace": workspace, "commit": head},
                                 on_finish=lambda j: self._after_check(j, head))
        return {"job": job.to_dict(), "warnings": self._dry_note(self._lock_warnings(workspace))}

    def _checks_file(self) -> Path:
        return self.data_dir / "checks.json"

    def read_checks(self) -> list[dict]:
        try:
            data = json.loads(self._checks_file().read_text(encoding="utf-8"))
            return data if isinstance(data, list) else []
        except (OSError, json.JSONDecodeError):
            return []

    def _after_check(self, job: processes.Job, head: str) -> None:
        entry = {"commit": head, "exitCode": job.exit_code, "at": processes.now_iso(), "jobId": job.id,
                 "workspace": job.meta.get("workspace"), "dry": bool(job.dry)}
        checks = (self.read_checks() + [entry])[-60:]
        tmp = self._checks_file().with_suffix(".tmp")
        tmp.write_text(json.dumps(checks, indent=1), encoding="utf-8")
        os.replace(tmp, self._checks_file())
        job.meta["verdict"] = "dry" if job.dry else ("green" if job.exit_code == 0 else "red")

    def prepush(self) -> dict:
        g = self.git_state()
        s = settings.load()
        records = runs.load_records(self.runs_dir(), s["stateDepth"], include_mutations=False)
        result = runs.prepush(g["head"], g["dirty"], records, self.read_checks())
        result["git"] = {k: g[k] for k in ("branch", "short", "dirty", "ahead", "behind", "upstream")}
        return result

    # ------------------------------------------------------------------ run data
    def _limit(self, query: dict, default: int, maximum: int) -> int:
        try:
            return max(1, min(maximum, int(query.get("limit", [default])[0])))
        except (TypeError, ValueError):
            return default

    def list_runs(self, query: dict) -> dict:
        s = settings.load()
        include = query.get("mutations", ["0"])[0] == "1" or s["includeMutations"]
        records = runs.load_records(self.runs_dir(), self._limit(query, 60, 300), include)
        return {"runs": [runs.brief(r) for r in records], "runsDir": str(self.runs_dir())}

    def get_run(self, run_id: str) -> dict:
        if not RUN_ID_RE.match(run_id):
            raise HubError(400, "invalid run id")
        record = runs.load_one(self.runs_dir(), run_id)
        if not record:
            raise HubError(404, "unknown run")
        return {"brief": runs.brief(record), "record": record}

    def overview(self) -> dict:
        a = self._analysis()
        s = settings.load()
        run = targets.runner()
        records = a["records"]
        by_target: dict[str, dict] = {}
        for row in a["rows"]:
            t = by_target.setdefault(row["target"], {"passed": 0, "failed": 0, "flaky": 0})
            t["failed" if row["status"] == "failed" else "passed"] += 1
            if row["flaky"]:
                t["flaky"] += 1
        dur = runs.durations(records)
        errors = {e["target"]: e for e in runs.target_errors(records)}
        last_seen: dict[str, dict] = {}
        for record in records:
            for t in runs.selected_targets(record):
                last_seen.setdefault(t["id"], {"runId": record["runId"], "at": record["startedAt"],
                                               "commit": (record["git"] or {}).get("short"), "filter": record["filter"]})
        out = []
        for target in run.ALL_TARGETS:
            if target.id not in by_target and target.id not in last_seen:
                continue
            counts = by_target.get(target.id, {"passed": 0, "failed": 0, "flaky": 0})
            catalogue = a["catalogue"].get(target.mc_line, [])
            out.append({"id": target.id, "label": target.label, "kind": target.kind, "line": target.mc_line,
                        "known": counts["passed"] + counts["failed"], "expected": len(catalogue) if target.kind == "server" else None,
                        **counts, "duration": dur.get(target.id), "lastRun": last_seen.get(target.id),
                        "error": errors.get(target.id)})
        return {"targets": out, "trend": runs.trend(records, s["historyDepth"]), "latest": runs.brief(records[0]) if records else None,
                "failing": len([r for r in a["rows"] if r["status"] == "failed"]),
                "flaky": len([r for r in a["rows"] if r["flaky"]]), "targetErrors": list(errors.values()),
                "runsDir": str(self.runs_dir()), "recordCount": len(records)}

    def tests(self, query: dict) -> dict:
        a = self._analysis()
        target = query.get("target", [""])[0]
        text = query.get("q", [""])[0].lower().strip()
        failed_only = query.get("failed", ["0"])[0] == "1"
        flaky_only = query.get("flaky", ["0"])[0] == "1"
        rows = a["rows"]
        if target:
            rows = [r for r in rows if r["target"] == target]
        if failed_only:
            rows = [r for r in rows if r["status"] == "failed"]
        if flaky_only:
            rows = [r for r in rows if r["flaky"]]
        if text:
            rows = [r for r in rows if text in r["id"].lower() or text in r["class"].lower() or text in (r["message"] or "").lower()]
        rows = sorted(rows, key=lambda r: (r["status"] != "failed", r["class"], r["id"], r["target"]))
        total = len(rows)
        offset = max(0, int(query.get("offset", ["0"])[0] or 0))
        limit = self._limit(query, 300, 1000)
        page = rows[offset: offset + limit]
        return {"total": total, "offset": offset, "rows": page}

    def failures(self) -> dict:
        a = self._analysis()
        rows = [r for r in a["rows"] if r["status"] == "failed"]
        rows.sort(key=lambda r: (r["target"], r["class"], r["id"]))
        return {"rows": rows, "targetErrors": runs.target_errors(a["records"]),
                "latestRunId": a["records"][0]["runId"] if a["records"] else None}

    def test_history(self, query: dict) -> dict:
        target = query.get("target", [""])[0]
        test_id = query.get("id", [""])[0]
        a = self._analysis()
        if target not in targets.runner().BY_ID or not test_id:
            raise HubError(400, "target and id are required")
        seq = runs.history(a["records"], target, test_id)
        return {"target": target, "id": test_id, "history": seq[-60:], "flaky": runs.flaky_kind(seq),
                "firstFailed": runs.first_failed(seq)}

    def compare(self, query: dict) -> dict:
        ida, idb = query.get("a", [""])[0], query.get("b", [""])[0]
        if not (RUN_ID_RE.match(ida) and RUN_ID_RE.match(idb)):
            raise HubError(400, "a and b must be run ids")
        ra, rb = runs.load_one(self.runs_dir(), ida), runs.load_one(self.runs_dir(), idb)
        if not ra or not rb:
            raise HubError(404, "unknown run")
        if (ra["startedAt"] or "") > (rb["startedAt"] or ""):
            ra, rb = rb, ra
        return runs.compare(ra, rb)

    def _log_excerpt_for(self, row: dict) -> str:
        record = runs.load_one(self.runs_dir(), row["runId"])
        if not record:
            return ""
        target = next((t for t in record["targets"] if t["id"] == row["target"]), None)
        if not target:
            return ""
        text = runs.read_log(self.runs_dir().parent, target.get("log"))
        return runs.log_excerpt(text, row["id"]) if text else ""

    def failure_markdown(self, query: dict) -> str:
        a = self._analysis()
        key = (query.get("target", [""])[0], query.get("id", [""])[0])
        row = next((r for r in a["rows"] if (r["target"], r["id"]) == key), None)
        if not row:
            raise HubError(404, "unknown test")
        return runs.failure_markdown(row, self._log_excerpt_for(row))

    def summary_markdown(self, query: dict) -> str:
        run_id = query.get("run", [""])[0]
        if run_id:
            if not RUN_ID_RE.match(run_id):
                raise HubError(400, "invalid run id")
            record = runs.load_one(self.runs_dir(), run_id)
        else:
            found = runs.load_records(self.runs_dir(), 1, settings.load()["includeMutations"])
            record = found[0] if found else None
        if not record:
            raise HubError(404, "no run")
        return runs.summary_markdown(record)

    # ------------------------------------------------------------------ AI
    def providers(self) -> dict:
        s = settings.load()
        return {"providers": [ai.provider_state(pid, conf) for pid, conf in s["providers"].items()],
                "default": s["defaultProvider"]}

    def _ai_rows(self, body: dict) -> list[dict]:
        analysis = self._analysis()
        index = {(r["target"], r["id"]): r for r in analysis["rows"]}
        wanted = body.get("tests")
        if wanted in (None, []):
            rows = [r for r in analysis["rows"] if r["status"] == "failed"]
        else:
            if not isinstance(wanted, list):
                raise HubError(400, "tests must be a list of {target, id}")
            rows = []
            for item in wanted[:60]:
                key = (item.get("target"), item.get("id")) if isinstance(item, dict) else (None, None)
                if key not in index:
                    raise HubError(400, f"unknown test: {key[1]!r} on {key[0]!r}")
                rows.append(index[key])
        if not rows:
            raise HubError(400, "no failing tests to fix")
        return rows[:60]

    def _ai_build(self, body: dict, branch: str, worktree: str) -> dict:
        rows = self._ai_rows(body)
        analysis = self._analysis()
        run = targets.runner()
        excerpts = {}
        for row in rows[:25]:
            if row["status"] == "failed":
                excerpts[(row["target"], row["id"])] = self._log_excerpt_for(row)
        commits = []
        for rid in sorted({r["runId"] for r in rows}):
            rec = runs.load_one(self.runs_dir(), rid)
            if rec and rec["git"].get("commit"):
                commits.append(rec["git"]["commit"])
        line_of = {tid: t.mc_line for tid, t in run.BY_ID.items()}
        hints = ai.file_hints(self.repo, rows, analysis["catalogue"])
        rerun = ai.rerun_commands(rows, analysis["catalogue"], line_of)
        extra = str(body.get("extra") or "")[:4000]
        text, unknown = ai.build_prompt(ai.read_template(), rows, excerpts,
                                        {"worktree": worktree, "branch": branch, "head": self.head(), "runCommits": commits},
                                        hints, rerun, extra)
        return {"prompt": text, "unknown": unknown, "rows": rows}

    def ai_preview(self, body: dict) -> dict:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        branch, root = ai.ai_paths(self.repo, stamp)
        built = self._ai_build(body, branch, str(root))
        provider = body.get("provider") or settings.load()["defaultProvider"]
        s = settings.load()
        if provider not in s["providers"]:
            raise HubError(400, "unknown provider")
        state = ai.provider_state(provider, s["providers"][provider])
        return {"prompt": built["prompt"], "unknownPlaceholders": built["unknown"], "tests": len(built["rows"]),
                "provider": state, "chars": len(built["prompt"]), "branch": branch, "worktree": str(root),
                "templateFile": str(ai.TEMPLATE_FILE.relative_to(paths.REPO).as_posix())}

    def ai_start(self, body: dict) -> dict:
        s = settings.load()
        provider = body.get("provider") or s["defaultProvider"]
        if provider not in s["providers"]:
            raise HubError(400, "unknown provider")
        if not body.get("confirm"):
            raise HubError(400, "starting an AI job needs confirm=true (use Preview prompt first)")
        conf = s["providers"][provider]
        state = ai.provider_state(provider, conf)
        if not state["installed"] and not settings.dry_run():
            raise HubError(400, f"{state['label']} is not installed / not on PATH. Install: {state['install']}", provider=state)
        main = paths.main_repo_root(self.repo)
        self.require_disk(self.repo, main)
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        branch, root = ai.ai_paths(self.repo, stamp)
        if not ai.valid_branch(branch):
            raise HubError(500, "internal: invalid branch name")
        built = self._ai_build(body, branch, str(root))
        prompt_dir = paths.LOGS_DIR / "ai"
        prompt_dir.mkdir(parents=True, exist_ok=True)
        prompt_file = prompt_dir / f"{stamp}-prompt.md"
        prompt_file.write_text(built["prompt"], encoding="utf-8")
        base = self.head()
        try:
            command = ai.render_command(conf, built["prompt"], str(prompt_file), str(root), branch)
        except ai.ProviderError as error:
            if not settings.dry_run():
                raise HubError(400, str(error)) from error
            command = {"argv": [conf["template"].split()[0], "...(dry run)"], "stdin_text": None}
        steps = [
            {"label": f"create worktree {root.name} on {branch}",
             "call": lambda log: worktrees.create_worktree(self.repo, root, branch, base, log)},
            {"label": f"{state['label']} on {len(built['rows'])} failing test(s)", "argv": command["argv"],
             "cwd": str(root), "stdin_text": command["stdin_text"]},
        ]
        job = self.manager.start(
            "ai", f"AI fix ({provider}): {len(built['rows'])} test(s)", steps,
            meta={"provider": provider, "branch": branch, "worktree": str(root), "base": base, "promptFile": prompt_file.name,
                  "tests": [{"target": r["target"], "id": r["id"]} for r in built["rows"]]},
            on_finish=lambda j: self._after_ai(j, root, base))
        return {"job": job.to_dict(), "warnings": self._dry_note([])}

    def _after_ai(self, job: processes.Job, root: Path, base: str) -> None:
        if job.dry or not root.is_dir():
            return
        job.meta["result"] = ai.collect_result(root, base)
        result = job.meta["result"]
        job.log(f"[hub] branch {result['branch']}: {result['commits']} commit(s), {len(result['changed'])} changed file(s), "
                f"head {result['short']}. Nothing was merged or pushed.")

    def ai_job_files(self, job_id: str, what: str) -> dict:
        job = self._job(job_id)
        if job.kind != "ai":
            raise HubError(400, "not an AI job")
        if what == "prompt":
            name = job.meta.get("promptFile") or ""
            path = paths.safe_join(paths.LOGS_DIR / "ai", name) if name else None
            if not path or not path.is_file():
                raise HubError(404, "prompt file is gone")
            return {"text": path.read_text(encoding="utf-8")}
        worktree = Path(job.meta.get("worktree", ""))
        if not worktree.is_dir() or not paths.is_within(worktree, paths.main_repo_root(self.repo) / ".claude" / "worktrees"):
            raise HubError(404, "the job's worktree is gone")
        if what == "diff":
            return ai.diff_text(worktree, job.meta["base"])
        if what == "result":
            return ai.collect_result(worktree, job.meta["base"])
        raise HubError(400, "unknown file")

    # ------------------------------------------------------------------ worktrees
    def list_worktrees(self) -> dict:
        items = worktrees.agent_worktrees(self.repo)
        return {"worktrees": items, "root": str(paths.main_repo_root(self.repo) / ".claude" / "worktrees"),
                "base": worktrees.base_ref(self.repo)}

    def delete_worktree(self, body: dict) -> dict:
        path = _need(body, "path")
        _wt, reason = worktrees.removal_check(self.repo, path)
        if reason:
            raise HubError(400, reason)
        if settings.dry_run():
            return {"dryRun": True, "message": f"[dry run] would remove {path}"}
        try:
            return worktrees.remove(self.repo, path, bool(body.get("confirm")), bool(body.get("confirmUnmerged")))
        except PermissionError as error:
            raise HubError(409, str(error), needsSecondConfirm=True) from error
        except ValueError as error:
            raise HubError(400, str(error)) from error
        except RuntimeError as error:
            raise HubError(500, str(error)) from error

    # ------------------------------------------------------------------ settings, open
    def get_settings(self) -> dict:
        s = settings.load()
        return {"settings": s, "providers": self.providers()["providers"], "envDryRun": bool(os.environ.get("SB_HUB_DRY_RUN")),
                "file": str(settings.SETTINGS_FILE.relative_to(paths.REPO).as_posix()),
                "template": ai.read_template(), "templateFile": str(ai.TEMPLATE_FILE.relative_to(paths.REPO).as_posix())}

    def save_settings(self, body: dict) -> dict:
        patch = body.get("settings")
        if not isinstance(patch, dict):
            raise HubError(400, "settings object required")
        try:
            settings.save(patch)
        except settings.SettingsError as error:
            raise HubError(400, str(error)) from error
        self._analysis_cache = None
        return self.get_settings()

    def open_path(self, body: dict) -> dict:
        what = _need(body, "what")
        path: Path | None = None
        if what == "logs":
            path = paths.LOGS_DIR
        elif what == "runs":
            path = self.runs_dir()
        elif what == "job-log":
            path = self._job(_need(body, "id")).log_path
        elif what == "run-dir":
            entry = targets.find_loader(_need(body, "target"))
            workspace = body.get("workspace") or "repo"
            path = paths.safe_join(self.workspace(workspace), *entry["runDir"].split("/"))
        elif what == "worktree":
            wanted = _need(body, "path")
            known = [Path(w["path"]) for w in worktrees.agent_worktrees(self.repo)]
            path = next((p for p in known if p.resolve() == Path(wanted).resolve()), None)
        if path is None:
            raise HubError(400, "unknown location")
        if settings.dry_run():
            return {"dryRun": True, "opened": str(path)}
        if not path.exists():
            raise HubError(404, f"{path} does not exist")
        try:
            if os.name == "nt":
                if path.is_file():
                    subprocess.Popen(["explorer", "/select,", str(path)])
                else:
                    os.startfile(str(path))  # noqa: S606 - local dev tool, path validated above
            elif sys.platform == "darwin":
                subprocess.Popen(["open", str(path)])
            else:
                subprocess.Popen(["xdg-open", str(path)])
        except OSError as error:
            raise HubError(500, f"could not open: {error}") from error
        return {"opened": str(path)}

    def prepare_gate(self) -> dict:
        head = self.head()
        gate = paths.gate_dir()
        job = self.manager.start("gate", "Prepare gate worktree", [
            {"label": f"gate worktree at {head[:8]}", "call": lambda log: worktrees.ensure_gate(self.repo, gate, head, log)}],
            meta={"workspace": "gate"})
        return {"job": job.to_dict()}

    # ------------------------------------------------------------------ routing tables
    def route_get(self, path: str, query: dict):
        """(kind, payload): kind is 'json' or 'text'. Raises HubError(404) for unknown paths."""
        simple = {
            "/api/mods": self.mods_state, "/api/state": self.state, "/api/targets": self.get_targets, "/api/processes": self.list_processes,
            "/api/overview": self.overview, "/api/failures": self.failures, "/api/prepush": self.prepush,
            "/api/worktrees": self.list_worktrees, "/api/settings": self.get_settings, "/api/providers": self.providers,
        }
        if path in simple:
            return "json", simple[path]()
        if path == "/api/runs":
            return "json", self.list_runs(query)
        if path.startswith("/api/run/"):
            return "json", self.get_run(path.rsplit("/", 1)[1])
        if path == "/api/tests":
            return "json", self.tests(query)
        if path == "/api/test/history":
            return "json", self.test_history(query)
        if path == "/api/compare":
            return "json", self.compare(query)
        if path == "/api/summary.md":
            return "text", self.summary_markdown(query)
        if path == "/api/failure.md":
            return "text", self.failure_markdown(query)
        if path.startswith("/api/process/"):
            parts = path.split("/")
            if len(parts) == 4:
                try:
                    after = int(query.get("after", ["0"])[0])
                except ValueError:
                    after = 0
                return "json", self.get_process(parts[3], after)
            if len(parts) == 5 and parts[4] == "log":
                return "text", self.job_log_text(parts[3])
        if path.startswith("/api/ai/job/"):
            parts = path.split("/")
            if len(parts) == 6:
                return "json", self.ai_job_files(parts[4], parts[5])
        raise HubError(404, f"not found: {path}")

    def route_post(self, path: str, body: dict):
        table = {
            "/api/mods": self.save_mods, "/api/integration/launch": self.launch_integration,
            "/api/launch": self.launch, "/api/tests/run": self.run_tests, "/api/check/run": self.run_check,
            "/api/process/stop": self.stop_process, "/api/ai/preview": self.ai_preview, "/api/ai/start": self.ai_start,
            "/api/worktrees/delete": self.delete_worktree, "/api/settings": self.save_settings,
            "/api/open": self.open_path, "/api/gate/prepare": lambda _b: self.prepare_gate(),
        }
        handler = table.get(path)
        if not handler:
            raise HubError(404, f"not found: {path}")
        return handler(body)
