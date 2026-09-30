---
name: mc-26-api-nachschlagen
description: "Lokale Jars/Werkzeuge, um MC-26.1.2- und Fabric/NeoForge-API-Signaturen exakt zu verifizieren"
metadata: 
  node_type: memory
  type: reference
  originSessionId: 0010ebfd-5be9-4072-a491-8250db878921
  modified: 2026-08-20T11:29:36.379Z
---

MC 26.1.2 nutzt offizielle Mojang-Mappings (kein Intermediary mehr) — Fabric-API-Jars sind direkt Mojang-gemappt.

- Mojang-gemapptes MC-Jar: `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/minecraft-merged-deobf-26.1.2.jar`
- Fabric-API-Module direkt aus `~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/...` (z.B. `fabric-rendering-v1/23.1.1+...`), NeoForge-Universal-Jar aus `net.neoforged/neoforge/26.1.2.75/`.
- NeoForge-gepatchte MC-**Quellen**: `~/.gradle/caches/neoformruntime/intermediate_results/applyNeoforgePatches_*_output.zip` (zeigt, wo NeoForge-Events gefeuert werden).
- Fertig dekompilierte, **benannte 26.2-Quellen** (schneller als Vineflower selbst laufen zu lassen): `~/.gradle/caches/minecraftforge/forgegradle/mavenizer/caches/forge/.global/mcp/26.2-*/forge/*/decompile/decompile.jar` — rund 7000 `.java`, mit `unzip -p <jar> net/minecraft/.../X.java` direkt lesbar. Fuer 1.21.11 gibt es das nicht; dort `javap -c` auf `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.11-loom.mappings.*/...jar`.
- Signaturen: `javap -classpath <jar> -p <FQCN>` (JDK: `C:\Program Files\Zulu\zulu-25\bin`); Implementierungen: Vineflower `~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/1.11.2/...jar`.

Wichtige 26.1-Render-Umbenennungen: Submit-Pipeline (`SubmitNodeCollector.submitBreakingBlockModel/submitCustomGeometry`), `LevelRenderState.blockBreakingRenderStates` (Record `BlockBreakingRenderState(pos, state, progress 0-9)`), `RenderTypes` statt `RenderType`-Statics, Modelle via `Minecraft.getModelManager().getBlockStateModelSet().get(state)`, Quads via `VertexConsumer.putBakedQuad(pose, quad, quadInstance)`, Licht via `LevelRenderer.getLightCoords(level, pos)`, Tint via `BlockColors.getTintSource(state, i).colorInWorld(...)` (nullable). Lokaler Abbau-Fortschritt: `Minecraft.gameMode.isDestroying()` + `getDestroyStage()` (0-9, sonst -1).
