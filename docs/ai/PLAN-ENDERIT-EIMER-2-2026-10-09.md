# Enderit-Eimer fasst 2 Eimer (Queue N21 + N28, Branch `claude-q-ebucket`)

## Entscheidung: eigene Items statt Füllstand-Komponente
- `enderite_{water,lava,soul_lava}_bucket` (bestehende Ids) = **halb** (1/2), neue `enderite_*_bucket_full` = **voll** (2/2).
- Grund: Brennstoff (`cooking_fuel`), Crafting-Rest (`craftRemainder`), Werfer-Verhalten, Item-Modell und Tags sind in
  Vanilla je Item. Mit Items gibt ein voller Lava-Eimer im Ofen den halben zurück (der dann nochmal brennt), ein
  Rezept mit Wassereimer lässt den halben übrig - mit einer Komponente ginge dabei ein Eimer verloren.
  Bestehende Welten: alte gefüllte Enderit-Eimer bleiben 1 Eimer (gleiche Bedeutung wie bisher).
- Volle Eimer stehen nicht im Kreativtab (wie die abgenutzten Keramik-Eimer), nur durch zweimal Schöpfen; Testzentrale: Fass neben der Tiegel-Truhe.

## Regeln (ModBucketItem)
- Leer + Benutzen: schöpft → halb. Halb + Benutzen: schöpft nur (gleiche Flüssigkeit) → voll; gießt nie aus.
- Voll + Benutzen: gießt einen Eimer aus → halb. Schleichen + Benutzen (halb/voll): gießt einen Eimer aus.
- Vanilla-Kessel (CauldronBucketMixin) und verstärkter Kessel (CrucibleCompat.SbCauldronBuckets): voll gießt einen
  Eimer → halb; halb nimmt einen vollen Kessel der eigenen Flüssigkeit → voll.
- Werfer (BucketDispensing über `afterPour`): voll → halb, halb → leer.
- Tooltip: „Enthält 1/2 Eimer“ / „2/2“ + Bedienhinweis.
- Seelenlava: ebenfalls 2 Eimer (N28 und Auftrag; N21 „Seelenlava nur einfach aufnehmbar“ damit überholt - Besitzer ggf. bestätigen).

## Texturen
- `tools/textures/enderite_bucket_half_2026_10_09.py`: Halb = leerer Enderit-Eimer je Frame + Öffnung (Zeilen 2-5) aus der
  aktuellen Voll-Textur, ohne die zwei obersten äußeren Pixel (4,3)/(11,3); Überlauf entfällt. Wasser/Lava gleichen der
  abgenommenen Vorlage bis auf 6 Pixel in 4 Frames (dort zeigte die Vorlage Glanz statt Flüssigkeit), Seelenlava neu (Farben 09.10.).
