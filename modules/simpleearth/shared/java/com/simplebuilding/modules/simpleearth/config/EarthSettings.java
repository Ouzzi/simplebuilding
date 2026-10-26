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
    public enum Kind { INT, DECIMAL, TOGGLE }

    public record Setting(String key, double min, double max, double def, double step, Kind kind) {
        public double clamp(double v) {
            if (!Double.isFinite(v)) return def;
            double c = Math.max(min, Math.min(max, v));
            return kind == Kind.DECIMAL ? c : Math.round(c);
        }
    }

    /** V1-V9 (24.10.) and V10-V12 (owner 25.10., WD25-17: the cube shape stronger). */
    public static final int VERSIONS = 12;
    /** Owner 24.10.: V6 is the favourite and the standard. */
    public static final int DEFAULT_VERSION = 6;

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
            i("detail", 1, 5, 3, 1),
            i("detailRange", 0, 2, 0, 1),
            i("renderDistance", 1, 2, 2, 1),
            i("paintBudget", 16, 2048, 192, 16),
            i("textureCache", 16, 256, 64, 8),
            i("tileCache", 256, 16384, 2048, 256),
            d("faceEdge", 0, 1, 0.28, 0.04),
            t("moonPhases", true),
            t("showOthers", true),
            t("realDistance", true));
    /** Heights, edge, speed, sampling - every version has its own copy (E1, E2, E11). */
    public static final List<Setting> SHARED = List.of(
            i("activationY", 512, 16384, 1024, 64),
            i("switchY", 640, 20000, 1536, 64),
            i("ceilingY", 1024, 30000, 6144, 256),
            i("fadeBand", 16, 1024, 128, 16),
            i("edge", 512, 262144, 2048, 512),
            i("faceResolution", 64, 512, 256, 64),
            d("orbitDistance", 0.5, 8, 3, 0.25),
            d("relief", 0, 4, 1, 0.25),
            d("atmosphere", 0, 1, 0.6, 0.05),
            d("moonSize", 0, 3, 1, 0.1),
            d("stars", 0, 1, 1, 0.05));

    private static final Setting ROUNDING = d("rounding", 0, 0.3, 0.07, 0.01);
    private static final Setting CORNER_TURN = d("cornerTurn", 0.05, 2, 0.35, 0.05);
    private static final Setting INVERSION = d("inversion", 0.2, 3, 0.9, 0.1);
    private static final Setting WINDOW_START = d("windowStart", 0, 0.95, 0.5, 0.05);
    private static final Setting FOLD_START = d("foldStart", 0, 0.9, 0.1, 0.05);
    private static final Setting TIP_TIME = d("tipTime", 0.1, 2, 0.6, 0.05);
    private static final Setting EDGE_LINES = d("edgeLines", 0, 1, 0.8, 0.05);

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
            default -> out.addAll(List.of(TIP_TIME, EDGE_LINES, d("cornerTilt", 0, 1, 1, 0.05), d("tiltStart", 0, 0.95, 0.35, 0.05)));
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
