package com.simplebuilding.framework.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Since 0.1.13. Runtime of the areas of one Simple Optimization sub-mod (concept §4): decides them through
 * {@link OptCompat}, answers {@link #live} fast for hot mixin paths, records which target classes each area's mixins hook
 * ({@link #shouldApply}) and runs the target check of §4.5 ({@link #scan}). The target check comes after the mixins were
 * chosen (Mixin calls {@code acceptTargets} after every config is prepared), so a hit switches the area off at runtime:
 * every mixin of an area asks {@link #live} first. Pure Java; the loader adapter hands in the mod lists.
 */
public final class OptAreas {
    private final String owner;
    private final List<OptCompat.Area> areas;
    private final boolean[] live;
    private final Map<String, Set<String>> targets = new ConcurrentHashMap<>();
    private volatile boolean conflictScan = true;

    public OptAreas(String owner, List<OptCompat.Area> areas) {
        this.owner = owner;
        this.areas = List.copyOf(areas);
        this.live = new boolean[areas.size()];
    }

    /** Resolves every area, publishes the decisions and logs the table. */
    public List<OptCompat.Decision> decide(OptSettings settings, boolean switchOn, Predicate<String> isLoaded,
                                           Map<String, OptCompat.Winner> table, Consumer<String> log) {
        conflictScan = settings.conflictScan();
        List<OptCompat.Decision> out = new ArrayList<>();
        for (OptCompat.Area a : areas)
            out.add(OptCompat.resolve(a, settings.mode(a.id()), switchOn, isLoaded, table.getOrDefault(a.id(), OptCompat.Winner.THEIRS)));
        publish(out);
        log.accept(OptCompat.table(owner, out));
        return out;
    }

    /** Makes {@code decisions} the live state (also used by tests to restore). */
    public void publish(List<OptCompat.Decision> decisions) {
        OptCompat.publish(owner, decisions);
        for (int i = 0; i < areas.size(); i++) live[i] = OptCompat.active(owner, areas.get(i).id());
    }

    /** Whether {@code area} runs now: decided active and not switched off by the target check. Identity lookup, no allocation. */
    public boolean live(OptCompat.Area area) {
        for (int i = 0; i < live.length; i++) if (areas.get(i) == area) return live[i];
        return false;
    }

    /** The area id of a mixin class {@code ...mixin.<area>.Name}; null when it lies outside an area package. */
    public static String areaOf(String mixinClassName) {
        int at = mixinClassName.indexOf(".mixin.");
        if (at < 0) return null;
        String rest = mixinClassName.substring(at + 7);
        int dot = rest.indexOf('.');
        return dot < 0 ? null : rest.substring(0, dot);
    }

    /** Mixin plugin answer: mixins outside an area always apply, area mixins only while the area is active. Records the target. */
    public boolean shouldApply(String targetClassName, String mixinClassName) {
        String area = areaOf(mixinClassName);
        if (area == null) return true;
        if (targetClassName != null) targets.computeIfAbsent(area, k -> ConcurrentHashMap.newKeySet()).add(targetClassName.replace('/', '.'));
        return OptCompat.active(owner, area);
    }

    /** Target classes recorded per area (tests, status). */
    public Map<String, Set<String>> targets() {
        return Map.copyOf(targets);
    }

    /**
     * Target check: every active area whose target classes another mod (not ignored) also hooks goes off with reason
     * CONFLICT. Areas forced with mode "ours" stay on with a warning; {@code "conflictScan": false} skips the check.
     */
    public List<OptCompat.Decision> scan(Map<String, Set<String>> modTargets, Predicate<String> ignore, Consumer<String> warn) {
        List<OptCompat.Decision> now = OptCompat.decisions(owner);
        if (!conflictScan) return now;
        Map<String, String> hits = OptCompat.conflicts(targets, modTargets, ignore);
        if (hits.isEmpty()) return now;
        List<OptCompat.Decision> out = new ArrayList<>();
        for (OptCompat.Decision d : now) {
            String mod = d.active() ? hits.get(d.area().id()) : null;
            if (mod == null) {
                out.add(d);
            } else if (d.mode() == OptCompat.Mode.OURS) {
                warn.accept(owner + " " + d.area().code() + " " + d.area().id() + ": " + mod + " hooks the same class; kept on (mode ours)");
                out.add(d);
            } else {
                OptCompat.Decision off = OptCompat.downgrade(d, mod);
                warn.accept(owner + " " + d.area().code() + " " + d.area().id() + " off: " + off.hint());
                out.add(off);
            }
        }
        publish(out);
        return out;
    }

    public void conflictScan(boolean on) {
        conflictScan = on;
    }
}
