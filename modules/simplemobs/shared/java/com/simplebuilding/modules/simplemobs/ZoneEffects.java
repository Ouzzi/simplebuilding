package com.simplebuilding.modules.simplemobs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

/**
 * The effect pools of the danger zones (WP-22, KONZEPT-A-WP22 §1.5): a new monster of level S (0..5, from Simple Resonance) may carry
 * status effects, never more health. Each pool has a chance at level 5 and a level cap; the chance grows as {@code c x (S/5)^1.3}.
 * The pools are drawn in random order, at most {@link MobsConfig#zoneMaxEffects} succeed, the level of an effect follows S around a
 * bell curve. Pure rules here; {@link ZoneSpawns} is the hook.
 */
public final class ZoneEffects {
    /** Effect pools: id (config key), chance at level 5, level cap. */
    public enum Pool {
        STRENGTH("strength", 0.35, 3), SPEED("speed", 0.25, 2), EXTRA_HEARTS("extra_hearts", 0.30, 2), OOZING("oozing", 0.20, 1),
        INFESTED("infested", 0.20, 1), DEFLECTION("deflection", 0.25, 5), VOLATILE("volatile", 0.15, 5), MIRROR("mirror", 0.10, 5);

        public final String id;
        public final double defaultChance;
        public final int defaultCap;

        Pool(String id, double defaultChance, int defaultCap) {
            this.id = id;
            this.defaultChance = defaultChance;
            this.defaultCap = defaultCap;
        }

        /** The Mob effect of the pool. */
        public Holder<MobEffect> effect() {
            return switch (this) {
                case STRENGTH -> MobEffects.STRENGTH;
                case SPEED -> MobEffects.SPEED;
                case EXTRA_HEARTS -> MobEffects.ABSORPTION;
                case OOZING -> MobEffects.OOZING;
                case INFESTED -> MobEffects.INFESTED;
                case DEFLECTION -> CombatEffects.deflection();
                case VOLATILE -> CombatEffects.volatileEffect();
                case MIRROR -> CombatEffects.mirror();
            };
        }
    }

    /** One drawn effect: the pool and its level 1..5 (amplifier = level - 1). */
    public record Rolled(Pool pool, int level) {}

    public static final double EXPONENT = 1.3, LEVEL_SPREAD = 0.9;

    private ZoneEffects() {}

    /** p_e(S) = c x (S / 5)^1.3, 0 at S = 0; S is clamped to 0..5. */
    public static double chance(double cAtFive, double level) {
        double s = Math.max(0.0, Math.min(5.0, level));
        return s <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, cAtFive)) * Math.pow(s / 5.0, EXPONENT);
    }

    /** Weight of effect level {@code k} (1..cap) for zone level {@code s}: exp(-(k - max(1, s))^2 / (2 x 0.9^2)). */
    public static double levelWeight(int k, double s) {
        double d = k - Math.max(1.0, s);
        return Math.exp(-(d * d) / (2.0 * LEVEL_SPREAD * LEVEL_SPREAD));
    }

    /** The level 1..cap picked by a uniform roll in [0,1) from the bell weights (cap below 1 counts as 1). */
    public static int pickLevel(double s, int cap, double roll) {
        int max = Math.max(1, Math.min(5, cap));
        double total = 0.0;
        for (int k = 1; k <= max; k++) total += levelWeight(k, s);
        double acc = 0.0;
        for (int k = 1; k <= max; k++) {
            acc += levelWeight(k, s) / total;
            if (roll < acc) return k;
        }
        return max;
    }

    /**
     * Draws the effects of a spawn of level {@code s}: the pools in random order, each with its chance, at most {@code maxEffects}.
     * {@code chance} and {@code cap} give the configured numbers per pool; {@code rng} yields uniform values in [0,1).
     */
    public static List<Rolled> roll(double s, int maxEffects, java.util.function.ToDoubleFunction<Pool> chance, java.util.function.ToIntFunction<Pool> cap,
                                    DoubleSupplier rng) {
        List<Rolled> out = new ArrayList<>();
        if (s <= 0.0 || maxEffects <= 0) return out;
        Pool[] pools = Pool.values();
        for (int i = pools.length - 1; i > 0; i--) {
            int j = Math.min(i, (int) (rng.getAsDouble() * (i + 1)));
            Pool t = pools[i];
            pools[i] = pools[j];
            pools[j] = t;
        }
        for (Pool pool : pools) {
            if (out.size() >= maxEffects) break;
            if (cap.applyAsInt(pool) <= 0) continue;
            if (rng.getAsDouble() < chance(chance.applyAsDouble(pool), s)) out.add(new Rolled(pool, pickLevel(s, cap.applyAsInt(pool), rng.getAsDouble())));
        }
        return out;
    }

    /** Puts the drawn effects on the mob for good (infinite duration, saved with the mob), with few particles and no HUD icon. */
    public static void apply(Mob mob, List<Rolled> rolled) {
        for (Rolled r : rolled) {
            mob.addEffect(new MobEffectInstance(r.pool().effect(), MobEffectInstance.INFINITE_DURATION, r.level() - 1, true, true, false));
        }
    }
}
