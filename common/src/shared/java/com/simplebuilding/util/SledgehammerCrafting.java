package com.simplebuilding.util;

import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Vorschlaghammer an der Werkbank (Besitzer 2026-09-28, erstes Rezept: Echoscherbe + Hammer ->
 * Pulsating Armor Trim): der Hammer ist Werkzeug, nicht Zutat. Er bleibt im Raster liegen und verliert
 * {@link #CRAFT_DAMAGE} Haltbarkeit, so wie ein Schlag in der Welt ({@link SledgehammerEntityInteraction#HAMMER_DAMAGE});
 * ein Hammer, der dabei bricht, verschwindet. Gilt fuer jedes formlose Rezept mit einem Hammer
 * ({@code ShapelessRecipeMixin}); ein Rezept, das einen Hammer verbraucht, gibt es nicht.
 */
public final class SledgehammerCrafting {
    /** Haltbarkeit, die ein Hammer pro Werkbank-Rezept verliert. */
    public static final int CRAFT_DAMAGE = SledgehammerEntityInteraction.HAMMER_DAMAGE;

    private SledgehammerCrafting() {
    }

    /** Vanillas Reste, dazu jeder Hammer im Raster als beschaedigte Kopie (oder nichts, wenn er bricht). */
    public static NonNullList<ItemStack> remainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = CraftingRecipe.defaultCraftingReminder(input);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof SledgehammerItem) {
                result.set(slot, damaged(stack));
            }
        }
        return result;
    }

    /** Der Hammer nach einem Werkbank-Rezept; leer, wenn er dabei bricht. */
    public static ItemStack damaged(ItemStack hammer) {
        ItemStack copy = hammer.copyWithCount(1);
        if (!copy.isDamageableItem() || copy.has(DataComponents.UNBREAKABLE)) {
            return copy;
        }
        int damage = copy.getDamageValue() + CRAFT_DAMAGE;
        if (damage >= copy.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        copy.setDamageValue(damage);
        return copy;
    }
}
