package com.simplebuilding.framework.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Test centre stations a module contributes to a super mod's test centre (plan PLAN-MODULAUFTEILUNG §1.4) - pure
 * Java, no Minecraft class. Since 0.1.10.
 *
 * <p>A module registers in its mod constructor what its station shows: item ids ({@code "ns:path"}, missing ids are
 * skipped by the test centre) under a title translation key. The test centre (SimpleBuilding:
 * {@code TestCentreLayout}) lays out one station per entry and counts the shown items as covered. Nobody imports the
 * other side: the module names ids, the test centre reads this list.
 */
public final class TestCentreStations {
    /** One station: owner namespace, station id (unique per owner), title key, items in shown order. */
    public record Station(String owner, String id, String titleKey, List<String> items) {
        public Station {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(titleKey, "titleKey");
            items = List.copyOf(items);
        }

        /** {@code owner:id}, the key a test centre may use for the section. */
        public String key() {
            return owner + ":" + id;
        }
    }

    private static final Map<String, Station> STATIONS = new LinkedHashMap<>();

    private TestCentreStations() {
    }

    /** Adds (or replaces, keeping its place) the station {@code owner:id}. */
    public static synchronized void register(String owner, String id, String titleKey, List<String> items) {
        Station station = new Station(owner, id, titleKey, items);
        STATIONS.put(station.key(), station);
    }

    /** Takes {@code owner:id} out again; true when it was there. */
    public static synchronized boolean unregister(String owner, String id) {
        return STATIONS.remove(owner + ":" + id) != null;
    }

    /** Every station in registration order. */
    public static synchronized List<Station> all() {
        return List.copyOf(STATIONS.values());
    }

    /** The stations of one owner in registration order. */
    public static synchronized List<Station> of(String owner) {
        List<Station> out = new ArrayList<>();
        for (Station station : STATIONS.values()) {
            if (station.owner().equals(owner)) {
                out.add(station);
            }
        }
        return List.copyOf(out);
    }
}
