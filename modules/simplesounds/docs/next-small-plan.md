# Next-small task plan (2026-09-30)

Branch: codex-next-small. Worker worktree only; no push, merge, Forge changes,
other Minecraft ports, agents, or client tests.

## Existing flow

QoL config normalization runs at the server config reader. The two durability
mixins multiply attack damage and mining speed for any damageable held stack
above the durability threshold; they do not increase item durability.
The current multiplier default and cap are both 1.5. The custom interaction tab
uses the module's shared EN/DE language resources on both loaders.

Sounds observes the local player in its client tick mixin. SoundConfig resolves
effect overrides/global level; SoundBudget limits all playback. Simple Visuals
stores its global level as particles.globalLevel in config/simplevisuals.json;
its config screen saves changes there. There is no cross-module runtime API.

## Implementation stages and files

1. QoL: change the default/nonfinite fallback to 1, retain cap 1.5 and stored
   values, expand actual server-hook tests for Vanilla/SimpleBuilding/custom
   damageable stacks, update shared EN/DE, wiki and module documentation.
2. Sounds: default-on client-local followVisuals, optional loader detection,
   bounded reader of the saved Visuals level, live refresh after config saves,
   own-level fallback and effect-override precedence. Update config screen,
   language, wiki, data gate and existing module server catalogue.
3. Regenerate wiki, run relevant module suites, integration and filtered test
   centre suites; finish with gradlew.bat check -q and record actual results.

## Risks and decisions

- Cap 1.5 preserves the existing +50% maximum for combat/mining. Saved 1.5 is
  retained because it cannot be distinguished from an intentional owner choice.
- Sounds must remain usable without Visuals. No imports/reflection into another
  module's implementation. Read only its saved public JSON setting, at most once
  per 20 client ticks, with a size limit and safe fallback on invalid input.
  This is a data-file integration rather than a new shared runtime API; document
  that limitation and test refresh, missing/corrupt files and loader absence.
- No other module or shared build/runner implementation is edited. Manifest
  fields already satisfy the data contract; preserve all existing entries/tests.
- Language resources have one shared location per module, used by both loaders;
  there are no separate loader language files to duplicate.

## Verification and completion

Keep every existing case. Exercise actual Player attack/mining hooks on server,
not just normalization helpers. Check all five sound levels, overrides, fallback,
client option persistence, bilingual completeness and unchanged budgets/caps.
No client/audio acceptance is claimed from server tests. Final report includes
all stage commits, run ids/counts and exact remaining owner/orchestrator steps.
The latest addendum defers the complete merged server gate to the orchestrator.

## Results and deviations

Pending implementation and verification.
