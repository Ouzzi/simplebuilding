# Hufeisen-Vorlage Runde 2: Plan, 2026-10-04

## Auftrag
Besitzer zu Runde 1 (`tools/textures/horseshoe_template_and_riding_books_2026_10_04.py`, A-J): Hintergrund gleich
lassen, nur das Hufeisenmotiv schoener, wieder 10 Vorschlaege. Nur Vorschau + Generator, NICHT einbauen.

## Umsetzung
- Generator `tools/textures/horseshoe_template_motif_2026_10_04.py`, Vorschau
  `C:\Users\o_o\code\minecraft-mods\previews\hufeisen-vorlage-runde2-vorschau.png` (eingebaut + A-J, 16x, 1x/2x im Slot).
- Basis = eingebaute PNG. Motivfeld = Vanilla-Motivbereich x 4..12, y 4..10. Annahme: Im alten Hufeisen-Rahmen
  (x 5..11, y 4..10) werden die Pfeilreste der Basic-Platte auf Plattenkupfer (193,90,54) gesetzt; sie gehoerten
  zum Motiv, nicht zum Hintergrund.
- Farben nur aus den Vanilla-Rampen iron_ingot/copper_ingot (wie eingebaut). Licht oben links, Kupferschatten unten rechts.
- 10 Formvarianten (keine Recolors): rund, breit mit Stollen, Parabel, eckig, diagonal, schlank mit Umriss, V-Zehe,
  gefalzt, Keilform, kraeftig mit Relief.

## Verifikation
- Generator prueft je Variante: alle Pixel ausserhalb Motiv und altem Rahmen identisch zur eingebauten Textur, Alpha
  identisch, Motiv nur im Feld, nur Palettenfarben, Mindestanteil Eisen. Sichtpruefung 16x und 1x gegen Vanilla.
- Kein Gameplay-/Asset-Change, daher keine GameTests/Gates.

## Runde 3 (Besitzer: "alle etwas zu gross")
- Generator `tools/textures/horseshoe_template_motif_round3_2026_10_04.py` (nutzt render/check aus Runde 2), Vorschau
  `previews\hufeisen-vorlage-runde3-vorschau.png` (jetzt | R2 | R3 je Buchstabe, 16x + 1x im Slot).
- Vanilla gemessen (Cyan-Motivpixel aus dem 26.3-Client-Jar): Netherit 5x6 (7x7 mit Umriss), Besatz meist 5-8 x 5-8.
- Jede Variante pixelgenau neu, Eisen-Box je Buchstabe kleiner als R2 und hoechstens 8x6 (Generator prueft das);
  Details reduziert (ein Nagelloch je Arm, keine Zehenloecher).

## Runde 4 (2026-10-05, Besitzer: "die meisten dennoch zu gross")
- Generator `tools/textures/horseshoe_template_motif_round4_2026_10_05.py`, Vorschau
  `previews\hufeisen-vorlage-runde4-vorschau.png` (Runde 3 | Runde 4, 16x + 1x).
- Eisen-Box hoechstens 5x5 (Generator prueft), mittig x 6..10 / y 5..9, 1-px-Baender wo moeglich, keine Nagelloecher.
