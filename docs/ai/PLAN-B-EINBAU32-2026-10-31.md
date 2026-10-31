# Plan b-einbau32 (31.10.2026): Einbau der Besitzer-Wahlen vom 31.10.

Branch `b-einbau32`, Basis hub/b-bundle83 = 328577124. Nur MC 26.3 Fabric. Quellen: PLAN-B-OPTIK32, PLAN-B-MOBS32, PLAN-B-ORES32,
Generatoren `tools/textures/optik32_*.py`, `modules/simpleanimals/tools/optik32_models.py`, `tools/textures/ores_2026_10_30.py`.
Antwort des Besitzers: Krebs A+B, Wildschwein A, Kessel A, Sattel C, Rüstung C, Frosch C; Pinsel/Warze alt; Spiegel/Bleierz/Pfeil/Trüffel A.

## A12 Vanilla-Nutzen und Eingriff
| Punkt | Nutzen | Eingriff |
|---|---|---|
| Krebs-Modelle A/B | nur Optik, Vielfalt wie Vanilla-Tiervarianten | keiner (Hitbox, Drops, Verhalten gleich) |
| Wildschwein A | Optik („tiefergelegtes Schwein“) | schwach: Hitbox 0,9 hoch -> 0,875, Augenhöhe 0,7 -> 0,6 (passt zum tieferen Kopf) |
| Kessel-Gerüst | Optik, Vanilla-Küche (Kessel über Lagerfeuer) | keiner: kein Block, keine Kollision, Client-Schalter |
| Item-Texturen, Frosch-Eimer-Modell/Tooltip | zeigt den gefangenen Frosch | keiner |

## 1. Krebs: Modell A (Detail) und B (Winkerkrabbe) als Varianten
- Generator `modules/simpleanimals/tools/gen_models.py`: `crab_spec` ersetzt durch `crab_a` (rot/korallrot, wie bisher je Biom) und
  neu `crab_fiddler` = `crab_b` aus `optik32_models.py` (eigene Farbe Orange wie in der Vorschau). Ausgabe `CrabLayers.java` (A),
  `CrabFiddlerLayers.java` (B), Texturen `crab/red.png`, `crab/coral.png` (neues UV-Layout A) und `crab/fiddler.png`.
  Java-Klassenname aus dem Modellschlüssel (`crab_fiddler` -> `CrabFiddler`). `optik32_models.py` importiert die Spezifikation aus gen_models (keine Doppelung).
- `Crab`: zweiter synchronisierter Wert `MODEL` (0 = Krebs, 1 = Winkerkrabbe), NBT `Model`, beim Spawn gewürfelt (`finalizeSpawn`, auch Spawn-Ei).
  **Verhältnis 1:3** (25 % Winkerkrabbe): seltener, aber oft genug, dass man am Strand bei den ohnehin seltenen Einzelkrebsen (Cluster 1) ab und zu eine sieht;
  1:9 wäre bei der niedrigen Krebs-Dichte praktisch nie zu sehen. Die Farbvariante (`Variant`) bleibt unabhängig (gilt sichtbar nur für Modell A; die
  Winkerkrabbe hat ihre eine Farbe). Krebse haben weder Eimer noch Zucht (A12, Plan WP-18): nichts zu übertragen.
- Client: `CrabModel` liest Beine als `<seite>_thigh_<i>` (beide Modelle), `CrabRenderer` hält zwei Modelle und wählt in `submit` (wie Vanilla `CowRenderer`),
  Textur der Winkerkrabbe `crab/fiddler.png`. `AnimalsClient` registriert die zweite Ebene `simpleanimals:crab_fiddler`.
- Test (Katalog `AnimalsTests.ALL` + Fabric-Adapter): `crab_models` – beide Modelle per `finalizeSpawn`/Würfel, Verteilung 800 Würfe 12–38 % Winkerkrabbe,
  Wert bleibt nach Speichern/Laden (TagValueOutput/Input), Klemmen.

## 2. Wildschwein Form A „kompakt-tief“
- `boar_spec`: Beine 7 -> 5 px, Rumpf/Beine-Pivot 17 -> 19 (2 px tiefer), Kopf 12 -> 15 (eine Stufe = 3 px tiefer, wie Vorschau). UV-Layout neu (Beinkasten kleiner) -> alle Wildschwein-Texturen neu erzeugt.
- Hitbox `sized(0.9F, 0.875F).eyeHeight(0.6F)` (Rücken 14 px). Test: bestehende Boar-Tests (Ramm, Trüffel) laufen weiter; `registered` unverändert.

## 3. Kessel-Gerüst A (IG31-21)
- `simplevisuals`: Schalter `visuals.cauldronStand` (Standard an, Reiter general, Lang EN/DE mit „Default: true.“/„Standard: true.“, Wiki-Eintrag `config_visuals_cauldronStand`).
- Shared `com.simplevisuals.CauldronStand`: Regel `applies(cauldron, below)` (Kessel = `AbstractCauldronBlock`, darunter `CampfireBlock`, Seelenfeuer inkl., an/aus egal)
  und die Kästen (Pixel): vier Eckpfosten 2x2 (x/z 1–3 und 13–15, y -9..0 = Lagerfeuer-Oberkante bis Kesselunterkante), Rahmen oben 2x2 rundum (y -2..0).
- Fabric-Client `CauldronStandModel` (FRAPI `WrapperBlockStateModel`, eingehängt über `ModelLoadingPlugin.modifyBlockModelAfterBake` für alle Kessel-Zustände):
  `emitQuads` gibt erst den Kessel aus, dann – Schalter an und Lagerfeuer darunter – die Gerüst-Quads mit dem Partikel-Sprite des Kessels (= Kessel-Seitentextur,
  „Farbe aus der Kesseltextur“). `createGeometryKey` = null, wenn das Gerüst steht (kein Mesh-Cache mit falschem Nachbarn). Kein Mixin.
- Treppe: nichts (Besitzer).
- Test: Gametest `cauldronStandRule` (Katalog VisualsTests + Adapter): Regel für Kessel/Wasserkessel/Lavakessel über Lagerfeuer, Seelenlagerfeuer, nicht über Stein/
  Magma, Schalter aus = nie; Kästen innerhalb 0..16 und unter dem Kessel. Render selbst: Client-Sicht offen (kein Client-Lauf erlaubt).

## 4. Item-Texturen (IG31-23)
- `modules/simpledinos/tools/gen_blocks.py`: großer Sattel = `optik32_items.saddle('C')`, T-Rex-Rüstung = `armor('C', tier)` für leather/iron/diamond (Ebenen-Quelle per `L.snap`).
- Frosch-Eimer (`simplemobmanners`): neuer Generator-Abschnitt in `tools/textures/frog_bucket_2026_10_15.py` schreibt `frog_bucket_temperate/warm/cold.png`
  aus `optik32_items.frog('C', kind)`; `frog_bucket.png` = gemäßigt (Fallback, Inventarbild ohne Komponente). Modelle `item/frog_bucket_<kind>.json`,
  `items/frog_bucket.json` = `minecraft:select` auf `minecraft:component` `minecraft:frog/variant` (temperate/warm/cold, Fallback gemäßigt).
- Tooltip `FrogBucketItem.appendHoverText`: Zeile „Frosch: <Name>“ bei Namen, Zeile „Art: Gemäßigt/Warm/Kalt“ bei Variante (grau). Lang EN/DE.
- Test: `frogBucket` erweitert (Tooltip enthält Name und Variante). Pinsel/Warze: unverändert.

## 5. Prüfen, nichts ändern
`ores_2026_10_30.py`: `LEAD_VARIANT='a'`, `ARROW_VARIANT='a'`, `MIRROR_VARIANT='a'` (geprüft); Trüffel: `gen_items.py` „variant A“ (geprüft). IG31V: nichts.
Nicht angefasst: Rohblei/Bleiblock, Zebra/Zorse, Signaldämpfer, Doppelkolben (b-abn32).

## Verifikation
Lokal ohne Gradle: `gen_models.py --check`, `gen_blocks.py --check`, Frosch-Generator `--check`, JSON-Syntax, `tools/check_item_properties.py`.
Ein sbt-Lauf: Compile 26.3 Fabric + simpleanimals/simpledinos/simplevisuals/simplemobmanners; Testrunner `module-simpleanimals/simpledinos/simplevisuals/simplemobmanners-standalone-fabric-263`; `check_client_mixins.py`.
Abweichung vom Auftrag: simplemobmanners in Compile und Tests ergänzt (Frosch-Eimer-Code liegt dort).
Nicht testbar ohne Client: Aussehen Krebs/Wildschwein/Gerüst/Items; Gerüst-Render (FRAPI) nur kompiliert.
