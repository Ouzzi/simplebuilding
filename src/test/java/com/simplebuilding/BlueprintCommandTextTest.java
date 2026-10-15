package com.simplebuilding.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Der Befehlsleser der Blaupause (Queue N55), rein textuell: setblock/fill, relative und absolute
 * Koordinaten, Modi, und klare Meldungen mit Zeile statt stillem Ueberspringen.
 *
 * <p><b>Was diesen Test bricht:</b> ein Leser, der Unbekanntes uebergeht, Zeilen falsch zaehlt oder
 * Kommentare/Schraegstrich nicht beherrscht.
 */
class BlueprintCommandTextTest {

    @Test
    void liestSetblockUndFill() {
        BlueprintCommandText.Result r = BlueprintCommandText.parse("# c\n\n/setblock ~1 ~2 ~-3 minecraft:stone\nfill 0 0 0 4 5 6 oak_planks hollow\n");
        assertTrue(r.issues().isEmpty(), r.issues().toString());
        assertEquals(2, r.ops().size());
        assertEquals(3, r.ops().get(0).line());
        assertEquals("minecraft:stone", r.ops().get(0).block());
        assertEquals(-3L, r.ops().get(0).box()[2]);
        assertEquals(BlueprintCommandText.Mode.HOLLOW, r.ops().get(1).mode());
        assertEquals(6L, r.ops().get(1).box()[5]);
    }

    @Test
    void blockzustandMitLeerraumBleibtEinWort() {
        BlueprintCommandText.Result r = BlueprintCommandText.parse("setblock 0 0 0 oak_stairs[facing=north, half=top]");
        assertTrue(r.issues().isEmpty(), r.issues().toString());
        assertEquals("oak_stairs[facing=north, half=top]", r.ops().get(0).block());
    }

    @Test
    void meldetNichtUnterstuetztesMitZeile() {
        String[][] cases = {
                {"say hi", "unknown_command"}, {"setblock ^ ^ ^ stone", "local_coordinate"}, {"setblock 1.5 0 0 stone", "bad_coordinate"},
                {"fill 0 0 0 1 1 1 stone keep", "unsupported_mode"}, {"fill 0 0 0 1 1 1 stone replace dirt", "unsupported_filter"},
                {"setblock 0 0 0 chest{Items:[]}", "block_data"}, {"setblock 0 0 stone", "bad_syntax"}, {"setblock 0 0 0 \"x\"", "bad_block"},
                {"setblock 9999999999 0 0 stone", "bad_coordinate"},
        };
        for (String[] c : cases) {
            BlueprintCommandText.Result r = BlueprintCommandText.parse("setblock 0 0 0 stone\n" + c[0]);
            assertEquals(1, r.issues().size(), c[0]);
            assertEquals(c[1], r.issues().get(0).key(), c[0]);
            assertEquals(2, r.issues().get(0).line(), c[0]);
        }
    }

    @Test
    void begrenztDieBefehlszahl() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= BlueprintCommandText.MAX_COMMANDS; i++) {
            sb.append("setblock 0 0 0 stone\n");
        }
        BlueprintCommandText.Result r = BlueprintCommandText.parse(sb.toString());
        assertEquals("too_many_commands", r.issues().get(0).key());
        assertEquals(BlueprintCommandText.MAX_COMMANDS, r.ops().size());
    }

    @Test
    void formatErkennung() {
        assertEquals("fill", BlueprintCommandText.firstCommand("# x\n  /fill 0 0 0 1 1 1 stone"));
        assertEquals("setblock", BlueprintCommandText.firstCommand("setblock 0 0 0 stone"));
        assertNull(BlueprintCommandText.firstCommand("stone 0..3,0,0"));
        assertNull(BlueprintCommandText.firstCommand("# only comments\n\n"));
    }
}
