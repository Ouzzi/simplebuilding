package com.simplebeacons.beacon;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The four beacon blocks of Simple Beacons (plan docs/ai/PLAN-A-BEACON29-2026-10-29.md, BS28b-4/5) - one block entity
 * class, the kind decides the effect lists:
 * <ul>
 * <li>{@link #STRONG}: primary levels 1-4, the secondary power on level 5 (Regeneration, or the primary again as "II");
 * <li>{@link #MIGHTY}: made of a Strong Beacon, primary levels 1-5, secondary on level 6, night vision, extra hearts, placeholders;
 * <li>{@link #EVIL}: made of a Vanilla beacon with the Evil Upgrade, mild negative powers, primary 1-4 + secondary on 5;
 * <li>{@link #MIGHTY_EVIL}: primary 1-5, secondary on 6.
 * </ul>
 * Evil kinds only reach players that are no newcomers (see {@link EvilGate}).
 */
public enum BeaconKind {
    STRONG("strong_beacon", 4, false, List.of(
            List.of(p(MobEffects.SPEED), p(MobEffects.HASTE), p(MobEffects.SATURATION)),
            List.of(p(MobEffects.RESISTANCE), p(MobEffects.JUMP_BOOST), p(MobEffects.SLOW_FALLING)),
            List.of(p(MobEffects.STRENGTH), p(MobEffects.INVISIBILITY), p(MobEffects.DOLPHINS_GRACE)),
            List.of(p(MobEffects.LUCK))),
            List.of(MobEffects.REGENERATION)),
    MIGHTY("mighty_beacon", 5, false, List.of(
            List.of(p(MobEffects.SPEED), p(MobEffects.HASTE), p(MobEffects.SATURATION)),
            List.of(p(MobEffects.RESISTANCE), p(MobEffects.JUMP_BOOST), p(MobEffects.SLOW_FALLING)),
            List.of(p(MobEffects.STRENGTH), p(MobEffects.INVISIBILITY), p(MobEffects.DOLPHINS_GRACE)),
            // Placeholders (owner 28.10.): Resistance II, Speed II, Jump Boost II until real powers replace them
            List.of(p(MobEffects.LUCK), p(MobEffects.NIGHT_VISION), new Power(MobEffects.RESISTANCE, 1)),
            List.of(p(MobEffects.HEALTH_BOOST), new Power(MobEffects.SPEED, 1), new Power(MobEffects.JUMP_BOOST, 1))),
            List.of(MobEffects.REGENERATION)),
    EVIL("evil_beacon", 4, true, List.of(
            List.of(p(MobEffects.SLOWNESS), p(MobEffects.MINING_FATIGUE), p(MobEffects.HUNGER)),
            List.of(p(MobEffects.WEAKNESS), p(MobEffects.UNLUCK), p(MobEffects.OOZING)),
            List.of(p(MobEffects.DARKNESS), p(MobEffects.GLOWING), p(MobEffects.INFESTED)),
            List.of(p(MobEffects.WEAVING))),
            List.of(MobEffects.NAUSEA)),
    MIGHTY_EVIL("mighty_evil_beacon", 5, true, List.of(
            List.of(p(MobEffects.SLOWNESS), p(MobEffects.MINING_FATIGUE), p(MobEffects.HUNGER)),
            List.of(p(MobEffects.WEAKNESS), p(MobEffects.UNLUCK), p(MobEffects.OOZING)),
            List.of(p(MobEffects.DARKNESS), p(MobEffects.GLOWING), p(MobEffects.INFESTED)),
            List.of(p(MobEffects.WEAVING)),
            List.of(p(MobEffects.WIND_CHARGED), new Power(MobEffects.HUNGER, 1), new Power(MobEffects.MINING_FATIGUE, 1))),
            List.of(MobEffects.NAUSEA));

    /** One selectable primary power: the effect and its base amplifier (0 = level I, 1 = a "II" placeholder). */
    public record Power(Holder<MobEffect> effect, int amplifier) {
    }

    private static Power p(Holder<MobEffect> effect) {
        return new Power(effect, 0);
    }

    /** The block's registry path. */
    public final String id;
    private final int primaryLevels;
    private final boolean evil;
    private final List<List<Power>> powers;
    private final List<Holder<MobEffect>> secondaries;

    BeaconKind(String id, int primaryLevels, boolean evil, List<List<Power>> powers, List<Holder<MobEffect>> secondaries) {
        this.id = id;
        this.primaryLevels = primaryLevels;
        this.evil = evil;
        this.powers = powers;
        this.secondaries = secondaries;
    }

    public boolean evil() {
        return this.evil;
    }

    public boolean mighty() {
        return this == MIGHTY || this == MIGHTY_EVIL;
    }

    /** Number of primary levels (rows of the screen). */
    public int primaryLevels() {
        return this.primaryLevels;
    }

    /** The level that unlocks the secondary power = the highest pyramid. */
    public int secondaryLevel() {
        return this.primaryLevels + 1;
    }

    /** The powers of primary level {@code level} (1-based). */
    public List<Power> powers(int level) {
        return this.powers.get(level - 1);
    }

    /** The secondary powers (the effect of the level above the primary ones). */
    public List<Holder<MobEffect>> secondaries() {
        return this.secondaries;
    }

    /** The lowest primary level offering {@code effect} at amplifier {@code amplifier}, or 0. */
    public int primaryLevelOf(@Nullable Holder<MobEffect> effect, int amplifier) {
        if (effect == null) return 0;
        for (int level = 1; level <= this.primaryLevels; level++) {
            for (Power power : powers(level)) {
                if (power.effect().equals(effect) && power.amplifier() == amplifier) return level;
            }
        }
        return 0;
    }

    /** The lowest amplifier {@code effect} is offered with as a primary power, or -1. */
    public int baseAmplifier(@Nullable Holder<MobEffect> effect) {
        for (int amplifier = 0; amplifier <= 1; amplifier++) {
            if (primaryLevelOf(effect, amplifier) > 0) return amplifier;
        }
        return -1;
    }

    public boolean isSecondary(@Nullable Holder<MobEffect> effect) {
        return effect != null && this.secondaries.contains(effect);
    }

    /** Whether {@code effect} is part of this kind at all. */
    public boolean knows(@Nullable Holder<MobEffect> effect) {
        return baseAmplifier(effect) >= 0 || isSecondary(effect);
    }

    /** Every effect of this kind (primary and secondary), for the config and the screen. */
    public List<Holder<MobEffect>> allEffects() {
        java.util.List<Holder<MobEffect>> all = new java.util.ArrayList<>();
        for (int level = 1; level <= this.primaryLevels; level++) {
            for (Power power : powers(level)) if (!all.contains(power.effect())) all.add(power.effect());
        }
        for (Holder<MobEffect> secondary : this.secondaries) if (!all.contains(secondary)) all.add(secondary);
        return all;
    }

    /** The kind of a block of this module, STRONG for anything else. */
    public static BeaconKind of(@Nullable Block block) {
        return block instanceof StrongBeaconBlock beacon ? beacon.kind() : STRONG;
    }

    public static BeaconKind byOrdinal(int ordinal) {
        BeaconKind[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : STRONG;
    }
}
