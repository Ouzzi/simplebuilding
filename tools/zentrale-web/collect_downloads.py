#!/usr/bin/env python3
"""Collects the release jars of a gate-green checkout into the Zentrale download folder.

Run in the gate checkout right after a GREEN gate (+ green standalone run), before the next job rebuilds:

    python3 tools/zentrale-web/collect_downloads.py --out <downloads dir> --ref <ref> \
        --verdict "VERDICT: GREEN" --standalone "alles gruen" --since <gate start, ISO>

Layout of <out>: ``index.json`` (manifest read by the Downloads tab) and ``files/<commit>/<loader>/<mc>/<jar>``.
Only gate-green builds are accepted; the newest ``--keep`` builds stay, older files are removed.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tomllib
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import modselect  # noqa: E402

SKIP = re.compile(r"-(sources|dev|javadoc|slim|shadow|dev-shadow|plain)\.jar$")
# SimpleBuilding builds outside modules.json (other Minecraft lines); the 26.3 projects come from modules.json.
SB_EXTRA = [
    ("build/libs", "fabric", "minecraft_version"),
    ("neoforge/build/libs", "neoforge", "minecraft_version"),
    ("forge/build/libs", "forge", "minecraft_version"),
    ("mc1_21_11/fabric/build/libs", "fabric", "mc11_minecraft_version"),
    ("mc1_21_11/neoforge/build/libs", "neoforge", "mc11_minecraft_version"),
    ("mc26_4/fabric/build/libs", "fabric", "mc264_minecraft_version"),
    ("mc26_4/forge/build/libs", "forge", "mc264_minecraft_version"),
]
FALLBACK_MC = {"minecraft_version": "26.2", "mc11_minecraft_version": "1.21.11", "mc264_minecraft_version": "26.4"}


def gradle_props(repo):
    props = {}
    path = repo / "gradle.properties"
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
    return props


def jar_meta(path):
    """(mod id, version) from the jar's loader metadata, None when unreadable."""
    try:
        with zipfile.ZipFile(path) as z:
            names = set(z.namelist())
            if "fabric.mod.json" in names:
                d = json.loads(z.read("fabric.mod.json").decode("utf-8"))
                return d.get("id"), d.get("version")
            for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                if name in names:
                    d = tomllib.loads(z.read(name).decode("utf-8"))
                    mods = d.get("mods") or [{}]
                    return mods[0].get("modId"), mods[0].get("version")
    except Exception:
        return None
    return None, None


def pick_jar(folder, since):
    if not folder.is_dir():
        return None
    jars = [p for p in folder.glob("*.jar") if not SKIP.search(p.name) and p.stat().st_mtime >= since]
    return max(jars, key=lambda p: p.stat().st_mtime) if jars else None


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def candidates(repo):
    """(module id, loader, minecraft, folder) for every buildable mod jar."""
    props = gradle_props(repo)
    out = []
    for m in modselect.load_registry(repo):
        if m.get("example"):
            continue
        for loader, project in (m.get("projects") or {}).items():
            out.append((m["id"], loader, m.get("minecraft", ""), repo.joinpath(*project.strip(":").split(":"), "build", "libs")))
    for folder, loader, prop in SB_EXTRA:
        out.append(("simplebuilding", loader, props.get(prop, FALLBACK_MC[prop]), repo / folder))
    return out


def git_commit(repo, ref):
    try:
        return subprocess.run(["git", "-C", str(repo), "rev-parse", ref or "HEAD"], capture_output=True, text=True,
                              timeout=10, check=True).stdout.strip()
    except Exception:
        return ""


def collect(repo, out, ref, verdict, standalone, since=0.0, keep=3, commit=None, now=None):
    if "GREEN" not in verdict or "RED" in verdict or "NOT" in verdict:
        raise ValueError("gate not green: " + verdict)
    if "alles gruen" not in standalone or "NICHT" in standalone:
        raise ValueError("standalone not green: " + standalone)
    repo, out = Path(repo), Path(out)
    commit = commit or git_commit(repo, "HEAD")
    if not re.fullmatch(r"[0-9a-f]{7,40}", commit or ""):
        raise ValueError("no commit")
    short = commit[:10]
    nodes = {n["id"]: n for n in modselect.catalog(repo)}
    target = out / "files" / short
    if target.exists():
        shutil.rmtree(target)
    files = []
    for mod, loader, mc, folder in candidates(repo):
        jar = pick_jar(folder, since)
        if not jar:
            continue
        meta = jar_meta(jar)
        if meta is None:
            continue
        _, version = meta
        dest = target / loader / mc / jar.name
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(jar, dest)
        n = nodes.get(mod, {})
        files.append({
            "module": mod, "name": n.get("name", mod), "kind": n.get("kind", "mod"), "parent": n.get("parent"),
            "loader": loader, "minecraft": mc, "version": version if version and "$" not in version else "",
            "file": jar.name, "size": dest.stat().st_size, "sha256": sha256(dest),
            "url": f"files/{short}/{loader}/{mc}/{jar.name}",
        })
    if not files:
        shutil.rmtree(target, ignore_errors=True)
        raise ValueError("no jars found (build ran? --since too late?)")
    index_path = out / "index.json"
    index = {"schemaVersion": 1, "builds": []}
    if index_path.exists():
        try:
            index = json.loads(index_path.read_text(encoding="utf-8"))
        except ValueError:
            pass
    stamp = (now or dt.datetime.now(dt.timezone.utc)).isoformat(timespec="seconds")
    build = {"commit": commit, "short": short, "ref": ref, "date": stamp, "verdict": verdict.strip(),
             "standalone": standalone.strip(), "files": files}
    builds = [build] + [b for b in index.get("builds", []) if b.get("commit") != commit]
    kept, dropped = builds[:keep], builds[keep:]
    for b in dropped:
        shutil.rmtree(out / "files" / b.get("short", "_none_"), ignore_errors=True)
    index = {"schemaVersion": 1, "updatedAt": stamp, "builds": kept}
    tmp = index_path.with_suffix(".tmp")
    out.mkdir(parents=True, exist_ok=True)
    tmp.write_text(json.dumps(index, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    os.replace(tmp, index_path)
    return build


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--repo", default=str(modselect.REPO))
    ap.add_argument("--out", required=True)
    ap.add_argument("--ref", required=True)
    ap.add_argument("--verdict", required=True, help='gate line, e.g. "VERDICT: GREEN"')
    ap.add_argument("--standalone", required=True, help='standalone line, e.g. "alles gruen"')
    ap.add_argument("--since", default="", help="ignore jars older than this ISO time (gate start)")
    ap.add_argument("--keep", type=int, default=3)
    a = ap.parse_args(argv)
    since = dt.datetime.fromisoformat(a.since).timestamp() if a.since else 0.0
    try:
        b = collect(a.repo, a.out, a.ref, a.verdict, a.standalone, since, max(1, a.keep))
    except ValueError as e:
        print("collect_downloads: " + str(e), file=sys.stderr)
        return 2
    print(f"collect_downloads: {len(b['files'])} jars for {b['short']} ({a.ref})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
