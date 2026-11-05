package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
 * Trumpet (BS28-5 / TROMP-3, docs/ai/PLAN-A-MISC05-2026-11-05.md): while use is held it plays one continuous tone (owner 05.11.).
 * The tone itself is a looping client sound ({@code client.TrumpetLoop}, which follows the player and its pitch live); the server
 * sends start/change/stop to players in range ({@link TrumpetNet}). The pitch follows the look angle ({@code xRot} +15 deg = lowest,
 * -15 deg = highest) in the C major scale, 15 steps over two octaves = 24 semitones. Two seamless loop samples of one steady tone
 * (K1, even and bright, no attack, no release): notes 0-11 use the low one, 12-24 the high one, each tuned to the middle of its octave.
 */
public class TrumpetItem extends Item {
    /** Loudness of the loop: reaches about 24 blocks. */
    public static final float VOLUME = 1.5F;
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
        int note = note(user.getXRot());
        if (TrumpetNet.update(server.players(), user, note, server.getGameTime())) {
            // Vanilla's note particle takes its colour from dx (0..1), like a note block.
            server.sendParticles(ParticleTypes.NOTE, user.getX(), user.getY() + user.getBbHeight() + 0.5, user.getZ(), 0, note / 24.0, 0.0, 0.0, 1.0);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (level instanceof ServerLevel server) TrumpetNet.stop(server.players(), user);
        return super.releaseUsing(stack, level, user, remainingUseTicks);
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

    public static SoundEvent sound(int note) { return high(note) ? MobsSounds.TRUMPET_HIGH : MobsSounds.TRUMPET_LOW; }
}
