"""Background jobs: start (argv lists only), follow (log tail), stop (whole process tree), remember.

A Job is a list of steps run one after another in a worker thread: either a subprocess (argv list,
never a shell string) or an internal callable (workspace preparation). All output goes to one log
file under tools/launchhub/logs/ and to an in-memory tail the UI polls with an offset.
"""

from __future__ import annotations

import json
import os
import signal
import subprocess
import sys
import threading
import time
import uuid
from collections import deque
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from . import settings

TAIL_LINES = 6000
KEEP_JOBS = 300
GRACE_SECONDS = 8

ACTIVE = ("starting", "running", "stopping")

#: What a simulated job runs in dry-run mode "sim": one line per second, proves live log and stop.
SIM_CODE = ("import sys,time\n"
            "print('[simulated] ' + sys.argv[1], flush=True)\n"
            "for i in range(60):\n"
            "    time.sleep(1); print('[simulated] tick', i, flush=True)\n")


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def display_argv(argv: list[str]) -> str:
    return " ".join(f'"{a}"' if (" " in a or not a) else a for a in argv)


class Job:
    def __init__(self, job_id: str, kind: str, label: str, steps: list[dict], meta: dict, log_path: Path,
                 keep_stdin: bool, dry: str):
        self.id = job_id
        self.kind = kind
        self.label = label
        self.steps = steps
        self.meta = meta
        self.log_path = log_path
        self.keep_stdin = keep_stdin
        self.dry = dry
        self.status = "starting"
        self.started_at = now_iso()
        self.started_ts = time.time()
        self.ended_at: str | None = None
        self.exit_code: int | None = None
        self.stop_requested = False
        self.proc: subprocess.Popen | None = None
        self.lines: deque[str] = deque(maxlen=TAIL_LINES)
        self.total = 0
        self.on_finish: Callable[["Job"], None] | None = None
        self._lock = threading.Lock()
        self._file = None

    # -- logging ------------------------------------------------------------
    def log(self, line: str) -> None:
        line = line.rstrip("\r\n")
        with self._lock:
            self.lines.append(line)
            self.total += 1
            if self._file:
                try:
                    self._file.write(line + "\n")
                except OSError:
                    pass

    def tail(self, after: int = 0) -> tuple[list[str], int]:
        with self._lock:
            first = self.total - len(self.lines)
            start = max(after, first)
            return list(self.lines)[start - first:], self.total

    def to_dict(self) -> dict:
        return {"id": self.id, "kind": self.kind, "label": self.label, "status": self.status,
                "startedAt": self.started_at, "endedAt": self.ended_at, "exitCode": self.exit_code,
                "dry": self.dry, "meta": self.meta, "lines": self.total, "logFile": self.log_path.name,
                "steps": [s.get("label") or display_argv(s.get("argv", [])) for s in self.steps],
                "canStdin": bool(self.keep_stdin and self.proc and self.status in ACTIVE)}


def kill_tree(proc: subprocess.Popen, force: bool) -> None:
    """Stops a process and its children. Windows: taskkill /T (/F), elsewhere the process group."""
    if proc.poll() is not None:
        return
    try:
        if os.name == "nt":
            args = ["taskkill", "/PID", str(proc.pid), "/T"] + (["/F"] if force else [])
            subprocess.run(args, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20, check=False)
        else:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL if force else signal.SIGTERM)
    except (OSError, subprocess.SubprocessError):
        try:
            proc.kill()
        except OSError:
            pass


class Manager:
    def __init__(self, logs_dir: Path):
        self.logs_dir = logs_dir
        self.jobs: dict[str, Job] = {}
        self.lock = threading.Lock()
        logs_dir.mkdir(parents=True, exist_ok=True)
        self._load_old()

    # -- persistence --------------------------------------------------------
    def _meta_path(self, job_id: str) -> Path:
        return self.logs_dir / f"{job_id}.meta.json"

    def _persist(self, job: Job) -> None:
        try:
            tmp = self._meta_path(job.id).with_suffix(".tmp")
            tmp.write_text(json.dumps(job.to_dict(), ensure_ascii=False), encoding="utf-8")
            os.replace(tmp, self._meta_path(job.id))
        except OSError:
            pass

    def _load_old(self) -> None:
        metas = sorted(self.logs_dir.glob("*.meta.json"), reverse=True)
        for path in metas[KEEP_JOBS:]:
            for extra in (path, path.with_name(path.name.replace(".meta.json", ".log"))):
                try:
                    extra.unlink()
                except OSError:
                    pass
        for path in metas[:KEEP_JOBS]:
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            job = Job(data["id"], data["kind"], data["label"], [], data.get("meta") or {},
                      self.logs_dir / data.get("logFile", data["id"] + ".log"), False, data.get("dry", ""))
            job.status = "lost" if data["status"] in ACTIVE else data["status"]
            job.started_at, job.ended_at, job.exit_code = data["startedAt"], data.get("endedAt"), data.get("exitCode")
            job.total = data.get("lines", 0)
            job.steps = [{"label": s} for s in data.get("steps", [])]
            job._old = True
            self.jobs[job.id] = job

    # -- queries ------------------------------------------------------------
    def get(self, job_id: str) -> Job | None:
        return self.jobs.get(job_id)

    def list(self) -> list[Job]:
        return sorted(self.jobs.values(), key=lambda j: j.started_at + j.id, reverse=True)

    def active(self, kind: str | None = None) -> list[Job]:
        return [j for j in self.jobs.values() if j.status in ACTIVE and (kind is None or j.kind == kind)]

    def active_where(self, **meta) -> list[Job]:
        return [j for j in self.active() if all(j.meta.get(k) == v for k, v in meta.items())]

    def read_full_log(self, job: Job, max_bytes: int = 8_000_000) -> str:
        try:
            with open(job.log_path, "rb") as handle:
                return handle.read(max_bytes).decode("utf-8", errors="replace")
        except OSError:
            return "\n".join(job.lines)

    # -- start --------------------------------------------------------------
    def start(self, kind: str, label: str, steps: list[dict], meta: dict | None = None,
              keep_stdin: bool = False, on_finish: Callable[[Job], None] | None = None) -> Job:
        job_id = f"{kind}-{datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')}-{uuid.uuid4().hex[:4]}"
        dry = settings.dry_run()
        job = Job(job_id, kind, label, steps, dict(meta or {}), self.logs_dir / f"{job_id}.log", keep_stdin, dry)
        job.on_finish = on_finish
        try:
            job._file = open(job.log_path, "w", encoding="utf-8", buffering=1)
        except OSError:
            job._file = None
        with self.lock:
            self.jobs[job.id] = job
        self._persist(job)
        threading.Thread(target=self._work, args=(job,), name=job.id, daemon=True).start()
        return job

    def _work(self, job: Job) -> None:
        code = 0
        try:
            if job.dry == "1":
                job.log("[dry run] nothing is started. The hub would run:")
                for step in job.steps:
                    if step.get("call"):
                        job.log(f"  (internal) {step.get('label', 'prepare workspace')}")
                    else:
                        job.log(f"  cwd  {step.get('cwd')}")
                        job.log(f"  argv {display_argv(step['argv'])}")
                        if step.get("env"):
                            job.log(f"  env  {json.dumps(step['env'])}")
                job.status = "running"
                time.sleep(0.2)
            else:
                for step in job.steps:
                    if job.stop_requested:
                        break
                    code = self._run_step(job, step)
                    if code != 0 and not step.get("continue"):
                        break
        except Exception as error:  # noqa: BLE001 - the worker must never die silently
            job.log(f"[hub] internal error: {type(error).__name__}: {error}")
            code = code or 1
        job.exit_code = code
        job.ended_at = now_iso()
        job.status = "stopped" if (job.stop_requested or code == 0) else "failed"
        job.log(f"[hub] finished, exit code {code}" + (" (stopped by user)" if job.stop_requested else "")
                + (" (dry run)" if job.dry else ""))
        if job.on_finish:
            try:
                job.on_finish(job)
            except Exception as error:  # noqa: BLE001
                job.log(f"[hub] post-processing failed: {type(error).__name__}: {error}")
        with job._lock:
            if job._file:
                job._file.close()
                job._file = None
        self._persist(job)

    def _run_step(self, job: Job, step: dict) -> int:
        if step.get("call"):
            job.log(f"[hub] {step.get('label', 'prepare')}")
            job.status = "running"
            if job.dry:
                job.log("[dry run] internal step skipped")
                return 0
            try:
                ok = step["call"](job.log)
            except Exception as error:  # noqa: BLE001
                job.log(f"[hub] step failed: {error}")
                return 1
            return 0 if ok in (None, True) else 1
        argv = list(step["argv"])
        if job.dry == "sim":
            argv = [sys.executable, "-u", "-c", SIM_CODE, step.get("label") or argv[0]]
        env = dict(os.environ)
        env.update(step.get("env") or {})
        job.log(f"$ {display_argv(step['argv'])}")
        job.log(f"  (cwd {step.get('cwd')})")
        stdin_text = step.get("stdin_text")
        flags = 0
        extra = {}
        if os.name == "nt":
            flags = subprocess.CREATE_NEW_PROCESS_GROUP | 0x08000000  # CREATE_NO_WINDOW
        else:
            extra["start_new_session"] = True
        try:
            proc = subprocess.Popen(
                argv, cwd=step.get("cwd"), env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                stdin=subprocess.PIPE if (stdin_text is not None or job.keep_stdin) else subprocess.DEVNULL,
                text=True, encoding="utf-8", errors="replace", creationflags=flags, **extra)
        except (OSError, ValueError) as error:
            job.log(f"[hub] could not start: {error}")
            return 127
        job.proc = proc
        job.status = "running" if job.status != "stopping" else "stopping"
        if stdin_text is not None:
            def feed() -> None:
                try:
                    proc.stdin.write(stdin_text)
                    proc.stdin.close()
                except (OSError, ValueError):
                    pass
            threading.Thread(target=feed, daemon=True).start()
        assert proc.stdout is not None
        for line in proc.stdout:
            job.log(line)
        return proc.wait()

    # -- control ------------------------------------------------------------
    def stop(self, job_id: str, force: bool = False) -> bool:
        job = self.jobs.get(job_id)
        if not job or job.status not in ACTIVE:
            return False
        job.stop_requested = True
        job.status = "stopping"
        proc = job.proc
        if proc is None:
            return True
        if force:
            job.log("[hub] killing the process tree")
            kill_tree(proc, True)
            return True
        job.log("[hub] asking the process tree to stop ...")
        kill_tree(proc, False)

        def escalate() -> None:
            if proc.poll() is None:
                job.log(f"[hub] still running after {GRACE_SECONDS} s - killing the process tree")
                kill_tree(proc, True)
        timer = threading.Timer(GRACE_SECONDS, escalate)
        timer.daemon = True
        timer.start()
        return True

    def graceful_stop(self, job_id: str, command: str = "stop") -> bool:
        """Servers: type the stop command on stdin, then fall back to a normal stop after a while."""
        job = self.jobs.get(job_id)
        if not job or job.status not in ACTIVE or not job.proc or not job.keep_stdin:
            return False
        try:
            job.proc.stdin.write(command + "\n")
            job.proc.stdin.flush()
        except (OSError, ValueError):
            return self.stop(job_id)
        job.log(f"[hub] sent '{command}' to the server console")
        job.stop_requested = True
        job.status = "stopping"

        def escalate() -> None:
            if job.proc and job.proc.poll() is None:
                job.log("[hub] server did not stop in 60 s - killing the process tree")
                kill_tree(job.proc, True)
        timer = threading.Timer(60, escalate)
        timer.daemon = True
        timer.start()
        return True

    def shutdown(self) -> None:
        """On hub exit: nothing is killed on purpose - clients started from the hub keep running."""
        return None
