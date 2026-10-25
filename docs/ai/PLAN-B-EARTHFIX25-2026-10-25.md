# Plan B-EARTHFIX25 (25.10.2026) – Simple Earth: Menü bedienbar (WD25-23), Vorwärtsflug + schnelleres Rendering (WD25-22)

Branch `b-earthfix25` (Basis claude-wave1 17d79f991), nur 26.3 Fabric. Besitzer: „im Orbit kann man sich nicht vor bewegen“,
„Rendering bei V10 usw. langsam … wie Chunkbase schnell“, „Einstellungen lassen sich nicht bedienen“.

## Ist-Zustand / Befund
- Höhe je Probe: `NoiseBasedChunkGenerator.getBaseHeight` baut je Spalte einen NoiseChunk und rechnet die ganze Rauschsäule
  (finalDensity über die volle Höhe) – teuer, 4096× je Kachel. Chunkbase nimmt nur Klima-Biome + grobe Oberfläche.
- V4/V5/V10–V12: Seitenflächen mischen zwei Kacheln, jede 1/16-Stufe war ein eigenes, neu zu malendes Bild (Malstau beim Gleiten).
- Menü und Vorwärtsgleiten: im Code keine offensichtliche Sperre → erst mit echtem Client-Test (echte Maus-/Tastenereignisse) messen.

## Umsetzung
1. Server `fastHeights` (Standard an): Bodenhöhe aus `noise_router.chunk_surface_level` (vorläufige Oberfläche, eine Dichtefunktion
   je Spalte, Sampler je RandomState gecacht, Rückfall auf exakt bei Fehlern/anderen Generatoren). Cache-Signatur trennt beide.
2. Mischgewicht in 1/8-Stufen → wenige gecachte Bilder je Seitenfläche.
3. Messpunkte: `TerrainSampler.NANOS/TILES` (ms je Kachel), `FaceTextures.lastFrame()` (Bilder unfertig).
4. Gametest `sampling_speed` (Overworld-Generator mit festem Seed: µs je Spalte exakt/schnell, Höhenabweichung, ms je Kachel).
5. Client-Test: Zeit bis V10-Bild fertig (kalt, exakt vs. schnell), Vorwärtsgleiten (Strecke), Menü mit echten Mausereignissen
   (Versionsknopf, Klick auf Regler, Ziehen, Umschalter, Mausrad). Befunde → Fix → erneuter Lauf.

## Verifikation
module-simpleearth-fabric-263 + standalone, ein Client-Lauf zur Zeit (`module-simpleearth-client-263`), check_data, `./gradlew check -q`.
Vorschau `/root/sb-tools-b/previews/earthfix25-preview.html`.
