package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.shield.NewPlayerShield;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Newcomer protection (T12): harmful potion effects of players, their projectiles and clouds. */
@Mixin(LivingEntity.class)
public abstract class PlayEffectMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$playShieldEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (source != null && NewPlayerShield.blocksEffect((LivingEntity) (Object) this, effect, source)) cir.setReturnValue(false);
    }
}
