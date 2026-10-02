"""Offline-Testlauf: drives tools/testrunner/offline_gate.ps1 (unattended gate for one or more SHAs).

The script runs as its own detached process (survives a hub restart and a missing network). The hub only
reads its files under <main checkout>/.ai-runs (SB_OFFLINE_RUNS_DIR overrides), starts it after validating
every ref with git, and stops it only when the PID in the lock really belongs to an offline_gate script.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import time
from pathlib import Path

from . import paths, settings

SCRIPT = ("tools", "testrunner", "offline_gate.ps1")
SCRIPT_RE = re.compile(r"offline[_-]gate\.ps1", re.IGNORECASE)
SHELL_RE = re.compile(r"^(pwsh|powershell)\.exe$", re.IGNORECASE)
LOG_NAME_RE = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._-]{0,120}\.log$")
PROFILES = ("263", "all")
MAX_REFS = 20
MAX_QUEUE_BYTES = 8000
MD_LINE_RE = re.compile(r"^- (\d{4}-\d\d-\d\d \d\d:\d\d)  (.*)$")


def runs_dir(repo: Path) -> Path:
    override = paths.env_flag("SB_OFFLINE_RUNS_DIR")
    return Path(override).resolve() if override else paths.main_repo_root(repo) / ".ai-runs"


def _read(path: Path) -> str | None:
    try:
        with open(path, encoding="utf-8-sig", errors="replace", newline="") as handle:  # keep stray CRs (md parser)
            return handle.read()
    except OSError:
        return None


# ---------------------------------------------------------------------------------------- parsing
def parse_results_json(text: str | None) -> list[dict]:
    """Runs from offline-results.json, newest first. Broken or missing file -> []."""
    if not text:
        return []
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        return []
    runs = data.get("runs") if isinstance(data, dict) else None
    if not isinstance(runs, list):
        return []
    out = []
    for run in runs:
        if not isinstance(run, dict):
            continue
        shas = [s for s in (run.get("shas") or []) if isinstance(s, dict)]
        for sha in shas:
            sha["groups"] = [g for g in (sha.get("groups") or []) if isinstance(g, dict)]
        out.append(dict(run, shas=shas, source="json"))
    return list(reversed(out))


def parse_results_md(text: str | None) -> list[dict]:
    """Best-effort reading of offline-results.md (older runs written before the JSON existed), newest first."""
    runs: list[dict] = []
    current = None
    for raw in (text or "").split("\n"):  # the old script left stray CRs mid-line, so no splitlines()
        raw = raw.replace("\r", "")
        m = MD_LINE_RE.match(raw.strip())
        if not m:
            continue
        stamp, rest = m.groups()
        start = re.match(r"\*\*Lauf gestartet\*\* \((\w+), Profil (\w+)\): (.*)$", rest)
        if start:
            current = {"id": "md-" + stamp, "started": stamp, "finished": None, "mode": start[1], "profile": start[2],
                       "refs": [r.strip() for r in start[3].split(",") if r.strip()], "pid": None, "status": "unknown",
                       "shas": [], "source": "md"}
            runs.append(current)
            continue
        if current is None:
            continue
        parts = rest.split("  ", 1)
        if len(parts) != 2:
            continue
        sha, what = parts[0].strip(), parts[1].strip()
        entry = next((s for s in current["shas"] if s["sha"] == sha), None)
        if entry is None:
            entry = {"ref": sha, "sha": sha, "error": None, "notes": [], "groups": [], "verdict": None, "verdictText": None}
            current["shas"].append(entry)
        log = re.search(r"\(([^()]*\.log)\)\s*$", what)
        log_name = Path(log[1].replace("\\", "/")).name if log else None
        if what.startswith("**VERDICT"):
            v = re.match(r"\*\*VERDICT 26\.3: (.*)\*\*$", what)
            entry["verdictText"] = v[1] if v else what
            text_v = entry["verdictText"] or ""
            entry["verdict"] = "red" if text_v.startswith("RED") else "green-no-forge" if "ohne Forge" in text_v else "green"
            current["finished"] = stamp
            current["status"] = "done"
        elif what.startswith("**FEHLER**"):
            entry["error"] = what.split(":", 1)[-1].strip()
            entry["verdict"], entry["verdictText"] = "error", "FEHLER"
        elif what.startswith("Hinweis:"):
            entry["notes"].append(what[len("Hinweis:"):].strip())
        elif what.startswith("check:"):
            entry["groups"].append({"name": "check", "kind": "check", "ok": what == "check: OK", "passed": None,
                                    "total": None, "failed": None, "red": [], "log": log_name})
        else:
            g = re.match(r"([\w.-]+): (.*)$", what)
            if not g:
                continue
            name, body = g.groups()
            green = re.match(r"gruen (\d+)/(\d+)$", body)
            group = {"name": name, "kind": "tests", "ok": bool(green), "passed": None, "total": None, "failed": None,
                     "red": [], "log": log_name}
            if green:
                group["passed"], group["total"], group["failed"] = int(green[1]), int(green[2]), 0
            else:
                counts = re.search(r"NICHT gruen: (\d+)/(\d+) bestanden, (\d+) rot", body)
                if counts:
                    group["passed"], group["total"], group["failed"] = (int(x) for x in counts.groups())
                group["red"] = [p.strip() for p in re.sub(r"\([^()]*\.log\)\s*$", "", re.sub(r"^\*\*ROT\*\* -", "", body)).split("|")
                                if re.match(r"\s*(ROT|FEHLER) ", p)]
            entry["groups"].append(group)
    return list(reversed(runs))


def parse_lock(text: str | None) -> int | None:
    first = (text or "").strip().splitlines()[:1]
    return int(first[0]) if first and first[0].strip().isdigit() else None


# ---------------------------------------------------------------------------------------- processes
def process_info(pid: int) -> dict | None:
    """{pid, name, commandLine, created} of a running process, None when it does not exist (Windows: CIM)."""
    if os.name != "nt":
        try:
            cmd = Path(f"/proc/{pid}/cmdline").read_bytes().replace(b"\0", b" ").decode(errors="replace").strip()
            return {"pid": pid, "name": Path(cmd.split(" ")[0]).name, "commandLine": cmd, "created": None}
        except OSError:
            return None
    script = (f"$p = Get-CimInstance Win32_Process -Filter 'ProcessId={int(pid)}'; if ($p) {{ "
              "[ordered]@{pid=$p.ProcessId; name=$p.Name; commandLine=$p.CommandLine; "
              "created=$p.CreationDate.ToUniversalTime().ToString('o')} | ConvertTo-Json -Compress }")
    shell = shutil.which("pwsh") or shutil.which("powershell") or "powershell"
    try:
        done = subprocess.run([shell, "-NoProfile", "-NonInteractive", "-Command", script], capture_output=True,
                              text=True, timeout=20, check=False, creationflags=0x08000000)
    except (OSError, subprocess.SubprocessError):
        return None
    try:
        data = json.loads(done.stdout.strip() or "null")
    except json.JSONDecodeError:
        return None
    return data if isinstance(data, dict) else None


def process_key(pid: int) -> str | None:
    """Cheap liveness check: a key unique for this process instance (creation time), None when it is gone.

    process_info() needs a PowerShell/CIM round trip (seconds); the hub polls, so the identity is cached
    per key and only looked up again when a PID is reused by a new process."""
    if os.name != "nt":
        return str(pid) if Path(f"/proc/{pid}").exists() else None
    import ctypes
    from ctypes import wintypes
    k32 = ctypes.WinDLL("kernel32", use_last_error=True)
    k32.OpenProcess.restype = wintypes.HANDLE
    handle = k32.OpenProcess(0x1000, False, int(pid))  # PROCESS_QUERY_LIMITED_INFORMATION
    if not handle:
        return None
    try:
        code = wintypes.DWORD()
        if not k32.GetExitCodeProcess(handle, ctypes.byref(code)) or code.value != 259:  # STILL_ACTIVE
            return None
        times = [wintypes.FILETIME() for _ in range(4)]
        if not k32.GetProcessTimes(handle, *(ctypes.byref(t) for t in times)):
            return None
        return f"{pid}-{(times[0].dwHighDateTime << 32) | times[0].dwLowDateTime}"
    finally:
        k32.CloseHandle(handle)


def is_gate_process(info: dict | None) -> bool:
    """The lock's PID is only trusted when it is a PowerShell running an offline_gate script (PID reuse!)."""
    if not info:
        return False
    name = info.get("name") or ""
    return bool((SHELL_RE.match(name) or (os.name != "nt" and "pwsh" in name))
                and SCRIPT_RE.search(info.get("commandLine") or ""))


def _shell() -> str | None:
    return shutil.which("pwsh")


# ---------------------------------------------------------------------------------------- mixin
class OfflineMixin:
    def offline_dir(self) -> Path:
        return runs_dir(self.repo)

    def offline_script(self) -> Path:
        return self.repo.joinpath(*SCRIPT)

    def offline_lock(self) -> dict:
        """{pid, alive, identity, info}: alive = the PID is a running offline_gate script."""
        pid = parse_lock(_read(self.offline_dir() / "offline-gate.lock"))
        if pid is None:
            return {"pid": None, "alive": False, "stale": False, "info": None}
        key = process_key(pid)
        info = None if key is None else self.cached(f"offline-proc-{key}", 86400, lambda: process_info(pid))
        alive = is_gate_process(info)
        return {"pid": pid, "alive": alive, "stale": not alive, "info": info}

    def _resolve_ref(self, ref: str) -> str | None:
        if not settings._plain_ref(ref):
            return None
        code, out = paths.git(["rev-parse", "--verify", "--quiet", ref + "^{commit}"], self.repo)
        return out.strip() if code == 0 and paths.is_sha(out.strip()) else None

    def _validate_refs(self, refs: list[str]) -> list[str]:
        from .api import HubError
        if len(refs) > MAX_REFS:
            raise HubError(400, f"at most {MAX_REFS} refs per run")
        resolved, bad = [], []
        for ref in refs:
            sha = self._resolve_ref(ref)
            if sha is None:
                bad.append(ref)
            elif sha not in resolved:
                resolved.append(sha)
        if bad:
            raise HubError(400, "unknown or invalid ref (git rev-parse): " + ", ".join(bad[:10]), bad=bad[:10])
        return resolved

    @staticmethod
    def _split_refs(value) -> list[str]:
        if isinstance(value, str):
            value = re.split(r"[,\s]+", value)
        if not isinstance(value, list) or not all(isinstance(v, str) for v in value):
            from .api import HubError
            raise HubError(400, "refs must be a list of strings")
        return [v.strip() for v in value if v.strip()]

    @staticmethod
    def _queue_refs(text: str) -> list[str]:
        return [r for line in text.splitlines() for r in re.split(r"[,\s]+", line.split("#", 1)[0].strip()) if r]

    def offline_state(self) -> dict:
        folder = self.offline_dir()
        lock = self.offline_lock()
        runs = parse_results_json(_read(folder / "offline-results.json"))
        source = "json"
        if not runs:
            runs, source = parse_results_md(_read(folder / "offline-results.md")), "md"
            if runs and lock["alive"] and runs[0]["status"] == "unknown":
                runs[0]["status"], runs[0]["pid"] = "running", lock["pid"]   # the md has no status; the newest unfinished run is the live one
        for run in runs:
            if run.get("status") == "running" and not (lock["alive"] and lock["pid"] == run.get("pid")):
                run["status"] = "aborted"   # killed without its finally block (taskkill /F, shutdown, power loss)
        status_file = folder / "offline-status.txt"
        status_text = (_read(status_file) or "").strip()
        try:
            status_age = int(time.time() - status_file.stat().st_mtime)
        except OSError:
            status_age = None
        code, master = paths.git(["rev-parse", "--verify", "--quiet", "master^{commit}"], self.repo)
        if code != 0:
            master = self.head()
        return {
            "dir": str(folder), "script": str(self.offline_script()), "scriptExists": self.offline_script().is_file(),
            "shell": _shell(), "dryRun": settings.dry_run(),
            "running": lock["alive"], "lock": {"pid": lock["pid"], "alive": lock["alive"], "stale": lock["stale"]},
            "status": status_text, "statusAgeSeconds": status_age,
            "runs": runs[:20], "source": source,
            "queue": _read(folder / "offline-queue.txt") or "",
            "master": master.strip()[:40] if master else "",
        }

    def offline_argv(self, refs: list[str], profile: str, online: bool) -> list[str]:
        argv = [_shell() or "pwsh", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(self.offline_script())]
        if refs:
            argv += ["-Refs", ",".join(refs)]
        argv += ["-Profile", profile]
        if online:
            argv.append("-Online")
        return argv

    def offline_start(self, body: dict) -> dict:
        from .api import HubError
        profile = body.get("profile", "all")
        if profile not in PROFILES:
            raise HubError(400, "profile must be '263' or 'all'")
        online = body.get("online", False)
        if not isinstance(online, bool):
            raise HubError(400, "online must be true or false")
        use_queue = body.get("useQueue", False) is True
        if use_queue:
            queued = self._queue_refs(_read(self.offline_dir() / "offline-queue.txt") or "")
            if not queued:
                raise HubError(400, "the queue (offline-queue.txt) is empty")
            self._validate_refs(queued)
            refs: list[str] = []          # the script reads the queue itself
        else:
            refs = self._validate_refs(self._split_refs(body.get("refs", [])))
            if not refs:
                raise HubError(400, "no ref given (or use the queue)")
        if not self.offline_script().is_file():
            raise HubError(500, f"script missing: {self.offline_script()}")
        lock = self.offline_lock()
        if lock["alive"]:
            raise HubError(409, f"an offline run is already running (PID {lock['pid']})", pid=lock["pid"])
        argv = self.offline_argv([r[:12] for r in refs], profile, online)
        if settings.dry_run():
            return {"dryRun": True, "argv": argv, "cwd": str(self.repo), "refs": refs, "started": False}
        if not _shell():
            raise HubError(500, "PowerShell 7 (pwsh) not found on PATH")
        folder = self.offline_dir()
        folder.mkdir(parents=True, exist_ok=True)
        proc = spawn_detached(argv, self.repo, folder / "offline-launch.log")
        return {"dryRun": False, "argv": argv, "cwd": str(self.repo), "refs": refs, "started": True, "pid": proc.pid}

    def offline_stop(self, body: dict) -> dict:
        from .api import HubError
        lock_path = self.offline_dir() / "offline-gate.lock"
        pid = parse_lock(_read(lock_path))
        if pid is None:
            raise HubError(409, "no offline run is running (no lock)")
        info = process_info(pid)
        if info is None:
            if not settings.dry_run():
                try:
                    lock_path.unlink()
                except OSError:
                    pass
            return {"stopped": False, "staleLockRemoved": True, "pid": pid}
        if not is_gate_process(info):
            raise HubError(409, f"PID {pid} from the lock is not an offline_gate script ({info.get('name')}); "
                                "nothing was stopped", pid=pid)
        if settings.dry_run():
            return {"dryRun": True, "stopped": False, "pid": pid, "argv": ["taskkill", "/PID", str(pid), "/T", "/F"]}
        again = process_info(pid)
        if not again or again.get("created") != info.get("created") or again.get("commandLine") != info.get("commandLine"):
            raise HubError(409, "process identity changed; nothing was stopped")
        if os.name == "nt":
            subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"], stdout=subprocess.DEVNULL,
                           stderr=subprocess.DEVNULL, timeout=30, check=False)
        else:  # pragma: no cover
            os.kill(pid, 15)
        deadline = time.time() + 10
        while time.time() < deadline and process_key(pid):
            time.sleep(0.5)
        try:
            lock_path.unlink()
        except OSError:
            pass
        try:
            (self.offline_dir() / "offline-status.txt").write_text(
                time.strftime("%Y-%m-%d %H:%M") + "  abgebrochen (im Hub gestoppt)\n", encoding="utf-8")
        except OSError:
            pass
        return {"stopped": True, "pid": pid}

    def offline_queue_save(self, body: dict) -> dict:
        from .api import HubError
        text = body.get("text")
        if not isinstance(text, str) or len(text.encode("utf-8")) > MAX_QUEUE_BYTES or "\x00" in text:
            raise HubError(400, "text must be a string below 8 KB")
        self._validate_refs(self._queue_refs(text))
        if settings.dry_run():
            return {"dryRun": True, "saved": False}
        folder = self.offline_dir()
        folder.mkdir(parents=True, exist_ok=True)
        (folder / "offline-queue.txt").write_text(text.replace("\r\n", "\n").rstrip("\n") + "\n", encoding="utf-8")
        return {"saved": True}

    def offline_log(self, query: dict) -> str:
        from .api import HubError
        name = (query.get("name") or [""])[0]
        if not LOG_NAME_RE.match(name):
            raise HubError(400, "invalid log name")
        try:
            path = paths.safe_join(self.offline_dir() / "offline-logs", name)
        except ValueError as error:
            raise HubError(400, str(error)) from error
        if not path.is_file():
            raise HubError(404, f"log not found: {name}")
        with open(path, "rb") as handle:
            data = handle.read(8_000_000)
        return data.decode("utf-8", errors="replace")


def spawn_detached(argv: list[str], cwd: Path, launch_log: Path) -> subprocess.Popen:
    """Own process group, no console window, not tied to the hub (breaks away from a job object when allowed)."""
    kwargs: dict = {"cwd": str(cwd), "stdin": subprocess.DEVNULL, "close_fds": True}
    log = open(launch_log, "ab")
    kwargs["stdout"] = log
    kwargs["stderr"] = subprocess.STDOUT
    try:
        if os.name == "nt":
            base = subprocess.CREATE_NEW_PROCESS_GROUP | 0x08000000  # CREATE_NO_WINDOW
            try:
                return subprocess.Popen(argv, creationflags=base | 0x01000000, **kwargs)  # CREATE_BREAKAWAY_FROM_JOB
            except OSError:
                return subprocess.Popen(argv, creationflags=base, **kwargs)
        return subprocess.Popen(argv, start_new_session=True, **kwargs)
    finally:
        log.close()
