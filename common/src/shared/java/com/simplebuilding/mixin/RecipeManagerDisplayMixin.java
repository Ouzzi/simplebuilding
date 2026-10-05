package com.simplebuilding.mixin;

import com.simplebuilding.util.TrimUpgrades;
import com.simplebuilding.version.McVersion;
import java.util.stream.StreamSupport;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps slot-enabling recipes intact, but omits their misleading displays before display IDs are assigned. */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerDisplayMixin {
    @ModifyVariable(method = "unpackRecipeInfo", at = @At("HEAD"), argsOnly = true)
    private static Iterable<RecipeHolder<?>> simplebuilding$hideSmithingDummies(Iterable<RecipeHolder<?>> recipes) {
        if (!McVersion.SMITHING_RECIPE_BOOK) {
            return recipes;
        }
        return StreamSupport.stream(recipes.spliterator(), false)
                .filter(recipe -> !TrimUpgrades.DUMMY_RECIPES.contains(recipe.id().identifier()))
                .toList();
    }
}
