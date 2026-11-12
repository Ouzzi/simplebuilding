package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Summoning: a sculk sensor near a complete {@link HowlerPattern} hears a goat horn (any instrument) blown by a player
 * and starts the 5 s manifestation ({@link HowlerRitualEntity}). Called from the sensor mixin on every activation.
 */
public final class HowlerRitual {
    public enum Result { STARTED, NO_FIGURE, NOT_A_HORN, DISABLED, BUSY, TOO_FAR, PEACEFUL, WRONG_DIMENSION, TOO_BRIGHT }

    private HowlerRitual() {}

    /** Sculk sensor mixin hook (server side, after the sensor switched to active). {@code null}: not a horn call at all. */
    public static @Nullable Result onSensorActivated(Level level, BlockPos sensor, @Nullable Entity source, int frequency) {
        if (!(level instanceof ServerLevel server) || !MobsRegistry.on()) return null;
        if (frequency != VibrationSystem.getGameEventFrequency(GameEvent.INSTRUMENT_PLAY)) return null;
        if (!(source instanceof Player player) || !blewHorn(player)) return Result.NOT_A_HORN;
        if (!MobsConfig.howlerEnabled) return Result.DISABLED;
        var figure = HowlerPattern.nearSensor(server, sensor);
        return figure.isEmpty() ? Result.NO_FIGURE : start(server, figure.get(), sensor, player, HowlerLogic.MANIFEST_TICKS, null);
    }

    /** A player who just played a goat horn has the horn on cooldown (Vanilla sets it for the whole sound). */
    public static boolean blewHorn(Player player) {
        return player.getCooldowns().isOnCooldown(new ItemStack(Items.GOAT_HORN));
    }

    /**
     * Starts the manifestation if every rule allows it; otherwise the figure shows the abort cue ({@link #abortEffects}) where it applies.
     *
     * @param forcedLight test hook: light value to use instead of the world's light ({@code null}: real light)
     */
    public static Result start(ServerLevel level, HowlerPattern.Match m, BlockPos sensor, @Nullable Player player, int ticks, @Nullable Integer forcedLight) {
        if (!MobsConfig.howlerEnabled) return Result.DISABLED;
        if (!level.getEntitiesOfClass(HowlerRitualEntity.class, new net.minecraft.world.phys.AABB(m.center()).inflate(1.0)).isEmpty()) return Result.BUSY;
        if (player != null && player.distanceToSqr(Vec3.atCenterOf(m.center())) > HowlerLogic.PLAYER_RANGE * HowlerLogic.PLAYER_RANGE) return Result.TOO_FAR;
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            abortEffects(level, m.center(), "too_peaceful");
            return Result.PEACEFUL;
        }
        if (!MobsConfig.howlerAllowDimensions.contains(level.dimension().identifier().toString())) {
            abortEffects(level, m.center(), "wrong_dimension");
            return Result.WRONG_DIMENSION;
        }
        if (!HowlerLogic.darkEnough(light(level, m, forcedLight), MobsConfig.howlerMaxLight)) {
            abortEffects(level, m.center(), "too_bright");
            return Result.TOO_BRIGHT;
        }
        HowlerRitualEntity ritual = MobsRegistry.HOWLER_RITUAL.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (ritual == null) return Result.DISABLED;
        ritual.setup(m.center(), sensor, ticks, forcedLight);
        level.addFreshEntity(ritual);
        level.playSound(null, m.center(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 2.0f, 0.6f);
        // GADGET (owner 01.11.): the awakening is sound (above) and particles, no action bar text
        var c = Vec3.atCenterOf(m.center());
        level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y + 0.5, c.z, 24, 0.6, 1.2, 0.6, 0.03);
        return Result.STARTED;
    }

    /** Light at the skull: block + sky light like monster spawns (owner 11.10.). */
    public static int light(ServerLevel level, HowlerPattern.Match m, @Nullable Integer forcedLight) {
        return forcedLight != null ? forcedLight : level.getMaxLocalRawBrightness(m.head());
    }

    public static boolean sensorPresent(Level level, BlockPos sensor) {
        return level.getBlockState(sensor).getBlock() instanceof SculkSensorBlock;
    }

    /**
     * Abort or refusal at the figure: sculk souls and smoke, the sensor's "stop" click with a muffled sniff; every block stays.
     * GADGET (owner 01.11., PLAN40 simplemobs-2): no action bar text - the cue sits at the figure, two reasons add their own
     * particles: {@code too_bright} glints of light, {@code wrong_dimension} reversed portal sparks ({@code fades} and
     * {@code too_peaceful} only the plain cue). The rules themselves are in the guide and the wiki.
     */
    public static void abortEffects(ServerLevel level, BlockPos center, String reason) {
        var c = Vec3.atCenterOf(center);
        level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y + 0.5, c.z, 16, 0.6, 1.2, 0.6, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.5, c.z, 20, 0.6, 1.2, 0.6, 0.02);
        switch (reason) {
            case "too_bright" -> level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.5, c.z, 8, 0.4, 0.4, 0.4, 0.01);
            case "wrong_dimension" -> level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.5, c.z, 24, 0.6, 1.0, 0.6, 0.05);
            default -> { }
        }
        level.playSound(null, center, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.playSound(null, center, SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 0.6f, 0.7f);
    }
}
