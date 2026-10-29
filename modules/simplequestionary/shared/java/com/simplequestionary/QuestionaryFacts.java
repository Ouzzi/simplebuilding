package com.simplequestionary;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

/**
 * The recipe side of the book's "Use" page: read from the server's RecipeManager (clients do not have every recipe),
 * so it never goes stale - the recipe with the most different ingredients as a 3x3 grid in its shape (shaped recipes
 * keep their pattern, shapeless ones fill row by row, a furnace recipe sits in the middle), the station it is made at
 * (crafting table, furnace ...), how many recipes make the item and in how many recipes it is an ingredient.
 */
public final class QuestionaryFacts {
    /** Grid cells (3x3, row by row; air = empty cell). */
    public static final int MAX_INGREDIENTS = 9;
    private static final Identifier AIR = BuiltInRegistries.ITEM.getKey(Items.AIR);

    /** {@code madeFrom} is empty (not crafted) or the 3x3 grid; {@code station} is air when the recipe names none. */
    public record Facts(Identifier item, List<Identifier> madeFrom, Identifier station, int makes, int usedIn) {
        public static final Facts NONE = new Facts(AIR, List.of(), AIR, 0, 0);
    }

    private record Key(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentPatch patch) {}

    private static net.minecraft.world.item.crafting.RecipeManager cachedFor;
    private static final java.util.Map<Key, Facts> CACHE = new java.util.HashMap<>();

    /** REV2-C-3: one pass over all recipes per item until the recipes are reloaded (a new RecipeManager), not per slot change. */
    public static synchronized Facts compute(Level level, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (stack.isEmpty() || server == null) return Facts.NONE;
        var manager = server.getRecipeManager();
        if (manager != cachedFor || CACHE.size() > 512) {
            CACHE.clear();
            cachedFor = manager;
        }
        return CACHE.computeIfAbsent(new Key(stack.getItem(), stack.getComponentsPatch()), k -> scan(level, stack, server));
    }

    private static Facts scan(Level level, ItemStack stack, MinecraftServer server) {
        ContextMap context = SlotDisplayContext.fromLevel(level);
        List<Identifier> madeFrom = List.of();
        Identifier station = AIR;
        int best = 0, makes = 0, usedIn = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            List<Ingredient> ingredients;
            try {
                ingredients = recipe.placementInfo().ingredients();
            } catch (RuntimeException e) {
                ingredients = List.of();
            }
            RecipeDisplay display = displayMaking(recipe, stack, context);
            if (display != null) {
                makes++;
                List<Identifier> grid = grid(display, ingredients, context);
                // The recipe with the most different ingredients tells the most (not "block -> 9 ingots").
                int distinct = distinct(grid);
                if (distinct > best) {
                    best = distinct;
                    madeFrom = grid;
                    station = key(first(display.craftingStation(), context));
                }
            }
            for (Ingredient ingredient : ingredients) {
                if (ingredient.test(stack)) {
                    usedIn++;
                    break;
                }
            }
        }
        return new Facts(BuiltInRegistries.ITEM.getKey(stack.getItem()), madeFrom, station, makes, usedIn);
    }

    /** The 3x3 grid of one recipe display: the shape of a shaped recipe, else the ingredients row by row. */
    public static List<Identifier> grid(RecipeDisplay display, List<Ingredient> ingredients, ContextMap context) {
        Identifier[] cells = new Identifier[MAX_INGREDIENTS];
        Arrays.fill(cells, AIR);
        if (display instanceof ShapedCraftingRecipeDisplay shaped && shaped.width() <= 3 && shaped.height() <= 3) {
            for (int r = 0; r < shaped.height(); r++) {
                for (int c = 0; c < shaped.width(); c++) cells[r * 3 + c] = key(first(shaped.ingredients().get(r * shaped.width() + c), context));
            }
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            for (int i = 0; i < Math.min(MAX_INGREDIENTS, shapeless.ingredients().size()); i++) cells[i] = key(first(shapeless.ingredients().get(i), context));
        } else if (display instanceof FurnaceRecipeDisplay furnace) {
            cells[4] = key(first(furnace.ingredient(), context));
        } else {
            for (int i = 0; i < Math.min(MAX_INGREDIENTS, ingredients.size()); i++) {
                cells[i] = ingredients.get(i).items().findFirst().map(Holder::value).map(BuiltInRegistries.ITEM::getKey).orElse(AIR);
            }
        }
        return List.of(cells);
    }

    /** Different items in a grid (air not counted). */
    public static int distinct(List<Identifier> grid) {
        Set<Identifier> seen = new HashSet<>(grid);
        seen.remove(AIR);
        return seen.size();
    }

    /** The first display of {@code recipe} whose result is {@code stack}'s item, or null. */
    public static RecipeDisplay displayMaking(Recipe<?> recipe, ItemStack stack, ContextMap context) {
        try {
            for (RecipeDisplay display : recipe.display()) {
                for (ItemStack result : display.result().resolveForStacks(context)) {
                    if (result.is(stack.getItem())) return display;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    private static ItemStack first(SlotDisplay slot, ContextMap context) {
        try {
            return slot.resolveForFirstStack(context);
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    private static Identifier key(ItemStack stack) {
        return stack.isEmpty() ? AIR : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private QuestionaryFacts() {}
}
