# Dimensions settings task plan (2026-09-30)

Branch: `codex-next-dimensions`. Work only in this worktree; no push, merge, client tests, Forge changes, or other Minecraft ports.

## Current flow

`DimensionSettings` loads `config/simpledimension/server.json`. The Cloth screen saves local settings and applies them on the integrated server thread; remote clients cannot edit server settings. Loader interaction hooks call `PortalActivation`; server ticking and `SkyPortalBlock` route travel through `DimensionRuntime`. Outbound access currently checks both the global switch and the definition's `enabled` field, while generated return portals bypass those switches. `DimensionConfigStore` supplies dimension definitions and world generation independently.

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

Pending implementation and verification.
