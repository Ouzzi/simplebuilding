package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OctantConfigurePayload(
        Optional<BlockPos> pos1,
        Optional<BlockPos> pos2,
        String shapeName,
        boolean locked,
        int orientationOrdinal, // 0=X, 1=Y, 2=Z
        boolean hollow,
        boolean layerMode,
        String fillOrder
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OctantConfigurePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "octant_configure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OctantConfigurePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), OctantConfigurePayload::pos1,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), OctantConfigurePayload::pos2,
            ByteBufCodecs.STRING_UTF8, OctantConfigurePayload::shapeName,
            ByteBufCodecs.BOOL, OctantConfigurePayload::locked,
            ByteBufCodecs.INT, OctantConfigurePayload::orientationOrdinal,
            ByteBufCodecs.BOOL, OctantConfigurePayload::hollow,
            ByteBufCodecs.BOOL, OctantConfigurePayload::layerMode,
            ByteBufCodecs.STRING_UTF8, OctantConfigurePayload::fillOrder,
            OctantConfigurePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}