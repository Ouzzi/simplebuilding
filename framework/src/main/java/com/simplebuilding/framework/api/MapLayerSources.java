package com.simplebuilding.framework.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extra layers on the wayfinder map (Simple Maps) that other modules contribute - pure Java, no Minecraft class.
 *
 * <p>A module registers in its mod constructor a source id (for example {@code "shops"}) with a title translation key
 * and a collector. Simple Maps asks the collector on the server, for the map's own dimension and only inside areas the
 * map has already explored, and sends the markers to the client; nobody imports the other side. The collector gets the
 * {@code ServerLevel} as {@code Object} (this API has no Minecraft dependency) and must return only what any player may
 * see on the map: no owners' secrets, no stock counts beyond what the offer shows.
 */
public final class MapLayerSources {
    /** One marker: block position, a short icon kind ({@code "shop"}), tooltip lines (plain text, at most 6 of 80 chars are kept). */
    public record Marker(int x, int z, String kind, List<String> lines) {
        public Marker {
            Objects.requireNonNull(kind, "kind");
            lines = List.copyOf(lines);
        }
    }

    /** Collects the markers inside the block rectangle {@code [minX..maxX] x [minZ..maxZ]} of {@code level} (a {@code ServerLevel}). */
    @FunctionalInterface
    public interface Collector {
        List<Marker> collect(Object level, int minX, int minZ, int maxX, int maxZ);
    }

    /**
     * Source that depends on who looks (visibility rules): {@link #collectFor} is what Simple Maps calls; the plain
     * {@link #collect} stays for sources that show the same to everybody.
     */
    public interface ViewerCollector extends Collector {
        @Override default List<Marker> collect(Object level, int minX, int minZ, int maxX, int maxZ) { return collectFor(level, null, minX, minZ, maxX, maxZ); }
        /** {@code viewer}: UUID of the player whose map is filled, never null from Simple Maps. */
        List<Marker> collectFor(Object level, java.util.UUID viewer, int minX, int minZ, int maxX, int maxZ);
    }

    /** One registered source: owner namespace, id (unique), title key (for the map settings), collector. */
    public record Source(String owner, String id, String titleKey, Collector collector) {
        public Source {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(titleKey, "titleKey");
            Objects.requireNonNull(collector, "collector");
        }
    }

    private static final Map<String, Source> SOURCES = new LinkedHashMap<>();

    private MapLayerSources() {
    }

    /** Adds (or replaces, keeping its place) the source {@code id}. */
    public static synchronized void register(String owner, String id, String titleKey, Collector collector) {
        SOURCES.put(id, new Source(owner, id, titleKey, collector));
    }

    /** Collects for one viewer: {@link ViewerCollector}s get the viewer, all others the plain call. */
    public static List<Marker> collect(Source source, Object level, java.util.UUID viewer, int minX, int minZ, int maxX, int maxZ) {
        return source.collector() instanceof ViewerCollector v ? v.collectFor(level, viewer, minX, minZ, maxX, maxZ)
                : source.collector().collect(level, minX, minZ, maxX, maxZ);
    }

    /** The source with this id, or {@code null}. */
    public static synchronized Source get(String id) {
        return SOURCES.get(id);
    }

    /** Every source in registration order. */
    public static synchronized List<Source> all() {
        return new ArrayList<>(SOURCES.values());
    }
}
