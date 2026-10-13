package com.simplebuilding.modules.simpleend;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Drifting (from Spoiled Drift Fruit, owner 2026-10-13): short, uncontrolled floating - gravity almost gone, steering
 * weak, and every {@link #PUSH_EVERY} ticks a random push sideways and up. Never lethal: the fall distance is reset
 * every tick, and the fruit adds Slow Falling that outlasts the drift.
 */
public final class DriftingEffect extends MobEffect {
    public static final int COLOR = 0x9C8FB0;
    public static final int PUSH_EVERY = 10;
    public static final double PUSH = 0.16, LIFT = 0.08;

    public DriftingEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        addAttributeModifier(Attributes.GRAVITY, SimpleEnd.id("effect.drifting.gravity"), -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SimpleEnd.id("effect.drifting.speed"), -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
        entity.resetFallDistance();
        if (entity.tickCount % PUSH_EVERY == 0) push(entity);
        return true;
    }

    /** One random drift push (sideways plus a little lift), sent to the client for players. */
    public static void push(LivingEntity entity) {
        var r = entity.getRandom();
        double angle = r.nextDouble() * Math.PI * 2;
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * 0.5 + Math.cos(angle) * PUSH, Math.max(v.y, 0) + LIFT * (0.5 + r.nextDouble()), v.z * 0.5 + Math.sin(angle) * PUSH);
        entity.needsSync = true;
    }
}
