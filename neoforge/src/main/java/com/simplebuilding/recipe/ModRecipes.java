package com.simplebuilding.recipe;

import com.simplebuilding.Simplebuilding;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
    public static RecipeSerializer<CountBasedSmithingRecipe> COUNT_BASED_SMITHING_SERIALIZER;
    public static RecipeType<CountBasedSmithingRecipe> COUNT_BASED_SMITHING;
    public static RecipeSerializer<UpgradeSmithingRecipe> UPGRADE_SMITHING_SERIALIZER;

    private ModRecipes() {
    }

    public static void registerRecipes() {
        Simplebuilding.LOGGER.info("Registering Recipes for {}", Simplebuilding.MOD_ID);
    }
}
