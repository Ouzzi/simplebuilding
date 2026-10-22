package com.simplebuilding.modules.simplemodels.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Whether the recipe book is open, so the Models bookmark can leave its edge free (SM21 f). */
@Mixin(AbstractRecipeBookScreen.class)
public interface ModelRecipeBookAccessor {
    @Accessor("recipeBookComponent") RecipeBookComponent<?> simplemodels$book();
}
