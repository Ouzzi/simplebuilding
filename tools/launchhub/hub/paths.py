"""Locations, path safety, disk space and small git helpers. No side effects on import."""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import tempfile
import time
from pathlib import Path

HUB_DIR = Path(__file__).resolve().parents[1]          # tools/launchhub
REPO = HUB_DIR.parents[1]                              # repo root (or the worktree this hub lives in)
LOGS_DIR = HUB_DIR / "logs"
DATA_DIR = HUB_DIR / "data"
STATIC_DIR = HUB_DIR / "static"

GIT_TIMEOUT = 20
_SHA = re.compile(r"^[0-9a-f]{7,40}$")


def env_flag(name: str) -> str:
    return (os.environ.get(name) or "").strip()


def git(args: list[str], cwd: Path | str, timeout: int = GIT_TIMEOUT) -> tuple[int, str]:
    """Runs git with an argv list. Returns (exit code, stdout+stderr stripped); never raises."""
    try:
        done = subprocess.run(
            ["git", "-C", str(cwd), *args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            timeout=timeout, check=False, text=True, encoding="utf-8", errors="replace",
        )
        return done.returncode, done.stdout.strip()
    except (OSError, subprocess.SubprocessError) as error:
        return 127, str(error)


def is_sha(value: str) -> bool:
    return bool(_SHA.match(value or ""))


def is_within(child: Path | str, parent: Path | str) -> bool:
    """True when child is parent or lies below it (after resolving both)."""
    try:
        c = Path(child).resolve()
        p = Path(parent).resolve()
    except OSError:
        return False
    return c == p or p in c.parents


def safe_join(root: Path | str, *parts: str) -> Path:
    """root/parts, refusing anything that resolves outside root (.., absolute parts, drive letters)."""
    base = Path(root).resolve()
    for part in parts:
        if not part or Path(part).is_absolute() or ":" in part or "\x00" in part:
            raise ValueError(f"unsafe path part: {part!r}")
    target = base.joinpath(*parts).resolve()
    if not is_within(target, base):
        raise ValueError("path escapes its root")
    return target


def main_repo_root(repo: Path = REPO) -> Path:
    """The main checkout, also when `repo` is a linked worktree."""
    code, out = git(["rev-parse", "--path-format=absolute", "--git-common-dir"], repo)
    if code == 0 and out:
        common = Path(out)
        if common.name == ".git":
            return common.parent
    return repo


def runs_dir(repo: Path = REPO) -> Path:
    """Where testing/runs lives. SB_HUB_RUNS_DIR wins; a worktree without runs falls back to the main checkout."""
    override = env_flag("SB_HUB_RUNS_DIR")
    if override:
        return Path(override).resolve()
    local = repo / "testing" / "runs"
    if local.is_dir():
        return local
    main = main_repo_root(repo) / "testing" / "runs"
    return main if main.is_dir() else local


def gate_dir() -> Path:
    override = env_flag("SB_HUB_GATE_DIR")
    return Path(override).resolve() if override else Path(tempfile.gettempdir()) / "sbgate"


def free_gb(path: Path | str) -> float:
    try:
        target = Path(path)
        while not target.exists() and target != target.parent:
            target = target.parent
        return round(shutil.disk_usage(target).free / 1024 ** 3, 1)
    except OSError:
        return -1.0


def dir_size(path: Path | str, budget_seconds: float = 20.0) -> tuple[int, bool]:
    """Total bytes below path. (bytes, complete) - stops after the time budget, links are not followed."""
    deadline = time.monotonic() + budget_seconds
    total = 0
    stack = [str(path)]
    while stack:
        if time.monotonic() > deadline:
            return total, False
        current = stack.pop()
        try:
            with os.scandir(current) as entries:
                for entry in entries:
                    try:
                        if entry.is_symlink():
                            continue
                        if entry.is_dir(follow_symlinks=False):
                            stack.append(entry.path)
                        else:
                            total += entry.stat(follow_symlinks=False).st_size
                    except OSError:
                        continue
        except OSError:
            continue
    return total, True


def human_bytes(value: int) -> str:
    size = float(value)
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if size < 1024 or unit == "TB":
            return f"{size:.0f} {unit}" if unit == "B" else f"{size:.1f} {unit}"
        size /= 1024
    return f"{value} B"
