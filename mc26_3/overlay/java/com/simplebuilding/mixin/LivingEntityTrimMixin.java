package com.simplebuilding.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.3 twin of the 26.2 trim-ability mixin: empty. On 26.3 the armour trim abilities live in Simple Mobs
 * ({@code simplemobs} TrimAbilities and its mixins; ZO9 A / MOB27-3, docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md); the shared mixin list
 * names this class on both lines.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTrimMixin {
}
