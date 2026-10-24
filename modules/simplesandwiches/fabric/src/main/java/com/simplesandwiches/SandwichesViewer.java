package com.simplesandwiches;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplesandwiches.block.SliceBlock;
import com.simplesandwiches.config.SandwichConfig;
import com.simplesandwiches.item.KnifeItem;
import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Sandwiches in the recipe viewer (JEI24), tab "Special Ways": milk ripening into butter and cheese in a cauldron
 * (was SimpleBuilding's "Cauldron and Crucible" tab), the knife's slices and the sandwich on the cutting board. Free of
 * JEI classes; registered from {@code SandwichesFabric} (Fabric only: the shared code meets SimpleLib only by id, here
 * the Fabric build compiles against it).
 */
public final class SandwichesViewer {
    public static final String NOTE = "jei.simplesandwiches.note.";

    private SandwichesViewer() {
    }

    public static void register() {
        RecipeViewer.special(Sandwiches.MOD_ID, SandwichesViewer::entries);
    }

    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        if (ModItems.BUTTER_BLOCK == null || ModItems.KNIFE == null) return out;
        List<InWorldUpgrade.Input> milk = List.of(InWorldUpgrade.Input.of(Items.CAULDRON, 1), InWorldUpgrade.Input.of(Items.MILK_BUCKET, 1));
        out.add(new InWorldUpgrade("simplesandwiches/milk_butter", milk, List.of(), new ItemStack(ModItems.BUTTER_BLOCK),
                List.of(Component.translatable(NOTE + "milk_butter", seconds(SandwichConfig.butterTicks)),
                        Component.translatable(NOTE + "milk_take"))));
        out.add(new InWorldUpgrade("simplesandwiches/milk_cheese", milk, List.of(), new ItemStack(ModItems.CHEESE_BLOCK),
                List.of(Component.translatable(NOTE + "milk_cheese", seconds(SandwichConfig.butterTicks + SandwichConfig.cheeseTicks)),
                        Component.translatable(NOTE + "milk_take"))));
        List<Item> knife = List.of(ModItems.KNIFE);
        slice(out, "cheese_slices", ModItems.CHEESE_BLOCK, ModItems.CHEESE_SLICE, SliceBlock.MAX, knife);
        slice(out, "butter_slices", ModItems.BUTTER_BLOCK, ModItems.BUTTER_SLICE, SliceBlock.MAX, knife);
        slice(out, "melon_slices", Items.MELON, Items.MELON_SLICE, KnifeItem.MELON_SLICES, knife);
        slice(out, "cake_slices", Items.CAKE, ModItems.CAKE_SLICE, ModItems.CAKE_SLICE_STACK, knife);
        List<Item> boards = RecipeViewer.tagged(ModTags.CUTTING_BOARD_ITEMS);
        List<Item> ingredients = RecipeViewer.tagged(ModTags.SANDWICH_INGREDIENTS);
        if (!boards.isEmpty() && !ingredients.isEmpty() && ModItems.SANDWICH != null) {
            out.add(new InWorldUpgrade("simplesandwiches/sandwich",
                    List.of(InWorldUpgrade.Input.of(Items.BREAD, 1), InWorldUpgrade.Input.of(ModItems.BUTTER_SLICE, 1),
                            new InWorldUpgrade.Input(ingredients, 1)),
                    boards, new ItemStack(ModItems.SANDWICH),
                    List.of(Component.translatable(NOTE + "sandwich.open"),
                            Component.translatable(NOTE + "sandwich.fill", SandwichConfig.maxIngredients),
                            Component.translatable(NOTE + "sandwich.close"))));
        }
        return out;
    }

    private static void slice(List<InWorldUpgrade> out, String id, Item from, Item to, int count, List<Item> knife) {
        if (from == null || to == null) return;
        out.add(new InWorldUpgrade("simplesandwiches/" + id, List.of(InWorldUpgrade.Input.of(from, 1)), knife, new ItemStack(to, count),
                List.of(Component.translatable(NOTE + "knife"))));
    }

    private static String seconds(int ticks) {
        return Integer.toString(Math.round(ticks / 20.0F));
    }
}
