package com.simplebuildingtools;

import com.simplelib.api.RecipeViewer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Building Tools in the recipe viewer (b-fix09a): the creative building wand and the creative blueprint have no
 * recipe, one info page explains them (SimpleBuilding's {@code jei.simplebuilding.info.creative_tools} before the split, now
 * {@code jei.simplebuildingtools.info.creative_tools}, so it shows without SimpleBuilding too). Shown by SimpleLib's JEI plugin.
 */
public final class BtViewer {
    public static final String INFO = "jei.simplebuildingtools.info.";

    private BtViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimpleBuildingTools.MOD_ID, BtViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        List<ItemStack> stacks = new ArrayList<>();
        for (String path : List.of("creative_building_wand", "creative_blueprint")) {
            Item item = BuiltInRegistries.ITEM.getValue(SimpleBuildingTools.id(path));
            if (item != null && item != Items.AIR) stacks.add(new ItemStack(item));
        }
        return stacks.isEmpty() ? List.of() : List.of(new RecipeViewer.Info(stacks, List.of(Component.translatable(INFO + "creative_tools"))));
    }
}
