package com.simplebuilding.framework.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Since 0.1.12. Compatibility engine of Simple Optimization (docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md §4):
 * every optimization area names the known mods that cover the same thing; a mode per area (auto|ours|theirs|off) and a
 * measured priority table decide whether our part runs. We never switch another mod off. Pure Java: the loader adapter
 * hands in {@code isLoaded} (Fabric {@code FabricLoader.isModLoaded}), so nothing here touches a foreign class.
 */
public final class OptCompat {
    public enum Side { CLIENT, SERVER, BOTH }

    public enum Mode {
        AUTO, OURS, THEIRS, OFF;

        /** Unknown or missing text means {@link #AUTO}. */
        public static Mode parse(String text) {
            if (text == null) return AUTO;
            try {
                return valueOf(text.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return AUTO;
            }
        }
    }

    /** Winner of the measured comparison for an area (priority table); the start value everywhere is THEIRS. */
    public enum Winner { OURS, THEIRS }

    public enum Reason {
        /** No known mod for this area is loaded: ours runs. */
        NO_RIVAL,
        /** mode "off" in the config. */
        CONFIG_OFF,
        /** "Enable Simple X" of the super-mod is off. */
        SWITCH_OFF,
        /** A known mod covers the area and wins (mode theirs, or auto with the table saying theirs). */
        RIVAL_WINS,
        /** mode "ours" (or auto with a measured win) and both parts may run side by side. */
        OURS_CHOSEN,
        /** mode "ours" (or a measured win) but both parts would hook the same target: falls back to theirs. */
        OURS_NOT_ALLOWED,
        /** Since 0.1.13: target check (concept §4.5) - a mod not on the area's list hooks one of its target classes. */
        CONFLICT,
        /** Since 0.1.14 (concept §4.4): one of the area's mixins did not apply at class load (target changed, clash): off, no crash. */
        MIXIN_FAILED
    }

    /**
     * One optimization area. {@code code} is the concept id (O1, S5a ...), {@code rivals} the mod ids from the published
     * jars' fabric.mod.json, {@code coexists} whether ours may run next to a rival without both hooking the same target.
     */
    public record Area(String id, String code, Side side, List<String> rivals, boolean coexists) {
        public Area {
            rivals = List.copyOf(rivals);
        }
    }

    /** {@code hint} is a one-line advice for the status (may be empty), never an action on the other mod. */
    public record Decision(Area area, Mode mode, boolean active, String rival, Reason reason, String hint) {}

    private static final Map<String, List<Decision>> PUBLISHED = new ConcurrentHashMap<>();

    private OptCompat() {}

    /** The rule of concept §4.3. {@code rival} in the result is the first loaded mod of the area's list, or "". */
    public static Decision resolve(Area area, Mode mode, boolean switchOn, Predicate<String> isLoaded, Winner measured) {
        String rival = area.rivals().stream().filter(isLoaded).findFirst().orElse("");
        if (mode == Mode.OFF) return new Decision(area, mode, false, rival, Reason.CONFIG_OFF, "");
        if (!switchOn) return new Decision(area, mode, false, rival, Reason.SWITCH_OFF, "");
        if (rival.isEmpty()) return new Decision(area, mode, true, rival, Reason.NO_RIVAL, "");
        boolean wantOurs = mode == Mode.OURS || (mode == Mode.AUTO && measured == Winner.OURS);
        if (!wantOurs) return new Decision(area, mode, false, rival, Reason.RIVAL_WINS, "");
        if (area.coexists()) return new Decision(area, mode, true, rival, Reason.OURS_CHOSEN, "");
        String hint = mode == Mode.OURS
                ? "mode ours is not allowed next to " + rival + " (same target); using theirs"
                : "measured faster than " + rival + ": switch that part of " + rival + " off to use ours";
        return new Decision(area, mode, false, rival, Reason.OURS_NOT_ALLOWED, hint);
    }

    /**
     * Since 0.1.13. Target check of concept §4.5: for every area (id → its target classes) the first other mod (sorted)
     * whose mixins target one of those classes, unless {@code ignore} accepts that mod (our family, the loader, Fabric API).
     * Class names may use '/' or '.'.
     */
    public static Map<String, String> conflicts(Map<String, Set<String>> areaTargets, Map<String, Set<String>> modTargets,
                                                Predicate<String> ignore) {
        Map<String, String> out = new TreeMap<>();
        List<String> mods = modTargets.keySet().stream().filter(m -> !ignore.test(m)).sorted().toList();
        areaTargets.forEach((area, targets) -> {
            Set<String> mine = new HashSet<>();
            for (String t : targets) mine.add(t.replace('/', '.'));
            for (String mod : mods) {
                if (modTargets.get(mod).stream().anyMatch(t -> mine.contains(t.replace('/', '.')))) {
                    out.put(area, mod);
                    break;
                }
            }
        });
        return out;
    }

    /** Since 0.1.13. The decision after the target check found {@code mod} on the same target: off, reason CONFLICT. */
    public static Decision downgrade(Decision d, String mod) {
        return new Decision(d.area(), d.mode(), false, mod, Reason.CONFLICT, "possible conflict with " + mod
                + " (same target class); set mode ours or conflictScan false to keep ours");
    }

    /** Since 0.1.14. The decision after mixin {@code mixinSimpleName} of its area did not apply: off, reason MIXIN_FAILED. */
    public static Decision mixinFailed(Decision d, String mixinSimpleName) {
        return new Decision(d.area(), d.mode(), false, d.rival(), Reason.MIXIN_FAILED, "mixin " + mixinSimpleName + " not applied");
    }

    /** The startup log block: one line per area. */
    public static String table(String owner, List<Decision> decisions) {
        StringBuilder out = new StringBuilder(owner).append(" areas:");
        if (decisions.isEmpty()) out.append(" none yet");
        for (Decision d : decisions) {
            out.append(System.lineSeparator()).append("  ")
                    .append(String.format(Locale.ROOT, "%-4s %-18s mode=%-6s -> %-3s (%s%s)%s", d.area().code(), d.area().id(),
                            d.mode().name().toLowerCase(Locale.ROOT), d.active() ? "on" : "off",
                            d.reason().name().toLowerCase(Locale.ROOT), d.rival().isEmpty() ? "" : ": " + d.rival(),
                            d.hint().isEmpty() ? "" : " - " + d.hint()));
        }
        return out.toString();
    }

    /** Keeps the decisions of {@code owner} (a sub-mod id) for status commands and tests. */
    public static void publish(String owner, List<Decision> decisions) {
        PUBLISHED.put(owner, List.copyOf(decisions));
    }

    public static List<Decision> decisions(String owner) {
        return PUBLISHED.getOrDefault(owner, List.of());
    }

    /** Whether {@code area} of {@code owner} was decided active; unknown areas are off. */
    public static boolean active(String owner, String area) {
        for (Decision d : decisions(owner)) if (d.area().id().equals(area)) return d.active();
        return false;
    }

    /** Every published decision, owner order unspecified. */
    public static List<Decision> all() {
        List<Decision> out = new ArrayList<>();
        PUBLISHED.values().forEach(out::addAll);
        return out;
    }
}
