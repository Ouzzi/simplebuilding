# PLAN A-MOJANG08 – keine Mojang-Pixel im Repo (Release-Blocker 1), 08.11.2026

Branch `a-mojang08` (Basis hub `a-rel08` 7d6874efb). Auftrag: RELEASE-CHECK-2026-11-08 Blocker 1, /root/sb-tools-a/a05-auftrag.md + a05b-zusatz.md.

## 1. Plan

1. Pixelvergleich aller getrackten PNGs gegen `minecraft-client.jar` 26.3 (`~/.gradle/caches/fabric-loom/26.3/`, gleich dem NeoForm-Jar auf sb-agent) in drei Stufen:
   exakt (RGBA, transparente Pixel normiert), nahezu (≥95 % gleiche Pixel, gleiche Größe), Farbtausch (gleiche Alpha-Maske, Abbildung Vanilla-Farbe → eigene Farbe;
   1:1 und n:1, Anteil der Pixel, die der Abbildung folgen ≥0,90). Treffer 0,90–0,99 einzeln angesehen, Fehltreffer verworfen (Abschnitt 3).
2. Lösung je Treffer: a) unverändert gebraucht → Verweis auf `minecraft:…`, Datei weg; b) umgefärbt/abgeleitet → Rezept in die vanilla_derived-Pipeline, Datei aus Git, `.gitignore`;
   c) Wiki-/Tool-Kopien löschen bzw. zur Build-Zeit erzeugen; tote Kopien löschen.
3. Prüfen: Rezept baut jede Datei pixelgleich nach; Pipeline-Ausgaben vorher/nachher gleich; Generator-`--check`, precheck, check_missing_textures, sbt Compile + Module + Kern-Filter.

Risiken: Generatoren, die eigene Nachbartexturen lesen (Rampen), brechen ohne die Dateien → `recolour08.open_own()`; Modul-check_data verlangen Dateien → kennen jetzt die Rezepte.

## 2. Umsetzung

- **Neu** `tools/textures/recolour08_2026_11_08.py` + `.json`: je Ausgabe Quelle (Vanilla-Pfad), `map` Palettenindex des Vanilla-Sprites → eigene Farbe (nur wo abweichend), `fix` Pixelindex → eigene Farbe.
  Es wird weder ein Vanilla-Pixel noch eine Vanilla-Farbe gespeichert. `--capture` (einmalig), `--verify` (151/151 pixelgleich).
- `vanilla_derived.py`: 76 neue OUTPUTS-Zeilen (simplebuilding) und Einträge in MODULE_OUTPUTS (simplecrucibles, simpleend, simplelib neu, simplemachines, simpleovens, simplepads neu, simplestorage);
  `module_images` ruft `derived_module(jar, module)`; 11 Wiki-Renders in WIKI_RENDERS. simplelib/simplepads fabric: `module-vanilla-derived.gradle` angewendet.
- Verweise auf Vanilla: Tiegel-Innenseite (iron/reinforced, gen_resources.py), Auto-Smither-Unterseite (4 Modelle), Simple-Fun-Schweinekopf (Java).
- Generator-Leser über `recolour08.open_own`: vehicles_2026_10_10, crucible_placeholders, ovens_boats, shelves01; generate_textures liest Pad-Grundplatten aus dem Jar.
  Pipeline vorher/nachher: 490 bestehende abgeleitete Ausgaben bytegleich, 151 neue.
- simplestorage/simplemachines check_data: Rezept-Texturen gelten als vorhanden.
- Gelöscht: 695 Dateien (PNG + `.layers.json`, 2 verwaiste `.mcmeta`); `.gitignore` +462 Zeilen (Repo-Pfade, Wiki-Kopien/Sheets/Renders, ig3114-Vorschauen).
- Abweichung: die 26.2-Wurzel (`src/main/resources`) verliert die gelöschten/abgeleiteten SB-Texturen ebenso wie bei früheren Rezepten (vanilla_derived läuft nur für 26.3) – Port später.

## 3. Ergebnis je Datei

| Datei | Fundtyp | Lösung | Status |
|---|---|---|---|
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/auto_smither_bottom.png` | exakt ← `block/crafter_bottom` | a) Modelle → `minecraft:block/crafter_bottom`, Datei gelöscht | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/iron_crucible_inner.png` | exakt ← `block/cauldron_inner` | a) gen_resources.py → `minecraft:block/cauldron_inner` (24 Modelle neu), Datei gelöscht | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_crucible_inner.png` | exakt ← `block/cauldron_inner` | a) wie iron_crucible_inner | erledigt |
| `modules/simplefun/shared/resources/assets/simplefun/textures/entity/pig/pig.png` | exakt ← `entity/pig/pig_temperate` | a) PigHeadFeatureRenderer → `minecraft:textures/entity/pig/pig_temperate.png`, Datei gelöscht | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_smoker_front.png` | Farbtausch 1:1 ← `block/smoker_front` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_smoker_side.png` | Farbtausch 1:1 ← `block/smoker_side` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_smoker_top.png` | Farbtausch 1:1 ← `block/smoker_top` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_blast_furnace_top.png` | Farbtausch 1:1 ← `block/blast_furnace_top` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_smoker_front.png` | Farbtausch 1:1 ← `block/smoker_front` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_smoker_front_on.png` | Farbtausch 1:1 ← `block/smoker_front_on` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_smoker_side.png` | Farbtausch 1:1 ← `block/smoker_side` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_smoker_top.png` | Farbtausch 1:1 ← `block/smoker_top` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_blast_furnace_top.png` | Farbtausch 1:1 ← `block/blast_furnace_top` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_smoker_front.png` | Farbtausch 1:1 ← `block/smoker_front` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_smoker_front_on.png` | Farbtausch 1:1 ← `block/smoker_front_on` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_smoker_side.png` | Farbtausch 1:1 ← `block/smoker_side` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_smoker_top.png` | Farbtausch 1:1 ← `block/smoker_top` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/shulker.png` | exakt ← `entity/shulker/shulker` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid/enderite_leggings.png` | Farbtausch 1:1 ← `entity/equipment/humanoid/netherite` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid/netherite.png` | exakt ← `entity/equipment/humanoid/netherite` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid_leggings/enderite.png` | Farbtausch 1:1 ← `entity/equipment/humanoid/netherite` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid_leggings/netherite.png` | exakt ← `entity/equipment/humanoid_leggings/netherite` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/enderite_bundle_open_back.png` | Farbtausch 1:1 ← `item/black_bundle_open_back` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/netherite_bundle_open_back.png` | Farbtausch 1:1 ← `item/black_bundle_open_back` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/reinforced_bundle_open_back.png` | Farbtausch 1:1 ← `item/black_bundle_open_back` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/reinforced_chest.png` | Farbtausch 1:1 ← `entity/chest/copper` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/spawn_elytra.png` | Farbtausch 1:1 ← `item/elytra` | gelöscht (auf 26.3 ungenutzt: Modelle in removed-on-26.3 bzw. ohne Verweis) | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/auto_smither_front.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `block/crafter_north` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/auto_smither_front_crafting.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `block/crafter_north_crafting` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/auto_smither_side.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `block/crafter_south` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/auto_smither_side_triggered.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `block/crafter_south` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `block/crafter_south_triggered` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_side_crafting.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `block/crafter_south_triggered` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_side_triggered.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `block/crafter_south_triggered` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_top.png` | Farbtausch 1:1 ← `block/crafter_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_top_crafting.png` | Farbtausch 1:1 ← `block/crafter_top_crafting` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_top_triggered.png` | Farbtausch 1:1 ← `block/crafter_top_triggered` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/diamond_pole.png` | Farbtausch 1:1 ← `block/lightning_rod` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/enderite_pole.png` | Farbtausch 1:1 ← `block/lightning_rod` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/gold_pole.png` | Farbtausch 1:1 ← `block/lightning_rod` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/iron_pole.png` | Farbtausch 1:1 ← `block/lightning_rod` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/block/netherite_pole.png` | Farbtausch 1:1 ← `block/lightning_rod` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/enderite_trapped.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `entity/chest/trapped` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/enderite_trapped_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.929 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/enderite_trapped_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.931 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/netherite_trapped.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/trapped` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/netherite_trapped_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/netherite_trapped_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/reinforced_trapped.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/trapped` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/reinforced_trapped_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/reinforced_trapped_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/entity/training_dummy/straw_armor_stand.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `entity/armorstand/armorstand` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/item/astral_redstone.png` | Farbtausch 1:1 ← `item/gunpowder` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `mc26_3/overlay/resources/assets/simplebuilding/textures/item/nihil_redstone.png` | Farbtausch 1:1 ← `item/gunpowder` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/enderite_crucible_inner.png` | Farbtausch 1:1 ← `block/cauldron_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/netherite_crucible_inner.png` | Farbtausch 1:1 ← `block/cauldron_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_cauldron_bottom.png` | nahezu (≥95 % gleich) 0.952 ← `block/cauldron_bottom` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_cauldron_extreme.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `block/lava_still` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_cauldron_inner.png` | nahezu (≥95 % gleich) 0.984 ← `block/cauldron_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_cauldron_top.png` | nahezu (≥95 % gleich) 0.974 ← `block/cauldron_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/ashwood_planks.png` | Farbtausch 1:1 ← `block/birch_planks` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/duskwood_planks.png` | Farbtausch 1:1 ← `block/birch_planks` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplelib/shared/resources/assets/simplelib/textures/item/basic_upgrade_template.png` | Farbtausch (n:1, Anteil Abbildung) 0.910 ← `item/netherite_upgrade_smithing_template` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_hopper_inside.png` | Farbtausch 1:1 ← `block/cauldron_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_hopper_outside.png` | Farbtausch (n:1, Anteil Abbildung) 0.977 ← `block/hopper_outside` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_hopper_top.png` | Farbtausch 1:1 ← `block/hopper_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_piston_head.png` | Farbtausch (n:1, Anteil Abbildung) 0.965 ← `block/piston_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_piston_inner.png` | Farbtausch (n:1, Anteil Abbildung) 0.938 ← `block/piston_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_piston_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `block/piston_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_piston_side_worn1.png` | Farbtausch (n:1, Anteil Abbildung) 0.910 ← `block/piston_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/enderite_piston_top.png` | Farbtausch (n:1, Anteil Abbildung) 0.965 ← `block/piston_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_hopper_inside.png` | Farbtausch 1:1 ← `block/cauldron_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_hopper_outside.png` | Farbtausch (n:1, Anteil Abbildung) 0.969 ← `block/hopper_outside` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_hopper_top.png` | Farbtausch 1:1 ← `block/hopper_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_piston_head.png` | Farbtausch (n:1, Anteil Abbildung) 0.969 ← `block/piston_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_piston_inner.png` | Farbtausch (n:1, Anteil Abbildung) 0.941 ← `block/piston_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_piston_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.937 ← `block/piston_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_piston_side_worn1.png` | Farbtausch (n:1, Anteil Abbildung) 0.914 ← `block/piston_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/netherite_piston_top.png` | Farbtausch (n:1, Anteil Abbildung) 0.969 ← `block/piston_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_hopper_inside.png` | Farbtausch 1:1 ← `block/cauldron_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_hopper_outside.png` | Farbtausch (n:1, Anteil Abbildung) 0.969 ← `block/hopper_outside` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_hopper_top.png` | Farbtausch 1:1 ← `block/hopper_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_piston_head.png` | Farbtausch (n:1, Anteil Abbildung) 0.969 ← `block/piston_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_piston_inner.png` | Farbtausch (n:1, Anteil Abbildung) 0.941 ← `block/piston_inner` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_piston_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.937 ← `block/piston_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_piston_top.png` | Farbtausch (n:1, Anteil Abbildung) 0.969 ← `block/piston_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/block/reinforced_piston_top_sticky.png` | Farbtausch 1:1 ← `block/piston_top_sticky` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/item/enderite_hopper.png` | Farbtausch 1:1 ← `item/hopper` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/item/netherite_hopper.png` | Farbtausch 1:1 ← `item/hopper` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplemachines/shared/resources/assets/simplemachines/textures/item/reinforced_hopper.png` | Farbtausch 1:1 ← `item/hopper` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_blast_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.918 ← `block/blast_furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_blast_furnace_top.png` | Farbtausch (n:1, Anteil Abbildung) 0.992 ← `block/blast_furnace_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.906 ← `block/furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_smoker_front.png` | Farbtausch 1:1 ← `block/smoker_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_smoker_front_on.png` | Farbtausch (n:1, Anteil Abbildung) 0.992 ← `block/smoker_front_on` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_smoker_side.png` | Farbtausch 1:1 ← `block/smoker_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_smoker_top.png` | Farbtausch 1:1 ← `block/smoker_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_blast_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/blast_furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_blast_furnace_top.png` | Farbtausch 1:1 ← `block/blast_furnace_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.914 ← `block/furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_smoker_front.png` | Farbtausch 1:1 ← `block/smoker_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_smoker_front_on.png` | Farbtausch 1:1 ← `block/smoker_front_on` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_smoker_side.png` | Farbtausch 1:1 ← `block/smoker_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_smoker_top.png` | Farbtausch 1:1 ← `block/smoker_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_blast_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/blast_furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_blast_furnace_top.png` | Farbtausch 1:1 ← `block/blast_furnace_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.914 ← `block/furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_smoker_front.png` | Farbtausch 1:1 ← `block/smoker_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_smoker_front_on.png` | Farbtausch 1:1 ← `block/smoker_front_on` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_smoker_side.png` | Farbtausch 1:1 ← `block/smoker_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_smoker_top.png` | Farbtausch 1:1 ← `block/smoker_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplepads/shared/resources/assets/simplepads/textures/item/spawn_elytra.png` | Farbtausch 1:1 ← `item/elytra` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/block/copper_barrel_top_open.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/barrel_top_open` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/block/netherite_barrel_top_open.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/barrel_top_open` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/block/netherite_chest.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/block/reinforced_barrel_top_open.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/barrel_top_open` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/block/reinforced_chest.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/enderite.png` | Farbtausch (n:1, Anteil Abbildung) 0.923 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/enderite_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.929 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/enderite_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.931 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/enderite_trapped.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `entity/chest/trapped` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/enderite_trapped_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.929 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/enderite_trapped_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.931 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/netherite.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/netherite_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/netherite_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/netherite_trapped.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/trapped` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/netherite_trapped_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/netherite_trapped_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/reinforced.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/reinforced_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/reinforced_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/reinforced_trapped.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/trapped` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/reinforced_trapped_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/entity/chest/reinforced_trapped_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/enderite_bundle_open_back.png` | Farbtausch 1:1 ← `item/black_bundle_open_back` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/enderite_bundle_open_front.png` | Farbtausch (n:1, Anteil Abbildung) 0.916 ← `item/black_bundle_open_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/netherite_bundle_open_back.png` | Farbtausch 1:1 ← `item/black_bundle_open_back` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/netherite_bundle_open_front.png` | Farbtausch (n:1, Anteil Abbildung) 0.916 ← `item/black_bundle_open_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/reinforced_bundle.png` | Farbtausch (n:1, Anteil Abbildung) 0.900 ← `item/black_bundle` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/reinforced_bundle_open_back.png` | Farbtausch 1:1 ← `item/black_bundle_open_back` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/reinforced_bundle_open_front.png` | Farbtausch (n:1, Anteil Abbildung) 0.916 ← `item/black_bundle_open_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/reinforced_chest.png` | Farbtausch 1:1 ← `entity/chest/copper` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/astral_purpur_block.png` | Farbtausch 1:1 ← `block/purpur_block` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_blast_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.918 ← `block/blast_furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_blast_furnace_top.png` | Farbtausch (n:1, Anteil Abbildung) 0.992 ← `block/blast_furnace_top` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.906 ← `block/furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_smoker_bottom.png` | Farbtausch (n:1, Anteil Abbildung) 0.992 ← `block/smoker_bottom` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_smoker_front_on.png` | Farbtausch (n:1, Anteil Abbildung) 0.992 ← `block/smoker_front_on` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_upgrade_template.png` | Farbtausch (n:1, Anteil Abbildung) 0.910 ← `item/netherite_upgrade_smithing_template` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_blast_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/blast_furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_chest.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.914 ← `block/furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/nihil_purpur_block.png` | Farbtausch 1:1 ← `block/purpur_block` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_blast_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.922 ← `block/blast_furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_chest.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_furnace_side.png` | Farbtausch (n:1, Anteil Abbildung) 0.914 ← `block/furnace_side` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/enderite.png` | Farbtausch (n:1, Anteil Abbildung) 0.923 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/enderite_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.929 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/enderite_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.931 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/netherite.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/netherite_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/netherite_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/reinforced.png` | Farbtausch (n:1, Anteil Abbildung) 0.927 ← `entity/chest/normal` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/reinforced_left.png` | Farbtausch (n:1, Anteil Abbildung) 0.933 ← `entity/chest/trapped_left` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/chest/reinforced_right.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `entity/chest/trapped_right` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid/enderite.png` | Farbtausch (n:1, Anteil Abbildung) 0.941 ← `entity/equipment/humanoid/netherite` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid_leggings/enderite_leggings.png` | Farbtausch (n:1, Anteil Abbildung) 0.952 ← `entity/equipment/humanoid_leggings/netherite` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/basic_upgrade_template.png` | Farbtausch (n:1, Anteil Abbildung) 0.910 ← `item/netherite_upgrade_smithing_template` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/enderite_bundle_open_front.png` | Farbtausch (n:1, Anteil Abbildung) 0.916 ← `item/black_bundle_open_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/enderite_carrot.png` | Farbtausch (n:1, Anteil Abbildung) 0.948 ← `item/carrot` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/enderite_helmet.png` | Farbtausch (n:1, Anteil Abbildung) 0.935 ← `item/netherite_helmet` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/enderite_leggings.png` | Farbtausch (n:1, Anteil Abbildung) 0.942 ← `item/netherite_leggings` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/enderite_spear_in_hand.png` | Farbtausch (n:1, Anteil Abbildung) 0.909 ← `item/stone_spear_in_hand` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/netherite_bundle_open_front.png` | Farbtausch (n:1, Anteil Abbildung) 0.916 ← `item/black_bundle_open_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/netherite_carrot.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `item/carrot` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/reinforced_bundle.png` | Farbtausch (n:1, Anteil Abbildung) 0.900 ← `item/black_bundle` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `src/main/resources/assets/simplebuilding/textures/item/reinforced_bundle_open_front.png` | Farbtausch (n:1, Anteil Abbildung) 0.916 ← `item/black_bundle_open_front` | b) recolour08-Rezept, vanilla_derived, .gitignore | erledigt |
| `art/dev-textures/replaced-2026-09-28/entity/chest/reinforced_chest.png` | Farbtausch 1:1 ← `entity/chest/copper` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/block/reinforced_chest_left.png` | Farbtausch 1:1 ← `entity/chest/copper_left` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/block/reinforced_chest_right.png` | Farbtausch 1:1 ← `entity/chest/copper_right` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/entity/chest/reinforced_chest_left.png` | Farbtausch 1:1 ← `entity/chest/copper_left` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/entity/chest/reinforced_chest_right.png` | Farbtausch 1:1 ← `entity/chest/copper_right` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/item/chest/reinforced_chest_left.png` | Farbtausch 1:1 ← `entity/chest/copper_left` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/item/chest/reinforced_chest_right.png` | Farbtausch 1:1 ← `entity/chest/copper_right` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/item/reinforced_chest_left.png` | Farbtausch 1:1 ← `entity/chest/copper_left` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `art/dev-textures/unused-2026-09-27/item/reinforced_chest_right.png` | Farbtausch 1:1 ← `entity/chest/copper_right` | c) gelöscht (Archiv unbenutzter Dev-Texturen) | erledigt |
| `tools/textures/hand/enderite_carrot.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `item/carrot` | c) gelöscht (Vorlage; Ressource jetzt Rezept) | erledigt |
| `tools/textures/hand/pad_base_diamond.png` | exakt ← `block/diamond_block` | c) generate_textures.py liest Vanilla-Block aus dem Jar, gelöscht | erledigt |
| `tools/textures/hand/pad_base_gold.png` | exakt ← `block/gold_block` | c) wie pad_base_diamond | erledigt |
| `tools/textures/hand/pad_base_iron.png` | exakt ← `block/iron_block` | c) wie pad_base_diamond | erledigt |
| `tools/textures/hand/pad_res/netherite_pressure_plate.png` | exakt ← `block/netherite_block` | c) generate_textures.py liest netherite_block aus dem Jar, gelöscht | erledigt |
| `tools/textures/hand/q1/spawn_elytra.png` | Farbtausch 1:1 ← `item/elytra` | c) gelöscht (Vorlage; Ressource jetzt Rezept) | erledigt |
| `tools/textures/hand/r7/enderite_spear_in_hand.png` | Farbtausch (n:1, Anteil Abbildung) 0.942 ← `item/stone_spear_in_hand` | c) gelöscht (Vorlage; Ressource jetzt Rezept) | erledigt |
| `tools/textures/ig3114/music_A_blank_disc.png` | Farbtausch (n:1, Anteil Abbildung) 1.000 ← `item/music_disc_relic` | c) gelöscht + .gitignore (Vorschau) | erledigt |
| `tools/textures/ig3114/music_B_blank_disc.png` | Farbtausch (n:1, Anteil Abbildung) 0.983 ← `item/music_disc_relic` | c) gelöscht + .gitignore (Vorschau) | erledigt |

**Wiki-Kopien** (alle gelöscht, `.gitignore`, werden von wiki/generate.py aus der Pipeline geschrieben bzw. entfallen): `wiki/assets/textures/block` 36, `wiki/assets/textures/item` 13, `wiki/assets/textures/sheets/simplebuilding` 97, `wiki/assets/textures/sheets/simplecrucibles` 8, `wiki/assets/textures/sheets/simplefun` 1, `wiki/assets/textures/sheets/simplemachines` 12, `wiki/assets/textures/sheets/simpleovens` 4, `wiki/assets/textures/sheets/simplestorage` 25, `wiki/assets/textures/simplearrows` 2, `wiki/assets/textures/simpleend` 2, `wiki/assets/textures/simplelib` 1, `wiki/assets/textures/simplemachines` 3, `wiki/assets/textures/simpleores` 4, `wiki/assets/textures/simplepads` 1, `wiki/assets/textures/simplestorage` 3; dazu 11 3D-Renders (`astral_purpur_block.png`, `auto_smither.png`, `autonomous_crafter.png`, `block/astral_redstone.png`, `block/nihil_redstone.png`, `diamond_pole.png`, `enderite_pole.png`, `gold_pole.png`, `iron_pole.png`, `netherite_pole.png`, `nihil_purpur_block.png`) und 81 neue Sheet-Kopien abgeleiteter Modultexturen, die der Wiki-Lauf schreibt.

**Fehltreffer (bleiben):** `simpledinos block/egg_overlay_0` (= `redstone_dust_overlay`, beide vollständig transparent, keine Pixel);
`simplecrucibles *_crucible_handle` (bee_nest_side 0,90), `simplelights black_copper_light_pane_off` (Gemälde 0,91), `simplemobs` dread_glow, hollow_howler_glow/heart/antennae,
hollow_watcher_eyes (dünn besetzte Leuchtschichten 0,94–0,98), `simplefoundry casting_tray_empty` (0,99, Flächen), `simpleshops display_case_glass*` (17×, 0,97, einfarbige Fläche) – angesehen, eigene Zeichnung.

## 4. Verifikation

- `recolour08 --verify`: 151/151 pixelgleich; Pipeline vor/nach: 490/490 bytegleich + 151 neu.
- `precheck.py --fix` (alle Generatoren) + `precheck.py`: 85/86 grün; rot nur `tools/guides/module_guides.py --check` (simplequalityoflife/simpletools de_de „Öfen“→„Oefen“) – auf der Basis 7d6874efb identisch rot, nicht angefasst.
- `precheck.py --fast` grün; `check_missing_textures` 0 Fehler; `layers.py check` 0 Probleme; `tools/textures/tests` 57 OK; `wiki/generate.py --all --check` grün.
- sbt: SBT_RESULT

## 5. Offen / Besitzer

- **Git-Historie** enthält die alten Pixel weiter (kein Umschreiben, kein Force-Push) – Besitzer-Entscheidung, ob vor dem öffentlichen Release ein bereinigter Export/Squash-Repo nötig ist.
- **mc1_21_11** (nicht im Auftragsumfang): 82 Treffer (48 exakt, 34 Farbtausch), z. B. `block/diamond_block`, `end_stone`, `blast_furnace_front`, Tiegel/Trichter/Räucherofen. Braucht 1.21.11-Pipeline, beim Port (Regel „Ports erst auf Anforderung“).
- **Wiki-3D-Renders allgemein**: Renders von Blöcken mit Vanilla-Texturen (z. B. `render/*_cauldron`, Purpur-Schach) zeigen projizierte Mojang-Pixel; nur die 11 Renders der hier umgestellten Texturen sind jetzt Build-Zeit. Vorschlag: alle Renders zur Build-Zeit (Rückfrage).
- Teil-Ableitungen unter 90 % Abbildungsanteil (z. B. Pads mit Vanilla-Grundplatte aus generate_textures, eigene Overlays) sind nicht erfasst; Methode im Skript `recolour08` erweiterbar.
- Die historischen Generatoren (generate_textures, autonomous_crafter/auto_smither, material_rods, texture_round7 …) schreiben die Pfade bei einem Neulauf weiter – jetzt in ignorierte Pfade; Quelle der Wahrheit ist das Rezept.
- Prod-Übernahme `development`/Bündel → Production: offen (Gate/Besitzer).
