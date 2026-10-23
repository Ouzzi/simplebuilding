"""Vanilla-derived textures: built, never committed (owner rule 2026-10-12).

A texture whose pixels come from a Mojang texture (a recolour or mask of a sprite read from the Minecraft client
jar) must not live in the public repository. It is generated from the local 26.3 client jar at build time:

  - Gradle (mc26_3/resources.gradle, task generateVanillaDerived263) runs this script before processResources of
    every 26.3 loader and ships build/generated-resources/vanillaDerived as an extra resource root;
  - wiki/generate.py writes them into build/vanilla-derived/resources (or placeholders without a jar) and reads
    them from there like a resource layer;
  - the repository paths are in .gitignore and the --check modes of the texture generators skip them.

The generators stay where the art was made; this file only lists the outputs and calls them.

Run: python3.12 tools/textures/vanilla_derived.py --out DIR [--jar client.jar] [--placeholder-if-missing]
     python3.12 tools/textures/vanilla_derived.py --list
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import importlib
import os
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
NS = 'simplebuilding'
# Where wiki/generate.py and the local tools keep the generated set (build/ is ignored by git).
LOCAL_OUT = ROOT / 'build/vanilla-derived/resources'

# Texture path (under assets/simplebuilding/textures/) -> (size, generator description).
OUTPUTS = {
    'item/blaze_book.png': ((16, 16), 'n41_round3_2026_10_12.blaze_book(2) - Vanilla written_book layout (D9)'),
    'entity/chest/astral_vault.png': ((64, 64), 'n46_round3_2026_10_12.game_chests() - Vanilla ender chest (A2)'),
    'entity/chest/nihil_vault.png': ((64, 64), 'n46_round3_2026_10_12.game_chests() - Vanilla ender chest (N2)'),
    # Older generators (owner 2026-10-12: build them too; Git history is not rewritten):
    'gui/container/auto_smither.png': ((256, 256), 'auto_smither_gui.derived()'),
    'item/color_brush.png': ((16, 16), 'color_brush_2026_10_09.derived()'),
    'item/color_brush_tip.png': ((16, 16), 'color_brush_2026_10_09.derived()'),
    'item/color_brush_tip_palette.png': ((16, 16), 'color_brush_2026_10_09.derived()'),
    'block/astral_piston.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/astral_piston_active.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/nihil_piston.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/nihil_piston_active.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/astral_rail.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/astral_rail_on.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/nihil_rail.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/nihil_rail_on.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/sulfur_fire_0.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/sulfur_fire_1.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/copper_fire_0.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/copper_fire_1.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/silent_dandelion.png': ((16, 16), 'silent_dandelion_2026_10_03.derived()'),
    'item/music_disc_voidline.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_voidline_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_voidline_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_voidline_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/jukebox_amplifier_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/jukebox_amplifier_top.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/note_amplifier_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/note_amplifier_top.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/placed_egg_white.png': ((16, 16), 'placed_egg_textures.derived()'),
    'block/placed_egg_blue.png': ((16, 16), 'placed_egg_textures.derived()'),
    'block/placed_egg_brown.png': ((16, 16), 'placed_egg_textures.derived()'),
    'block/standing_stick.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'block/standing_bone.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'block/standing_blaze_rod.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'block/standing_breeze_rod.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'item/reinforced_furnace_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_furnace_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_furnace_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/reinforced_hopper_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_hopper_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_hopper_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    # Top views of the placed discs (Item display 3D, placed_display_3d_2026_10_16.py): 22 Vanilla + 16 mod discs.
    # Explicit lines on purpose: the Groovy fallback (mc26_3/resources.gradle) greps this dict and cannot expand comprehensions.
    'block/placed_disc_music_disc_11.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_13.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_5.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_blocks.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_bounce.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_cat.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_chirp.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_creator.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_creator_music_box.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_far.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_lava_chicken.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_mall.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_mellohi.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_otherside.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_pigstep.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_precipice.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_relic.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_stal.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_strad.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_tears.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_wait.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_ward.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_voidline.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_voidline_b_side.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_voidline_track_3.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_voidline_track_4.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_driftwood.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_driftwood_b_side.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_driftwood_track_3.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_driftwood_track_4.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_daybreak.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_daybreak_b_side.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_daybreak_track_3.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_daybreak_track_4.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_brimstone.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_brimstone_b_side.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_brimstone_track_3.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    'block/placed_disc_music_disc_brimstone_track_4.png': ((16, 16), 'placed_display_3d_2026_10_16.derived() - disc top view'),
    # Sulfur torch / lantern / campfire (N67): Vanilla torch, lantern and campfire with the sulfur flame.
    'block/sulfur_torch.png': ((16, 16), 'sulfur_lights_2026_10_15.derived()'),
    'block/sulfur_lantern.png': ((16, 48), 'sulfur_lights_2026_10_15.derived()'),
    'item/sulfur_lantern.png': ((16, 16), 'sulfur_lights_2026_10_15.derived()'),
    'item/sulfur_campfire.png': ((16, 16), 'sulfur_lights_2026_10_15.derived()'),
    'block/sulfur_campfire_fire.png': ((16, 128), 'sulfur_lights_2026_10_15.derived()'),
    'block/sulfur_campfire_log_lit.png': ((16, 64), 'sulfur_lights_2026_10_15.derived()'),
}
# Module textures (namespace = module id, shipped by that module's own jars through gradle/module-vanilla-derived.gradle):
# module -> {texture path under assets/<module>/textures/: (size, generator description)}.
MODULE_OUTPUTS = {
    'simplemobs': {'item/shellker_shell.png': ((16, 16), 'mobs2_textures.derived() - Vanilla shulker_shell (N48)'),
                   'item/soul_fire_charge.png': ((16, 16), 'mobs2_textures.derived() - Vanilla fire_charge (N49)'),
                   'item/soulfire_powder.png': ((16, 16), 'mobs2_textures.derived() - Vanilla blaze_powder (N49)'),
                   'entity/blighted_wither/blighted_wither.png': ((64, 64), 'wither_textures.derived() - Vanilla wither skin (N26)'),
                   'entity/blighted_wither/abyssal_wither.png': ((64, 64), 'wither_textures.derived() - Vanilla wither skin (N50)')},
    'simpleblocks': {'block/cracked_red_nether_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/cracked_tuff_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_tuff_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/cracked_mud_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_mud_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/cracked_end_stone_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_end_stone_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_deepslate_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_deepslate_tiles.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/cracked_cinnabar_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_cinnabar_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/cracked_sulfur_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/mossy_sulfur_bricks.png': ((16, 16), 'missing_blocks_2026_10_16.derived() - Vanilla brick + cracks/moss (N69)'), 'block/shapecutter_top.png': ((16, 16), 'shapecutter_2026_10_21.derived() - Vanilla stonecutter, polished metal base (A4b)'), 'block/shapecutter_side.png': ((16, 16), 'shapecutter_2026_10_21.derived() - Vanilla stonecutter, polished metal base (A4b)'), 'block/shapecutter_bottom.png': ((16, 16), 'shapecutter_2026_10_21.derived() - Vanilla stonecutter, polished metal base (A4b)'), 'block/shapecutter_saw.png': ((16, 48), 'shapecutter_2026_10_21.derived() - Vanilla stonecutter blade, variant BLADE (A4b)'), 'block/sculk_deepslate.png': ((16, 16), 'blocks_b23_2026_10_23.derived() - Vanilla deepslate + own sculk veins (W23-3)'), 'block/hushed_deepslate_tiles.png': ((16, 16), 'blocks_b23_2026_10_23.derived() - Vanilla deepslate tiles + own sculk (W23-3)'), 'block/chiseled_packed_ice.png': ((16, 16), 'blocks_b23_2026_10_23.derived() - Vanilla packed ice + carved relief (W23-3)'), 'block/chiseled_blue_ice.png': ((16, 16), 'blocks_b23_2026_10_23.derived() - Vanilla blue ice + carved relief (W23-3)')},
    # simplestorage (split stage 8b-2, vehicles_2026_10_10 art; 8b-3 tier shulker shells and rare shulker hulls): one line (gradle/module-vanilla-derived.gradle reads its placeholder sizes from it).
    'simplestorage': {'item/reinforced_chest_minecart.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/reinforced_chest_boat_overlay.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/reinforced_chest_raft_overlay.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/netherite_chest_minecart.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/netherite_chest_boat_overlay.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/netherite_chest_raft_overlay.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/enderite_chest_minecart.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/enderite_chest_boat_overlay.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/enderite_chest_raft_overlay.png': ((16, 16), 'storage_vehicles_2026_10_20.derived() - Vanilla minecart / chest boat masks (split stage 8b-2)'), 'item/reinforced_shulker_shell.png': ((16, 16), 'shulker_shell_textures.derived() - Vanilla shulker_shell + tier plating (split stage 8b-3)'), 'item/netherite_shulker_shell.png': ((16, 16), 'shulker_shell_textures.derived() - Vanilla shulker_shell + tier plating (split stage 8b-3)'), 'item/enderite_shulker_shell.png': ((16, 16), 'shulker_shell_textures.derived() - Vanilla shulker_shell + tier plating (split stage 8b-3)'), 'entity/rare_shulker/reinforced.png': ((64, 64), 'shulker_shell_textures.derived() - tier shulker box + Vanilla shulker head (split stage 8b-3)'), 'entity/rare_shulker/netherite.png': ((64, 64), 'shulker_shell_textures.derived() - tier shulker box + Vanilla shulker head (split stage 8b-3)'), 'entity/rare_shulker/enderite.png': ((64, 64), 'shulker_shell_textures.derived() - tier shulker box + Vanilla shulker head (split stage 8b-3)')},
    'simpleovens': {f'item/{tier}_furnace_minecart.png': ((16, 16), 'ovens_carts_2026_10_13.derived() - Vanilla minecart (N19)')
                    for tier in ('reinforced', 'netherite', 'enderite')},
    # simplecrucibles: one line (gradle/module-vanilla-derived.gradle reads its placeholder sizes from it).
    'simplecrucibles': {'block/enderite_crucible_side.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/enderite_crucible_top.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/enderite_barrel_side.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/enderite_barrel_top.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/enderite_barrel_top_open.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/enderite_barrel_bottom.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/enderite_barrel_flange.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla cauldron/barrel/netherite (split stage 5)'), 'block/soul_lava_still.png': ((16, 320), 'soul_lava_2026_10_09.derived() - Vanilla lava recolour (split stage 5b)'), 'block/soul_lava_flow.png': ((32, 512), 'soul_lava_2026_10_09.derived() - Vanilla lava recolour (split stage 5b)'), 'item/soul_lava_bucket.png': ((16, 16), 'soul_lava_2026_10_09.derived() - Vanilla lava bucket recolour (split stage 5b)'), 'item/copper_bucket_0.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_water_bucket_0.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_lava_bucket_0.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_bucket_1.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_water_bucket_1.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_lava_bucket_1.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_bucket_2.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_water_bucket_2.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_lava_bucket_2.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_bucket_3.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_water_bucket_3.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/copper_lava_bucket_3.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)'), 'item/enderite_bucket.png': ((16, 320), 'crucible_art_v2_2026_10_05.derived_module() - Vanilla bucket (split stage 5b)')},
    'simplebeacons': {'gui/container/strong_beacon.png': ((256, 288), 'strong_beacon_gui.derived() - Vanilla beacon GUI + 2 rows (T12)')},
}
MODULE_GENERATORS = {'simplemobs': ('mobs2_textures', 'wither_textures'), 'simpleovens': ('ovens_carts_2026_10_13',),
                     'simplestorage': ('storage_vehicles_2026_10_20', 'shulker_shell_textures'),
                     'simplebeacons': ('strong_beacon_gui',), 'simpleblocks': ('missing_blocks_2026_10_16', 'shapecutter_2026_10_21', 'blocks_b23_2026_10_23'), 'simplecrucibles': ('crucibles_derived_2026_10_16', 'soul_lava_2026_10_09')}
# Older generators with a derived(jar) -> {texture path: Image or PNG bytes} function.
OLDER_GENERATORS = ('auto_smither_gui', 'color_brush_2026_10_09', 'end_pistons_2026_10_03', 'end_rails_2026_10_04',
                    'fires_2026_10_11', 'silent_dandelion_2026_10_03',
                    'music_disc_textures', 'placed_egg_textures', 'standing_rods_2026_10_04', 'vehicles_2026_10_10',
                    'placed_display_3d_2026_10_16', 'sulfur_lights_2026_10_15')
# Probe file every usable jar contains.
PROBE = 'assets/minecraft/textures/entity/chest/ender.png'
# Shipped next to placeholder textures (assets/<ns>/<MARKER>, also by the Gradle fallbacks): the client logs an error
# and tools/textures/placeholder_audit.py reports the jar. Absent when the real textures were drawn.
MARKER = 'vanilla_derived_placeholders.txt'


def probe() -> str:
    """One line for the build's Python check: 'SBPY <major.minor> <missing pip packages>'. The build calls the script
    with --probe instead of python -c: Java on Windows does not escape quotes inside an argument (the -c code lost its
    quotes and every Python 'did not run' -> grey placeholders, owner 19.10.)."""
    missing = []
    for module, package in (('PIL', 'pillow'), ('numpy', 'numpy')):
        try:
            __import__(module)
        except ImportError:
            missing.append(package)
    return ' '.join(['SBPY', '%d.%d' % sys.version_info[:2]] + missing)


# Where the 26.3 loaders cache the client jar, relative to GRADLE_USER_HOME (the same layout on Windows, under
# %USERPROFILE%\.gradle): Fabric Loom, NeoForm runtime, ForgeGradle mavenizer.
CACHE_JARS = (('caches', 'fabric-loom', '26.3', 'minecraft-client.jar'),
              ('caches', 'neoformruntime', 'artifacts', 'minecraft_26.3_client.jar'),
              ('caches', 'minecraftforge', 'forgegradle', 'mavenizer', 'caches', 'minecraft_tasks', '26.3', 'client.jar'))


def jar_candidates(explicit: str | None, env: dict, home_dir: str, join=os.path.join) -> list[str]:
    """Candidate jar paths in search order. join is os.path.join; tests pass ntpath.join to check Windows paths.
    Last: the official launcher's 26.3 jar (%APPDATA%\\.minecraft on Windows, ~/.minecraft elsewhere)."""
    home = env.get('GRADLE_USER_HOME') or join(home_dir, '.gradle')
    launcher = [join(d, '.minecraft', 'versions', '26.3', '26.3.jar') for d in (env.get('APPDATA'), home_dir) if d]
    return ([c for c in (explicit, env.get('SB_CLIENT_JAR')) if c] + [join(home, *parts) for parts in CACHE_JARS]
            + launcher)


def find_jar(explicit: str | None = None) -> Path | None:
    """The 26.3 client jar: explicit, SB_CLIENT_JAR, or the Loom / NeoForm / ForgeGradle cache under
    GRADLE_USER_HOME (default ~/.gradle, on Windows %USERPROFILE%\\.gradle)."""
    for c in jar_candidates(explicit, os.environ, str(Path.home())):
        if not Path(c).is_file():
            continue
        try:
            with zipfile.ZipFile(c) as z:
                z.getinfo(PROBE)
            return Path(c)
        except (OSError, KeyError, zipfile.BadZipFile):
            continue
    return None


def extract_vanilla_textures(jar: Path) -> None:
    """build/vanilla-textures: the Vanilla textures as files, which the crucible/barrel/bucket generators read at
    import time (build/ is not committed). Extracted once."""
    base = ROOT / 'build/vanilla-textures'
    if (base / 'block/iron_block.png').exists():
        return
    prefix = 'assets/minecraft/textures/'
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.startswith(prefix) and name.endswith('.png'):
                target = base / name[len(prefix):]
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(name))


def module_images(jar: Path, module: str) -> dict:
    """Texture path (under assets/<module>/textures/) -> image for one module."""
    if str(HERE) not in sys.path:
        sys.path.append(str(HERE))
    out = {}
    if module == 'simplecrucibles':
        extract_vanilla_textures(jar)  # the crucible art reads the Vanilla textures as files at import time
    for name in MODULE_GENERATORS[module]:
        out.update(importlib.import_module(name).derived(jar))
    assert set(out) == set(MODULE_OUTPUTS[module]), f'MODULE_OUTPUTS[{module}] and its generators disagree'
    return out


def images(jar: Path) -> dict:
    """Texture path -> RGBA image, drawn from the given jar."""
    os.environ['SB_CLIENT_JAR'] = str(jar)
    if str(HERE) not in sys.path:
        sys.path.append(str(HERE))
    import n41_round3_2026_10_12 as n41
    import n46_round3_2026_10_12 as n46
    chests = n46.game_chests()
    out = {'item/blaze_book.png': n41.blaze_book(2),
           'entity/chest/astral_vault.png': chests['astral'],
           'entity/chest/nihil_vault.png': chests['nihil']}
    extract_vanilla_textures(jar)
    for name in OLDER_GENERATORS:
        out.update(importlib.import_module(name).derived(jar))
    assert set(out) == set(OUTPUTS), 'OUTPUTS and images() disagree'
    return out


# Wiki 3D renders (wiki/assets/textures/render/<name>.png, block renders as block/<name>, 128x128) that show a
# Vanilla-derived texture (or draw over a Mojang sprite): not committed, placeholders without the renderer.
WIKI_RENDERS = {'astral_vault': 128, 'nihil_vault': 128, 'enderite_barrel': 128, 'enderite_crucible': 128,
                'astral_piston': 128, 'nihil_piston': 128, 'jukebox_amplifier': 128, 'note_amplifier': 128,
                'enderite_furnace_minecart': 128, 'enderite_hopper_minecart': 128,
                'netherite_furnace_minecart': 128, 'netherite_hopper_minecart': 128,
                'reinforced_furnace_minecart': 128,
                'reinforced_hopper_minecart': 128, 'block/astral_rail': 128, 'block/nihil_rail': 128,
                'block/copper_fire': 128, 'block/sulfur_fire': 128, 'block/placed_egg': 128,
                'block/potted_silent_dandelion': 128, 'block/silent_dandelion': 128, 'block/standing_rod': 128,
                'block/sulfur_torch': 128, 'block/sulfur_wall_torch': 128, 'block/sulfur_lantern': 128,
                'block/sulfur_campfire': 128}


def placeholder_png(key: str, w: int, h: int) -> bytes:
    """A deterministic stand-in (no Mojang pixels, no Pillow needed): grey checker, one tint per key."""
    import struct
    import zlib
    tint = hashlib.sha256(key.encode()).digest()
    a = bytes((96 + tint[0] % 64, 96 + tint[1] % 64, 96 + tint[2] % 64, 255))
    b = bytes((a[0] // 2, a[1] // 2, a[2] // 2, 255))
    step = max(1, w // 8)
    raw = b''.join(b'\0' + b''.join(a if ((x // step) + (y // step)) % 2 else b for x in range(w)) for y in range(h))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


# Outputs that are byte-identical in the real set (one texture for all six faces): their placeholders must be
# identical too, or the wiki texture index (which merges equal copies) would differ between jar and no-jar runs.
SAME_AS = {'block/jukebox_amplifier_top.png': 'block/jukebox_amplifier_side.png',
           'block/note_amplifier_top.png': 'block/note_amplifier_side.png',
           # Same pixels as the simpleovens module copies (the wiki merges equal copies, jar and placeholder alike).
           **{f'item/{t}_furnace_minecart.png': f'simpleovens:item/{t}_furnace_minecart.png'
              for t in ('reinforced', 'netherite', 'enderite')}}


def placeholder(rel: str, module: str | None = None) -> bytes:
    (w, h), _ = (MODULE_OUTPUTS[module] if module else OUTPUTS)[rel]
    return placeholder_png(f'{module}:{rel}' if module else SAME_AS.get(rel, rel), w, h)


def write(out_root: Path, jar: Path | None, placeholders: bool = False, module: str | None = None) -> str:
    """Writes assets/<simplebuilding or module>/textures/<rel> under out_root; returns 'jar' or 'placeholder'."""
    if jar is None and not placeholders:
        tried = '\n  '.join(jar_candidates(None, os.environ, str(Path.home())))
        raise SystemExit('vanilla_derived: 26.3 client jar not found. Searched:\n  ' + tried + '\nRun any 26.3 '
                         'build once (e.g. gradlew :mc26_3:fabric:compileJava), or pass -PsbClientJar=<path> / '
                         'SB_CLIENT_JAR.')
    outputs = MODULE_OUTPUTS[module] if module else OUTPUTS
    drawn = (module_images(jar, module) if module else images(jar)) if jar is not None else None
    art = {rel: _png(im) for rel, im in drawn.items()} if drawn is not None else \
        {rel: placeholder(rel, module) for rel in outputs}
    base = out_root / 'assets' / (module or NS) / 'textures'
    for rel, data in art.items():
        p = base / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        if not p.exists() or p.read_bytes() != data:
            p.write_bytes(data)
    # Leftovers of an older checkout (an entry that left OUTPUTS) would become texture entries of the wiki index.
    for old in base.rglob('*.png') if base.is_dir() else ():
        if old.relative_to(base).as_posix() not in art:
            old.unlink()
    marker = out_root / 'assets' / (module or NS) / MARKER
    if jar is None:
        marker.write_text('vanilla_derived.py --placeholder-if-missing: no 26.3 client jar or no Pillow/numpy\n',
                          encoding='utf-8')
    elif marker.exists():
        marker.unlink()
    return 'jar' if jar is not None else 'placeholder'


def _png(im) -> bytes:
    if isinstance(im, bytes):  # a generator that encodes its own PNG bytes (e.g. optimized)
        return im
    import io
    buf = io.BytesIO()
    im.save(buf, format='PNG')
    return buf.getvalue()


def ensure_local() -> tuple[Path, str]:
    """For local tools (wiki): the set under build/vanilla-derived/resources; placeholders without a jar or Pillow."""
    try:
        import PIL  # noqa: F401
        jar = find_jar()
    except ImportError:
        jar = None
    mode = write(LOCAL_OUT, jar, placeholders=True)
    for module in MODULE_OUTPUTS:
        write(LOCAL_OUT, jar, placeholders=True, module=module)
    return LOCAL_OUT, mode


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path)
    ap.add_argument('--jar')
    ap.add_argument('--placeholder-if-missing', action='store_true')
    ap.add_argument('--list', action='store_true')
    ap.add_argument('--module', choices=sorted(MODULE_OUTPUTS), help='only this module\'s textures (its own jars)')
    ap.add_argument('--probe', action='store_true', help='print "SBPY <version> <missing packages>" (the build\'s check)')
    a = ap.parse_args()
    if a.probe:
        print(probe())
        return
    if a.list or not a.out:
        for rel, (size, how) in OUTPUTS.items():
            print(f'assets/{NS}/textures/{rel}  {size[0]}x{size[1]}  {how}')
        for module, outputs in MODULE_OUTPUTS.items():
            for rel, (size, how) in outputs.items():
                print(f'assets/{module}/textures/{rel}  {size[0]}x{size[1]}  {how}')
        return
    jar = find_jar(a.jar)
    if a.jar and jar is None:
        raise SystemExit(f'vanilla_derived: --jar {a.jar} is not a 26.3 client jar (no {PROBE})')
    try:
        import numpy  # noqa: F401
        import PIL  # noqa: F401
    except ImportError as e:
        if jar is not None and not a.placeholder_if_missing:
            raise SystemExit(f'vanilla_derived: {e}. Install: "{sys.executable}" -m pip install pillow numpy')
        jar = None  # placeholders need neither
    mode = write(a.out, jar, a.placeholder_if_missing, a.module)
    count = len(MODULE_OUTPUTS[a.module] if a.module else OUTPUTS)
    print(f'vanilla_derived: {count} textures ({mode}{", " + str(jar) if jar else ""}) -> {a.out}')


if __name__ == '__main__':
    main()
