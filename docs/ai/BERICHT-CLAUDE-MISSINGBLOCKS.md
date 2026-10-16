# Bericht claude-missingblocks (2026-10-16)
Neues optionales Sub-Mod `simpleblocks` („Simple Blocks“, Schalter „Enable Simple Blocks“, 3 Loader, im SB-Jar gebündelt); Konzept §7.
Neue Blöcke (33): hushed_deepslate_tiles/_tile_stairs/_tile_slab/_tile_wall, sculk_deepslate; calcite_/netherrack_/smooth_basalt_ stairs+slab+wall;
cut_sandstone_/cut_red_sandstone_ stairs+wall; smooth_sandstone_wall, smooth_red_sandstone_wall; cracked_ red_nether/tuff/mud/end_stone/cinnabar/sulfur_bricks;
mossy_ tuff/mud/end_stone/deepslate_bricks/deepslate_tiles/cinnabar/sulfur_bricks.
Vibrationen: Block-Tags dampens_vibrations + occludes_vibration_signals (GameTest: Sensor hinter dem Block hört nichts, Kontrollsensor schon). Sculk-Tiefenschiefer: Rezept + Ancient-City-Pool (Fabric/NeoForge/Forge-Hook), 1-2 XP.
Von Vanilla 26.3 schon vorhanden -> weggelassen: Beton-Treppen/-Stufen, Sandstein-/Rotsandstein-Treppe/Stufe/Mauer, glatte Sandstein-Treppe/Stufe, geschnittene Sandstein-Stufe, rissige Tiefenschieferziegel/-fliesen und Netherziegel, Steinziegel-Varianten, alle Ziegel-Treppen/Stufen/Mauern von Tuff/Lehm/Endstein/Zinnober/Schwefel/Tiefenschiefer.
simplelib: nichts (alle Grundblöcke sind Vanilla, keine geteilten Teile). SB-Hauptmod nur: Config-Schalter, Lang, mods.toml/fabric.mod.json, Gradle-Bündelung.
Texturen: Fliesen + Sculk-Tiefenschiefer eigene Pixel (docs/ai/previews/claude-missingblocks/own_8x.png); rissig/bemoost beim Bauen aus Vanilla abgeleitet (Vorschau /root/previews/claude-missingblocks/aged_8x.png, nicht eingecheckt); Formen per Modell auf Vanilla-Texturen.
Daten ohne Datagen: modules/simpleblocks/tools/generate_data.py (285 Dateien, --check in check_data.py). Wiki, Guide-Buch, Discover, Testzentrale-Station, EN/DE.
Abweichungen: Modul statt Hauptmod (Besitzer-Korrektur); Commits nach Inhalt statt je Teil; Datagen des Hauptmods entfällt. Offen: Merge nach wave1, Client-/Besitzer-Abnahme.
Tests (sb-test): module-simpleblocks fabric/neoforge/forge + standalone fabric/neoforge 45/45 gruen (9 Tests je Ziel), config_option 20/20, Client-Smoke 3/3 (Szene /root/previews/claude-missingblocks/0001_simpleblocks-front.png, Vanilla-Pixel, nicht eingecheckt). Nicht gefahren: Forge-Standalone, voller Gate. data_integrity: 1 rot (simpleend Kistenboot-Modelle, nicht von uns).
