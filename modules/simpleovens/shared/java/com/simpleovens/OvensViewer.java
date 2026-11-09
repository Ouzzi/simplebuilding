package com.simpleovens;

import com.simplelib.api.RecipeViewer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Ovens in the recipe viewer (b-fix09a): one info page on all nine upgraded furnaces, smokers and blast furnaces -
 * what the tiers change (SimpleBuilding's {@code jei.simplebuilding.info.furnace_tiers} before the split, now
 * {@code jei.simpleovens.info.furnace_tiers}, so it shows without SimpleBuilding too). Shown by SimpleLib's JEI plugin.
 */
public final class OvensViewer {
    public static final String INFO = "jei.simpleovens.info.";

    private OvensViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimpleOvens.MOD_ID, OvensViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        List<ItemStack> stacks = new ArrayList<>();
        for (String tier : List.of("reinforced", "netherite", "enderite")) {
            for (String kind : List.of("furnace", "smoker", "blast_furnace")) {
                Item item = BuiltInRegistries.ITEM.getValue(SimpleOvens.id(tier + "_" + kind));
                if (item != null && item != Items.AIR) stacks.add(new ItemStack(item));
            }
        }
        return stacks.isEmpty() ? List.of() : List.of(new RecipeViewer.Info(stacks, List.of(Component.translatable(INFO + "furnace_tiers"))));
    }
}
