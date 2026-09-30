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

The data-file approach was rejected during implementation: it would follow saved data,
not necessarily the active level, and fall outside the strict framework API contract.
Use a generic optional CosmeticIntensity supplier registry in framework 0.1.1 instead.
Visuals publishes its live level once at initialization; Sounds reads that API in its
existing playback resolver. Absence needs no loader-specific probing and no hard mod
dependency. The shared API is generic by mod id; no root build/runner/Hub blocks change.
Necessary scope extension: framework API/version and the Visuals publisher, producer
regression and nested-library declarations in both modules' Fabric/NeoForge builds.
No other module language/manual is edited. Visuals module suites become relevant tests.
The API library is bundled as a deduplicated nested jar on both loaders so normal jars
work without relying on the development classpath. Verification completed; results follow.

## Completed verification

- Plan commit: `8148c989`; QoL implementation: `b606e27d`; Sounds/API: `7f3ffbbc`.
- QoL Fabric/NeoForge: **48/48, alles gruen**, `2026-09-30T20-53-16Z-7aed`.
- Sounds/Visuals Fabric/NeoForge plus integration: **71/71, alles gruen**,
  `2026-09-30T21-22-48Z-11f3` (17+17 Sounds, 18+18 Visuals, 1 integration).
- Strengthened actual loader/provider presence: **2/2, alles gruen**,
  `2026-09-30T21-27-22Z-2913`; loaded Visuals on Fabric, absent on NeoForge.
- Both isolated test centres rebuilt, full item/block coverage: **10/10, alles gruen**,
  `2026-09-30T21-31-02Z-670a`, single filter `simplebuilding:*test_centre*`.
- Default wiki generation and check, both changed module generations and all-module
  wiki check passed. Generated files were never edited by hand.
- Final `gradlew.bat :modules:simplesounds:neoforge:jar
  :modules:simplevisuals:neoforge:jar check -q`: **FINAL_CHECK_EXIT=0**.
  Log: `scratchpad/next-small/final-check.log`; includes shared/26.2 compilation.
- Both Fabric and both NeoForge jar archives contain the framework 0.1.1 API with
  loader nesting/deduplication metadata. The NeoForge dependency range is `[0.1.1,)`.
- The first test launch failed during cold ForgeGradle configuration before tests:
  Java 8 was missing and automatic provisioning had a checksum mismatch. A local
  Temurin 8 download was checked against the official package checksum; subsequent
  commands set JAVA_OPTS with org.gradle.project.org.gradle.java.installations.paths
  to `scratchpad/next-small/java8/jdk8u504-b01` in this worktree. No setup/source
  change in the main checkout, no Forge runtime, and no deferred-line source edits.

## Remaining steps and limits

The orchestrator must review the generic API/Visuals scope extension and run the
complete merged gate. Owner acceptance: cap 1.5 (+50% maximum), preservation of old
1.5 values, client config/audio behavior, and `/sbtestcentre build` in the owner
world. No blocking implementation question remains. No worker client tests, actual
third-party tool mods, arbitrary modpacks, owner saves, Forge runtime/library
packaging, or other Minecraft runtime lines were tested. No new pixel art.
No push or merge. The pre-existing untracked `.serena/` directory is left alone.
