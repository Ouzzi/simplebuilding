package com.simplegadgets.network;

import com.simplegadgets.SimpleGadgets;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Laser dot of a player, both ways: the client reports its dot, the server hands it on to every other player of the same world
 * (moved from SimpleBuilding's {@code simplebuilding:laser_pos}; Simple Tweaks: LaserManager.LaserPayload).
 */
public record LaserPayload(UUID player, float x, float y, float z, boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LaserPayload> ID = new CustomPacketPayload.Type<>(SimpleGadgets.id("laser_pos"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LaserPayload::player,
            ByteBufCodecs.FLOAT, LaserPayload::x,
            ByteBufCodecs.FLOAT, LaserPayload::y,
            ByteBufCodecs.FLOAT, LaserPayload::z,
            ByteBufCodecs.BOOL, LaserPayload::active,
            LaserPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
