package com.simplebuilding.modules.simpletweaks.play;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;

/** A sound only one player hears, at their own position. */
public final class PlaySounds {
    public static void to(ServerPlayer p, Holder<SoundEvent> sound, float volume, float pitch) {
        if (p.connection == null || volume <= 0) return;
        p.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS, p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
    }
    private PlaySounds() {}
}
