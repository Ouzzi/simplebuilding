"""
"Save to repo" from the wiki pixel editor (TX22-5, docs/ai/KONZEPT-WIKI-UEBERARBEITUNG-2026-10-23.md section 5,
plan docs/ai/PLAN-A-WIKI24-2026-10-23.md).

One set of rules for both ends:
- the dev queue on the web host (tools/zentrale-web/devqueue.py, POST /api/textures) checks a save with `check_save`
  before it stores it;
- the pickup on the agent host checks it again against the base branch and commits it without Claude:

    python3 tools/textures/editor_save.py pickup --git-dir HUB.git --api "ssh <web host>" [--base claude-wave1]
    python3 tools/textures/editor_save.py apply SAVE.json --git-dir HUB.git [--base claude-wave1]   one save from a file

(--api: command prefix that reaches the queue's local client devq-api; hosts stay outside the repository.)

A save may only replace a texture listed in wiki/data/textures.js (exact id + source, never build/ = Vanilla-derived,
A8) that is tracked on the base branch. The PNG written is always the composition of the layers, encoded here
(tools/textures/layers.py), never the uploaded bytes. Commits are built with git plumbing in the hub repo (no
worktree, no checkout) on a branch tex-<name>-<save id>; the main session takes tex-* branches into the next bundle.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import re
import shlex
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers as ly  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
TEXTURES_JS = "wiki/data/textures.js"
MAX_BODY = 1536 * 1024        # whole request (layers text + base64 PNG + JSON)
MAX_LAYERS = 1024 * 1024      # layers document as text
MAX_PNG = 256 * 1024          # uploaded PNG (only compared, never stored)
MAX_SIDE = 512
MAX_COMMENT = 500
# repository paths a texture may live at: SimpleBuilding's resources, the 26.3 overlay, a module's resources
SOURCE = re.compile(r"^(?:src/main/resources|mc26_3/overlay/resources|modules/[a-z0-9_]+/[a-z0-9_]+(?:/src/main)?/resources)"
                    r"/assets/[a-z0-9_.-]+/textures/[a-z0-9_./-]+\.png$")
SAVE_ID = re.compile(r"^[0-9]{6,32}$")
STATES = ("new", "picked", "ready", "failed")


class SaveError(ValueError):
    pass


# ---------------------------------------------------------------- whitelist

def parse_textures_js(text: str) -> dict[str, dict]:
    """id -> entry of wiki/data/textures.js (window.WIKI_TEXTURES = {...};)."""
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end < start:
        raise SaveError("textures.js unreadable")
    data = json.loads(text[start:end + 1])
    return {t["id"]: t for t in data.get("textures", []) if isinstance(t, dict) and isinstance(t.get("id"), str)}


def load_whitelist(repo: Path = ROOT) -> dict[str, dict]:
    return parse_textures_js((repo / TEXTURES_JS).read_text(encoding="utf-8"))


def safe_source(source: object) -> bool:
    """A relative repository path to an own texture PNG: no absolute path, no .., no backslash, never build/."""
    if not isinstance(source, str) or len(source) > 300:
        return False
    parts = source.split("/")
    return (bool(SOURCE.match(source)) and "\\" not in source and not source.startswith("build/")
            and all(p not in ("", ".", "..") for p in parts))


# ---------------------------------------------------------------- check

def clean_comment(comment: object) -> str:
    if comment is None:
        return ""
    if not isinstance(comment, str):
        raise SaveError("comment must be text")
    text = "".join(c for c in comment if c in "\n\t" or (ord(c) >= 32 and ord(c) != 127)).strip()
    if len(text) > MAX_COMMENT:
        raise SaveError(f"comment longer than {MAX_COMMENT} characters")
    return text


def check_save(payload: dict, whitelist: dict[str, dict]) -> dict:
    """The cleaned save {texture, source, layers (canonical text), comment} or SaveError."""
    if not isinstance(payload, dict):
        raise SaveError("payload must be an object")
    tid, source = payload.get("id"), payload.get("source")
    entry = whitelist.get(tid) if isinstance(tid, str) else None
    if entry is None:
        raise SaveError("unknown texture id")
    if source != entry.get("source") or not safe_source(source):
        raise SaveError("source is not the texture's own file (or a Vanilla-derived one)")
    raw = payload.get("layers")
    if isinstance(raw, dict):
        raw = json.dumps(raw)
    if not isinstance(raw, str) or len(raw.encode("utf-8")) > MAX_LAYERS:
        raise SaveError("layers missing or too large")
    try:
        doc = json.loads(raw)
    except ValueError as e:
        raise SaveError("layers: invalid json") from e
    problems = ly.validate(doc)
    if problems:
        raise SaveError("layers: " + problems[0])
    w, h = doc["width"], doc["height"]
    if w > MAX_SIDE or h > MAX_SIDE:
        raise SaveError(f"larger than {MAX_SIDE}x{MAX_SIDE}")
    if (w, h) != (entry.get("w"), entry.get("h")):
        raise SaveError(f"size {w}x{h} differs from the texture ({entry.get('w')}x{entry.get('h')})")
    rgba = ly.compose(doc)
    png = payload.get("png")
    if png not in (None, ""):
        if not isinstance(png, str) or len(png) > MAX_PNG * 4 // 3 + 8:
            raise SaveError("png too large")
        try:
            data = base64.b64decode(png, validate=True)
            pw, ph, prgba = ly.png_read(data)
        except Exception as e:  # any broken PNG (binascii, zlib, struct, index errors)
            raise SaveError("png unreadable") from e
        if (pw, ph) != (w, h) or any(not ly.same_pixel(prgba[i:i + 4], rgba[i:i + 4]) for i in range(0, w * h * 4, 4)):
            raise SaveError("png differs from the composition of the layers")
    return {"texture": tid, "source": source, "layers": ly.dumps(doc), "comment": clean_comment(payload.get("comment"))}


# ---------------------------------------------------------------- commit (git plumbing, no worktree)

def git(git_dir: Path, *args: str, data: bytes | None = None, env: dict | None = None) -> str:
    done = subprocess.run(["git", "--git-dir", str(git_dir), *args], input=data, capture_output=True, timeout=120,
                          env={**os.environ, **(env or {})})
    if done.returncode:
        raise SaveError(f"git {args[0]}: {done.stderr.decode('utf-8', 'replace').strip()[:300]}")
    return done.stdout.decode("utf-8").strip()


def branch_name(save: dict, sid: str) -> str:
    stem = re.sub(r"[^a-z0-9_-]+", "-", save["texture"].split(":")[-1].split("/")[-1].lower()).strip("-")[:40] or "texture"
    return f"tex-{stem}-{sid}"


def apply_save(payload: dict, sid: str, git_dir: Path, base: str) -> str:
    """Check the save against the base branch and commit PNG + layer source on a new branch; returns the branch."""
    if not SAVE_ID.match(sid):
        raise SaveError("invalid save id")
    whitelist = parse_textures_js(git(git_dir, "show", f"{base}:{TEXTURES_JS}"))
    save = check_save(payload, whitelist)
    source = save["source"]
    if git(git_dir, "cat-file", "-t", f"{base}:{source}") != "blob":
        raise SaveError("texture is not tracked on the base branch")
    doc = json.loads(save["layers"])
    png = ly.png_write(doc["width"], doc["height"], ly.compose(doc))
    layers_rel = ly.layers_path(Path(source)).as_posix()
    # "layers.py check" on exactly these two files (PNG = composition of the layers)
    with tempfile.TemporaryDirectory() as tmp:
        lp = Path(tmp) / Path(layers_rel).name
        lp.write_text(save["layers"], encoding="utf-8", newline="\n")
        ly.png_path(lp).write_bytes(png)
        problems = ly.check_file(lp, set())
        if problems:
            raise SaveError("layers check: " + problems[0])
        index = Path(tmp) / "index"
        env = {"GIT_INDEX_FILE": str(index)}
        git(git_dir, "read-tree", base, env=env)
        for rel, body in ((source, png), (layers_rel, save["layers"].encode("utf-8"))):
            blob = git(git_dir, "hash-object", "-w", "--stdin", data=body)
            git(git_dir, "update-index", "--add", "--cacheinfo", f"100644,{blob},{rel}", env=env)
        tree = git(git_dir, "write-tree", env=env)
    parent = git(git_dir, "rev-parse", f"{base}^{{commit}}")
    message = f"tex({save['texture']}): Editor-Speicherung aus dem Dev-Wiki\n\n"
    if save["comment"]:
        message += save["comment"] + "\n\n"
    message += f"Save-ID: {sid}\nSource: {source}\n"
    who = {"GIT_AUTHOR_NAME": "SB Wiki-Pixeleditor", "GIT_AUTHOR_EMAIL": "wiki-editor@simplebuilding.invalid",
           "GIT_COMMITTER_NAME": "SB Wiki-Pixeleditor", "GIT_COMMITTER_EMAIL": "wiki-editor@simplebuilding.invalid"}
    commit = git(git_dir, "commit-tree", tree, "-p", parent, "-F", "-", data=message.encode("utf-8"), env=who)
    branch = branch_name(save, sid)
    git(git_dir, "update-ref", f"refs/heads/{branch}", commit, "0" * 40)  # never overwrites an existing branch
    return branch


# ---------------------------------------------------------------- pickup (cron on the agent host)

def api(cmd: list[str], method: str, path: str, body: dict | None = None):
    """devq-api over ssh: the remote command line is one quoted string (ssh joins its arguments for a shell)."""
    remote = ["devq-api", method, path] + ([json.dumps(body, ensure_ascii=True)] if body is not None else [])
    full = cmd + [" ".join(shlex.quote(a) for a in remote)] if cmd and cmd[0] == "ssh" else cmd + remote
    done = subprocess.run(full, capture_output=True, timeout=60, stdin=subprocess.DEVNULL)
    if done.returncode:
        raise SaveError(f"api {method} {path}: {done.stdout.decode('utf-8', 'replace')[:200]}")
    return json.loads(done.stdout.decode("utf-8") or "null")


def pickup(cmd: list[str], git_dir: Path, base: str, log=print) -> int:
    done = 0
    for item in api(cmd, "GET", "textures") or []:
        sid = str(item.get("sid", ""))
        if item.get("state") != "new" or not SAVE_ID.match(sid):
            continue
        api(cmd, "PATCH", f"textures/{sid}", {"state": "picked", "note": "abgeholt"})
        try:
            save = api(cmd, "GET", f"textures/{sid}")
            branch = apply_save({"id": save.get("texture"), "source": save.get("source"), "layers": save.get("layers"),
                                 "comment": save.get("comment")}, sid, git_dir, base)
            api(cmd, "PATCH", f"textures/{sid}", {"state": "ready", "note": branch, "branch": branch})
            log(f"{sid}: {branch}")
        except (SaveError, ValueError, OSError, subprocess.SubprocessError) as e:
            api(cmd, "PATCH", f"textures/{sid}", {"state": "failed", "note": str(e)[:400]})
            log(f"{sid}: failed {e}")
        done += 1
    return done


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("command", choices=("pickup", "apply"))
    p.add_argument("file", nargs="?", help="apply: save JSON {id, source, layers, comment, sid}")
    p.add_argument("--git-dir", required=True, help="hub repository (bare) the tex-* branches are created in")
    p.add_argument("--base", default="claude-wave1")
    p.add_argument("--api", help='pickup: command prefix for devq-api, e.g. "ssh -o BatchMode=yes <web host>"')
    a = p.parse_args(argv)
    if a.command == "pickup":
        if not a.api:
            p.error("pickup needs --api")
        pickup(shlex.split(a.api), Path(a.git_dir), a.base)
        return 0
    if not a.file:
        p.error("apply needs a file")
    save = json.loads(Path(a.file).read_text(encoding="utf-8"))
    print(apply_save(save, str(save.get("sid", "")), Path(a.git_dir), a.base))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
