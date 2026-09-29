"""Git worktrees: the gate worktree, AI worktrees, and the cleanup helper for .claude/worktrees."""

from __future__ import annotations

import threading
import time
from pathlib import Path

from . import paths

_SIZES: dict[str, dict] = {}
_SIZE_LOCK = threading.Lock()


def parse_porcelain(text: str) -> list[dict]:
    items: list[dict] = []
    current: dict = {}
    for line in text.splitlines() + [""]:
        if not line.strip():
            if current:
                items.append(current)
                current = {}
            continue
        key, _, value = line.partition(" ")
        if key == "worktree":
            current["path"] = value
        elif key == "HEAD":
            current["head"] = value
        elif key == "branch":
            current["branch"] = value.replace("refs/heads/", "", 1)
        elif key == "detached":
            current["detached"] = True
        elif key == "locked":
            current["locked"] = True
        elif key == "bare":
            current["bare"] = True
    return items


def list_all(repo: Path) -> list[dict]:
    code, out = paths.git(["worktree", "list", "--porcelain"], repo)
    return parse_porcelain(out) if code == 0 else []


def base_ref(repo: Path) -> str:
    code, _ = paths.git(["rev-parse", "--verify", "--quiet", "master"], repo)
    return "master" if code == 0 else "HEAD"


def status(path: str, base: str) -> dict:
    """Dirty flag, commits not in `base`, last commit."""
    wt = Path(path)
    info = {"dirty": False, "unmerged": 0, "merged": True, "last": "", "lastAt": ""}
    if not wt.is_dir():
        info["missing"] = True
        return info
    code, out = paths.git(["status", "--porcelain"], wt)
    info["dirty"] = code == 0 and bool(out.strip())
    code, out = paths.git(["rev-list", "--count", f"{base}..HEAD"], wt)
    if code == 0 and out.isdigit():
        info["unmerged"] = int(out)
    info["merged"] = info["unmerged"] == 0
    code, out = paths.git(["log", "-1", "--format=%h %s|%cI"], wt)
    if code == 0 and "|" in out:
        subject, _, when = out.rpartition("|")
        info["last"], info["lastAt"] = subject, when
    return info


def _measure(path: str) -> None:
    total, complete = paths.dir_size(path, budget_seconds=30)
    with _SIZE_LOCK:
        _SIZES[path] = {"bytes": total, "complete": complete, "at": time.time(), "busy": False}


def size_of(path: str, max_age: float = 600.0) -> dict:
    """Cached size; starts a background measurement when unknown or old (worktrees hold GBs of build output)."""
    with _SIZE_LOCK:
        entry = _SIZES.get(path)
        if entry and (entry.get("busy") or time.time() - entry.get("at", 0) < max_age):
            return entry
        _SIZES[path] = {"bytes": (entry or {}).get("bytes"), "complete": False, "at": 0, "busy": True}
    threading.Thread(target=_measure, args=(path,), daemon=True).start()
    return _SIZES[path]


def agent_worktrees(repo: Path) -> list[dict]:
    main = paths.main_repo_root(repo)
    root = main / ".claude" / "worktrees"
    base = base_ref(repo)
    from concurrent.futures import ThreadPoolExecutor
    chosen = [item for item in list_all(repo)
              if item.get("path") and paths.is_within(item["path"], root) and Path(item["path"]).resolve() != root.resolve()]
    with ThreadPoolExecutor(max_workers=8) as pool:
        stats = list(pool.map(lambda item: status(item["path"], base), chosen))
    out = []
    for item, stat in zip(chosen, stats):
        path = item["path"]
        size = size_of(path)
        out.append({
            "path": path, "name": Path(path).name, "branch": item.get("branch"), "head": (item.get("head") or "")[:8],
            "detached": bool(item.get("detached")), "locked": bool(item.get("locked")),
            "isHub": Path(path).resolve() == repo.resolve(), "base": base, **stat,
            "sizeBytes": size.get("bytes"), "sizeComplete": size.get("complete"), "sizeBusy": size.get("busy"),
        })
    out.sort(key=lambda w: (w["merged"], w["name"]))
    return out


def removal_check(repo: Path, path: str) -> tuple[dict | None, str | None]:
    """The worktree record when `path` may be removed at all, else a reason."""
    for wt in agent_worktrees(repo):
        if Path(wt["path"]).resolve() == Path(path).resolve():
            if wt["isHub"]:
                return None, "this is the worktree the hub itself runs from"
            if wt["locked"]:
                return None, "the worktree is locked (git worktree unlock first)"
            return wt, None
    return None, "not a worktree under .claude/worktrees"


def remove(repo: Path, path: str, confirm: bool, confirm_unmerged: bool, log=None) -> dict:
    wt, reason = removal_check(repo, path)
    if not wt:
        raise ValueError(reason)
    if not confirm:
        raise ValueError("deleting needs confirm=true")
    risky = wt["unmerged"] > 0 or wt["dirty"]
    if risky and not confirm_unmerged:
        raise PermissionError(f"{wt['unmerged']} unmerged commit(s)" + (" and uncommitted changes" if wt["dirty"] else "")
                              + " - a second confirmation is required")
    main = paths.main_repo_root(repo)
    code, out = paths.git(["worktree", "remove", "--force", str(wt["path"])], main, timeout=300)
    if code != 0:
        raise RuntimeError(out or "git worktree remove failed")
    branch_note = ""
    if wt["branch"] and wt["merged"]:
        code, out = paths.git(["branch", "-d", wt["branch"]], main)
        branch_note = f"branch {wt['branch']} deleted" if code == 0 else f"branch kept ({out.splitlines()[-1] if out else 'in use'})"
    return {"removed": wt["path"], "branch": branch_note}


def ensure_gate(repo: Path, gate: Path, commit: str, log) -> bool:
    """Makes the gate worktree exist and sit (detached) on `commit`. Refuses to touch one with local changes."""
    main = paths.main_repo_root(repo)
    known = {Path(w["path"]).resolve() for w in list_all(repo) if w.get("path")}
    if gate.exists() and gate.resolve() not in known:
        log(f"[hub] {gate} exists but is not a worktree of this repository - refusing to use it")
        return False
    if not paths.is_sha(commit):
        log(f"[hub] invalid commit {commit!r}")
        return False
    if not gate.exists():
        log(f"[hub] creating gate worktree {gate} at {commit[:8]}")
        code, out = paths.git(["worktree", "add", "--detach", str(gate), commit], main, timeout=600)
        log(out)
        return code == 0
    code, out = paths.git(["status", "--porcelain"], gate)
    if code == 0 and out.strip():
        log("[hub] the gate worktree has local changes - not moving it:\n" + out[:800])
        return False
    code, head = paths.git(["rev-parse", "HEAD"], gate)
    if head == commit:
        log(f"[hub] gate worktree already at {commit[:8]}")
        return True
    log(f"[hub] moving gate worktree to {commit[:8]}")
    code, out = paths.git(["checkout", "--detach", commit], gate, timeout=600)
    log(out)
    return code == 0


def create_worktree(repo: Path, path: Path, branch: str, base_sha: str, log) -> bool:
    main = paths.main_repo_root(repo)
    if not paths.is_sha(base_sha):
        log("[hub] invalid base commit")
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    log(f"[hub] git worktree add -b {branch} {path} {base_sha[:8]}")
    code, out = paths.git(["worktree", "add", "-b", branch, str(path), base_sha], main, timeout=600)
    log(out)
    return code == 0
