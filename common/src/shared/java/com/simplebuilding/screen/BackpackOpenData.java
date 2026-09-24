package com.simplebuilding.screen;

import com.simplebuilding.items.custom.BackpackTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Was der Client zum Aufbau des Rucksack-Menues braucht: die Stufe (Slot-Anordnung), den
 * Stapelfaktor aus Tiefe Taschen (damit die Klick-Vorhersage des Clients dieselben Grenzen rechnet
 * wie der Server) und fuer einen abgestellten Rucksack dessen Position.
 *
 * <p>Der Codec ist bewusst auf {@link ByteBuf} typisiert und nicht auf
 * {@code RegistryFriendlyByteBuf}: Forges {@code openMenu} reicht einen {@code FriendlyByteBuf}.
 */
public record BackpackOpenData(boolean placed, BlockPos pos, int tierId, int stackMultiplier) {
    public static final StreamCodec<ByteBuf, BackpackOpenData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BackpackOpenData::placed,
            BlockPos.STREAM_CODEC, BackpackOpenData::pos,
            ByteBufCodecs.VAR_INT, BackpackOpenData::tierId,
            ByteBufCodecs.VAR_INT, BackpackOpenData::stackMultiplier,
            BackpackOpenData::new);

    public static BackpackOpenData worn(BackpackTier tier, int multiplier) {
        return new BackpackOpenData(false, BlockPos.ZERO, tier.ordinal(), multiplier);
    }

    public static BackpackOpenData placed(BlockPos pos, BackpackTier tier, int multiplier) {
        return new BackpackOpenData(true, pos, tier.ordinal(), multiplier);
    }

    public BackpackTier tier() {
        return BackpackTier.byId(this.tierId);
    }
}
