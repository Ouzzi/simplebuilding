package com.simplebuilding.modules.simpleoptmemory;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Opt Memory: memory areas of Simple Optimization (concept §3: O4 block-state tables vs FerriteCore, O5 model
 * baking vs ModernFix). Stage 1 ships no area yet, only the switch, the config file and the detection log line, so the
 * sub-mod already shows which memory mods would cover its later areas.
 */
public final class MemoryOpt {
    public static final String MOD_ID = "simpleoptmemory", FILE = MOD_ID + ".json";
    public static final String SUPER_FILE = "simpleoptimization.json", SWITCH = "enableSimpleOptMemory";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    /** Mod ids from the published 26.3 jars (fabric.mod.json, Modrinth 2026-10-18), for the later areas O4/O5. */
    public static final List<String> WATCHED = List.of("ferritecore", "modernfix");
    public static final List<OptCompat.Area> AREAS = List.of();
    private static volatile boolean booted;

    private MemoryOpt() {}

    public static synchronized void boot(Path configDir, Predicate<String> isLoaded) {
        if (booted) return;
        booted = true;
        OptSettings.load(configDir, FILE, Map.of(), LOG::warn);
        OptCompat.publish(MOD_ID, List.of());
        List<String> found = WATCHED.stream().filter(isLoaded).toList();
        LOG.info("{} (switch {}); memory mods present: {}", OptCompat.table(MOD_ID, List.of()),
                OptSettings.switchOn(configDir, SUPER_FILE, SWITCH, LOG::warn) ? "on" : "off", found.isEmpty() ? "none" : found);
    }
}
