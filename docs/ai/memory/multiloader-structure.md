---
name: multiloader-structure
description: Wie das simplebuilding-Repo Fabric+NeoForge teilt und was Client-Klassen deshalb beachten müssen
metadata: 
  node_type: memory
  type: project
  originSessionId: 0010ebfd-5be9-4072-a491-8250db878921
  modified: 2026-08-20T11:29:26.661Z
---

Branch `feature/multiloader-fabric-forge-neoforge` (MC 26.1.2, Mojang-Mappings, Java 25):

- Root-Projekt = Fabric (Fabric Loom). `:neoforge` = NeoForge (ModDevGradle).
- **`neoforge/build.gradle` kompiliert das Root-`src/main/java` mit** (srcDir auf Root), abzüglich einer expliziten Exclude-Liste (u.a. `SimplebuildingClient.java`, `Simplebuilding.java`, `ModRegistries` etc.).
- **Konsequenz:** Neue Klassen unter Root-`src/` müssen loader-neutral sein (nur Vanilla-/Mod-APIs, keine `net.fabricmc.*`-Imports), sonst bricht der NeoForge-Build — oder die Datei muss in die Exclude-Liste.
- Bewährtes Muster: loader-neutrale Render-/Logik-Klasse in `com.simplebuilding.client.render.*`, Event-Registrierung getrennt in `SimplebuildingClient` (Fabric, `LevelRenderEvents.*`) und `SimplebuildingNeoForgeClient`/`NeoForgeClientHooks` (NeoForge, `NeoForge.EVENT_BUS`).
- Äquivalente Hooks (26.1.2): Fabric `END_EXTRACTION` ↔ NeoForge `ExtractLevelRenderStateEvent` (beide nach Vanilla-Extraktion); Fabric `AFTER_TRANSLUCENT_TERRAIN` ↔ NeoForge `RenderLevelStageEvent.AfterTranslucentBlocks`.
- Claude-Worktrees unter `.claude/worktrees/` starten teils auf dem "Initial commit" — vor Arbeit `git reset --hard feature/multiloader-fabric-forge-neoforge` prüfen/ausführen.

Siehe auch [[mc-26-api-nachschlagen]].
