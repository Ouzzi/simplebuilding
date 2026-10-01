# Forge follow-up — 2026-10-01

## Baseline

Worktree `C:/Users/o_o/AppData/Local/Temp/cx-next-forge`, branch
`codex-next-forge`, base `5f294df9`. Claims, Dimensions settings/UI and QoL/Sounds
are merged. Java 25.0.4.1, Python 3.12.14 and the supplied Temurin 8u504 are
available. No builds, saves or clients in the main checkout will be touched.

Dimensions still has a scaffold Forge entrypoint and no manifest Forge target.
Tweaks' Forge adapter lacks the merged Claims lifecycle/hooks/catalogue.
Forge conventions use framework only as an implementation dependency.
SimpleBuilding's inherited AutoConfig shim supplies defaults without persistence.
Module screens are excluded where Cloth is unavailable. Forge remains opt-in.

## Work and files

1. Compare canonical Fabric/NeoForge adapters and catalogues with Forge for all
   affected modules. Implement Dimensions registry, lifecycle, interactions,
   tint/config hook and full tests; update Tweaks Claims hooks/catalogue and
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
4. Generate/check wiki and data, run server catalogues with bounded concurrency,
   normal-loader regressions for shared changes, test-centre coverage, default
   check and opt-in Forge check. Commit reviewable parts with the requested
   GPT-6 Astra trailer. Append evidence to HANDOFF/QUEUE only; no push/merge.

## Risks and verification

Forge registry wrappers, event cancellation, client class loading, dependency
deduplication and stale catalogues need runtime evidence. Preserve every current
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
