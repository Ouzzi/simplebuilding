package com.simplebuilding.modules.simpleoptserver;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import com.simplebuilding.framework.api.SubMods;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Opt Server: server areas of Simple Optimization (concept §3, §5). Stage 1/2: S5a dynamic view distance.
 * Areas that change gameplay (S5b activation range, mob caps; here: lowering the simulation distance) default off.
 */
public final class ServerOpt {
    public static final String MOD_ID = "simpleoptserver", FILE = MOD_ID + ".json";
    public static final String SUPER_FILE = "simpleoptimization.json", SWITCH = "enableSimpleOptServer";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    /** Mod id from ServerCore's published 26.3 jar (fabric.mod.json, Modrinth 2026-10-18). */
    public static final OptCompat.Area DYNAMIC_DISTANCE = new OptCompat.Area("dynamic_distance", "S5a", OptCompat.Side.SERVER,
            List.of("servercore"), false);
    public static final List<OptCompat.Area> AREAS = List.of(DYNAMIC_DISTANCE);

    private static volatile DynamicDistance.Params params = DynamicDistance.Params.DEFAULTS;
    private static volatile boolean booted;

    private ServerOpt() {}

    public static Map<String, Map<String, Object>> defaults() {
        Map<String, Map<String, Object>> d = new LinkedHashMap<>();
        var p = DynamicDistance.Params.DEFAULTS;
        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("targetMspt", p.targetMspt());
        dist.put("minView", p.minView());
        dist.put("minSimulation", p.minSimulation());
        dist.put("stepSeconds", p.stepSeconds());
        dist.put("adjustSimulation", p.adjustSimulation());
        d.put(DYNAMIC_DISTANCE.id(), dist);
        return d;
    }

    public static synchronized void boot(Path configDir, Predicate<String> isLoaded) {
        if (booted) return;
        booted = true;
        apply(OptSettings.load(configDir, FILE, defaults(), LOG::warn),
                OptSettings.switchOn(configDir, SUPER_FILE, SWITCH, LOG::warn), isLoaded, priorities());
    }

    public static List<OptCompat.Decision> apply(OptSettings s, boolean switchOn, Predicate<String> isLoaded, Map<String, OptCompat.Winner> table) {
        String a = DYNAMIC_DISTANCE.id();
        params = new DynamicDistance.Params(s.doubleValue(a, "targetMspt", 45, 10, 1000), s.intValue(a, "minView", 6, 3, 32),
                s.intValue(a, "minSimulation", 5, 3, 32), s.intValue(a, "stepSeconds", 15, 1, 3600), s.boolValue(a, "adjustSimulation", false));
        List<OptCompat.Decision> out = new ArrayList<>();
        for (OptCompat.Area area : AREAS)
            out.add(OptCompat.resolve(area, s.mode(area.id()), switchOn, isLoaded, table.getOrDefault(area.id(), OptCompat.Winner.THEIRS)));
        OptCompat.publish(MOD_ID, out);
        LOG.info(OptCompat.table(MOD_ID, out));
        return out;
    }

    /** Tests only. */
    static void restore(List<OptCompat.Decision> decisions, DynamicDistance.Params p) {
        OptCompat.publish(MOD_ID, decisions);
        params = p;
    }

    public static Map<String, OptCompat.Winner> priorities() {
        try (InputStream in = ServerOpt.class.getResourceAsStream("/data/" + MOD_ID + "/priorities.json")) {
            return in == null ? Map.of() : OptSettings.priorities(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Decided at start and the runtime super-mod switch still on. */
    public static boolean active(OptCompat.Area area) {
        return OptCompat.active(MOD_ID, area.id()) && SubMods.enabled(MOD_ID);
    }

    public static DynamicDistance.Params params() {
        return params;
    }
}
