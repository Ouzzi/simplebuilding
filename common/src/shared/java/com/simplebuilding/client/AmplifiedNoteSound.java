package com.simplebuilding.client;

import com.simplebuilding.networking.AmplifiedNotePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;

/** The server already checked range; retain direction, pitch, seed and the player's volume settings. */
public final class AmplifiedNoteSound {
    private AmplifiedNoteSound() {
    }

    public static void play(AmplifiedNotePayload payload) {
        var note = payload.sound();
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                note.getSound().value().location(), note.getSource(), 1.0F, note.getPitch(),
                RandomSource.create(note.getSeed()), false, 0, SoundInstance.Attenuation.NONE,
                note.getX(), note.getY(), note.getZ(), false));
    }
}
