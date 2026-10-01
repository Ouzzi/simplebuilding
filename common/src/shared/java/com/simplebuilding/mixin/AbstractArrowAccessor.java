package com.simplebuilding.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Netherit-Pfeilspitze (2026-10-01): eine Durchbohrung mehr als der Bogen vergibt. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("setPierceLevel")
    void simplebuilding$setPierceLevel(byte level);
}
