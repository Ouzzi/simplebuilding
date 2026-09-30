#!/usr/bin/env python3
"""Small helper for the AI-assisted workflow (see docs/ai/WORKFLOW.md).

    python tools/ai/aitool.py codex <name> <brief.md> [--base master] [--provider codex|claude]
    python tools/ai/aitool.py status
    python tools/ai/aitool.py gate [--ref master] [--integration] [--push]
    python tools/ai/aitool.py sync-memory --to-repo | --from-repo
    python tools/ai/aitool.py merge-help union|ours|theirs <files...>
    python tools/ai/aitool.py merge-module <branch> [--coauthor "Name <mail>"]

Everything is stdlib only. Paths derive from the repository location, never from a user name.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
TMP = Path(os.environ.get("TEMP") or tempfile.gettempdir())
RUNS = REPO / ".ai-runs"          # logs and final answers (gitignored)
GATE = TMP / "sbgate"             # disposable worktree for gates


def run(argv, cwd=REPO, check=True, capture=False, **kw):
    return subprocess.run(argv, cwd=cwd, check=check, text=True, encoding="utf-8",
                          capture_output=capture, **kw)


def out(argv, cwd=REPO):
    return run(argv, cwd=cwd, check=False, capture=True).stdout.strip()


# ---------------------------------------------------------------- codex / claude runs
def cmd_codex(a):
    """Create a worktree + branch and run a headless agent with the brief. Never pushes or merges."""
    RUNS.mkdir(exist_ok=True)
    wt = TMP / f"cx-{a.name}"
    branch = f"codex-{a.name}"
    if not wt.exists():
        run(["git", "worktree", "add", "-q", "-b", branch, str(wt), a.base])
    brief = Path(a.brief).read_text(encoding="utf-8")
    log = RUNS / f"log-{a.name}.txt"
    last = RUNS / f"out-{a.name}.txt"
    if a.provider == "codex":
        # No sandbox: the Codex sandbox cannot write the shared Gradle cache or git metadata of a worktree.
        # The run is confined by convention to its own worktree; review every branch before merging.
        argv = ["codex", "exec", "--dangerously-bypass-approvals-and-sandbox", "-C", str(wt),
                "-o", str(last), "-"]
    else:
        argv = ["claude", "-p", "--permission-mode", "acceptEdits"]
    exe = shutil.which(argv[0])  # npm installs codex as codex.cmd on Windows
    if not exe:
        sys.exit(f"'{argv[0]}' not found in PATH; install it first (see docs/ai/LAPTOP-SETUP.md)")
    argv[0] = exe
    flags = subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0
    # The brief goes in through a file handle (a pipe can deadlock on Windows when the child is a .cmd shim).
    brief_file = RUNS / f"brief-{a.name}.md"
    brief_file.write_text(brief, encoding="utf-8")
    with open(log, "wb") as lf, open(brief_file, "rb") as bf:
        p = subprocess.Popen(argv, cwd=wt, stdin=bf, stdout=lf, stderr=subprocess.STDOUT, creationflags=flags)
    print(f"started {a.provider} run '{a.name}' pid {p.pid}\n worktree {wt}\n branch   {branch}\n log      {log}\n answer   {last}")


def cmd_status(_a):
    """One line per agent worktree: commits ahead of master, dirty files, finished?"""
    print(f"{'run':<22}{'log KB':>8}{'commits':>9}{'dirty':>7}  state")
    for wt in sorted(TMP.glob("cx-*")):
        name = wt.name[3:]
        if not (wt / ".git").exists():
            continue
        ahead = out(["git", "rev-list", "--count", "master..HEAD"], wt) or "?"
        dirty = len(out(["git", "status", "--short"], wt).splitlines())
        log = RUNS / f"log-{name}.txt"
        last = RUNS / f"out-{name}.txt"
        kb = log.stat().st_size // 1024 if log.exists() else 0
        state = "FINISHED" if last.exists() and last.stat().st_size else ("running?" if log.exists() else "no log")
        print(f"{name:<22}{kb:>8}{ahead:>9}{dirty:>7}  {state}")
    free = 0
    try:
        import shutil
        free = shutil.disk_usage(TMP).free // 2**30
    except OSError:
        pass
    print(f"disk free: {free} GB")


# ---------------------------------------------------------------- gate
def cmd_gate(a):
    """Gate in the disposable worktree (never in the main repo: a running client would crash)."""
    if not GATE.exists():
        run(["git", "worktree", "add", "-q", "--detach", str(GATE), a.ref])
    run(["git", "checkout", "-q", "--", "."], cwd=GATE)
    run(["git", "clean", "-fdq", "modules", "integration"], cwd=GATE, check=False)
    run(["git", "checkout", "-q", "--detach", a.ref], cwd=GATE)
    print("gate on", out(["git", "log", "--oneline", "-1"], GATE))
    gradle = str(GATE / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    ok = True

    r = run([gradle, "check", "-q"], cwd=GATE, check=False)
    print("check:", "OK" if r.returncode == 0 else f"FAILED ({r.returncode})")
    ok &= r.returncode == 0

    def runner(targets):
        r = run([sys.executable, "tools/testrunner/run.py", "--targets", targets], cwd=GATE, check=False, capture=True)
        text = r.stdout
        green = re.search(r"alles gruen: (\d+)/(\d+)", text)
        print(f"tests [{targets}]:", green.group(0) if green else "NOT GREEN")
        if not green:
            print("\n".join(l for l in text.splitlines() if "ROT " in l or "FEHLER" in l)[:3000])
        return bool(green)

    ok &= runner("fabric-263,neoforge-263")
    if a.integration:
        # The integration suite plus every module suite the manifest at this ref declares (client smokes stay manual).
        listing = run([sys.executable, "tools/testrunner/run.py", "--list"], cwd=GATE, check=False, capture=True).stdout
        modules = sorted(set(re.findall(r"^\s+(module-[a-z0-9_]+-(?:fabric|neoforge)-263)", listing, re.M)))
        ok &= runner(",".join(["integration-263"] + modules))
    print("VERDICT:", "GREEN" if ok else "RED")
    if ok and a.push:
        if out(["git", "status", "--short", "--untracked-files=no"]):
            print("main repo has uncommitted changes, not pushing")
            return 1
        run(["git", "push", "origin", a.ref])
    return 0 if ok else 1


# ---------------------------------------------------------------- memory sync
def memory_dir() -> Path:
    # Claude Code keys the per-project memory by the project path: every separator/colon/space becomes '-'.
    key = re.sub(r"[^A-Za-z0-9]", "-", str(REPO))
    return Path.home() / ".claude" / "projects" / key / "memory"


def cmd_sync_memory(a):
    src_repo = REPO / "docs" / "ai" / "memory"
    mem = memory_dir()
    if a.to_repo:
        if not mem.is_dir():
            sys.exit(f"no memory directory at {mem}")
        src_repo.mkdir(parents=True, exist_ok=True)
        n = 0
        for f in mem.glob("*.md"):
            (src_repo / f.name).write_bytes(f.read_bytes())
            n += 1
        print(f"copied {n} memory files to {src_repo}")
    else:
        mem.mkdir(parents=True, exist_ok=True)
        n = 0
        for f in src_repo.glob("*.md"):
            target = mem / f.name
            if not target.exists() or a.force:
                target.write_bytes(f.read_bytes())
                n += 1
        print(f"installed {n} memory files into {mem} (existing files kept unless --force)")


# ---------------------------------------------------------------- merge helpers
def cmd_merge_help(a):
    pat = re.compile(r"<<<<<<< [^\n]*\r?\n(.*?)=======\r?\n(.*?)>>>>>>> [^\n]*\r?\n", re.S)
    pick = {"union": lambda m: m.group(1) + m.group(2), "ours": lambda m: m.group(1), "theirs": lambda m: m.group(2)}[a.mode]
    for f in a.files:
        p = Path(f)
        s = p.read_text(encoding="utf-8")
        p.write_text(pat.sub(pick, s), encoding="utf-8", newline="")
        print("resolved", f, "with", a.mode)


def cmd_merge_module(a):
    """Merge a finished module branch: registries per id, docs per union, derived data regenerated, checks, commit.

    Stops (merge left open) when a file other than the known registries/docs conflicts."""
    branch = a.branch if a.branch.startswith("codex-") else f"codex-{a.branch}"
    r = run(["git", "merge", "--no-ff", "--no-commit", branch], check=False, capture=True)
    conflicts = out(["git", "diff", "--name-only", "--diff-filter=U"]).split()
    registries = {"modules/modules.json", "integration/enabled-mods.json"}
    docs = {".claude/QUEUE.md", "docs/HANDOFF.md"}
    generated = [c for c in conflicts if c.startswith("wiki/data/")]
    unknown = [c for c in conflicts if c not in registries | docs and c not in generated]
    if unknown:
        print("unresolved conflicts (merge left open):")
        for name in unknown:
            print("  " + name)
        return 1
    if registries & set(conflicts):
        run([sys.executable, "tools/ai/merge_helpers/jsonreg3.py"])
    for f in sorted(docs & set(conflicts)):
        cmd_merge_help(argparse.Namespace(mode="union", files=[f]))
    steps = [
        ("multimod", [sys.executable, "tools/multimod.py", "--check"]),
        ("wiki", [sys.executable, "wiki/generate.py", "--all"]),
        ("quests", [sys.executable, "tools/quests/generate_quests.py"]),
        ("wiki tests", [sys.executable, "-m", "unittest", "discover", "-s", "wiki/tests"]),
        ("guide pages", [sys.executable, "tools/guide_book_pages.py"]),
        ("textures", [sys.executable, "tools/textures/generate_textures.py", "--check"]),
    ]
    ok = True
    for label, argv in steps:
        res = run(argv, check=False, capture=True)
        print(f"{label}: {'OK' if res.returncode == 0 else 'FAILED'}")
        if res.returncode:
            print((res.stdout + res.stderr)[-1500:])
        ok &= res.returncode == 0
    run(["git", "add", "-A", ".claude/QUEUE.md", "docs/HANDOFF.md", "modules/modules.json", "integration/enabled-mods.json", "wiki"])
    left = out(["git", "diff", "--name-only", "--diff-filter=U"])
    if left or not ok:
        print("not committed: fix the points above, then `git commit`")
        return 1
    msg = f"Merge {branch}"
    trailer = a.coauthor or os.environ.get("AITOOL_COAUTHOR")
    if trailer:
        msg += "\n\nCo-Authored-By: " + trailer
    run(["git", "commit", "-q", "-m", msg])
    print("committed", out(["git", "log", "--oneline", "-1"]))
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("codex"); c.add_argument("name"); c.add_argument("brief"); c.add_argument("--base", default="master")
    c.add_argument("--provider", choices=["codex", "claude"], default="codex"); c.set_defaults(fn=cmd_codex)
    sub.add_parser("status").set_defaults(fn=cmd_status)
    g = sub.add_parser("gate"); g.add_argument("--ref", default="master"); g.add_argument("--integration", action="store_true")
    g.add_argument("--push", action="store_true"); g.set_defaults(fn=cmd_gate)
    m = sub.add_parser("sync-memory"); grp = m.add_mutually_exclusive_group(required=True)
    grp.add_argument("--to-repo", action="store_true"); grp.add_argument("--from-repo", action="store_true")
    m.add_argument("--force", action="store_true"); m.set_defaults(fn=cmd_sync_memory)
    h = sub.add_parser("merge-help"); h.add_argument("mode", choices=["union", "ours", "theirs"]); h.add_argument("files", nargs="+")
    h.set_defaults(fn=cmd_merge_help)
    mm = sub.add_parser("merge-module"); mm.add_argument("branch"); mm.add_argument("--coauthor"); mm.set_defaults(fn=cmd_merge_module)
    a = ap.parse_args()
    sys.exit(a.fn(a) or 0)


if __name__ == "__main__":
    main()
