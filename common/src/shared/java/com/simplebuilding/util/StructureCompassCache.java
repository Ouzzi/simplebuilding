package com.simplebuilding.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Entlastet die Strukturssuche des Amboss-Kompasses (Review F-01): Die Suche (Radius 300 Chunks)
 * lief bei jedem {@code createResult}, also bei jedem Tippen im Namensfeld. Jetzt gilt je Spieler,
 * Dimension, Strukturtag und 512-Block-Zelle ein Ergebnis (auch "nicht gefunden") mehrere Minuten,
 * und je Spieler laeuft hoechstens eine neue Suche pro {@link #COOLDOWN_TICKS}.
 */
public final class StructureCompassCache {
    public static final long TTL_TICKS = 6000L;
    public static final long COOLDOWN_TICKS = 100L;
    private static final int MAX_ENTRIES = 512;

    /** Gefundene Position oder null ("nicht gefunden"), samt Suchzeit. */
    private record Cached(@Nullable Long packedPos, long time) {}

    private static final Map<String, Cached> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private static final Map<UUID, Long> LAST_SEARCH = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private StructureCompassCache() {}

    public enum State { HIT, MISS_NOT_FOUND, SEARCH_ALLOWED, COOLDOWN }

    public record Lookup(State state, long packedPos) {}

    private static String key(UUID player, String dimension, String tag, int x, int z) {
        return player + "|" + dimension + "|" + tag + "|" + (x >> 9) + "|" + (z >> 9);
    }

    /** Was ist fuer diese Anfrage zu tun? Bucht bei {@code SEARCH_ALLOWED} den Cooldown sofort. */
    public static synchronized Lookup lookup(UUID player, String dimension, String tag, int x, int z, long now) {
        Cached e = CACHE.get(key(player, dimension, tag, x, z));
        if (e != null && now - e.time() >= 0 && now - e.time() < TTL_TICKS) {
            return e.packedPos() == null ? new Lookup(State.MISS_NOT_FOUND, 0) : new Lookup(State.HIT, e.packedPos());
        }
        Long last = LAST_SEARCH.get(player);
        if (last != null && now - last >= 0 && now - last < COOLDOWN_TICKS) {
            return new Lookup(State.COOLDOWN, 0);
        }
        LAST_SEARCH.put(player, now);
        return new Lookup(State.SEARCH_ALLOWED, 0);
    }

    public static synchronized void store(UUID player, String dimension, String tag, int x, int z, long now, @Nullable Long packedPos) {
        CACHE.put(key(player, dimension, tag, x, z), new Cached(packedPos, now));
    }

    public static synchronized void clear() {
        CACHE.clear();
        LAST_SEARCH.clear();
    }
}
