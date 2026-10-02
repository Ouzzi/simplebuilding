package com.simplebuilding.mixin;

import com.simplebuilding.fletching.ArrowRecovery;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pfeil-Rueckgabe (2026-10-02, {@link ArrowRecovery}): am Aufruf von {@code doPostHurtEffects} in
 * {@code onHitEntity} - den gibt es nur nach einem erfolgreichen Treffer auf ein Lebewesen, vor dem Verwerfen.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowRecoveryMixin {

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;doPostHurtEffects(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void simplebuilding$rememberArrow(AbstractArrow arrow, LivingEntity target) {
        ((AbstractArrowAccessor) arrow).simplebuilding$doPostHurtEffects(target);
        ArrowRecovery.onHit(arrow, target);
    }
}
