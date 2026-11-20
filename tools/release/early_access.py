#!/usr/bin/env python3
"""Early access for Simple Mods Pro (N108): new versions first on simplemods.de (download area account.simplemods.de/early,
login + perk), Modrinth/CurseForge always one version behind - from the first update AFTER the release; the release version
itself goes everywhere at the same time (``--launch``).

State = ``<dir>/index.json`` (read by smweb/early.py): releases newest first, each ``{version, date, launch, public, files,
published: {modrinth: date, curseforge: date}}``. ``public`` = may be on the platforms now: every release except the newest,
and the newest only when it is the launch release.

    early_access.py stage   --dir /srv/early-access --version 1.3.0 --file build/libs/x-1.3.0.jar [--launch]
    early_access.py plan    --dir /srv/early-access                 # which versions are due on which platform
    early_access.py publish --dir /srv/early-access --target modrinth [--upload --owner-approved]

``publish`` is a DRY RUN by default (prints what it would send). A real upload needs ``--upload --owner-approved`` and the token
in the environment (MODRINTH_TOKEN / CURSEFORGE_TOKEN, plus MODRINTH_PROJECT / CURSEFORGE_PROJECT); owner order N108: uploads only
after the owner's go. Tokens are never printed.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import shutil
import sys
import urllib.request
import uuid
from pathlib import Path

TARGETS = ("modrinth", "curseforge")


def load(d: Path) -> dict:
    try:
        data = json.loads((d / "index.json").read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {"releases": []}
    except (OSError, ValueError):
        return {"releases": []}


def save(d: Path, data: dict):
    d.mkdir(parents=True, exist_ok=True)
    tmp = d / "index.json.tmp"
    tmp.write_text(json.dumps(data, indent=1, ensure_ascii=False), encoding="utf-8")
    os.replace(tmp, d / "index.json")


def mark_public(data: dict) -> dict:
    rel = data.get("releases", [])
    for i, r in enumerate(rel):
        r["public"] = i > 0 or bool(r.get("launch"))
    return data


def stage(d: Path, version: str, files: list[Path], launch: bool, today: str | None = None) -> dict:
    data = load(d)
    if any(r["version"] == version for r in data["releases"]):
        raise SystemExit(f"version {version} is already staged")
    (d / "files").mkdir(parents=True, exist_ok=True)
    entries = []
    for f in files:
        if not f.is_file() or f.suffix not in (".jar", ".zip"):
            raise SystemExit(f"not a jar/zip: {f}")
        shutil.copy2(f, d / "files" / f.name)
        entries.append({"name": f.name, "size": f.stat().st_size, "sha256": hashlib.sha256(f.read_bytes()).hexdigest()})
    data["releases"].insert(0, {"version": version, "date": today or dt.date.today().isoformat(), "launch": launch,
                                "files": entries, "published": {}})
    save(d, mark_public(data))
    return data


def due(data: dict) -> list[tuple[str, str]]:
    """(target, version) pairs that are public but not yet on the platform, oldest first."""
    out = []
    for r in reversed(mark_public(data)["releases"]):
        for t in TARGETS:
            if r["public"] and not (r.get("published") or {}).get(t):
                out.append((t, r["version"]))
    return out


def multipart(fields: dict, files: list[tuple[str, str, bytes]]) -> tuple[bytes, str]:
    b = uuid.uuid4().hex
    parts = []
    for k, v in fields.items():
        parts.append(f'--{b}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
    for field, name, data in files:
        parts.append(f'--{b}\r\nContent-Disposition: form-data; name="{field}"; filename="{name}"\r\n'
                     f"Content-Type: application/java-archive\r\n\r\n".encode() + data + b"\r\n")
    return b"".join(parts) + f"--{b}--\r\n".encode(), f"multipart/form-data; boundary={b}"


def requests_for(target: str, release: dict, d: Path, project: str, game_versions: list[str], loaders: list[str]) -> list[dict]:
    """The HTTP requests an upload would make (no network here)."""
    out = []
    names = [f["name"] for f in release["files"]]
    if target == "modrinth":
        meta = {"project_id": project, "version_number": release["version"], "name": release["version"], "changelog": "",
                "dependencies": [], "game_versions": game_versions, "version_type": "release", "loaders": loaders,
                "featured": False, "file_parts": [f"file{i}" for i in range(len(names))]}
        out.append({"method": "POST", "url": "https://api.modrinth.com/v2/version", "fields": {"data": json.dumps(meta)},
                    "files": [(f"file{i}", n) for i, n in enumerate(names)], "auth": "MODRINTH_TOKEN"})
    else:
        for n in names:
            meta = {"changelog": "", "changelogType": "text", "displayName": f"{n}", "gameVersions": [], "releaseType": "release"}
            out.append({"method": "POST", "url": f"https://minecraft.curseforge.com/api/projects/{project}/upload-file",
                        "fields": {"metadata": json.dumps(meta)}, "files": [("file", n)], "auth": "CURSEFORGE_TOKEN"})
    return out


def send(req: dict, d: Path, token: str) -> dict:
    body, ctype = multipart(req["fields"], [(f, n, (d / "files" / n).read_bytes()) for f, n in req["files"]])
    h = {"Content-Type": ctype, "User-Agent": "simplemods-release"}
    h["Authorization" if req["auth"] == "MODRINTH_TOKEN" else "X-Api-Token"] = token
    with urllib.request.urlopen(urllib.request.Request(req["url"], data=body, headers=h, method="POST"), timeout=120) as r:
        return json.load(r)


def publish(d: Path, target: str, upload: bool, approved: bool, game_versions: list[str], loaders: list[str],
            env=os.environ, sender=send, out=print) -> int:
    data = load(d)
    todo = [v for t, v in due(data) if t == target]
    if not todo:
        out(f"{target}: nothing due")
        return 0
    project = env.get(f"{target.upper()}_PROJECT", "<project>")
    for version in todo:
        rel = next(r for r in data["releases"] if r["version"] == version)
        for req in requests_for(target, rel, d, project, game_versions, loaders):
            out(f"{'UPLOAD' if upload else 'dry run'}: {req['method']} {req['url']} files={[n for _, n in req['files']]}")
            if upload:
                if not approved:
                    out("refused: uploads need the owner's go (--owner-approved)")
                    return 2
                token = env.get(req["auth"])
                if not token:
                    out(f"refused: {req['auth']} not set")
                    return 2
                sender(req, d, token)
        if upload:
            rel.setdefault("published", {})[target] = dt.date.today().isoformat()
            save(d, data)
    return 0


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("command", choices=["stage", "plan", "publish"])
    ap.add_argument("--dir", required=True)
    ap.add_argument("--version")
    ap.add_argument("--file", action="append", default=[])
    ap.add_argument("--launch", action="store_true")
    ap.add_argument("--target", choices=TARGETS)
    ap.add_argument("--game-version", action="append", default=[])
    ap.add_argument("--loader", action="append", default=[])
    ap.add_argument("--upload", action="store_true")
    ap.add_argument("--owner-approved", action="store_true")
    a = ap.parse_args(argv)
    d = Path(a.dir)
    if a.command == "stage":
        if not a.version or not a.file:
            ap.error("stage needs --version and --file")
        stage(d, a.version, [Path(f) for f in a.file], a.launch)
        print(f"staged {a.version} ({'launch: public everywhere at once' if a.launch else 'early access only until the next version'})")
        return 0
    if a.command == "plan":
        for t, v in due(load(d)) or [("-", "nothing due")]:
            print(f"{t}: {v}")
        return 0
    if not a.target:
        ap.error("publish needs --target")
    return publish(d, a.target, a.upload, a.owner_approved, a.game_version or ["26.3"], a.loader or ["fabric"])


if __name__ == "__main__":
    sys.exit(main())
