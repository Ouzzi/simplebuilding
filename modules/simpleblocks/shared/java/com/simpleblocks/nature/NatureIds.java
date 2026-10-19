package com.simpleblocks.nature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ids of the blocks moved from SimpleBuilding, without touching {@link NatureBlocks}: the loaders register the id moves
 * in their constructor, before the registry events, and creating the blocks there would hit frozen registries (NeoForge,
 * Forge). {@link NatureBlocks#NAMES} must equal {@link #NAMES} (module test {@code moved_blocks_registered}).
 */
public final class NatureIds {
    /** Block paths in registration (and tab) order. */
    public static final List<String> NAMES = List.of("suspended_sand", "suspended_gravel", "levitating_sand", "levitating_gravel",
            "dirt_slab", "grass_slab", "sand_slab", "gravel_slab", "cracked_ice", "chiseled_packed_ice", "chiseled_blue_ice",
            "nautilus_shell_block", "scarlet_froglight", "aqua_froglight", "azure_froglight");
    /** The rising block's entity type path. */
    public static final String LEVITATING_ENTITY = "levitating_block";
    /** Recipe ids that came from SimpleBuilding (recipe book) -> folder of their unlock advancement {@code recipes/<folder>/<path>}. */
    public static final Map<String, String> MOVED_RECIPES = movedRecipes();

    private NatureIds() {}

    private static Map<String, String> movedRecipes() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String path : List.of("suspended_sand", "suspended_gravel", "levitating_sand", "levitating_gravel", "dirt_slab", "grass_slab",
                "sand_slab", "gravel_slab", "cracked_ice_from_ice_stonecutting", "chiseled_packed_ice_from_packed_ice_stonecutting",
                "chiseled_blue_ice_from_blue_ice_stonecutting", "nautilus_shell_block", "scarlet_froglight", "aqua_froglight", "azure_froglight")) {
            out.put(path, "building_blocks");
        }
        out.put("nautilus_shell_from_nautilus_shell_block", "misc");
        return Map.copyOf(out);
    }
}
