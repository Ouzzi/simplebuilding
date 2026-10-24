package com.simpletrims.placed;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Placed trim templates as 2 px thick stepped models instead of the punched item plate (owner 2026-10-15, D1): the models
 * {@code simpletrims:block/placed_<item path>} and item definitions {@code simpletrims:placed_<item path>} come from
 * {@code tools/textures/placed_display_3d_2026_10_16.py} (this module's own copy of the geometry, no reference to
 * SimpleBuilding). Templates of other mods keep the flat plate. The hammer switches one back ({@code PlacedTrimTemplateBlock#FLAT}).
 */
public final class PlacedSolids {
    private static final List<String> TRIMS = List.of("bolt", "coast", "dune", "eye", "flow", "host", "raiser", "rib", "sentry",
            "shaper", "silence", "snout", "spire", "tide", "vex", "ward", "wayfinder", "wild");
    /** Item ids that have a 3D model. */
    public static final Set<String> IDS;

    static {
        List<String> ids = new ArrayList<>();
        for (String trim : TRIMS) {
            ids.add("minecraft:" + trim + "_armor_trim_smithing_template");
        }
        for (String own : List.of("glowing", "emitting", "pulsating", "inverting")) {
            ids.add("simpletrims:" + own + "_trim_template");
        }
        IDS = Set.copyOf(ids);
    }

    private PlacedSolids() {
    }

    public static boolean has(Item item) {
        return IDS.contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    public static boolean has(ItemStack stack) {
        return !stack.isEmpty() && has(stack.getItem());
    }

    /** The item definition that draws {@code item} in 3D. */
    public static Identifier modelId(Item item) {
        return Identifier.fromNamespaceAndPath("simpletrims", "placed_" + BuiltInRegistries.ITEM.getKey(item).getPath());
    }
}
