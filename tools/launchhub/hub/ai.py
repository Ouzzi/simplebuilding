"""AI fix jobs: prompt building, provider commands, and the result of a finished job.

The prompt comes from tools/launchhub/prompt_template.md ({{placeholders}}, editable by the owner).
Provider commands are templates split into argv elements; placeholders are substituted per element,
so no user or record text ever reaches a shell.
"""

from __future__ import annotations

import os
import re
import shlex
import shutil
from pathlib import Path

from . import paths, runs

TEMPLATE_FILE = paths.HUB_DIR / "prompt_template.md"
PLACEHOLDER = re.compile(r"\{\{(\w+)\}\}")
BRANCH_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9/_.\-]{2,80}$")

GATEST_DIRS = ("common/src/shared/java/com/simplebuilding/gametest",
               "mc1_21_11/shared/java/com/simplebuilding/gametest")


class ProviderError(RuntimeError):
    pass


# ----------------------------------------------------------------------------
# Template
# ----------------------------------------------------------------------------

def render_template(template: str, values: dict[str, str]) -> tuple[str, list[str]]:
    """Fills {{name}} placeholders. Returns (text, names that had no value and stayed as they were)."""
    unknown: list[str] = []

    def sub(match: re.Match) -> str:
        name = match.group(1)
        if name in values:
            return values[name]
        unknown.append(name)
        return match.group(0)

    return PLACEHOLDER.sub(sub, template), unknown


def read_template() -> str:
    return TEMPLATE_FILE.read_text(encoding="utf-8")


# ----------------------------------------------------------------------------
# Prompt parts
# ----------------------------------------------------------------------------

def failure_block(row: dict, excerpt: str) -> str:
    lines = [f"### `{row['id']}`  (target `{row['target']}`)"]
    lines.append(f"- status: {row['status']}; first failed in run `{row.get('firstFailedRun') or row['runId']}`"
                 + (f"; flaky: {row['flaky']}" if row.get("flaky") else ""))
    lines += ["```", (row.get("message") or "(the record has no message)").strip()]
    if excerpt:
        lines += ["--- log excerpt ---", excerpt.strip()]
    lines.append("```")
    return "\n".join(lines)


def source_index(repo: Path) -> dict[str, list[str]]:
    """class name -> repo relative paths of the test sources that define it (only the gametest folders)."""
    index: dict[str, list[str]] = {}
    for rel in GATEST_DIRS:
        folder = repo / rel
        if folder.is_dir():
            for entry in sorted(os.listdir(folder)):
                if entry.endswith(".java"):
                    index.setdefault(entry[:-5], []).append(f"{rel}/{entry}")
    return index


def file_hints(repo: Path, rows: list[dict], catalogue: dict[str, list[dict]]) -> str:
    by_id = {}
    for entries in catalogue.values():
        for entry in entries:
            by_id.setdefault(entry["id"], entry)
    index = source_index(repo)
    lines: list[str] = []
    seen: set[str] = set()
    for row in rows:
        entry = by_id.get(row["id"])
        if not entry or entry["id"] in seen:
            continue
        seen.add(entry["id"])
        files = ", ".join(f"`{p}`" for p in index.get(entry["testClass"], [])) or f"class `{entry['testClass']}`"
        lines.append(f"- `{row['id']}`: method `{entry['method']}` in {files}")
    lines.append("- Registration of all tests: `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`")
    lines.append("- Test runner and its records: `tools/testrunner/run.py`, `testing/README.md`")
    return "\n".join(lines)


def rerun_commands(rows: list[dict], catalogue: dict[str, list[dict]], line_of_target: dict[str, str]) -> str:
    """One command per single filter pattern and target group, as the agent should run them."""
    from . import targets  # local import: targets pulls in run.py
    per_target: dict[str, list[str]] = {}
    for row in rows:
        per_target.setdefault(row["target"], []).append(row["id"])
    lines = []
    for target_id, ids in per_target.items():
        if target_id.startswith("client-"):
            lines.append(f"- `{target_id}` is a client suite (screenshot checkpoints), run it only when no owner client is open: "
                         f"`python tools/testrunner/run.py --targets {target_id}` "
                         "(narrow with the env var `SIMPLEBUILDING_CLIENT_ONLY=<client test name>`)")
            continue
        entries = catalogue.get(line_of_target.get(target_id, ""), [])
        for pattern in targets.build_patterns(ids, entries):
            lines.append(f"- `python tools/testrunner/run.py --targets {target_id} --filter={pattern}`")
    lines.append("- Compile check: `./gradlew.bat check -q` only at the very end, inside your worktree.")
    return "\n".join(lines)


def build_prompt(template: str, rows: list[dict], excerpts: dict[tuple[str, str], str], ctx: dict,
                 hints: str, rerun: str, extra: str = "") -> tuple[str, list[str]]:
    blocks = [failure_block(r, excerpts.get((r["target"], r["id"]), "")) for r in rows]
    run_ids = sorted({r["runId"] for r in rows})
    commits = ctx.get("runCommits") or []
    stale = ""
    if ctx.get("head") and commits and ctx["head"] not in commits:
        stale = ("The record is from an older commit than the base of this worktree - the test may already be fixed. "
                 "Re-run the failing tests first.")
    values = {
        "worktree": ctx.get("worktree", ""), "branch": ctx.get("branch", ""), "base": (ctx.get("head") or "")[:10],
        "targets": ", ".join(sorted({r["target"] for r in rows})) or "(none)",
        "run_commit": ", ".join(c[:10] for c in commits) or "unknown",
        "run_ids": ", ".join(f"`{r}`" for r in run_ids) or "unknown", "stale_note": stale,
        "failures": "\n\n".join(blocks) or "(no failing tests selected)",
        "file_hints": hints, "rerun_commands": rerun, "extra": extra.strip(),
    }
    return render_template(template, values)


# ----------------------------------------------------------------------------
# Providers
# ----------------------------------------------------------------------------

def split_template(template: str) -> list[str]:
    try:
        parts = shlex.split(template, posix=True)
    except ValueError as error:
        raise ProviderError(f"command template does not parse: {error}") from error
    if not parts:
        raise ProviderError("command template is empty")
    return parts


def provider_state(pid: str, conf: dict) -> dict:
    try:
        exe = split_template(conf["template"])[0]
    except ProviderError as error:
        return {"id": pid, "label": conf.get("label", pid), "installed": False, "path": None, "problem": str(error),
                "install": conf.get("install", "")}
    found = shutil.which(exe)
    return {"id": pid, "label": conf.get("label", pid), "installed": bool(found), "path": found,
            "executable": exe, "install": conf.get("install", ""), "template": conf["template"],
            "stdin": bool(conf.get("stdin"))}


def render_command(conf: dict, prompt_text: str, prompt_file: str, worktree: str, branch: str) -> dict:
    """argv (list), the stdin text (or None) for one provider. Raises ProviderError when the CLI is missing."""
    parts = split_template(conf["template"])
    exe = shutil.which(parts[0])
    if not exe:
        raise ProviderError(f"'{parts[0]}' is not installed / not on PATH. Install hint: {conf.get('install', 'see the CLI docs')}")
    values = {"{prompt_file}": prompt_file, "{prompt}": prompt_text, "{worktree}": worktree, "{branch}": branch}
    argv = [exe]
    uses_prompt = False
    for token in parts[1:]:
        for key, val in values.items():
            if key in token:
                uses_prompt = uses_prompt or key in ("{prompt_file}", "{prompt}")
                token = token.replace(key, val)
        argv.append(token)
    stdin_text = None
    if conf.get("stdin"):
        stdin_text = prompt_text
    elif not uses_prompt:
        argv.append(prompt_text)
    return {"argv": argv, "stdin_text": stdin_text}


def valid_branch(name: str) -> bool:
    return bool(BRANCH_RE.match(name)) and ".." not in name and not name.endswith((".lock", "/", "."))


# ----------------------------------------------------------------------------
# Result of a finished job
# ----------------------------------------------------------------------------

def collect_result(worktree: Path, base: str) -> dict:
    code, head = paths.git(["rev-parse", "HEAD"], worktree)
    head = head if code == 0 else ""
    code, names = paths.git(["diff", "--name-status", f"{base}..HEAD"], worktree)
    changed = []
    if code == 0:
        for line in names.splitlines():
            status, _, path = line.partition("\t")
            changed.append({"status": status, "path": path})
    code, dirty = paths.git(["status", "--porcelain"], worktree)
    code, count = paths.git(["rev-list", "--count", f"{base}..HEAD"], worktree)
    code, branch = paths.git(["rev-parse", "--abbrev-ref", "HEAD"], worktree)
    conflicts = None
    if head and head != base:
        code, out = paths.git(["merge-tree", "--write-tree", "--name-only", base, head], worktree)
        conflicts = code != 0
    return {"commit": head, "short": head[:8], "branch": branch, "base": base, "commits": int(count) if count.isdigit() else 0,
            "changed": changed, "uncommitted": [l for l in dirty.splitlines()][:60], "conflicts": conflicts}


def diff_text(worktree: Path, base: str, max_chars: int = 200_000) -> dict:
    code, stat = paths.git(["diff", "--stat", f"{base}..HEAD"], worktree)
    code, body = paths.git(["diff", "--no-color", f"{base}..HEAD"], worktree, timeout=60)
    return {"stat": stat, "diff": body[:max_chars], "truncated": len(body) > max_chars}


def ai_paths(repo: Path, stamp: str) -> tuple[str, Path]:
    branch = f"hub-fix/{stamp}"
    root = paths.main_repo_root(repo) / ".claude" / "worktrees" / f"hubfix-{stamp}"
    return branch, root
