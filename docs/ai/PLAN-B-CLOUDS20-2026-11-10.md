# Plan B-CLOUDS20 (10.11.): Wolkenblock Variante B (Milchglas), N20a

Branch `b-clouds20`, Basis `hub/b-bundle171-gen` (enthält b-clouds17). Besitzer CLOUDS17 = B. Vorlauf: PLAN-B-CLOUDS17-2026-11-10.md.

## Umsetzung
- `cloud_block.png`: Alpha 160/200/210/230 (Tonstufen wie Variante B des Generators), keine Lücken, Farben = abgenommene Quelle (Ebenen aus 67315b9c4
  wiederhergestellt, da die CLOUDS17-Löcher die Farbe der Lückenpixel entfernt hatten). Schicht in 26.3 folgt dem Alpha: nur 1..254 = TRANSLUCENT.
- Generator `gen_clouds17_alpha.py`: neue Option `--variant a|b` (Standard jetzt b) für In-place-Lauf.
- Wolkenstein unverändert (Eis, Alpha 185–220). Wiki-Kopie (PNG + layers.json) mitgezogen.
- Culling unverändert (Kern überspringt Flächen gegen Wolkenblock, Wolkenblock zeichnet zum Kern, Wolke–Wolke übersprungen).
- Test `cloud_texture_layers`: Wolkenblock nur Alpha 1..254 (translucent). Kein neuer Test sonst.

## Performance
Beide Wolkenblöcke sind jetzt translucent und werden je Abschnitt bei Kamerabewegung sortiert (teurer als cutout der Variante A). Das
Culling hält die Fläche gering: Wolke–Wolke und Kern–Hülle zeichnen nichts, es bleiben Insel-Außenflächen (Hülle zu Luft/Nebel) und
frei liegende Kernflächen – Größenordnung Eis/Wasser/Buntglas in Vanilla. Preis: Hülle der Insel ist keine billige cutout-Fläche mehr.
Client-FPS über großer Insel im Bündel-Client-Lauf prüfen (nicht getestet).

## Verifikation
Ein sbt: Compile Kern + simplelayers, module-simplelayers fabric + standalone, `layers.py check`.
