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
    public static SoundEvent sound(int note) { return high(note) ? MobsSounds.TRUMPET_HIGH : MobsSounds.TRUMPET_LOW; }
}
