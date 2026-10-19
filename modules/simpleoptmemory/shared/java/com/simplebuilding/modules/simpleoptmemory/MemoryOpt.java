package com.simplebuilding.modules.simpleoptmemory;

import com.simplebuilding.framework.api.OptAreas;
import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Opt Memory: memory areas of Simple Optimization (concept §3: O4 block-state tables vs FerriteCore, O5 model
 * baking vs ModernFix later). {@link #boot} runs in the mixin plugin before any game class loads, like Simple Opt Render.
 */
public final class MemoryOpt {
    public static final String MOD_ID = "simpleoptmemory", FILE = MOD_ID + ".json";
    public static final String SUPER_FILE = "simpleoptimization.json", SWITCH = "enableSimpleOptMemory";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    /** Mod ids from the published 26.3 jars (fabric.mod.json, Modrinth 2026-10-18); modernfix is watched for O5 later. */
    public static final List<String> WATCHED = List.of("ferritecore", "modernfix");

    /** O4: shared property and face tables of block states (FerriteCore covers the same constructors). */
    public static final OptCompat.Area STATE_TABLES = new OptCompat.Area("state_tables", "O4", OptCompat.Side.BOTH,
            List.of("ferritecore"), false);
    public static final List<OptCompat.Area> AREAS = List.of(STATE_TABLES);
    public static final OptAreas RUNTIME = new OptAreas(MOD_ID, AREAS);
    private static volatile boolean booted;

    private MemoryOpt() {}

    public static Map<String, Map<String, Object>> defaults() {
        Map<String, Map<String, Object>> d = new LinkedHashMap<>();
        d.put(STATE_TABLES.id(), Map.of());
        return d;
    }

    public static synchronized void boot(Path configDir, Predicate<String> isLoaded) {
        if (booted) return;
        booted = true;
        apply(OptSettings.load(configDir, FILE, defaults(), LOG::warn),
                OptSettings.switchOn(configDir, SUPER_FILE, SWITCH, LOG::warn), isLoaded, priorities());
        List<String> found = WATCHED.stream().filter(isLoaded).toList();
        LOG.info("{}: memory mods present: {}", MOD_ID, found.isEmpty() ? "none" : found);
    }

    public static List<OptCompat.Decision> apply(OptSettings s, boolean switchOn, Predicate<String> isLoaded, Map<String, OptCompat.Winner> table) {
        return RUNTIME.decide(s, switchOn, isLoaded, table, LOG::info);
    }

    public static Map<String, OptCompat.Winner> priorities() {
        try (InputStream in = MemoryOpt.class.getResourceAsStream("/data/" + MOD_ID + "/priorities.json")) {
            return in == null ? Map.of() : OptSettings.priorities(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static boolean live(OptCompat.Area area) {
        return RUNTIME.live(area);
    }
}
