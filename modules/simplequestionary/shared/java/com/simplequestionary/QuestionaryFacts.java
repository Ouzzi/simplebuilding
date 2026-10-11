package com.simplequestionary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

/**
 * The automatic line under the "Use" page: read from the server's RecipeManager (clients do not have every recipe),
 * so it never goes stale - what the item is made from (the first recipe that makes it), how many recipes make it and
 * in how many recipes it is an ingredient.
 */
public final class QuestionaryFacts {
    /** At most this many ingredient icons. */
    public static final int MAX_INGREDIENTS = 9;

    public record Facts(Identifier item, List<Identifier> madeFrom, int makes, int usedIn) {
        public static final Facts NONE = new Facts(BuiltInRegistries.ITEM.getKey(net.minecraft.world.item.Items.AIR), List.of(), 0, 0);
    }

    public static Facts compute(Level level, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (stack.isEmpty() || server == null) return Facts.NONE;
        ContextMap context = SlotDisplayContext.fromLevel(level);
        Set<Identifier> madeFrom = new LinkedHashSet<>();
        int makes = 0, usedIn = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            List<Ingredient> ingredients;
            try {
                ingredients = recipe.placementInfo().ingredients();
            } catch (RuntimeException e) {
                ingredients = List.of();
            }
            if (makes(recipe, stack, context)) {
                if (makes++ == 0) {
                    for (Ingredient ingredient : ingredients) {
                        ingredient.items().findFirst().map(Holder::value).map(BuiltInRegistries.ITEM::getKey)
                                .filter(id -> madeFrom.size() < MAX_INGREDIENTS).ifPresent(madeFrom::add);
                    }
                }
            }
            for (Ingredient ingredient : ingredients) {
                if (ingredient.test(stack)) {
                    usedIn++;
                    break;
                }
            }
        }
        return new Facts(BuiltInRegistries.ITEM.getKey(stack.getItem()), new ArrayList<>(madeFrom), makes, usedIn);
    }

    private static boolean makes(Recipe<?> recipe, ItemStack stack, ContextMap context) {
        try {
            for (RecipeDisplay display : recipe.display()) {
                for (ItemStack result : display.result().resolveForStacks(context)) {
                    if (result.is(stack.getItem())) return true;
                }
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }

    private QuestionaryFacts() {}
}
