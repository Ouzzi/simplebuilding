# Launch Hub project-mod loading — 2026-10-01

Verified in `C:/Users/o_o/AppData/Local/Temp/cx-next-merge-review`, based on
`b358c324`. No owner clients, worlds, or main-checkout builds were touched.

## Fix

`hub-project-mods.gradle` previously resolved producer tasks during
`projectsEvaluated`, before producers were configured with configuration on demand.
This failed with `Task ... jarJar not found`. NeoForge's ModDevGradle `jarJar`
task also has no `archiveFile`: it produces resources embedded by the normal
`jar` task. The correct distributable artifact is `jar` on all three loaders.

Producer archive lookup is now lazy, with explicit producer task dependencies.
NeoForge's launch source set is configured during project evaluation, before run
configuration is finalized. Ordinary runs without the Hub property stay unchanged.

## Evidence

- Configuration-only `runServer --dry-run` passed for Fabric, NeoForge, and Forge
  with only SimpleBuilding + Wiring Example selected: exactly one module jar
  producer, with the other nine excluded. NeoForge also passed with
  `-Pdev_mods=false`; without `-Phub_mod_selection=true`, no module jar producer
  was added to its ordinary server task graph.
- With all 11 project mods selected, Fabric's real `runServer --initSettings`
  listed every module and completed common initialization successfully. Its
  managed `build/hubLocalMods` folder contained exactly the ten module jars.
- NeoForge and Forge each ran a real dedicated server with all 11 selected mods.
  Discovery logs confirmed all ten module IDs plus SimpleBuilding, bootstrap ran,
  the server reached `Done`, and an authenticated local RCON `stop` shut it down.
  Both Gradle runs completed successfully.
- Deselection was then tested with only SimpleBuilding selected on all three
  loaders. Each real server reached `Done`, stopped cleanly, and completed its
  Gradle run successfully. None of the ten deselected module IDs appeared in its
  discovery list; Fabric's managed staging folder was empty.
- Selection bytes were restored afterwards. Gradle and game heaps were bounded
  at 2 GiB with two Gradle workers. Tests were serial and used isolated worktree
  worlds, unique localhost server/RCON ports, and temporary credentials.

Logs retained in the worktree's ignored `.ai-runs/` directory:
`hub-live-fabric.log`, `hub-server-all-{neoforge,forge}.log`,
`hub-server-base-{fabric,neoforge,forge}.log`, and copied Forge debug logs
`hub-server-{all,base}-forge-debug.log` containing the discovered mod IDs.

## Reproduction and limits

Configuration regression check (PowerShell; Java 25 configured):

```powershell
./gradlew.bat :mc26_3:neoforge:runServer --dry-run -Phub_mod_selection=true '-Dorg.gradle.jvmargs=-Xmx2G' '-Dorg.gradle.workers.max=2' '-Dorg.gradle.configureondemand=true'
```

Forge additionally requires `-Pforge263=true -Pforge_runs=true` and the actual
Java 8 path via `-Porg.gradle.java.installations.paths=...` for Slime Launcher.
Client rendering and existing-world/modpack acceptance remain untested here.

The initial NeoForge `--initSettings` probe discovered all modules but exited
nonzero: NeoForge's wrapper expects a server thread, while vanilla intentionally
returns after creating configuration files. That probe was inconclusive and was
replaced by the successful real-server startup/shutdown checks above. Existing
Windows performance-counter and Forge mixin metadata warnings were nonfatal.
