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
    #: "server" proves itself through JUnit XML plus the catalogue cross-check.
    #: "client" drives a real Minecraft client and proves itself through screenshots.
    kind: str = "server"
    #: Client targets only: where the test sources live and where the shots land.
    sources: str = ""
    screenshots: str = ""


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
    # The client targets are not in the default selection: each boots a real Minecraft
    # client and takes minutes, where the whole server sweep takes about two. They run
    # on --release-gate, or when asked for by id.
    Target(
        id="client-fabric-262",
        label="Client Fabric - MC 26.2",
        loader="fabric",
        mc_line="26.2",
        gradle_task=":runClientGameTest",
        report="",
        catalogue="",
        kind="client",
        sources="src/gametest/java/com/simplebuilding/clienttest",
        screenshots="build/run/clientGameTest/screenshots",
    ),
    Target(
        id="client-neoforge-262",
        label="Client NeoForge - MC 26.2",
        loader="neoforge",
        mc_line="26.2",
        gradle_task=":neoforge:runClientGameTest",
        report="",
        catalogue="",
        kind="client",
        sources="neoforge/src/clientGameTest/java/com/simplebuilding/neoforge/clienttest",
        screenshots="neoforge/build/run/clientGameTest/screenshots",
    ),
    Target(
        id="client-fabric-12111",
        label="Client Fabric - MC 1.21.11",
        loader="fabric",
        mc_line="1.21.11",
        gradle_task=":mc1_21_11:fabric:runClientGameTest",
        report="",
        catalogue="",
        kind="client",
        sources="mc1_21_11/fabric/src/gametest/java/com/simplebuilding/clienttest",
        screenshots="mc1_21_11/fabric/build/run/clientGameTest/screenshots",
    ),
    Target(
        id="client-neoforge-12111",
        label="Client NeoForge - MC 1.21.11",
        loader="neoforge",
        mc_line="1.21.11",
        gradle_task=":mc1_21_11:neoforge:runClientGameTest",
        report="",
        catalogue="",
        kind="client",
        sources="mc1_21_11/neoforge/src/clientGameTest/java/com/simplebuilding/neoforge/clienttest",
        screenshots="mc1_21_11/neoforge/build/run/clientGameTest/screenshots",
    ),
)

BY_ID = {t.id: t for t in TARGETS}

#: The sweep a plain run does. Client targets are opt in, see their comment above.
DEFAULT_TARGETS = tuple(t for t in TARGETS if t.kind == "server")

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

#: A screenshot name as the client tests spell them: lowercase words joined by hyphens,
#: for example "highlight-c-sledgehammer". Tight enough that neither a resource id
#: ("minecraft:netherite_pickaxe") nor a sentence slips through.
#: A screenshot name: lowercase words joined by hyphens.
#:
#: Deliberately matched anywhere in a screenshot-taking file rather than only as the argument of
#: a screenshot call. Four of the checkpoints - the multi block breaking ones - hand their name
#: to a helper method that takes the shot, so a call-site-only pattern loses them, and losing a
#: real checkpoint is worse than one false name.
SHOT_NAME = re.compile(r'"([a-z][a-z0-9]*(?:-[a-z0-9]+)+)"')

#: A frame of Script.awaitStableFrame's stillness check: "settle<serial>x<attempt>a" or "...b".
SETTLE_SHOT = re.compile(r"settle\d+x\d+[ab]$")

#: The one false name that shape produces: a logger id. It is excluded by where it stands, not
#: by its spelling, so a future logger called something else is excluded too.
LOGGER_NAME = re.compile(r'getLogger\(\s*"([^"]+)"')

#: Client test bodies that every target shares. They live beside the loader specific drivers,
#: the same way the server tests live in common/src/shared/java, so their checkpoints have to be
#: counted for every client target - otherwise a shared test would look like a promise nobody made.
SHARED_CLIENT_SOURCES = {
    "26.2": "common/src/shared/clientgametest/java/com/simplebuilding/clientgametest",
    "1.21.11": "mc1_21_11/shared/clientgametest/java/com/simplebuilding/clientgametest",
}


def expected_shots(target: Target) -> list[str]:
    """The screenshots the client tests of this target say they will take.

    Read from the sources rather than from a list kept by hand, for the same reason the server
    targets read their catalogue: a list maintained separately drifts, and a drifted expectation
    is worse than none - it turns green.

    Two places are read: the loader's own test directory and the shared one for that Minecraft
    line. A test written once in the shared form has to count for every target that runs it.
    """
    directories = [REPO / target.sources]
    shared = SHARED_CLIENT_SOURCES.get(target.mc_line)
    if shared:
        directories.append(REPO / shared)

    names: set[str] = set()
    for directory in directories:
        if not directory.is_dir():
            continue
        for source in sorted(directory.glob("*.java")):
            text = source.read_text(encoding="utf-8", errors="replace")
            # Only files that actually take a screenshot; a helper beside them can hold strings
            # of the same shape without promising anything.
            if "takeScreenshot(" not in text and "shot(" not in text:
                continue
            names.update(SHOT_NAME.findall(text))
            names.difference_update(LOGGER_NAME.findall(text))
    return sorted(names)


def taken_shots(target: Target, not_older_than: float) -> tuple[list[str], list[str]]:
    """The screenshots that are on disk, split into fresh ones and leftovers."""
    directory = REPO / target.screenshots
    fresh: list[str] = []
    stale: list[str] = []
    if not directory.is_dir():
        return fresh, stale
    for shot in sorted(directory.glob("*.png")):
        # The harness numbers them: 0004_highlight-c-sledgehammer.png
        name = re.sub(r"^\d+_", "", shot.stem)
        (fresh if shot.stat().st_mtime >= not_older_than else stale).append(name)
    return fresh, stale


#: Log lines that mean "the client is showing a modal dialog and will never exit on its own".
#:
#: A client run that dies this way does not fail - it waits, and the whole timeout is spent
#: staring at a window nobody is looking at. Naming the cause turns 45 wasted minutes into a
#: sentence. The list is short on purpose: only patterns that can ONLY mean a blocking dialog.
HAENGER = (
    ("Error loading mods", "Der Mod-Ladefehler-Dialog steht offen - das Fenster wartet auf einen Klick."),
    ("is in a defined mixin package and cannot be referenced directly",
     "Eine Mixin-Konfiguration beansprucht ein ganzes Paket, in dem auch normale Klassen liegen. "
     "Der Accessor gehoert in ein eigenes Unterpaket."),
    ("Failed to start the minecraft server", "Der integrierte Server ist nicht hochgekommen."),
    ("A potential solution has been determined", "NeoForge zeigt seinen Fehlerbildschirm."),
)


def haenger_grund(log_text: str) -> str | None:
    """Names the reason a client run hung, if the log says one."""
    for muster, erklaerung in HAENGER:
        if muster in log_text:
            return f"Grund: {erklaerung} (Logzeile enthaelt \"{muster}\")"
    return None


def run_client_target(target: Target, run_id: str, timeout: int) -> dict:
    """Runs one client target and proves it by the screenshots it left behind.

    A client game test writes no JUnit report, so the exit code is all Gradle offers - and an
    exit code on its own is exactly the kind of evidence this runner exists to distrust. The
    screenshots are the substitute: every name the sources promise has to be on disk and newer
    than the start of this run. A test that died half way through takes the later shots with
    it, and the missing names say where it stopped.
    """
    started_at = now_utc()
    started_clock = time.time() - 1
    command = gradlew() + [target.gradle_task]
    exit_code, output, timed_out = run_capture(command, timeout)
    duration_ms = int((now_utc() - started_at).total_seconds() * 1000)

    log_path = RUNS_DIR / f"{run_id}-{target.id}.log"
    log_path.write_text(strip_ansi(output), encoding="utf-8")

    expected = expected_shots(target)
    fresh, _stale = taken_shots(target, started_clock)
    missing = [name for name in expected if name not in fresh]
    # The settle shots are the frames Script.awaitStableFrame compares before every scene; they
    # are named without a hyphen on purpose so they never read as a promised checkpoint, and
    # listing them as "unnamed" every run would bury a real stray screenshot among fifty of them.
    extra = [name for name in fresh if name not in expected and not SETTLE_SHOT.match(name)]

    error = None
    warning = None
    if timed_out:
        error = (f"Zeitgrenze von {timeout}s ueberschritten - der Lauf wurde abgebrochen. "
                 + (haenger_grund(output) or "Kein bekannter Aufhaenger im Log gefunden."))
    elif not expected:
        error = f"keine Screenshot-Namen in {target.sources} gefunden - der Beweis fehlt"
    elif missing:
        error = (
            f"{len(missing)} von {len(expected)} Screenshots fehlen oder sind alt, der Test kam "
            "nicht bis dorthin: " + ", ".join(missing)
        )
    elif exit_code != 0:
        tail = [l for l in strip_ansi(output).splitlines() if l.strip()][-3:]
        error = "Gradle brach ab: " + " | ".join(tail)
    if extra and not error:
        warning = (
            "Screenshots ohne Namen im Quelltext (vermutlich zusammengesetzt): "
            + ", ".join(extra)
        )

    passed = len(expected) - len(missing)
    return {
        "id": target.id,
        "label": target.label,
        "loader": target.loader,
        "mcLine": target.mc_line,
        "gradleTask": target.gradle_task,
        "kind": "client",
        "selected": True,
        "exitCode": exit_code,
        "durationMs": duration_ms,
        "log": log_path.name,
        "counts": {"total": len(expected), "passed": passed, "failed": len(missing), "foreign": len(extra)},
        "missing": missing,
        "unexpected": extra,
        # Each screenshot stands in for one checkpoint the test reached.
        "tests": [
            {
                "id": f"{MOD_ID}:{name}",
                "status": "passed" if name in fresh else "failed",
                "message": None if name in fresh else "kein frischer Screenshot",
                "durationMs": 0,
            }
            for name in expected
        ],
        "error": error,
        "warning": warning,
    }


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
    warning = None
    complete = report["fresh"] and not missing and not unexpected and bool(report["tests"])
    if timed_out and complete:
        # A timeout on top of a fresh, complete report is not a hung run: the tests all ran and
        # are accounted for. The usual cause is the machine sleeping mid run - one run in this
        # repo reported "8h 44m" for a target that takes 48 seconds awake. Calling that a hang
        # would send the next person hunting for a deadlock that is not there.
        warning = (
            f"Der Lauf brauchte laenger als die Zeitgrenze von {timeout}s, hat aber einen "
            "vollstaendigen und frischen Bericht geschrieben - vermutlich hat der Rechner "
            "zwischendurch geschlafen. Die Tests selbst sind ausgewertet."
        )
    elif timed_out:
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
        "warning": warning,
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
        if target.kind == "client":
            # A test selector means nothing here: the client tests are whole scenes, not a
            # catalogue of ids that can be picked from.
            target_records.append(run_client_target(target, run_id, timeout))
        else:
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
                    "warning": None,
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
    print(f"  {'Ziel':<30}{'Exit':>5}{'Tests':>7}{'gruen':>7}{'rot':>5}{'Dauer':>9}")
    print("  " + "-" * 63)
    for target in record["targets"]:
        if not target["selected"]:
            print(f"  {target['label']:<30}{'-':>5}{'uebersprungen':>28}")
            continue
        counts = target["counts"]
        state = "!" if (target["error"] or counts["failed"]) else ""
        print(
            f"  {target['label']:<30}"
            f"{target['exitCode']:>5}"
            f"{counts['total']:>7}"
            f"{counts['passed']:>7}"
            f"{counts['failed']:>5}"
            f"{target['durationMs'] / 1000:>8.1f}s"
            f" {state}"
        )
    print()

    for target in record["targets"]:
        if target.get("warning"):
            print(f"  HINWEIS {target['label']}: {target['warning']}")
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
        print(f"    {target.id:<23}{target.label:<28}{target.gradle_task}")
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

#: Where the two Minecraft lines deliberately test the same concern with different tests.
#:
#: Data driven villager trades only exist from MC 26.1 on. The older line builds its offers in
#: code, so several trade tests cannot share an id with their counterpart - they reach the same
#: statement through a different mechanism. Declaring the pairs says WHAT covers the concern on
#: the other side, which an "ignore this id" list would not.
#:
#: An entry with an empty side means the concern genuinely exists on one line only.
#: Anything not listed here makes the parity check fail - that is the point: an unported test
#: is invisible in a run where every target is green, because absence is what green cannot show.
LINE_DIFFERENCES: tuple[tuple[tuple[str, ...], tuple[str, ...], str], ...] = (
    (
        ("building_enchantment_game_test_constructors_touch_stick_cycles_the_first_block_state_property",),
        (),
        "MC 26.2 only: on 1.21.11 there is no shared ConstructorsTouchInteraction - the same "
        "logic sits twice in the loader modules, and a loader neutral test body cannot reach "
        "either copy",
    ),
    (
        ("config_option_game_test_trade_switch_conditions_still_name_real_config_fields_on_both_loaders",),
        (),
        "MC 26.2 only: reads the shipped trade jsons and their loader conditions, which the "
        "older line does not have",
    ),
    (
        ("trade_and_migration_game_test_all_mod_trades_are_loaded_into_the_datapack_registry",),
        (),
        "MC 26.2 only: there is no datapack trade registry on 1.21.11",
    ),
    (
        ("trade_and_migration_game_test_profession_trade_sets_resolve_the_mod_trades",),
        (),
        "MC 26.2 only: profession trade sets are data driven and do not exist on 1.21.11",
    ),
    (
        ("trade_registry_game_test_all_mod_trades_reach_the_registry",),
        ("trade_registry_game_test_all_mod_trades_resolve_against_the_server_registries",),
        "same concern - every mod trade is reachable - through the datapack registry on 26.2 "
        "and through the code built definitions on 1.21.11",
    ),
    (
        ("trade_and_migration_game_test_mod_trades_are_merged_into_the_vanilla_trade_pools",),
        ("trade_and_migration_game_test_mod_trades_are_merged_into_the_villager_trade_pools",
         "trade_and_migration_game_test_mod_trades_are_merged_into_the_wandering_trader_pools"),
        "same concern - the mod offers land in the vanilla pools - as one test on 26.2 and "
        "split in two on 1.21.11, where villager and wandering trader pools are separate lists",
    ),
)


#: How far each client target is behind the richest one right now, and why that is still
#: open. These numbers are debt, not permission: the gate stays red while any of them is above
#: zero. What they buy is a distinction the gate could not otherwise make - between the known
#: shortfall and a NEW one, which is the case P5 exists to catch.
#:
#: Fabric and NeoForge use different client test frameworks (NeoForge ships no client test API
#: at all, so its driver is a hand written step machine), which is why these could not simply be
#: copied across. The way out was to write each test once as a step list against a shared facade
#: and give each target a thin driver - work packages P2 and P3 in testing/PLAN.md, both done.
#: EMPTY since 2026-09-10, and that is the point of it being written down: all four client
#: targets now declare the same checkpoints (85 that morning, 102 by the evening - the number is
#: read from the sources, not kept here), because all four run the same shared test bodies. The three entries that used to be here (68, 73, 73) are paid, not forgiven. An
#: empty dict is not a switched off gate - the checks below still fail on any target that
#: falls behind, and now they fail immediately instead of against a tolerated number.
CLIENT_PARITY_DEBT: dict[str, int] = {}


def check_parity() -> tuple[bool, list[str]]:
    """Do the targets cover the SAME things?

    Every target being green says nothing about them testing the same suite. A test that
    exists on one Minecraft line and was never ported is green on one side and absent on the
    other, and absence is exactly what a green run cannot show. Same for the client targets,
    where the two loaders keep separate test sources.

    So this compares the two sets the runner already reads anyway - catalogue ids per line,
    screenshot names per client target - and fails on any difference that is not declared in
    LINE_DIFFERENCES.
    """
    notes: list[str] = []
    ok = True

    catalogue = read_catalogue()
    # read_catalogue keys its result by these two names, and the declarations below are written
    # in that order - side A is the newer line, side B the older one.
    newer, older = "26.2", "1.21.11"
    if set(catalogue) != {newer, older}:
        return False, [f"Server-Paritaet: Kataloge fuer {sorted(catalogue)} gefunden, "
                       f"erwartet waren {newer} und {older}"]

    ids = {line: {entry["id"].split(":", 1)[-1] for entry in entries}
           for line, entries in catalogue.items()}
    only_newer = ids[newer] - ids[older]
    only_older = ids[older] - ids[newer]

    declared_newer = {name for side_a, _, _ in LINE_DIFFERENCES for name in side_a}
    declared_older = {name for _, side_b, _ in LINE_DIFFERENCES for name in side_b}

    undeclared = ([(newer, t) for t in sorted(only_newer - declared_newer)]
                  + [(older, t) for t in sorted(only_older - declared_older)])
    for line, test in undeclared:
        other = older if line == newer else newer
        ok = False
        notes.append(
            f"Server-Paritaet: {test} gibt es nur auf MC {line}, nicht auf MC {other}. Entweder "
            "portieren, oder als Gegenstueck bzw. begruendete Ausnahme in LINE_DIFFERENCES "
            "eintragen und im Quelltext vermerken."
        )

    stale = sorted((declared_newer - only_newer) | (declared_older - only_older))
    if stale:
        ok = False
        notes.append(
            "Server-Paritaet: diese Eintraege in LINE_DIFFERENCES treffen nicht mehr zu - der "
            "Test laeuft inzwischen auf beiden Linien oder gar nicht mehr: " + ", ".join(stale)
        )

    if ok:
        notes.append(
            f"Server-Paritaet: in Ordnung - {len(ids[newer] & ids[older])} Tests mit derselben Id "
            f"auf beiden Linien, {len(LINE_DIFFERENCES)} erklaerte Unterschiede"
        )

    client_targets = [t for t in TARGETS if t.kind == "client"]
    shots = {t.id: set(expected_shots(t)) for t in client_targets}
    leer = sorted(t for t, s in shots.items() if not s)
    if leer:
        return False, notes + ["Client-Paritaet: keine Screenshot-Namen gefunden fuer "
                               + ", ".join(leer)]

    # Der reichste Satz ist der Massstab: dorthin sind die anderen zu bringen.
    richest = max(shots, key=lambda t: len(shots[t]))
    hinkt = {t: sorted(shots[richest] - shots[t]) for t in shots if t != richest}
    hinkt = {t: fehlt for t, fehlt in hinkt.items() if fehlt}
    if hinkt:
        ok = False
        for target, fehlt in sorted(hinkt.items()):
            bekannt = CLIENT_PARITY_DEBT.get(target)
            gezeigt = ", ".join(fehlt[:5]) + (f" (+{len(fehlt) - 5} weitere)" if len(fehlt) > 5 else "")
            if bekannt is None:
                notes.append(
                    f"Client-Paritaet: NEU - {target} hinkt {richest} um {len(fehlt)} Pruefpunkte "
                    f"hinterher, ohne dass das in CLIENT_PARITY_DEBT stuende: {gezeigt}"
                )
            elif len(fehlt) > bekannt:
                notes.append(
                    f"Client-Paritaet: GEWACHSEN - {target} hinkt jetzt um {len(fehlt)} statt um "
                    f"{bekannt} Pruefpunkte hinterher. Neu dazugekommen ist Abdeckung auf "
                    f"{richest}, die hier fehlt: {gezeigt}"
                )
            elif len(fehlt) < bekannt:
                notes.append(
                    f"Client-Paritaet: {target} hinkt nur noch um {len(fehlt)} statt um {bekannt} "
                    "Pruefpunkte hinterher - bitte CLIENT_PARITY_DEBT nachziehen, sonst kann die "
                    "Luecke unbemerkt wieder wachsen."
                )
            else:
                notes.append(
                    f"Client-Paritaet: {target} deckt {len(fehlt)} Pruefpunkte weniger ab als "
                    f"{richest} - bekannter Rueckstand, siehe testing/PLAN.md (P2/P3): {gezeigt}"
                )
    else:
        notes.append(
            f"Client-Paritaet: in Ordnung - alle {len(client_targets)} Ziele decken dieselben "
            f"{len(shots[richest])} Pruefpunkte ab"
        )

    beglichen = sorted(set(CLIENT_PARITY_DEBT) - set(hinkt))
    if beglichen:
        ok = False
        notes.append(
            "Client-Paritaet: diese Ziele stehen noch in CLIENT_PARITY_DEBT, hinken aber nicht "
            "mehr hinterher - Eintrag streichen: " + ", ".join(beglichen)
        )

    return ok, notes


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
        # The 1.21.11 client tests are a translated COPY of the shared tree, so an assertion
        # added on the 26.2 side is not on the other line until someone re-runs the port. The
        # parity gate below cannot see that: both trees would still promise the same
        # screenshots. This is the check that can - it re-translates and compares.
        ("tools/port_client_tests_to_1_21_11.py --check",
         [sys.executable, "tools/port_client_tests_to_1_21_11.py", "--check"]),
        # Same question for the server tests, asked of the BODIES: a class that exists on both
        # lines can still be a September copy on one of them. Fifteen classes were, for a week,
        # and neither the id parity nor the class list could tell.
        ("tools/port_tests_to_1_21_11.py --drift",
         [sys.executable, "tools/port_tests_to_1_21_11.py", "--drift"]),
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

    parity_ok, parity_notes = check_parity()
    notes.extend(parity_notes)
    ok = ok and parity_ok
    return ok, notes


# ----------------------------------------------------------------------------

def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Runs the SimpleBuilding in-game tests and records the result.",
    )
    parser.add_argument(
        "--targets",
        default="all",
        help=("comma separated target ids; 'all' (default) runs the four server targets, "
              "'client' only the client ones, 'everything' both: " + ", ".join(t.id for t in TARGETS)),
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
        return list(DEFAULT_TARGETS)
    if spec.strip() == "client":
        return [t for t in TARGETS if t.kind == "client"]
    if spec.strip() == "everything":
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
