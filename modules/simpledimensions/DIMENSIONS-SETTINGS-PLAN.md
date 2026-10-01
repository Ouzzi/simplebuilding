# Dimensions settings task plan (2026-09-30)

Branch: `codex-next-dimensions`. Work only in this worktree; no push, merge, client tests, Forge changes, or other Minecraft ports.

## Current flow

`DimensionSettings` loads `config/simpledimension/server.json`. The Cloth screen saves local settings and applies them on the integrated server thread; remote clients cannot edit server settings. Loader interaction hooks call `PortalActivation`; registered server tick hooks route player contact through `DimensionRuntime` (`SkyPortalBlock` supplies the surface and visuals). Outbound access originally checked both the global switch and the definition's `enabled` field, while generated return portals bypassed those switches. `DimensionConfigStore` supplies dimension definitions and world generation independently.

## Implementation stages

1. Record this plan and append the request to the queue.
2. Add three default-on server settings in their own Dimensions tab. Centralize access checks for ignition and both new and already-linked outbound travel, preserving generated return paths. Built-in dimension access must use these settings instead of the per-definition `enabled` field; retain compatibility for custom definitions. Reuse the current persistence and server-thread application flow.
3. Remove the shipped copper/blue-ice example and production preset. Preserve the existing recipe/parser test coverage using test-owned fixtures. Keep the six glowstone arches and separate light unchanged. Update module-only configuration metadata, EN/DE resources, wiki manual and design documentation.
4. Extend both existing loader test catalogues with real interaction/server-tick off/on/return tests for all three dimensions, persistence checks, and config/default/tab/language checks. Preserve all existing tests.
5. Run the module data check, wiki generation/checks, both complete Dimensions server catalogues, the relevant test-centre filter if needed, and final `gradlew.bat check -q`. Read actual results, including `alles gruen`. Append results to HANDOFF/QUEUE and commit on this branch with the requested GPT-6 Astra trailer.

## Files and boundaries

Expected edits: module settings, config screen, activation/runtime, portal presets, test bodies/adapters, config-options.json, module lang/wiki/tools, `docs/modules/simpledimensions.md`; append-only HANDOFF/QUEUE. Manifest fields are inspected for completeness, with no unrelated module entry changes. Generated wiki output may be refreshed by the generator but shared derived outputs belong to the orchestrator's regeneration step if required by the plugin boundary. No root build/runner/Hub changes.

## Risks and verification limits

- A linked outbound portal must not bypass a disabled setting; a generated return portal must continue to work after persistence/reload.
- Settings must gate access without removing loaded dimensions or rewriting existing dimension definition files.
- Old built-in `enabled` values must not remain a second hidden switch. Custom definition behavior remains compatible.
- Tests must exercise the real loader interaction hooks and server tick travel, not only direct helpers. Shared runtime state and per-tick destination budgets require sequential test stages and cleanup.
- Language resources are currently shared by both loader builds; verify actual resource delivery and both EN/DE dictionaries without editing SimpleBuilding's language files.
- No new portal mechanics, shape configurator, assets, or dependency. No client visual acceptance or real owner-world test claimed.
- The current orchestrator addendum supersedes the earlier full-gate request: only relevant module/filtered server tests and final check here; complete merged gate remains with the orchestrator.

## Outcomes and deviations

- Plan commit: `37293dc4`. Implementation, tests, languages and documentation: `f83a75c4`.
- Added `skyblockEnabled`, `miningEnabled`, `travelEnabled`, all default on, and the dedicated Dimensions tab. Both ignition and outbound travel paths share the server-owned access predicate; generated return links bypass it. Settings save atomically and local GUI application stays on the server thread. New built-in definitions omit `enabled`; existing definitions remain byte-for-byte untouched and their legacy flag is ignored for built-ins.
- Removed the alternate preset/example. Deviation from step 3: retained all existing test methods and adapted preset assertions to the shipped six arches instead of preserving a removed preset as a new test fixture. Existing mixed-recipe parser coverage remains unchanged.
- Both loader builds use the same module-owned language resource directory. No SimpleBuilding/other-module language file was edited. Module data checks cover names, defaults, scope, category, tooltips and GUI save bindings; both runtime suites read both shipped locales.
- Four additional server cases per loader: three serialized real `ServerPlayerGameMode.useItemOn`/registered-tick journeys and independent persistence/default/legacy tests. No direct travel or tick-helper calls in the new journey tests. Cases exercise disabled ignition, unlinked and linked outbound refusal, reenablement, safe exact returns while off after disk reload, and unchanged definition files. Existing 34 server cases per loader remain intact.
- Full Dimensions suites: `2026-09-30T20-51-56Z-7330`, **alles gruen: 76/76 bestanden, 0 rot** (38 per loader). No failed intermediate game-test run.
- Filtered test-centre suites: `2026-09-30T21-05-46Z-cc12`, **alles gruen: 10/10 bestanden, 0 rot**. Both isolated centres rebuilt, complete item/block coverage passed; owner world untouched.
- Default and module wiki generation/checks passed. Outside the module/doc paths, the generator refreshed only `wiki/data/simpledimensions.json`, its JS counterpart and the manifest-driven wiki index. This derived-output exception keeps `checkWiki` reproducible; no shared implementation or build/runner/Hub change. All manifest entries were already complete (`tools/multimod.py` passed), so no entry needed editing.
- No client tests, external claim integration, owner-world acceptance, Forge runtime or other-Minecraft-line ports. Server tests prove persistence by disk load and runtime reconstruction, not a physical dedicated-server process restart. Complete merged gate remains the orchestrator's responsibility.
- Final `gradlew.bat check -q`: **GRADLE_EXIT=0**, output read. All 23 JUnit tests across the five existing catalogues passed, no failures/errors/skips; 19 wiki tests passed; balance reported 223 generated locations, 0 errors. All standard module/data/atlas/Jade/wiki checks and existing shared/26.2 compilation passed. Log: `scratchpad/dimensions-settings/final-check.log`. The long first build populated missing Minecraft/NeoForge caches; it did not port or modify deferred-line sources.
- Remaining steps: orchestrator integrates the branch and runs `python tools/ai/aitool.py gate --integration` on the merged result. Owner accepts the Dimensions tab and real travel on Fabric/NeoForge, including dedicated-server restart/multiplayer and the owner-world test centre. No new implementation decision is needed. Forge, other Minecraft runtime lines and third-party claim adapters remain separate work. No push or merge performed.
