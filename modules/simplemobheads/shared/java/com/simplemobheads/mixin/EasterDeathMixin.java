package com.simplemobheads.mixin;

import com.simplemobheads.easter.Technoblade;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Technoblade never dies (EH21 TO-1): Vanilla asks here whether a totem saves the dying entity; a crowned pig saves itself. */
@Mixin(LivingEntity.class)
public abstract class EasterDeathMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$neverDies(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (Technoblade.neverDies((LivingEntity) (Object) this, source) != Technoblade.Saved.NOT_CROWNED) {
            cir.setReturnValue(true);
        }
    }
}
