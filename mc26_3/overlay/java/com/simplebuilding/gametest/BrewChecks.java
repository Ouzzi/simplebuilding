package com.simplebuilding.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** MC 26.3 side of {@code BrewChecks}: one brewing stand step through the data driven brewing recipes. */
final class BrewChecks {

    private BrewChecks() {
    }

    /** What the brewing stand makes of {@code input} with {@code reagent}, or EMPTY when no recipe matches. */
    static ItemStack brew(ServerLevel level, ItemStack input, ItemStack reagent) {
        BrewingInput brewing = new BrewingInput(input, reagent);
        return level.recipeAccess().getRecipeFor(RecipeType.BREWING, brewing, level)
                .map(recipe -> recipe.value().assemble(brewing))
                .orElse(ItemStack.EMPTY);
    }
}
