# Plan Textur-Runde 5: Trainingspuppe + Weisheitskugel (2026-10-04)

Besitzer-Diktat: „Die Textur für den Target Dummy auch anpassen.“ und „Sage Orb Textur zu groß.“
Nur Vorschläge (A–C), **nichts einbauen** – der Besitzer wählt.

## Ist-Zustand
### Trainingspuppe (Runde 3, Variante A eingebaut)
- `textures/entity/training_dummy/stuffing.png` (128x64, Kürbiskopf + Strohrumpf mit Zielscheibe) – Generator
  `tools/textures/training_dummy_v3_2026_10_04.py`. Passt (vanilla carved_pumpkin / hay_block / target 1:1).
- `textures/entity/training_dummy/training_dummy.png` (Ständer-Stangen, Stroh mit Bändern, Steinplatte) – passt zum Rumpf.
- Partikel: Code nutzt `BlockParticleOption(HAY_BLOCK)` (Treffer/Abbau) – keine eigene Textur; Kürbis fehlt in den
  Partikeln (nur Code-Thema, hier nicht geändert, im Bericht genannt).
- **Item `textures/item/training_dummy.png`** (= Wiki `wiki/assets/textures/item/training_dummy.png`, identisch) passt
  nicht: flaches 6x6-Orange ohne Schattierung/Rippen, brauner Rand oben wirkt wie eine Mütze, Gesicht ist ein
  Smiley (zwei Punkt-Augen, gerader Mund) statt des geschnitzten Dreieck-Augen/Zacken-Grinsens, kein Stiel; Kopf
  sitzt direkt auf der Schulterstange; Rumpf ist nur die dünne Ständerstange, die Zielscheibe ein 2x2-Schachbrett;
  die roten Strohbänder des Modells fehlen.

### Weisheitskugel (`textures/item/sage_orb.png`, 12 Frames animiert, frametime 2)
- Vanilla-Erfahrungskugel Stufe 4 (12x12, Bbox 2..14), pro Frame wie ExperienceOrbRenderer getönt
  (`round3_settled_2026_10_02.orb_strip`).
- Gemessen im Client-Jar 26.3: Enderperle 13x13, Feuerkugel/Schleimball/Schneeball/Magmacreme 12x12, Herz des Meeres
  14x14, Erfahrungskugel Stufe 0/1/4 = 8/10/12, Goldnugget/Ghast-Träne 6x8. Die Kugel ist also so groß wie die
  Kugel-Items – „zu groß“ heißt: kleiner als diese, Richtung Erfahrungskugel klein / Nugget-Klasse.
  (Der Auftrag nannte „Enderperle 8x8-ish“ – gemessen ist sie 13x13; Annahme: Ziel ist sichtbar kleiner als heute.)

## Umsetzung
- `tools/textures/training_dummy_item_v4_2026_10_04.py`: drei neu gezeichnete 16x16-Item-Icons A–C (Pixel-Karten,
  Farben aus carved_pumpkin/pumpkin_side, hay_block_side inkl. Band, target_side, Straw-Stand-Item); Vorschau
  aktuell|A|B|C, 16x + 1x auf Inventar-Slot, dazu Modell-Ansicht (vorn/seitlich) der eingebauten Puppe.
  Optional `--install X` (schreibt Item + Wiki-Kopie) für die spätere Wahl.
- `tools/textures/sage_orb_smaller_2026_10_04.py`: drei neu gezeichnete Kugeln (Index-Karten in der Palette der
  Vanilla-Kugel, gleiche Puls-Tönung, 12 Frames) – A 10x10, B 8x8, C 9x9; Vorschau mit beschrifteten Vanilla-Größen.
  Optional `--install X`.

## Risiken
- Icon-Lesbarkeit bei 1x – deshalb 1x-Ansicht auf Slot-Grau in der Vorschau.
- Kugel animiert: Vorschau zeigt Frame 0 und den hellsten Frame.

## Verifikation
- Generatoren laufen, Vorschauen visuell geprüft (gegen Vanilla und Modell). Keine Mod-Dateien geändert ⇒ keine Gates
  nötig (nur Tools + Doku).
