# Multimod development (26.3 foundation)

SimpleBuilding keeps its id, packages, source trees, Gradle projects, datagen and run tasks.
`modules/modules.json` describes it alongside additional mods. `settings.gradle` discovers
additional `:modules:<id>:fabric` and `:modules:<id>:neoforge` projects from that manifest.
No module discovery changes the 26.2, 1.21.11 or opt-in 26.4 projects.

## Add a module

Run `python tools/newmod.py mymod "My Mod"` from the repository. The generator refuses existing
ids/directories, copies `tools/templates/module`, registers both loader projects and enables
the module in the integration selection. New source belongs in `modules/mymod/shared/java`,
with loader entrypoints/resources in `fabric/` and `neoforge/`. The two convention scripts
under `gradle/module-*.gradle` pin Minecraft 26.3 and Java 25. Scaffold version starts at 0.1.0;
set `ext.moduleVersion` before applying the convention when changing it, and update metadata
and the manifest together. The initial plain token is wiring evidence; replace it with the
module's implementation. It reuses Vanilla paper rendering, so no pixel art is introduced.

`framework/` is a loader-neutral Java API skeleton with its own version and API major constant.
SimpleBuilding does not depend on it. Modules must not import one another's implementation
packages. Use framework API contracts or public registry ids plus Vanilla interfaces. Declare
required mod/version dependencies in loader metadata; optional interactions must check whether
the other mod is loaded. Version the framework independently; breaking API changes require a
major-version decision and an integration compatibility test. Registry ids are persistent data.

## Integration instance

`integration` is a dedicated Fabric 26.3 Loom project. Run:

```
gradlew.bat :integration:runIntegrationClient
gradlew.bat :integration:runIntegrationServer
python tools/testrunner/run.py --targets integration-263
```

All use `integration/run-fabric-263`, exclusively owned by the integration instance. No existing
run directory or owner save is reused. Only one integration process may use it at once. Create
the client world named **Integration** (flat, creative, cheats enabled); “fresh world” archives
that named save under `hub-old-worlds`, without deleting it. Server worlds and gametest worlds
also remain under this separate run directory. Stop the instance before changing its selection.

`integration/enabled-mods.json` lists enabled repo module and dev-mod ids. Gradle reads it during
run preparation, builds repo jars, resolves only selected dev-mod configurations and syncs only
selected jars into the integration mods directory. Sync removes stale jars in that generated
directory, never worlds. Disabled repo projects may still compile: the runtime jar set is the
selection boundary. Fabric Loader/API and the integration harness are infrastructure, always
loaded. SimpleBuilding requires Cloth Config; an incompatible selection is rejected before launch.
The harness test requires SimpleBuilding and wiringexample and proves storing/removing the foreign
token in a SimpleBuilding reinforced hopper through public registry ids and Vanilla Container.
It has its own namespace, catalogue, report and target; existing counts/default targets are unchanged.
NeoForge example projects compile, but a NeoForge integration launcher is deferred.

## Dev mods and Launch Hub

`tools/devmods.json` lists pinned Maven coordinates (property substitutions refer to
`gradle.properties`), loader constraints, Minecraft version, defaults and purpose. JEI, Jade,
Mouse Tweaks, AppleSkin, Mod Menu and Cloth Config are individually selectable in integration.
Add an entry with a unique id, compatible per-loader `sources.<loader>.maven`, a version,
`minecraft: "26.3"`, name, purpose and boolean default. Add its repository to integration if needed.
Owner-installed 26.3 Fabric mods are also inventoried with per-jar metadata versions,
`runtimeDependencies` and repo-relative `sources.fabric.local` paths. They default off so the
foundation does not silently change the test environment. Local sources are searched first in
this worktree, then in the main checkout derived from Git's common directory; nothing is written
there. Missing selected jars fail preparation with the mod id. To add another, read its loader
metadata and add a relative path or filename glob under a run mods folder (no absolute paths or
`..`). Dependency/version constraints remain enforced by the loader; presets do not auto-enable
optional dependency chains. Fabric Loader/API are integration infrastructure, not switches. Existing normal-run arrangements remain intact.

The Hub **Mods** page persists selection, shows built/staged/local jar presence, and offers built-in
presets (only SimpleBuilding, all repo modules, all with dev mods) and local custom presets.
The first two presets retain required Cloth Config. Launch buttons use the selected workspace
and validated ids. Gate preparation copies the current validated selection after checkout.
Dry-run (`SB_HUB_DRY_RUN=1`) records fixed argv without launching Minecraft or changing worlds.
Normal 26.3 Hub launches also select JEI/Jade/Mouse Tweaks/AppleSkin using `-Phub_mod_selection=true`.
Fabric Hub runs also stage selected local mods under `build/hubLocalMods` and point the loader's
`fabric.modsFolder` there. Original run/mods jars stay intact, and cannot silently override a disabled
switch. NeoForge normal runs retain their previous local-jar arrangement.
Their existing compile/runtime dependencies retain Cloth Config and Mod Menu; disabling those
in a normal run is deferred. Direct Gradle normal launches keep their previous defaults.
Module toggles apply to integration; SimpleBuilding remains the normal run's main mod.

`python tools/multimod.py` checks ids, compatibility, required fields and project paths.
Root `check` includes that validator and integration compilation. Launch Hub tests exercise
rejected selections, presets, safe scaffolding and dry-run argv. Keep loader metadata, manifest
versions and registry coordinates synchronized, and add interaction tests when contracts grow.

## Shared module data contract and wiki

Every manifest entry (including SimpleBuilding) has `id`, `name`, `displayName`,
`description`, `version`, `loaders` (fabric/neoforge/forge subset), `minecraft: "26.3"`,
`requires`, `optional`, and `paths`: `root`, `shared`, `fabric`, `neoforge`, `forge`,
`generated`, `lang`, `wikiManual`, `balanceDir`. Paths are repository-relative; unavailable
loader paths are null. Keep existing fields. SimpleBuilding stays in its existing trees,
including its existing `balance/` storage; additional modules use `balance/<id>/`.
Planned ids: simplemoney, simplefun, simplequalityoflife, simpleriding, simplevisuals,
simpledimensions, simplemodels (formerly renamed), simpletweaks (only unported features).
Register a planned module when its projects exist, not as an empty Gradle project.

Keep bilingual chapters/notes in `modules/<id>/wiki/manual.json` using the same schema as
`wiki/manual.json`; language keys use the module namespace. Datagen belongs under the
manifest's `generated` directory. Keep tunable values/loot/trades/recipes in data or named
constants that balance extractors can read. No destructive migration of existing balance data.

`python wiki/generate.py --all` generates every manifest module;
`--module <id>` selects one; no selection preserves the SimpleBuilding default.
`--all --check` checks JSON, JS, metadata and bilingual completeness for all modules.
`wiki/modules.py` is the additional-module extraction interface; SimpleBuilding retains its
specialized extractor and byte-compatible payload. Resource precedence for additional modules:
generated, shared resources, then loader resources in manifest order (later recipe ids win;
tags append unless replace=true). Keep common facts shared; export explicit loader differences
as prose until separate per-loader views exist.

Small modules may contain only manual/lang files. Item definitions, blockstates, language
keys and literal Java Identifier registrations contribute inventory evidence. Dynamic registries
must export `<generated>/wiki/items.json` as `{"items":[{"id":"mod:item",...}]}`; include
`kind: "block"` for blocks. Do not rely on Java parsing for dynamic ids. Every discovered item,
block and feature needs English/German prose; exact/namespaced/glob notes work. Complex trades,
config, enchantments, advancements, quests, inWorld and obtain sections may be exported in
`<generated>/wiki/data.json` using the corresponding generated wiki UI schema. This export
must be produced from actual registries/data, never a second hand-maintained balance source.
Absent sections are empty. Module textures are isolated under `assets/textures/<id>/`.

Pure Python verification: `python -m unittest discover -s wiki/tests -v` (also in `check`).
The wiki switcher persists `?mod=<id>` and guarded localStorage. Existing SimpleBuilding
query/hash links remain valid. Cross-module chapter references use full registry ids in
`related`, which become links when the target module documents the id.
## Producer/consumer data contract

Every entry, including SimpleBuilding, supplies id, name, displayName, description,
version, loaders (fabric/neoforge/forge), minecraft (26.3), requires and optional arrays.
Preserve projects and other existing fields. paths supplies repository-relative root,
shared, fabric, neoforge, forge, generated, lang, wikiManual and balanceDir. Planned ids:
simplemoney, simplefun, simplequalityoflife, simpleriding, simplevisuals, simpledimensions,
simplemodels (formerly renamed, item/block model customization), simpletweaks (only
features not already in SimpleBuilding). Do not register absent projects as buildable mods.

Keep hand-written chapters at modules/<id>/wiki/manual.json (wiki/manual.json schema),
localization in the module namespace and datagen output under its own generated path.
Expose tuning as named Java constants or hand-written JSON. The Balancing-Zentrale
reads manifest paths with isolated services and stores; see BALANCING-ZENTRALE.md for
supported extractors and explicit gaps. Extra datagen tasks can be declared as datagenTask.
Storage is balance/<id>/; SimpleBuilding retains balance/ as the non-destructive legacy
exception until an explicit migration. Never copy its version sequence into a second
writable store. checkBalance checks all manifest modules, including empty modules.
## Forge 26.3 and producer data contract

Forge loader projects from the manifest are discovered only with `-Pforge263=true`.
The module scaffold includes `forge/` alongside Fabric/NeoForge, using ForgeGradle 7
and Java 25. See `docs/FORGE-26.3.md` for run-toolchain requirements and validation.
The existing Fabric integration harness and selections are unchanged; Forge integration
runtime is deferred. A declared loader identifies a project, not a verified release.

Every manifest entry provides id/name/displayName/description/version/loaders/minecraft,
projects, paths and requires/optional lists. Paths name root, shared, fabric, neoforge,
forge, generated, lang, wikiManual and balanceDir. Module producers keep hand-written
chapters in `modules/<id>/wiki/manual.json` (the root manual schema), module-prefixed
lang keys, generated resources under their own generated directory and tunable data or
named constants readable by balance extractors. Loader conventions include
`generated/resources`. Balance storage is `balance/<id>/`; SimpleBuilding keeps its
existing `balance/` working without moving any history. Output directories need not
exist before the first generated output. Consumer extraction/rendering is a separate
infra task; the manifest paths are its contract.
