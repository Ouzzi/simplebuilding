# Plan A-STRONG23 (23.10.2026) - W23-7 / W23-8

## W23-7 Blazing-Obsidian-Animation
Ist: 16 Frames, interpolate, frametime 5 = 80 Ticks = 4 s Loop (zu schnell laut Besitzer).
Soll: frametime 12 = 192 Ticks, rund 10 s Loop (2,4x langsamer). Begruendung: ein schmelzender, glimmender Block soll langsam atmen; Vanilla-Lava/Magma bewegen sich ebenfalls traege (Lava-Fluss 20 Frames, aber nur ruhiges Wabern); interpolate bleibt an, damit es weich bleibt. Kein Umbau der Frames.
Dateien: tools/textures/blazing_obsidian_t1_2026_10_21.py (ANIMATION, Quelle fuer astral_enchanting_textures.py), mc26_3/overlay/.../blazing_obsidian.png.mcmeta, wiki/data/textures.js (Generator).

## W23-8 Strong-Upgrade-Vorlage
Ist (handgeschriebene JSON, kein Datagen):
- Erste Vorlage (strong_upgrade_template, nur mit Simple Mobs): 4 Diamant + 4 Obsidian + 1 Netherherz -> 1.
- Vervielfaeltigung (strong_upgrade_template_duplication): Vorlage + 6 Diamant + 1 Obsidian -> 2 (Vanilla-Ring, ohne Netherherz).
Soll:
- Vervielfaeltigung nur mit Netherherz, formlos: Vorlage + 1 Weinender Obsidian + 1 Diamant + 1 Netherherz -> 2. Mit Simple-Mobs-Ladebedingung wie die Erstvorlage (Netherherz existiert sonst nicht; standalone = keine Kopie).
- Kosten je zusaetzliche Vorlage: 1 Diamant + 1 Weinender Obsidian + 1 Netherherz, gegenueber 4 Diamant + 4 Weinender Obsidian + 1 Netherherz bei der ersten: guenstiger (alter Weg: 6 Diamant + 1 Obsidian ohne Herz).
- Abweichung/Vermerk: Besitzer sagte "Obsidian", dazu "generell in den Strong-Upgrade-Rezepten Crying Obsidian" -> Weinender Obsidian in beiden Rezepten (Erstvorlage ebenfalls umgestellt).
Dateien: recipe/strong_upgrade_template.json, recipe/strong_upgrade_template_duplication.json, questionary EN/DE, wiki/manual.json, simplemoney prices.json (Text), tools/check_data.py, BeaconsTests (Craft-Test; Fabric-Adapter existiert, Test-ID unveraendert).
Verifikation (sbt): compile, module-simplebeacons-fabric-263 + -standalone-fabric-263, fabric-263 Filter beacon, Generatoren, check. check_balance: Balance-Dateien betreffen Beacons-Rezepte nicht (zu pruefen per grep).

## Plan-Abgleich
Siehe Abschlussabschnitt unten (nach Lauf ausfuellen).
