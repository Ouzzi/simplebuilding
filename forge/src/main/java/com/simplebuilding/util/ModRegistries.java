package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRegistries {

    public static RecipeSerializer<ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER;

    private ModRegistries() {
    }

    public static void registerModStuffs() {
        Simplebuilding.LOGGER.info("Registering NeoForge registries for {}", Simplebuilding.MOD_ID);
    }
}
