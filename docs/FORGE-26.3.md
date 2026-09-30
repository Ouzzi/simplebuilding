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
The existing Forge AutoConfig shim provides validated defaults without file persistence
or a Cloth Config GUI. This port does not claim support for Forge builds of optional
JEI/Jade/Curios/Cloth integrations. These dependencies are not added speculatively.
