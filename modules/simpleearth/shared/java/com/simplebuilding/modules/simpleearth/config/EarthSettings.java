package com.simplebuilding.modules.simpleearth.config;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings schema of the twelve earth versions: one list per version (shared values first, then the version's own), each
 * entry with range, step and default. The client config stores one value set per version, the menu shows the list of the
 * chosen version as its scroll list, the server clamps the heights/edge/speed it receives once more ({@link EarthServerConfig}).
 * Labels: {@code text.autoconfig.simpleearth.option.<key>} (+ {@code .@Tooltip}).
 */
public final class EarthSettings {
    /** LOG: a whole number whose slider runs on a logarithmic scale (edge length 64 ... 1 048 576, E29-7). */
    public enum Kind { INT, DECIMAL, TOGGLE, LOG }

    public record Setting(String key, double min, double max, double def, double step, Kind kind) {
        public double clamp(double v) {
            if (!Double.isFinite(v)) return def;
            double c = Math.max(min, Math.min(max, v));
            return kind == Kind.DECIMAL ? c : Math.round(c);
        }
    }

    /** V1-V9 (24.10.) and V10-V12 (owner 25.10., WD25-17: the cube shape stronger). */
    public static final int VERSIONS = 27;
    /** Owner 26.10. (E29-6): the versions with a direct button - his favourite V5 and the new V13-V24; V1-V12 sit in a drop-down. */
    public static final int[] FAVOURITES = {25, 26, 27};
    /** Owner 24.10.: V6 was the standard; since E30 (26.10.) the folding world V25 with the new flight phases. */
    public static final int DEFAULT_VERSION = 25;

    private static Setting i(String key, double min, double max, double def, double step) {
        return new Setting(key, min, max, def, step, Kind.INT);
    }

    private static Setting d(String key, double min, double max, double def, double step) {
        return new Setting(key, min, max, def, step, Kind.DECIMAL);
    }

    /** An on/off value (0/1), shown as on/off in the menu. */
    private static Setting t(String key, boolean def) {
        return new Setting(key, 0, 1, def ? 1 : 0, 1, Kind.TOGGLE);
    }

    /**
     * Display settings for all versions (owner 25.10., WD25-16): detail level, how far the earth is drawn, frame budget and
     * caches, E9/E10 on the client. Stored once ({@code render} in the client config), shown at the top of the menu.
     * {@code detail} 2 = the resolution before 25.10. ({@code faceResolution}), 3 (standard) twice as fine near the player.
     */
    public static final List<Setting> RENDER = List.of(
            i("detailRange", 0, 2, 0, 1),
            i("renderDistance", 1, 2, 2, 1),
            i("paintBudget", 16, 2048, 192, 16),
            i("textureCache", 16, 256, 64, 8),
            i("tileCache", 256, 16384, 2048, 256),
            d("faceEdge", 0, 1, 0.28, 0.04),
            t("moonPhases", true),
            t("showOthers", true),
            t("realDistance", true));
    /**
     * Heights, size and look - one set for all versions (E29-1). E30-2 (owner 26.10.): the important ones first - edge length,
     * render height (where the earth starts), blend-in band, detail level (n = the edge in n pixels, n x n per face); there is
     * no activation height any more (E30-1: world 0 ~ 0 is the centre of the top face).
     */
    public static final List<Setting> SHARED = List.of(
            new Setting("edge", 64, 1048576, 2048, 64, Kind.LOG),
            i("switchY", 256, 30000, 1536, 16),
            i("fadeBand", 1, 4096, 128, 1),
            new Setting("detail", 1, 1024, 256, 1, Kind.LOG),
            i("ceilingY", 512, 60000, 6144, 64),
            d("orbitDistance", 0.1, 20, 3, 0.05),
            d("relief", 0, 8, 1, 0.05),
            d("atmosphere", 0, 2, 0.6, 0.05),
            d("moonSize", 0, 10, 1, 0.05),
            d("stars", 0, 1, 1, 0.05));

    private static final Setting ROUNDING = d("rounding", 0, 0.3, 0.07, 0.01);
    private static final Setting CORNER_TURN = d("cornerTurn", 0.05, 2, 0.35, 0.05);
    private static final Setting INVERSION = d("inversion", 0.2, 3, 0.9, 0.1);
    private static final Setting WINDOW_START = d("windowStart", 0, 0.95, 0.5, 0.05);
    private static final Setting FOLD_START = d("foldStart", 0, 0.9, 0.1, 0.05);
    private static final Setting TIP_TIME = d("tipTime", 0.1, 2, 0.6, 0.05);
    private static final Setting EDGE_LINES = d("edgeLines", 0, 1, 0.8, 0.05);
    private static final Setting FOLLOW = d("follow", 1, 60, 30, 1);
    /** E30-3: phase B band and curve, a corner's own band and curve, the orbit turn, the hinge lines. */
    private static final List<Setting> FOLD_WORLD = List.of(i("foldBand", 16, 20000, 1024, 16), i("cornerFoldBand", 16, 20000, 2048, 16),
            d("easing", 1, 8, 2, 0.25), d("cornerEasing", 1, 8, 3, 0.25), d("rotation", 0, 1, 1, 0.05), d("lineAlpha", 0, 1, 0.3, 0.05));
    private static final Setting NORTH = d("northRate", 0, 10, 2.5, 0.25);
    private static final Setting CORNER_START = d("cornerStart", 0, 0.95, 0.4, 0.05);

    /** The full list of one version (1..12). */
    public static List<Setting> forVersion(int version) {
        List<Setting> out = new ArrayList<>(SHARED);
        switch (version) {
            case 1 -> out.addAll(List.of(ROUNDING, CORNER_TURN));
            case 2 -> out.addAll(List.of(ROUNDING, CORNER_TURN, d("foldRadius", 0.2, 1, 0.6, 0.05)));
            case 3 -> out.addAll(List.of(ROUNDING, INVERSION, WINDOW_START, d("windowSize", 0.1, 0.9, 0.62, 0.02)));
            case 4 -> out.addAll(List.of(ROUNDING, d("followRate", 1, 30, 6, 0.5), d("northRate", 0, 10, 2.5, 0.25)));
            case 5 -> out.addAll(List.of(d("exponent", 3, 20, 8, 0.5), d("followRate", 1, 30, 10, 0.5), d("northRate", 0, 10, 2.5, 0.25)));
            case 6 -> out.add(FOLD_START);
            case 7 -> out.addAll(List.of(FOLD_START, d("cellDepth", 0.02, 1, 0.18, 0.02), d("lineAlpha", 0, 1, 0.6, 0.05)));
            case 8 -> out.addAll(List.of(FOLD_START, INVERSION, WINDOW_START));
            case 9 -> out.addAll(List.of(FOLD_START, d("lineAlpha", 0, 1, 0.35, 0.05)));
            case 10 -> out.addAll(List.of(TIP_TIME, EDGE_LINES, d("rounding", 0, 0.3, 0.02, 0.01)));
            case 11 -> out.addAll(List.of(TIP_TIME, i("cells", 2, 8, 3, 1), d("seam", 0, 0.2, 0.05, 0.01)));
            // E29-5 a) V13-V16 like V5, orbiting: rounder (2.4, near a sphere) to cubic; V15 corner window, V16 fourth face
            case 13 -> out.addAll(List.of(d("exponent", 2, 20, 2.4, 0.1), FOLLOW, NORTH));
            case 14 -> out.addAll(List.of(d("exponent", 2, 20, 4, 0.1), FOLLOW, NORTH));
            case 15 -> out.addAll(List.of(d("exponent", 2, 20, 8, 0.1), FOLLOW, NORTH, CORNER_START));
            case 16 -> out.addAll(List.of(d("exponent", 2, 20, 6, 0.1), FOLLOW, NORTH, CORNER_START));
            // b) V17-V20 like V12, orbiting: sharp cube, V18 corner up with height, V19 window, V20 fourth face
            case 17 -> out.addAll(List.of(d("rounding", 0, 0.3, 0.03, 0.01), EDGE_LINES, FOLLOW, NORTH));
            case 18 -> out.addAll(List.of(d("rounding", 0, 0.3, 0.03, 0.01), EDGE_LINES, FOLLOW, NORTH, d("tipTime", 0.1, 4, 1.2, 0.05),
                    d("cornerTilt", 0, 1, 1, 0.05), d("tiltStart", 0, 0.95, 0.35, 0.05)));
            case 19 -> out.addAll(List.of(d("rounding", 0, 0.3, 0.08, 0.01), EDGE_LINES, FOLLOW, NORTH, CORNER_START));
            case 20 -> out.addAll(List.of(d("rounding", 0, 0.3, 0.15, 0.01), EDGE_LINES, FOLLOW, NORTH, CORNER_START));
            // c) V21-V24 like V9, rolling
            case 21 -> out.addAll(List.of(FOLD_START, d("rotation", 0, 1, 0.6, 0.05), d("lineAlpha", 0, 1, 0.2, 0.05)));
            case 22 -> out.addAll(List.of(d("foldAngle", 0, 90, 45, 1), d("rotation", 0, 1, 1, 0.05), d("lineAlpha", 0, 1, 0.2, 0.05)));
            case 23 -> out.addAll(List.of(d("foldAngle", 0, 90, 15, 1), d("rotation", 0, 1, 1, 0.05), d("lineAlpha", 0, 1, 0.2, 0.05)));
            case 24 -> out.addAll(List.of(d("rotation", 0, 1, 1, 0.05), d("lineAlpha", 0, 1, 0.5, 0.05)));
            // E30: the folding world (V23/V24 shell + V5 body), phases by height
            case 25 -> out.addAll(FOLD_WORLD);
            case 26 -> {
                out.addAll(FOLD_WORLD);
                out.addAll(List.of(d("roundness", 0, 1, 0.6, 0.05), d("exponent", 2, 20, 6, 0.1)));
            }
            case 27 -> {
                out.addAll(FOLD_WORLD);
                out.addAll(List.of(d("roundness", 0, 1, 1, 0.05), d("exponent", 2, 20, 2.4, 0.1)));
            }
            case 12 -> out.addAll(List.of(d("tipTime", 0.1, 4, 1.2, 0.05), EDGE_LINES, d("cornerTilt", 0, 1, 1, 0.05), d("tiltStart", 0, 0.95, 0.35, 0.05)));
        }
        return out;
    }

    public static Setting findRender(String key) {
        for (Setting s : RENDER) if (s.key().equals(key)) return s;
        return null;
    }

    public static Setting find(int version, String key) {
        for (Setting s : forVersion(version)) if (s.key().equals(key)) return s;
        return null;
    }

    /** Every key of any version (lang/wiki checks read this). */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (Setting s : RENDER) keys.add(s.key());
        for (int v = 1; v <= VERSIONS; v++) for (Setting s : forVersion(v)) if (!keys.contains(s.key())) keys.add(s.key());
        return keys;
    }

    private EarthSettings() {}
}
