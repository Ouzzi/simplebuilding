package com.simplebuilding.modules.simpletweaks.deed;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Tweaks in the recipe viewer's "Special Ways" tab (JEI24): copying a claim deed in the crafting grid
 * ({@link DeedCloningRecipe}, a special recipe JEI cannot show by itself). Free of JEI classes.
 */
public final class DeedViewer {
    private DeedViewer() {
    }

    public static void register() {
        RecipeViewer.special("simpletweaks", DeedViewer::entries);
    }

    public static List<InWorldUpgrade> entries() {
        Item deed = ClaimDeedItem.item();
        if (deed == null || deed == Items.AIR) return List.of();
        return List.of(new InWorldUpgrade("simpletweaks/claim_deed_copy",
                List.of(InWorldUpgrade.Input.of(deed, 1), InWorldUpgrade.Input.of(deed, 1)), List.of(Items.CRAFTING_TABLE),
                new ItemStack(deed), List.of(Component.translatable("jei.simpletweaks.note.deed_copy"),
                        Component.translatable("jei.simpletweaks.note.deed_copy_once"))));
    }
}
