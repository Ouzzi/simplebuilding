# Audit Vanilla-Nähe der eigenen Texturen (21.10.2026, b-layers1)

Grundlage für **Teil 2** der Queue-Zeile „Bestehende Texturen … auf Vanilla-Nähe prüfen“ (PRINZIPIEN A8 a).
In Teil 1 wurde **kein Pixel geändert**; hier steht nur, wo eine Textur deutlich vom Minecraft-Stil abweicht.
**Der Besitzer entscheidet**, was in Teil 2 neu gezeichnet wird.

Erzeugt mit `python tools/textures/vanilla_nearness_audit.py --out <datei> [--sheet vorschau.png]` (reines Python,
ändert nichts). Umfang: alle 2086 eingecheckten eigenen Texturen (`src/main`, `mc26_3/overlay`, `modules/*`; ohne
Port-Kopie `mc1_21_11/`, ohne Vanilla-abgeleitete git-ignorierte Pfade). Nicht bewertet: Färbe-/Trim-Masken
(`_dyed`, `overlay`, `color_palettes`, `trims/`), GUI-Blätter, Leucht-/Augen-Ebenen (`_glow`, `_eyes`) – die sind
absichtlich grau oder flach.

## Merkmale

| Merkmal | Vanilla-Maßstab | Abweichung |
|---|---|---|
| halbtransparent | keine Halbtransparenz außer Glas/Eis/Portal | Pixel mit 0 < Alpha < 255 |
| schwarz | nie reines Schwarz | `#000000` deckend |
| kontur | Items mit 1-px-Kontur, dunkler als das Innere | Randpixel im Mittel nicht dunkler |
| licht | Licht von oben links | unten rechts > 0,12 heller als oben links |
| farben | 4–6 Stufen je Material | 16×16 mit > 64 Farben (Verlauf/Weichzeichnung) |
| flach | Schattierung in Stufen | ≤ 3 Farben auf größerer Fläche |
| grell | gedämpfte Sättigung | mittlere Sättigung > 0,75 bei Helligkeit > 0,5 |
| kontrast | sichtbare Hell-Dunkel-Stufen | Helligkeitsspanne < 0,12 |

## Stichprobe (16-fach angesehen) – Einordnung

- **Echt abweichend, Kandidaten für Teil 2:** verzauberte Bücher `item/enchanted_book_*` (halbtransparenter Schein-Rand,
  bis 90 Farben, Licht unten rechts – Vanilla-Buch ist deckend mit 1-px-Kontur); `simplefun:item/brick_snowball`
  (weicher Verlauf, 105 Farben); `simplemoney:item/money_bill`/`blank_note` (139–166 Farben, Verlauf);
  End-Blätter `simpleend:block/*_leaves*` (74–100 Grautöne statt weniger Stufen); Netherit-Truhe `item/netherite_chest`
  (halbtransparent + reines Schwarz); Milch/Butter `simplesandwiches:block/milk_cauldron/*`, `butter_block_inner`
  (flach, 3 Farben, ohne Schattierung).
- **Meist gewollt (Fehlalarm der Heuristik):** gefärbte Varianten (Taschen-Jukeboxen, Kisten in Färbefarben – „grell“/„licht“
  ergeben sich aus der Farbe), Lebensmittel-Streifen der Sandwiches (kräftige Zutatenfarben), Glut/Feuer-Animationen
  („grell“), Effekt-Symbole 18×18 (wenige Farben wie Vanilla).
- Gesamt: 137 von 2086 auffällig, davon 23 mit zwei oder mehr Merkmalen (oben in der Liste).

## Liste (nach Zahl der Merkmale)

| Merkmal | Anzahl |
|---|---|
| grell | 53 |
| licht | 31 |
| kontrast | 19 |
| halbtransparent | 17 |
| farben | 16 |
| flach | 12 |
| schwarz | 7 |
| kontur | 4 |

| Textur | Groesse | Merkmale |
|---|---|---|
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_cover.png` | 16x16 | halbtransparent (152 px), licht, farben (66) |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_fast_chiseling.png` | 16x16 | halbtransparent (152 px), licht, farben (67) |
| `modules/simplefun/shared/resources/assets/simplefun/textures/item/brick_snowball.png` | 16x16 | halbtransparent (46 px), farben (105) |
| `modules/simplemobs/shared/resources/assets/simplemobs/textures/entity/hollow_howler/hollow_howler_antennae.png` | 128x128 | flach (2 Farben), grell |
| `modules/simplemobs/shared/resources/assets/simplemobs/textures/entity/hollow_howler/hollow_howler_heart.png` | 128x128 | flach (3 Farben), grell |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/yellow_pocket_jukebox_open_back.png` | 16x16 | licht, grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/butter_block_inner.png` | 16x16 | flach (3 Farben), kontrast |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/milk_cauldron/milk_0.png` | 16x16 | flach (3 Farben), kontrast |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/milk_cauldron/milk_1.png` | 16x16 | flach (3 Farben), kontrast |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/milk_cauldron/milk_2.png` | 16x16 | flach (3 Farben), kontrast |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/milk_cauldron/milk_3.png` | 16x16 | flach (3 Farben), kontrast |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/netherite_chest.png` | 64x64 | halbtransparent (142 px), schwarz |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_break_through.png` | 16x16 | halbtransparent (152 px), licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_color_palette.png` | 16x16 | halbtransparent (152 px), licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_constructors_touch.png` | 16x16 | halbtransparent (152 px), licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_kinetic_protection.png` | 16x16 | licht, farben (90) |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_linear.png` | 16x16 | halbtransparent (152 px), licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_master_builder.png` | 16x16 | halbtransparent (152 px), licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_radius.png` | 16x16 | halbtransparent (152 px), licht |
| `src/main/resources/assets/simplebuilding/textures/item/netherite_chest.png` | 64x64 | halbtransparent (142 px), schwarz |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/iron_crucible_bottom.png` | 16x16 | kontrast |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/iron_crucible_handle.png` | 16x16 | flach (3 Farben) |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/iron_crucible_inner.png` | 16x16 | kontrast |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_cauldron_bottom.png` | 16x16 | flach (3 Farben) |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_cauldron_extreme.png` | 16x320 | grell |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_crucible_bottom.png` | 16x16 | kontrast |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/reinforced_crucible_inner.png` | 16x16 | kontrast |
| `modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/mob_effect/soul_burn.png` | 18x18 | flach (3 Farben) |
| `modules/simpledimensions/shared/resources/assets/simpledimension/textures/block/portal.png` | 16x512 | kontrast |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/ashwood_hanging_sign.png` | 32x32 | licht |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/ashwood_leaves.png` | 16x16 | farben (74) |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/ashwood_leaves_inner.png` | 16x16 | farben (94) |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/duskwood_leaves.png` | 16x16 | farben (86) |
| `modules/simpleend/shared/resources/assets/simpleend/textures/block/duskwood_leaves_inner.png` | 16x16 | farben (100) |
| `modules/simplefun/shared/resources/assets/simplefun/textures/mob_effect/piggy_effect.png` | 16x16 | schwarz |
| `modules/simplelib/shared/resources/assets/simplelib/textures/item/basic_upgrade_template.png` | 16x16 | grell |
| `modules/simplemobs/shared/resources/assets/simplemobs/textures/item/shellker_spawn_egg.png` | 16x16 | halbtransparent (1 px) |
| `modules/simplemobs/shared/resources/assets/simplemobs/textures/item/soulfire_blaze_spawn_egg.png` | 16x16 | halbtransparent (1 px) |
| `modules/simplemobs/shared/resources/assets/simplemobs/textures/mob_effect/shivering.png` | 18x18 | flach (3 Farben) |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/blank_note.png` | 16x16 | farben (139) |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/money_bill.png` | 16x16 | farben (166) |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/raw_bill.png` | 16x16 | farben (125) |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/refined_blank_note.png` | 16x16 | farben (139) |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/resin_fiber.png` | 16x16 | grell |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/special_fiber.png` | 16x16 | licht |
| `modules/simplemoney/shared/resources/assets/simplemoney/textures/item/special_paper.png` | 16x16 | farben (135) |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/black_pocket_jukebox_open_front.png` | 16x16 | kontur |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/light_gray_pocket_jukebox.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/light_gray_pocket_jukebox_open_back.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/lime_pocket_jukebox.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/lime_pocket_jukebox_open_back.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/orange_pocket_jukebox.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/orange_pocket_jukebox_open_front.png` | 16x16 | grell |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/pink_pocket_jukebox.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/pink_pocket_jukebox_open_back.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/purple_pocket_jukebox_open_front.png` | 16x16 | kontur |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/red_pocket_jukebox_open_front.png` | 16x16 | kontur |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/white_pocket_jukebox.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/white_pocket_jukebox_open_back.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/yellow_pocket_jukebox.png` | 16x16 | licht |
| `modules/simplemusic/shared/resources/assets/simplemusic/textures/item/yellow_pocket_jukebox_open_front.png` | 16x16 | grell |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/enderite_furnace_front_on.png` | 16x16 | licht |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_blast_furnace_top.png` | 16x16 | kontrast |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/netherite_furnace_front_on.png` | 16x16 | licht |
| `modules/simpleovens/shared/resources/assets/simpleovens/textures/block/reinforced_furnace_front_on.png` | 16x16 | licht |
| `modules/simpleriding/shared/resources/assets/simpleriding/textures/entity/horseshoe/diamond.png` | 64x64 | grell |
| `modules/simpleriding/shared/resources/assets/simpleriding/textures/entity/horseshoe/golden.png` | 64x64 | grell |
| `modules/simpleriding/shared/resources/assets/simpleriding/textures/item/enchanted_book_leaping.png` | 16x16 | halbtransparent (27 px) |
| `modules/simpleriding/shared/resources/assets/simpleriding/textures/item/enchanted_book_tailwind.png` | 16x16 | halbtransparent (27 px) |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/cheese_block_side.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/cheese_block_top.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_carrot.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_cheese.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_chicken_cooked.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_golden.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_pie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_potato.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_0_salmon_cooked.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_carrot.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_cheese.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_chicken_cooked.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_enderite.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_golden.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_pie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_1_potato.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_carrot.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_cheese.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_chicken_cooked.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_cookie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_enderite.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_golden.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_pie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_2_potato.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_carrot.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_cheese.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_chicken_cooked.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_cookie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_enderite.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_golden.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_pie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_3_potato.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_4_carrot.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_4_cheese.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_4_chicken_cooked.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_4_enderite.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_4_pie.png` | 16x16 | grell |
| `modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/item/sandwich/layer_4_potato.png` | 16x16 | grell |
| `modules/simpleshops/shared/resources/assets/simpleshops/textures/block/display_case_glass.png` | 16x16 | flach (3 Farben) |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/item/reinforced_chest.png` | 64x64 | schwarz |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/palettes/shulker/lime.png` | 7x1 | grell |
| `modules/simplestorage/shared/resources/assets/simplestorage/textures/palettes/shulker/yellow.png` | 7x1 | grell |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_furnace_front_on.png` | 16x16 | licht |
| `src/main/resources/assets/simplebuilding/textures/block/enderite_hopper_inside.png` | 16x16 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/block/flypad.png` | 16x16 | farben (98) |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_blast_furnace_top.png` | 16x16 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_furnace_front_on.png` | 16x16 | licht |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_hopper_inside.png` | 16x16 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/block/netherite_hopper_top.png` | 16x16 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_flypad.png` | 16x16 | farben (99) |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_furnace_front_on.png` | 16x16 | licht |
| `src/main/resources/assets/simplebuilding/textures/block/reinforced_hopper_inside.png` | 16x16 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/block/shulker.png` | 64x64 | schwarz |
| `src/main/resources/assets/simplebuilding/textures/block/stellar_flypad.png` | 16x16 | farben (192) |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid/enderite_leggings.png` | 64x32 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid_leggings/enderite.png` | 64x32 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/item/basic_upgrade_template.png` | 16x16 | grell |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_bridge.png` | 16x16 | halbtransparent (152 px) |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_drawer.png` | 16x16 | schwarz |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_funnel.png` | 16x16 | licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_strip_miner.png` | 16x16 | licht |
| `src/main/resources/assets/simplebuilding/textures/item/enchanted_book_vein_miner.png` | 16x16 | licht |
| `src/main/resources/assets/simplebuilding/textures/item/magnet.png` | 16x16 | kontur |
| `src/main/resources/assets/simplebuilding/textures/item/netherite_hopper.png` | 16x16 | kontrast |
| `src/main/resources/assets/simplebuilding/textures/item/reinforced_chest.png` | 64x64 | schwarz |
| `src/main/resources/assets/simplebuilding/textures/item/spawn_elytra.png` | 16x16 | halbtransparent (122 px) |
| `src/main/resources/assets/simplebuilding/textures/palettes/shulker/lime.png` | 7x1 | grell |
| `src/main/resources/assets/simplebuilding/textures/palettes/shulker/yellow.png` | 7x1 | grell |
