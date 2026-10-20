package com.simplemusic.client;

import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Test listening in the editor (owner decision N2 e1): the client plays the code itself, non-positional, on the
 * "Jukebox/Note Blocks" volume. Nothing reaches the server or other players.
 */
public final class SheetPreview {
    private static NoteCode.Song song;
    private static Holder<SoundEvent>[] sounds;
    private static int step;
    private static int tick;

    private SheetPreview() {}

    public static void play(NoteCode.Song newSong) {
        song = newSong.steps() == 0 ? null : newSong;
        sounds = song == null ? null : SheetSounds.sounds(song);
        step = 0;
        tick = 0;
    }

    public static void stop() {
        song = null;
    }

    public static boolean playing() {
        return song != null;
    }

    /** Called every client tick. */
    public static void tick() {
        if (song == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            for (NoteCode.Tone tone : song.tonesAt(step)) {
                Holder<SoundEvent> sound = sounds[tone.instrument()];
                if (sound == null) continue;
                mc.getSoundManager().play(new SimpleSoundInstance(sound.value().location(), SoundSource.RECORDS, tone.volume(),
                        tone.pitch(), SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
            }
        }
        if (++tick >= song.tempo()) {
            tick = 0;
            if (++step >= song.steps()) song = null;
        }
    }
}
