# Setting up a new machine (laptop) in one go

1. Install: Git, Java 25 (Zulu or Temurin; the repo also needs a Java 17/21 for some tools, see `gradle.properties` and `AGENTS.md`), Python 3.12, Node.js (for the Codex CLI), GitHub CLI (`gh auth login`), Gradle is provided by `gradlew`.
2. `git clone https://github.com/Ouzzi/simplebuilding` and `git fetch --all`. Read `AGENTS.md`, `docs/HANDOFF.md`, `docs/ai/WORKFLOW.md`.
3. AI tools: `npm install -g @openai/codex` then `codex login`; Claude Code from the official installer, then `claude` and sign in.
4. Restore the assistant memory: `python tools/ai/aitool.py sync-memory --from-repo`.
5. Gate worktree is created automatically on first `python tools/ai/aitool.py gate`; free disk needed: about 40 GB for the Gradle cache plus one or two worktrees.
6. First checks (in a scratch worktree, not while playing): `./gradlew.bat check -q` (Windows) or `./gradlew check -q`, then `python tools/testrunner/run.py --targets fabric-263,neoforge-263`. Expected: `alles gruen` (about 1560+ tests).
7. Dev tools: `python tools/launchhub/server.py` (hub on 8771), `python tools/devserver/serve.py` (balancing on 8770), wiki on 8765. The hub's `launch_targets.json` is path-independent; the integration instance uses its own run directory.
8. Mods folder for the dev client: the owner's 26.3 Fabric dev mods are managed through `integration/enabled-mods.json` and `tools/devmods.json`; data-pack mods written for older Minecraft formats crash world creation on 26.3 (see `docs/ai/WORKFLOW.md`, traps).
9. Secrets: none are stored in the repo. Git push needs GitHub auth; Codex/Claude need their own logins.
