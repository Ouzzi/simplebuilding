package com.simplebuilding.modules.simpleend;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Drifting (from Spoiled Drift Fruit, owner 2026-10-13; "drunk" since B23-M14 2026-10-23): the own steps are halved, and
 * a random pull of half the walking speed adds to them - at random intervals ({@link #MIN_TICKS}..{@link #MAX_TICKS})
 * the pull turns to a new random direction (now and then it rests). Forward + a pull forward = normal speed, forward +
 * a pull backward = standing still (or drifting back), forward + a pull sideways = diagonal. Never lethal: the fall
 * distance is reset every tick, and the fruit adds Slow Falling that outlasts the drift.
 *
 * <p>The pull is added in {@code LivingEntity#travel} ({@code mixin/LivingEntityDriftMixin}), which only runs on the side
 * that moves the entity (the own client for a player, the server for mobs), so it never counts twice.
 */
public final class DriftingEffect extends MobEffect {
    public static final int COLOR = 0x9C8FB0;
    /** Own steps while drifting: half (owner: "vor + vor = normal"). */
    public static final double SPEED_FACTOR = -0.5;
    /** The pull as a share of the normal walking acceleration. */
    public static final double PULL = 0.5;
    /** A pull lasts this many ticks before it turns. */
    public static final int MIN_TICKS = 20, MAX_TICKS = 60;
    /** Chance that an interval has no pull at all (a moment to steady yourself). */
    public static final float REST_CHANCE = 0.25F;

    /** Per entity: current pull direction (radians, NaN = resting) and ticks until it turns. Each side keeps its own. */
    private static final Map<LivingEntity, double[]> STATE = new WeakHashMap<>();

    public DriftingEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SimpleEnd.id("effect.drifting.speed"), SPEED_FACTOR, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
        entity.resetFallDistance();
        return true;
    }

    /**
     * One tick of the pull, before the entity moves (from {@code LivingEntity#travel}). Returns the added horizontal
     * velocity (zero while resting).
     */
    public static Vec3 drift(LivingEntity entity) {
        double[] s;
        synchronized (STATE) {
            s = STATE.computeIfAbsent(entity, e -> new double[]{Double.NaN, 0});
        }
        var r = entity.getRandom();
        if (--s[1] <= 0) {
            s[0] = r.nextFloat() < REST_CHANCE ? Double.NaN : r.nextDouble() * Math.PI * 2;
            s[1] = MIN_TICKS + r.nextInt(MAX_TICKS - MIN_TICKS + 1);
        }
        if (Double.isNaN(s[0])) return Vec3.ZERO;
        double a = PULL * acceleration(entity);
        Vec3 add = new Vec3(Math.cos(s[0]) * a, 0, Math.sin(s[0]) * a);
        entity.setDeltaMovement(entity.getDeltaMovement().add(add));
        return add;
    }

    /** Test hook: start a pull in {@code angle} (radians) for {@code ticks} ticks. */
    public static void setPull(LivingEntity entity, double angle, int ticks) {
        synchronized (STATE) {
            STATE.put(entity, new double[]{angle, ticks + 1});
        }
    }

    /** The walking acceleration of the entity's base speed, scaled by ground friction the way {@code LivingEntity} does. */
    static double acceleration(LivingEntity entity) {
        if (!entity.onGround()) return 0.02;
        double base = entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        BlockPos below = entity.getBlockPosBelowThatAffectsMyMovement();
        float f = entity.level().getBlockState(below).getBlock().getFriction();
        return base * (0.21600002F / (f * f * f));
    }
}
