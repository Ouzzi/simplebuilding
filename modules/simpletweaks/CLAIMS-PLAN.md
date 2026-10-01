# Claims implementation plan (2026-09-30)

Branch: codex-next-claims. Claims remain disabled by default at every stage.

## Current flow

The module currently registers a legacy deed and registry aliases only. No claim
data is loaded. Fabric and NeoForge share compatibility tests and have independent
server adapters. SimpleBuilding has existing tool protection checks; Dimensions
rejects detected claim mods without an adapter. Inspect actual callers before changes.

## Stages and files

1. Module-owned claim records/store, bounded server config, permission-aware command
   skeleton and deed behavior. Read the source repository in ignored scratchpad;
   preserve legacy NBT and refuse malformed input without replacing it.
2. Module-owned Vanilla server hooks, including clicked and adjacent target chunks.
3. Generic public permission contract and SimpleBuilding call sites, each affected
   wand/hammer/beam/teleporter target. No behavior change without an active provider.
4. Module-owned environmental/automation and indirect attack hooks.
5. UUID-based offline trust/untrust and explicit administration policy.
6. Dimensions permission adapter only; no settings or portal-shape edits.

Each stage has a separate commit after its relevant checks. Module resources,
tests, bilingual wiki/manual and balance metadata accompany the implementation.
QUEUE/HANDOFF receive append-only records. No Forge or other-line changes.

## Risks and decisions to verify

- Disabled mode must return before claim data lookup, creation, migration or writes.
- Persistence must be atomic and fail closed when enabled; malformed legacy data
  must remain untouched. Whitelist views must be immutable.
- Claims, trusted players, global records, cooldown and file sizes require caps.
- OP bypass, dimension allowlist, spawn and world-border policies must be explicit.
- Loader callbacks and shared tool helpers must be traced to actual mutations.
- Cross-border multi-target changes and unattributed automation need conservative
  checks. A helper-only test is not evidence that the game hook is protected.
- Legacy deeds are never authority; no gadget screen text or new pixel art.

## Verification

Use real server GameTests with owner and stranger on Fabric and NeoForge 26.3:
disabled behavior, legacy persistence round trip and malformed data, caps and
out-of-range config, command permissions, neighboring chunks, each installed
protection hook, tools and Dimensions adapter. Retain all existing tests.
Use module manifest targets and one relevant filter per invocation for base tests.
Read actual `alles gruen` results; process exit status alone is insufficient.
Run wiki generation/checks, module data/lang checks and final `gradlew.bat check -q`.
No worker client tests, full merged server gate, push or merge.

## Progress and deviations

- Plan recorded before implementation. AGENTS, HANDOFF, MULTIMOD, module audit and
  AI workflow/setup/voice guidance read. Source and implementation inspection next.
- Stage 1 implemented: immutable bounded ledger, atomic writes, read-only legacy
  import including unloaded dimensions, server config, deed and command skeleton.
  Source revision availability and hashes are recorded in audit/claims-source.json.
- Orchestrator command-collision finding resolved: disabled registration does not
  touch the dispatcher; enabled collision fails explicitly. Restart-bound settings
  are shared by command registration/reloads and the server runtime.
- Stage 1 verification: initial module suite 36/36; final claim-filtered run
  2026-09-30T21-34-00Z-cc24: Fabric 9/9, NeoForge 9/9, alles gruen 18/18.
  Includes actual deed use and Brigadier foreign-command regression. Module data
  and bilingual option completeness plus generated wiki check passed.
- Setup deviations: unavailable historical source commits recorded rather than
  assumed; Java 21 toolchain supplied locally; configure-on-demand avoids an
  unrelated Forge 26.2 dependency/bootstrap failure during module tests. An early
  unnamespaced test filter selected zero tests and is not counted as validation.
- Stage 2: actual Vanilla break/use/place, neighboring target, bed/chest footprint,
  bucket placement/pickup and entity interaction/melee/piercing hooks, Fabric and
  NeoForge only. Real owner/stranger controls and disabled hooks exercised.
  Run 2026-09-30T21-45-15Z-77ed: 13/13 per loader, alles gruen 26/26.
  Tests caught a bed fixture entity collision; moving players away preserved a
  meaningful positive control. No client, automation or indirect-damage claim.
- Stage 3 actual hook run 2026-09-30T22-48-57Z-7c6b: Fabric 18/18,
  NeoForge 18/18, alles gruen 36/36. Registry-resolved wand ticks, actual loader
  hammer area mining, beam item ticks, echo completion and pad block-entity ticks
  exercise owner and stranger behavior. Bed/straw-bed hammer controls included.
- Orchestrator reviews incorporated: provider contract lives in framework/;
  Minecraft geometry stays in the public SimpleBuilding adapter. A standard Java
  service installs the bridge only from mc26_3/framework. Both 26.3 loaders package
  the framework jar; no 26.2/Forge adapter, build change or behavior activation.
  Disabled/no-provider geometry returns before block reads. Break/undo/beam paths
  now check connected beds and chests; queued blueprint placement rechecks targets.
- Existing issue found outside claims scope: WandPlacement.stateFor applies neighbor
  updates before a bed head exists, turning the proposed foot into air, including
  for the owner. The attempted positive bed-wand test exposed that original flow.
  No baseline behavior change was made. Real hammer tests use hand-placed beds.
- Explicit remaining verification: all blueprint modes, multi-tick double-chest
  upgrades and every tool transformation have not been exercised with active claims.
- Stage 3 baseline regression: 2026-09-30T22-51-11Z-1a63,
  simplebuilding:*sledgehammer*: Fabric 35/35, NeoForge 35/35,
  alles gruen 70/70 without an active claims provider.

- Stage 4 continuation: existing draft retained, automation/natural-damage/security review and real regressions recorded in CLAIMS-STAGE4.md. Claims remains OFF and not enabled-ready; known unsupported automation paths are explicit activation blockers. Stages 5/6 are owned by the separate access worker; no implementation of those stages here.
