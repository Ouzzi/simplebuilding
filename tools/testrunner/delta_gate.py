"""Delta gate (owner 20.10.): after a red gate, a fix only reruns what it can have changed.

    python tools/testrunner/delta_gate.py --base <red ref> --base-logs gate.log standalone.log --ref <fix ref> [--plan-only]

The fix must sit on top of the red gate's ref. It then runs ``gradlew check``, every test that was red
at the base, and the targets of the modules the fix touches (plus the modules that require them,
SimpleBuilding's own targets when it requires one, and the integration suite). A change outside
``modules/<id>/`` that can reach everything (common code, simplelib, framework, build files, the test
runner) prints ``MODE full`` and runs nothing: the caller runs the full gate then. Docs, queue and wiki
only need ``check``. The last line is ``VERDICT: GREEN`` or ``VERDICT: RED`` like the full gate's.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retry_red import blockers, red_tests  # noqa: E402

CHECK_ONLY = ("docs/", ".claude/", "wiki/", "art/", "previews/", "balance/", "bench-results/", "testing/runs/")
CHECK_ONLY_FILES = {"readme.md", "readme_modrinth.md", "readme_old.md", "todo.md", "MULTILOADER_TODO.md", "AGENTS.md", "CLAUDE.md"}
FULL_MODULES = {"simplelib", "wiringexample"}
CORE = ["fabric-263", "neoforge-263", "forge-263"]


def git(*args: str) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def requires_map(manifest: dict) -> dict[str, set[str]]:
    return {m["id"]: set(m.get("requires", [])) for m in manifest["modules"]}


def plan(changed: list[str], manifest: dict, targets: list[str]) -> dict:
    """What a fix touching these paths has to rerun; mode 'full' when it cannot be narrowed down."""
    modules: set[str] = set()
    for path in changed:
        if path.startswith(CHECK_ONLY) or path in CHECK_ONLY_FILES:
            continue
        m = re.match(r"modules/([^/]+)/", path)
        if not m or m.group(1) in FULL_MODULES:
            return {"mode": "full", "reason": path}
        modules.add(m.group(1))
    req = requires_map(manifest)
    grown = True
    while grown:  # a module that requires a changed one is changed too
        dependants = {mid for mid, needs in req.items() if needs & modules} - modules
        grown = bool(dependants)
        modules |= dependants
    run: list[str] = []
    if "simplebuilding" in modules:
        run += CORE
    for mod in sorted(modules - {"simplebuilding"}):
        run += [t for t in targets if re.fullmatch(rf"module-{mod}-(?:standalone-)?(?:fabric|neoforge|forge)-263", t)]
    if run:
        run.append("integration-263")
    return {"mode": "delta", "modules": sorted(modules), "targets": run}


def runner(args: list[str]) -> tuple[bool, str]:
    r = subprocess.run([sys.executable, "tools/testrunner/run.py", *args, "--trigger", "delta-gate"],
                       capture_output=True, text=True)
    green = re.search(r"alles gruen: \d+/\d+", r.stdout)
    return r.returncode == 0 and bool(green) and not red_tests(r.stdout), r.stdout


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--base", required=True)
    p.add_argument("--ref", required=True)
    p.add_argument("--base-logs", nargs="+", type=Path, required=True)
    p.add_argument("--plan-only", action="store_true")
    a = p.parse_args(argv)

    if subprocess.run(["git", "merge-base", "--is-ancestor", a.base, a.ref]).returncode != 0:
        print(f"MODE full ({a.base} is not an ancestor of {a.ref})")
        return 2
    base_text = "\n".join(l.read_text(encoding="utf-8", errors="replace") for l in a.base_logs if l.exists())
    stop = [b for b in blockers(base_text) if b != "check failed"]
    if stop:
        print("MODE full (base gate: " + "; ".join(stop)[:200] + ")")
        return 2
    changed = git("diff", "--name-only", a.base, a.ref).split()
    manifest = json.loads(Path("modules/modules.json").read_text(encoding="utf-8"))
    listing = subprocess.run([sys.executable, "tools/testrunner/run.py", "--list"], capture_output=True, text=True).stdout
    targets = re.findall(r"^\s+((?:module-|integration-|fabric-|neoforge-|forge-)[a-z0-9_-]+)", listing, re.M)
    pl = plan(changed, manifest, targets)
    if pl["mode"] == "full":
        print(f"MODE full ({pl['reason']} changed)")
        return 2
    reds = red_tests(base_text)
    print(f"MODE delta from {a.base}: {len(changed)} files, modules {pl['modules'] or '-'}, "
          f"{len(pl['targets'])} targets, {len(reds)} red tests of the base")
    if a.plan_only:
        print("targets:", ",".join(pl["targets"]) or "-")
        return 0

    ok = True
    r = subprocess.run(["./gradlew", "check", "-q"], capture_output=True, text=True)
    print("check:", "OK" if r.returncode == 0 else f"FAILED ({r.returncode})")
    ok &= r.returncode == 0
    for target, test in reds:
        green, out = runner(["--targets", target, "--filter", test])
        print(f"base red {target} {test}: {'green' if green else 'RED'}")
        ok &= green
        if not green:
            print("\n".join(l for l in out.splitlines() if "ROT " in l or "FEHLER" in l)[:1500])
    if pl["targets"]:
        green, out = runner(["--targets", ",".join(pl["targets"])])
        m = re.search(r"(alles gruen|NICHT gruen): \d+/\d+[^\n]*", out)
        print(f"tests [{','.join(pl['targets'])}]:", m.group(0) if m else "NOT GREEN")
        ok &= green
        if not green:
            print("\n".join(l for l in out.splitlines() if "ROT " in l or "FEHLER" in l)[:3000])
    print("VERDICT:", "GREEN" if ok else "RED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
