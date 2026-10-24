package com.simplemusic.disc;

import com.simplemusic.config.MusicConfig;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * A pressed composition disc in the Vanilla jukebox (owner 21.10.): the server plays the steps itself like a note block
 * ({@link #playTones}, so every listener is in sync and amplifiers work), once, then the disc stays in silently like a
 * finished Vanilla disc. (The Orchestrion that also played them is gone, B23-M9.)
 * In memory only; after a restart the disc starts again.
 */
public final class SheetPlayback {
    private static final Map<GlobalPos, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final NoteCode.Song song;
        final Holder<SoundEvent>[] sounds;
        int step;
        int tickInStep;

        Session(NoteCode.Song song) {
            this.song = song;
            this.sounds = SheetSounds.sounds(song);
        }
    }

    private SheetPlayback() {}

    /** Starts the composition at a block; false when the code does not parse with the server's rules. */
    public static boolean start(ServerLevel level, BlockPos pos, String code) {
        NoteCode.Result r = NoteCode.parse(code, SheetSounds.policy());
        if (!r.ok() || r.song().steps() == 0) return false;
        SESSIONS.put(GlobalPos.of(level.dimension(), pos.immutable()), new Session(r.song()));
        return true;
    }

    public static boolean playing(ServerLevel level, BlockPos pos) {
        return SESSIONS.containsKey(GlobalPos.of(level.dimension(), pos));
    }

    /** One tick; true when the piece has just ended (the session is gone then). */
    public static boolean tick(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        Session s = SESSIONS.get(key);
        if (s == null) return false;
        if (s.tickInStep == 0) playTones(level, pos, s.song.tonesAt(s.step), s.sounds);
        if (++s.tickInStep >= s.song.tempo()) {
            s.tickInStep = 0;
            if (++s.step >= s.song.steps()) {
                SESSIONS.remove(key);
                return true;
            }
        }
        return false;
    }

    /**
     * Plays one step's tones at a block like a note block (at most {@code maxTonesPerTick}, range {@code toneRange}) with a
     * note particle; returns how many sounded.
     */
    public static int playTones(ServerLevel level, BlockPos pos, java.util.List<NoteCode.Tone> tones, Holder<SoundEvent>[] sounds) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        float range = MusicConfig.toneRange / 16.0F;
        int played = 0;
        for (NoteCode.Tone tone : tones) {
            if (played >= MusicConfig.maxTonesPerTick) break;
            Holder<SoundEvent> sound = sounds[tone.instrument()];
            if (sound == null) continue;
            level.playSeededSound(null, x, y, z, sound, SoundSource.RECORDS, volume(tone, range), tone.pitch(), level.getRandom().nextLong());
            played++;
        }
        if (played > 0) {
            level.sendParticles(ParticleTypes.NOTE, x, y + 0.7, z, 0, tones.get(0).note() / 24.0, 0.0, 0.0, 1.0);
        }
        return played;
    }

    /**
     * The game volume of a tone. Vanilla clamps the gain to 1 and only lets a volume above 1 widen the range (a note block
     * plays at 3.0 = 48 blocks), so {@code range * volume} made every {@code @vol} from 0.34 up as loud as a full tone:
     * a quieter tone is sent at its own volume (hearing range 16), a full one with the configured range.
     */
    public static float volume(NoteCode.Tone tone, float range) {
        return tone.volume() < 1.0F ? tone.volume() : range;
    }

    public static void stop(ServerLevel level, BlockPos pos) {
        SESSIONS.remove(GlobalPos.of(level.dimension(), pos));
    }

    public static void clear() {
        SESSIONS.clear();
    }
}
