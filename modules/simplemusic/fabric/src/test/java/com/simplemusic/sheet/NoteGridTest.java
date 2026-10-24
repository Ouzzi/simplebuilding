package com.simplemusic.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The editor grid (WB24-4, E2 A + E3 B). <b>What breaks this test:</b> a grid that writes code which no longer parses to
 * the same tones (round trip), a toggle that does not set/remove a note or ignores the chord/step limits, or broken code
 * that replaces the grid instead of leaving it standing (fromCode = null).
 */
class NoteGridTest {
    private static final NoteCode.Instrument HARP = new NoteCode.Instrument(NoteCode.Kind.NOTE_BLOCK, "harp", 0);
    private static final NoteCode.Instrument BASS = new NoteCode.Instrument(NoteCode.Kind.NOTE_BLOCK, "bass", -2);

    /** The song as comparable text: tempo, steps and per step the tones (instrument, note, volume) in sorted order. */
    static String describe(NoteCode.Song song) {
        StringBuilder out = new StringBuilder("tempo " + song.tempo() + " steps " + song.steps());
        for (int s = 0; s < song.steps(); s++) {
            List<String> tones = new ArrayList<>();
            for (NoteCode.Tone t : song.tonesAt(s)) tones.add(song.instruments().get(t.instrument()) + "/" + t.note() + "/" + t.volume());
            tones.sort(null);
            out.append('\n').append(s).append(' ').append(tones);
        }
        return out.toString();
    }

    private static NoteCode.Song song(String code) {
        NoteCode.Result r = NoteCode.parse(code);
        assertTrue(r.ok(), () -> "problems in " + code + ": " + r.problems());
        return r.song();
    }

    private static void roundTrip(String code) {
        NoteGrid grid = NoteGrid.fromCode(code, NoteCode.SoundPolicy.ALL);
        String written = grid.toCode();
        assertEquals(describe(song(code)), describe(song(written)), () -> "written:\n" + written);
    }

    @Test
    void roundTripKeepsTheSong() {
        roundTrip("tempo 6\nharp: c4 e4 g4 - c5 . . g4\nbass: c3 . [c2 g2] .");
        roundTrip("# comment\n$h = harp\n$h: x2 { c4 d4 } @vol 0.6 e4\nx2 {\n  bass: c3\n  bell: c6 d6\n}\nflute: . . . . . . . .");
        roundTrip("$k = sound minecraft:entity.cat.ambient\n$k: f#4 . 3\ncreeper: c4 . c4");
        // two tracks of the same instrument: 8 tones in one step need a second voice
        roundTrip("harp: [f#3 g3 a3 b3]\n$h = harp\n$h: [c4 d4 e4 f4]");
        roundTrip("harp: c4 . . .\nbass: . . . . . . . ."); // trailing rests keep the length
    }

    @Test
    void writesFlatReadableCode() {
        NoteGrid grid = NoteGrid.fromCode("tempo 5\nharp: c4 [e4 c4] .\nbass: . c3", NoteCode.SoundPolicy.ALL);
        assertEquals("tempo 5\nharp: c4 [c4 e4] .\nbass: . c3", grid.toCode());
        assertEquals("tempo 4", NoteGrid.empty().toCode());
    }

    @Test
    void toggleSetsAndRemovesNotes() {
        NoteGrid grid = NoteGrid.empty();
        assertEquals(NoteGrid.Toggle.ADDED, grid.toggle(HARP, 2, 6));
        assertTrue(grid.has(HARP, 2, 6));
        assertEquals("tempo 4\nharp: . . c4", grid.toCode());
        assertEquals(NoteGrid.Toggle.ADDED, grid.toggle(BASS, 0, 6));
        assertEquals("tempo 4\nharp: . . c4\nbass: c2", grid.toCode());
        assertEquals(NoteGrid.Toggle.REMOVED, grid.toggle(HARP, 2, 6), "a second click takes the note away");
        assertFalse(grid.has(HARP, 2, 6));
        assertFalse(grid.remove(HARP, 2, 6), "right click on an empty cell does nothing");
        assertTrue(grid.remove(BASS, 0, 6));
        assertEquals(NoteGrid.Toggle.REFUSED, grid.toggle(HARP, 0, 25), "outside the 25 notes");
    }

    @Test
    void toggleKeepsTheLimits() {
        NoteGrid grid = NoteGrid.empty();
        for (int n = 0; n < NoteCode.MAX_CHORD; n++) assertEquals(NoteGrid.Toggle.ADDED, grid.toggle(HARP, 0, n));
        assertEquals(NoteGrid.Toggle.REFUSED, grid.toggle(HARP, 0, 10), "chord limit");
        for (int n = 0; n < NoteCode.MAX_TONES_PER_STEP - NoteCode.MAX_CHORD; n++) grid.toggle(BASS, 0, n);
        NoteCode.Instrument bell = new NoteCode.Instrument(NoteCode.Kind.NOTE_BLOCK, "bell", 2);
        assertEquals(NoteGrid.Toggle.REFUSED, grid.toggle(bell, 0, 0), "tones per step");
        assertTrue(NoteCode.parse(grid.toCode()).ok());
        NoteGrid chord = NoteGrid.fromCode("harp: @vol 0.5 c4", NoteCode.SoundPolicy.ALL);
        chord.toggle(HARP, 0, 10);
        assertEquals("tempo 4\nharp: @vol 0.5 [c4 e4]", chord.toCode(), "an added chord tone keeps the chord's volume");
    }

    @Test
    void brokenCodeLeavesTheGridStanding() {
        assertNull(NoteGrid.fromCode("harp: c4 zz", NoteCode.SoundPolicy.ALL));
        assertNull(NoteGrid.fromCode("$k = sound minecraft:a.b\n$k: c4", id -> "sound_not_allowed"));
    }

    @Test
    void headsSitOnOneNote() {
        NoteCode.Instrument creeper = new NoteCode.Instrument(NoteCode.Kind.HEAD, "creeper", 0);
        NoteGrid grid = NoteGrid.empty();
        grid.toggle(creeper, 0, 3);
        assertTrue(grid.has(creeper, 0, 20), "any row of a head is its one sound");
        assertEquals(NoteGrid.Toggle.REMOVED, grid.toggle(creeper, 0, 7));
    }
}
