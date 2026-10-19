#!/usr/bin/env python3
"""Download the pinned comparison mods for the Simple Optimization benchmark (concept §6.1).

Only Fabric 26.3 jars are pinned (tools/bench/modsets.json: Modrinth version id + SHA-512). Every jar is checked
against its SHA-512 and kept in tools/bench/.cache/mods (git-ignored, never committed). Mods without a 26.3 version are
listed under "missing26_3" and skipped.

  python3 tools/bench/fetch_mods.py                 download/verify all pinned jars
  python3 tools/bench/fetch_mods.py cullleaves ...  only these mod ids
  python3 tools/bench/fetch_mods.py --refresh       ask Modrinth for the newest Fabric 26.3 version of each slug,
                                                   rewrite modsets.json (pins), list slugs without a 26.3 version
"""
from __future__ import annotations

import hashlib
import json
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODSETS = HERE / "modsets.json"
CACHE = HERE / ".cache" / "mods"
API = "https://api.modrinth.com/v2"
UA = {"User-Agent": "simplebuilding-bench/1.0 (github.com/Ouzzi/simplebuilding)"}


def get_json(url: str):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.load(r)


def sha512(path: Path) -> str:
    return hashlib.sha512(path.read_bytes()).hexdigest()


def load() -> dict:
    return json.loads(MODSETS.read_text(encoding="utf-8"))


def fetch(ids: list[str] | None = None) -> dict[str, Path]:
    """mod id -> verified jar path for every pinned mod (or only `ids`)."""
    CACHE.mkdir(parents=True, exist_ok=True)
    out = {}
    for m in load()["mods"]:
        if ids and m["modId"] not in ids:
            continue
        dest = CACHE / m["file"]
        if not dest.is_file() or sha512(dest) != m["sha512"]:
            tmp = dest.with_suffix(".part")
            with urllib.request.urlopen(urllib.request.Request(m["url"], headers=UA), timeout=300) as r:
                tmp.write_bytes(r.read())
            if sha512(tmp) != m["sha512"]:
                tmp.unlink()
                raise SystemExit(f"SHA-512 mismatch for {m['modId']} ({m['file']})")
            tmp.replace(dest)
        out[m["modId"]] = dest
    return out


def refresh() -> None:
    data = load()
    keep, missing = [], list(data.get("missing26_3", []))
    for m in data["mods"]:
        q = f'{API}/project/{m["slug"]}/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%2226.3%22%5D'
        versions = get_json(q)
        if not versions:
            missing.append(dict(slug=m["slug"], modId=m["modId"], latest="?", areas=m.get("areas", [])))
            continue
        v = versions[0]
        f = next(x for x in v["files"] if x["primary"])
        keep.append({**m, "version": v["version_number"], "versionId": v["id"], "file": f["filename"], "url": f["url"],
                     "sha512": f["hashes"]["sha512"]})
    data["mods"], data["missing26_3"] = keep, missing
    MODSETS.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")
    print(f"{len(keep)} pinned, without 26.3: {', '.join(x['modId'] for x in missing) or 'none'}")


if __name__ == "__main__":
    args = sys.argv[1:]
    if "--refresh" in args:
        refresh()
    else:
        for mid, path in fetch(args or None).items():
            print(f"{mid}: {path.name}")
        print("without 26.3 (skipped):", ", ".join(x["modId"] for x in load().get("missing26_3", [])) or "none")
