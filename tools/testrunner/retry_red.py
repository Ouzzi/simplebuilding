"""Second try for the red tests of a gate (owner 20.10.): each red test runs once more on its own.

    python tools/testrunner/retry_red.py --flaky-log /root/gate/WACKEL-TESTS.md gate.log standalone.log

Reads the ``ROT <target> <test>`` lines the test runner prints (also inside the gate log). The gate may
only turn green when red tests are its sole problem: a failed ``check``, a ``FEHLER`` (crash, timeout,
missing report) or a red suite without ROT lines stays red, and so do more than ``--max`` red tests,
which point at a real break rather than a wobble. Every test that is green on its second run is
appended to the flaky log; the exit code is 0 only when all of them were.
"""

from __future__ import annotations

import argparse
import datetime
import re
import subprocess
import sys
from pathlib import Path

ROT = re.compile(r"^\s*ROT (\S+) (\S+)\s*$")
GREEN = re.compile(r"alles gruen: \d+/\d+")


def red_tests(text: str) -> list[tuple[str, str]]:
    """(target, test) per ROT line, in order, without duplicates."""
    seen: list[tuple[str, str]] = []
    for line in text.splitlines():
        m = ROT.match(line)
        if m and m.groups() not in seen:
            seen.append(m.groups())
    return seen


def blockers(text: str) -> list[str]:
    """Reasons a second try cannot rescue this log."""
    found = []
    if re.search(r"^check: FAILED", text, re.M):
        found.append("check failed")
    found += [line.strip() for line in text.splitlines() if line.lstrip().startswith("FEHLER ")]
    if re.search(r"NOT GREEN|NICHT gruen", text) and not red_tests(text):
        found.append("red without ROT lines")
    return found


def retry(target: str, test: str, timeout: int) -> bool:
    """One run of just this test on just this target; green when the runner says so."""
    cmd = [sys.executable, "tools/testrunner/run.py", "--targets", target, "--filter", test,
           "--trigger", "gate-retry", "--timeout", str(timeout)]
    r = subprocess.run(cmd, capture_output=True, text=True)
    return r.returncode == 0 and bool(GREEN.search(r.stdout)) and not red_tests(r.stdout)


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("logs", nargs="+", type=Path)
    p.add_argument("--flaky-log", type=Path, help="markdown file the wobbly tests are appended to")
    p.add_argument("--ref", default="?", help="commit the gate ran on, for the flaky log")
    p.add_argument("--max", type=int, default=6, help="more red tests than this: no second try")
    p.add_argument("--timeout", type=int, default=900)
    a = p.parse_args(argv)

    reds, stop = [], []
    for log in a.logs:
        text = log.read_text(encoding="utf-8", errors="replace") if log.exists() else ""
        stop += blockers(text)
        reds += [r for r in red_tests(text) if r not in reds]
    if stop:
        print("retry: none (" + "; ".join(stop)[:300] + ")")
        return 1
    if not reds:
        print("retry: nothing red")
        return 1
    if len(reds) > a.max:
        print(f"retry: none ({len(reds)} red tests > {a.max})")
        return 1

    flaky, still = [], []
    for target, test in reds:
        (flaky if retry(target, test, a.timeout) else still).append((target, test))
    for target, test in flaky:
        print(f"retry: green on 2nd run {target} {test}")
    for target, test in still:
        print(f"retry: red again {target} {test}")
    if flaky and a.flaky_log:
        new = not a.flaky_log.exists()
        with a.flaky_log.open("a", encoding="utf-8") as f:
            if new:
                f.write("# Wackel-Tests (vom Gate-Zweitversuch gefunden)\n\n| Datum | Ref | Ziel | Test |\n|---|---|---|---|\n")
            day = datetime.date.today().isoformat()
            for target, test in flaky:
                f.write(f"| {day} | {a.ref} | {target} | {test} |\n")
    return 0 if not still else 1


if __name__ == "__main__":
    sys.exit(main())
