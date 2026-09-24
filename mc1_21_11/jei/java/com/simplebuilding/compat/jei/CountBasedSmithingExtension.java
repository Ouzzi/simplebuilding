package com.simplebuilding.compat.jei;

import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import java.util.List;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import net.minecraft.world.item.ItemStack;

/**
 * Teaches JEI's smithing category the mod's {@code simplebuilding:count_based_smithing} recipes
 * (template + base + N additions, e.g. 4 iron ingots for a copper to iron pickaxe). JEI only knows
 * the vanilla smithing classes; without this extension these recipes would not show at all. The
 * addition slot carries the real count.
 */
final class CountBasedSmithingExtension implements ISmithingCategoryExtension<CountBasedSmithingRecipe> {

    @Override
    public <T extends IIngredientAcceptor<T>> void setTemplate(CountBasedSmithingRecipe recipe, T ingredients) {
        recipe.templateIngredient().ifPresent(template -> ingredients.add(template));
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setBase(CountBasedSmithingRecipe recipe, T ingredients) {
        ingredients.add(recipe.baseIngredient());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setAddition(CountBasedSmithingRecipe recipe, T ingredients) {
        recipe.additionIngredient().ifPresent(addition -> {
            List<ItemStack> stacks = addition.items()
                    .map(item -> new ItemStack(item, recipe.getAdditionCount()))
                    .toList();
            ingredients.addItemStacks(stacks);
        });
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setOutput(CountBasedSmithingRecipe recipe, T ingredients) {
        ingredients.add(recipe.getResultStack());
    }
}
