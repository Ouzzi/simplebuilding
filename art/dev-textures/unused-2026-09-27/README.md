# Ungenutzte eigene Texturen (2026-09-27)

Aus `assets/simplebuilding/textures/` herausgenommen (Audit 2026-09-26 #44): kein Modell, kein Code,
kein Werkzeug (`tools/textures/generate_textures.py`), kein Wiki und kein Datagen verweist auf sie.
Pfade wie im Asset-Baum. Nicht mit der Mod ausgeliefert, aber aufbewahrt, weil es eigene Pixelkunst ist:

- `block|entity/chest|item/chest|item/*_chest_left|right`, `*_copper_right`: Reste der auskommentierten
  Verstaerkten/Netherit-Truhe (ModItems: `// Todo: REINFORCED_CHEST/NETHERITE_CHEST`).
- `entity/shulker/netherite_shulker*`, `item/reinforced_shulker_box.png`: Shulker-Entwuerfe.
- `block/*_smoker_bottom.png`, `block/smoker_side.png` (veraenderte Vanilla-Kopie): von keinem Ofen-Modell benutzt.
- `dev/speedometer_00..15.png`: alter Tacho-Entwurf; der Tacho (`SpeedometerHudOverlay`) zeichnet ohne Textur.

Wer eine davon wieder braucht, legt sie zurueck unter `src/main/resources/...` **und**
`mc1_21_11/fabric/src/main/resources/...` (beide Linien hatten identische Kopien).
