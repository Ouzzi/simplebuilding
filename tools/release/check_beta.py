#!/usr/bin/env python3
"""Beta release consistency (owner W3B 15.11.2026: every Simple mod is first released as a beta).

    python3 tools/release/check_beta.py           check, exit 1 on any finding
    python3 tools/release/check_beta.py --table   also print the per-mod checklist (Markdown)

Checks, without Gradle:
  * every version is <major>.<minor>.<patch>-beta.<n>: modules/modules.json, gradle.properties mod_version (SimpleBuilding)
    and every ext.moduleVersion override in modules/<id>/<loader>/build.gradle (must equal modules.json);
  * every lower bound on a Simple mod is satisfied by that mod's beta version (a pre-release sorts below its release:
    0.4.0-beta.1 < 0.4.0, so a forgotten ">=0.4.0" / "[0.4.0,)" would refuse to start): fabric.mod.json depends/recommends/
    suggests, NeoForge/Forge TOML versionRange, and the TOML text the mc26_3 loader builds inject;
  * docs/release/CHANGELOG-<id>.md names exactly that version and says beta; docs/release/pages/<id>.md carries the
    "Release channel: Beta" block with the version. Example modules (never published) are skipped for the docs.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

R = Path(__file__).resolve().parents[2]
VERSION = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$")
BETA = re.compile(r"^\d+\.\d+\.\d+-beta\.\d+$")


def parse(v: str):
    m = VERSION.match(v.strip())
    if not m:
        raise ValueError(f"not a version: {v!r}")
    pre = m.group(4)
    ids = tuple((0, int(x), "") if x.isdigit() else (1, 0, x) for x in pre.split(".")) if pre else None
    return (int(m.group(1)), int(m.group(2)), int(m.group(3))), ids


def cmp(a: str, b: str) -> int:
    (ca, pa), (cb, pb) = parse(pad(a)), parse(pad(b))
    if ca != cb:
        return -1 if ca < cb else 1
    if pa == pb:
        return 0
    if pa is None:
        return 1          # release > pre-release
    if pb is None:
        return -1
    return -1 if pa < pb else 1


def pad(v: str) -> str:
    """'1' -> '1.0.0', '26.3' -> '26.3.0' (Maven/Fabric short forms)."""
    core, rest = re.match(r"^([0-9.]+)(.*)$", v.strip()).groups()
    parts = core.rstrip(".").split(".")
    while len(parts) < 3:
        parts.append("0")
    return ".".join(parts) + rest


def fabric_ok(constraint: str, version: str) -> bool:
    for token in constraint.split():
        if token == "*":
            continue
        m = re.match(r"^(>=|<=|>|<|=|\^|~)?(.+)$", token)
        op, v = m.group(1) or "=", m.group(2)
        if v.endswith(".x") or v == "x":
            continue
        c = cmp(version, v)
        if (op == ">=" and c < 0) or (op == ">" and c <= 0) or (op == "<=" and c > 0) or (op == "<" and c >= 0) or (op == "=" and c != 0):
            return False
        if op in ("^", "~") and c < 0:
            return False
    return True


def maven_ok(rng: str, version: str) -> bool:
    m = re.match(r"^([\[(])\s*([^,]*)\s*,\s*([^\])]*)\s*([\])])$", rng.strip())
    if not m:
        return cmp(version, rng) == 0 if rng.strip() else True
    lo_inc, lo, hi, hi_inc = m.group(1) == "[", m.group(2), m.group(3), m.group(4) == "]"
    if lo:
        c = cmp(version, lo)
        if c < 0 or (c == 0 and not lo_inc):
            return False
    if hi:
        c = cmp(version, hi)
        if c > 0 or (c == 0 and not hi_inc):
            return False
    return True


def main(argv: list[str]) -> int:
    props = (R / "gradle.properties").read_text(encoding="utf-8")
    mod_version = re.search(r"^mod_version=(.+)$", props, re.M).group(1).strip()
    mods = json.loads((R / "modules/modules.json").read_text(encoding="utf-8"))["modules"]
    version = {}
    for m in mods:
        v = mod_version if m["version"] == "${mod_version}" else m["version"]
        version[m["id"]] = v
        version[m.get("modId", m["id"])] = v
    bad: list[str] = []
    rows: list[str] = []
    for mid, v in sorted(set((m["id"], version[m["id"]]) for m in mods)):
        if not BETA.match(v):
            bad.append(f"{mid}: version {v} is not <x.y.z>-beta.<n>")

    # ext.moduleVersion overrides
    overrides: dict[str, list[str]] = {}
    for f in sorted(R.glob("modules/*/*/build.gradle")):
        mid = f.parent.parent.name
        m = re.search(r"ext\.moduleVersion\s*=\s*'([^']+)'", f.read_text(encoding="utf-8"))
        if m:
            overrides.setdefault(mid, []).append(f.parent.name)
            if mid in version and m.group(1) != version[mid]:
                bad.append(f"{f.relative_to(R)}: ext.moduleVersion {m.group(1)} != modules.json {version[mid]}")
    for f in (R / "gradle/module-neoforge.gradle", R / "gradle/module-forge.gradle", R / "gradle/module-fabric.gradle"):
        if "modules/modules.json" not in f.read_text(encoding="utf-8"):
            bad.append(f"{f.relative_to(R)}: version fallback does not read modules.json")

    # fabric.mod.json lower bounds
    bounds = 0
    def tracked(*patterns):  # fixed source locations only: no walk through build/ or run/ folders
        return sorted({f for pat in patterns for f in R.glob(pat)})
    fabric_files = tracked("src/*/resources/fabric.mod.json", "integration/src/*/resources/fabric.mod.json",
                           "mc*/*/src/*/resources/fabric.mod.json", "modules/*/*/src/main/resources/fabric.mod.json")
    for f in fabric_files:
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except ValueError:
            continue  # templated files with ${...} outside strings
        for section in ("depends", "recommends", "suggests", "breaks"):
            for dep, constraint in (data.get(section) or {}).items():
                if dep not in version:
                    continue
                constraints = constraint if isinstance(constraint, list) else [constraint]
                ok = any(fabric_ok(c, version[dep]) for c in constraints)
                if section == "breaks":
                    ok = not ok
                bounds += 1
                if not ok:
                    bad.append(f"{f.relative_to(R)}: {section} {dep} {constraint!r} not met by {version[dep]}")

    # TOML ranges (files + text injected by the mc26_3 builds)
    toml_files = tracked("*/src/*/resources/META-INF/*.toml", "mc*/*/src/*/resources/META-INF/*.toml",
                         "modules/*/*/src/main/resources/META-INF/*.toml")
    texts = [(f, f.read_text(encoding="utf-8")) for f in toml_files]
    for g in R.glob("mc*/*/build.gradle"):
        texts.append((g, g.read_text(encoding="utf-8").replace('\\"', '"').replace("\\n", "\n")))
    block = re.compile(r'modId\s*=\s*"([a-z0-9_]+)"(.*?)(?=modId\s*=|\[\[|\Z)', re.S)
    for f, text in texts:
        for m in block.finditer(text):
            dep = m.group(1)
            if dep not in version:
                continue
            r = re.search(r'versionRange\s*=\s*"([^"]*)"', m.group(2))
            if not r or "$" in r.group(1):
                continue
            bounds += 1
            if not maven_ok(r.group(1), version[dep]):
                bad.append(f"{f.relative_to(R)}: {dep} versionRange {r.group(1)} not met by {version[dep]}")

    # Release docs
    for m in mods:
        mid, v = m["id"], version[m["id"]]
        if m.get("example") or m.get("release") is False:
            rows.append(f"| {mid} | {v} | – | – | – | {'example, never published' if m.get('example') else 'not in the release (REL75)'} |")
            continue
        cl = R / f"docs/release/CHANGELOG-{mid}.md"
        page = R / f"docs/release/pages/{mid}.md"
        cl_ok = page_ok = False
        if not cl.exists():
            bad.append(f"missing {cl.relative_to(R)}")
        else:
            t = cl.read_text(encoding="utf-8")
            head = t.splitlines()[0] if t else ""
            cl_ok = f" {v} " in head + " " and "beta" in head.lower() and "Beta release" in t
            if not cl_ok:
                bad.append(f"{cl.relative_to(R)}: header/notes do not name {v} as beta")
        if not page.exists():
            bad.append(f"missing {page.relative_to(R)}")
        else:
            t = page.read_text(encoding="utf-8")
            page_ok = "## Release channel: Beta" in t and f"`{v}`" in t
            if not page_ok:
                bad.append(f"{page.relative_to(R)}: no 'Release channel: Beta' block with {v}")
        loaders = ", ".join(sorted(m["projects"]))
        ov = ", ".join(overrides.get(mid, [])) or "–"
        rows.append(f"| {mid} | {v} | {loaders} | {ov} | {'ok' if cl_ok else 'FEHLT'} | {'ok' if page_ok else 'FEHLT'} |")

    if "--table" in argv:
        print("| Mod | Version | Loader-Projekte | ext.moduleVersion in | Changelog Beta | Store-Seite Beta |")
        print("|---|---|---|---|---|---|")
        print("\n".join(rows))
    print("\n".join(bad) if bad else
          f"beta: {len(mods)} modules on -beta versions, {bounds} Simple-mod dependency bounds satisfied, changelogs and store pages say Beta")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
