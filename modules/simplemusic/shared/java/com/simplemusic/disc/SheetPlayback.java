package com.simplemusic.disc;

import com.simplemusic.orchestrion.OrchestrionBlockEntity;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;

/**
 * A pressed composition disc in the Vanilla jukebox (owner 21.10.): the server plays the steps like the Orchestrion
 * ({@link OrchestrionBlockEntity#playTones}), once, then the disc stays in silently like a finished Vanilla disc.
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
        if (s.tickInStep == 0) OrchestrionBlockEntity.playTones(level, pos, s.song.tonesAt(s.step), s.sounds);
        if (++s.tickInStep >= s.song.tempo()) {
            s.tickInStep = 0;
            if (++s.step >= s.song.steps()) {
                SESSIONS.remove(key);
                return true;
            }
        }
        return false;
    }

    public static void stop(ServerLevel level, BlockPos pos) {
        SESSIONS.remove(GlobalPos.of(level.dimension(), pos));
    }

    public static void clear() {
        SESSIONS.clear();
    }
}
