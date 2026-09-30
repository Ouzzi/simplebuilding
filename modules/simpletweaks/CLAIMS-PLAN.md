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
