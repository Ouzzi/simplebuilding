# Redstone-Truhen (Nachtrag 4)

## Plan – 2026-10-03

Arbeitsbranch: `claude-gpt-trapped`. Nur dieser Worktree, kein Push, kein Master,
kein Minecraft-Client. Bestehendes `.serena/` bleibt unberührt.

### Befund und Entscheidungen

- `TieredChestBlock`, `TieredChestBlockEntity`, `TieredChests` und
  `TieredChestRenderer` tragen bereits Kapazität, Stapelgrenzen, Menüs,
  Doppeltruhen und Automation. Drei neue Block-IDs teilen diese Infrastruktur.
- Vanilla 26.3 (`javap` aus lokalem Minecraft-Jar): Signal über `ownSignal`,
  begrenzt auf 0–15; starkes Signal nur `Direction.UP`; Betrachteränderungen
  aktualisieren Nachbarn an Truhe und Block darunter mit Redstone-Orientierung.
  Die Mod muss ihren eigenen Zähler verwenden (Vanillas privater bleibt leer).
- Normale Truhen werden in der Welt per Vorschlaghammer aufgewertet:
  verstärkt → Netherit → Enderit. Dieselben Kosten und Erhaltung von Inhalt,
  Namen, Schloss, Wasser und beiden Hälften gelten für Redstone-Truhen.
- Annahme: Einstieg in die drei Redstone-Stufen jeweils durch die gewünschte
  normale Stufentruhe + Haken. Kein zusätzlicher Weg aus der Holz-Redstone-Truhe,
  da auch normale Holztruhen keinen direkten Upgrade-Weg zur verstärkten haben.
- Neues `McVersion.TRAPPED_TIERED_CHESTS` nur auf 26.3 aktiv. Versionsabhängiger
  Block erhält einen 26.2-Zwilling; keine Registrierung auf 26.2.

### Umsetzung

1. Block-/Item-Registrierung, bestehende BlockEntity-Typen aller drei Loader,
   Redstone-Signal und Vanilla-Statistik; Renderer/Texturwahl ergänzen.
2. Datagen für drei formlose Rezepte, Modelle, Loot und Tags; Hammer-Upgrades;
   Kreativtab neben normalen Truhen und Zweitplatzierung bei Vanilla-Redstone.
3. EN/DE in beiden Ressourcenbäumen, belegbare Wiki-Notizen, vorhandene
   Lager-/Truhen-Teststation erweitern.
4. `tier_chest_textures.py` erweitert ausschließlich abgeleitete Texturen:
   Vanilla-Rotakzent auf bestehender Stufentextur; beschriftete 16-fache
   Vorschau mit Vanilla normal/trapped, Einzel- und Doppeltruhen.
5. GameTests plus Fabric-Adapter/Katalog: Betrachter (inklusive Schließen,
   Grenze, Zuschauer, Nachzählung und beide Hälften), Paarung, Kapazität,
   Komparator, Eigenschaften, Rezepte, Upgrades und Ressourcenverdrahtung.

### Risiken und Verifikation

- Redstone-API unterscheidet sich zwischen 26.2 und 26.3; beide kompilieren.
- Null-Registrierungen auf 26.2 dürfen nicht in BlockEntity-Sets/Tags geraten.
- Kreativtab- und Datenintegritätstests können feste Reihenfolgen enthalten.
- Texturen müssen normale Besitzertexturen unverändert lassen.
- Datagen: `:mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263`.
- Fabric/NeoForge 26.3 zuerst gefiltert, anschließend vollständige Serversuiten;
  Testrunner-Zeilen `alles gruen` / `NICHT gruen` lesen, Exitcode reicht nicht.
- Compile `:compileJava :neoforge:compileJava :mc26_3:forge:compileJava`,
  vollständiges `check -q`, Wiki venv `--all`, uv-Python `--all --check`.
- Testzentrale serverseitig neu bauen und vollständige Itemabdeckung prüfen.
- Sichtabnahme im Minecraft-Client bleibt ausdrücklich offen.

## Umsetzung und Ergebnisse

### Wiederaufnahme nach Netzausfall

Die uncommittierte Umsetzung bleibt erhalten. Der vorhandene gefilterte Lauf
`2026-10-03T15-50-27Z-88cb` meldet `alles gruen: 26/26 bestanden, 0 rot`
(je 13 Truhentests auf Fabric und NeoForge 26.3). Dies ersetzt kein volles Gate.
Der lokale Vanilla-Bytecode bestätigt Signalgrenze, Richtung und Nachbarupdates.
Die bestehende Vorschau zeigt die unveränderten Grundtexturen mit rotem
Vanilla-Schließenakzent, jeweils einzeln und doppelt.

Nächste Schritte: Truhentest-Katalog sortieren; Datagen und vollständige
Server-/Compile-/Wiki-Prüfungen abschließen; generierte Dateien nach tatsächlichem
Inhaltsdiff auswählen. Build-Logs, Hilfsskripte, Datagen-Cache und `.serena/`
werden nicht committet. Kein Minecraft-Client wird gestartet.

Der erste vollständige Lauf (`2026-10-03T20-03-28Z-6c77`) fand drei Lücken
je Loader: Vanilla führt die Redstone-Truhe nicht unter Gebrauchblöcken; der
Suchtab-Eintrag steht daher jetzt gemeinsam mit den normalen Stufen hinter der
letzten Kupfertruhe. Die Zweitplatzierung bleibt bei Vanillas Redstone-Truhe im
Redstone-Tab. Die festen Erwartungen für Vanilla-Tab-Gegenstücke und exportierte
Hammer-Aufwertungen wurden um die neuen Einträge ergänzt. Alle 13 Truhentests
und der Neubau samt Abdeckung der Testzentrale bestanden bereits diesen Lauf.

### Verifizierter Abschlussstand

- Datagen und `syncGenerated263`: `DATAGEN_EXIT=0`, `BUILD SUCCESSFUL in 2m 44s`.
- Vollständige Serversuiten, Lauf `2026-10-03T20-10-42Z-5220`:
  Fabric 26.3 **892/892**, NeoForge 26.3 **892/892**;
  `alles gruen: 1784/1784 bestanden, 0 rot`.
  Enthalten: je fünf neue Redstone-Truhentests, acht bisherige Truhentests und
  sieben Testzentralentests, einschließlich Neubau und vollständiger Item-/Blockabdeckung.
- Separate Compiles `:compileJava :neoforge:compileJava :mc26_3:forge:compileJava`
  mit `-Pforge263=true`: `BUILD SUCCESSFUL in 40s`.
- Wiki mit vorhandener Pillow-venv `--all`, danach uv-Python `--all --check`:
  beide Exit 0; `wiki: up to date, everything documented.`
- Texturen: `trapped chest textures: 9/9 current` sowie
  `OK: 508 Texturen und 9 .mcmeta in 2 Baeumen aktuell`.
- EN/DE in beiden Ressourcenbäumen: jeweils sieben neue Schlüssel,
  keine doppelten Schlüssel. `git diff --check` ohne Befund.
- Vorschau: `C:/Users/o_o/code/minecraft-mods/previews/redstone-truhen-vorschau.png`,
  identische Worktree-Kopie `build/redstone-truhen-vorschau.png`, visuell geprüft.
  SHA-256: `7df5c23dea5627778f71dbb027311f4eb0772d5ccb991339b6bb91b491dd070d`.

### Grenzen und bestehende Fehler

Kein Minecraft-Client gestartet; die Sichtabnahme im Spiel und Forge-GameTests
bleiben offen. 26.2 hat nur den Compile-Zwilling und das deaktivierte Feature-Flag;
1.21.11 und 26.4 wurden nicht portiert.

`tools/guide_book_pages.py` meldet bereits auf unverändertem HEAD zwei Probleme:
`de_de guide topics 1: 14 lines`, `de_de guide topics 3: 15 lines` (Grenze 13).
Die Gegenprüfung verwendete unveränderte HEAD-Quellen in `build/trapped-guide-baseline/`;
keine Arbeitsdateien wurden zurückgesetzt. Diese bestehenden Inhaltsseiten wurden
nicht geändert. Logs: `build/trapped-guides.log` und `build/trapped-guides-baseline.log`.

Das erste Gesamt-Gate scheiterte an Netzwerk-/DNS-Fehlern beim Mojang-Manifest
in sechs NeoForge-Modulprojekten. Der Wiederholungslauf mit `-Pforge263=true check -q`
bestand vollständig: **`GATE_RETRY_EXIT=0`**, Log `build/trapped-gate-retry.log`.
Enthalten sind unter anderem `checkBalance`, Atlas-/Jade-Prüfungen, alle
Modul-Datenprüfungen und 38 erfolgreiche Wiki-Tests. Kein Gate wurde übersprungen.

### Geänderte Dateien (77)

- .claude/QUEUE.md
- common/src/mc26_2/java/com/simplebuilding/blocks/custom/TieredTrappedChestBlock.java
- common/src/mc26_2/java/com/simplebuilding/version/McVersion.java
- common/src/shared/java/com/simplebuilding/blocks/custom/TieredChestBlock.java
- common/src/shared/java/com/simplebuilding/blocks/entity/custom/TieredChestBlockEntity.java
- common/src/shared/java/com/simplebuilding/blocks/ModBlocks.java
- common/src/shared/java/com/simplebuilding/client/render/TieredChestRenderer.java
- common/src/shared/java/com/simplebuilding/dev/testcentre/TestCentreSections.java
- common/src/shared/java/com/simplebuilding/gametest/DataIntegrityTests.java
- common/src/shared/java/com/simplebuilding/gametest/InWorldExportTests.java
- common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java
- common/src/shared/java/com/simplebuilding/gametest/TieredChestTests.java
- common/src/shared/java/com/simplebuilding/items/ModItemGroupsContent.java
- common/src/shared/java/com/simplebuilding/items/ModItems.java
- common/src/shared/java/com/simplebuilding/items/SearchTabPlacement.java
- common/src/shared/java/com/simplebuilding/util/SledgehammerUpgrades.java
- docs/ai/PLAN-REDSTONE-TRUHEN-2026-10-02.md
- forge/src/main/java/com/simplebuilding/forge/ForgeModRegistries.java
- mc26_3/generated/assets/simplebuilding/blockstates/enderite_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/blockstates/netherite_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/blockstates/reinforced_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/items/enderite_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/items/netherite_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/items/reinforced_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/models/block/enderite_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/models/block/netherite_trapped_chest.json
- mc26_3/generated/assets/simplebuilding/models/block/reinforced_trapped_chest.json
- mc26_3/generated/data/c/tags/block/chests.json
- mc26_3/generated/data/c/tags/block/chests/trapped.json
- mc26_3/generated/data/c/tags/item/chests.json
- mc26_3/generated/data/c/tags/item/chests/trapped.json
- mc26_3/generated/data/minecraft/tags/block/mineable/pickaxe.json
- mc26_3/generated/data/simplebuilding/advancement/recipes/redstone/enderite_trapped_chest.json
- mc26_3/generated/data/simplebuilding/advancement/recipes/redstone/netherite_trapped_chest.json
- mc26_3/generated/data/simplebuilding/advancement/recipes/redstone/reinforced_trapped_chest.json
- mc26_3/generated/data/simplebuilding/loot_table/blocks/enderite_trapped_chest.json
- mc26_3/generated/data/simplebuilding/loot_table/blocks/netherite_trapped_chest.json
- mc26_3/generated/data/simplebuilding/loot_table/blocks/reinforced_trapped_chest.json
- mc26_3/generated/data/simplebuilding/recipe/enderite_trapped_chest.json
- mc26_3/generated/data/simplebuilding/recipe/netherite_trapped_chest.json
- mc26_3/generated/data/simplebuilding/recipe/reinforced_trapped_chest.json
- mc26_3/generated/data/simplebuilding/sledgehammer_upgrades/enderite.json
- mc26_3/generated/data/simplebuilding/sledgehammer_upgrades/netherite.json
- mc26_3/generated/data/simplebuilding/tags/item/enderite_ingot_tier.json
- mc26_3/generated/data/simplebuilding/tags/item/enderite_items.json
- mc26_3/generated/wiki/inworld.json
- mc26_3/generated/wiki/items.json
- mc26_3/overlay/java/com/simplebuilding/blocks/custom/TieredTrappedChestBlock.java
- mc26_3/overlay/java/com/simplebuilding/version/McVersion.java
- mc26_3/overlay/resources/assets/simplebuilding/lang/de_de.json
- mc26_3/overlay/resources/assets/simplebuilding/lang/en_us.json
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/enderite_trapped.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/enderite_trapped_left.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/enderite_trapped_right.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/netherite_trapped.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/netherite_trapped_left.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/netherite_trapped_right.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/reinforced_trapped.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/reinforced_trapped_left.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/reinforced_trapped_right.png
- neoforge/src/main/java/com/simplebuilding/neoforge/NeoForgeModRegistries.java
- src/main/java/com/simplebuilding/blocks/entity/ModBlockEntities.java
- src/main/java/com/simplebuilding/datagen/ModBlockTagProvider.java
- src/main/java/com/simplebuilding/datagen/ModItemTagProvider.java
- src/main/java/com/simplebuilding/datagen/ModLootTableProvider.java
- src/main/java/com/simplebuilding/datagen/ModModelProvider.java
- src/main/java/com/simplebuilding/datagen/ModRecipeProvider.java
- src/main/java/com/simplebuilding/gametest/TieredChestGameTest.java
- src/main/resources/assets/simplebuilding/lang/de_de.json
- src/main/resources/assets/simplebuilding/lang/en_us.json
- tools/textures/tier_chest_textures.py
- wiki/assets/textures/render/enderite_trapped_chest.png
- wiki/assets/textures/render/netherite_trapped_chest.png
- wiki/assets/textures/render/reinforced_trapped_chest.png
- wiki/data/simplebuilding.js
- wiki/data/simplebuilding.json
- wiki/manual.json
