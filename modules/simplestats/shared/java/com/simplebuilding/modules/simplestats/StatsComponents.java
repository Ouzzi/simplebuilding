package com.simplebuilding.modules.simplestats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simplestats.data.StatData;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** The {@code simplestats:item_stats} item component (saved and synchronized; carries the version field from day one). */
public final class StatsComponents {
    /** Hard decode limits, above the largest configurable budget: a value written under a bigger server budget still loads. */
    private static final StatData.Budget HARD = new StatData.Budget(262144, 65536);
    private static final int MAX_LIST_NAME = 64;

    private static final Codec<Map<String, Long>> LONGS = Codec.unboundedMap(Codec.STRING, Codec.LONG);
    private static final Codec<StatData.Detail> DETAIL = RecordCodecBuilder.create(i -> i.group(
            LONGS.optionalFieldOf("e", Map.of()).forGetter(StatData.Detail::entries),
            Codec.LONG.optionalFieldOf("o", 0L).forGetter(StatData.Detail::other)).apply(i, StatData.Detail::new));
    public static final Codec<StatData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("v", StatData.VERSION).forGetter(StatData::version),
            Codec.LONG.optionalFieldOf("id", 0L).forGetter(StatData::uid),
            Codec.LONG.optionalFieldOf("since", 0L).forGetter(StatData::since),
            LONGS.optionalFieldOf("totals", Map.of()).forGetter(StatData::totals),
            Codec.unboundedMap(Codec.STRING, DETAIL).optionalFieldOf("detail", Map.of()).forGetter(StatData::details),
            LONGS.optionalFieldOf("records", Map.of()).forGetter(StatData::records))
            .apply(i, (v, id, since, totals, detail, records) -> StatData.sanitize(v, id, since, totals, detail, records, HARD)));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatData> STREAM_CODEC = StreamCodec.of(StatsComponents::write, StatsComponents::read);

    /** Registered by the loader entry inside the data component registration window. */
    public static DataComponentType<StatData> ITEM_STATS;

    private StatsComponents() {}

    public static void register() {
        if (ITEM_STATS != null) return;
        // ignoreSwapAnimation: a changing counter must not replay the hand animation.
        ITEM_STATS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleStats.id("item_stats"),
                DataComponentType.<StatData>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).ignoreSwapAnimation().build());
    }

    /** The statistics of a stack, never null (empty when the item has none). */
    public static StatData of(ItemStack stack) {
        if (ITEM_STATS == null || stack.isEmpty()) return StatData.EMPTY;
        StatData data = stack.get(ITEM_STATS);
        return data == null ? StatData.EMPTY : data;
    }

    private static void writeMap(FriendlyByteBuf buf, Map<String, Long> map) {
        buf.writeVarInt(map.size());
        for (var e : map.entrySet()) {
            buf.writeUtf(e.getKey(), StatData.MAX_KEY * 4);
            buf.writeVarLong(e.getValue());
        }
    }

    private static Map<String, Long> readMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > HARD.maxEntriesPerList()) throw new io.netty.handler.codec.DecoderException("Too many statistic entries: " + size);
        Map<String, Long> map = new LinkedHashMap<>();
        for (int n = 0; n < size; n++) map.put(buf.readUtf(StatData.MAX_KEY * 4), buf.readVarLong());
        return map;
    }

    private static void write(RegistryFriendlyByteBuf buf, StatData data) {
        buf.writeVarInt(data.version());
        buf.writeLong(data.uid());
        buf.writeVarLong(data.since());
        writeMap(buf, data.totals());
        writeMap(buf, data.records());
        buf.writeVarInt(data.details().size());
        for (var list : data.details().entrySet()) {
            buf.writeUtf(list.getKey(), MAX_LIST_NAME * 4);
            writeMap(buf, list.getValue().entries());
            buf.writeVarLong(list.getValue().other());
        }
    }

    private static StatData read(RegistryFriendlyByteBuf buf) {
        int version = buf.readVarInt();
        long uid = buf.readLong(), since = buf.readVarLong();
        Map<String, Long> totals = readMap(buf), records = readMap(buf);
        int lists = buf.readVarInt();
        if (lists < 0 || lists > 256) throw new io.netty.handler.codec.DecoderException("Too many statistic lists: " + lists);
        Map<String, StatData.Detail> details = new LinkedHashMap<>();
        for (int n = 0; n < lists; n++) {
            String name = buf.readUtf(MAX_LIST_NAME * 4);
            Map<String, Long> entries = readMap(buf);
            details.put(name, new StatData.Detail(entries, buf.readVarLong()));
        }
        return StatData.sanitize(version, uid, since, totals, details, records, HARD);
    }
}
