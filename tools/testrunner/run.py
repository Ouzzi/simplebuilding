#!/usr/bin/env python3
"""
Runs the SimpleBuilding in-game tests and records what happened.

Why this exists: the mod ships four test targets (two loaders x two Minecraft
lines), each with its own Gradle task, its own log and its own JUnit report.
Driving them by hand means four invocations and then grepping four logs - and
the grepping is where mistakes happen. Two of them bit us repeatedly:

  * ``gradlew ... > log; echo $?`` reports the exit code of *echo*, so a failed
    build reads as a successful one.
  * A green Gradle run does not mean green tests, and a stale JUnit report from
    an earlier run looks exactly like a fresh all-green one.

So this script captures each exit code separately, checks that the report was
actually written by *this* run, and writes one machine readable record per run.
After that a single short table says whether everything is green, and the web
interface in ``testing/`` can show the history without anyone re-reading a log.

Usage
    python tools/testrunner/run.py                     # all four targets
    python tools/testrunner/run.py --targets fabric-262,neoforge-262
    python tools/testrunner/run.py --filter "simplebuilding:block_behaviour_*"
    python tools/testrunner/run.py --release-gate      # check + wiki + all tests
    python tools/testrunner/run.py --list              # show targets and catalogue
    python tools/testrunner/run.py --json              # only the record, no table

Selective runs go through the Gradle property ``-PgametestFilter``, which the
four run configurations translate into the mechanism their loader understands:
a ``--tests`` program argument for NeoForge, the JVM property
``fabric-api.gametest.filter`` for Fabric.

Outputs
    testing/runs/<runId>.json           the full record of one run
    testing/runs/<runId>-<target>.log   the raw Gradle output per target
    testing/data/runs.js                the last RUNS_IN_UI runs as window.TEST_RUNS
    testing/data/catalogue.js           every registered test as window.TEST_CATALOGUE

Exit code is 0 only when every selected target finished with exit code 0 and
zero failed tests. Anything else - a missing task, a missing report, a timeout,
a broken XML - is an error on that target and makes the run not ok.
"""

from __future__ import annotations

import argparse
import io
import json
import os
import re
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
TESTING = REPO / "testing"
RUNS_DIR = TESTING / "runs"
DATA_DIR = TESTING / "data"

#: How many runs the web interface carries inline. The page has to work when it
#: is opened straight from disk, where fetch() of a local file is blocked, so the
#: records are embedded rather than fetched - which is also why this is capped.
RUNS_IN_UI = 20

#: Per target. A hung Minecraft server would otherwise hold the whole run.
DEFAULT_TIMEOUT_SECONDS = 20 * 60

MOD_ID = "simplebuilding"


@dataclass(frozen=True)
class Target:
    """One of the four things that can be tested."""

    id: str
    label: str
    loader: str
    mc_line: str
    gradle_task: str
    report: str
    #: Which shared source tree this target's test catalogue comes from.
    catalogue: str


TARGETS: tuple[Target, ...] = (
    Target(
        id="fabric-262",
        label="Fabric - MC 26.2",
        loader="fabric",
        mc_line="26.2",
        gradle_task=":runGametest",
        report="build/junit.xml",
        catalogue="common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java",
    ),
    Target(
        id="neoforge-262",
        label="NeoForge - MC 26.2",
        loader="neoforge",
        mc_line="26.2",
        gradle_task=":neoforge:runGameTest",
        report="neoforge/build/neoforge-junit.xml",
        catalogue="common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java",
    ),
    Target(
        id="fabric-12111",
        label="Fabric - MC 1.21.11",
        loader="fabric",
        mc_line="1.21.11",
        gradle_task=":mc1_21_11:fabric:runGametest",
        report="mc1_21_11/fabric/build/junit.xml",
        catalogue="mc1_21_11/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java",
    ),
    Target(
        id="neoforge-12111",
        label="NeoForge - MC 1.21.11",
        loader="neoforge",
        mc_line="1.21.11",
        gradle_task=":mc1_21_11:neoforge:runGameTest",
        report="mc1_21_11/neoforge/build/neoforge-junit.xml",
        catalogue="mc1_21_11/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java",
    ),
)

BY_ID = {t.id: t for t in TARGETS}

_SPEC = re.compile(r'GameTestSpec\.named\(\s*"([^"]+)"\s*,\s*(\w+)::(\w+)\)')


# ----------------------------------------------------------------------------
# Kleinkram
# ----------------------------------------------------------------------------

def force_utf8_stdout() -> None:
    """Keeps a table with umlauts from killing the run on a cp1252 console."""
    for name in ("stdout", "stderr"):
        stream = getattr(sys, name)
        if isinstance(stream, io.TextIOWrapper) and (stream.encoding or "").lower() != "utf-8":
            stream.reconfigure(encoding="utf-8", errors="replace")


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def iso(moment: datetime) -> str:
    return moment.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def gradlew() -> list[str]:
    """The wrapper, spelled the way this platform can start it."""
    if os.name == "nt":
        return [str(REPO / "gradlew.bat")]
    return ["./gradlew"]


def run_capture(command: list[str], timeout: int) -> tuple[int, str, bool]:
    """Runs a command, returning (exit code, combined output, timed out).

    The exit code is taken from the process, never from a shell pipeline - that
    is the whole reason this helper exists.
    """
    try:
        finished = subprocess.run(
            command,
            cwd=str(REPO),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=timeout,
            check=False,
        )
    except subprocess.TimeoutExpired as expired:
        partial = expired.output or b""
        if isinstance(partial, str):
            partial = partial.encode("utf-8", "replace")
        return -1, partial.decode("utf-8", "replace"), True
    except OSError as error:
        return -1, f"{command[0]} konnte nicht gestartet werden: {error}", False
    return finished.returncode, finished.stdout.decode("utf-8", "replace"), False


ANSI = re.compile(r"\x1b\[[0-9;]*m")


def strip_ansi(text: str) -> str:
    return ANSI.sub("", text)


# ----------------------------------------------------------------------------
# Umgebung: git, Version, Testkatalog
# ----------------------------------------------------------------------------

def git_state() -> dict:
    def git(*args: str) -> str:
        code, out, _ = run_capture(["git", *args], timeout=30)
        return out.strip() if code == 0 else ""

    commit = git("rev-parse", "HEAD")
    return {
        "commit": commit,
        "short": commit[:7],
        "branch": git("rev-parse", "--abbrev-ref", "HEAD"),
        "dirty": bool(git("status", "--porcelain")),
    }


def gradle_properties() -> dict[str, str]:
    props: dict[str, str] = {}
    path = REPO / "gradle.properties"
    if not path.exists():
        return props
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        props[key.strip()] = value.strip()
    return props


def read_catalogue() -> dict[str, list[dict]]:
    """Every registered test id, per Minecraft line, read from the Java catalogue.

    The catalogue is the single place both loaders register from, so it is also
    the honest answer to "which tests should exist" - which is what tells the
    interface apart a test that passed from one that was filtered out.
    """
    out: dict[str, list[dict]] = {}
    for line in ("26.2", "1.21.11"):
        target = next(t for t in TARGETS if t.mc_line == line)
        path = REPO / target.catalogue
        entries: list[dict] = []
        if path.exists():
            text = path.read_text(encoding="utf-8", errors="replace")
            for name, test_class, method in _SPEC.findall(text):
                entries.append({"id": f"{MOD_ID}:{name}", "testClass": test_class, "method": method})
        out[line] = entries
    return out


# ----------------------------------------------------------------------------
# JUnit-Bericht auswerten
# ----------------------------------------------------------------------------

def parse_report(path: Path, not_older_than: float) -> dict:
    """Reads one JUnit report and says how trustworthy it is.

    ``not_older_than`` is the wall clock at which this run started. A report
    whose file is older than that belongs to an earlier run: reporting its
    contents would turn a broken run into a green one, which is exactly the
    mistake this guards against.
    """
    result = {
        "exists": path.exists(),
        "fresh": False,
        "tests": [],
        "counts": {"total": 0, "passed": 0, "failed": 0, "foreign": 0},
        "error": None,
    }
    if not path.exists():
        result["error"] = f"kein JUnit-Bericht unter {path.relative_to(REPO).as_posix()} geschrieben"
        return result

    mtime = path.stat().st_mtime
    result["fresh"] = mtime >= not_older_than
    if not result["fresh"]:
        result["error"] = (
            f"der Bericht {path.relative_to(REPO).as_posix()} ist aelter als dieser Lauf "
            f"(geschrieben {iso(datetime.fromtimestamp(mtime, timezone.utc))}) - "
            "der Testlauf hat also nichts geschrieben"
        )
        return result

    try:
        root = ElementTree.parse(path).getroot()
    except ElementTree.ParseError as error:
        result["error"] = f"JUnit-Bericht {path.relative_to(REPO).as_posix()} ist nicht lesbar: {error}"
        return result

    for case in root.iter("testcase"):
        name = case.get("name") or ""
        failure = case.find("failure")
        error_node = case.find("error")
        problem = failure if failure is not None else error_node
        try:
            time_ms = int(round(float(case.get("time") or 0) * 1000))
        except ValueError:
            time_ms = 0
        entry = {
            "id": name,
            "status": "failed" if problem is not None else "passed",
            "timeMs": time_ms,
            "message": (problem.get("message") if problem is not None else None),
            "type": (problem.get("type") if problem is not None else None),
        }
        # Fabric runs in the minecraft:default environment and picks up one test
        # that does not belong to this mod. It is kept for the record but must
        # never move the mod's numbers.
        entry["foreign"] = not name.startswith(f"{MOD_ID}:")
        result["tests"].append(entry)

    own = [t for t in result["tests"] if not t["foreign"]]
    result["counts"] = {
        "total": len(own),
        "passed": sum(1 for t in own if t["status"] == "passed"),
        "failed": sum(1 for t in own if t["status"] == "failed"),
        "foreign": len(result["tests"]) - len(own),
    }
    return result


# ----------------------------------------------------------------------------
# Der Lauf
# ----------------------------------------------------------------------------

def new_run_id(started: datetime) -> str:
    return started.strftime("%Y-%m-%dT%H-%M-%SZ") + "-" + uuid.uuid4().hex[:4]


def run_target(
    target: Target,
    run_id: str,
    test_filter: str | None,
    timeout: int,
    on_line: callable | None = None,
) -> dict:
    """Runs one target and returns its part of the record."""
    command = [*gradlew(), target.gradle_task]
    if test_filter:
        command.append(f"-PgametestFilter={test_filter}")

    started_at = now_utc()
    # A one second slack: the report is written moments before Gradle returns,
    # and file system timestamps are not always finer grained than a second.
    fresh_after = time.time() - 1.0

    if on_line:
        on_line(f"$ {' '.join(command)}")
    exit_code, output, timed_out = run_capture(command, timeout)
    duration_ms = int((now_utc() - started_at).total_seconds() * 1000)

    RUNS_DIR.mkdir(parents=True, exist_ok=True)
    log_path = RUNS_DIR / f"{run_id}-{target.id}.log"
    log_path.write_text(strip_ansi(output), encoding="utf-8")

    report = parse_report(REPO / target.report, fresh_after)

    # Was ran versus what the catalogue says exists. Both directions matter and
    # both have actually bitten: a test id that only one loader registers looks
    # like a passing test on one side and like nothing at all on the other,
    # because Fabric derives its ids from the adapter method name (it collapses
    # "ABlock" into "ablock") while NeoForge takes the catalogue name verbatim.
    expected = {e["id"] for e in read_catalogue().get(target.mc_line, [])}
    ran_ids = {t["id"] for t in report["tests"] if not t["foreign"]}
    missing = sorted(expected - ran_ids)
    unexpected = sorted(ran_ids - expected)

    error = None
    if timed_out:
        error = f"Zeitgrenze von {timeout}s ueberschritten - der Lauf wurde abgebrochen"
    elif exit_code != 0 and not report["tests"]:
        # Gradle failed before any test ran: task unknown, compile error, and so on.
        tail = [l for l in strip_ansi(output).splitlines() if l.strip()][-3:]
        error = "Gradle brach ab, ohne Tests zu starten: " + " | ".join(tail)
    elif report["error"]:
        error = report["error"]
    elif unexpected:
        error = (
            "diese Tests liefen, stehen aber nicht im Katalog - Katalog und Adapter "
            "sind auseinander: " + ", ".join(unexpected)
        )
    elif missing and not test_filter:
        error = (
            "ohne Auswahl haetten alle Tests laufen muessen, diese fehlen im Bericht: "
            + ", ".join(missing)
        )

    return {
        "id": target.id,
        "label": target.label,
        "loader": target.loader,
        "mcLine": target.mc_line,
        "gradleTask": target.gradle_task,
        "selected": True,
        "exitCode": exit_code,
        "durationMs": duration_ms,
        "reportPath": target.report,
        "reportFresh": report["fresh"],
        "logPath": f"runs/{log_path.name}",
        "counts": report["counts"],
        "expected": len(expected),
        "missing": missing,
        "unexpected": unexpected,
        "tests": report["tests"],
        "error": error,
    }


def execute(
    selected: list[Target],
    test_filter: str | None,
    trigger: str,
    timeout: int,
    on_line: callable | None = None,
    on_target: callable | None = None,
) -> dict:
    """Runs every selected target and writes the record plus the interface data."""
    started = now_utc()
    run_id = new_run_id(started)
    props = gradle_properties()

    target_records: list[dict] = []
    for target in selected:
        if on_target:
            on_target(target)
        target_records.append(run_target(target, run_id, test_filter, timeout, on_line))

    for target in TARGETS:
        if target not in selected:
            target_records.append(
                {
                    "id": target.id,
                    "label": target.label,
                    "loader": target.loader,
                    "mcLine": target.mc_line,
                    "gradleTask": target.gradle_task,
                    "selected": False,
                    "exitCode": None,
                    "durationMs": 0,
                    "reportPath": target.report,
                    "reportFresh": False,
                    "logPath": None,
                    "counts": {"total": 0, "passed": 0, "failed": 0, "foreign": 0},
                    "expected": len(read_catalogue().get(target.mc_line, [])),
                    "missing": [],
                    "unexpected": [],
                    "tests": [],
                    "error": None,
                }
            )
    order = {t.id: i for i, t in enumerate(TARGETS)}
    target_records.sort(key=lambda r: order[r["id"]])

    ran = [r for r in target_records if r["selected"]]
    finished = now_utc()
    record = {
        "schema": 1,
        "runId": run_id,
        "startedAt": iso(started),
        "finishedAt": iso(finished),
        "durationMs": int((finished - started).total_seconds() * 1000),
        "trigger": trigger,
        "filter": test_filter,
        "git": git_state(),
        "modVersion": props.get("mod_version", "?"),
        "mcVersions": {
            "26.2": props.get("minecraft_version", "26.2"),
            "1.21.11": props.get("mc11_minecraft_version", "1.21.11"),
        },
        "targets": target_records,
        "totals": {
            "total": sum(r["counts"]["total"] for r in ran),
            "passed": sum(r["counts"]["passed"] for r in ran),
            "failed": sum(r["counts"]["failed"] for r in ran),
        },
        "ok": bool(ran)
        and all(r["exitCode"] == 0 and r["counts"]["failed"] == 0 and not r["error"] for r in ran),
    }

    write_record(record)
    return record


def write_record(record: dict) -> None:
    RUNS_DIR.mkdir(parents=True, exist_ok=True)
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    # Write to a temporary name first: a half written JSON in the archive would
    # be picked up by the next call to refresh_ui_data and break the interface.
    target = RUNS_DIR / f"{record['runId']}.json"
    temporary = target.with_suffix(".json.part")
    temporary.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(target)

    refresh_ui_data()


def refresh_ui_data() -> None:
    """Regenerates the two .js files the web interface loads."""
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    records: list[dict] = []
    for path in sorted(RUNS_DIR.glob("*.json"), reverse=True):
        if len(records) >= RUNS_IN_UI:
            break
        try:
            records.append(json.loads(path.read_text(encoding="utf-8")))
        except (OSError, json.JSONDecodeError):
            continue
    records.sort(key=lambda r: r.get("startedAt", ""), reverse=True)

    header = (
        "// Erzeugt von tools/testrunner/run.py - nicht von Hand pflegen.\n"
        "// Die Datensaetze stehen hier vollstaendig drin, damit testing/index.html\n"
        "// auch per file:// funktioniert, wo fetch() auf lokale Dateien blockiert ist.\n"
    )
    (DATA_DIR / "runs.js").write_text(
        header + "window.TEST_RUNS = " + json.dumps(records, ensure_ascii=False, indent=2) + ";\n",
        encoding="utf-8",
    )
    (DATA_DIR / "catalogue.js").write_text(
        "// Erzeugt von tools/testrunner/run.py aus SimpleBuildingGameTests.java.\n"
        "window.TEST_CATALOGUE = "
        + json.dumps(read_catalogue(), ensure_ascii=False, indent=2)
        + ";\n",
        encoding="utf-8",
    )


def load_runs() -> list[dict]:
    records: list[dict] = []
    for path in sorted(RUNS_DIR.glob("*.json"), reverse=True)[:RUNS_IN_UI]:
        try:
            records.append(json.loads(path.read_text(encoding="utf-8")))
        except (OSError, json.JSONDecodeError):
            continue
    records.sort(key=lambda r: r.get("startedAt", ""), reverse=True)
    return records


# ----------------------------------------------------------------------------
# Ausgabe
# ----------------------------------------------------------------------------

def print_table(record: dict) -> None:
    filter_text = record["filter"] or "alles"
    git = record["git"]
    print()
    print(f"  Lauf {record['runId']}")
    print(
        f"  {record['modVersion']} | {git['short']}"
        f"{' (schmutzig)' if git['dirty'] else ''} auf {git['branch'] or '?'}"
        f" | Auswahl: {filter_text}"
    )
    print()
    print(f"  {'Ziel':<22}{'Exit':>5}{'Tests':>7}{'gruen':>7}{'rot':>5}{'Dauer':>9}")
    print("  " + "-" * 55)
    for target in record["targets"]:
        if not target["selected"]:
            print(f"  {target['label']:<22}{'-':>5}{'uebersprungen':>28}")
            continue
        counts = target["counts"]
        state = "!" if (target["error"] or counts["failed"]) else ""
        print(
            f"  {target['label']:<22}"
            f"{target['exitCode']:>5}"
            f"{counts['total']:>7}"
            f"{counts['passed']:>7}"
            f"{counts['failed']:>5}"
            f"{target['durationMs'] / 1000:>8.1f}s"
            f" {state}"
        )
    print()

    for target in record["targets"]:
        if target["error"]:
            print(f"  FEHLER {target['label']}: {target['error']}")
        for test in target["tests"]:
            if test["status"] == "failed":
                print(f"  ROT {target['id']} {test['id']}")
                if test["message"]:
                    print(f"      {test['message']}")
    if not record["ok"]:
        print()
    totals = record["totals"]
    verdict = "alles gruen" if record["ok"] else "NICHT gruen"
    print(f"  {verdict}: {totals['passed']}/{totals['total']} bestanden, {totals['failed']} rot")
    print(f"  Datensatz: testing/runs/{record['runId']}.json")
    print()


def print_list() -> None:
    print()
    print("  Ziele")
    for target in TARGETS:
        print(f"    {target.id:<16}{target.label:<22}{target.gradle_task}")
    catalogue = read_catalogue()
    print()
    print("  Testkatalog")
    for line, entries in catalogue.items():
        by_class: dict[str, int] = {}
        for entry in entries:
            by_class[entry["testClass"]] = by_class.get(entry["testClass"], 0) + 1
        print(f"    MC {line}: {len(entries)} Tests")
        for test_class, count in sorted(by_class.items()):
            prefix = entries[0]["id"].split(":")[0]
            hint = derive_selector(test_class, entries)
            print(f"      {test_class:<32}{count:>3}   {prefix}:{hint}")
    print()


def derive_selector(test_class: str, entries: list[dict]) -> str:
    """A wildcard that selects exactly one test class.

    Every id is derived from its class name, so the shared prefix of a class's
    ids plus a star is a selector for that class - no hand maintained mapping.
    """
    ids = [e["id"].split(":", 1)[1] for e in entries if e["testClass"] == test_class]
    if not ids:
        return "*"
    prefix = os.path.commonprefix(ids)
    return (prefix or "") + "*"


# ----------------------------------------------------------------------------
# Release-Tor
# ----------------------------------------------------------------------------

def release_gate(timeout: int) -> tuple[bool, list[str]]:
    """Everything that has to be green before a version leaves the house.

    Runs before the game tests, because a failing unit test or an out of date
    wiki is cheap to find and would make the four Minecraft runs a waste of time.
    """
    notes: list[str] = []
    ok = True
    for label, command in (
        ("gradlew check", [*gradlew(), "check"]),
        ("wiki/generate.py --check", [sys.executable, "wiki/generate.py", "--check"]),
    ):
        code, output, timed_out = run_capture(command, timeout)
        if timed_out:
            notes.append(f"{label}: Zeitgrenze ueberschritten")
            ok = False
        elif code != 0:
            tail = [l for l in strip_ansi(output).splitlines() if l.strip()][-4:]
            notes.append(f"{label}: Exit {code} - " + " | ".join(tail))
            ok = False
        else:
            notes.append(f"{label}: in Ordnung")
    return ok, notes


# ----------------------------------------------------------------------------

def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Runs the SimpleBuilding in-game tests and records the result.",
    )
    parser.add_argument(
        "--targets",
        default="all",
        help="comma separated target ids, or 'all' (default): " + ", ".join(t.id for t in TARGETS),
    )
    parser.add_argument(
        "--filter",
        default=None,
        help='test selector with wildcards, e.g. "simplebuilding:block_behaviour_*"',
    )
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT_SECONDS,
                        help="seconds per target before the run is given up on")
    parser.add_argument("--release-gate", action="store_true",
                        help="run gradlew check and the wiki check first, then every target")
    parser.add_argument("--list", action="store_true", help="show targets and catalogue, run nothing")
    parser.add_argument("--json", action="store_true", help="print the record as JSON instead of a table")
    parser.add_argument("--trigger", default="cli", help="what started this run (recorded in the run)")
    return parser.parse_args(argv)


def select_targets(spec: str) -> list[Target]:
    if spec.strip() in ("all", ""):
        return list(TARGETS)
    chosen: list[Target] = []
    for part in spec.split(","):
        key = part.strip()
        if not key:
            continue
        if key not in BY_ID:
            raise SystemExit(f"unbekanntes Ziel {key!r}; moeglich sind: " + ", ".join(BY_ID))
        chosen.append(BY_ID[key])
    if not chosen:
        raise SystemExit("keine Ziele ausgewaehlt")
    return chosen


def main(argv: list[str] | None = None) -> int:
    force_utf8_stdout()
    args = parse_args(argv)

    if args.list:
        print_list()
        return 0

    gate_ok, gate_notes = True, []
    if args.release_gate:
        if args.filter:
            raise SystemExit("--release-gate laeuft immer ueber alles; --filter passt nicht dazu")
        print("  Release-Tor: Vorpruefungen ...")
        gate_ok, gate_notes = release_gate(args.timeout)
        for note in gate_notes:
            print(f"    {note}")
        if not gate_ok:
            print()
            print("  NO-GO: die Vorpruefungen sind nicht gruen, die Spieltests laufen gar nicht erst.")
            return 1

    selected = list(TARGETS) if args.release_gate else select_targets(args.targets)
    trigger = "release-gate" if args.release_gate else args.trigger

    record = execute(selected, args.filter, trigger, args.timeout,
                     on_line=None if args.json else lambda line: print(f"  {line}"))

    if args.json:
        print(json.dumps(record, ensure_ascii=False, indent=2))
    else:
        print_table(record)
        if args.release_gate:
            everything = len(selected) == len(TARGETS) and not record["filter"]
            if record["ok"] and everything:
                print("  GO: alle vier Ziele gruen, Vorpruefungen gruen - bereit fuer Push und Upload.")
            else:
                print("  NO-GO: siehe oben.")
            print()

    return 0 if record["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
