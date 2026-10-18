package com.simplebuilding.modules.simpleoptrender;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
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
 * Simple Opt Render: client areas of Simple Optimization (docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md §3, §5).
 * {@link #boot} runs in the mixin plugin before any game class loads: it reads {@code config/simpleoptrender.json},
 * the super-mod switch and the priority table, decides each area through framework {@link OptCompat} and logs the table.
 * Mixins live in {@code mixin.<area>} and apply only while their area is active (restart needed after a change).
 */
public final class RenderOpt {
    public static final String MOD_ID = "simpleoptrender", FILE = MOD_ID + ".json";
    public static final String SUPER_FILE = "simpleoptimization.json", SWITCH = "enableSimpleOptRender";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    /*
     * Mod ids from the fabric.mod.json of the published 26.3 jars (Modrinth, 2026-10-18). Sodium builds chunk meshes
     * with its own mesher, so our leaf hook would never run there: it counts as covering O1.
     */
    public static final OptCompat.Area LEAF_CULLING = new OptCompat.Area("leaf_culling", "O1", OptCompat.Side.CLIENT,
            List.of("cullleaves", "moreculling", "sodium"), false);
    public static final OptCompat.Area LIGHTMAP_CACHE = new OptCompat.Area("lightmap_cache", "O7", OptCompat.Side.CLIENT,
            List.of("badoptimizations"), false);
    public static final OptCompat.Area FAST_IP_PING = new OptCompat.Area("fast_ip_ping", "M1", OptCompat.Side.CLIENT,
            List.of("fastipping"), false);
    public static final OptCompat.Area NO_TELEMETRY = new OptCompat.Area("no_telemetry", "M2", OptCompat.Side.CLIENT,
            List.of("no-telemetry"), false);
    public static final List<OptCompat.Area> AREAS = List.of(LEAF_CULLING, LIGHTMAP_CACHE, FAST_IP_PING, NO_TELEMETRY);

    /** Leaf culling depth: 0 culls every leaf-to-leaf face, n keeps faces seen through the outer n leaf layers. */
    public static final int MAX_DEPTH = 4;
    private static volatile int leafDepth = 0;
    private static volatile boolean booted;

    private RenderOpt() {}

    public static Map<String, Map<String, Object>> defaults() {
        Map<String, Map<String, Object>> d = new LinkedHashMap<>();
        d.put(LEAF_CULLING.id(), Map.of("depth", 0));
        d.put(LIGHTMAP_CACHE.id(), Map.of());
        d.put(FAST_IP_PING.id(), Map.of());
        d.put(NO_TELEMETRY.id(), Map.of());
        return d;
    }

    /** Once per game start (mixin plugin; the mod initializer calls it too in case no mixin config loaded). */
    public static synchronized void boot(Path configDir, Predicate<String> isLoaded) {
        if (booted) return;
        booted = true;
        apply(OptSettings.load(configDir, FILE, defaults(), LOG::warn),
                OptSettings.switchOn(configDir, SUPER_FILE, SWITCH, LOG::warn), isLoaded, priorities());
    }

    /** Decides and publishes every area (also used by the tests with fake loaded mods). */
    public static List<OptCompat.Decision> apply(OptSettings settings, boolean switchOn, Predicate<String> isLoaded,
                                                 Map<String, OptCompat.Winner> table) {
        leafDepth = settings.intValue(LEAF_CULLING.id(), "depth", 0, 0, MAX_DEPTH);
        List<OptCompat.Decision> out = new ArrayList<>();
        for (OptCompat.Area a : AREAS)
            out.add(OptCompat.resolve(a, settings.mode(a.id()), switchOn, isLoaded, table.getOrDefault(a.id(), OptCompat.Winner.THEIRS)));
        OptCompat.publish(MOD_ID, out);
        LOG.info(OptCompat.table(MOD_ID, out));
        return out;
    }

    /** {@code data/simpleoptrender/priorities.json} in the jar (only {@code tools/bench/run_bench.py --apply-priorities} writes it). */
    public static Map<String, OptCompat.Winner> priorities() {
        try (InputStream in = RenderOpt.class.getResourceAsStream("/data/" + MOD_ID + "/priorities.json")) {
            return in == null ? Map.of() : OptSettings.priorities(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Tests only: puts earlier decisions (and depth) back live. */
    static void restore(List<OptCompat.Decision> decisions, int depth) {
        OptCompat.publish(MOD_ID, decisions);
        leafDepth = depth;
    }

    public static boolean active(OptCompat.Area area) {
        return OptCompat.active(MOD_ID, area.id());
    }

    /** Number of areas decided at startup (client smoke test). */
    public static int decidedAreas() {
        return OptCompat.decisions(MOD_ID).size();
    }

    public static int leafDepth() {
        return leafDepth;
    }

    /** Mixin class {@code ...mixin.<area>.Name} → whether that area is on. Mixins outside an area package always apply. */
    public static boolean mixinActive(String mixinClassName) {
        int at = mixinClassName.indexOf(".mixin.");
        if (at < 0) return true;
        String rest = mixinClassName.substring(at + 7);
        int dot = rest.indexOf('.');
        return dot < 0 || OptCompat.active(MOD_ID, rest.substring(0, dot));
    }
}
