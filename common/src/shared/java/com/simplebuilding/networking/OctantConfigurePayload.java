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
    /** Form- und Reihenfolge-Namen sind Enum-Namen; laengere Strings lehnt schon das Dekodieren ab. */
    public static final int MAX_NAME_LENGTH = 32;

    public static final CustomPacketPayload.Type<OctantConfigurePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "octant_configure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OctantConfigurePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), OctantConfigurePayload::pos1,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), OctantConfigurePayload::pos2,
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), OctantConfigurePayload::shapeName,
            ByteBufCodecs.BOOL, OctantConfigurePayload::locked,
            ByteBufCodecs.INT, OctantConfigurePayload::orientationOrdinal,
            ByteBufCodecs.BOOL, OctantConfigurePayload::hollow,
            ByteBufCodecs.BOOL, OctantConfigurePayload::layerMode,
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), OctantConfigurePayload::fillOrder,
            OctantConfigurePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}