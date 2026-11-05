package com.simplepads;

import java.util.List;

/** The ids that moved from {@code simplebuilding:} to {@code simplepads:} on 04.11.2026 (paths unchanged; {@link SimplePads#registerIdMoves}). */
final class PadsIds {
    static final List<String> BLOCKS = List.of(
            "spawn_teleporter", "spawn_teleporter_tier_2", "spawn_teleporter_tier_3", "spawn_teleporter_tier_4",
            "enderite_spawn_teleporter", "launchpad", "netherite_launchpad", "enderite_launchpad", "diamond_pressure_plate",
            "netherite_pressure_plate", "enderite_pressure_plate", "elytra_pad", "reinforced_elytra_pad",
            "netherite_elytra_pad", "enderite_elytra_pad", "fine_elytra_pad", "flypad", "reinforced_flypad", "stellar_flypad",
            "netherite_flypad", "enderite_flypad", "chunk_loader", "netherite_chunk_loader", "enderite_chunk_loader",
            "potion_pad", "reinforced_potion_pad", "infused_potion_pad", "copper_pressure_plate",
            "exposed_copper_pressure_plate", "weathered_copper_pressure_plate", "oxidized_copper_pressure_plate",
            "waxed_copper_pressure_plate", "waxed_exposed_copper_pressure_plate", "waxed_weathered_copper_pressure_plate",
            "waxed_oxidized_copper_pressure_plate");
    static final List<String> BLOCK_ENTITY_TYPES = List.of(
            "spawn_teleporter_be", "launchpad_be", "elytra_pad_be", "flypad_be", "chunk_loader_be", "copper_pressure_plate_be",
            "netherite_pressure_plate_be", "potion_pad_be");
    static final List<String> COMPONENTS = List.of(
            "flight_time", "boost_level", "last_pad_tick", "is_safe_elytra", "potion_pad_cooldown", "easter_stage");
    static final List<String> RECIPES = List.of(
            "chunk_loader_smithing", "copper_pressure_plate", "diamond_pressure_plate", "easter/chunk_loader_funny_stick",
            "easter/chunk_loader_stage_1", "easter/chunk_loader_stage_2", "easter/chunk_loader_stage_3",
            "easter/elytra_pad_funny_stick", "easter/elytra_pad_stage_1", "easter/elytra_pad_stage_2",
            "easter/elytra_pad_stage_3", "easter/flypad_funny_stick", "easter/flypad_stage_1", "easter/flypad_stage_2",
            "easter/flypad_stage_3", "easter/launchpad_funny_stick", "easter/launchpad_stage_1", "easter/launchpad_stage_2",
            "easter/launchpad_stage_3", "easter/potion_pad_funny_stick", "easter/potion_pad_stage_1",
            "easter/potion_pad_stage_2", "easter/potion_pad_stage_3", "easter/spawn_teleporter_funny_stick",
            "easter/spawn_teleporter_stage_1", "easter/spawn_teleporter_stage_2", "easter/spawn_teleporter_stage_3",
            "elytra_pad_crafting", "enderite_chunk_loader_smithing", "enderite_elytra_pad_smithing",
            "enderite_launchpad_smithing", "enderite_pressure_plate_smithing", "enderite_spawn_teleporter_smithing",
            "flypad_tier1_crafting", "flypad_tier2_smithing", "infused_potion_pad_smithing", "launchpad_smithing",
            "netherite_chunk_loader_smithing", "netherite_elytra_pad_smithing", "netherite_launchpad_smithing",
            "netherite_pressure_plate_smithing", "potion_pad_smithing", "reinforced_potion_pad_smithing",
            "spawn_teleporter_smithing", "spawn_teleporter_tier2_smithing", "stellar_flypad_smithing",
            "waxed_copper_pressure_plate_from_honeycomb", "waxed_exposed_copper_pressure_plate_from_honeycomb",
            "waxed_oxidized_copper_pressure_plate_from_honeycomb", "waxed_weathered_copper_pressure_plate_from_honeycomb");
    static final List<String> ADVANCEMENTS = List.of(
            "easter/all_that_for_a_stick", "easter/it_was_worth_it", "easter/seriously", "easter/what_have_you_done",
            "pads/always_loaded", "pads/bottoms_up", "pads/copper_plated", "pads/fine_feathers", "pads/fly_me_to_the_moon",
            "pads/frequent_traveller", "pads/higher_ground", "pads/home_sweet_spawn", "pads/infusion", "pads/liftoff",
            "pads/stellar", "pads/under_pressure", "pads/wings_on_loan", "recipes/redstone/copper_pressure_plate",
            "recipes/redstone/diamond_pressure_plate", "recipes/redstone/waxed_copper_pressure_plate_from_honeycomb",
            "recipes/redstone/waxed_exposed_copper_pressure_plate_from_honeycomb",
            "recipes/redstone/waxed_oxidized_copper_pressure_plate_from_honeycomb",
            "recipes/redstone/waxed_weathered_copper_pressure_plate_from_honeycomb", "recipes/tools/chunk_loader_smithing",
            "recipes/tools/elytra_pad_crafting", "recipes/tools/enderite_chunk_loader_smithing",
            "recipes/tools/enderite_elytra_pad_smithing", "recipes/tools/enderite_launchpad_smithing",
            "recipes/tools/enderite_pressure_plate_smithing", "recipes/tools/enderite_spawn_teleporter_smithing",
            "recipes/tools/flypad_tier1_crafting", "recipes/tools/flypad_tier2_smithing",
            "recipes/tools/infused_potion_pad_smithing", "recipes/tools/launchpad_smithing",
            "recipes/tools/netherite_chunk_loader_smithing", "recipes/tools/netherite_elytra_pad_smithing",
            "recipes/tools/netherite_launchpad_smithing", "recipes/tools/netherite_pressure_plate_smithing",
            "recipes/tools/potion_pad_smithing", "recipes/tools/reinforced_potion_pad_smithing",
            "recipes/tools/spawn_teleporter_smithing", "recipes/tools/spawn_teleporter_tier2_smithing",
            "recipes/tools/stellar_flypad_smithing");

    private PadsIds() {
    }
}
