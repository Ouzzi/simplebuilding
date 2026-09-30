# Weiterarbeiten nur mit Codex (ChatGPT-CLI), solange das Claude-Limit voll ist

Stand 2026-09-30: Claude-Wochenlimit bei 99 %, Reset 2026-10-01 ca. 17:00 (MESZ). GPT-Abo frei (Reset 2026-10-06).

## Start
1. Terminal im Repo (`C:\Users\o_o\code\simplebuilding`), `git pull`.
2. `codex` starten (interaktiv) und als ersten Prompt: 
   `Lies docs/ai/CODEX-HANDOVER.md und arbeite dort weiter. Du bist Orchestrator (AGENTS.md, docs/ai/WORKFLOW.md).`
3. Oder headless: `pwsh -NoProfile -File $env:USERPROFILE\.claude\tools\gpt\gpt.ps1 -PromptFile <datei> -Dir C:\Users\o_o\code\simplebuilding -Mode auto -Effort high`

## Regeln
- Codex liest `AGENTS.md` selbst; `.claude/QUEUE.md` ist die Wahrheit über den Stand.
- Läufe: `python tools/ai/aitool.py codex <name> <brief>`, mergen mit `merge-module`, dann `gate --integration`. Push nur geprüfte SHA nach "VERDICT: GREEN".
- Nie im Haupt-Checkout bauen, solange der Minecraft-Client läuft (Gate-Worktree).
- Offene Punkte stehen in `docs/ai/CODEX-HANDOVER.md` (Codex pflegt die Datei).

## Zurück zu Claude
Nach Limit-Reset: `git pull`, in Claude Code `docs/ai/CODEX-HANDOVER.md` + `.claude/QUEUE.md` lesen lassen, dann normal weiter.
