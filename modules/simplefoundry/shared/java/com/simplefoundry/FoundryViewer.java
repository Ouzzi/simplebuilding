package com.simplefoundry;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Simple Foundry in the recipe viewer's "Special Ways" tab (SF-2, PLAN40): what the foundry melts and casts. The melts are a built-in
 * table ({@link Melts}), no recipe files, so JEI cannot know them. One entry per metal and input size (ore/ingot, nuggets, blocks):
 * input -> the desk, heat and fuel in the notes -> the cast ingots. Free of JEI classes (SimpleLib shows it).
 */
public final class FoundryViewer {
    private FoundryViewer() {}

    public static void register() {
        RecipeViewer.special(SimpleFoundry.MOD_ID, FoundryViewer::entries);
    }

    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        Item desk = FoundryRegistry.ITEMS.get("foundry_desk");
        if (desk == null) return out;
        for (Metal metal : Metal.values()) {
            // size = units per input item; the output is the number of ingots the units cast to (9 units = 1 ingot)
            int[][] shapes = {{Metal.INGOT, 1, 1}, {1, Metal.INGOT, 1}, {Metal.INGOT * 9, 1, 9}};
            String[] names = {"ore", "nugget", "block"};
            for (int i = 0; i < shapes.length; i++) {
                List<Item> inputs = Melts.items(metal, shapes[i][0]);
                if (inputs.isEmpty()) continue;
                out.add(new InWorldUpgrade(SimpleFoundry.MOD_ID + "/melt_" + metal.id + "_" + names[i],
                        List.of(new InWorldUpgrade.Input(inputs, shapes[i][1])), List.of(desk), new ItemStack(metal.ingot(), shapes[i][2]),
                        List.of(Component.translatable("jei.simplefoundry.note.melt"),
                                Component.translatable("jei.simplefoundry.note.heat." + metal.heat.name().toLowerCase(java.util.Locale.ROOT)),
                                Component.translatable("jei.simplefoundry.note.cast"))));
            }
        }
        return out;
    }
}
