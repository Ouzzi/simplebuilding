# Task: fix failing SimpleBuilding tests

You run headless, unattended, in a fresh git worktree: `{{worktree}}` on branch `{{branch}}`
(base commit `{{base}}`). Work only there, never in the main repository.
Read `AGENTS.md` (all rules) and `docs/HANDOFF.md` (current state) before you change anything.

## Failing tests
Target(s): {{targets}}
Recorded at commit `{{run_commit}}` (run {{run_ids}}). {{stale_note}}

{{failures}}

## File hints
{{file_hints}}

## Re-run commands (one filter pattern per call)
{{rerun_commands}}

## Working rules (compact copy, `AGENTS.md` is authoritative)
- Main line is MC 26.3 (`fabric-263`, `neoforge-263`). Fix and test there first; shared code must still compile for 26.2.
- Decide whether the test or the code is wrong. A comment is not proof; the code is. Do not weaken a test just to make it green.
- Push nothing. Do not merge. Do not touch the main repository or any other worktree.
- Never build in the main repository while the owner's clients run (`NoClassDefFoundError` in the running game).
- No on-screen text for gadgets (no action bar, no chat): use sounds, particles, block states.
- English text in American English; keep `en_us.json` and `de_de.json` in step; check for duplicate lang keys.
- Server authority: gameplay knobs stay server-side with upper limits.
- `tools/testrunner/run.py` exits 0 even when tests are red. Green means the line "alles gruen" in its output.
- After changing wiki-relevant facts run `python wiki/generate.py` and `python wiki/generate.py --check`.

## Token-saving method
- Read the failing test first (only the needed line range), then the code it exercises. Do not open whole large files.
- Run filtered tests, **one pattern per call** (a comma silently uses only the first pattern). Never re-run full suites needlessly.
- Start with the fastest proof: compile, then the single failing test, then its test class, then the neighbors.
- Do not print long logs; grep or read the failure lines only.
- Stop after two failed hypotheses on the same test and report what you learned instead of guessing further.

## Deliverable
1. Commit on the branch `{{branch}}` (small, focused commits; message ends with your co-author line).
2. Do not push.
3. Final message: branch, commit hash, what was wrong, which tests you ran with their result, what is still red and why.

{{extra}}
