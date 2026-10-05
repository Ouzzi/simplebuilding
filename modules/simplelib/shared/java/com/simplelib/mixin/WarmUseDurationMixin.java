package com.simplelib.mixin;

import com.simplelib.warm.Warm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Warm food is eaten 15 % faster (owner wish, config {@code eatSpeedBonus}). */
@Mixin(ItemStack.class)
public abstract class WarmUseDurationMixin {
    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void simplelib$warmFaster(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (cir.getReturnValueI() > 0 && entity != null && Warm.isWarm(self, entity.level())) {
            cir.setReturnValue(Warm.fasterUse(cir.getReturnValueI()));
        }
    }
}
