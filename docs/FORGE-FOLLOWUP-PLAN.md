# Forge follow-up — 2026-10-01

## Baseline

Worktree `C:/Users/o_o/AppData/Local/Temp/cx-next-forge`, branch
`codex-next-forge`, base `5f294df9`. Claims, Dimensions settings/UI and QoL/Sounds
are merged. Java 25.0.4.1, Python 3.12.14 and the supplied Temurin 8u504 are
available. No builds, saves or clients in the main checkout will be touched.

Dimensions still has a scaffold Forge entrypoint and no manifest Forge target.
Tweaks' Forge adapter lacks the merged Claims lifecycle/hooks/catalog.
Forge conventions use framework only as an implementation dependency.
SimpleBuilding's inherited AutoConfig shim supplies defaults without persistence.
Module screens are excluded where Cloth is unavailable. Forge remains opt-in.

## Work and files

1. Compare canonical Fabric/NeoForge adapters and catalogs with Forge for all
   affected modules. Implement Dimensions registry, lifecycle, interactions,
   tint/config hook and full tests; update Tweaks Claims hooks/catalog and
   any genuinely stale Forge adapters. Preserve server settings, six arches,
   safe return and disabled Claims. Files: module-owned Forge trees/manifest.
2. Inspect official Forge/Cloth artifacts and installed sources. Reuse a compatible
   real dependency if available; otherwise implement the smallest native Forge
   settings screen, with validated persistent JSON and existing translations,
   tabs/defaults/ranges. Remote clients get no gameplay write path. Scope includes
   existing Forge users of the shim and excluded configuration screens.
3. Add generic framework runtime packaging and portable Java-8 test arguments,
   preserving overrides. Inspect distributable jars and start actual Forge server
   runtimes from packaged artifacts, including optional-provider absence/presence.
   Shared changes are confined to generic conventions/runner support as needed.
4. Generate/check wiki and data, run server catalogs with bounded concurrency,
   normal-loader regressions for shared changes, test-center coverage, default
   check and opt-in Forge check. Commit reviewable parts with the requested
   GPT-6 Astra trailer. Append evidence to HANDOFF/QUEUE only; no push/merge.

## Risks and verification

Forge registry wrappers, event cancellation, client class loading, dependency
deduplication and stale catalogs need runtime evidence. Preserve every current
test body/ID and compare counts, rather than accept compilation or exit 0.
Use one Gradle invocation at a time, at most two workers and bounded heaps on
this 24-GB machine. Read actual `alles gruen` lines and record run IDs/counts.
No worker client launches: compile client hooks and document exact serial client
commands for the orchestrator. GUI rendering, actual player network delivery,
audio and owner-world behavior remain unverified. Without actual client evidence,
Forge remains experimental and opt-in; no new owner policy question is needed.

## Findings and deviations

This section will record artifact evidence, necessary generic changes and actual
verification results as work proceeds.

- Official evidence retrieved 2026-10-01: Forge's files page lists 66.0.9 and
  the pinned 66.0.8; this task keeps the installed/pinned 66.0.8. Cloth's official
  Maven metadata reports `cloth-config-forge` latest/release 17.0.144 and no 26.x
  version. Fabric/NeoForge publish 26.3.159; inspection of the actual 26.3.159
  NeoForge jar shows a required `neoforge [26.3.0.3-beta,)` dependency and bundled
  AutoConfig. It is not a Forge-compatible replacement. Sources:
  https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html and
  https://maven.shedaniel.me/me/shedaniel/cloth/cloth-config-forge/maven-metadata.xml
  (corresponding `cloth-config-fabric`/`cloth-config-neoforge` metadata also read).
- Native screen declarations reuse the canonical module options through a generic
  build-time import adaptation. Actual Minecraft widgets live in a small template,
  relocated into each module's own namespace; there are no fake Cloth GUI classes,
  cross-module internal imports or extra runtime mod requirements. Server settings
  remain local drafts except Dimensions' existing integrated-server-thread apply.
- Scope correction from adapter comparison: Money's later merged Money Links also
  lacks Forge loaded-mod detection and its four mixins. Both are necessary for the
  existing full catalog, so this follow-up includes them. Sounds' adapter gains
  the same real loader/provider-presence assertion as Fabric/NeoForge.
- Generic additions: nested framework packaging, namespace-based test selection,
  portable Java-8 override handling, optional manifest-resolved `forgeTestMods`
  selection and `forgePackaged` to exercise jars without project output or a loose
  framework jar. No default activation or deferred-line source edit.
- Initial build fills a cold Forge 26.3 Mavenizer cache (decompile/recompile).
  One Gradle invocation, two workers, 2-GiB Gradle heap. No client launched.
  The Java-8 override regression and manifest/Dimensions producer checks pass.
- Actual jar inspection found an additional packaging defect: the base Forge jar
  omitted the four `common/src/main` bootstrap classes. The 26.3 jar now includes
  that existing output. Other Minecraft lines and loader artifacts are untouched.
- Forge 66.0.8 has a two-argument BlockEntityType constructor and an
  `onlyOpCanSetNbt()` hook, unlike the canonical loaders' security-flag constructor.
  Dimensions overrides that hook for both portal types, preserving the existing
  nonoperator forged-item-NBT regression rather than weakening its expectations.
- The first packaged startup exposed duplicate framework modules. FG7 independently
  adds SourceSet runtime files to JavaExec and to its runtime token; the probe must
  remove loose libraries/project output from both. Gradle task configuration must
  run after FG7's registration action. These failed attempts are recorded as red,
  not counted as runtime verification.
- The first real catalog ran 40/41. `claims_linked_travel` starts at tick 155
  while `settings_skyblock` (tick 130) intentionally disables portal access.
  Settings tests now have a separate environment/batch on Fabric, NeoForge and
  Forge 26.3. All original test bodies, IDs, delays and assertions are retained;
  this module-owned test-isolation fix requires the normal-loader catalog rerun.
- Framework metadata/payload also enter processed resources, with compile-only
  linkage, preventing duplicate loose/nested APIs in ordinary development runs.
  The base Forge project compiles its four existing common bootstrap sources
  directly, so both development output and jars are self-contained.
- The full packaged sweep exposed a shared Gradle coordinate collision: every
  module Forge project was `com.simplebuilding.modules:forge`, so dependency
  resolution dropped Dimensions from the Tweaks runtime. Groups now include the
  manifest-discovered module id; this is a generic convention, not a module switch.
- Minecraft's `runBeforeTestEnd` schedules an assertion at timeout minus one; it
  is not cleanup. The merged Dimensions journeys leaked disabled settings to the
  next run, and the Claims portal fixture leaked its server provider after failure.
  The initial worker fix used GameTestListener on success/failure. During the
  run the orchestrator independently landed a5d8151e on master; its two exact
  Java test changes are now incorporated instead, preserving idempotent cleanup
  on success/failure and the timeout fallback without another test accessor.
  Forge's `addCleanup` is unavailable on vanilla/Fabric, so that Forge-only
  convenience method cannot be used by the canonical test body.
  The corrupted automated Dimensions settings were backed up and reset only in
  this worktree's GameTest directory. No owner config or save was touched.
- The packaged base catalog found a scan-order-dependent command-tree ordering
  failure (800/801); its 26.3 adapter now registers the shared command root at HIGH
  priority before extensions. The existing strict command test remains unchanged.
- Final server runs, repeated byte-identical settings, artifact inspection and
  both successful check gates are recorded in `FORGE-FOLLOWUP-RESULTS.md`.
  Forge remains experimental and opt-in. No clients, pushes or merges occurred.
