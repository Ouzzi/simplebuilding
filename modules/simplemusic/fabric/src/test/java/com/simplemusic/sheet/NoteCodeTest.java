package com.simplemusic.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The note code (concept §3.1). <b>What breaks this test:</b> a changed note mapping (harp f#3 = note 0, written pitch per
 * instrument octave), a block repetition that no longer keeps the tracks in step, a limit (tracks, tones per step,
 * chord, 10 minutes, code length) that stops firing, or a problem without its position.
 */
class NoteCodeTest {
    private static NoteCode.Result ok(String code) {
        NoteCode.Result r = NoteCode.parse(code);
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        return r;
    }

    private static List<String> keys(String code) {
        return NoteCode.parse(code).problems().stream().map(NoteCode.Problem::key).toList();
    }

    @Test
    void noteMapping() {
        assertEquals(0, NoteCode.noteOf("f#3", 0));
        assertEquals(6, NoteCode.noteOf("c4", 0));
        assertEquals(12, NoteCode.noteOf("f#4", 0));
        assertEquals(12, NoteCode.noteOf("gb4", 0));
        assertEquals(24, NoteCode.noteOf("f#5", 0));
        assertEquals(-1, NoteCode.noteOf("f3", 0));
        assertEquals(-1, NoteCode.noteOf("g5", 0));
        assertEquals(6, NoteCode.noteOf("c2", -2), "bass sounds two octaves lower");
        assertEquals(6, NoteCode.noteOf("c6", 2), "bell sounds two octaves higher");
        assertEquals(17, NoteCode.noteOf("17", 0));
        assertEquals(-1, NoteCode.noteOf("25", 0));
        assertEquals("f#3", NoteCode.nameOf(0, 0));
        assertEquals("c4", NoteCode.nameOf(6, 0));
        assertEquals("f#1", NoteCode.nameOf(0, -2));
        for (int octave = -2; octave <= 2; octave++) {
            for (int n = 0; n <= 24; n++) assertEquals(n, NoteCode.noteOf(NoteCode.nameOf(n, octave), octave));
        }
    }

    @Test
    void conceptExample() {
        NoteCode.Song song = ok("""
                # Kommentar
                tempo 4
                $h = harp
                $k = sound minecraft:entity.cat.ambient
                $h: c4 e4 g4 - c5 . . g4
                bass: c3 . c3 . [c3 f#3] .
                x2 { $h: c4 d4 }
                $k: . . . . . . . . . . . f#4
                """).song();
        assertEquals(4, song.tempo());
        // harp 8 steps, bass 6, then the block pads to 8 and adds 2 x 2 steps; the cat line is 12 long
        assertEquals(12, song.steps());
        assertEquals(48, song.ticks());
        assertEquals(3, song.instruments().size());
        List<NoteCode.Tone> first = song.tonesAt(0);
        assertEquals(2, first.size(), "harp c4 and bass c3 together");
        assertEquals(6, first.get(0).note());
        assertEquals(3, song.tonesAt(4).size(), "harp c5 + bass chord of 2");
    }

    @Test
    void chordsAndSteps() {
        NoteCode.Song song = ok("harp: [c4 e4 g4] . - c4\nbass: f#2").song();
        assertEquals(4, song.steps());
        assertEquals(4, song.tonesAt(0).size());
        assertEquals(0, song.tonesAt(1).size());
        assertEquals(0, song.tonesAt(2).size());
        assertEquals(1, song.tonesAt(3).size());
        assertEquals(1.0F, song.tonesAt(0).get(0).volume());
    }

    @Test
    void sameLabelContinuesTheTrack() {
        NoteCode.Song song = ok("harp: c4 d4\nbass: c3\nharp: e4").song();
        assertEquals(3, song.steps());
        assertEquals(1, song.tonesAt(2).size());
        assertEquals(10, song.tonesAt(2).get(0).note());
    }

    @Test
    void blockRepetitionKeepsTracksInStep() {
        NoteCode.Song song = ok("""
                harp: c4 c4 c4
                x3 {
                  harp: d4
                  bass: c3 . e3
                }
                harp: e4
                """).song();
        // sync at 3, block length 3 repeated 3 times = 12, then harp continues at 12
        assertEquals(13, song.steps());
        assertEquals(8, song.tonesAt(3).get(0).note());
        assertEquals(2, song.tonesAt(3).size());
        assertEquals(0, song.tonesAt(4).size());
        assertEquals(1, song.tonesAt(5).size(), "bass e3 only");
        assertEquals(2, song.tonesAt(6).size());
        assertEquals(10, song.tonesAt(12).get(0).note());
    }

    @Test
    void inlineRepetitionAndVolume() {
        NoteCode.Song song = ok("harp: x3 { c4 . } @vol 0.5 d4").song();
        assertEquals(7, song.steps());
        assertEquals(1, song.tonesAt(4).size());
        assertEquals(0.5F, song.tonesAt(6).get(0).volume());
        assertEquals(1.0F, song.tonesAt(0).get(0).volume());
    }

    @Test
    void headsIgnorePitch() {
        NoteCode.Song song = ok("creeper: c4 f#5").song();
        assertEquals(12, song.tonesAt(0).get(0).note());
        assertEquals(12, song.tonesAt(1).get(0).note());
        assertFalse(song.instruments().get(0).tunable());
    }

    @Test
    void problemsCarryTheirPosition() {
        String code = "harp: c4 zz g9";
        NoteCode.Result r = NoteCode.parse(code);
        assertEquals(List.of("bad_note", "out_of_range"), r.problems().stream().map(NoteCode.Problem::key).toList());
        NoteCode.Problem p = r.problems().get(0);
        assertEquals("zz", code.substring(p.start(), p.end()));
        assertEquals(0, p.line());
        assertEquals(List.of("g9", "f#3", "f#5"), r.problems().get(1).args());
        assertEquals(NoteCode.STYLE_ERROR, r.styles()[code.indexOf("zz")]);
        assertEquals(NoteCode.STYLE_NOTE, r.styles()[code.indexOf("c4")]);
        assertEquals(NoteCode.STYLE_INSTRUMENT, r.styles()[0]);
        assertEquals(NoteCode.Song.EMPTY, r.song(), "all or nothing");
    }

    @Test
    void lineErrors() {
        assertEquals(List.of("unknown_instrument"), keys("trumpet: c4"));
        assertEquals(List.of("unknown_alias"), keys("$x: c4"));
        assertEquals(List.of("bad_tempo"), keys("tempo 0"));
        assertEquals(List.of("bad_tempo"), keys("tempo 21"));
        assertEquals(List.of("unknown_line"), keys("hello world"));
        assertEquals(List.of("unclosed_block"), keys("x2 {\nharp: c4"));
        assertEquals(List.of("unexpected"), keys("}"));
        assertEquals(List.of("unclosed_chord"), keys("harp: [c4 e4"));
        assertEquals(List.of("chord_too_big"), keys("harp: [c4 d4 e4 f#4 g4]"));
        assertEquals(List.of("bad_volume"), keys("harp: @vol 2 c4"));
        assertEquals(List.of("bad_repeat"), keys("harp: x65 { c4 }"));
        assertEquals(List.of("duplicate_alias"), keys("$a = harp\n$a = bass"));
        assertEquals(List.of("bad_sound"), keys("$a = sound NotAnId"));
        assertTrue(ok("# only a comment\n\n").song().steps() == 0);
    }

    @Test
    void soundPolicyRefuses() {
        NoteCode.SoundPolicy policy = id -> id.startsWith("minecraft:music.") ? "blocked_sound" : null;
        NoteCode.Result r = NoteCode.parse("$m = sound minecraft:music.game\n$m: f#4", policy);
        assertEquals(List.of("blocked_sound", "unknown_alias"), r.problems().stream().map(NoteCode.Problem::key).toList());
        assertTrue(NoteCode.parse("$c = sound minecraft:entity.cat.ambient\n$c: f#4", policy).ok());
    }

    @Test
    void limits() {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 17; i++) many.append("$t").append(i).append(" = harp\n$t").append(i).append(": ").append(". ".repeat(i)).append("c4\n");
        assertEquals(List.of("too_many_tracks"), keys(many.toString()));

        StringBuilder loud = new StringBuilder();
        for (int i = 0; i < 3; i++) loud.append("$l").append(i).append(" = harp\n$l").append(i).append(": [c4 d4 e4]\n");
        assertEquals(List.of("too_many_tones"), keys(loud.toString()), "9 tones in one step");

        assertEquals(List.of("too_long"), keys("tempo 20\nharp: x60 { x10 { c4 } } c4"), "601 steps x 20 ticks > 10 min");
        assertTrue(NoteCode.parse("tempo 20\nharp: x60 { x10 { c4 } }").ok(), "exactly 600 steps x 20 = 12000 ticks");
        assertTrue(keys("harp: x64 { x64 { x64 { c4 } } }").contains("too_long"), "nested repeats stop at the limit");
        assertEquals(List.of("too_long_code"), keys("#".repeat(NoteCode.MAX_CODE_LENGTH + 1)));
    }
}
