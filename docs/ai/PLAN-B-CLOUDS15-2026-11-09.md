# Plan B-CLOUDS15 (09.11.): Wolken lichtdurchlässig + Umgebungsfarbe (N13b, CLOUDS14=ok)

Branch `b-clouds15`, Basis `hub/b-bundle165` + `hub/b-fix165`. Vorlauf: `docs/ai/PLAN-B-CLOUDS14-2026-11-09.md`.
Besitzer 09.11.: CLOUDS14=ok (= Empfehlung A) und Auftrag N13b „Wolkenblöcke zusätzlich lichtdurchlässig (wie Glas/Eis, ggf. leichte
Dämpfung), Nebel prüfen, Test“.

## Ist-Stand (MC 26.3-Quellen geprüft: BlockBehaviour, TransparentBlock, IceBlock, LightEngine, SkyLightEngine, ChunkSkyLightSources)

| Block | Klasse | Licht vorher | Vanilla-Vergleich |
|---|---|---|---|
| `cloud_block` (Hülle) | `TransparentBlock`, noOcclusion | Dämpfung 0, `propagatesSkylightDown` = Glas | Glas |
| `mist_block` (Nebel, Pfropfen der Nebelgruben) | `TransparentBlock`, noOcclusion, keine Kollision | Dämpfung 0 = Glas | Glas |
| `cloud_stone` (Kern, `default_block` der Inseln) | `Block` (CushionBlock), voll | **Dämpfung 15** (solidRender), wirft vollen Schatten | Stein |

Korrektur zu CLOUDS14: dort stand „Hülle Dämpfung 1“ – laut Quelle ist sie 0 (TransparentBlock überschreibt propagatesSkylightDown).
Nebel ist damit schon voll lichtdurchlässig (geprüft, keine Änderung; neuer Test sichert es ab).

## Umsetzung

1. **A:** `dimension_type/cloud_sea.json` → `attributes."minecraft:visual/ambient_light_color": "#3a4252"` (in `tools/gen_data.py`
   und in der JSON, Generator bleibt Quelle).
2. **Wolkenstein wie Eis:** `noOcclusion()` in den Eigenschaften (LayersRegistry). Dann ist er nicht mehr solidRender, voller Form →
   Vanilla `getLightDampening` = 1 (genau wie `Blocks.ICE`: noOcclusion, volle Form, kein propagatesSkylightDown). Himmelslicht sinkt je
   Block Wolkenstein um 1 statt auf 0. Kein eigenes Override nötig (Vanilla-Mechanik).
3. **Innenflächen nicht rendern:** noOcclusion würde zwischen benachbarten Wolkensteinen alle Flächen zeichnen (große Inseln!).
   `CushionBlock.skipRendering` → Fläche weg, wenn Nachbar auch Wolkenstein ist. `CloudBlock`/`MistBlock.skipRendering` → zusätzlich
   weg gegen Wolkenstein (vorher hat der undurchsichtige Stein diese Flächen verdeckt). Ergebnis optisch wie vorher.
4. Unverändert und geprüft: Kollision/Begehbarkeit (Form voll, `blocksMotion`), Fallschaden-Faktor, `isValidSpawn` = false auf allen
   drei Blöcken, `isRedstoneConductor`/`isSuffocating` (Vanilla-Standard aus Kollisionsform, nicht aus Occlusion), Schattierung
   `getShadeBrightness` 0.2 (volle Kollision, wie Eis → AO wie vorher).

## Entscheidungen (selbst, begründet)

- **Dämpfung 1 (Eis) statt 0 (Glas)** für den Wolkenstein: Auftrag „ggf. leichte Lichtdämpfung“, „kein voller Schatten“. Mit 0 gäbe es
  gar keinen Schatten unter Inseln (Kern unsichtbar fürs Licht), das wirkt unnatürlich. Folge: dünne Inseln/Ränder hell, sehr dicke
  Kerne (≥ ~14 Blöcke) unten weiterhin Licht 0 – dort sorgt A (Umgebungsfarbe) für bläuliches Grau statt Schwarz.
- **Keine Config.** Lichtwerte sind Block-Eigenschaften (beim Start fest, gecacht in BlockState); umschaltbar nur per Neustart und
  Registry-Trick – kein Nutzen für den Spieler (A12: Eingriff klein, nur eigene Blöcke/Dimension).
- **Mob-Spawning:** Wolkenmeer hat ohnehin `disable_mob_generation` + leere Spawn-Listen; Blöcke `isValidSpawn` false. Mehr Licht unter
  Inseln hat dort keine Spawn-Wirkung; in der Oberwelt verbautes Wolkenstein-Dach lässt jetzt Licht durch (wie Eis) – gewollt.

## Alte Welten / Lichtberechnung

- Neue Chunks: Licht wird beim Generieren mit den neuen Werten gerechnet (LightEngine liest `getLightDampening` aus dem State).
- **Schon generierte Chunks** behalten ihr gespeichertes Himmelslicht (0 unter dem Kern), bis dort ein Block geändert wird
  (LightEngine.hasDifferentLightProperties → Neuberechnung nur am geänderten Block). Vollständig neu rechnen: Vanilla-Option
  „Welt optimieren“ mit „Lichtdaten löschen“ (Erase cached data) im Welt-Bearbeiten-Menü bzw. `--forceUpgrade --eraseCache` am Server.
  Kein eigener Konverter (Wolkenmeer ist neu, kaum alte Welten).

## Tests (Gametests module-simplelayers fabric + standalone)

- `cloud_light_properties`: Dämpfung cloud_block 0 / mist 0 / cloud_stone 1, kein solidRender beim Stein, volle Kollision, isValidSpawn
  false (Zombie) auf allen drei.
- `cloud_roof_lets_light_through`: drei geschlossene Stein-Kästen (Innenraum 1×2×1), Dach Stein / Wolkenstein / Wolkenblock. Himmelslicht
  innen: Stein 0, Wolkenstein = oben − 2 (Dach −1, Luft −1), Wolkenblock = oben. Nur wenn der Testplatz Himmel hat (oben ≥ 2),
  sonst nur Eigenschaften.
- Datapack lädt (bestehendes `dimensions_load`), plus Prüfung der Umgebungsfarbe im JSON nicht nötig (Generator-Check).

## Vorschau

`/root/sb-tools-b/previews/b-clouds15-preview.html` (gerechnet, Lichtformel wie CLOUDS14, Schnitt durch eine Insel: vorher/nachher
Licht unter der Insel). Antwortcode **CLOUDS15=<ok|glas|einwand>** (glas = Dämpfung 0, kein Schatten).

## Ergebnis (sbt df7d1fcbf)

- `C_RC=0` (Compile Kern + simplelayers), `T_RC=0`: module-simplelayers fabric 22/22, standalone 22/22 (inkl. der 2 neuen Tests).
- `D_RC=1`: `simplebuilding:data_integrity*` 40/48 – alle 8 rot betreffen b-pal14-End-Blöcke (ender/astral/nihil purpur, ender_end_stone:
  fehlende Item-Definitionen/Loot/Rezepte). Ursache: Basis `b-bundle165` ohne Datagen (liegt auf `b-bundle165-gen`), kein Bezug zu
  Simple Layers (Kern unverändert). Darum kein zweiter Lauf.
- Abweichung vom Auftrag: kein Client-Bild, nur gerechnete Vorschau (B09-Regel: Client-Läufe im Bündel).
