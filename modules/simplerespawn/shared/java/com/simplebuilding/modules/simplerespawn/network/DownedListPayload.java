package com.simplebuilding.modules.simplerespawn.network;

import com.simplebuilding.modules.simplerespawn.SimpleRespawn;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to every client: all players that are downed right now (pose and revive prediction) and, in the same order, the
 * bleed-out ticks each has left (red name tag with the remaining time). Never registered C2S.
 */
public record DownedListPayload(List<UUID> players, List<Integer> ticksLeft) implements CustomPacketPayload {
    public static final Type<DownedListPayload> TYPE = new Type<>(SimpleRespawn.id("downed_list"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, DownedListPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(256)), DownedListPayload::players,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), DownedListPayload::ticksLeft, DownedListPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
