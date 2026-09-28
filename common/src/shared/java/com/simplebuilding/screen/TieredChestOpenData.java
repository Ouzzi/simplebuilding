package com.simplebuilding.screen;

import com.simplebuilding.blocks.custom.ChestTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Was der Client zum Aufbau eines Mod-Truhen-Menues braucht: die Stufe (Plaetze, Stapelfaktor)
 * und ob es eine Doppeltruhe ist (Spalten und Reihen, siehe {@link ChestTier}).
 *
 * <p>Auf {@link ByteBuf} typisiert wie {@code BackpackOpenData}: Forges {@code openMenu} reicht
 * einen {@code FriendlyByteBuf}.
 */
public record TieredChestOpenData(int tierId, boolean isDouble) {
    public static final StreamCodec<ByteBuf, TieredChestOpenData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TieredChestOpenData::tierId,
            ByteBufCodecs.BOOL, TieredChestOpenData::isDouble,
            TieredChestOpenData::new);

    public static TieredChestOpenData of(ChestTier tier, boolean isDouble) {
        return new TieredChestOpenData(tier.ordinal(), isDouble);
    }

    public ChestTier tier() {
        return ChestTier.byId(this.tierId);
    }
}
