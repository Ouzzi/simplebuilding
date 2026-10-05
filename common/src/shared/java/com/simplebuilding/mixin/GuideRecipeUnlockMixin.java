package com.simplebuilding.mixin;

import com.simplebuilding.guide.GuideUnlocks;
import com.simplebuilding.util.TrimUpgrades;
import com.simplebuilding.version.McVersion;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guide tabs open from the recipe book (plan P5/P6): every way a recipe gets into a player's recipe
 * book - recipe advancements, crafting it, {@code /recipe give}, the operator sync at login - goes
 * through {@code awardRecipes}, so one hook here serves every loader. Vanilla returns the number
 * of added displays, so hidden smithing gate recipes also need an explicit refresh.
 */
@Mixin(ServerPlayer.class)
public abstract class GuideRecipeUnlockMixin {

    @Inject(method = "awardRecipes", at = @At("RETURN"))
    private void simplebuilding$openGuideTabs(Collection<RecipeHolder<?>> recipes, CallbackInfoReturnable<Integer> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (cir.getReturnValueI() > 0 || McVersion.SMITHING_RECIPE_BOOK && recipes.stream().anyMatch(recipe ->
                TrimUpgrades.DUMMY_RECIPES.contains(recipe.id().identifier()) && player.getRecipeBook().contains(recipe.id()))) {
            GuideUnlocks.refresh(player, true);
        }
    }
}
