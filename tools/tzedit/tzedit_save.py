#!/usr/bin/env python3
"""
N50 TZEDIT50: "Speichern" of the testcentre editor (dev wiki page index.html#/tzeditor, wiki/assets/tzeditor.js), same pattern as the pixel
editor's tools/textures/editor_save.py:

- the dev queue on the web host (tools/zentrale-web/devqueue.py, POST /api/testcentre) checks a save with
  `check_save` and stores it as JSON in its data directory (never touches git there);
- the agent host pulls new saves read-only and commits each one with git plumbing on its own hub branch
  tz-layout-<area>-<save id> (never onto claude-wave1); the main session takes tz-layout-* into the next bundle:

    python3 tools/tzedit/tzedit_save.py pull --git-dir HUB.git --remote "ssh -o BatchMode=yes <web host>" [--base claude-wave1]
    python3 tools/tzedit/tzedit_save.py apply SAVE.json --git-dir HUB.git [--base claude-wave1]

A save is one layout area: {"variant", "area", "columns", "rows":[{"key","label","cells","tops"?}], "excluded"?, "comment"?}.
Cells hold a block/item id (namespace optional = minecraft), null (no field) or a placeholder #grey/#light/#red/#frame
(#red = should exist, #light = light grey "none" placeholder as built in the lines area). "tops" (Phase 2): {column: id}
of an octet that stands on the block of that cell (storage/raw block/log octets). Area/column meaning: export_layout.py.
The file written is always re-serialised here (never the uploaded text) into LAYOUT_DIR/layout_<variant>_<area>.json.
"""
from __future__ import annotations

import argparse
import json
import re
import shlex
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "textures"))
from editor_save import SaveError, git, remote_run  # noqa: E402  (one git/remote helper for both editors)

LAYOUT_DIR = "src/main/resources/data/simplebuilding/testcentre"
MAX_BODY = 512 * 1024
MAX_COMMENT = 500
MAX_COLUMNS = 200
MAX_ROWS = 2000
MAX_EXCLUDED = 5000
SAVE_ID = re.compile(r"^[0-9]{6,32}$")
NAME = re.compile(r"^[a-z0-9][a-z0-9_-]{0,47}$")          # variant, area, column, row key
ID = re.compile(r"^(?:[a-z0-9_.-]{1,64}:)?[a-z0-9_./-]{1,128}$")
PLACEHOLDERS = {"#grey", "#light", "#red", "#frame"}
STATES = ("new", "picked", "ready", "failed")


def _label(value: object) -> str:
    if not isinstance(value, str) or len(value) > 80 or any(ord(c) < 32 for c in value):
        raise SaveError("invalid row label")
    return value


def check_save(payload: object) -> dict:
    """The one rule set: returns the cleaned save (only known keys) or raises SaveError."""
    if not isinstance(payload, dict):
        raise SaveError("save must be an object")
    variant, area = payload.get("variant"), payload.get("area")
    if not isinstance(variant, str) or not NAME.match(variant):
        raise SaveError("invalid variant")
    if not isinstance(area, str) or not NAME.match(area):
        raise SaveError("invalid area")
    columns = payload.get("columns")
    if not isinstance(columns, list) or not 0 < len(columns) <= MAX_COLUMNS \
            or not all(isinstance(c, str) and NAME.match(c) for c in columns):
        raise SaveError("invalid columns")
    rows = payload.get("rows")
    if not isinstance(rows, list) or not 0 < len(rows) <= MAX_ROWS:
        raise SaveError("invalid rows")
    clean_rows, keys = [], set()
    for row in rows:
        if not isinstance(row, dict):
            raise SaveError("row must be an object")
        key = row.get("key")
        if not isinstance(key, str) or not NAME.match(key) or key in keys:
            raise SaveError(f"invalid or duplicate row key: {str(key)[:50]}")
        keys.add(key)
        cells = row.get("cells")
        if not isinstance(cells, list) or len(cells) != len(columns):
            raise SaveError(f"row {key}: {len(cells) if isinstance(cells, list) else 'no'} cells for {len(columns)} columns")
        for cell in cells:
            if cell is not None and not (isinstance(cell, str) and (cell in PLACEHOLDERS or ID.match(cell))):
                raise SaveError(f"row {key}: invalid cell {str(cell)[:60]}")
        clean = {"key": key, "label": _label(row.get("label", key)), "cells": list(cells)}
        tops = row.get("tops")
        if tops:
            if not isinstance(tops, dict) or not all(isinstance(c, str) and c in columns and isinstance(t, str) and ID.match(t)
                                                     for c, t in tops.items()):
                raise SaveError(f"row {key}: invalid tops")
            clean["tops"] = {c: tops[c] for c in columns if c in tops}
        clean_rows.append(clean)
    excluded = payload.get("excluded", [])
    if not isinstance(excluded, list) or len(excluded) > MAX_EXCLUDED \
            or not all(isinstance(e, str) and ID.match(e) for e in excluded):
        raise SaveError("invalid excluded list")
    comment = payload.get("comment", "")
    if not isinstance(comment, str) or len(comment) > MAX_COMMENT:
        raise SaveError("invalid comment")
    comment = "".join(c for c in comment if c in "\n\t" or ord(c) >= 32).strip()
    return {"schema": 1, "variant": variant, "area": area, "columns": list(columns), "rows": clean_rows,
            "excluded": sorted(set(excluded)), "comment": comment}


def layout_path(save: dict) -> str:
    return f"{LAYOUT_DIR}/layout_{save['variant']}_{save['area']}.json"


def layout_text(save: dict) -> str:
    """Stable file text: one row per line so diffs stay readable."""
    head = {k: save[k] for k in ("schema", "variant", "area", "columns")}
    lines = ["{"] + [f"  {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}," for k, v in head.items()]
    lines.append('  "rows": [')
    lines += [("    " + json.dumps(r, ensure_ascii=False) + ("," if i < len(save["rows"]) - 1 else ""))
              for i, r in enumerate(save["rows"])]
    lines.append("  ],")
    lines.append(f'  "excluded": {json.dumps(save["excluded"], ensure_ascii=False)}')
    lines.append("}")
    return "\n".join(lines) + "\n"


def branch_name(save: dict, sid: str) -> str:
    return f"tz-layout-{save['variant']}-{save['area']}-{sid}"[:120]


def apply_save(payload: dict, sid: str, git_dir: Path, base: str, by: str = "") -> str:
    """Commit the checked layout file on a new branch tz-layout-<variant>-<area>-<sid> from `base`; returns the branch."""
    if not SAVE_ID.match(sid):
        raise SaveError("invalid save id")
    save = check_save(payload)
    rel = layout_path(save)
    with tempfile.TemporaryDirectory() as tmp:
        env = {"GIT_INDEX_FILE": str(Path(tmp) / "index")}
        git(git_dir, "read-tree", base, env=env)
        blob = git(git_dir, "hash-object", "-w", "--stdin", data=layout_text(save).encode("utf-8"))
        git(git_dir, "update-index", "--add", "--cacheinfo", f"100644,{blob},{rel}", env=env)
        tree = git(git_dir, "write-tree", env=env)
    parent = git(git_dir, "rev-parse", f"{base}^{{commit}}")
    if git(git_dir, "rev-parse", f"{parent}^{{tree}}") == tree:
        raise SaveError("no change against the base branch")
    message = f"tz-layout({save['variant']}/{save['area']}): Testzentrale-Editor, Speicherung aus dem Dev-Wiki\n\n"
    if save["comment"]:
        message += save["comment"] + "\n\n"
    message += f"Save-ID: {sid}\nFile: {rel}\n" + (f"By: {by}\n" if by else "")
    who = {"GIT_AUTHOR_NAME": "SB Testzentrale-Editor", "GIT_AUTHOR_EMAIL": "tz-editor@simplebuilding.invalid",
           "GIT_COMMITTER_NAME": "SB Testzentrale-Editor", "GIT_COMMITTER_EMAIL": "tz-editor@simplebuilding.invalid"}
    commit = git(git_dir, "commit-tree", tree, "-p", parent, "-F", "-", data=message.encode("utf-8"), env=who)
    branch = branch_name(save, sid)
    git(git_dir, "update-ref", f"refs/heads/{branch}", commit, "0" * 40)  # never overwrites an existing branch
    return branch


def pull(cmd: list[str], remote_dir: str, git_dir: Path, base: str, report: Path | None = None,
         dry_run: bool = False, log=print) -> list[str]:
    """Read new saves from the web host (ls + cat, never written there) and branch each; idempotent: a save id that
    already has a tz-layout-* branch is skipped; of several saves of one area only the newest is branched."""
    names = remote_run(cmd, "ls", "-1", remote_dir).decode("utf-8", "replace").split()
    sids = sorted((n[:-5] for n in names if n.endswith(".json") and SAVE_ID.match(n[:-5])), key=int)
    have = git(git_dir, "for-each-ref", "--format=%(refname:short)", "refs/heads/tz-layout-*").split()
    branched = {b.rsplit("-", 1)[-1] for b in have}
    recs = {}
    for sid in sids:
        if sid in branched:
            continue
        try:
            rec = json.loads(remote_run(cmd, "cat", f"{remote_dir.rstrip('/')}/{sid}.json").decode("utf-8"))
        except (SaveError, ValueError) as e:
            log(f"{sid}: unreadable ({e})")
            continue
        if isinstance(rec, dict):
            recs[sid] = rec
    newest = {}
    for sid in recs:
        newest[(recs[sid].get("variant"), recs[sid].get("area"))] = sid
    made = []
    for sid, rec in recs.items():
        if newest[(rec.get("variant"), rec.get("area"))] != sid:
            log(f"{sid}: superseded by {newest[(rec.get('variant'), rec.get('area'))]}")
            continue
        if dry_run:
            log(f"{sid}: would commit {rec.get('variant')}/{rec.get('area')}")
            continue
        try:
            branch = apply_save(rec, sid, git_dir, base, str(rec.get("by", "")))
        except (SaveError, ValueError, OSError, subprocess.SubprocessError) as e:
            log(f"{sid}: failed {e}")
            continue
        commit = git(git_dir, "rev-parse", "--short", branch)
        made.append(branch)
        log(f"{sid}: {branch} {commit}")
        if report:
            with open(report, "a", encoding="utf-8") as f:
                f.write(f"- {branch} {commit} (Basis {base}) – Testzentrale-Editor-Speicherung {rec.get('variant')}/"
                        f"{rec.get('area')} (Save-ID {sid}) – automatisch abgeholt (tzedit_save.py pull)\n")
    return made


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("command", choices=("apply", "pull", "check"))
    p.add_argument("file", nargs="?", help="apply/check: save JSON")
    p.add_argument("--git-dir", help="hub repository (bare)")
    p.add_argument("--base", default="claude-wave1")
    p.add_argument("--sid", default="", help="apply: save id (default: from the file)")
    p.add_argument("--remote", help='pull: read-only command prefix to the web host, e.g. "ssh -o BatchMode=yes <web host>"')
    p.add_argument("--dir", default="/srv/devqueue/testcentre", help="pull: save directory on the web host")
    p.add_argument("--report", help="pull: append one line per new branch")
    p.add_argument("--dry-run", action="store_true")
    a = p.parse_args(argv)
    try:
        if a.command == "check":
            save = check_save(json.loads(Path(a.file).read_text(encoding="utf-8")))
            print(f"ok: {layout_path(save)} ({len(save['rows'])} rows x {len(save['columns'])} columns)")
        elif a.command == "apply":
            rec = json.loads(Path(a.file).read_text(encoding="utf-8"))
            print(apply_save(rec, a.sid or str(rec.get("sid", "")), Path(a.git_dir), a.base, str(rec.get("by", ""))))
        else:
            if not a.remote or not a.git_dir:
                p.error("pull needs --remote and --git-dir")
            pull(shlex.split(a.remote), a.dir, Path(a.git_dir), a.base, Path(a.report) if a.report else None, a.dry_run)
    except (SaveError, ValueError, OSError) as e:
        print(f"error: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
