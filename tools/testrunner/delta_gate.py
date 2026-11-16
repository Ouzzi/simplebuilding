"""Delta gate: rerun only what a change can have broken (owner 20.10.; standard bundle gate since N41, 13.11.).

    python tools/testrunner/delta_gate.py --since <last green ref> --ref <bundle ref> [--plan-only] [--due-file F]
    python tools/testrunner/delta_gate.py --base <red ref> --base-logs gate.log standalone.log --ref <fix ref> [--plan-only]

``--since`` (N41, bundle gate): the base is the last green state (GitHub master), no base logs. ``--base`` (old
mode): a fix on top of a red gate also reruns every test that was red at the base. Both run ``gradlew check``,
compile the touched Fabric projects, run ``data_integrity`` (plus ``test_centre`` when registrations change) and
the targets of the touched modules (plus the modules that require them, SimpleBuilding's own target when it
requires one, and the integration suite). Only the loaders in ``SB_GATE_LOADERS`` (default fabric: owner 22.10.,
26.3 Fabric only). A change that can reach everything (simplelib, wiringexample, core/framework Java, build
files, the test runner; N81: in simplelib/wiringexample only their Java and Gradle files) prints ``MODE full`` and exits 2: the caller runs the full gate then. With ``--due-file``
a due full gate (see gate_due.py) also prints ``MODE full``. Docs, queue, wiki and texture generators only need
``check``. The last line is ``VERDICT: GREEN`` or ``VERDICT: RED`` like the full gate's.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retry_red import blockers, red_tests  # noqa: E402

CHECK_ONLY = ("docs/", ".claude/", "wiki/", "art/", "previews/", "balance/", "bench-results/", "testing/runs/",
              "tools/textures/",  # generators only; the PNGs they write sit under modules/ or src/
              # web/infra tools no game test loads; tools/check_*.py are what check itself runs
              "tools/landing/", "tools/zentrale-web/", "tools/zentrale/", "tools/release_pages/", "tools/wiki/",
              "tools/ai/", "tools/check_", "tools/dup_pairs")
CHECK_ONLY_FILES = {"readme.md", "readme_modrinth.md", "readme_old.md", "todo.md", "MULTILOADER_TODO.md", "AGENTS.md",
                    "CLAUDE.md", "CHANGELOG.md", "AUDIT.md", "CREDITS.md", "CONTRIBUTING.md", "Wiki.md", ".gitignore"}
FULL_MODULES = {"simplelib", "wiringexample"}
# N81 (owner 16.11.): in the shared modules only code/build reaches everything; their lang/textures/data only run the module itself
FULL_MODULE_CODE = re.compile(r"modules/[^/]+/(?:.*/)?(?:java|kotlin)/|modules/[^/]+/.*\.(?:java|kt|gradle|gradle\.kts)$"
                              r"|modules/[^/]+/.*(?:mixins\.json|\.mod\.json|mods\.toml|\.accesswidener|accesstransformer\.cfg)$")
# probes, wiki data and notes of a module: no game test loads them
MODULE_CHECK_ONLY = re.compile(r"modules/[^/]+/(?:tools|wiki|docs)/")
# SimpleBuilding's own (client) game tests: the simplebuilding targets, not shared code
SB_TESTS = re.compile(r"common/src/[^/]+/(?:gametest|clientgametest)/")
MANIFEST = "modules/modules.json"
# SimpleBuilding's own resources (textures, models, data, generated files): the module "simplebuilding", not shared code
SB_RESOURCES = re.compile(r"(?:mc26_3/overlay/resources/|mc26_3/generated/|src/main/resources/|common/src/[^/]+/resources/)")
REGISTRY_FILE = re.compile(r"/(?:blockstates|items|loot_table|loot_tables|worldgen)/")
REGISTRY_LINE = re.compile(r"^[+-](?![+-]).*(?:\bregister\w*\s*\(|\bRegistry\.|DeferredRegister|\bRegistries\.)", re.M)
DATA_INTEGRITY = "simplebuilding:data_integrity*"
TEST_CENTRE = "simplebuilding:test_centre*"
CORE = ["fabric-263", "neoforge-263", "forge-263"]


def loaders() -> list[str]:
    return [l for l in os.environ.get("SB_GATE_LOADERS", "fabric").split(",") if l]


def git(*args: str) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def requires_map(manifest: dict) -> dict[str, set[str]]:
    return {m["id"]: set(m.get("requires", [])) for m in manifest["modules"]}


def manifest_changes(old: dict, new: dict) -> set[str]:
    """Ids of modules.json entries that were added or changed (removed ones have no targets left)."""
    before = {m["id"]: m for m in old.get("modules", [])}
    return {m["id"] for m in new.get("modules", []) if before.get(m["id"]) != m}


def plan(changed: list[str], manifest: dict, targets: list[str], loader_ids: list[str] | None = None,
         registry: bool = False, manifest_changed: set[str] | None = None) -> dict:
    """What a change touching these paths has to rerun; mode 'full' when it cannot be narrowed down.

    ``registry``: the diff adds/removes registrations (see registry_changed); then test_centre runs too.
    ``manifest_changed``: ids whose modules.json entry changed (manifest_changes); they run like changed modules."""
    loader_ids = loader_ids or loaders()
    lp = "|".join(map(re.escape, loader_ids))
    modules: set[str] = set()
    own_only: set[str] = set()  # shared modules with non-code changes: no dependants
    integration = False
    ids = {m["id"] for m in manifest["modules"]}
    for path in changed:
        if path.startswith(CHECK_ONLY) or path in CHECK_ONLY_FILES or MODULE_CHECK_ONLY.match(path):
            continue
        m = re.match(r"modules/([^/]+)/", path)
        if path == MANIFEST:
            modules |= (manifest_changed or set()) & ids
            integration = True
        elif m and m.group(1) in FULL_MODULES:
            if FULL_MODULE_CODE.match(path):
                return {"mode": "full", "reason": path}
            own_only.add(m.group(1))
            integration = True  # every mod uses them: at least the integration suite
        elif m:
            modules.add(m.group(1))
        elif SB_RESOURCES.match(path) or SB_TESTS.match(path):
            modules.add("simplebuilding")
        elif path.startswith("integration/"):
            integration = True
        else:
            return {"mode": "full", "reason": path}
    direct = sorted(modules)
    req = requires_map(manifest)
    grown = True
    while grown:  # a module that requires a changed one is changed too
        dependants = {mid for mid, needs in req.items() if needs & modules} - modules
        grown = bool(dependants)
        modules |= dependants
    modules |= own_only
    direct = sorted(set(direct) | own_only)
    run: list[str] = []
    if "simplebuilding" in modules:
        run += [c for c in CORE if c.split("-")[0] in loader_ids]
    for mod in sorted(modules - {"simplebuilding"}):
        run += [t for t in targets if re.fullmatch(rf"module-{re.escape(mod)}-(?:standalone-|with-[a-z0-9_-]+-)?(?:{lp})-263", t)]
    if run or integration:
        run.append("integration-263")
    filters: list[tuple[str, str]] = []
    core = f"{loader_ids[0]}-263"
    if (modules or integration) and core not in run:  # the full core target already holds both suites
        filters.append((core, DATA_INTEGRITY))
        if registry:
            filters.append((core, TEST_CENTRE))
    compile_: list[str] = []
    if modules or integration:
        compile_ = [f":mc26_3:{l}:compileJava" for l in loader_ids] + \
                   [f":modules:{m}:{l}:compileJava" for m in sorted(modules - {"simplebuilding"}) for l in loader_ids]
    return {"mode": "delta", "modules": sorted(modules), "changed_modules": direct, "targets": run,
            "filters": filters, "compile": compile_}


def registry_changed(name_status: str, diff_text: str) -> bool:
    """New/removed registrations: an added/deleted/renamed blockstate/item/loot/worldgen file, or a changed Java
    line that registers something."""
    for line in name_status.splitlines():
        parts = line.split("\t")
        if len(parts) >= 2 and parts[0][:1] in "ADR" and any(REGISTRY_FILE.search(p) for p in parts[1:]):
            return True
    return bool(REGISTRY_LINE.search(diff_text))


def existing_compile(tasks: list[str]) -> list[str]:
    """Only the compile tasks whose Gradle project directory exists at this ref."""
    return [t for t in tasks if Path(*t.split(":")[1:-1]).is_dir()]


def runner(args: list[str]) -> tuple[bool, str]:
    r = subprocess.run([sys.executable, "tools/testrunner/run.py", *args, "--trigger", "delta-gate"],
                       capture_output=True, text=True)
    green = re.search(r"alles gruen: \d+/\d+", r.stdout)
    return r.returncode == 0 and bool(green) and not red_tests(r.stdout), r.stdout


def run_tests(label: str, args: list[str]) -> bool:
    green, out = runner(args)
    m = re.search(r"(alles gruen|NICHT gruen): \d+/\d+[^\n]*", out)
    print(f"tests [{label}]:", m.group(0) if m else "NOT GREEN")
    if not green:
        print("\n".join(l for l in out.splitlines() if "ROT " in l or "FEHLER" in l)[:3000])
    return green


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    g = p.add_mutually_exclusive_group(required=True)
    g.add_argument("--since", help="last green ref (bundle gate, no base logs)")
    g.add_argument("--base", help="red gate ref (fix after a red gate, needs --base-logs)")
    p.add_argument("--ref", required=True)
    p.add_argument("--base-logs", nargs="+", type=Path, default=[])
    p.add_argument("--due-file", type=Path, help="full-due.json: a due full gate means MODE full")
    p.add_argument("--plan-only", action="store_true")
    a = p.parse_args(argv)
    base = a.since or a.base
    if a.base and not a.base_logs:
        p.error("--base needs --base-logs")

    if subprocess.run(["git", "merge-base", "--is-ancestor", base, a.ref]).returncode != 0:
        print(f"MODE full ({base} is not an ancestor of {a.ref})")
        return 2
    base_text = "\n".join(l.read_text(encoding="utf-8", errors="replace") for l in a.base_logs if l.exists())
    stop = [b for b in blockers(base_text) if b != "check failed"]
    if stop:
        print("MODE full (base gate: " + "; ".join(stop)[:200] + ")")
        return 2
    changed = git("diff", "--name-only", base, a.ref).split()
    registry = registry_changed(git("diff", "--name-status", base, a.ref),
                                git("diff", "-U0", base, a.ref, "--", "*.java"))
    manifest = json.loads(Path("modules/modules.json").read_text(encoding="utf-8"))
    listing = subprocess.run([sys.executable, "tools/testrunner/run.py", "--list"], capture_output=True, text=True).stdout
    targets = re.findall(r"^\s+((?:module-|integration-|fabric-|neoforge-|forge-)[a-z0-9_-]+)", listing, re.M)
    mchanged = None
    if MANIFEST in changed:
        try:
            mchanged = manifest_changes(json.loads(git("show", f"{base}:{MANIFEST}")), manifest)
        except (subprocess.CalledProcessError, ValueError):
            mchanged = None
        if mchanged is None:
            print(f"MODE full ({MANIFEST} not readable at {base})")
            return 2
    pl = plan(changed, manifest, targets, registry=registry, manifest_changed=mchanged)
    if pl["mode"] == "full":
        print(f"MODE full ({pl['reason']} changed)")
        return 2
    if a.due_file:
        import gate_due
        why = gate_due.due(gate_due.load(a.due_file), pl["changed_modules"])
        if why:
            print(f"MODE full (due: {why})")
            return 2
    reds = red_tests(base_text)
    print(f"MODE delta from {base}: {len(changed)} files, modules {pl['modules'] or '-'}, "
          f"{len(pl['targets'])} targets, {len(reds)} red tests of the base, registry {'yes' if registry else 'no'}")
    print("changed modules:", ",".join(pl["changed_modules"]) or "-")
    compile_ = existing_compile(pl["compile"])
    if a.plan_only:
        print("compile:", " ".join(compile_) or "-")
        print("targets:", ",".join(pl["targets"]) or "-")
        print("filters:", " ".join(f"{t}={f}" for t, f in pl["filters"]) or "-")
        return 0

    ok = True
    if compile_:
        r = subprocess.run(["./gradlew", *compile_, "-q"], capture_output=True, text=True)
        print("compile:", "OK" if r.returncode == 0 else f"FAILED ({r.returncode})")
        if r.returncode:
            print("\n".join(l for l in (r.stdout + r.stderr).splitlines() if "error" in l.lower())[:1500])
        ok &= r.returncode == 0
    r = subprocess.run(["./gradlew", "check", "-q"], capture_output=True, text=True)
    print("check:", "OK" if r.returncode == 0 else f"FAILED ({r.returncode})")
    ok &= r.returncode == 0
    for target, test in reds:
        green, out = runner(["--targets", target, "--filter", test])
        print(f"base red {target} {test}: {'green' if green else 'RED'}")
        ok &= green
        if not green:
            print("\n".join(l for l in out.splitlines() if "ROT " in l or "FEHLER" in l)[:1500])
    for target, flt in pl["filters"]:
        ok &= run_tests(f"{target} {flt}", ["--targets", target, "--filter", flt])
    if pl["targets"]:
        ok &= run_tests(",".join(pl["targets"]), ["--targets", ",".join(pl["targets"])])
    print("VERDICT:", "GREEN" if ok else "RED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
