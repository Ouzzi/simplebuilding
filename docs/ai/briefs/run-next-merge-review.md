Repo SimpleBuilding, read AGENTS.md and docs/HANDOFF.md before work. You are a
read-only review worker in your own worktree; do not push, merge, edit files,
start clients, build, or run tests. Existing workers already ran their full
checks; the orchestrator is running the merged Claims/Dimensions suites.
Use Caveman/Ponytail and the repo's scoped search/symbol conventions. No agents.

Review the merged feature wave on base 5f294df9 against 821dd131. Scope:
Claims OFF/no-provider regressions; cross-branch interaction of automation's
nullable Protection.Target.actor with Dimensions access/landing/return checks;
three Dimensions setting toggles and preservation of safe return; framework
packaging/version compatibility with CosmeticIntensity; QoL default 1/cap 1.5
and actual tool eligibility; Sounds/Visuals optional provider/override precedence.
Read docs/ai/CODEX-PLAN.md and the module verification reports to distinguish
known limitations from regressions. Claims stage-4 activation gaps are explicitly
known and Claims must stay OFF; do not report those as newly discovered defects.
Forge is being implemented separately and is excluded from this review.

Compare final merged code, not branch-to-branch whole-file diffs. Focus on concrete
new correctness/security problems, preserving all current test IDs and defaults.
No speculative refactors or formatting requests. Return only actionable findings
with exact files/lines, trigger, impact and minimal correction; separately state
review limits and whether no additional defect was found. Do not claim execution
evidence. Normal English prose in code references; final report short German.
