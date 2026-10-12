package com.simplebuilding.modules.simplerespawn.network;

import com.simplebuilding.modules.simplerespawn.SimpleRespawn;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to one client: its own role. {@link #NONE} ends every screen and bar, {@link #DOWNED} carries the bleed-out
 * ticks and the revive progress (0..1) with the helper's name, {@link #REVIVING} the progress and the downed player's
 * name for the helper. Never registered C2S.
 */
public record RespawnStatePayload(int role, int ticksLeft, int maxTicks, float progress, String other) implements CustomPacketPayload {
    public static final int NONE = 0;
    public static final int DOWNED = 1;
    public static final int REVIVING = 2;
    public static final Type<RespawnStatePayload> TYPE = new Type<>(SimpleRespawn.id("state"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, RespawnStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RespawnStatePayload::role, ByteBufCodecs.VAR_INT, RespawnStatePayload::ticksLeft,
            ByteBufCodecs.VAR_INT, RespawnStatePayload::maxTicks, ByteBufCodecs.FLOAT, RespawnStatePayload::progress,
            ByteBufCodecs.stringUtf8(64), RespawnStatePayload::other, RespawnStatePayload::new);

    public static RespawnStatePayload none() {
        return new RespawnStatePayload(NONE, 0, 0, 0f, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
