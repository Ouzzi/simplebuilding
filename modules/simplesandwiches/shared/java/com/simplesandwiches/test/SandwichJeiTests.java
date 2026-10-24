package com.simplesandwiches.test;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.registry.ModItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;

/** JEI24: Simple Sandwiches' special ways in the recipe viewer (milk, knife, board), also alone. */
public final class SandwichJeiTests {
    private SandwichJeiTests() {
    }

    public static void specialWays(GameTestHelper h) {
        List<InWorldUpgrade> entries = RecipeViewer.special(Sandwiches.MOD_ID);
        h.assertTrue(has(entries, "simplesandwiches/milk_butter", ModItems.BUTTER_BLOCK) && has(entries, "simplesandwiches/milk_cheese", ModItems.CHEESE_BLOCK),
                "milk ripening: " + ids(entries));
        h.assertTrue(has(entries, "simplesandwiches/cheese_slices", ModItems.CHEESE_SLICE) && has(entries, "simplesandwiches/butter_slices", ModItems.BUTTER_SLICE)
                && has(entries, "simplesandwiches/melon_slices", Items.MELON_SLICE) && has(entries, "simplesandwiches/cake_slices", ModItems.CAKE_SLICE),
                "knife slices: " + ids(entries));
        InWorldUpgrade sandwich = entries.stream().filter(e -> e.id().equals("simplesandwiches/sandwich")).findFirst().orElse(null);
        h.assertTrue(sandwich != null && sandwich.output().is(ModItems.SANDWICH) && !sandwich.tools().isEmpty()
                && !sandwich.inputs().get(2).items().isEmpty(), "board sandwich: " + sandwich);
        h.succeed();
    }

    private static boolean has(List<InWorldUpgrade> entries, String id, net.minecraft.world.item.Item output) {
        return entries.stream().anyMatch(e -> e.id().equals(id) && e.output().is(output));
    }

    private static List<String> ids(List<InWorldUpgrade> entries) {
        return entries.stream().map(InWorldUpgrade::id).toList();
    }
}
