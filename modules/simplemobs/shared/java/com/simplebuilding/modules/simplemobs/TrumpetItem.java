package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

/**
 * Trumpet (BS28-5 / TROMP-3, docs/ai/PLAN-A-MISC05-2026-11-05.md): hold use and a note sounds every {@link #INTERVAL} ticks.
 * The pitch follows the look angle ({@code xRot} +15 deg = lowest, -15 deg = highest) in the C major scale, 15 steps over two
 * octaves = 24 semitones = Vanilla's pitch range 0.5..2.0. Two samples of one steady tone (owner 03.11.: K1, even and bright,
 * no attack, no release sound): notes 0-11 play the low one, 12-24 the high one, each tuned to the middle of its octave so the
 * colour does not tilt. Each sample is {@link #SAMPLE_SECONDS} s with soft fades; played every 0.3 s they overlap into one
 * held tone. No loop instance, so every player in range hears it (server sound).
 */
public class TrumpetItem extends Item {
    public static final int INTERVAL = 6;
    public static final float SAMPLE_SECONDS = 0.5F;
    /** Note 0..24 above the lowest tone; the low sample is tuned to {@link #LOW_REF}, the high one to {@link #HIGH_REF}. */
    public static final int LOW_REF = 6, HIGH_REF = 18, SPLIT = 12, MAX_NOTE = 24;
    public static final int STAGES = 15;
    public static final float TILT = 15.0F, STAGE_DEGREES = 2.0F;
    private static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};

    public TrumpetItem(Properties properties) { super(properties); }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.TOOT_HORN; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!MobsConfig.trumpetEnabled) return InteractionResult.PASS;
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel server) || !MobsConfig.trumpetEnabled) return;
        if ((getUseDuration(stack, user) - remainingUseTicks) % INTERVAL == 0) play(server, user, note(user.getXRot()));
    }

    /** Scale step 0..14 of a look angle: {@link #TILT} deg below the horizon is 0, {@link #TILT} deg above is 14. */
    public static int stage(float xRot) {
        return Math.max(0, Math.min(STAGES - 1, Math.round((TILT - xRot) / STAGE_DEGREES)));
    }

    /** Semitones above the lowest tone (0..24) of scale step {@code stage}: C major over two octaves, the top step is the high C. */
    public static int semitone(int stage) {
        int s = Math.max(0, Math.min(STAGES - 1, stage));
        return s == STAGES - 1 ? MAX_NOTE : s / 7 * 12 + MAJOR[s % 7];
    }

    public static int note(float xRot) { return semitone(stage(xRot)); }

    public static boolean high(int note) { return note >= SPLIT; }

    /** Pitch of {@code note} for its sample (0.707..1.414, inside Vanilla's 0.5..2.0). */
    public static float pitch(int note) {
        return (float) Math.pow(2.0, (note - (high(note) ? HIGH_REF : LOW_REF)) / 12.0);
    }

    /** Loudness: the longer a lowered sample plays, the more of them overlap, so it is made a little quieter (about 16-24 blocks). */
    public static float volume(float pitch) {
        return 1.5F * Math.min(1.0F, (float) Math.sqrt(0.6F * pitch));
    }

    public static SoundEvent sound(int note) { return high(note) ? MobsSounds.TRUMPET_HIGH : MobsSounds.TRUMPET_LOW; }

    public static void play(ServerLevel level, LivingEntity user, int note) {
        float pitch = pitch(note);
        level.playSound(null, user.getX(), user.getEyeY(), user.getZ(), sound(note), SoundSource.PLAYERS, volume(pitch), pitch);
        // Vanilla's note particle takes its colour from dx (0..1), like a note block.
        level.sendParticles(ParticleTypes.NOTE, user.getX(), user.getY() + user.getBbHeight() + 0.5, user.getZ(), 0, note / 24.0, 0.0, 0.0, 1.0);
    }
}
