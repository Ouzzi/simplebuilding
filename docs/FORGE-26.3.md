# Forge 26.3

## Official release evidence (2026-09-30)

Forge for Minecraft 26.3 exists. The official [files page](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html)
lists latest **66.0.8**, released 2026-09-28, with an installer and MDK.
The [Maven metadata](https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml)
was downloaded and contains 26.3-66.0.0, .2, .3, .4, .5, .6 and .8.
The [66.0.8 MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.8/forge-26.3-66.0.8-mdk.zip)
was downloaded; its SHA1 is `a7809855a8e4d014f4e386bd7fddc0283bd56767`, matching the files page.
Its build.gradle uses `net.minecraftforge:forge:26.3-66.0.8`, Java 25,
ForgeGradle `[7.0.17,8)`, and eventbus-validator 7.0.6. Its wrapper uses Gradle 9.7.1.
ModDevGradle is not used or required by this MDK. This repository pins ForgeGradle 7.0.36
and already uses Gradle 9.7.1. ForgeGradle 7.0.36 additionally needs a Java 8 installation
when creating Slime Launcher run tasks (compilation uses Java 25).

## Build and run

Forge 26.3 is separate from the existing 26.2 Forge project. It is off by default:

```powershell
./gradlew.bat -Pforge263=true :mc26_3:forge:build
./gradlew.bat -Pforge263=true check -q
python tools/testrunner/run.py --targets forge-263
python tools/testrunner/run.py --targets forge-263 --filter "simplebuilding:*test_centre*"
```

The runner supplies `-Pforge_runs=true` and the Java 8 toolchain path. Override the path
with `SIMPLEBUILDING_JAVA8_HOME`. Direct launches use:

```powershell
./gradlew.bat -Pforge263=true -Pforge_runs=true "-Porg.gradle.java.installations.paths=C:/Program Files/Java/jre1.8.0_431" :mc26_3:forge:runServer
./gradlew.bat -Pforge263=true -Pforge_runs=true "-Porg.gradle.java.installations.paths=C:/Program Files/Java/jre1.8.0_431" :mc26_3:forge:runClient
```

Launch Hub exposes server, client, client with a fresh test-centre world, and server tests
under `forge-263`. Its own directories are `mc26_3/forge/run` and `run-gametest`;
existing saves and instances are not reused. Automated client tests are not implemented.

## Overlay and resources

The project compiles `common/src/shared/java`, `mc26_3/overlay/java` and the existing
Forge adapter sources. Files in `mc26_3/forge/src/main/java` replace Forge adapters by
relative path; existing 26.2 files are not moved or changed. New loader hooks live
alongside those overrides. Keys use the KeyMapping constructor without KEYSYM; GameTest
metadata uses `McVersion.testData` (including the 26.3 dimension). `Forge263Events`
places search-tab items and cancels Breeze-head farmland trampling. The startup overlay
registers tiered shulker cauldron/dispenser behavior and the client HUD-toggle key.
Loot injection runs in `createAndValidateFullContext` after loot holders bind, before
validation: Forge's per-table load event is too early to obtain the loot registry.
Each reload uses its own lookup, with no static global loading context. Resources use the same
`mc26_3/resources.gradle` merge and resource verification as Fabric and NeoForge.
`:mc26_3:forge:runDatagen` delegates to canonical Fabric 26.3 datagen (verified, exit 0), including
`syncGenerated263`. It does not introduce a second generated-resource authority.
No 26.2 dev-mod jars are staged in this 26.3 instance. The canonical sync compares
normalized text without surrounding whitespace; a missing final newline does not create
a redundant overlay or change a wiki recipe source path.

## Multimod contract

All manifest entries retain existing fields and add displayName, description, loaders,
paths (root/shared/fabric/neoforge/forge/generated/lang/wikiManual/balanceDir), requires
and optional. SimpleBuilding also supplies langOverlay and generatedBase for its
existing layered resources; module lang/generated paths contain their complete resources.
SimpleBuilding retains `balance/`; module storage is `balance/<id>/`.
Generated-resource and balance directories may be absent until the module produces data.
Paths are repository-relative and validated against traversal. Module manuals use the
same `features` schema as `wiki/manual.json`. The scaffold supplies separate en_us/de_de
files with the module prefix, a manual and a Forge loader project. Only
`-Pforge263=true` includes module Forge projects. Framework is a Java build dependency,
not a required runtime mod id. The Fabric integration instance is unchanged; a Forge
integration runtime is deferred. There are no placeholder registrations for the planned
module ids; their producers create those entries when implementations exist.

## Enabling by default later

After owner acceptance, set `forge263=true` in gradle.properties. Root check then compiles
and checks Forge 26.3 and module Forge projects. Separately move `FORGE263_TARGETS` into
`TARGETS` in tools/testrunner/run.py when Forge should join default/release server sweeps;
remove its extra record-loop concatenation at the same time to avoid duplicate rows.
Keep `forge_runs` separately opt in because creating run tasks needs Java 8.

## Verification and remaining limits

Forge 26.3 server catalogue: **778/778 passed, alles gruen**, run
`2026-09-30T13-04-03Z-0963`. This covers the same 777 shared tests as each Fabric/NeoForge
26.3 target, plus the existing Forge-only handled-payload test. **No Forge-specific skips
or known-failure exceptions.** Test-centre construction and every SimpleBuilding item/block
coverage passed. The targeted shulker rerun passed 8/8 (`2026-09-30T13-02-07Z-6500`).
Launch Hub/registry/scaffold tests: 37 passed, including client/server/fresh-world dry-run
argv and Forge target isolation. Testing-page JavaScript syntax passed. Wiki generated and
`--check` current. Both full gates passed (exit 0): `gradlew.bat -Pforge263=true check -q` and default
`gradlew.bat check -q`. Fabric/NeoForge centre checks passed 10/10; their full server suites
were not repeated. Both loaders' centre worlds were rebuilt with full item/block coverage.
Client display and a separate normal dedicated-server launch were not verified. See HANDOFF.
These are historical results, not evidence for later changes. See
`FORGE-FOLLOWUP-PLAN.md` for the merged Claims/Dimensions/configuration follow-up
and `FORGE-FOLLOWUP-RESULTS.md` for its current run IDs, counts and limits.
This port does not claim support for Forge builds of optional JEI/Jade/Curios/Cloth
integrations. These dependencies are not added speculatively.

## Native settings and packaged runtime (2026-10-01 follow-up)

The pinned Forge remains 66.0.8 (66.0.9 is now listed on the official files page).
Cloth's [official Forge Maven metadata](https://maven.shedaniel.me/me/shedaniel/cloth/cloth-config-forge/maven-metadata.xml)
ends at 17.0.144, with no 26.x artifact. Cloth 26.3.159 exists for Fabric and
NeoForge; the actual NeoForge jar requires `neoforge [26.3.0.3-beta,)` and cannot
serve as a Forge dependency. Therefore Forge uses native Minecraft widgets.

`gradle/forge-native-config.gradle` adapts the existing module screen declarations
at build time, replacing their builder import with a module-local native widget
implementation. Categories, localized labels/tooltips, defaults and bounds remain
defined by the canonical screens. Each module registers its own Forge Mods-menu
screen; SimpleModels keeps its existing native browser. No fake Cloth GUI classes
or cross-module implementation imports are packaged. Tweaks' separate Claims JSON
does not use the AutoConfig shim and is unchanged by this screen convention.

SimpleBuilding's 26.3 AutoConfig compatibility entrypoint now reads validated JSON
and writes atomically. Malformed originals remain untouched when loading falls
back to defaults; missing groups recover and server bounds apply. Its native screen
uses a local draft: Cancel discards edits, server options require restart, remote
connections disable gameplay edits, and only cosmetic options update the live
config. Dimensions retains its canonical server-thread update for a local
integrated server and its read-only remote-server notice. Module screens retain
their existing persistence/server-authority behavior. GUI rendering and interaction
still require the orchestrator's serial client verification.

`gradle/forge-framework.gradle` embeds the framework jar with version-selected
Forge Jar-in-Jar metadata. The API is a library, not a required mod id. The 26.3
SimpleBuilding Forge artifact also includes the existing protection service bridge.
Its four common bootstrap classes are included directly in the base jar.
`python tools/forge/inspect_jars.py` inspects actual distributables and nested APIs.

For server verification, set `ORG_GRADLE_PROJECT_forgePackaged=true`. The generic
run convention substitutes the built mod jar for project output and removes the
loose framework/common dependencies from both ForgeGradle's runtime token and
JavaExec classpath. Module test dependencies come from the manifest's
`tests.loaders.forge.testMods`; `-PforgeTestMods=id,id` can override them for
provider presence/absence probes. Dependencies activate only for the explicitly
requested module test task. `SIMPLEBUILDING_JAVA8_HOME` overrides legacy absolute
manifest toolchain arguments; explicit manifest paths remain valid without it.

## Serial client verification for the orchestrator

No client was launched by the follow-up worker. In a fresh verification worktree,
after the owner clients have closed, build and stage the module jars into that
worktree's base Forge instance. Do not reuse the owner's saves or mods directory.

```powershell
$env:SIMPLEBUILDING_JAVA8_HOME = 'C:/Users/o_o/AppData/Local/Temp/cx-next-small/scratchpad/next-small/java8/jdk8u504-b01'
$forgeArgs = @('-Pforge263=true', '-Pforge_runs=true', '-PforgePackaged=true',
    '-Dorg.gradle.workers.max=1', "-Porg.gradle.java.installations.paths=$env:SIMPLEBUILDING_JAVA8_HOME")
$modules = (Get-Content modules/modules.json -Raw | ConvertFrom-Json).modules |
    Where-Object { $_.id -notin @('simplebuilding', 'wiringexample') -and $_.projects.forge }
./gradlew.bat @forgeArgs :mc26_3:forge:jar @($modules | ForEach-Object { $_.projects.forge + ':jar' })
$modsDir = Join-Path (Get-Location) 'mc26_3/forge/run/mods'
New-Item -ItemType Directory -Path $modsDir -Force
foreach ($module in $modules) {
    Copy-Item "modules/$($module.id)/forge/build/libs/*.jar" $modsDir
}
./gradlew.bat @forgeArgs :mc26_3:forge:runClient
```

Check the Forge Mods-menu screens in EN and DE: tabs, names/tooltips, defaults and
ranges, text entry/focus, paging, invalid values, reset, Cancel, save and restart.
Verify local Dimensions toggles apply on the server thread while safe return stays
available; remote server settings cannot be changed from these client screens.
Check portal tint synchronization, Claims interactions, QoL packets, and Visuals/
Sounds together and separately in fresh instances. The server GameTests and jar
inspection do not prove rendering, mouse/keyboard interaction, real network-client
delivery, audio, or a production installer launch. Keep Forge experimental/opt-in
until those checks pass. The orchestrator also owns the full merged normal-loader
and `integration-263` gate.
