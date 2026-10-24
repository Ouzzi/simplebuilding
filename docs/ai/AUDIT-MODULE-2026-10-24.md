# Modul-Audit 24.10. (AUDM24) – Vollständigkeit aller Module

Branch `b-audmods24` (Basis claude-wave1 b74998032). Besitzer-Auftrag: jede Untermod prüfen, ob sie vollständig ist
oder ihr etwas aus anderen Mods fehlt (z. B. JEI-Rezepte, die vorher da waren); Grundlage für die Übersicht „welche
Sub-Mods wo enthalten sind, was jede macht“. Plan: `docs/ai/PLAN-B-AUDMODS-2026-10-24.md`.

**Methode (wiederholbar, ohne Gradle, ~4 s):** `python3.12 tools/audit/module_completeness.py --json docs/ai/module-inventory-2026-10-24.json --md <datei>`.
Liest `modules/modules.json`, die Ressourcenbäume je Modul (Kern: wirksamer 26.3-Baum inkl. `removed-on-26.3.txt`),
Lang, Java-Quellen, Testkataloge und vergleicht mit dem Stand **vor den Splits** (`134cb5a06^`, 11.10.): 272 IDs haben
seitdem den Namensraum gewechselt (Octets 105, Storage 51, Crucibles 37, Mobheads 32, Blocks 15, SimpleLib 13, Ovens 9,
Hammers 7, Trims 3). Zweck DE von Hand in `tools/audit/module_purposes.json`. Modelle/Texturen/Blockstates prüft
weiterhin `tools/check_missing_textures.py` (heute 0 Fehler, 1465 Item-Definitionen, 928 Blockstates).
Das Inventar-JSON (`module[]`: id, name_de/en, purpose_de/en, supermod, bundle, required, super_of, required_by,
standalone, counts, jei, commands, findings[]) ist die Datenbasis fürs Übersichts-Artifact (Phase 3).

Schwere: **P1** kaputt/verloren · **P2** fehlt für Spieler · **P3** prüfen/Qualität · **P4** Info/erwartet.
Heuristiken (geprüft, siehe „Bewertung“): Blöcke ohne eigenes Loot (Wand-Varianten, `getDrops`), ohne mineable-Tag
(Sofortabbau), Items nur per Java (Loot-Injektion, Umwandlung, Kreativ).

## Ergebnis in Kürze
- **Split-Vergleich sauber:** keine verlorenen Rezepte, kein verlorenes Block-Loot, keine verlorenen Vanilla-/c-Tags,
  keine verlorenen Advancements und keine fehlenden Tooltip-/JEI-Info-Schlüssel bei den 272 verschobenen IDs.
  Rezeptarten-Wechsel nur bei Simple Trims (Absicht, s. Top-10 #4).
- Lang EN/DE in allen 44 Modulen paritätisch; jeder Block/jedes Item hat einen Namen (Render-Stand-ins `placed_*`/`held_*`/
  `standing_*`/`enchanted_book_*` ausgenommen).
- **Behoben (1):** Schwebender/Aufsteigender Sand/Kies (Simple Blocks) fehlten in `minecraft:mineable/shovel` (schon im
  alten Kern) – abbaubar nur langsam per Hand-Tempo; Tag ergänzt + Prüfung in `NatureTests.movedBlocksRegistered`.
- JEI: 10 Module mit eigenem Plugin (Kern, Money, Fun, Lib, Trims, Mobheads, Blocks, Crucibles, Octets, Hammers), 22 Module mit Items/Blöcken ohne eigenes Plugin (standalone keine Infoseiten/Kategorien).

## Top-10-Funde
| # | Schwere | Modul | Fund | Weiter |
|---|---|---|---|---|
| 1 | P1 | simplemoney | `MoneyLinksJei` (Infoseite „Geld-Links“) hat nur `@JeiPlugin` – auf Fabric fehlt der Entrypoint `jei_mod_plugin` in `fabric.mod.json`, JEI lädt das Plugin dort nie (NeoForge/Forge per Annotation ok) | b-jei24 (AUDM24-1) |
| 2 | P2 | simplebuilding | 16 Rezepte `simplebuilding:fletching` (Befiederungstisch) ohne JEI-Kategorie; Pfeile nur über die Infoseite `crafted_arrow` | b-jei24 (AUDM24-2) |
| 3 | P2 | simpletweaks | Rezept `simpletweaks:claim_deed_copy` (Urkunde kopieren) in keinem JEI-Code | b-jei24 (AUDM24-2) |
| 4 | P3 | simpletrims | Vor dem Split hatte die Pulsierend-Vorlage ein `crafting_shapeless` + Schmiede-Dummies; jetzt `simpletrims:tool_shapeless` (eigener Serializer) und `TrimUpgradeRecipe`-Erweiterung – prüfen, dass JEI das Vorlagenrezept zeigt; ebenso `simplestorage:backpack_upgrade`, `simplebuilding:enchanted_shapeless` | b-jei24 (AUDM24-2) |
| 5 | P2→behoben | simpleblocks | Suspended/Levitating Sand/Gravel ohne `mineable/shovel` | erledigt |
| 6 | P3 | 22 Module | Kein eigenes JEI-Plugin trotz Items/Blöcken: u. a. simplestorage (53 Items), simpleend (49), simplemusic (28), simplemobs (25), simplesandwiches (24), simpleovens (13), simplebeacons, simpleshops – allein ohne Info-/In-World-Seiten (integriert deckt der Kern-JEI einen Teil ab) | b-jei24 (AUDM24-3) |
| 7 | P3 | simplestorage, simplesandwiches, simplebuilding | Aschholz-/Dämmerholz-Varianten (Kisten, Schneidebretter, Kern: Kessel, Platten, geschnitzte/hohle Stämme) sind immer registriert, aber nur mit Simple End herstellbar → allein unherstellbare Einträge in Tab/JEI | AUDM24-4 |
| 8 | P3 | 7 Module | Config-Labels ohne Tooltip (Riding 6, Dimensions 6, Aesthetics/Optimization je 3 „Enable …“-Schalter, Money, Fun, Kern-Befehlstexte) | b-cmdcfg24 (AUDM24-5) |
| 9 | P4 | simplebuilding | 111 Namensschlüssel und alte `tooltip.simplebuilding.{bundle,quiver}*`/`jei.simplebuilding.info.*_head` bleiben im Kern-Lang: nötig, weil 26.2/1.21.11 die IDs noch unter `simplebuilding` registrieren – erst beim Port-Run aufräumen | AUDM24-6 |
| 10 | P3 | simplecrucibles, simplemusic, simplestorage u. a. | Items nur per Java erreichbar (Eimer-Abnutzung, Platten per Loot-Injektion, Shulker-Schalen) – im Wiki „Herkunft“ fehlt dafür keine Seite, aber JEI zeigt keinen Weg (Infoseite fehlt) | b-jei24 (AUDM24-3) |

## Bewertung der Heuristik-Funde (Stichproben gegen Code)
- `LOOT-MISSING-GETDROPS`: Simple Octets (176) und Kern (Schach, Hängematten-Seil, Ziegenhorn-Halter, Stehende Stange)
  droppen über `Block#getDrops` – kein Fehler. Wandköpfe/-schilder/-fackeln, Kolbenköpfe, Feuer, Portale, `placed_*`,
  Seelenlava: erwartet ohne eigene Tabelle.
- `TAG-NO-MINEABLE`: übrig sind Sofortabbau-/Hand-Blöcke (Leuchtplatten wie Glasscheiben, Redstone-Linien, End-Stäbe,
  Lampen, Hängematten/Wolle-Achtel, Butter/Käse 0,5, Knospen, End-Gras, Starkes Leuchtfeuer wie Vanilla-Leuchtfeuer,
  Froschlichter wie Vanilla). Kein weiterer Fehler.
- `SPLIT-JEI-MENTION-LOST netherite_sledgehammer`: falsch positiv – `InWorldCategory` nennt den Hammer jetzt über
  `HammerRefs.item("netherite")`. `SPLIT-LANG-EXTRA-LOST` Mobheads: Simple Mobheads hat eigene Infoseiten je Kopf.
- `ITEM-NO-ORIGIN music_disc_voidline_track_3`: Spur 3/4 entsteht per Vorschlaghammer-Umwandlung (`MusicDiscs`), kein Fehler.
- Kreativtab: jedes Modul mit Items hat Tab-Code (nur `wiringexample`, Testmodul, nicht).
- Standalone-Target: alle 41 Spieler-Module haben eins (Kern läuft als `fabric-263`, `simplelib` ist Bibliothek).

## Übersicht aller Module
| Modul (id) | Super-Mod | Pflicht/optional | allein | Blöcke | Items | Rezepte | Adv. | Config | Befehle | Tests | JEI | Funde P1/P2/P3/P4 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SimpleBuilding (`simplebuilding`) | Super-Mod | eigenständig | nein | 339 | 606 | 885 | 120 | 282 | 4 | 1233 | ja | 0/2/5/3 |
| Wiring Example (`wiringexample`) | – | eigenständig | nein | 0 | 1 | 0 | 0 | 0 | 0 | 0 | – | 0/1/2/0 |
| Simple Money (`simplemoney`) | simplebuilding | optional | ja | 0 | 8 | 9 | 0 | 11 | 0 | 23 | ja | 1/0/1/0 |
| Simple Riding (`simpleriding`) | simplebuilding | optional | ja | 0 | 8 | 8 | 1 | 31 | 0 | 44 | – | 0/0/2/0 |
| Simple Models (`simplemodels`) | simpleaesthetics | Pflicht | ja | 0 | 1 | 1 | 0 | 7 | 0 | 21 | – | 0/0/1/0 |
| Simple Fun (`simplefun`) | simplebuilding | optional | ja | 0 | 2 | 2 | 1 | 21 | 1 | 30 | ja | 0/0/1/0 |
| Simple Visuals (`simplevisuals`) | simpleaesthetics | Pflicht | ja | 0 | 0 | 0 | 0 | 51 | 1 | 22 | – | 0/0/0/0 |
| Simple Sounds (`simplesounds`) | simpleaesthetics | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 19 | – | 0/0/0/0 |
| Simple Quality of Life (`simplequalityoflife`) | Super-Mod | eigenständig | ja | 0 | 1 | 1 | 0 | 7 | 1 | 6 | – | 0/0/1/0 |
| Simple Tweaks (`simpletweaks`) | simplebuilding | optional | ja | 1 | 3 | 3 | 0 | 0 | 18 | 164 | – | 0/1/2/0 |
| Simple Dimensions (`simpledimensions`) | simplebuilding | optional | ja | 2 | 2 | 1 | 0 | 54 | 0 | 45 | – | 0/0/3/1 |
| Simple Sandwiches (`simplesandwiches`) | simplebuilding | optional | ja | 19 | 23 | 17 | 0 | 0 | 0 | 38 | – | 0/0/4/0 |
| SimpleLib (`simplelib`) | – | eigenständig | nein | 7 | 17 | 29 | 0 | 5 | 0 | 36 | ja | 0/0/1/0 |
| Simple Interfaces (`simpleinterfaces`) | simplequalityoflife | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 36 | – | 0/0/0/0 |
| Simple Maps (`simplemaps`) | simplebuilding | optional | ja | 0 | 5 | 7 | 0 | 0 | 1 | 54 | – | 0/0/1/0 |
| Simple Trims (`simpletrims`) | simplebuilding | optional | ja | 1 | 5 | 10 | 3 | 1 | 0 | 22 | ja | 0/1/1/1 |
| Simple Mobs (`simplemobs`) | simplebuilding | optional | ja | 2 | 25 | 66 | 0 | 0 | 0 | 109 | – | 0/0/3/0 |
| Simple Weather (`simpleweather`) | simplequalityoflife | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 5 | – | 0/0/0/0 |
| Simple Movement (`simplemovement`) | simplequalityoflife | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 1 | 18 | – | 0/0/0/0 |
| Simple Tools (`simpletools`) | simplequalityoflife | Pflicht | ja | 1 | 3 | 4 | 0 | 0 | 0 | 52 | – | 0/0/1/0 |
| Simple Farming (`simplefarming`) | simplequalityoflife | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 12 | – | 0/0/0/0 |
| Simple Mob Manners (`simplemobmanners`) | simplequalityoflife | Pflicht | ja | 0 | 1 | 0 | 0 | 0 | 0 | 11 | – | 0/0/2/0 |
| Simple Loot (`simpleloot`) | simplequalityoflife | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 7 | – | 0/0/0/0 |
| Simple Questionary (`simplequestionary`) | simplebuilding | optional | ja | 1 | 1 | 1 | 0 | 0 | 0 | 15 | – | 0/0/1/0 |
| Simple Aesthetics (`simpleaesthetics`) | Super-Mod | eigenständig | ja | 0 | 0 | 0 | 0 | 3 | 0 | 2 | – | 0/0/1/0 |
| Simple End Dragon Fight (`simpleenddragonfight`) | simpleend | optional | ja | 0 | 3 | 1 | 0 | 0 | 0 | 15 | – | 0/0/1/0 |
| Simple End (`simpleend`) | simplebuilding | optional | ja | 47 | 49 | 35 | 0 | 0 | 0 | 42 | – | 0/0/3/1 |
| Simple Respawn (`simplerespawn`) | simplebuilding | optional | ja | 0 | 1 | 1 | 1 | 0 | 0 | 19 | – | 0/0/1/0 |
| Simple Beacons (`simplebeacons`) | simplebuilding | optional | ja | 2 | 5 | 6 | 0 | 0 | 0 | 27 | – | 0/0/3/0 |
| Simple Ovens (`simpleovens`) | simplebuilding | Pflicht | ja | 9 | 13 | 9 | 0 | 0 | 0 | 18 | – | 0/0/1/0 |
| Simple Mobheads (`simplemobheads`) | simplebuilding | Pflicht | ja | 194 | 98 | 1 | 99 | 0 | 2 | 45 | ja | 0/1/0/1 |
| Simple Shops (`simpleshops`) | simplebuilding | optional | ja | 5 | 6 | 6 | 0 | 0 | 1 | 76 | – | 0/0/1/0 |
| Simple Wanderers (`simplewanderers`) | simplebuilding | optional | ja | 0 | 2 | 1 | 0 | 0 | 0 | 22 | – | 0/0/1/0 |
| Simple Stats (`simplestats`) | – | eigenständig | ja | 0 | 1 | 1 | 0 | 0 | 0 | 23 | – | 0/0/1/0 |
| Simple Blocks (`simpleblocks`) | simplebuilding | Pflicht | ja | 54 | 55 | 283 | 0 | 0 | 0 | 41 | ja | 0/0/1/0 |
| Simple Crucibles (`simplecrucibles`) | simplebuilding | Pflicht | ja | 11 | 36 | 6 | 0 | 0 | 0 | 71 | ja | 0/0/1/1 |
| Simple Octets (`simpleoctets`) | simplebuilding | Pflicht | ja | 176 | 160 | 521 | 0 | 0 | 0 | 22 | ja | 0/0/1/1 |
| Simple Hammers (`simplehammers`) | simplebuilding | Pflicht | ja | 0 | 8 | 11 | 0 | 0 | 0 | 17 | ja | 0/1/0/0 |
| Simple Optimization (`simpleoptimization`) | Super-Mod | eigenständig | ja | 0 | 0 | 0 | 0 | 3 | 0 | 2 | – | 0/0/1/0 |
| Simple Opt Render (`simpleoptrender`) | simpleoptimization | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 11 | – | 0/0/0/0 |
| Simple Opt Memory (`simpleoptmemory`) | simpleoptimization | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 0 | 3 | – | 0/0/0/0 |
| Simple Opt Server (`simpleoptserver`) | simpleoptimization | Pflicht | ja | 0 | 0 | 0 | 0 | 0 | 1 | 6 | – | 0/0/0/0 |
| Simple Storage (`simplestorage`) | simplebuilding | Pflicht | ja | 37 | 53 | 177 | 0 | 0 | 0 | 120 | – | 0/0/5/2 |
| Simple Music (`simplemusic`) | simplebuilding | optional | ja | 18 | 28 | 37 | 0 | 0 | 1 | 37 | – | 0/0/3/0 |

## Je Modul (generiert von `tools/audit/module_completeness.py --md`)

### SimpleBuilding (`simplebuilding`)

Kern- und Super-Mod: Baustäbe, Blaupause, Oktant, Pads, Enderit, Kerne, Rucksack-/Maschinen-Rest, Verzauberungen und Handbücher; bündelt die Pflicht-Sub-Mods (Ovens, Mobheads, Crucibles, Octets, Blocks, Hammers, Storage) und optional weitere.

- Super-Mod: – (eigenständig); enthält: simplebeacons, simpleblocks, simplecrucibles, simpledimensions, simpleend, simplefun, simplehammers, simplemaps, simplemobheads, simplemobs, simplemoney, simplemusic, simpleoctets, simpleovens, simplequestionary, simplerespawn, simpleriding, simplesandwiches, simpleshops, simplestorage, simpletrims, simpletweaks, simplewanderers; Pflicht für: –; Standalone-Target: nein
- Inhalt: 339 Blöcke, 606 Items, 11 Entities, 885 Rezepte, 341 Loot-Tables, 120 Advancements (+755 Rezeptbuch), 282 Config-Labels, 4 Befehle (/killboats, /killcarts, /sbtestcentre, /simplebuilding), 1233 Tests, 48 Wiki-Abschnitte
- JEI: SimplebuildingJeiPlugin.java; Kategorien: CauldronWorldCategory, InWorldCategory, MobDropCategory; Info-Aufrufe: 3; REI: SimplebuildingReiCommonPlugin.java, SimplebuildingReiClientPlugin.java; EMI: –
- **P2 ITEM-NO-ORIGIN** (1): items without recipe, loot, other data source or mention in Java: `music_disc_voidline_track_3`
- **P2 JEI-RECIPE-TYPE-NO-CATEGORY** (1): custom recipe types not named in any JEI plugin code (module or core) - recipes invisible in JEI [b-jei24]: `simplebuilding:fletching (16 recipes)`
- **P3 CONFIG-NO-TOOLTIP** (39): config labels without tooltip key [b-cmdcfg24]: `commands.simplebuilding.config.client_side`, `commands.simplebuilding.config.get`, `commands.simplebuilding.config.invalid`, `commands.simplebuilding.config.list`, `commands.simplebuilding.config.on_reload`, `commands.simplebuilding.config.recipes_on_reload`, `commands.simplebuilding.config.reset`, `commands.simplebuilding.config.restart`, `commands.simplebuilding.config.set`, `commands.simplebuilding.config.unknown`, `text.autoconfig.simplebuilding.option.server`, `text.autoconfig.simplebuilding.option.server.arrows` … (+27)
- **P3 ITEM-NO-ORIGIN-ALONE** (12): items whose only recipe/loot needs a partner mod and that no module Java names (standalone rule 1 - fine if the item is only registered with the partner): `ashwood_cauldron`, `ashwood_sheet`, `carved_ashwood_log`, `carved_duskwood_log`, `duskwood_cauldron`, `duskwood_sheet`, `hollow_ashwood_log`, `hollow_duskwood_log`, `stripped_ashwood_cauldron`, `stripped_ashwood_sheet`, `stripped_duskwood_cauldron`, `stripped_duskwood_sheet`
- **P3 ITEM-ORIGIN-CODE-ONLY** (14): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `blazing_obsidian_chip`, `crafted_arrow`, `creative_blueprint`, `creative_building_wand`, `creative_spacer`, `crying_obsidian_chip`, `enderite_furnace_minecart`, `music_disc_brimstone_b_side`, `music_disc_daybreak_b_side`, `music_disc_driftwood_b_side`, `music_disc_voidline_b_side`, `netherite_furnace_minecart` … (+2)
- **P3 JEI-RECIPE-TYPE-VANILLA-CLASS** (1): custom serializer on a Vanilla crafting/smithing recipe class, no JEI code names it - check that JEI draws it (special/custom recipes stay hidden) [b-jei24]: `simplebuilding:enchanted_shapeless (2 recipes)`
- **P3 TAG-NO-MINEABLE** (71): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `astral_redstone`, `astral_redstone_torch`, `astralit_end_rod`, `astralit_lamp`, `black_copper_light_pane`, `black_hammock`, `black_light_pane`, `blue_copper_light_pane`, `blue_hammock`, `blue_light_pane`, `bridge_redstone`, `brown_copper_light_pane` … (+59)
- **P4 LANG-NAME-WITHOUT-MODEL** (111): name keys without a 26.3 item definition/blockstate: ids moved to sub-mods on 26.3 only; 26.2/1.21.11 still register them under simplebuilding - keep: `aqua_froglight`, `azure_froglight`, `backpack`, `basic_upgrade_template`, `blaze_head`, `blaze_wall_head`, `bogged_skull`, `bogged_wall_skull`, `breeze_head`, `breeze_wall_head`, `cave_spider_head`, `cave_spider_wall_head` … (+99)
- **P4 LOOT-MISSING-EXPECTED** (13): no loot table, name suggests dropsLike/technical block: `astral_redstone_wall_torch`, `copper_fire`, `enderite_piston_head`, `netherite_piston_head`, `nihil_redstone_wall_torch`, `placed_blueprint`, `placed_egg`, `placed_paint_box`, `placed_small_parts`, `placed_smithing_template`, `reinforced_piston_head`, `sulfur_fire` … (+1)
- **P4 LOOT-MISSING-GETDROPS** (5): no loot table, but the module overrides Block#getDrops (check the block class): `checker_octet`, `chess_pieces`, `goat_horn_holder`, `hammock_rope`, `standing_rod`

### Wiring Example (`wiringexample`)

Minimales Testmodul, prüft nur die Loader- und Integrationsverdrahtung (nicht für Spieler).

- Super-Mod: – (eigenständig); enthält: –; Pflicht für: –; Standalone-Target: nein
- Inhalt: 0 Blöcke, 1 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 0 Tests, 1 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P2 TAB-NO-HOOK** (1): items/blocks but no creative tab code found in module Java
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `token`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Money (`simplemoney`)

Währung (Geldscheine), gewichtete Dorfbewohner-Handel und Schatzbeute; Preislisten für alle Simple-Mods, nur aktiv, wenn der jeweilige Mod geladen ist.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 8 Items, 0 Entities, 9 Rezepte, 0 Loot-Tables, 0 Advancements (+9 Rezeptbuch), 11 Config-Labels, 0 Befehle (–), 23 Tests, 73 Wiki-Abschnitte
- JEI: MoneyLinksJei.java; Kategorien: –; Info-Aufrufe: 2; REI: –; EMI: –
- **P1 JEI-PLUGIN-NOT-REGISTERED** (1): JEI plugin class exists but fabric.mod.json has no jei_mod_plugin entrypoint for it (check: loader-specific plugin may live in fabric/): `MoneyLinksJei.java`
- **P3 CONFIG-NO-TOOLTIP** (1): config labels without tooltip key [b-cmdcfg24]: `text.autoconfig.simplemoney.option.trades`

### Simple Riding (`simpleriding`)

Reiten: Sattel-Tempo, Pferdesprünge, Nautilus-Sprint, Hufeisen und Reittier-Rüstungen mit Beute und Handel; Enderit-Hufeisen nur mit SimpleBuilding.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 8 Items, 0 Entities, 8 Rezepte, 0 Loot-Tables, 1 Advancements (+8 Rezeptbuch), 31 Config-Labels, 0 Befehle (–), 44 Tests, 35 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 CONFIG-NO-TOOLTIP** (6): config labels without tooltip key [b-cmdcfg24]: `text.autoconfig.simpleriding.option.enchantments`, `text.autoconfig.simpleriding.option.enchantments.horseJump`, `text.autoconfig.simpleriding.option.enchantments.swiftRide`, `text.autoconfig.simpleriding.option.horseshoes`, `text.autoconfig.simpleriding.option.safety`, `text.autoconfig.simpleriding.option.worldGen`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Models (`simplemodels`)

Vom Server freigegebene eigene Item-Modelle, Zuweisung am Amboss und ein durchsuchbarer Modell-Browser.

- Super-Mod: simpleaesthetics (required); enthält: –; Pflicht für: simpleaesthetics; Standalone-Target: ja
- Inhalt: 0 Blöcke, 1 Items, 0 Entities, 1 Rezepte, 0 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 7 Config-Labels, 0 Befehle (–), 21 Tests, 14 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Fun (`simplefun`)

Begrenzte Spaß-Mechaniken: werfbare Ziegel, Yeet, kosmetische Verwandlungen (die Tierköpfe sind seit 1.4.0 in Simple Mobheads).

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 2 Items, 0 Entities, 2 Rezepte, 0 Loot-Tables, 1 Advancements (+2 Rezeptbuch), 21 Config-Labels, 1 Befehle (/simplefun), 30 Tests, 38 Wiki-Abschnitte
- JEI: FunJeiPlugin.java; Kategorien: –; Info-Aufrufe: 1; REI: –; EMI: –
- **P3 CONFIG-NO-TOOLTIP** (1): config labels without tooltip key [b-cmdcfg24]: `text.autoconfig.simplefun.option.fun`

### Simple Visuals (`simplevisuals`)

Clientseitig: begrenztes HUD, Tooltips, Amboss-Formatierung und Vanilla-Immersionspartikel.

- Super-Mod: simpleaesthetics (required); enthält: –; Pflicht für: simpleaesthetics; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 51 Config-Labels, 1 Befehle (/simplevisuals), 22 Tests, 78 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Sounds (`simplesounds`)

Clientseitig: lokale Vanilla-Klänge passend zu den zwölf Immersionseffekten von Simple Visuals.

- Super-Mod: simpleaesthetics (required); enthält: –; Pflicht für: simpleaesthetics; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 19 Tests, 20 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Quality of Life (`simplequalityoflife`)

Super-Mod ohne eigene Spielinhalte: bündelt Weather, Interfaces, Loot, Mob Manners, Farming, Tools und Movement mit je einem Schalter; Handbuch und Startquest.

- Super-Mod: – (eigenständig); enthält: simplefarming, simpleinterfaces, simpleloot, simplemobmanners, simplemovement, simpletools, simpleweather; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 1 Items, 0 Entities, 1 Rezepte, 0 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 7 Config-Labels, 1 Befehle (/simplequalityoflife), 6 Tests, 9 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Tweaks (`simpletweaks`)

Server-Claims (Urkunde, Claim-Tisch, Vertrauenslisten, standardmäßig an) und Legacy-Kompatibilität alter SimpleBuilding-Tweaks.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 1 Blöcke, 3 Items, 0 Entities, 3 Rezepte, 1 Loot-Tables, 0 Advancements (+2 Rezeptbuch), 0 Config-Labels, 18 Befehle (/afk, /backup, /claim, /coords, /endersee, /freeze, /inspect, /invsee, /lag, /maintenance, /phantoms, /playtime, /pregen, /pvp, /seen, /tweaks, /unfreeze, /vanish), 164 Tests, 41 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P2 JEI-RECIPE-TYPE-NO-CATEGORY** (1): custom recipe types not named in any JEI plugin code (module or core) - recipes invisible in JEI [b-jei24]: `simpletweaks:claim_deed_copy (1 recipes)`
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `claim_table`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Dimensions (`simpledimensions`, ns `simpledimension`)

Konfigurierbare eigene Dimensionen, Lichtbögen und sichere verknüpfte Portale.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 2 Blöcke, 2 Items, 0 Entities, 1 Rezepte, 0 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 54 Config-Labels, 0 Befehle (–), 45 Tests, 7 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 CONFIG-NO-TOOLTIP** (6): config labels without tooltip key [b-cmdcfg24]: `simpledimension.config.access`, `simpledimension.config.advanced`, `simpledimension.config.dimensions`, `simpledimension.config.remote`, `simpledimension.config.remoteNotice`, `simpledimension.config.safety`
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `light_blue_portal`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P4 LOOT-MISSING-EXPECTED** (2): no loot table, name suggests dropsLike/technical block: `light_blue_portal`, `sky_portal`

### Simple Sandwiches (`simplesandwiches`)

Sandwiches auf dem Schneidebrett, Küchenmesser, Käse und Butter aus dem Kessel, Essen direkt aus Bündeln.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 19 Blöcke, 23 Items, 0 Entities, 17 Rezepte, 19 Loot-Tables, 0 Advancements (+17 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 38 Tests, 6 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-NO-ORIGIN-ALONE** (2): items whose only recipe/loot needs a partner mod and that no module Java names (standalone rule 1 - fine if the item is only registered with the partner): `ashwood_cutting_board`, `duskwood_cutting_board`
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `sandwich`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P3 TAG-NO-MINEABLE** (3): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `butter_block`, `butter_carpet`, `cheese_block`

### SimpleLib (`simplelib`)

Gebündelte Bibliothek (Jar-in-Jar): Warm-Essen, gemeinsame Grundstoffe (Enderit, Rissiger Diamant …), IdMoves, In-World-Aufwertungen, UI-Bausteine, legendäre Momente; kein eigenständiger Spielinhalt.

- Super-Mod: – (eigenständig); enthält: –; Pflicht für: simplebeacons, simpleblocks, simplebuilding, simplecrucibles, simplehammers, simpleinterfaces, simplemaps, simplemobheads, simplemobs, simplemusic, simpleoctets, simpleovens, simplequestionary, simpleshops, simplestorage, simpletrims, simpletweaks; Standalone-Target: nein
- Inhalt: 7 Blöcke, 17 Items, 0 Entities, 29 Rezepte, 7 Loot-Tables, 0 Advancements (+26 Rezeptbuch), 5 Config-Labels, 0 Befehle (–), 36 Tests, 4 Wiki-Abschnitte
- JEI: SimpleLibJeiPlugin.java; Kategorien: InWorldUpgradeCategory; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 TAG-NO-MINEABLE** (1): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `warped_wart`

### Simple Interfaces (`simpleinterfaces`)

Sub-Mod von QoL: Vanilla-Container-Bildschirme neu gestaltet (Farbe je Block), verknüpfte Container, Easy Shulkers/Endertruhen; allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 36 Tests, 43 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Maps (`simplemaps`)

Wegfinder-Karten: endlose Karte je Dimension mit Zoom, Wegpunkte in der Locator-Bar, gerahmte Ansichten, Kartentisch-Aufwertungen, Webkarten-Export.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 5 Items, 0 Entities, 7 Rezepte, 0 Loot-Tables, 0 Advancements (+7 Rezeptbuch), 0 Config-Labels, 1 Befehle (/simplemaps), 54 Tests, 9 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Trims (`simpletrims`)

Rüstungsbesatz Leuchtend/Strahlend/Pulsierend/Invertierend, platzierbare Besatzvorlagen und Aufwertungen (aus SimpleBuilding verschoben, IDs migrieren); Axt allein, Vorschlaghammer mit Simple Hammers.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 1 Blöcke, 5 Items, 0 Entities, 10 Rezepte, 0 Loot-Tables, 3 Advancements (+10 Rezeptbuch), 1 Config-Labels, 0 Befehle (–), 22 Tests, 6 Wiki-Abschnitte
- JEI: TrimsJeiPlugin.java; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P2 SPLIT-RECIPE-KIND-LOST** (3): recipe kinds (type) the old id had and the new one lacks: `emitting_trim_template: smithing_transform`, `glowing_trim_template: smithing_transform`, `pulsating_trim_template: crafting_shapeless, smithing_transform`
- **P3 JEI-RECIPE-TYPE-VANILLA-CLASS** (1): custom serializer on a Vanilla crafting/smithing recipe class, no JEI code names it - check that JEI draws it (special/custom recipes stay hidden) [b-jei24]: `simpletrims:tool_shapeless (1 recipes)`
- **P4 LOOT-MISSING-EXPECTED** (1): no loot table, name suggests dropsLike/technical block: `placed_template`

### Simple Mobs (`simplemobs`)

Neue Mobs und Bosse: Hollow Howler/Watcher, Shellker, Seelenfeuer-Lohe, Schreckensritter, Täuscher, Verdorbener/Abgrund-Wither mit Netherherz und Rotem Nethergem, Trank der Stille.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 2 Blöcke, 25 Items, 12 Entities, 66 Rezepte, 10 Loot-Tables, 0 Advancements (+10 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 109 Tests, 13 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-ORIGIN-CODE-ONLY** (2): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `end_wings`, `trick_mirror`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P3 TAG-NO-MINEABLE** (2): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `nether_gem`, `nether_heart`

### Simple Weather (`simpleweather`)

Sub-Mod von QoL: Wetter-Schalter (Server) und Regendichte (Client); allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 5 Tests, 3 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Movement (`simplemovement`)

Sub-Mod von QoL: Kriechen per Taste, vom Server erlaubtes Auto-Laufen, schnellere Leitern, Frostläufer auf Pulverschnee; allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 1 Befehle (/crawl), 18 Tests, 17 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Tools (`simpletools`)

Sub-Mod von QoL: Bonus bei voller Haltbarkeit, Sparsamkeit und Seelenbindung, Todestresor, Biom-Pinsel, Amboss-Reparatur ohne Aufpreis, Lavaeimer in Öfen; allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 1 Blöcke, 3 Items, 0 Entities, 4 Rezepte, 1 Loot-Tables, 0 Advancements (+4 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 52 Tests, 32 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Farming (`simplefarming`)

Sub-Mod von QoL: Federfall schützt Ackerland, Schärfe mäht Gras, die Hacke erntet und pflanzt reife Feldfrüchte neu; allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 12 Tests, 8 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Mob Manners (`simplemobmanners`)

Sub-Mod von QoL: Mobs nach Typ oder Namensschild stumm schalten, benannte Babys bleiben klein, Piglins ignorieren Gold-Besatz und -Werkzeuge; allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 0 Blöcke, 1 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 11 Tests, 11 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `frog_bucket`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Loot (`simpleloot`)

Sub-Mod von QoL: Tresor-Abklingzeit in Tagen statt einmal je Spieler; allein spielbar.

- Super-Mod: simplequalityoflife (required); enthält: –; Pflicht für: simplequalityoflife; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 7 Tests, 5 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Questionary (`simplequestionary`)

Fragentisch: ein Item hineinlegen und in einem offenen Buch seine Geschichte, seinen Nutzen und ein Geheimnis lesen.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 1 Blöcke, 1 Items, 0 Entities, 1 Rezepte, 1 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 15 Tests, 2 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Aesthetics (`simpleaesthetics`)

Super-Mod ohne eigene Inhalte: bündelt Simple Visuals, Simple Sounds und Simple Models mit je einem Schalter.

- Super-Mod: – (eigenständig); enthält: simplemodels, simplesounds, simplevisuals; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 3 Config-Labels, 0 Befehle (–), 2 Tests, 1 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 CONFIG-NO-TOOLTIP** (3): config labels without tooltip key [b-cmdcfg24]: `text.autoconfig.simpleaesthetics.option.enableSimpleModels`, `text.autoconfig.simpleaesthetics.option.enableSimpleSounds`, `text.autoconfig.simpleaesthetics.option.enableSimpleVisuals`

### Simple End Dragon Fight (`simpleenddragonfight`)

Sub-Mod von Simple End: Enderdrache mit +1 Lebensleiste je Spieler, schneller, Auftritts-Animationen und Drachenmilben ab halber Gesundheit.

- Super-Mod: simpleend (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 3 Items, 1 Entities, 1 Rezepte, 1 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 15 Tests, 6 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple End (`simpleend`)

End-Holz Aschholz/Dämmerholz (volles Holzset, Knospen, Bäume, Gestrüpp), End-Gras, Biome Aschental und Morgenland auf den äußeren Inseln; bündelt Simple End Dragon Fight.

- Super-Mod: simplebuilding (optional); enthält: simpleenddragonfight; Pflicht für: –; Standalone-Target: ja
- Inhalt: 47 Blöcke, 49 Items, 5 Entities, 35 Rezepte, 44 Loot-Tables, 0 Advancements (+35 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 42 Tests, 8 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `burrowed_end_stone`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P3 TAG-NO-MINEABLE** (6): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `ash_bud`, `dusk_bud`, `end_bush`, `end_grass`, `potted_ash_bud`, `potted_dusk_bud`
- **P4 LOOT-MISSING-EXPECTED** (4): no loot table, name suggests dropsLike/technical block: `ashwood_wall_hanging_sign`, `ashwood_wall_sign`, `duskwood_wall_hanging_sign`, `duskwood_wall_sign`

### Simple Respawn (`simplerespawn`)

Niedergeschlagen statt tot: Mitspieler können innerhalb der Ausblutzeit wiederbeleben; eigener Bildschirm mit Zuschauen, Inventar-Ansicht und Aufgeben.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 1 Items, 0 Entities, 1 Rezepte, 0 Loot-Tables, 1 Advancements (+1 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 19 Tests, 6 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Beacons (`simplebeacons`)

Starkes Leuchtfeuer (6 Pyramidenstufen, 10 weitere Effekte, Reichweite nach Material), Starke Aquisphäre und die Starke Aufwertungsvorlage; Axt allein, Vorschlaghammer mit Hammer-Mod.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 2 Blöcke, 5 Items, 0 Entities, 6 Rezepte, 2 Loot-Tables, 0 Advancements (+6 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 27 Tests, 8 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-ORIGIN-CODE-ONLY** (1): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `strong_upgrade_shard`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P3 TAG-NO-MINEABLE** (2): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `strong_beacon`, `strong_conduit`

### Simple Ovens (`simpleovens`)

Pflicht-Sub-Mod von SB: Verstärkte/Netherit-/Enderit-Öfen, -Räucheröfen, -Schmelzöfen (2×/4×/8×) und Antriebsloren (IDs aus SB migrieren); Axt allein, Vorschlaghammer mit Simple Hammers.

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 9 Blöcke, 13 Items, 3 Entities, 9 Rezepte, 9 Loot-Tables, 0 Advancements (+9 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 18 Tests, 7 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Mobheads (`simplemobheads`)

Pflicht-Sub-Mod von SB: ein Kopf für jeden Mob (Drop durch geladenen Creeper), tragbar mit kleinem Geheimnis, platzierbar, als Notenblock-Klang; Köpfe aus Simple Fun und SB sind hierher gezogen.

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 194 Blöcke, 98 Items, 1 Entities, 1 Rezepte, 97 Loot-Tables, 99 Advancements (+1 Rezeptbuch), 0 Config-Labels, 2 Befehle (/simplefun, /simplemobheads), 45 Tests, 15 Wiki-Abschnitte
- JEI: MobHeadsJeiPlugin.java; Kategorien: –; Info-Aufrufe: 1; REI: –; EMI: –
- **P2 SPLIT-LANG-EXTRA-LOST** (4): extra lang keys (tooltip/desc/info) of the old id missing under the new namespace: `blaze_head: jei (1 old keys, e.g. jei.simplebuilding.info.blaze_head; still in old namespace)`, `drowned_head: jei (1 old keys, e.g. jei.simplebuilding.info.drowned_head; still in old namespace)`, `enderman_head: jei (1 old keys, e.g. jei.simplebuilding.info.enderman_head; still in old namespace)`, `shulker_head: jei (1 old keys, e.g. jei.simplebuilding.info.shulker_head; still in old namespace)`
- **P4 LOOT-MISSING-EXPECTED** (97): no loot table, name suggests dropsLike/technical block: `abyssal_wither_wall_head`, `allay_wall_head`, `armadillo_wall_head`, `axolotl_wall_head`, `bat_wall_head`, `bee_wall_head`, `blaze_wall_head`, `blighted_wither_wall_head`, `bogged_wall_skull`, `breeze_wall_head`, `camel_husk_wall_head`, `camel_wall_head` … (+85)

### Simple Shops (`simpleshops`)

Spieler-Läden: Verkaufsblock mit Preis aus bis zu fünf exakten Preis-Items und Vitrine mit bis zu fünf Waren, geschützt vor Fremden, Explosionen, Kolben und Trichtern.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 5 Blöcke, 6 Items, 0 Entities, 6 Rezepte, 5 Loot-Tables, 0 Advancements (+6 Rezeptbuch), 0 Config-Labels, 1 Befehle (/servershop), 76 Tests, 11 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Wanderers (`simplewanderers`)

Dorf-Wanderer: ein Fahrender Händler je Dorf mit angeleintem Pferd, eigenem Sortiment und Nachschub je nach Dorfbiom; allein mit Vanilla spielbar.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 2 Items, 2 Entities, 1 Rezepte, 0 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 22 Tests, 6 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Stats (`simplestats`)

Jedes Werkzeug, jede Waffe und jedes Rüstungsteil führt eigene Statistiken (Blöcke, Kills nach Mob, absorbierter Schaden, Strecken, Fänge) mit eigenem Statistik-Bildschirm.

- Super-Mod: – (eigenständig); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 1 Items, 0 Entities, 1 Rezepte, 0 Loot-Tables, 0 Advancements (+1 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 23 Tests, 10 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]

### Simple Blocks (`simpleblocks`)

Pflicht-Sub-Mod von SB: fehlende Baublöcke (Gedämpfte Tiefenschiefer-Fliesen, Sculk-Tiefenschiefer, fehlende Vanilla-Treppen/-Stufen/-Mauern, rissige/bemooste Ziegel) und SBs heimatlose Baublöcke; die Formsäge schneidet Stein und Holz in jede Form.

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 54 Blöcke, 55 Items, 1 Entities, 283 Rezepte, 54 Loot-Tables, 0 Advancements (+82 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 41 Tests, 9 Wiki-Abschnitte
- JEI: BlocksJeiPlugin.java; Kategorien: SawCategory; Info-Aufrufe: 1; REI: –; EMI: –
- **P3 TAG-NO-MINEABLE** (3): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `aqua_froglight`, `azure_froglight`, `scarlet_froglight`

### Simple Crucibles (`simplecrucibles`)

Pflicht-Sub-Mod von SB: Eisen-/Verstärkter/Netherit-/Enderit-Tiegel und -Fässer (Kochstation ohne Brennstoff), Verstärkter Kessel, Feldküchen im Dorf, Eimer-Familie und Seelenlava (IDs migrieren).

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 11 Blöcke, 36 Items, 0 Entities, 6 Rezepte, 11 Loot-Tables, 0 Advancements (+6 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 71 Tests, 7 Wiki-Abschnitte
- JEI: CruciblesJeiPlugin.java; Kategorien: CrucibleCategory; Info-Aufrufe: 1; REI: –; EMI: –
- **P3 ITEM-ORIGIN-CODE-ONLY** (22): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `brittle_ceramic_bucket`, `brittle_ceramic_lava_bucket`, `brittle_ceramic_water_bucket`, `ceramic_lava_bucket`, `ceramic_water_bucket`, `chipped_ceramic_bucket`, `chipped_ceramic_lava_bucket`, `chipped_ceramic_water_bucket`, `copper_lava_bucket`, `copper_water_bucket`, `cracked_ceramic_bucket`, `cracked_ceramic_lava_bucket` … (+10)
- **P4 LOOT-MISSING-EXPECTED** (1): no loot table, name suggests dropsLike/technical block: `soul_lava`

### Simple Octets (`simpleoctets`)

Pflicht-Sub-Mod von SB: Achtelblöcke (0,5³) aus jedem Holz und jedem Block mit Treppe/Stufe, mischbar in einer Zelle; Steinsäge schneidet, Werkbank setzt zurück, Vorschlaghammer schnitzt.

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 176 Blöcke, 160 Items, 0 Entities, 521 Rezepte, 0 Loot-Tables, 0 Advancements (+347 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 22 Tests, 6 Wiki-Abschnitte
- JEI: OctetsJeiPlugin.java; Kategorien: –; Info-Aufrufe: 2; REI: –; EMI: –
- **P3 TAG-NO-MINEABLE** (17): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `black_wool_octet`, `blue_wool_octet`, `brown_wool_octet`, `cyan_wool_octet`, `gray_wool_octet`, `green_wool_octet`, `light_blue_wool_octet`, `light_gray_wool_octet`, `lime_wool_octet`, `magenta_wool_octet`, `mixed_octet`, `orange_wool_octet` … (+5)
- **P4 LOOT-MISSING-GETDROPS** (176): no loot table, but the module overrides Block#getDrops (check the block class): `acacia_log_octet`, `acacia_octet`, `andesite_octet`, `ashwood_log_octet`, `ashwood_octet`, `astralit_bricks_octet`, `bamboo_mosaic_octet`, `bamboo_octet`, `birch_log_octet`, `birch_octet`, `black_concrete_octet`, `black_terracotta_octet` … (+164)

### Simple Hammers (`simplehammers`)

Pflicht-Sub-Mod von SB: sieben Vorschlaghämmer (Stein bis Enderit) für 3×3-Abbau, Umformen in Treppen/Stufen, Achtel schnitzen, Zerschlagen und schrittweise In-World-Aufwertungen; Verzauberungen Durchbruch, Überschreiben, Radius.

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 0 Blöcke, 8 Items, 0 Entities, 11 Rezepte, 0 Loot-Tables, 0 Advancements (+11 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 17 Tests, 10 Wiki-Abschnitte
- JEI: HammersJeiPlugin.java; Kategorien: –; Info-Aufrufe: 1; REI: –; EMI: –
- **P2 SPLIT-JEI-MENTION-LOST** (1): named in old JEI/REI code, in no JEI/REI code now (in-world/info pages?) [b-jei24]: `netherite_sledgehammer`

### Simple Optimization (`simpleoptimization`)

Super-Mod ohne eigene Inhalte (nur Fabric 26.3): bündelt Opt Render, Opt Memory und Opt Server mit je einem Schalter.

- Super-Mod: – (eigenständig); enthält: simpleoptmemory, simpleoptrender, simpleoptserver; Pflicht für: –; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 3 Config-Labels, 0 Befehle (–), 2 Tests, 1 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 CONFIG-NO-TOOLTIP** (3): config labels without tooltip key [b-cmdcfg24]: `text.autoconfig.simpleoptimization.option.enableSimpleOptMemory`, `text.autoconfig.simpleoptimization.option.enableSimpleOptRender`, `text.autoconfig.simpleoptimization.option.enableSimpleOptServer`

### Simple Opt Render (`simpleoptrender`)

Client-Optimierungen (Laub-Culling, Lightmap-Cache, schneller IP-Ping, Telemetrie aus); tritt zurück, wenn ein bekannter Optimierungsmod den Bereich abdeckt.

- Super-Mod: simpleoptimization (required); enthält: –; Pflicht für: simpleoptimization; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 11 Tests, 7 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Opt Memory (`simpleoptmemory`)

Speicher-Optimierungen (Blockzustände, Modelle); Stufe 1 enthält nur Schalter, Config und Mod-Erkennung.

- Super-Mod: simpleoptimization (required); enthält: –; Pflicht für: simpleoptimization; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 3 Tests, 2 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Opt Server (`simpleoptserver`)

Server-Optimierung: Sichtweite folgt der Serverlast (MSPT) in den Grenzen der server.properties; tritt vor ServerCore zurück.

- Super-Mod: simpleoptimization (required); enthält: –; Pflicht für: simpleoptimization; Standalone-Target: ja
- Inhalt: 0 Blöcke, 0 Items, 0 Entities, 0 Rezepte, 0 Loot-Tables, 0 Advancements (+0 Rezeptbuch), 0 Config-Labels, 1 Befehle (/simpleopt), 6 Tests, 2 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- Funde: keine

### Simple Storage (`simplestorage`)

Pflicht-Sub-Mod von SB: Kisten aller Hölzer, Lager-Werkbank, gestufte/Fallen-Truhen, Shulkerkisten-Stufen, Truhenloren/-boote, Bündel, Köcher, Rucksäcke und die Lager-Verzauberungen (IDs migrieren).

- Super-Mod: simplebuilding (required); enthält: –; Pflicht für: simplebuilding; Standalone-Target: ja
- Inhalt: 37 Blöcke, 53 Items, 6 Entities, 177 Rezepte, 36 Loot-Tables, 0 Advancements (+177 Rezeptbuch), 0 Config-Labels, 0 Befehle (–), 120 Tests, 12 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-NO-ORIGIN-ALONE** (2): items whose only recipe/loot needs a partner mod and that no module Java names (standalone rule 1 - fine if the item is only registered with the partner): `ashwood_crate`, `duskwood_crate`
- **P3 ITEM-ORIGIN-CODE-ONLY** (3): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `enderite_shulker_shell`, `netherite_shulker_shell`, `reinforced_shulker_shell`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P3 JEI-RECIPE-TYPE-VANILLA-CLASS** (1): custom serializer on a Vanilla crafting/smithing recipe class, no JEI code names it - check that JEI draws it (special/custom recipes stay hidden) [b-jei24]: `simplestorage:backpack_upgrade (2 recipes)`
- **P3 TAG-NO-MINEABLE** (4): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `backpack`, `enderite_backpack`, `netherite_backpack`, `reinforced_backpack`
- **P4 LOOT-MISSING-EXPECTED** (1): no loot table, name suggests dropsLike/technical block: `placed_bundle`
- **P4 SPLIT-LANG-LEFTOVER** (7): old tooltip/JEI/Jade keys of the moved id stay in the old namespace and no Java uses them (cleanup; the new owner covers it otherwise, e.g. own JEI info pages): `enderite_bundle: tooltip (1 old keys, e.g. tooltip.simplebuilding.enderite_bundle; still in old namespace)`, `enderite_quiver: tooltip (1 old keys, e.g. tooltip.simplebuilding.enderite_quiver; still in old namespace)`, `netherite_bundle: tooltip (1 old keys, e.g. tooltip.simplebuilding.netherite_bundle; still in old namespace)`, `netherite_quiver: tooltip (1 old keys, e.g. tooltip.simplebuilding.netherite_quiver; still in old namespace)`, `quiver: tooltip (1 old keys, e.g. tooltip.simplebuilding.quiver; still in old namespace)`, `reinforced_bundle: tooltip (1 old keys, e.g. tooltip.simplebuilding.reinforced_bundle; still in old namespace)`, `reinforced_quiver: tooltip (1 old keys, e.g. tooltip.simplebuilding.reinforced_quiver; still in old namespace)`

### Simple Music (`simplemusic`)

Notenblätter mit Text-Notencode (Editor, .txt/.nbs-Import), gepresst auf Rohlinge in der Plattenpresse, hochgeladene Ogg-Platten (Kontingent, OP-Freigabe), Taschen-Jukebox; braucht SimpleLib.

- Super-Mod: simplebuilding (optional); enthält: –; Pflicht für: –; Standalone-Target: ja
- Inhalt: 18 Blöcke, 28 Items, 0 Entities, 37 Rezepte, 18 Loot-Tables, 0 Advancements (+37 Rezeptbuch), 0 Config-Labels, 1 Befehle (/music), 37 Tests, 15 Wiki-Abschnitte
- JEI: kein Plugin; Kategorien: –; Info-Aufrufe: 0; REI: –; EMI: –
- **P3 ITEM-ORIGIN-CODE-ONLY** (7): no recipe/loot/data source; the id is named in Java (loot injection, conversion, drop or creative-only - check): `custom_disc`, `music_disc_deep_pulse`, `music_disc_ember_forge`, `music_disc_end_departure`, `music_disc_fortress_march`, `music_disc_obsidian_throne`, `music_disc_sky_forge`
- **P3 JEI-NO-PLUGIN** (1): module has items/blocks but no own JEI plugin (no info pages / categories standalone) [b-jei24]
- **P3 TAG-NO-MINEABLE** (17): blocks in no mineable/* tag (fine for instant-break blocks - check hardness): `black_pocket_jukebox`, `blue_pocket_jukebox`, `brown_pocket_jukebox`, `cyan_pocket_jukebox`, `gray_pocket_jukebox`, `green_pocket_jukebox`, `light_blue_pocket_jukebox`, `light_gray_pocket_jukebox`, `lime_pocket_jukebox`, `magenta_pocket_jukebox`, `orange_pocket_jukebox`, `pink_pocket_jukebox` … (+5)
