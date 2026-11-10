# Plan B-CLOUDS17 (10.11.): Wolkenstein wie Eis, Wolkenblock wie Glas (N17a, CLOUDS15=ok)

Branch `b-clouds17`, Basis `hub/b-bundle168-gen` (enthält b-clouds15). Vorlauf: `docs/ai/PLAN-B-CLOUDS15-2026-11-09.md`.
Besitzer 10.11.: CLOUDS15=ok + „Wolkenstein wie Eis, Wolkenblock wie Glas“ (Licht + Rendertyp + Seitenflächen-Culling).
Nur Wolken-Blöcke/-Texturen/-Tests in Simple Layers; keine Backrooms-Dateien (b-back18 arbeitet parallel).

## Ist-Stand (MC 26.3 geprüft, minecraft-merged-deobf-26.3.jar)

- **Licht schon erledigt (CLOUDS15):** Wolkenblock + Nebel = `TransparentBlock` (Dämpfung 0, `propagatesSkylightDown`, kein Schatten =
  Glas), Wolkenstein `noOcclusion` + volle Form → Dämpfung 1 = Eis. Bleibt so, Tests sichern es ab.
- **Rendertyp in 26.3:** `ItemBlockRenderTypes`/`BlockRenderLayerMap` gibt es nicht mehr. Die Schicht (`ChunkSectionLayer.byTransparency`)
  folgt aus der Textur: `SpriteContents.transparency()` → nur Alpha 255 = SOLID, Alpha 0 dabei = CUTOUT, Alpha 1..254 = TRANSLUCENT.
  Vanilla: `ice.png` überall Alpha 190 (translucent), `glass.png` Alpha 0/255 (cutout), `white_stained_glass` 102/163 (translucent).
- **Texturen jetzt:** `cloud_stone.png` und `cloud_block.png` voll deckend (Alpha 255) → beide SOLID. `mist_block.png` Alpha 150–225 →
  schon translucent (Nebel unverändert lassen, passt).
- **Culling jetzt:** Wolkenstein überspringt Flächen zu Wolkenstein; Wolkenblock/Nebel überspringen zu Wolke/Nebel/Wolkenstein.

## Umsetzung

1. **Texturen nur im Alpha-Kanal** (Farben sind abgenommen: CLOUDS15/TEX, „abgenommene Texturen nicht anfassen“):
   - `cloud_stone`: Alpha wie Eis (Grundton 200, dunkle Körnung 220, Lichter 190) → TRANSLUCENT, Struktur bleibt lesbar.
   - `cloud_block` (Standard = Variante A „Glas“): Alpha 0/255 → CUTOUT. Kleine Lücken (2–3-px-Haufen, ~12 %) zwischen den
     Wolkenballen, dort wo die Textur ihre dunkelsten Mulden hat; Rest deckend. Variante B „Milchglas“ (translucent, Alpha 170–230)
     nur in der Vorschau.
   - `.layers.json` neu aus den PNGs (`layers.py import --force`, eine Ebene „Grundform“ + Alpha), `layers.py check`.
2. **Culling (Performance, siehe unten):**
   - `CushionBlock.skipRendering`: zusätzlich gegen `CloudBlock` → der Kern zeichnet hinter der Wolkenhülle keine (teuren, sortierten)
     translucent-Flächen. Sichtbar wird der eisartige Kern nur, wo er frei liegt (abgebaut, verbaut, an Luft/Nebel).
   - `CloudBlock.skipRendering`: nicht mehr gegen `CushionBlock` (wie Glas: Glas zeichnet seine Fläche zu allem außer Glas). Durch
     die Glas-Lücken sieht man so die gegenüberliegende Hülle statt durch die ganze Insel in den Himmel.
   - `MistBlock` unverändert.
3. **Tests** (module-simplelayers, Katalog `LayersTests.ALL` + Fabric-Adapter):
   - `cloud_light_properties` erweitert: `propagatesSkylightDown` Wolkenblock ja / Wolkenstein nein (wie Glas/Eis), `canOcclude` beide nein.
   - neu `cloud_face_culling`: `BlockState.skipRendering` Stein–Stein ja, Stein–Wolke ja, Wolke–Wolke ja, Wolke–Stein nein, Stein–Luft nein.
   - neu `cloud_texture_layers`: PNG aus dem Klassenpfad lesen → Wolkenstein nur Alpha 1..254 (translucent wie Eis), Wolkenblock nur
     0/255 mit mind. einem 0 (cutout wie Glas). Sichert die Render-Schicht ohne Client.
   - bestehend: `cloud_roof_lets_light_through` (Licht unter Decke), `mist_has_no_collision`, `fall_damage_factors`, Spawn-Regel.

## Entscheidungen (selbst, begründet)

- **Kein Code für die Render-Schicht:** 26.3 hat keine Zuordnungstabelle mehr, die Textur entscheidet (Vanilla-Mechanik). Eine
  Fabric-API-Zuordnung wäre ohne Wirkung bzw. gegen Vanilla.
- **Wolkenstein translucent, nicht cutout:** Eis-Optik (milchig durchscheinend) geht nur mit Teil-Alpha; cutout wäre löchrig (Glas).
  **Performance:** translucent-Flächen werden je Abschnitt sortiert, wenn sich die Kamera bewegt. Durch Punkt 2 hat eine intakte Insel
  praktisch keine translucent-Fläche (Kern innen gegen Kern und gegen Hülle übersprungen); es bleiben nur frei liegende Kernflächen –
  Größenordnung wie Eisflächen/Wasser in Vanilla. Die Hülle (größte Fläche) ist cutout = so billig wie solid.
- **Wolkenblock cutout (Glas) als Standard, Milchglas als Alternative:** wörtlich „wie Glas“, billigster Weg, Hülle bleibt weiß und
  geschlossen wirkend. Lücken klein, sonst wirkt die Insel wie ein Gitter. Besitzer wählt in CLOUDS17.
- **Kollision, Fallschaden, Spawn-Regel, Lichtwerte, Nebel:** unverändert. `isValidSpawn` ist auf allen drei Blöcken fest `false`
  (Vanilla-Glas/-Eis: Glas verbietet Spawns ebenfalls, Eis erlaubt nur Eisbären – hier keine Spawns, Wolkenmeer hat ohnehin
  `disable_mob_generation`).
- **Keine Config** (Block-Eigenschaft/Textur, A12: kleiner Eingriff nur an eigenen Blöcken). Ressourcenpaket kann Alpha ändern.

## Verifikation

- Ein sbt: Compile Kern + simplelayers, `module-simplelayers-fabric-263` + `module-simplelayers-standalone-fabric-263`,
  `--targets fabric-263 --filter 'simplebuilding:data_integrity*'`; `layers.py check` lokal.
- Vorschau `/root/sb-tools-b/previews/b-clouds17-preview.html` (gerechnet: Texturen 1x/8x über Himmel/Stein, Insel-Schnitt vorher/nachher
  neben Vanilla Eis/Glas), Antwortcode **CLOUDS17=<A|B|einwand>**. Kein Client-Bild (B09: Client-Läufe im Bündel).
