package com.simplebuilding.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;

/**
 * Rezeptbuch-Platzierung am Schmiedetisch (Vanilla hat dort kein Buch, {@code SmithingMenu} ist kein
 * {@link RecipeBookMenu}). Verhalten wie Vanillas {@code ServerPlaceRecipe}: die drei Felder wandern zurueck ins
 * Inventar, dann werden Vorlage, Basis und Material aus dem Inventar eingelegt; Shift legt so viele Durchgaenge wie
 * moeglich, ein erneuter Klick auf dasselbe Rezept einen mehr. Fehlt ein Teil, bleiben die Felder leer und der Client
 * bekommt das Geisterrezept.
 *
 * <p>Anders als beim Herstellen zaehlen beschaedigte, verzauberte und benannte Items mit (die verzauberte Ruestung ist
 * genau die Basis). Nichts entsteht neu: alles stammt aus Feldern oder Inventar; passt das Zurueckraeumen nicht ins
 * Inventar, bleibt alles unveraendert.
 */
public final class SmithingPlacement {
    private static final int MAIN_INVENTORY = 36;

    private SmithingPlacement() {
    }

    public static RecipeBookMenu.PostPlaceAction place(SmithingMenu menu, SmithingRecipe recipe, Inventory inventory, boolean useMaxItems) {
        List<Optional<Ingredient>> ingredients = List.of(recipe.templateIngredient(), Optional.of(recipe.baseIngredient()),
                recipe.additionIngredient());
        int[] perCraft = {1, 1, recipe instanceof CountBasedSmithingRecipe counted ? Math.max(1, counted.getAdditionCount()) : 1};
        List<Slot> grid = List.of(menu.getSlot(0), menu.getSlot(1), menu.getSlot(2));

        int placedCrafts = placedCrafts(grid, ingredients, perCraft);
        if (!clearGrid(grid, inventory)) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }

        int[] reserved = new int[MAIN_INVENTORY];
        int maxCrafts = Integer.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (ingredients.get(i).isEmpty()) {
                continue;
            }
            Plan probe = collect(inventory, ingredients.get(i).get(), Integer.MAX_VALUE, new int[MAIN_INVENTORY]);
            int stackLimit = probe.prototype.isEmpty() ? 0 : probe.prototype.getMaxStackSize();
            maxCrafts = Math.min(maxCrafts, Math.min(probe.total, stackLimit) / perCraft[i]);
        }
        if (maxCrafts <= 0) {
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
        }
        int crafts = useMaxItems ? maxCrafts : Math.min(maxCrafts, placedCrafts + 1);

        List<Plan> plans = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (ingredients.get(i).isEmpty()) {
                plans.add(null);
                continue;
            }
            Plan plan = collect(inventory, ingredients.get(i).get(), crafts * perCraft[i], reserved);
            if (plan.total < crafts * perCraft[i]) {
                return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
            }
            plans.add(plan);
        }
        for (int i = 0; i < 3; i++) {
            Plan plan = plans.get(i);
            if (plan == null) {
                continue;
            }
            ItemStack placed = ItemStack.EMPTY;
            for (int[] take : plan.takes) {
                ItemStack taken = inventory.removeItem(take[0], take[1]);
                if (placed.isEmpty()) {
                    placed = taken;
                } else {
                    placed.grow(taken.getCount());
                }
            }
            grid.get(i).set(placed);
        }
        inventory.setChanged();
        return RecipeBookMenu.PostPlaceAction.NOTHING;
    }

    /** Wie viele Durchgaenge schon passend in den Feldern liegen (0, wenn sie nicht zum Rezept passen). */
    private static int placedCrafts(List<Slot> grid, List<Optional<Ingredient>> ingredients, int[] perCraft) {
        int crafts = Integer.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = grid.get(i).getItem();
            if (ingredients.get(i).isEmpty()) {
                if (!stack.isEmpty()) {
                    return 0;
                }
                continue;
            }
            if (stack.isEmpty() || !ingredients.get(i).get().test(stack)) {
                return 0;
            }
            crafts = Math.min(crafts, stack.getCount() / perCraft[i]);
        }
        return crafts == Integer.MAX_VALUE ? 0 : crafts;
    }

    /** Raeumt die drei Felder ins Inventar; passt etwas nicht, wird alles zurueckgesetzt und false geliefert. */
    private static boolean clearGrid(List<Slot> grid, Inventory inventory) {
        List<ItemStack> inventoryBefore = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventoryBefore.add(inventory.getItem(i).copy());
        }
        List<ItemStack> gridBefore = new ArrayList<>();
        for (Slot slot : grid) {
            gridBefore.add(slot.getItem().copy());
        }
        boolean fits = true;
        for (Slot slot : grid) {
            ItemStack stack = slot.getItem().copy();
            if (stack.isEmpty()) {
                continue;
            }
            inventory.add(stack);
            if (!stack.isEmpty()) {
                fits = false;
                break;
            }
            slot.set(ItemStack.EMPTY);
        }
        if (!fits) {
            for (int i = 0; i < inventoryBefore.size(); i++) {
                inventory.setItem(i, inventoryBefore.get(i));
            }
            for (int i = 0; i < grid.size(); i++) {
                grid.get(i).set(gridBefore.get(i));
            }
        }
        return fits;
    }

    /** Sammelt bis zu {@code need} passende, untereinander stapelbare Items aus dem Hauptinventar. */
    private static Plan collect(Inventory inventory, Ingredient ingredient, int need, int[] reserved) {
        Plan plan = new Plan();
        for (int i = 0; i < MAIN_INVENTORY && plan.total < need; i++) {
            ItemStack stack = inventory.getItem(i);
            int free = stack.getCount() - reserved[i];
            if (stack.isEmpty() || free <= 0 || !ingredient.test(stack)) {
                continue;
            }
            if (plan.prototype.isEmpty()) {
                plan.prototype = stack;
            } else if (!ItemStack.isSameItemSameComponents(plan.prototype, stack)) {
                continue;
            }
            int take = (int) Math.min(free, (long) need - plan.total);
            reserved[i] += take;
            plan.total += take;
            plan.takes.add(new int[] {i, take});
        }
        return plan;
    }

    private static final class Plan {
        ItemStack prototype = ItemStack.EMPTY;
        int total;
        final List<int[]> takes = new ArrayList<>();
    }
}
