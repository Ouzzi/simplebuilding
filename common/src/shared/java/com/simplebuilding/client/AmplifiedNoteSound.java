package com.simplebuilding.client;

import com.simplebuilding.networking.AmplifiedNotePayload;
import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;

/**
 * The server already checked range; retain direction, pitch, seed and the player's volume settings. The note plays
 * louder than vanilla by the amplifier factor ({@link SpeakerBoost#noteGain}, 2026-10-06).
 */
public final class AmplifiedNoteSound extends SimpleSoundInstance implements AmplifiedSound {
    private final float gain;

    private AmplifiedNoteSound(AmplifiedNotePayload payload) {
        super(payload.sound().getSound().value().location(), payload.sound().getSource(), 1.0F, payload.sound().getPitch(),
                RandomSource.create(payload.sound().getSeed()), false, 0, SoundInstance.Attenuation.NONE,
                payload.sound().getX(), payload.sound().getY(), payload.sound().getZ(), false);
        this.gain = SpeakerBoost.noteGain(payload.sound().getVolume());
    }

    @Override
    public float simplebuilding$gain() {
        return gain;
    }

    public static void play(AmplifiedNotePayload payload) {
        Minecraft.getInstance().getSoundManager().play(new AmplifiedNoteSound(payload));
    }
}
