package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.MobsRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Silence effect: a bearer dampens its vibrations (same path Vanilla uses for wool item entities). */
@Mixin(Entity.class)
public abstract class EntitySilenceMixin {
    @Inject(method = "dampensVibrations", at = @At("HEAD"), cancellable = true)
    private void simplemobs$silence(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living && MobsRegistry.silenced(living)) cir.setReturnValue(true);
    }
}
