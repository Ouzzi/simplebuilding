package com.simplemaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Item components of the wayfinder maps. */
public final class MapsComponents {
    /** The map's id (data in {@code simplemaps:wayfinder_<id>}); copies share it like Vanilla map copies. */
    public record MapRef(int id) {
        public static final Codec<MapRef> CODEC = Codec.INT.xmap(MapRef::new, MapRef::id);
        public static final StreamCodec<ByteBuf, MapRef> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(MapRef::new, MapRef::id);
    }

    /** What an item frame shows (Feature 4): centre block and zoom (blocks per pixel). */
    public record View(int x, int z, int zoom) {
        public static final Codec<View> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(View::x),
                Codec.INT.fieldOf("z").forGetter(View::z),
                Codec.INT.fieldOf("zoom").forGetter(View::zoom)
        ).apply(i, View::new));
        public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, View::x, ByteBufCodecs.INT, View::z, ByteBufCodecs.VAR_INT, View::zoom, View::new);

        public View {
            zoom = WayfinderData.validZoom(zoom);
        }
    }

    /**
     * Area to take over when the cartography result is taken (like Vanilla's MAP_POST_PROCESSING): {@code kind} 0 =
     * a Vanilla filled map with that map id, 1 = another wayfinder map with that wayfinder id.
     */
    public record Pending(int kind, int source) {
        public static final int VANILLA = 0, WAYFINDER = 1;
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1).fieldOf("kind").forGetter(Pending::kind),
                Codec.INT.fieldOf("source").forGetter(Pending::source)
        ).apply(i, Pending::new));
        public static final StreamCodec<ByteBuf, Pending> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Pending::kind, ByteBufCodecs.VAR_INT, Pending::source, Pending::new);
    }

    public static final DataComponentType<MapRef> MAP_ID = DataComponentType.<MapRef>builder()
            .persistent(MapRef.CODEC).networkSynchronized(MapRef.STREAM_CODEC).build();
    public static final DataComponentType<Waypoints> WAYPOINTS = DataComponentType.<Waypoints>builder()
            .persistent(Waypoints.CODEC).networkSynchronized(Waypoints.STREAM_CODEC).cacheEncoding().build();
    public static final DataComponentType<View> VIEW = DataComponentType.<View>builder()
            .persistent(View.CODEC).networkSynchronized(View.STREAM_CODEC).build();
    public static final DataComponentType<Pending> PENDING = DataComponentType.<Pending>builder()
            .persistent(Pending.CODEC).networkSynchronized(Pending.STREAM_CODEC).build();

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMaps.id("map_id"), MAP_ID);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMaps.id("waypoints"), WAYPOINTS);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMaps.id("view"), VIEW);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMaps.id("pending"), PENDING);
    }

    private MapsComponents() {}
}
