package com.simplebeacons.conduit;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * The four conduit blocks of Simple Beacons (plan docs/ai/PLAN-A-IG31A-2026-10-30.md, IG31-2): one block entity class, the
 * kind decides frame size, range, attack, effects, particles and ambient sound.
 * <ul>
 * <li>{@link #STRONG}: frames up to 7x7x7, range 24 per 7 frame blocks, Conduit Power + Dolphin's Grace;
 * <li>{@link #MIGHTY}: frames up to 9x9x9 (90 blocks), range 32 per 7, attack 16, adds Night Vision;
 * <li>{@link #EVIL}: the opposite - players that are no newcomers (see {@code EvilGate}) lose speed and mining pace while
 *     really under water inside the range; no attack on mobs;
 * <li>{@link #MIGHTY_EVIL}: 9x9x9 frame, larger range, harsher effects (Mining Fatigue II, Slowness, Weakness, Darkness).
 * </ul>
 */
public enum ConduitKind {
    STRONG("strong_conduit", 3, 24, false, 0xFF9FE6FF),
    MIGHTY("mighty_conduit", 4, 32, false, 0xFFFFE08A),
    EVIL("evil_conduit", 3, 24, true, 0xFFC87BFF),
    MIGHTY_EVIL("mighty_evil_conduit", 4, 32, true, 0xFFFF5A6E);

    public record Power(Holder<MobEffect> effect, int amplifier) {
    }

    /** The block's registry path. */
    public final String id;
    private final int maxRadius;
    private final int rangePer7;
    private final boolean evil;
    private final int tint;

    ConduitKind(String id, int maxRadius, int rangePer7, boolean evil, int tint) {
        this.id = id;
        this.maxRadius = maxRadius;
        this.rangePer7 = rangePer7;
        this.evil = evil;
        this.tint = tint;
    }

    public boolean evil() {
        return this.evil;
    }

    public boolean mighty() {
        return this == MIGHTY || this == MIGHTY_EVIL;
    }

    /** Largest frame radius that counts (3 = 7x7x7, 4 = 9x9x9). */
    public int maxRadius() {
        return this.maxRadius;
    }

    /** Blocks of range per 7 frame blocks (Vanilla: 16). */
    public int rangePer7() {
        return this.rangePer7;
    }

    /** ARGB multiplied onto the Vanilla conduit textures by the renderer. */
    public int tint() {
        return this.tint;
    }

    /** The server cap of the range: the config value, 1.5 x for the mighty kinds. */
    public int rangeCap(int configCap) {
        return mighty() ? configCap * 3 / 2 : configCap;
    }

    /** Attack radius (0 = none): the evil kinds never attack; 8/12 for Strong, 8/12/16 for Mighty with the full 5/7/9 frame. */
    public int killRange(int frameBlocks, int radius) {
        if (this.evil) return 0;
        if (this == MIGHTY && radius == 4 && frameBlocks >= StrongConduitLogic.FULL_HUGE) return 16;
        if (radius >= 3 && frameBlocks >= StrongConduitLogic.FULL_LARGE) return 12;
        return frameBlocks >= StrongConduitLogic.FULL_SMALL ? 8 : 0;
    }

    /** Effects for a player in range (good ones for the good kinds, bad ones for the evil kinds). */
    public List<Power> effects() {
        return switch (this) {
            case STRONG -> List.of(new Power(MobEffects.CONDUIT_POWER, 0), new Power(MobEffects.DOLPHINS_GRACE, 0));
            case MIGHTY -> List.of(new Power(MobEffects.CONDUIT_POWER, 0), new Power(MobEffects.DOLPHINS_GRACE, 0),
                    new Power(MobEffects.NIGHT_VISION, 0));
            case EVIL -> List.of(new Power(MobEffects.MINING_FATIGUE, 0), new Power(MobEffects.SLOWNESS, 0));
            case MIGHTY_EVIL -> List.of(new Power(MobEffects.MINING_FATIGUE, 1), new Power(MobEffects.SLOWNESS, 0),
                    new Power(MobEffects.WEAKNESS, 0), new Power(MobEffects.DARKNESS, 0));
        };
    }

    /** The particle that rises around an active conduit (the locator: visible from far in the water). */
    public ParticleOptions aura() {
        return switch (this) {
            case STRONG -> ParticleTypes.GLOW;
            case MIGHTY -> ParticleTypes.END_ROD;
            case EVIL -> ParticleTypes.SCULK_SOUL;
            case MIGHTY_EVIL -> ParticleTypes.SQUID_INK;
        };
    }

    /** The extra ambient sound played every few seconds beside Vanilla's conduit hum (null = none). */
    public SoundEvent ambient() {
        return this.evil ? SoundEvents.SCULK_CLICKING : SoundEvents.CONDUIT_AMBIENT_SHORT;
    }

    /** Volume of the hum: louder than Vanilla's so the conduit is found by ear (Vanilla 1.0). */
    public float volume() {
        return mighty() ? 2.5F : 1.6F;
    }

    /** Pitch of the hum: the evil kinds sound lower. */
    public float pitch() {
        return this.evil ? 0.55F : (mighty() ? 0.85F : 1.0F);
    }

    public static ConduitKind byOrdinal(int ordinal) {
        ConduitKind[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : STRONG;
    }
}
