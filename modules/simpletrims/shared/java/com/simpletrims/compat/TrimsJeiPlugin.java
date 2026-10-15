package com.simpletrims.compat;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TrimUpgrades;
import com.simpletrims.recipe.TrimUpgradeRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Loaded only by JEI: the trim upgrades (template + armor + material) show the armor WITH the upgrade as the result,
 * not the plain armor the recipe display carries, and follow the armor the player focuses on.
 */
@JeiPlugin
public final class TrimsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return SimpleTrims.id("jei");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(TrimUpgradeRecipe.class, new Extension());
    }

    private static final class Extension implements ISmithingCategoryExtension<TrimUpgradeRecipe> {
        private static ItemStack first(Optional<Ingredient> ingredient) {
            return ingredient.flatMap(i -> i.items().findFirst()).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }

        /** The armor of the base list with this upgrade: same order and length as the base slot. */
        private static List<ItemStack> outputs(TrimUpgradeRecipe recipe) {
            ItemStack template = first(recipe.templateIngredient());
            ItemStack material = first(recipe.additionIngredient());
            List<ItemStack> out = new ArrayList<>();
            recipe.baseIngredient().items().forEach(holder -> {
                ItemStack base = new ItemStack(holder);
                ItemStack result = TrimUpgrades.result(template, base, material);
                out.add(result == null || result.isEmpty() ? base : result);
            });
            return out;
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setTemplate(TrimUpgradeRecipe recipe, T ingredients) {
            recipe.templateIngredient().ifPresent(ingredients::add);
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setBase(TrimUpgradeRecipe recipe, T ingredients) {
            ingredients.add(recipe.baseIngredient());
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setAddition(TrimUpgradeRecipe recipe, T ingredients) {
            recipe.additionIngredient().ifPresent(ingredients::add);
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setOutput(TrimUpgradeRecipe recipe, T ingredients) {
            ingredients.addItemStacks(outputs(recipe));
        }

        @Override
        public void onDisplayedIngredientsUpdate(TrimUpgradeRecipe recipe, IRecipeSlotDrawable templateSlot,
                                                 IRecipeSlotDrawable baseSlot, IRecipeSlotDrawable additionSlot,
                                                 IRecipeSlotDrawable outputSlot, IFocusGroup focuses) {
            ItemStack base = baseSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
            if (base.isEmpty()) {
                return;
            }
            ItemStack template = templateSlot.getDisplayedItemStack().orElse(first(recipe.templateIngredient()));
            ItemStack material = additionSlot.getDisplayedItemStack().orElse(first(recipe.additionIngredient()));
            ItemStack result = TrimUpgrades.result(template, base, material);
            if (result != null && !result.isEmpty()) {
                outputSlot.createDisplayOverrides().add(result);
            }
        }
    }
}
