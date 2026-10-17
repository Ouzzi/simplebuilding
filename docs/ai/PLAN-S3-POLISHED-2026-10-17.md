# Plan: Modulfamilie Stufe 3 – polierter Endstein nach simplelib (2026-10-17)

Branch `claude-s3-polished` (von `claude-wave1` 8b097d1bd), Sitzung A. Grundlage: `PLAN-MODULFAMILIE-2026-10-16.md`
(§5 F4/F6: nur polierter Endstein nach simplelib; Astral-/Nihil-Endstein, Astralit/Nihilith bleiben bis Simple Enderite),
`PLAN-MODULAUFTEILUNG-2026-10-11.md` §2.5/Stufe 3d, AGENTS.md Regel 16.

## Ist-Zustand (grep)
- Familie = genau **ein** Block: `simplebuilding:polished_end_stone` (`ModBlocks` 370, `ModItems` 179). Treppe/Stufe/Mauer
  gibt es nicht; keine Octet-Variante (`OctetMaterials` kennt nur `end_stone`/`end_stone_bricks`), kein JEI-Sonderfall.
- Rezepte (SB-Datagen): `polished_end_stone` (2x2 Endsteinziegel -> 4), Steinsäge aus `end_stone` und `end_stone_bricks`;
  Zutat in `astral_end_stone`/`nihil_end_stone` (bleiben SB). Tag `minecraft:mineable/pickaxe`, `simplebuilding:craftable`.
  Fortschritt `simplebuilding:end/polished_off` (bleibt SB). Textur eigenes Bild (nicht Vanilla-abgeleitet).
- Vorlage: Stufe 3d (`LibMaterials`-Gruppen + `SharedMaterials` + `IdMovesHook` + Condition `simplelib:material_requested`).

## Umsetzung (wie 3d, neue Material-Gruppe `polished_end_stone`)
1. simplelib: `LibMaterials` Gruppe/Block/BlockItem (gleiche Eigenschaften), `gen_resources.py` (Modell, Blockstate, Item,
   Loot, 3 Rezepte + Rezept-Fortschritte mit Condition, `mineable/pickaxe`), Lang en/de, Textur, `wiki/manual.json`-Notiz.
2. SB 26.3 (`mc26_3/overlay/.../SharedMaterials`): Gruppe, Item/Block-Id, 3 Rezepte -> `IdMovesHook` verschiebt
   automatisch Item, Block, Rezepte (Rezeptbuch), Rezept-Fortschritte; Migrationstest nutzt dieselben Listen.
   `ModBlocks`/`ModItems` über `SharedMaterials.block/item`; 26.2-Zwilling registriert weiter selbst.
3. SB-Datagen: Modell/Loot/Tag/Rezepte hinter `!MATERIALS_IN_LIB`; 26.3-Datagen auf sb-test neu.
4. Tests: `DataIntegrityTests` (Rezeptprüfung über `MATERIAL_NAMESPACE`), Mutation p11 auf simplelib-Tag.
5. Generatoren auf sb-test: discover, module_guides, money generate_links, `wiki/generate.py --all`.

## Abweichungen (bewusst)
- Keine neue Treppe/Stufe/Mauer: existieren nicht; neue Blöcke wären ein eigener Wunsch (-> Simple Blocks/simplelib,
  offen in Übergabe A).
- Money-Preis bleibt in `money/simplebuilding/prices.json` (wie 3d, `MoneyLinks.priceOf` löst die verschobene Id auf).
- Simple End/Blocks rufen `requestMaterial("polished_end_stone")` noch nicht (nutzen den Block bisher nicht).

## Verifikation (sb-test)
Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`; Tests SimpleLib fabric+neoforge, SB-Filter `end_brick`/`DataIntegrity`/
`migration`, Standalone simplelib + simpleend; `checkModuleGuides checkModuleData checkWiki checkDiscover`.

## Stand (17.10.)
Umgesetzt wie geplant: `simplelib:polished_end_stone` (Gruppe `polished_end_stone`, SB fordert an), IdMoves für Item, Block,
3 Rezepte + Rezept-Fortschritte; 26.3-Datagen, discover, Guides, Money-Links, Wiki neu (sb-test). Ergebnisse siehe unten.
Ergebnisse sb-test (bf8378fa5): Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava` grün; `checkModuleGuides checkModuleData
checkWiki checkDiscover` Exit 0; SimpleLib fabric 26/26, neoforge 26/26, Simple End standalone fabric 36/36;
`data_integrity_game_test_*` fabric+neoforge 100/100; `migration*` 4/4. Nicht gelaufen: Forge-Tests, Client-Tests, volles Gate.
Offen: Merge nach `claude-wave1` + Gate; Treppe/Stufe/Mauer des polierten Endsteins (neuer Wunsch, nicht Teil des Umzugs).
