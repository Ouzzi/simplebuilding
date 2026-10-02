package com.simplebuilding.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Netherit-Pfeilspitze (2026-10-01): eine Durchbohrung mehr als der Bogen vergibt; Pfeil-Rueckgabe (2026-10-02). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("setPierceLevel")
    void simplebuilding$setPierceLevel(byte level);

    /** Pfeil-Rueckgabe (AbstractArrowRecoveryMixin) ruft die Treffer-Nachwirkung selbst auf. */
    @Invoker("doPostHurtEffects")
    void simplebuilding$doPostHurtEffects(net.minecraft.world.entity.LivingEntity target);

    /** Tests: einen Treffer auf ein Wesen ausloesen, ohne dass der Pfeil fliegen muss. */
    @Invoker("onHitEntity")
    void simplebuilding$onHitEntity(net.minecraft.world.phys.EntityHitResult hit);
}
