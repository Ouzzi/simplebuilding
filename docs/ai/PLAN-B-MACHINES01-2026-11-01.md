# Plan MACH32-MOD (Sitzung B, b-machines01, 01.11.) – Sub-Mod Simple Machines

Queue: „MACH32-MOD: alle Blöcke aus dem Tab Simple Machines (Kolben-Upgrades, Doppelkolben, Dämpfer …) ins Sub-Mod Simple Machines.“
Besitzer-Anmerkung (b-antworten-20261101c): „alle die in simple machines tab liegen in simple machines submod legen“.
Basis: `hub/b-bundle89` (Gate läuft, Bündel-Branch unangetastet). Vorbilder: IG31-13 (Simple Ores), Stufe 4 (Simple Ovens), WP-17 (altes Modul `simpleredstone`).

## 1. Ist-Stand

Der Kreativ-Tab „SimpleMachines“ ist SBs `functional`-Tab (`ModItemGroupsContent#functionalRows`, Lang `itemgroup.simplebuilding.functional`).
Inhalt heute, nach Besitzer des Codes:

| Gruppe | Zeilen im Tab | Code/Ids | Stand |
|---|---|---|---|
| Doppelkolben, Signaldämpfer | `double_pistons` | SB-Kern `com.simplebuilding.machines.*`, `simplebuilding:double_piston(_head,_arm)`, `signal_dampener` (seit IG31-12, vorher Modul `simpleredstone`) | **Stufe 1, diese Branch** |
| Öfen, Räucher-, Hochöfen, Ofenloren | `hoppers_and_furnaces`, `smokers_and_blast_furnaces`, `rail_vehicles` | `simpleovens:*` | schon Sub-Mod (nur Tab-Zeile per Id) |
| Bündel, Köcher, Rucksäcke, Farbkästen-Zeile, Truhen, Shulker, Truhenloren/-boote | `bundles_and_quivers`, `backpacks`, `chests`, `shulker_boxes`, `trapped_*`, `chest_vehicles` | `simplestorage:*` (Farbkästen `simplebuilding:*_paint_box`) | Lager-Sub-Mod, Tab-Zeilen per Id |
| Eimer, Tiegel, Fässer | `buckets`, `enderite_milk`, `ceramic_buckets` | `simplecrucibles:*` | schon Sub-Mod |
| Trichter (Verstärkt/Netherit/Enderit) + Trichterloren | `hoppers_and_furnaces`, `rail_vehicles` | SB-Kern (`ModHopperBlockEntity`, `NetheriteHopperScreenHandler`, `VehicleTiers`, 198 Referenzen) | offen |
| Kolben-Upgrades (Verstärkt/Netherit/Enderit, Sticky) | `pistons` | SB-Kern (`ReinforcedPistonBlock`, `NetheriteBreakerPistonBlock`, `ModPistonHeadBlock`, Mixins `Piston*Mixin`, `PistonHandlerMixin`, 380 Referenzen) | offen |
| Auto-Schmied | `auto_smither` | SB-Kern (`AutoSmitherBlock/-BlockEntity/-Menu/-Screen`, Forge-Mixin, 176 Referenzen) | offen |
| End-Signale, Lampen, Kolben, Schienen, Tresore (Astral/Nihil) | `end_signals`, `end_signal_blocks`, `end_rails`, `astral_storage` | SB-Kern (End-Systeme, Kanal-Logik) | offen |
| Astral-Verzauberung, Trainingspuppe, Hängematten, Lautsprecher, breite Türen | `astral_enchanting`, `training_dummy`, `hammocks`, `amplifiers`, `wide_doors` | je SB-Kern | offen, großteils eigene Heimat (Enchanting/Puppen/Hängematten/Musik/Türen) |

## 2. Schnitt (Entscheidung + Begründung)

Ein Rundumschlag über ~900 Referenzen (Kolben-Mixins, Trichter-Block-Entities und -Screens, Auto-Schmied, End-Systeme, 3 Loader) in
einem Lauf mit nur einem Compile-/Testlauf am Ende wäre unverantwortlich: ein Fehler in der Mixin-Kette bricht jede Welt.
Darum **Stufen**, jede für sich lauffähig und testbar (Muster Stufe 4/5/…):

1. **Stufe 1 (dieser Branch):** Modul `modules/simplemachines` (Fabric 26.3, Pflicht-Sub-Mod SB, Namespace `simplemachines`) mit
   dem, was schon sauber trennbar ist: Doppelkolben + Signaldämpfer (1 Kern-Abhängigkeit: `ServerTuning`, durch eigene Config ersetzt).
   Das Modul-Gerüst (Config, Guide, Discover, Tab, Wiki, Tests, modtree, IdMoves, SB requires) steht damit für alle Folgestufen.
2. **Stufe 2:** Kolben-Upgrades (Verstärkt/Netherit/Enderit, Sticky, Breach-Verhalten) + die Piston-Mixins nach `simplemachines`.
3. **Stufe 3:** Trichter + Trichterloren (+ `VehicleTiers`-Anteil).
4. **Stufe 4:** Auto-Schmied (+ Autonomer Crafter/Werkbank mit Lager, falls im selben Tab).
5. **Stufe 5:** End-Signale/Lampen/Kolben/Schienen/Tresore – Besitzer klären, ob in Simple Machines oder in `simpleend` (Heimat „End-Systeme“;
   Rückfrage MACH32-ENDSYS, Vorschlag: in `simpleend`, weil Astral/Nihil-Kanäle End-Logik sind, nur der Tab-Eintrag bleibt SimpleMachines).
6. Rest (Astral-Verzauberung, Puppe, Hängematten, Lautsprecher, Türen) behalten ihre Heimat; ihre Tab-Zeilen bleiben im Tab
   „SimpleMachines“ (der Tab selbst bleibt SB-Tab mit Zeilen per Id, wie bei Ovens/Storage/Crucibles).

## 3. Stufe 1 – Umsetzung

- Neues Modul `modules/simplemachines` (Paket `com.simplemachines`, Fabric 26.3 only wie zuvor `simpleredstone`; NeoForge/Forge bleiben
  kompilierbar, weil SB dort die Teile nie registrierte: `McVersion.MACHINE_PARTS` bleibt nur als Tab-/Test-Schalter).
  Aufgebaut aus dem Gerüst des aufgelösten `simpleredstone` (Config, Guide, Discover, Questionary, FTB-Kapitel, Icon) + dem aktuellen
  Teile-Code (`git mv` aus `common/.../machines`, History bleibt).
- **IdMoves** (im Modul, `SimpleMachines#registerIdMoves`, je alte Id genau eine Kante):

| alt | neu | Registries |
|---|---|---|
| `simplebuilding:` und `simpleredstone:` `double_piston`, `signal_dampener` | `simplemachines:` gleich | ITEM, RECIPE, ADVANCEMENT `recipes/redstone/<pfad>` |
| `simplebuilding:` und `simpleredstone:` `double_piston`, `double_piston_head`, `double_piston_arm`, `signal_dampener` | `simplemachines:` gleich | BLOCK |
| `simpleredstone:double_piston_barrel` | `minecraft:air` | BLOCK |
| `simpleredstone:guide_book` (IG31-12 hatte es auf `simplebuilding:guide_book_machines` gelegt) | `simplemachines:guide_book` (Modul-Handbuch kommt zurück) | ITEM, RECIPE, ADVANCEMENT `recipes/misc/guide_book` |

  Aus `IdMovesHook` (SB, 26.3) entfallen die `simpleredstone:`→`simplebuilding:`-Kanten (sonst doppelte Quelle).
- **Config:** `server.features.doublePiston/signalDampener` (SB) entfallen samt Lang, `RecipeFilter`, Katalog, Tests; sie leben in
  `config/simplemachines-server.json` (`features.*`, `/simplemachines config`), Super-Mod-Schalter `enableSimpleMachines`.
  **Abweichung:** ein ausgeschalteter Schalter nimmt das Rezept nicht mehr mit (SBs `RecipeFilter` gilt nur für `simplebuilding:`);
  die Teile stehen still bzw. geben 0 aus wie bisher. Offener Folgepunkt für ein Modul-Rezeptfilter-API (Framework).
- **SB:** `requires simplemachines` (modules.json, `mc26_3/fabric/build.gradle` include/implementation, fabric.mod.json depends),
  Tab-Zeile `double_pistons` per `MachineRefs` (neu, wie `OvenRefs`), Tooltips schreibt das Modul selbst (`MachinePartItem`),
  Datagen-Tag/`ModBlocks`/`ModItems`/`MachineParts` entfallen, `mc26_3/generated/wiki/items.json` und Pickaxe-Tag bereinigt.
- **Tests:** SB-Katalog `machine_part_game_test_*` + Adapter entfallen; 19 Tests + Discover/Guide-Tests in `MachinesTests`/`ModuleGameTest`
  (`module_game_test_*`), davon neu `old_ids_move` (beide alten Namespaces + Barrel + Guide) und `sub_mod_switch`.
- Wiki: Features `double_piston`/`signal_dampener` aus `wiki/manual.json` ins Modul-Manual (EN+DE), `wiki/generate.py --all`.
- Texturen/Modelle unverändert (Namespace-Umbenennung; Variante-A-Platzhalter bleibt, Besitzer-Wahl MACH32-DAEMPFER offen).
- modtree neu (HTML+MD+`creative_tab_order.json`).

## 4. Risiken

- Zwei alte Namespaces → gleiche neue Id: je alte Id eine Kante (IdMoves wirft bei doppelter Quelle mit anderem Ziel); getestet in `old_ids_move`.
- Item-Reihenfolge im SB-Tab hängt am Laden des Moduls (Pflicht-Sub-Mod, daher immer da); fehlt es, liefert `MachineRefs` Luft und
  `DataIntegrityTests` schlägt an (gewollt).
- Gradle-Lauf: ein Compile für Fabric + Modul + SB, Modul-Gametests (+ standalone), `fabric-263` gefiltert auf `simplebuilding:machine*`,
  `data_integrity*`, `config*`.

## 5. Verifikation

`check_data.py` (Modul), `check_loot_format --strict`, `check_dup_impl`, `check_item_properties`, `check_tooltip_text`, `check_lang_keys`,
`check_load_conditions`, `multimod`, `modtree --check`, `module_guides --check`, `generate_quests --check`, `wiki/generate.py --all --check`,
`check_client_mixins` (keine neuen Mixins); dann ein sbt-Lauf (Compile + Gametests).
