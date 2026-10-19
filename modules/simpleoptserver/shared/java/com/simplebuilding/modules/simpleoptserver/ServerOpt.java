package com.simplebuilding.modules.simpleoptserver;

import com.simplebuilding.framework.api.OptAreas;
import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import com.simplebuilding.framework.api.SubMods;
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
 * Simple Opt Server: server areas of Simple Optimization (concept §3, §5): S5a dynamic view distance, S5b activation range
 * (mixin, decided by the plugin before game classes load). Areas that change gameplay (S5b, lowering the simulation
 * distance) default off.
 */
public final class ServerOpt {
    public static final String MOD_ID = "simpleoptserver", FILE = MOD_ID + ".json";
    public static final String SUPER_FILE = "simpleoptimization.json", SWITCH = "enableSimpleOptServer";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    /** Mod id from ServerCore's published 26.3 jar (fabric.mod.json, Modrinth 2026-10-18). */
    public static final OptCompat.Area DYNAMIC_DISTANCE = new OptCompat.Area("dynamic_distance", "S5a", OptCompat.Side.SERVER,
            List.of("servercore"), false);
    /** S5b: far mobs tick less often. Changes gameplay, so it starts off (concept §5, §9.4). */
    public static final OptCompat.Area ACTIVATION_RANGE = new OptCompat.Area("activation_range", "S5b", OptCompat.Side.SERVER,
            List.of("servercore"), false);
    public static final List<OptCompat.Area> AREAS = List.of(DYNAMIC_DISTANCE, ACTIVATION_RANGE);
    public static final OptAreas RUNTIME = new OptAreas(MOD_ID, AREAS);

    private static volatile DynamicDistance.Params params = DynamicDistance.Params.DEFAULTS;
    private static volatile ActivationRange.Params activation = ActivationRange.Params.DEFAULTS;
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
        var ar = ActivationRange.Params.DEFAULTS;
        Map<String, Object> act = new LinkedHashMap<>();
        act.put("mode", "off");
        act.put("range", ar.range());
        act.put("interval", ar.interval());
        d.put(ACTIVATION_RANGE.id(), act);
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
        params = new DynamicDistance.Params(s.doubleValue(a, "targetMspt", 45, 1, 1000), s.intValue(a, "minView", 6, 3, 32),
                s.intValue(a, "minSimulation", 5, 3, 32), s.intValue(a, "stepSeconds", 15, 1, 3600), s.boolValue(a, "adjustSimulation", false));
        String r = ACTIVATION_RANGE.id();
        activation = new ActivationRange.Params(s.intValue(r, "range", 32, ActivationRange.MIN_RANGE, ActivationRange.MAX_RANGE),
                s.intValue(r, "interval", 4, 2, ActivationRange.MAX_INTERVAL));
        return RUNTIME.decide(s, switchOn, isLoaded, table, LOG::info);
    }

    /** Tests only. */
    static void restore(List<OptCompat.Decision> decisions, DynamicDistance.Params p, ActivationRange.Params a) {
        RUNTIME.publish(decisions);
        params = p;
        activation = a;
    }

    public static Map<String, OptCompat.Winner> priorities() {
        try (InputStream in = ServerOpt.class.getResourceAsStream("/data/" + MOD_ID + "/priorities.json")) {
            return in == null ? Map.of() : OptSettings.priorities(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Decided at start, not switched off by the target check, and the runtime super-mod switch still on. */
    public static boolean active(OptCompat.Area area) {
        return RUNTIME.live(area) && SubMods.enabled(MOD_ID);
    }

    public static ActivationRange.Params activation() {
        return activation;
    }

    public static DynamicDistance.Params params() {
        return params;
    }
}
