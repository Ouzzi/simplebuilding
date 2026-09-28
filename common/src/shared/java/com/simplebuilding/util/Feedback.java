package com.simplebuilding.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Feedback without on-screen text (owner rule: gadgets speak through sounds, particles and block
 * states, never through chat or the action bar). Helpers for the cases vanilla has no one-liner for.
 */
public final class Feedback {
    private Feedback() {
    }

    /**
     * Plays a sound that only this player hears, at the player's position (a warning that concerns
     * nobody else, e.g. a flypad's edge). Vanilla's {@code Level#playSound(player, ...)} does the
     * opposite: it plays for everyone except that player.
     */
    public static void playTo(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }
}
