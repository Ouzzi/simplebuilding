"""Launch targets, test target catalogue, command templating and filter patterns.

Argv lists are built without launching Gradle; network availability is probed briefly. Every id that reaches a
command line is checked against an allow-list first.
"""

from __future__ import annotations

import fnmatch
import importlib.util
import json
import os
import re
import sys
import socket
import threading
import time
from concurrent.futures import Future, TimeoutError
from pathlib import Path

from . import paths

LAUNCH_FILE = paths.HUB_DIR / "launch_targets.json"
RUN_PY = paths.REPO / "tools" / "testrunner" / "run.py"

#: Filter patterns go to Gradle as -PgametestFilter. Only the characters a test id and a * can have.
FILTER_RE = re.compile(r"^[A-Za-z0-9_:*.\-]{1,300}$")
CLIENT_ENTRY_RE = re.compile(r"^[a-z0-9][a-z0-9\-]{0,80}$")

_runner = None
_dns_lock = threading.Lock()
_dns_result = None
_dns_expires = 0.0


def gradle_offline() -> bool:
    """Bound DNS waits to 0.5 s, cache for 15 s, keep at most one resolver alive."""
    global _dns_result, _dns_expires
    if os.environ.get("SIMPLEBUILDING_GRADLE_OFFLINE") == "1":
        return True
    with _dns_lock:
        if _dns_result is None or (_dns_result.done() and time.monotonic() >= _dns_expires):
            result = Future()
            _dns_result = result
            _dns_expires = time.monotonic() + 15

            def resolve():
                try:
                    socket.getaddrinfo("piston-meta.mojang.com", 443)
                    result.set_result(False)
                except OSError:
                    result.set_result(True)

            threading.Thread(target=resolve, daemon=True).start()
        result = _dns_result
    try:
        return result.result(timeout=0.5)
    except TimeoutError:
        return True


def gradle_args(needs_forge262: bool = False) -> list[str]:
    offline = gradle_offline()
    if offline and needs_forge262:
        raise TargetError("Forge 26.2 braucht Netz (Mavenizer)")
    return ([] if needs_forge262 else ["-PskipForge262=true"]) + (["--offline"] if offline else [])


class TargetError(ValueError):
    pass


def runner():
    """tools/testrunner/run.py as a module (its targets, catalogue and constants are reused, not copied)."""
    global _runner
    if _runner is None:
        spec = importlib.util.spec_from_file_location("sb_testrunner_run", RUN_PY)
        module = importlib.util.module_from_spec(spec)
        sys.modules["sb_testrunner_run"] = module
        spec.loader.exec_module(module)
        _runner = module
    return _runner


def load_launch(path: Path = LAUNCH_FILE) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    manifest = path.parents[2] / "modules/modules.json"
    if manifest.exists():
        data["moduleTargets"] = {m["id"]: m["tests"] for m in json.loads(manifest.read_text(encoding="utf-8"))["modules"] if m.get("tests")}
    return data


def loaders(data: dict | None = None) -> dict[str, dict]:
    """id -> loader entry (with its line id added)."""
    data = data or load_launch()
    out = {}
    for line in data["lines"]:
        for entry in line["loaders"]:
            out[entry["id"]] = dict(entry, line=line["id"], lineLabel=line["label"], main=line["main"])
    return out


def find_loader(target_id: str, data: dict | None = None) -> dict:
    found = loaders(data).get(target_id)
    if not found:
        raise TargetError(f"unknown launch target: {target_id!r}")
    return found


def expand_gradle_args(args: list[str]) -> list[str]:
    out: list[str] = []
    for arg in args:
        if arg == "@forge":
            out.extend(runner().FORGE_GRADLE_ARGS)
        else:
            out.append(arg)
    return out


def gradlew_path(workspace: Path) -> str:
    return str(workspace / ("gradlew.bat" if os.name == "nt" else "gradlew"))


def launch_command(entry: dict, kind: str, workspace: Path, data: dict | None = None,
                   program_args: str | None = None) -> list[str]:
    """argv for :runClient / :runServer of one loader entry, e.g. gradlew.bat :mc26_3:fabric:runClient."""
    data = data or load_launch()
    if kind not in data["tasks"]:
        raise TargetError(f"unknown launch kind: {kind!r}")
    argv = [gradlew_path(workspace), *gradle_args(entry["prefix"] == ":forge:"),
            *expand_gradle_args(entry["gradleArgs"]), entry["prefix"] + data["tasks"][kind]]
    if program_args:
        argv.append("--args=" + program_args)
    return argv


def world_path(entry: dict, workspace: Path, data: dict | None = None) -> Path:
    data = data or load_launch()
    return paths.safe_join(workspace, *entry["runDir"].split("/"), "saves", data["worldName"])


# ----------------------------------------------------------------------------
# Test targets (from run.py)
# ----------------------------------------------------------------------------

def test_targets() -> list[dict]:
    run = runner()
    snapshot = {t.id for t in run.SNAPSHOT_TARGETS}
    return [
        {"id": t.id, "label": t.label, "kind": t.kind, "loader": t.loader, "line": t.mc_line,
         "task": t.gradle_task, "snapshot": t.id in snapshot}
        for t in run.ALL_TARGETS
    ]


def test_presets() -> dict[str, dict]:
    run = runner()
    server = [t.id for t in run.DEFAULT_TARGETS]
    client = [t.id for t in run.TARGETS if t.kind == "client"]
    return {
        "main": {"label": "Main line 26.3 (Fabric + NeoForge)", "targets": ["fabric-263", "neoforge-263"]},
        "all-server": {"label": "All server lines", "targets": server},
        "main-client": {"label": "Client suites 26.3", "targets": ["client-fabric-263", "client-neoforge-263"]},
        "all-client": {"label": "All client suites", "targets": client},
    }


def validate_targets(ids) -> list[str]:
    known = runner().BY_ID
    if not isinstance(ids, list) or not ids:
        raise TargetError("choose at least one test target")
    out = []
    for item in ids:
        if not isinstance(item, str) or item not in known:
            raise TargetError(f"unknown test target: {item!r}")
        if item not in out:
            out.append(item)
    return out


def validate_filter(pattern) -> str | None:
    """One pattern per run. A comma is refused: the runner silently keeps only the first pattern."""
    if pattern is None or pattern == "":
        return None
    if not isinstance(pattern, str):
        raise TargetError("filter must be text")
    if "," in pattern:
        raise TargetError("one filter pattern per run - a comma silently uses only the first pattern "
                          "(select several tests and let the hub queue sequential runs instead)")
    if not FILTER_RE.match(pattern):
        raise TargetError("filter may only contain letters, digits, _ : . - and *")
    return pattern


def validate_client_entries(names) -> list[str]:
    if not names:
        return []
    if not isinstance(names, list):
        raise TargetError("client test names must be a list")
    for name in names:
        if not isinstance(name, str) or not CLIENT_ENTRY_RE.match(name):
            raise TargetError(f"invalid client test name: {name!r}")
    return list(dict.fromkeys(names))


def test_argv(workspace: Path, target_ids: list[str], pattern: str | None, python: str | None = None,
              trigger: str = "hub") -> list[str]:
    script = paths.safe_join(workspace, "tools", "testrunner", "run.py")
    argv = [python or sys.executable, str(script), "--targets", ",".join(validate_targets(target_ids)),
            "--trigger", trigger]
    if pattern:
        argv.append("--filter=" + validate_filter(pattern))
    return argv


def test_env(target_ids: list[str]) -> dict[str, str]:
    """Pass the same policy through the Python test runner, without changing hub globals."""
    ids = validate_targets(target_ids)
    args = gradle_args(any(runner().BY_ID[tid].gradle_task.startswith(":forge:") for tid in ids))
    return {"SIMPLEBUILDING_GRADLE_OFFLINE": "1" if "--offline" in args else "0",
            "SIMPLEBUILDING_SKIP_FORGE262": "1" if "-PskipForge262=true" in args else "0"}


def check_argv(workspace: Path, mc264: bool = False) -> list[str]:
    # The full gate includes Forge 26.2; never silently reduce its coverage.
    argv = [gradlew_path(workspace), *gradle_args(needs_forge262=True)]
    if mc264:
        argv.append("-Pmc264=true")
    argv += ["check", "-q"]
    return argv


# ----------------------------------------------------------------------------
# Filter patterns for a set of tests
# ----------------------------------------------------------------------------

def glob_matches(pattern: str, test_id: str) -> bool:
    return fnmatch.fnmatchcase(test_id, pattern)


def build_patterns(selected: list[str], catalogue: list[dict]) -> list[str]:
    """A valid list of single filter patterns that together select exactly `selected`.

    Each pattern is one run. Tests of one class are folded into a wildcard when the wildcard hits
    nothing outside the selection; everything else runs as its exact id. `catalogue` is
    run.read_catalogue()[line]: [{"id", "testClass"}, ...].
    """
    chosen = list(dict.fromkeys(selected))
    chosen_set = set(chosen)
    all_ids = [e["id"] for e in catalogue]
    by_class: dict[str, list[str]] = {}
    class_of = {e["id"]: e["testClass"] for e in catalogue}
    loose: list[str] = []
    for test_id in chosen:
        cls = class_of.get(test_id)
        (by_class.setdefault(cls, []) if cls else loose).append(test_id)
    patterns: list[str] = []
    for cls, ids in sorted(by_class.items()):
        if len(ids) > 1:
            prefix = os.path.commonprefix(ids)
            candidate = prefix + "*"
            hits = {i for i in all_ids if glob_matches(candidate, i)}
            if len(prefix) > len("simplebuilding:") and hits and hits <= chosen_set:
                patterns.append(candidate)
                continue
        patterns.extend(ids)
    patterns.extend(loose)
    return list(dict.fromkeys(patterns))
