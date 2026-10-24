# Plan b-wb24small (24.10.2026): WB24-1, WB24-5, WB24-7
NUR 26.3 Fabric.
1. WB24-1 Inverser Besatz: Rand = Fuellfarbe minus `TrimInversion.RIM_SUBTRACT` (80) je Kanal, keine eigene Randfarbe; Stufe 3 Uebergang = Mittel Fuellung/Rand (halbe Subtraktion). Datei `TrimInversion`, Test `trim_inversion`. Vorschau `wb24-1-trim-subtract.html`.
2. WB24-5 Vitrinen: Variante C (Glas Alpha 48 + Rahmenrampe) fuer alle 16 Farbstoffe. `DisplayCaseBlock.Tint` = NONE + 16 DyeColor, Schleichen + Farbstoff setzt/ersetzt; Generator `tools/textures/simple_shops_tint_2026_10_24.py` (HSV aus der Rot-Rampe), 2 Blockstates, 32 Modelle, 48 Texturen. Test `display_tint_red` erweitert (Umfaerben). Vorschau `wb24-5-display-tint.html`.
3. WB24-7: `MapsConfig.pictureMap` Standard an; Test + Wiki-Text angepasst.
Verifikation: Targets module-simpletrims/simpleshops/simplemaps (fabric-263 + standalone), Generatoren, `./gradlew check`.
