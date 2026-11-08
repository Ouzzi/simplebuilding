package com.simpleores;

import com.simplelib.api.RecipeViewer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Simple Ores in the recipe viewer (JEI24, a-fix09b): info pages for Sage Ore, the Sage Orb and Dimensional Scrap - only
 * world generation and loot give them (SimpleBuilding's pages before the split, now {@code jei.simpleores.info.*}). Free
 * of JEI classes, shown by SimpleLib's JEI plugin. Registered from {@link SimpleOres#init}.
 */
public final class OresViewer {
    public static final String INFO = "jei.simpleores.info.";

    private OresViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimpleOres.MOD_ID, OresViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        List<RecipeViewer.Info> out = new ArrayList<>();
        page(out, "sage_ore", OresRegistry.SAGE_ORE, OresRegistry.DEEPSLATE_SAGE_ORE);
        page(out, "sage_orb", OresRegistry.item("sage_orb"));
        page(out, "dimensional_scrap", OresRegistry.DIMENSIONAL_SCRAP, OresRegistry.NETHER_DIMENSIONAL_SCRAP, OresRegistry.END_DIMENSIONAL_SCRAP);
        return out;
    }

    private static void page(List<RecipeViewer.Info> out, String key, ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemLike item : items) {
            if (item != null && !new ItemStack(item).isEmpty()) stacks.add(new ItemStack(item));
        }
        if (!stacks.isEmpty()) out.add(new RecipeViewer.Info(stacks, List.of(Component.translatable(INFO + key))));
    }
}
