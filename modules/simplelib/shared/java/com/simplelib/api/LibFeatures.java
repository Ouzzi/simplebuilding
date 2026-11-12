package com.simplelib.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SimpleLib features a mod has to ask for (simplelib-3, PLAN40, owner PLAN40=ok 12.11.2026): SimpleLib comes with every Simple mod,
 * but a player who installs only, say, Simple Ores should get neither a camera mode on left Alt nor a choir instead of Vanilla's
 * challenge jingle (principle A1/A12). A mod asks in its {@code fabric.mod.json}:
 * <pre>{@code "custom": {"simplelib:features": ["free_look", "legendary_moments"]}}</pre>
 * <ul>
 *   <li>{@link #FREE_LOOK}: without a request the two Free Look keys come without a default key (bindable in Controls).</li>
 *   <li>{@link #LEGENDARY_MOMENTS}: without a request no choir and no particle show, and Vanilla's challenge toast keeps its sound;
 *   the switches in {@code simplelib-server.json} stay and only count with a request.</li>
 * </ul>
 * The Fabric entry decides once, first thing in its main initializer (Fabric runs every main entrypoint before any client
 * entrypoint, so the key mapping sees the decision whatever the mod order). NeoForge and Forge never decide: there everything stays
 * on, as before. GameTests switch a feature on for their run with {@link #forTest}.
 */
public final class LibFeatures {
    /** The custom key in fabric.mod.json. */
    public static final String KEY = "simplelib:features";
    public static final String FREE_LOOK = "free_look";
    public static final String LEGENDARY_MOMENTS = "legendary_moments";
    public static final List<String> ALL = List.of(FREE_LOOK, LEGENDARY_MOMENTS);

    /** Feature -> the mods that asked for it; null until a loader decided (then every feature counts as requested). */
    private static volatile Map<String, List<String>> requesters;
    private static final Map<String, AtomicInteger> TEST_ON = new ConcurrentHashMap<>();

    private LibFeatures() {}

    /** The rule, pure: mod id -> its declared list, to feature -> requesting mods (sorted); unknown names are ignored. */
    public static Map<String, List<String>> byFeature(Map<String, ? extends Collection<String>> declared) {
        Map<String, List<String>> out = new TreeMap<>();
        for (String feature : ALL) out.put(feature, new ArrayList<>());
        new TreeMap<>(declared).forEach((mod, features) -> {
            for (String f : features) {
                List<String> mods = out.get(f);
                if (mods != null && !mods.contains(mod)) mods.add(mod);
            }
        });
        out.replaceAll((f, mods) -> List.copyOf(mods));
        return Map.copyOf(out);
    }

    /** The loader's decision (Fabric, once, before anything reads a feature). */
    public static void decide(Map<String, ? extends Collection<String>> declared) {
        requesters = byFeature(declared);
    }

    /** Whether a loader decided (Fabric). */
    public static boolean decided() {
        return requesters != null;
    }

    /** Whether {@code feature} is on: asked for by a loaded mod, switched on by a running test, or no loader decided. */
    public static boolean requested(String feature) {
        AtomicInteger test = TEST_ON.get(feature);
        if (test != null && test.get() > 0) return true;
        Map<String, List<String>> r = requesters;
        return r == null || !r.getOrDefault(feature, List.of()).isEmpty();
    }

    /** The mods that asked for {@code feature} (empty before the decision). */
    public static List<String> requesters(String feature) {
        Map<String, List<String>> r = requesters;
        return r == null ? List.of() : r.getOrDefault(feature, List.of());
    }

    /** A switch for a test run; closing it needs no exception handling. */
    public interface TestSwitch extends AutoCloseable {
        @Override
        void close();
    }

    /** GameTests: {@code feature} is on until the returned switch is closed (counted, so parallel tests do not switch it off early). */
    public static TestSwitch forTest(String feature) {
        AtomicInteger count = TEST_ON.computeIfAbsent(feature, f -> new AtomicInteger());
        count.incrementAndGet();
        return count::decrementAndGet;
    }
}
