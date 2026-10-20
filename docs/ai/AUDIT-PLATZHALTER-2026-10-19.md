# Platzhalter-/Textur-Audit der Kreativtabs

Erzeugt von `tools/textures/placeholder_audit.py` (b-texfix20). Grau = Platzhalter der Vanilla-derived-Texturen,
„fehlt“ = Textur/Item-Definition nicht im Jar. Kontaktbögen nur in der HTML-Seite (Mojang-Pixel, nicht im Repo).

Lauf 20.10. auf sb-test, Stand b-texfix20 9dab92135: Tab-Dump aus `integration-263` (SB_CREATIVE_TAB_DUMP, 3/3 grün),
alle Fabric-26.3-Jars aus `modules/modules.json` (43), Client-Jar 26.3. Ergebnis: **0 Platzhalter, 0 fehlende Texturen**.
„ohne Icon“ = Spezial-Renderer, den der Offline-Renderer nicht nachbaut (Mob-Köpfe, Banner, Betten, Schilder …), kein Fehler.
Gegenprobe (`-PsbSkipVanillaDerived`): simplebuilding-Jar 78 Platzhalter + Marker, simpleovens Marker → Exit 1.

## Jars

| Jar | Texturen | Platzhalter | Marker |
|---|---:|---:|---|
| `simplebuilding-26.3-1.3.1.jar` | 1215 | 0 | – |
| `wiringexample-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplemoney-26.3-fabric-1.2.16.jar` | 8 | 0 | – |
| `simpleriding-26.3-fabric-1.0.5.jar` | 18 | 0 | – |
| `simplemodels-26.3-fabric-0.1.0.jar` | 1 | 0 | – |
| `simplefun-26.3-fabric-1.4.0.jar` | 4 | 0 | – |
| `simplevisuals-26.3-fabric-1.0.7.jar` | 0 | 0 | – |
| `simplesounds-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplequalityoflife-26.3-fabric-1.0.6.jar` | 1 | 0 | – |
| `simpletweaks-26.3-fabric-1.2.12.jar` | 5 | 0 | – |
| `simpledimensions-26.3-fabric-0.1.0.jar` | 2 | 0 | – |
| `simplesandwiches-26.3-fabric-0.1.0.jar` | 241 | 0 | – |
| `simplelib-26.3-fabric-0.2.0.jar` | 17 | 0 | – |
| `simpleinterfaces-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplemaps-26.3-fabric-0.1.0.jar` | 4 | 0 | – |
| `simpletrims-26.3-fabric-0.1.0.jar` | 4 | 0 | – |
| `simplemobs-26.3-fabric-0.1.0.jar` | 45 | 0 | – |
| `simpleweather-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplemovement-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simpletools-26.3-fabric-0.1.0.jar` | 4 | 0 | – |
| `simplefarming-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplemobmanners-26.3-fabric-0.1.0.jar` | 1 | 0 | – |
| `simpleloot-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplequestionary-26.3-fabric-0.1.0.jar` | 5 | 0 | – |
| `simpleaesthetics-26.3-fabric-1.0.0.jar` | 0 | 0 | – |
| `simpleenddragonfight-26.3-fabric-0.1.0.jar` | 5 | 0 | – |
| `simpleend-26.3-fabric-0.1.0.jar` | 62 | 0 | – |
| `simplerespawn-26.3-fabric-0.1.0.jar` | 1 | 0 | – |
| `simplebeacons-26.3-fabric-0.1.0.jar` | 4 | 0 | – |
| `simpleovens-26.3-fabric-1.0.0.jar` | 40 | 0 | – |
| `simplemobheads-26.3-fabric-1.0.0.jar` | 1 | 0 | – |
| `simpleshops-26.3-fabric-0.1.0.jar` | 12 | 0 | – |
| `simplewanderers-26.3-fabric-0.1.0.jar` | 3 | 0 | – |
| `simplestats-26.3-fabric-0.1.0.jar` | 1 | 0 | – |
| `simpleblocks-26.3-fabric-1.0.0.jar` | 30 | 0 | – |
| `simplecrucibles-26.3-fabric-1.0.0.jar` | 87 | 0 | – |
| `simpleoctets-26.3-fabric-1.0.0.jar` | 2 | 0 | – |
| `simplehammers-26.3-fabric-1.0.0.jar` | 8 | 0 | – |
| `simpleoptimization-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simpleoptrender-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simpleoptmemory-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simpleoptserver-26.3-fabric-0.1.0.jar` | 0 | 0 | – |
| `simplestorage-26.3-fabric-1.0.0.jar` | 4 | 0 | – |

## Kreativtabs

| Tab | Einträge | ok | Platzhalter | fehlt | ohne Icon |
|---|---:|---:|---:|---:|---:|
| `simpletweaks:claims` | 3 | 3 | 0 | 0 | 0 |
| `minecraft:hotbar` | 0 | 0 | 0 | 0 | 0 |
| `simpleovens:ovens` | 13 | 13 | 0 | 0 | 0 |
| `minecraft:op_blocks` | 34 | 34 | 0 | 0 | 0 |
| `simplestorage:storage` | 16 | 16 | 0 | 0 | 0 |
| `minecraft:building_blocks` | 1027 | 1027 | 0 | 0 | 0 |
| `simpleblocks:blocks` | 461 | 461 | 0 | 0 | 0 |
| `simpleshops:shops` | 5 | 5 | 0 | 0 | 0 |
| `simplebuilding:combat` | 61 | 61 | 0 | 0 | 0 |
| `simplemobs:mobs` | 21 | 21 | 0 | 0 | 0 |
| `minecraft:food_and_drinks` | 281 | 281 | 0 | 0 | 0 |
| `simplebuilding:food` | 6 | 6 | 0 | 0 | 0 |
| `minecraft:search` | 3566 | 3566 | 0 | 0 | 263 |
| `simplebeacons:beacons` | 4 | 4 | 0 | 0 | 1 |
| `simpleriding:riding_items` | 10 | 10 | 0 | 0 | 0 |
| `simplebuilding:functional` | 143 | 143 | 0 | 0 | 4 |
| `simpleend:main` | 49 | 49 | 0 | 0 | 0 |
| `minecraft:ingredients` | 280 | 280 | 0 | 0 | 0 |
| `simplesandwiches:kitchen` | 26 | 26 | 0 | 0 | 0 |
| `simplecrucibles:main` | 23 | 23 | 0 | 0 | 0 |
| `minecraft:functional_blocks` | 516 | 516 | 0 | 0 | 143 |
| `minecraft:tools_and_utilities` | 293 | 293 | 0 | 0 | 0 |
| `simplemoney:money_items` | 8 | 8 | 0 | 0 | 0 |
| `simplehammers:hammers` | 11 | 11 | 0 | 0 | 0 |
| `simplebuilding:pads` | 45 | 45 | 0 | 0 | 3 |
| `simplebuilding:materials` | 68 | 68 | 0 | 0 | 0 |
| `simplefun:fun` | 2 | 2 | 0 | 0 | 0 |
| `simplebuilding:tools` | 111 | 111 | 0 | 0 | 0 |
| `simplemobheads:heads` | 213 | 213 | 0 | 0 | 212 |
| `minecraft:colored_blocks` | 327 | 327 | 0 | 0 | 33 |
| `simplebuilding:arrows` | 90 | 90 | 0 | 0 | 0 |
| `minecraft:inventory` | 0 | 0 | 0 | 0 | 0 |
| `minecraft:redstone_blocks` | 141 | 141 | 0 | 0 | 1 |
| `minecraft:spawn_eggs` | 97 | 97 | 0 | 0 | 0 |
| `simplemaps:maps` | 4 | 4 | 0 | 0 | 0 |
| `simpletrims:trims` | 4 | 4 | 0 | 0 | 0 |
| `minecraft:natural_blocks` | 300 | 300 | 0 | 0 | 0 |
| `minecraft:combat` | 258 | 258 | 0 | 0 | 1 |
| `simplebuilding:enchanted_dev` | 93 | 93 | 0 | 0 | 0 |
| `simpleoctets:main` | 176 | 176 | 0 | 0 | 0 |

## Befunde

- keine
