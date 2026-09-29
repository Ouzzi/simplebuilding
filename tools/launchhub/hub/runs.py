"""Run records (testing/runs/*.json written by tools/testrunner/run.py) and everything derived from them.

The runner exits 0 even when tests are red, so nothing here ever looks at an exit code of the runner
process: a run is judged by its record (see `verdict`). All analysis functions are pure and work on
the "slim" records `load_records` returns, newest first.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import statistics
import threading
from datetime import datetime, timezone
from pathlib import Path

ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]")
MAX_MESSAGE = 1500

_CACHE: dict[str, tuple[float, int, dict]] = {}
_LOCK = threading.Lock()


# ----------------------------------------------------------------------------
# Loading
# ----------------------------------------------------------------------------

def is_mutation(record: dict) -> bool:
    """Mutation rounds are red on purpose (mutations.py); they must not pollute failures or trends."""
    return str(record.get("trigger") or "").startswith("mutation-")


def slim(record: dict) -> dict:
    """Keeps what the hub needs; the big 'missing' lists shrink to counts."""
    targets = []
    for target in record.get("targets", []):
        tests = []
        for test in target.get("tests", []):
            if test.get("foreign"):
                continue
            message = test.get("message")
            tests.append({
                "id": test.get("id", ""),
                "status": test.get("status", "?"),
                "timeMs": test.get("timeMs", test.get("durationMs", 0)) or 0,
                "message": (message[:MAX_MESSAGE] if isinstance(message, str) else None),
                "type": test.get("type"),
            })
        targets.append({
            "id": target.get("id"),
            "label": target.get("label"),
            "kind": target.get("kind", "server"),
            "mcLine": target.get("mcLine"),
            "selected": bool(target.get("selected")),
            "exitCode": target.get("exitCode"),
            "durationMs": target.get("durationMs") or 0,
            "counts": target.get("counts") or {"total": 0, "passed": 0, "failed": 0},
            "expected": target.get("expected"),
            "missingCount": len(target.get("missing") or []),
            "unexpectedCount": len(target.get("unexpected") or []),
            "error": target.get("error"),
            "warning": target.get("warning"),
            "log": target.get("logPath") or target.get("log"),
            "tests": tests,
        })
    return {
        "runId": record.get("runId"),
        "startedAt": record.get("startedAt"),
        "finishedAt": record.get("finishedAt"),
        "durationMs": record.get("durationMs") or 0,
        "trigger": record.get("trigger"),
        "filter": record.get("filter"),
        "git": record.get("git") or {},
        "modVersion": record.get("modVersion"),
        "totals": record.get("totals") or {"total": 0, "passed": 0, "failed": 0},
        "ok": bool(record.get("ok")),
        "targets": targets,
    }


def load_records(runs_dir: Path, limit: int, include_mutations: bool = False, scan_cap: int = 900) -> list[dict]:
    """The newest `limit` usable records, newest first. Parsed files are cached by mtime and size."""
    out: list[dict] = []
    if not runs_dir.is_dir():
        return out
    names = sorted((n for n in os.listdir(runs_dir) if n.endswith(".json")), reverse=True)[:scan_cap]
    for name in names:
        if len(out) >= limit:
            break
        path = runs_dir / name
        try:
            stat = path.stat()
        except OSError:
            continue
        key = str(path)
        with _LOCK:
            cached = _CACHE.get(key)
        if cached and cached[0] == stat.st_mtime and cached[1] == stat.st_size:
            record = cached[2]
        else:
            try:
                record = slim(json.loads(path.read_text(encoding="utf-8")))
            except (OSError, json.JSONDecodeError, AttributeError):
                continue
            with _LOCK:
                _CACHE[key] = (stat.st_mtime, stat.st_size, record)
        if not record.get("runId"):
            continue
        if is_mutation(record) and not include_mutations:
            continue
        out.append(record)
    out.sort(key=lambda r: r.get("startedAt") or "", reverse=True)
    return out


def find_record(records: list[dict], run_id: str) -> dict | None:
    return next((r for r in records if r["runId"] == run_id), None)


def load_one(runs_dir: Path, run_id: str) -> dict | None:
    if not re.fullmatch(r"[0-9A-Za-z\-]{10,60}", run_id or ""):
        return None
    path = runs_dir / f"{run_id}.json"
    if not path.is_file():
        return None
    try:
        return slim(json.loads(path.read_text(encoding="utf-8")))
    except (OSError, json.JSONDecodeError):
        return None


# ----------------------------------------------------------------------------
# Verdict ("alles gruen")
# ----------------------------------------------------------------------------

def selected_targets(record: dict) -> list[dict]:
    return [t for t in record["targets"] if t["selected"]]


def verdict(record: dict) -> str:
    """'green' | 'red' | 'none'. Judged from the record, never from an exit code."""
    ran = selected_targets(record)
    if not ran:
        return "none"
    for target in ran:
        if target["error"] or target["counts"].get("failed", 0) > 0:
            return "red"
        if target["counts"].get("total", 0) == 0:
            return "red"
    return "green" if record["ok"] else "red"


def full_coverage(record: dict) -> bool:
    """Unfiltered and no registered test missing - the strict sense of green used for pushing."""
    if record.get("filter"):
        return False
    return all(t["missingCount"] == 0 for t in selected_targets(record) if t["kind"] != "client")


def brief(record: dict) -> dict:
    ran = selected_targets(record)
    return {
        "runId": record["runId"], "startedAt": record["startedAt"], "durationMs": record["durationMs"],
        "trigger": record["trigger"], "filter": record["filter"], "commit": (record["git"] or {}).get("short"),
        "dirty": (record["git"] or {}).get("dirty"), "branch": (record["git"] or {}).get("branch"),
        "totals": record["totals"], "verdict": verdict(record), "fullCoverage": full_coverage(record),
        "targets": [{"id": t["id"], "kind": t["kind"], "total": t["counts"].get("total", 0),
                     "passed": t["counts"].get("passed", 0), "failed": t["counts"].get("failed", 0),
                     "durationMs": t["durationMs"], "error": bool(t["error"])} for t in ran],
    }


# ----------------------------------------------------------------------------
# Per-test state and history
# ----------------------------------------------------------------------------

def latest_states(records: list[dict]) -> dict[str, dict[str, dict]]:
    """target id -> test id -> {status, runId, at, message, timeMs, type} of the newest record that ran the test."""
    states: dict[str, dict[str, dict]] = {}
    closed: set[str] = set()
    for record in records:  # newest first
        for target in selected_targets(record):
            if target["id"] in closed:
                continue
            if target["kind"] == "client":
                # Client suites always run whole and have no catalogue to tell a stale checkpoint from a
                # live one: only the newest run of the target counts.
                closed.add(target["id"])
            bucket = states.setdefault(target["id"], {})
            for test in target["tests"]:
                if test["id"] not in bucket:
                    bucket[test["id"]] = {"status": test["status"], "runId": record["runId"],
                                          "at": record["startedAt"], "message": test["message"],
                                          "timeMs": test["timeMs"], "type": test["type"]}
    return states


def history(records: list[dict], target_id: str, test_id: str) -> list[dict]:
    """Chronological (oldest first) results of one test."""
    out = []
    for record in reversed(records):
        for target in selected_targets(record):
            if target["id"] != target_id:
                continue
            for test in target["tests"]:
                if test["id"] == test_id:
                    git = record["git"] or {}
                    out.append({"runId": record["runId"], "at": record["startedAt"], "status": test["status"],
                                "commit": git.get("commit") or "", "short": git.get("short") or "",
                                "dirty": bool(git.get("dirty")), "timeMs": test["timeMs"],
                                "message": test["message"], "trigger": record["trigger"]})
    return out


def first_failed(seq: list[dict]) -> dict | None:
    """The run in which the current failing streak began (None when the test is not failing now)."""
    if not seq or seq[-1]["status"] != "failed":
        return None
    start = len(seq) - 1
    while start > 0 and seq[start - 1]["status"] == "failed":
        start -= 1
    return seq[start]


def flaky_kind(seq: list[dict], window: int = 12) -> str | None:
    """'confirmed': same clean commit produced both outcomes. 'suspect': >= 3 flips in the last `window` results."""
    recent = seq[-window:]
    by_commit: dict[str, set[str]] = {}
    for item in recent:
        if item["commit"] and not item["dirty"]:
            by_commit.setdefault(item["commit"], set()).add(item["status"] == "failed")
    if any(len(v) == 2 for v in by_commit.values()):
        return "confirmed"
    flips = sum(1 for a, b in zip(recent, recent[1:]) if (a["status"] == "failed") != (b["status"] == "failed"))
    return "suspect" if flips >= 3 else None


def test_class(test_id: str, class_of: dict[str, str]) -> str:
    """Feature group of a test id: the catalogue's test class, else the text before '_game_test_' / first '-'."""
    if test_id in class_of:
        return class_of[test_id]
    name = test_id.split(":", 1)[-1]
    if "_game_test_" in name:
        return name.split("_game_test_", 1)[0]
    return name.split("-", 1)[0]


def rows(records: list[dict], class_of: dict[str, str], known_ids: dict[str, set[str]] | None = None) -> list[dict]:
    """One row per (target, test) with latest status, streak start and flaky flag."""
    states = latest_states(records)
    out = []
    seqs: dict[tuple[str, str], list[dict]] = {}
    for record in reversed(records):
        git = record["git"] or {}
        for target in selected_targets(record):
            for test in target["tests"]:
                seqs.setdefault((target["id"], test["id"]), []).append(
                    {"runId": record["runId"], "at": record["startedAt"], "status": test["status"],
                     "commit": git.get("commit") or "", "dirty": bool(git.get("dirty"))})
    for target_id, bucket in states.items():
        known = (known_ids or {}).get(target_id)
        for test_id, state in bucket.items():
            if known is not None and known and test_id not in known:
                continue  # removed from the catalogue: an old result, not a live test
            seq = seqs.get((target_id, test_id), [])
            ff = first_failed(seq)
            out.append({
                "target": target_id, "id": test_id, "class": test_class(test_id, class_of),
                "status": state["status"], "runId": state["runId"], "at": state["at"],
                "message": state["message"], "timeMs": state["timeMs"],
                "firstFailedRun": ff["runId"] if ff else None, "firstFailedAt": ff["at"] if ff else None,
                "flaky": flaky_kind(seq), "results": len(seq),
                "passedOf": [1 if s["status"] != "failed" else 0 for s in seq[-12:]],
            })
    return out


def target_errors(records: list[dict]) -> list[dict]:
    """Targets whose newest record has a target level error (broken build, no report) - no failing test to blame."""
    seen: set[str] = set()
    out = []
    for record in records:
        for target in selected_targets(record):
            if target["id"] in seen:
                continue
            seen.add(target["id"])
            if target["error"]:
                out.append({"target": target["id"], "runId": record["runId"], "at": record["startedAt"],
                            "error": target["error"]})
    return out


# ----------------------------------------------------------------------------
# Trends, durations, comparison
# ----------------------------------------------------------------------------

def sweeps(records: list[dict], limit: int) -> list[dict]:
    """Full unfiltered server runs, chronological, last `limit`."""
    picked = [r for r in records if not r.get("filter") and any(t["kind"] != "client" for t in selected_targets(r))]
    return list(reversed(picked[:limit]))


def trend(records: list[dict], limit: int = 30) -> dict:
    points = []
    per_target: dict[str, list[dict]] = {}
    for record in sweeps(records, limit):
        points.append({"runId": record["runId"], "at": record["startedAt"], "total": record["totals"].get("total", 0),
                       "passed": record["totals"].get("passed", 0), "failed": record["totals"].get("failed", 0),
                       "verdict": verdict(record), "commit": (record["git"] or {}).get("short")})
        for target in selected_targets(record):
            per_target.setdefault(target["id"], []).append(
                {"runId": record["runId"], "total": target["counts"].get("total", 0),
                 "failed": target["counts"].get("failed", 0), "durationMs": target["durationMs"]})
    return {"runs": points, "targets": per_target}


def durations(records: list[dict], limit: int = 15) -> dict[str, dict]:
    per: dict[str, list[int]] = {}
    for record in records:
        if record.get("filter"):
            continue
        for target in selected_targets(record):
            if target["durationMs"] > 0 and len(per.setdefault(target["id"], [])) < limit:
                per[target["id"]].append(target["durationMs"])
    return {tid: {"lastMs": v[0], "medianMs": int(statistics.median(v)), "samples": len(v)} for tid, v in per.items() if v}


def compare(a: dict, b: dict) -> dict:
    """b (newer) against a (older): newly failing, newly fixed, only in one of them."""
    def flat(record):
        out = {}
        for target in selected_targets(record):
            for test in target["tests"]:
                out[(target["id"], test["id"])] = test
        return out
    fa, fb = flat(a), flat(b)
    newly_failing, newly_fixed, still_failing = [], [], []
    for key, test in fb.items():
        before = fa.get(key)
        entry = {"target": key[0], "id": key[1], "message": test["message"]}
        if test["status"] == "failed":
            if before is None or before["status"] != "failed":
                newly_failing.append(dict(entry, before=(before or {}).get("status", "not run")))
            else:
                still_failing.append(entry)
        elif before is not None and before["status"] == "failed":
            newly_fixed.append(entry)
    return {"a": brief(a), "b": brief(b), "newlyFailing": newly_failing, "newlyFixed": newly_fixed,
            "stillFailing": still_failing, "onlyInA": len(fa.keys() - fb.keys()), "onlyInB": len(fb.keys() - fa.keys())}


# ----------------------------------------------------------------------------
# Markdown
# ----------------------------------------------------------------------------

def strip_ansi(text: str) -> str:
    return ANSI.sub("", text)


def failure_markdown(row: dict, excerpt: str = "") -> str:
    lines = [f"### `{row['id']}` on `{row['target']}`",
             f"- Status: **{row['status']}** in run `{row['runId']}` ({row['at']})"]
    if row.get("firstFailedRun"):
        lines.append(f"- First failed in run `{row['firstFailedRun']}`")
    if row.get("flaky"):
        lines.append(f"- Flaky: {row['flaky']}")
    lines += ["", "```", (row.get("message") or "(no message in the record)").strip()]
    if excerpt:
        lines += ["", excerpt.strip()]
    lines.append("```")
    return "\n".join(lines) + "\n"


def summary_markdown(record: dict) -> str:
    git = record["git"] or {}
    lines = [f"# Test run `{record['runId']}`", "",
             f"- Verdict: **{'ALL GREEN' if verdict(record) == 'green' else verdict(record).upper()}** "
             f"(judged from the record, not from an exit code)",
             f"- Commit: `{git.get('short', '?')}`{' (dirty)' if git.get('dirty') else ''} on `{git.get('branch', '?')}`",
             f"- Filter: `{record['filter'] or 'none'}`, trigger `{record['trigger']}`, "
             f"{record['durationMs'] / 1000:.0f} s",
             f"- Totals: {record['totals'].get('passed', 0)}/{record['totals'].get('total', 0)} passed, "
             f"{record['totals'].get('failed', 0)} failed", "",
             "| Target | Tests | Passed | Failed | Missing | Duration |", "|---|---:|---:|---:|---:|---:|"]
    for target in selected_targets(record):
        c = target["counts"]
        lines.append(f"| {target['id']} | {c.get('total', 0)} | {c.get('passed', 0)} | {c.get('failed', 0)} | "
                     f"{target['missingCount']} | {target['durationMs'] / 1000:.0f} s |")
    problems = [(t, x) for t in selected_targets(record) for x in t["tests"] if x["status"] == "failed"]
    errors = [t for t in selected_targets(record) if t["error"]]
    if errors:
        lines += ["", "## Target errors"]
        lines += [f"- `{t['id']}`: {t['error']}" for t in errors]
    if problems:
        lines += ["", "## Failing tests"]
        for target, test in problems:
            lines += ["", f"### `{test['id']}` ({target['id']})", "```", (test["message"] or "(no message)").strip(), "```"]
    return "\n".join(lines) + "\n"


# ----------------------------------------------------------------------------
# Log excerpts
# ----------------------------------------------------------------------------

def log_excerpt(text: str, test_id: str, before: int = 2, after: int = 28, max_chars: int = 3500) -> str:
    """Lines around the first mention of the test in a raw Gradle log (ANSI stripped, bounded)."""
    needles = [test_id, test_id.split(":", 1)[-1]]
    lines = strip_ansi(text).splitlines()
    for i, line in enumerate(lines):
        if any(n and n in line for n in needles):
            chunk = "\n".join(lines[max(0, i - before): i + after])
            return chunk[:max_chars]
    return ""


def read_log(testing_dir: Path, log_ref: str | None, max_bytes: int = 4_000_000) -> str:
    """Reads a run log named by a record (relative to testing/, e.g. 'runs/<id>-<target>.log' or a bare file name)."""
    if not log_ref:
        return ""
    name = Path(log_ref).name
    for candidate in (testing_dir / "runs" / name, testing_dir / name):
        try:
            if candidate.is_file():
                with open(candidate, "rb") as handle:
                    return handle.read(max_bytes).decode("utf-8", errors="replace")
        except OSError:
            continue
    return ""


# ----------------------------------------------------------------------------
# Syncing records produced in another checkout (gate worktree)
# ----------------------------------------------------------------------------

def sync_runs(src: Path, dst: Path, since: float) -> list[str]:
    """Copies run records and logs written after `since` from a workspace's testing/runs into the hub's runs dir."""
    copied: list[str] = []
    if not src.is_dir() or src.resolve() == dst.resolve():
        return copied
    dst.mkdir(parents=True, exist_ok=True)
    for entry in os.scandir(src):
        if not entry.is_file() or not (entry.name.endswith(".json") or entry.name.endswith(".log")):
            continue
        if entry.stat().st_mtime < since:
            continue
        target = dst / entry.name
        if not target.exists():
            shutil.copy2(entry.path, target)
            copied.append(entry.name)
    return copied


# ----------------------------------------------------------------------------
# Pre-push checklist
# ----------------------------------------------------------------------------

GATE_TARGETS = ("fabric-263", "neoforge-263")


def prepush(head: str, dirty: bool, records: list[dict], checks: list[dict]) -> dict:
    """Two green lights for the exact HEAD commit: Gradle check and the unfiltered 26.3 server suites."""
    head = head or ""
    check_items = [c for c in checks if c.get("commit") == head]
    latest_check = check_items[-1] if check_items else None
    check_ok = bool(latest_check and latest_check.get("exitCode") == 0 and not latest_check.get("dry"))

    suite = None
    for record in records:
        git = record["git"] or {}
        if git.get("commit") != head:
            continue
        ran = {t["id"] for t in selected_targets(record) if t["kind"] != "client"}
        if not all(t in ran for t in GATE_TARGETS):
            continue
        suite = record
        break
    suite_ok = bool(suite and verdict(suite) == "green" and full_coverage(suite) and not (suite["git"] or {}).get("dirty"))

    items = [
        {"id": "clean", "label": "Working tree clean", "ok": not dirty,
         "detail": "uncommitted changes: the results would not describe HEAD" if dirty else "clean"},
        {"id": "check", "label": "Gradle check green for HEAD", "ok": check_ok,
         "detail": (f"exit {latest_check.get('exitCode')} at {latest_check.get('at')}" if latest_check else "no check recorded for this commit"),
         "run": latest_check},
        {"id": "suite", "label": "26.3 server suites green for HEAD (unfiltered, nothing missing)", "ok": suite_ok,
         "detail": (f"run {suite['runId']}: {verdict(suite)}" + ("" if full_coverage(suite) else ", filtered or incomplete")
                    if suite else "no unfiltered fabric-263 + neoforge-263 run for this commit"),
         "runId": suite["runId"] if suite else None},
    ]
    return {"head": head, "ready": all(i["ok"] for i in items), "items": items}


def parse_iso(text: str | None) -> datetime | None:
    if not text:
        return None
    try:
        return datetime.strptime(text, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    except ValueError:
        return None
