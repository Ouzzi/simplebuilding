package com.simplebuilding.modules.simplemobs;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * High Gravity (Schwerkraft), from Shellker bullets and the Potion of High Gravity: jumps keep 10 % of their strength
 * (attribute), Levitation and Elytra lift keep 10 % and gliding sinks fast (LivingEntity mixin). The Shellker helmet
 * makes its wearer immune: the effect cannot be applied and is dropped when the helmet is put on.
 */
public final class HighGravityEffect extends MobEffect {
    public static final int COLOR = 0x4A3F5C;

    public HighGravityEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        addAttributeModifier(Attributes.JUMP_STRENGTH, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "effect.high_gravity"),
                EndNetherLogic.UPWARD_FACTOR - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % 10 == 0;
    }

    /** Returning false removes the effect: a helmet put on later still frees the wearer. */
    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        return !EndNetherMobs.wearsHelmet(mob);
    }
}
