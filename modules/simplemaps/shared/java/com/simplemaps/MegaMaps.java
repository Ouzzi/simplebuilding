package com.simplemaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * What a mega wayfinder map holds (owner B23-K1/K2): one map per dimension and the dimension shown last. Each entry
 * keeps its own wayfinder id ({@code -1} = not visited yet) and, while another dimension is shown, its waypoints. The
 * shown entry lives in the stack's {@link MapsComponents#MAP_ID} and {@link MapsComponents#WAYPOINTS}, so the screen,
 * the map in hand, frames and the locator bar work unchanged. Overworld, Nether and End are always there; further
 * dimensions are unlocked at the anvil ({@link #unlockTarget}).
 */
public record MegaMaps(String selected, List<Entry> entries) {
    /** Unlocked dimensions one map may hold (the three built-in ones included). */
    public static final int MAX = 32;
    /** Config value: any block unlocks the dimension the player stands in. */
    public static final String ANY = "any";

    public record Entry(String dimension, int mapId, int kind, Identifier icon, Waypoints waypoints) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.string(1, 256).fieldOf("dimension").forGetter(Entry::dimension),
                Codec.INT.optionalFieldOf("map_id", -1).forGetter(Entry::mapId),
                Codec.intRange(0, 2).optionalFieldOf("kind", 0).forGetter(Entry::kind),
                Identifier.CODEC.fieldOf("icon").forGetter(Entry::icon),
                Waypoints.CODEC.optionalFieldOf("waypoints", Waypoints.EMPTY).forGetter(Entry::waypoints)
        ).apply(i, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(256), Entry::dimension, ByteBufCodecs.VAR_INT, e -> e.mapId() + 1,
                ByteBufCodecs.VAR_INT, Entry::kind, Identifier.STREAM_CODEC, Entry::icon, Waypoints.STREAM_CODEC, Entry::waypoints,
                (d, id, k, icon, w) -> new Entry(d, id - 1, k, icon, w));

        public boolean visited() {
            return mapId >= 0;
        }

        public WayfinderMapItem.Kind kindValue() {
            return WayfinderMapItem.Kind.values()[Math.max(0, Math.min(2, kind))];
        }

        Entry withMap(int id, int newKind) {
            return new Entry(dimension, id, newKind, icon, waypoints);
        }

        Entry withWaypoints(Waypoints w) {
            return new Entry(dimension, mapId, kind, icon, w);
        }
    }

    /** Overworld (grass block), Nether (netherrack), End (end stone): always on the map, never unlocked at the anvil. */
    public static final List<Entry> BUILT_IN = List.of(
            new Entry("minecraft:overworld", -1, 0, Identifier.withDefaultNamespace("grass_block"), Waypoints.EMPTY),
            new Entry("minecraft:the_nether", -1, 1, Identifier.withDefaultNamespace("netherrack"), Waypoints.EMPTY),
            new Entry("minecraft:the_end", -1, 2, Identifier.withDefaultNamespace("end_stone"), Waypoints.EMPTY));

    public static final MegaMaps EMPTY = new MegaMaps("", List.of());
    public static final Codec<MegaMaps> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("selected", "").forGetter(MegaMaps::selected),
            Entry.CODEC.listOf(0, MAX).optionalFieldOf("entries", List.of()).forGetter(MegaMaps::entries)
    ).apply(i, MegaMaps::new));
    public static final StreamCodec<ByteBuf, MegaMaps> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), MegaMaps::selected, Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), MegaMaps::entries,
            MegaMaps::new);

    public MegaMaps {
        selected = selected == null ? "" : selected;
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static boolean builtIn(String dimension) {
        return BUILT_IN.stream().anyMatch(e -> e.dimension().equals(dimension));
    }

    /** Every dimension of this map in button order: the three built-in ones, then the unlocked ones in unlock order. */
    public List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (Entry b : BUILT_IN) out.add(stored(b.dimension()).orElse(b));
        for (Entry e : entries) if (!builtIn(e.dimension())) out.add(e);
        return out;
    }

    private Optional<Entry> stored(String dimension) {
        return entries.stream().filter(e -> e.dimension().equals(dimension)).findFirst();
    }

    public Optional<Entry> entry(String dimension) {
        return all().stream().filter(e -> e.dimension().equals(dimension)).findFirst();
    }

    public boolean unlocked(String dimension) {
        return builtIn(dimension) || stored(dimension).isPresent();
    }

    /** This map with {@code entry} stored (replacing the one of its dimension). */
    public MegaMaps with(Entry entry) {
        // Replaced in place, so the extra dimensions keep their unlock order.
        List<Entry> out = new ArrayList<>(entries);
        int at = -1;
        for (int i = 0; i < out.size(); i++) if (out.get(i).dimension().equals(entry.dimension())) at = i;
        if (at >= 0) out.set(at, entry);
        else out.add(entry);
        return new MegaMaps(selected, out);
    }

    public MegaMaps withSelected(String dimension) {
        return new MegaMaps(dimension, entries);
    }

    /** A new unlocked dimension with this icon (null when it is built in, already there or the map is full). */
    public MegaMaps unlock(String dimension, Identifier icon) {
        if (unlocked(dimension) || all().size() >= MAX) return null;
        return with(new Entry(dimension, -1, 0, icon, Waypoints.EMPTY));
    }

    /**
     * Owner B23-K2: which dimension the item in the anvil's second slot unlocks, or null. {@code rules} maps a dimension id to
     * an item id or {@link #ANY}; dimensions not listed use {@code fallback} ({@link #ANY} or "" = not unlockable). A listed
     * item unlocks its dimension anywhere; {@link #ANY} takes any block, but only for the dimension the player stands in
     * (so only where the player has been). Built-in, already unlocked and not existing dimensions are never offered.
     */
    public String unlockTarget(Map<String, String> rules, String fallback, String here, Predicate<String> exists, String itemId, boolean block) {
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if (rule.getValue().equals(itemId) && offer(rule.getKey(), exists)) return rule.getKey();
        }
        String hereRule = rules.getOrDefault(here, fallback);
        return block && ANY.equals(hereRule) && offer(here, exists) ? here : null;
    }

    private boolean offer(String dimension, Predicate<String> exists) {
        return !unlocked(dimension) && exists.test(dimension) && all().size() < MAX;
    }

    /** Stores the shown entry's live waypoints back into it (before switching away). */
    public MegaMaps storeWaypoints(Waypoints live) {
        return entry(selected).map(e -> with(e.withWaypoints(live))).orElse(this);
    }

    public MegaMaps withMap(String dimension, int mapId, WayfinderMapItem.Kind kind) {
        Entry e = entry(dimension).orElse(null);
        return e == null ? this : with(e.withMap(mapId, kind.ordinal()));
    }
}
