package com.simplebuilding.screen;

import com.simplebuilding.blocks.custom.ChestTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Was der Client zum Aufbau eines Mod-Truhen-Menues braucht: die Stufe (Plaetze, Stapelfaktor)
 * und ob es eine Doppeltruhe ist (Spalten und Reihen, siehe {@link ChestTier}) - oder eine gestufte
 * Shulkerkiste (immer einzeln, ihre Plaetze nehmen keine Shulkerkisten).
 *
 * <p>Auf {@link ByteBuf} typisiert wie {@code BackpackOpenData}: Forges {@code openMenu} reicht
 * einen {@code FriendlyByteBuf}.
 */
public record TieredChestOpenData(int tierId, boolean isDouble, boolean isShulkerBox) {
    public static final StreamCodec<ByteBuf, TieredChestOpenData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TieredChestOpenData::tierId,
            ByteBufCodecs.BOOL, TieredChestOpenData::isDouble,
            ByteBufCodecs.BOOL, TieredChestOpenData::isShulkerBox,
            TieredChestOpenData::new);

    public static TieredChestOpenData of(ChestTier tier, boolean isDouble) {
        return new TieredChestOpenData(tier.ordinal(), isDouble, false);
    }

    /** A tier shulker box: always single, and its slots refuse what vanilla's shulker slots refuse. */
    public static TieredChestOpenData shulker(ChestTier tier) {
        return new TieredChestOpenData(tier.ordinal(), false, true);
    }

    public ChestTier tier() {
        return ChestTier.byId(this.tierId);
    }
}
