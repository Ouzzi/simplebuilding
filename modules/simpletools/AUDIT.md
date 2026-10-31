# AUDIT Simple Tools (`simpletools`) – Release-Audit b-rel32-qolb, 31.10.2026

Basis: b-rel32 d9cda1e21, Branch `b-rel32-qolb`. Umfang: 5147 Zeilen Java (davon ca. 1,9k Tests), 19 Mixin-Klassen (17 in der Mixin-Liste inkl. 2 Accessoren). Nur MC 26.3 Fabric. Pruefliste C1–C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Durchgesehene Dateien (jede Datei vollstaendig gelesen)

Java (Fabric 26.3 + shared; neoforge/forge-Loader-Klassen nicht Teil des Auftrags, nur NUR-Fabric):
- `clienttest/java/com/simplebuilding/modules/simpletools/ModuleClientSmoke.java` (95 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpletools/ModuleGameTest.java` (73 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpletools/ToolsFabric.java` (52 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpletools/ToolsFabricClient.java` (14 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpletools/ToolsModMenu.java` (9 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/DeathTests.java` (754 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/FrameTests.java` (261 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/FurnaceLavaFillHandler.java` (87 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ItemFrames.java` (108 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/SimpleTools.java` (165 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/Thrift.java` (47 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ToolsCommands.java` (38 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java` (71 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ToolsLoot.java` (31 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ToolsRegistry.java` (65 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ToolsScreen.java` (76 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/ToolsTests.java` (138 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/brush/BiomeBrush.java` (181 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/brush/BiomeBrushItem.java` (82 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/brush/BrushTests.java` (210 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/client/DeathVaultScreen.java` (331 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/discover/ToolsDiscover.java` (275 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/guide/ToolsGuide.java` (137 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/AnvilRepairCostMixin.java` (49 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/AnvilSoulboundMixin.java` (36 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/DiscoverTipMixin.java` (23 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/EntityRemovedMixin.java` (23 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/FullDurabilityDamageMixin.java` (39 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/FullDurabilityEfficiencyMixin.java` (34 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/InventoryDropAllMixin.java` (32 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ItemEntityAgeAccessor.java` (12 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ItemEntityLedgerMixin.java` (61 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ItemFrameFixedAccessor.java` (15 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ItemFrameInteractMixin.java` (40 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ItemStackSoulboundTooltipMixin.java` (29 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/LivingDropCaptureMixin.java` (21 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/PlayerDeathDropsMixin.java` (30 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ServerPlayerRestoreMixin.java` (20 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/ThriftDurabilityMixin.java` (28 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/mixin/WanderingTraderSoulboundMixin.java` (19 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/network/ConfigSyncPayload.java` (18 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/soulbound/Soulbound.java` (153 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/vault/DeathLedger.java` (292 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/vault/DeathVault.java` (201 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/vault/DeathVaultBlock.java` (47 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/vault/DeathVaultMenu.java` (212 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/vault/Fate.java` (64 Zeilen)
- `shared/java/com/simplebuilding/modules/simpletools/vault/RemovalContext.java` (57 Zeilen)

Ressourcen/Daten (Zeilen):
- `fabric/src/main/resources/fabric.mod.json` (47)
- `shared/resources/assets/simpletools/blockstates/death_vault.json` (7)
- `shared/resources/assets/simpletools/icon.layers.json` (64)
- `shared/resources/assets/simpletools/items/biome_brush.json` (0)
- `shared/resources/assets/simpletools/items/death_vault.json` (6)
- `shared/resources/assets/simpletools/items/guide_book.json` (6)
- `shared/resources/assets/simpletools/lang/de_de.json` (165)
- `shared/resources/assets/simpletools/lang/en_us.json` (165)
- `shared/resources/assets/simpletools/models/block/death_vault.json` (8)
- `shared/resources/assets/simpletools/models/item/biome_brush.json` (0)
- `shared/resources/assets/simpletools/models/item/guide_book.json` (6)
- `shared/resources/assets/simpletools/questionary/de_de.json` (35)
- `shared/resources/assets/simpletools/questionary/en_us.json` (35)
- `shared/resources/assets/simpletools/textures/block/death_vault_bottom.layers.json` (28)
- `shared/resources/assets/simpletools/textures/block/death_vault_side.layers.json` (46)
- `shared/resources/assets/simpletools/textures/block/death_vault_top.layers.json` (64)
- `shared/resources/assets/simpletools/textures/item/biome_brush.layers.json` (28)
- `shared/resources/assets/simpletools/textures/item/guide_book.layers.json` (28)
- `shared/resources/data/minecraft/tags/block/mineable/pickaxe.json` (5)
- `shared/resources/data/minecraft/tags/block/needs_diamond_tool.json` (5)
- `shared/resources/data/minecraft/tags/enchantment/in_enchanting_table.json` (5)
- `shared/resources/data/minecraft/tags/enchantment/non_treasure.json` (5)
- `shared/resources/data/minecraft/tags/enchantment/tradeable.json` (5)
- `shared/resources/data/minecraft/tags/enchantment/treasure.json` (5)
- `shared/resources/data/minecraft/tags/item/enchantable/durability.json` (6)
- `shared/resources/data/simplequalityoflife/enchantment/thrift.json` (21)
- `shared/resources/data/simpletools/advancement/recipes/decorations/death_vault.json` (32)
- `shared/resources/data/simpletools/advancement/recipes/misc/guide_book.json` (43)
- `shared/resources/data/simpletools/advancement/recipes/tools/biome_brush.json` (59)
- `shared/resources/data/simpletools/advancement/recipes/tools/biome_brush_nether_heart.json` (50)
- `shared/resources/data/simpletools/biome_brush/badlands.json` (7)
- `shared/resources/data/simpletools/biome_brush/basalt_deltas.json` (7)
- `shared/resources/data/simpletools/biome_brush/birch_forest.json` (7)
- `shared/resources/data/simpletools/biome_brush/cherry_grove.json` (7)
- `shared/resources/data/simpletools/biome_brush/crimson_forest.json` (7)
- `shared/resources/data/simpletools/biome_brush/dark_forest.json` (7)
- `shared/resources/data/simpletools/biome_brush/desert.json` (7)
- `shared/resources/data/simpletools/biome_brush/end_barrens.json` (7)
- `shared/resources/data/simpletools/biome_brush/end_highlands.json` (7)
- `shared/resources/data/simpletools/biome_brush/forest.json` (7)
- `shared/resources/data/simpletools/biome_brush/ice_spikes.json` (7)
- `shared/resources/data/simpletools/biome_brush/jungle.json` (7)
- `shared/resources/data/simpletools/biome_brush/mangrove_swamp.json` (7)
- `shared/resources/data/simpletools/biome_brush/mushroom_fields.json` (7)
- `shared/resources/data/simpletools/biome_brush/nether_wastes.json` (7)
- `shared/resources/data/simpletools/biome_brush/pale_garden.json` (7)
- `shared/resources/data/simpletools/biome_brush/plains.json` (7)
- `shared/resources/data/simpletools/biome_brush/savanna.json` (7)
- `shared/resources/data/simpletools/biome_brush/snowy_plains.json` (7)
- `shared/resources/data/simpletools/biome_brush/soul_sand_valley.json` (8)
- `shared/resources/data/simpletools/biome_brush/swamp.json` (7)
- `shared/resources/data/simpletools/biome_brush/taiga.json` (7)
- `shared/resources/data/simpletools/biome_brush/warped_forest.json` (7)
- `shared/resources/data/simpletools/enchantment/soulbound.json` (21)
- `shared/resources/data/simpletools/ftbquests/chapters/simpletools_start.json5` (41)
- `shared/resources/data/simpletools/ftbquests/install.txt` (6)
- `shared/resources/data/simpletools/ftbquests/lang/en_us/chapters/simpletools_start.json5` (10)
- `shared/resources/data/simpletools/loot_table/blocks/death_vault.json` (17)
- `shared/resources/data/simpletools/recipe/biome_brush.json` (45)
- `shared/resources/data/simpletools/recipe/biome_brush_nether_heart.json` (36)
- `shared/resources/data/simpletools/recipe/death_vault.json` (18)
- `shared/resources/data/simpletools/recipe/guide_book.json` (12)
- `shared/resources/data/simpletools/tags/enchantment/exclusive_set/soulbound.json` (5)
- `shared/resources/data/simpletools/tags/item/death_vault_offerings.json` (9)
- `shared/resources/data/simpletools/tags/worldgen/biome/biome_brush_locked.json` (11)
- `shared/resources/simpletools.mixins.json` (27)
- 7 PNG-Dateien (Texturen/Icons, aus `*.layers.json` erzeugt; Pixelgroesse geprueft durch check_mod_icons / check_missing_textures)
- `fabric/build.gradle`, `wiki/manual.json`, `tools/check_data.py`


## Ergebnis C1–C9

- **C1** Alle Klassen gelesen (Vault, Ledger, Menü, Screen, Soulbound, Brush, Frames, Registry, Config, Guide/Discover, Fabric-Einstiege). Server/Client getrennt: `DeathVaultScreen` nur im Client-Entry (`ToolsFabricClient`), `ToolsGuide` zieht `GameTestHelper` nur in einer Methode (Klasse liegt im Vanilla-Jar). Statische Zustände (`Soulbound.dying`, `DeathVault.collecting`, `RemovalContext.*`) nur Server-Haupt-Thread und per try/finally (`PlayerDeathDropsMixin`) bereinigt. DataSlots auf Short-Grenze geklemmt. Befunde tools-P3-3/-4/-5.
- **C2** Je Mixin (Ziel gegen 26.3 durch `check_client_mixins.py` im Test-Lauf): AnvilRepairCost (`AnvilMenu.createResult` RETURN, Kosten der Zutaten behalten); AnvilSoulbound (`createResult` RETURN, prio 1500, setzt Kosten; Fix tools-P3-2); DiscoverTip (`InventoryChangeTrigger.trigger` HEAD, generiert); EntityRemoved (`Entity.setRemoved` HEAD, Item-Fate); FullDurabilityDamage (`LivingEntity.getAttributeValue` RETURN, nur Player); FullDurabilityEfficiency (`Player.getDestroySpeed` RETURN); InventoryDropAll (`Inventory.dropAll` HEAD/RETURN, Soulbound raus/rein); ItemEntityAgeAccessor (Test-Zugriff); ItemEntityLedger (`ItemEntity.tick/playerTouch/hurtServer/isMergable`, Kontext-Marker); ItemFrameFixedAccessor; ItemFrameInteract (`ItemFrame.interact` HEAD prio 1100, readAdditionalSaveData, dropItem); ItemStackSoulboundTooltip (`addDetailsToTooltip` TAIL, nur Hardcore-Hinweis); LivingDropCapture (`createItemStackToDrop` RETURN); PlayerDeathDrops (MixinExtras `@WrapMethod dropEquipment`, try/finally); ServerPlayerRestore (`restoreFrom` TAIL); ThriftDurability (`ItemStack.getMaxDamage` RETURN); WanderingTraderSoulbound (`updateTrades` TAIL). `defaultRequire=1` (hart: fehlendes Ziel bricht beim Start ab, nicht still). Konfliktrisiko (nicht gegen Sodium/Iris/Lithium/JEI/Mod Menu im Spiel getestet; keines der Ziele liegt in deren Kerngebiet Rendering/Chunk-Optimierung): `ItemStack.getMaxDamage`, `LivingEntity.getAttributeValue`, `Player.getDestroySpeed` sind Allerwelts-Ziele, alle per RETURN (kettenvertraeglich mit anderen Haltbarkeits-/Attribut-Mods); `ItemEntity.isMergable` kann mit Entity-Optimierern (Merge-Logik) zusammentreffen.
- **C3** Defaults sinnvoll (alle Features an; Rückbau ueber Schalter; `deathVaultRedeem` aus, `fullDurabilityBonusMultiplier` 1.0 = kein Effekt). Grenzen in `normalizeNumbers` geklemmt (NaN-fest). Cloth-Screen (3 Reiter), `/simpletools` und `/simpletools config` angemeldet (`check_config_commands` 0 Probleme). Lang EN/DE je 165 Schluessel identisch, jede Optionsbeschreibung mit „Default:“/„Standard:“ (Skript geprueft).
- **C4** Rezepte (Brush 2 Varianten per Lade-Bedingung, Vault, Guide-Buch), Rezept-Advancements, Tags (Enchantments, Mineable, Vault-Opfer, gesperrte Biome), Loot-Tabelle des Vault im 26er-Format, 24 Biome-Brush-Datendateien, Texturen/Modelle vorhanden (`check_missing_textures` 0 Fehler). `check_lang_keys.py` gruen.
- **C5** Allein spielbar (Standalone-Target vorhanden). Fremdzugriffe nur bedingt: `simplemobs` (Rezept-Bedingung), `simplemoney:money_bill` (Tag-Eintrag `required:false`), `simplevisuals:frame_chisels` (leerer Tag), `simplebuilding` nur suggests. SimpleLib und Framework als Jar-in-Jar. Befund tools-P3-7 (depends ohne simplelib-Zeile, harmlos). Cloth Config ist harte Abhaengigkeit (Rückfrage Q-B1).
- **C6** A12: starke Eingriffe sind abschaltbar (`enableSoulbound`, `enableDeathVault`, `anvilRepairKeepsCost`, Item-Frame-Schalter, `biomeBrushEnabled`/`OpsOnly`). Soulbound/Thrift sind Datenpack-Verzauberungen (Vanilla-Mechanik, Treasure/Tag-Regeln wie Mending). Death Vault ist reiner Anzeigeblock ohne Block-Entity (kein Weltrisiko).
- **C7** Keine C2S-Pakete, nur S2C `config_sync` (nie C2S registriert, 32 KB-Grenze, Client ignoriert Muell). Menues: `DeathVaultMenu` prueft `stillValid`, Anzeigefelder nicht entnehmbar, Button nur mit Redeem-Option und eigenem Spieler. Vanilla-Client ohne Mod: Block/Item/Menue brauchen die Mod am Client (Fabric-ueblich). Dedicated Server: Client-Klassen nur im Client-Entry.
- **C8** Texturen aus eigenen Pixel-Ebenen (`*.layers.json`), keine Mojang-Pixel; keine IPs/Pfade/Schluessel (Grep sauber). Lizenz CC0-1.0 in fabric.mod.json, LICENSE im Repo-Wurzelordner.
- **C9** Kein TODO/FIXME im Code. Offene Konzeptpunkte: Death-Vault-Rueckkauf (`deathVaultRedeem`) ist bewusst „Future Feature“, standardmaessig aus (Release-Hinweis: als „experimentell“ beschreiben oder in der Beschreibung weglassen). Queue-Zeilen REV24-M-9 erledigt.

## Befunde

| ID | Befund | Fix / Restpunkt |
|---|---|---|
| simpletools-P3-1 | `ToolsLoot` fuegte das Soulbound-Buch in den End-City-Loot, auch wenn der Sub-Mod ausgeschaltet war (nur `enableSoulbound` geprueft). | gefixt: Commit e4a1919c3 (`Soulbound.active()`). |
| simpletools-P3-2 | `AnvilSoulboundMixin` verlangte 35 Level fuer Soulbound-Buecher auch bei ausgeschaltetem Soulbound/Sub-Mod. | gefixt: Commit e4a1919c3. |
| simpletools-P3-3 | `ItemEntityLedgerMixin.isMergable` ruft bei jeder Merge-Pruefung `DeathLedger.of()` auf (DataStorage-Lookup + HashMap). Billig, aber in Item-Farmen mit tausenden Entities messbar nicht null. | Restpunkt (kein Release-Blocker): optionaler Kurzschluss ueber einen statischen „es gibt getrackte Items“-Zaehler. |
| simpletools-P3-4 | Item-Entity, die durch ein Portal die Dimension wechselt: `EntityRemovedMixin` behandelt `CHANGED_DIMENSION` als „entladen“; die Kopie in der Zieldimension hat vermutlich eine neue UUID, der Ledger-Eintrag bleibt dann dauerhaft „liegt“ mit stehendem Timer. | Restpunkt, selten (nur Todes-Drops, die in ein Portal fallen); im Vault als „pausiert“ sichtbar. Verifikation: Test mit Item durch Netherportal. |
| simpletools-P3-5 | Generierte `ToolsDiscover.glob` kompiliert pro Inventar-Aenderung je Tip-Muster einen Regex (`Pattern.matches`). | Restpunkt im gemeinsamen Generator `tools/discover/Discover.java.in` (betrifft alle Module, nicht hier aendern). |
| simpletools-P3-6 | Server schickt `Component.translatable` (Befehls-Rueckmeldungen) – ein Vanilla-Client ohne Mod sieht den Schluessel. Betrifft nur Operatoren. | Restpunkt; Items/Bloecke setzen die Mod am Client ohnehin voraus. |
| simpletools-P3-7 | `fabric.mod.json` hat keine `simplelib`-depends-Zeile (die anderen fuenf Mods schon). SimpleLib liegt als Jar-in-Jar bei, daher kein Fehler. | Restpunkt (Konsistenz). |

## Test (ein sbt-Lauf fuer alle sechs Mods)

Lauf 31.10.2026 auf sb-test, Branch `b-rel32-qolb` (Commit e4a1919c3; Docs/Lang-Kategorie ff9fdd1fa): `:mc26_3:fabric:compileJava` + Modul-Compile C=0; `module-simpletools-fabric-263` **51/51 gruen**, `module-simpletools-standalone-fabric-263` **51/51 gruen**; `check_client_mixins.py` M=0 (660 Mixins, 0 Probleme, alle Mods); `check_lang_keys.py` L=0 (52 Module).

## Urteil

**shipbar** (nach gruenem Testlauf, siehe unten). Keine P1/P2-Befunde; zwei P3 gefixt, fuenf P3 als Restpunkt dokumentiert. Rückfragen: Q-B1 (Cloth Config), Q-B4 (Biome-Brush-Preis), Q-B5 (Redeem-Option im Release).
