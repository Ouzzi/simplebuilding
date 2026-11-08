package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The map part of the deed snapshot (SP22-8 §4.5, WH26-9), display only; the server re-checks every action.
 * {@code map}: the holder may see the land (owner, or any right in it, C7). {@code owns}: the holder is the owner
 * (trade, give up, claim here). {@code here}: the chunk the holder stands in ({@link #HERE_FREE} claimable now,
 * {@link #HERE_OWN} theirs, {@link #HERE_NO} not claimable). {@code buy}: {@link #BUY} the trade tab is open
 * ({@code room} = credits still allowed below the server limit, {@code offers} = the configured trades), {@link #LIMIT}
 * at the server limit, {@link #OFF} expansion off or not the owner. {@code tabs}: Overworld, Nether, End, every other
 * dimension of the server the claims allow and every dimension with land of the owner, each with the owner's chunk
 * count there. {@code result}: answer key of the last action ("").
 */
public record DeedMap(boolean map, boolean owns, int credits, int here, int buy, int room, List<Offer> offers,
                      boolean mapsPresent, List<Tab> tabs, String result) {
    public static final int HERE_FREE = 0, HERE_OWN = 1, HERE_NO = 2;
    public static final int BUY = 0, LIMIT = 2, OFF = 3;
    public static final int MAX_TABS = 32, MAX_OFFERS = 8;
    public static final DeedMap NONE = new DeedMap(false, false, 0, HERE_NO, OFF, 0, List.of(), false, List.of(), "");

    /** A trade of the buy tab: {@code count} x item {@code id} for {@code credits} chunk credits. */
    public record Offer(String id, int count, int credits) {
        static final StreamCodec<ByteBuf, Offer> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(128), Offer::id,
                ByteBufCodecs.VAR_INT, Offer::count, ByteBufCodecs.VAR_INT, Offer::credits, Offer::new);
    }
    public record Tab(String dimension, int chunks) {
        static final StreamCodec<ByteBuf, Tab> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(128), Tab::dimension, ByteBufCodecs.VAR_INT, Tab::chunks, Tab::new);
    }

    private static final StreamCodec<ByteBuf, List<Offer>> OFFERS = Offer.CODEC.apply(ByteBufCodecs.list(MAX_OFFERS));
    private static final StreamCodec<ByteBuf, List<Tab>> TABS = Tab.CODEC.apply(ByteBufCodecs.list(MAX_TABS));

    static void write(ByteBuf b, DeedMap m) {
        ByteBufCodecs.BOOL.encode(b, m.map); ByteBufCodecs.BOOL.encode(b, m.owns);
        ByteBufCodecs.VAR_INT.encode(b, m.credits); ByteBufCodecs.VAR_INT.encode(b, m.here); ByteBufCodecs.VAR_INT.encode(b, m.buy);
        ByteBufCodecs.VAR_INT.encode(b, m.room); OFFERS.encode(b, m.offers);
        ByteBufCodecs.BOOL.encode(b, m.mapsPresent); TABS.encode(b, m.tabs); ByteBufCodecs.stringUtf8(32).encode(b, m.result);
    }
    static DeedMap read(ByteBuf b) {
        return new DeedMap(ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), OFFERS.decode(b),
                ByteBufCodecs.BOOL.decode(b), TABS.decode(b), ByteBufCodecs.stringUtf8(32).decode(b));
    }

    /** The owner's chunk count in {@code dimension} (0 without a tab). */
    public int chunksIn(String dimension) {
        for (Tab t : tabs) if (t.dimension().equals(dimension)) return t.chunks();
        return 0;
    }
}
