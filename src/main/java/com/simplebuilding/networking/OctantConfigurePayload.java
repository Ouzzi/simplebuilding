package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public record OctantConfigurePayload(
        Optional<BlockPos> pos1,
        Optional<BlockPos> pos2,
        String shapeName,
        boolean locked,
        int orientationOrdinal, // 0=X, 1=Y, 2=Z
        boolean hollow,
        boolean layerMode,
        String fillOrder
) implements CustomPayload {
    public static final CustomPayload.Id<OctantConfigurePayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "octant_configure"));

    public static final PacketCodec<RegistryByteBuf, OctantConfigurePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.optional(BlockPos.PACKET_CODEC), OctantConfigurePayload::pos1,
            PacketCodecs.optional(BlockPos.PACKET_CODEC), OctantConfigurePayload::pos2,
            PacketCodecs.STRING, OctantConfigurePayload::shapeName,
            PacketCodecs.BOOLEAN, OctantConfigurePayload::locked,
            PacketCodecs.INTEGER, OctantConfigurePayload::orientationOrdinal,
            PacketCodecs.BOOLEAN, OctantConfigurePayload::hollow,
            PacketCodecs.BOOLEAN, OctantConfigurePayload::layerMode,
            PacketCodecs.STRING, OctantConfigurePayload::fillOrder,
            OctantConfigurePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}