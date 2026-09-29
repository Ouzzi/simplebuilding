package com.simplebuilding.compat.jei;

import com.simplebuilding.recipe.EnchantedShapelessRecipe;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/**
 * Formlose Rezepte mit verlangter Verzauberung ({@link EnchantedShapelessRecipe}, Flypad I: Elytra mit
 * Reparatur) in JEIs Werkbank-Kategorie: die verlangte Zutat erscheint mit Verzauberungsglanz und dem Namen der
 * Verzauberung als Zeile ({@link EnchantedShapelessRecipe#requiredDisplay}), damit niemand eine gewoehnliche
 * Elytra einlegt und sich wundert. Raster 2x2 wie Vanillas formlose Rezepte mit vier Zutaten.
 */
final class EnchantedShapelessExtension implements ICraftingCategoryExtension<EnchantedShapelessRecipe> {

    @Override
    public List<SlotDisplay> getIngredients(RecipeHolder<EnchantedShapelessRecipe> holder) {
        return holder.value().ingredientDisplays();
    }

    @Override
    public void setRecipe(RecipeHolder<EnchantedShapelessRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, IFocusGroup focuses) {
        EnchantedShapelessRecipe recipe = holder.value();
        grid.createAndSetIngredientsFromDisplays(builder, recipe.ingredientDisplays(), 0, 0);
        grid.createAndSetOutputs(builder, recipe.display().getFirst().result());
        builder.setShapeless();
    }
}
