package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The map part of the deed snapshot (SP22-8 §4.5), display only; the server re-checks every action.
 * {@code map}: the holder may see the land (owner, or any right in it, C7). {@code owns}: the holder is the owner
 * (buy, give up, claim here). {@code here}: the chunk the holder stands in ({@link #HERE_FREE} claimable now,
 * {@link #HERE_OWN} theirs, {@link #HERE_NO} not claimable). {@code buy}: {@link #BUY} the next step is offered
 * ({@code nextChunks} = land after it, {@code nextLevels}, {@code nextCosts}), {@link #MAX} all bought, {@link #LIMIT}
 * above the server limit, {@link #OFF} expansion off. {@code tabs}: Overworld, Nether, End and every other dimension
 * with land of the owner, each with the owner's chunk count there. {@code result}: answer key of the last action ("").
 */
public record DeedMap(boolean map, boolean owns, int credits, int here, int buy, int nextChunks, int nextLevels,
                      List<Cost> nextCosts, boolean mapsPresent, List<Tab> tabs, String result) {
    public static final int HERE_FREE = 0, HERE_OWN = 1, HERE_NO = 2;
    public static final int BUY = 0, MAX = 1, LIMIT = 2, OFF = 3;
    public static final int MAX_TABS = 32, MAX_COSTS = 8;
    public static final DeedMap NONE = new DeedMap(false, false, 0, HERE_NO, OFF, 0, 0, List.of(), false, List.of(), "");

    public record Cost(String id, int count) {
        static final StreamCodec<ByteBuf, Cost> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(128), Cost::id, ByteBufCodecs.VAR_INT, Cost::count, Cost::new);
    }
    public record Tab(String dimension, int chunks) {
        static final StreamCodec<ByteBuf, Tab> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(128), Tab::dimension, ByteBufCodecs.VAR_INT, Tab::chunks, Tab::new);
    }

    private static final StreamCodec<ByteBuf, List<Cost>> COSTS = Cost.CODEC.apply(ByteBufCodecs.list(MAX_COSTS));
    private static final StreamCodec<ByteBuf, List<Tab>> TABS = Tab.CODEC.apply(ByteBufCodecs.list(MAX_TABS));

    static void write(ByteBuf b, DeedMap m) {
        ByteBufCodecs.BOOL.encode(b, m.map); ByteBufCodecs.BOOL.encode(b, m.owns);
        ByteBufCodecs.VAR_INT.encode(b, m.credits); ByteBufCodecs.VAR_INT.encode(b, m.here); ByteBufCodecs.VAR_INT.encode(b, m.buy);
        ByteBufCodecs.VAR_INT.encode(b, m.nextChunks); ByteBufCodecs.VAR_INT.encode(b, m.nextLevels); COSTS.encode(b, m.nextCosts);
        ByteBufCodecs.BOOL.encode(b, m.mapsPresent); TABS.encode(b, m.tabs); ByteBufCodecs.stringUtf8(32).encode(b, m.result);
    }
    static DeedMap read(ByteBuf b) {
        return new DeedMap(ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), COSTS.decode(b),
                ByteBufCodecs.BOOL.decode(b), TABS.decode(b), ByteBufCodecs.stringUtf8(32).decode(b));
    }

    /** The owner's chunk count in {@code dimension} (0 without a tab). */
    public int chunksIn(String dimension) {
        for (Tab t : tabs) if (t.dimension().equals(dimension)) return t.chunks();
        return 0;
    }
}
