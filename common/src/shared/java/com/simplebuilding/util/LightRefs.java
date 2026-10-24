package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The Light Panes that left SimpleBuilding, by id (WB24-6, docs/ai/PLAN-B-AUD5-2026-10-24.md): the 37 panes belong to the
 * required, bundled sub-mod Simple Lights ({@code simplelights:*}, 26.3 only, {@link McVersion#LIGHT_PANES}) and
 * SimpleBuilding imports none of its classes (principle 3): tabs, the search tab, the guide, the test centre and the tests
 * find them in the registry. Call after registration; an unregistered id gives air (empty lists without Light Panes).
 */
public final class LightRefs {
    private LightRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.LIGHTS_NAMESPACE, path);
    }

    /** All 37 pane paths in tab order: normal, transparent, the sixteen colors, redstone, copper, the sixteen copper colors, copper redstone. */
    public static List<String> paths() {
        if (!McVersion.LIGHT_PANES) {
            return List.of();
        }
        List<String> out = new ArrayList<>(List.of("light_pane", "transparent_light_pane"));
        for (DyeColor dye : DyeColor.values()) {
            out.add(dye.getSerializedName() + "_light_pane");
        }
        out.add("redstone_light_pane");
        out.add("copper_light_pane");
        for (DyeColor dye : DyeColor.values()) {
            out.add(dye.getSerializedName() + "_copper_light_pane");
        }
        out.add("copper_redstone_light_pane");
        return List.copyOf(out);
    }

    /** The pane items in {@link #paths()} order (index 0 normal, 1 transparent, 2-17 colors, 18 redstone, 19 copper, 20-35 copper colors, 36 copper redstone). */
    public static List<Item> items() {
        return paths().stream().map(path -> BuiltInRegistries.ITEM.getValue(id(path))).toList();
    }

    public static List<Block> blocks() {
        return paths().stream().map(path -> BuiltInRegistries.BLOCK.getValue(id(path))).toList();
    }

    /** True for every Light Pane block (all hang flat on the block behind them, {@code facing} = away from it). */
    public static boolean isPane(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        return McVersion.LIGHT_PANES && key.getNamespace().equals(McVersion.LIGHTS_NAMESPACE) && key.getPath().endsWith("light_pane");
    }
}
