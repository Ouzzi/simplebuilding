package com.simplebuilding.mixin;

import com.simplebuilding.guide.GuideUnlocks;
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
 * through {@code awardRecipes}, so one hook here serves every loader. Only when something new was
 * added (the return value) are the tabs checked again.
 */
@Mixin(ServerPlayer.class)
public abstract class GuideRecipeUnlockMixin {

    @Inject(method = "awardRecipes", at = @At("RETURN"))
    private void simplebuilding$openGuideTabs(Collection<RecipeHolder<?>> recipes, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0) GuideUnlocks.refresh((ServerPlayer) (Object) this, true);
    }
}
