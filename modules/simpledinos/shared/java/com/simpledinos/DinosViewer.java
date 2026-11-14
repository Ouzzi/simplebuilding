package com.simpledinos;

import com.simplelib.api.RecipeViewer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Dinosaurs in the recipe viewer (PLAN40 simpledinos-4): one info page on the fossil block and the eggs - the way
 * fossil, brush, egg, brooding and hatchling. Eggs that do not exist (yet) are skipped. Shown by SimpleLib's JEI plugin.
 */
public final class DinosViewer {
    public static final String INFO = "jei.simpledinos.info.";

    private DinosViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimpleDinos.MOD_ID, DinosViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        List<ItemStack> stacks = new ArrayList<>();
        for (String path : List.of("fossil_block", "trex_egg", "triceratops_egg", "stegosaurus_egg", "utahraptor_egg", "parasaurolophus_egg")) {
            Item item = BuiltInRegistries.ITEM.getValue(SimpleDinos.id(path));
            if (item != null && item != Items.AIR) stacks.add(new ItemStack(item));
        }
        return stacks.isEmpty() ? List.of() : List.of(new RecipeViewer.Info(stacks, List.of(Component.translatable(INFO + "egg_path"))));
    }
}
