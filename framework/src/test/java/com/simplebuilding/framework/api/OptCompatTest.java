package com.simplebuilding.framework.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Simple Optimization compat rule (concept §4.3) and its config file, without a loader. */
class OptCompatTest {
    private static final OptCompat.Area LEAVES = new OptCompat.Area("leaf_culling", "O1", OptCompat.Side.CLIENT,
            List.of("cullleaves", "moreculling"), false);
    private static final OptCompat.Area SIDE_BY_SIDE = new OptCompat.Area("x", "X1", OptCompat.Side.SERVER, List.of("other"), true);

    private static OptCompat.Decision decide(OptCompat.Area a, OptCompat.Mode m, Set<String> loaded, OptCompat.Winner w) {
        return OptCompat.resolve(a, m, true, loaded::contains, w);
    }

    @Test
    void withoutRivalOursRunsInEveryModeButOff() {
        for (OptCompat.Mode m : List.of(OptCompat.Mode.AUTO, OptCompat.Mode.OURS, OptCompat.Mode.THEIRS)) {
            var d = decide(LEAVES, m, Set.of(), OptCompat.Winner.THEIRS);
            assertTrue(d.active(), m.name());
            assertEquals(OptCompat.Reason.NO_RIVAL, d.reason());
        }
        assertFalse(decide(LEAVES, OptCompat.Mode.OFF, Set.of(), OptCompat.Winner.OURS).active());
        assertFalse(OptCompat.resolve(LEAVES, OptCompat.Mode.AUTO, false, s -> false, OptCompat.Winner.OURS).active(), "super-mod switch");
    }

    @Test
    void rivalWinsByDefaultAndOursNeedsCoexistence() {
        var auto = decide(LEAVES, OptCompat.Mode.AUTO, Set.of("moreculling"), OptCompat.Winner.THEIRS);
        assertFalse(auto.active());
        assertEquals("moreculling", auto.rival());
        assertEquals(OptCompat.Reason.RIVAL_WINS, auto.reason());
        var forced = decide(LEAVES, OptCompat.Mode.OURS, Set.of("cullleaves"), OptCompat.Winner.THEIRS);
        assertFalse(forced.active(), "same target: falls back to theirs");
        assertEquals(OptCompat.Reason.OURS_NOT_ALLOWED, forced.reason());
        var measured = decide(LEAVES, OptCompat.Mode.AUTO, Set.of("cullleaves"), OptCompat.Winner.OURS);
        assertFalse(measured.active());
        assertTrue(measured.hint().contains("cullleaves"), "hint names the other mod, never switches it off");
        assertTrue(decide(SIDE_BY_SIDE, OptCompat.Mode.OURS, Set.of("other"), OptCompat.Winner.THEIRS).active());
        assertTrue(decide(SIDE_BY_SIDE, OptCompat.Mode.AUTO, Set.of("other"), OptCompat.Winner.OURS).active());
        assertFalse(decide(SIDE_BY_SIDE, OptCompat.Mode.AUTO, Set.of("other"), OptCompat.Winner.THEIRS).active());
        assertFalse(decide(SIDE_BY_SIDE, OptCompat.Mode.THEIRS, Set.of("other"), OptCompat.Winner.OURS).active());
    }

    @Test
    void tableAndPublish() {
        var d = decide(LEAVES, OptCompat.Mode.AUTO, Set.of("cullleaves"), OptCompat.Winner.THEIRS);
        String table = OptCompat.table("simpleoptrender", List.of(d));
        assertTrue(table.contains("O1") && table.contains("off") && table.contains("cullleaves"), table);
        OptCompat.publish("test-owner", List.of(d));
        assertFalse(OptCompat.active("test-owner", "leaf_culling"));
        assertFalse(OptCompat.active("test-owner", "unknown"));
        assertEquals(OptCompat.Mode.AUTO, OptCompat.Mode.parse("nonsense"));
        assertEquals(OptCompat.Mode.THEIRS, OptCompat.Mode.parse(" Theirs "));
    }

    @Test
    void settingsMergeDefaultsClampAndQuarantine() throws Exception {
        Map<String, Map<String, Object>> defaults = new LinkedHashMap<>();
        defaults.put("leaf_culling", Map.of("depth", 0));
        defaults.put("lightmap_cache", Map.of());
        var dir = Files.createTempDirectory("optsettings");
        Files.writeString(dir.resolve("o.json"), "{\"areas\":{\"leaf_culling\":{\"mode\":\"off\",\"depth\":99}}}");
        var s = OptSettings.load(dir, "o.json", defaults, w -> {});
        assertEquals(OptCompat.Mode.OFF, s.mode("leaf_culling"));
        assertEquals(4, s.intValue("leaf_culling", "depth", 0, 0, 4));
        assertEquals(OptCompat.Mode.AUTO, s.mode("lightmap_cache"));
        var written = JsonParser.parseString(Files.readString(dir.resolve("o.json"))).getAsJsonObject();
        assertTrue(written.getAsJsonObject("areas").has("lightmap_cache") && written.has("conflictScan"), "file completed");
        Files.writeString(dir.resolve("o.json"), "{ broken");
        assertEquals(OptCompat.Mode.AUTO, OptSettings.load(dir, "o.json", defaults, w -> {}).mode("leaf_culling"));
        assertTrue(OptSettings.switchOn(dir, "missing.json", "enableX", w -> {}));
        Files.writeString(dir.resolve("s.json"), "{\"enableX\": false}");
        assertFalse(OptSettings.switchOn(dir, "s.json", "enableX", w -> {}));
        assertEquals(OptCompat.Winner.OURS, OptSettings.priorities("{\"areas\":{\"a\":{\"winner\":\"ours\"}}}").get("a"));
        assertTrue(OptSettings.priorities("nope").isEmpty());
    }
}
