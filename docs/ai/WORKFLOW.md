# AI-assisted workflow (how this repo is developed)

This file, `AGENTS.md` (rules), `docs/HANDOFF.md` (state) and `.claude/QUEUE.md` (queue) are all a new assistant or a new machine needs. The owner writes German; answers are short, result first, questions as a list.

## Roles
- **Orchestrator** (Claude Code or any assistant in the main checkout): reads requests, writes briefs, starts runs, reads reports, merges, runs gates, pushes. Keeps its own context small: it does not read whole logs.
- **Worker runs** (Codex CLI or `claude -p`, one per task, each in its own git worktree and branch `codex-<name>`): implement a brief, test, commit. They never push and never merge.
- **Gate**: a disposable worktree (`%TEMP%/sbgate`) where `check` and the test suites run against the exact commit that will be pushed.

## Loop for every wave
1. Write or update the owner's wish in `.claude/QUEUE.md`.
2. Write a brief per task (pieces in `docs/ai/briefs/`: `pre.md` = rules preamble, `mm-contract.md` = multi-mod contract, `port.md` = module port task, `mkports2.py` = generator for the module briefs). Keep each brief self-contained and testable: state the verification commands and the "green" lines to read.
3. Start runs: `python tools/ai/aitool.py codex <name> <brief.md>` (creates `%TEMP%/cx-<name>`, branch `codex-<name>`, log in `.ai-runs/`). Watch with `python tools/ai/aitool.py status`. Start at most 4 runs at once (CPU, RAM, disk: each worktree + Gradle build costs several GB; check `status` for free disk).
4. When a run finishes read only its final answer (`.ai-runs/out-<name>.txt`), then merge it. For module branches use `python tools/ai/aitool.py merge-module <name> --coauthor "<model> <noreply@...>"`: it merges, resolves the module registries per id (`tools/ai/merge_helpers/jsonreg3.py`) and the queue/handoff docs per union, regenerates wiki/quests, runs the derived-data checks and commits; it stops with the merge open when any other file conflicts. Other branches: `git merge --no-ff codex-<name>`. Conflicts: lang JSON -> 3-way merge by key (`tools/ai/merge_helpers/jsonmerge3.py <path>` while the merge is open); docs/queue -> `aitool.py merge-help union <files>`; generated wiki data -> take theirs and regenerate (`python wiki/generate.py --all`). Never resolve by dropping a module's tests.
5. On the 26.3 main line, regenerate derived files after merges: `python wiki/generate.py --all`, `python tools/quests/generate_quests.py`, `python tools/textures/generate_textures.py --check` (regenerate only after intentional texture changes), `python tools/guide_book_pages.py` (all have `--check`).
6. Gate: `python tools/ai/aitool.py gate --integration` (runs `check`, the 26.3 server suites, the integration suite and every module suite declared in the manifest in the gate worktree and prints `VERDICT`). Push only after GREEN: `python tools/ai/aitool.py gate --integration --push`.
7. Update `docs/HANDOFF.md` and the queue, commit, push.

## Traps learned the hard way
- `tools/testrunner/run.py` exits 0 even with red tests: read the `alles gruen` line. `--filter` takes ONE pattern (commas use only the first); a filter that matches no test makes Gradle fail.
- Every lang key must exist in BOTH `src/main/resources/assets/simplebuilding/lang/*.json` and `mc26_3/overlay/resources/assets/simplebuilding/lang/*.json` (the overlay overrides on 26.3).
- Agents that only ran filtered tests leave red full suites after merging: always run the full suite on the merged result.
- Two `@ModifyConstant` on the same literal crash at bootstrap when another mod (Architectury) touches it too: use MixinExtras `@ModifyExpressionValue`.
- Do not build in the main checkout while an owner client runs (NoClassDefFoundError); use the gate worktree.
- Codex's own sandbox cannot write the shared Gradle cache or worktree git metadata; the helper starts it with `--dangerously-bypass-approvals-and-sandbox` inside its worktree and every branch is reviewed before merging.
- Killing a session can kill child runs; check `status` after a crash and restart a continuation run with a note that uncommitted work exists in the worktree.

## Memory
The assistant's long-term notes live in `docs/ai/memory/` (copy of `~/.claude/projects/<project>/memory`). Sync: `python tools/ai/aitool.py sync-memory --to-repo` (before committing) and `--from-repo` (on a new machine). Treat them as background facts that may be outdated: the code wins.

## Tools inventory (what the dev flow uses)
- Launch & test hub `python tools/launchhub/server.py` (port 8771): starts clients/servers, test runs, AI fix runs, mods switchboard for the integration instance.
- Wiki `python -m http.server 8765 --directory wiki` and Balancing-Zentrale `python tools/devserver/serve.py` (port 8770); both have per-mod switchers.
- Codex CLI (`npm install -g @openai/codex`, `codex login`), Claude Code (`claude`).
- Dev mods for the test instances are listed and switchable in `tools/devmods.json`.

## Current facts and historical notes

HANDOFF describes the integrated 26.3 state, not the last worker branch. Queue checkboxes
mean implementation/verification complete, not owner acceptance. Keep client gates, visual
acceptance and owner decisions open separately. Old briefs and memory are historical inputs;
recipes, constants and executable code are evidence. Forge 26.3 is opt-in (`-Pforge263=true`);
the normal task gate remains Fabric/NeoForge 26.3 plus shared compilation.
