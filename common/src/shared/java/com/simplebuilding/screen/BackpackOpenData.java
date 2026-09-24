package com.simplebuilding.screen;

import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.util.DyedStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Was der Client zum Aufbau des Rucksack-Menues braucht: die Stufe (Slot-Anordnung), den
 * Stapelfaktor aus Tiefe Taschen (damit die Klick-Vorhersage des Clients dieselben Grenzen rechnet
 * wie der Server), fuer einen abgestellten Rucksack dessen Position und die Farbe des Rucksacks
 * ({@link DyedStorage#UNDYED} ohne Farbstoff), nach der der Bildschirm die Rucksack-Reihen toent.
 * Die Farbe kommt mit, weil der Client die Komponenten eines abgestellten Rucksacks nicht kennt.
 *
 * <p>Der Codec ist bewusst auf {@link ByteBuf} typisiert und nicht auf
 * {@code RegistryFriendlyByteBuf}: Forges {@code openMenu} reicht einen {@code FriendlyByteBuf}.
 */
public record BackpackOpenData(boolean placed, BlockPos pos, int tierId, int stackMultiplier, int dyeColor) {
    public static final StreamCodec<ByteBuf, BackpackOpenData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BackpackOpenData::placed,
            BlockPos.STREAM_CODEC, BackpackOpenData::pos,
            ByteBufCodecs.VAR_INT, BackpackOpenData::tierId,
            ByteBufCodecs.VAR_INT, BackpackOpenData::stackMultiplier,
            ByteBufCodecs.INT, BackpackOpenData::dyeColor,
            BackpackOpenData::new);

    public static BackpackOpenData worn(BackpackTier tier, int multiplier) {
        return worn(tier, multiplier, DyedStorage.UNDYED);
    }

    public static BackpackOpenData worn(BackpackTier tier, int multiplier, int dyeColor) {
        return new BackpackOpenData(false, BlockPos.ZERO, tier.ordinal(), multiplier, dyeColor);
    }

    public static BackpackOpenData placed(BlockPos pos, BackpackTier tier, int multiplier) {
        return placed(pos, tier, multiplier, DyedStorage.UNDYED);
    }

    public static BackpackOpenData placed(BlockPos pos, BackpackTier tier, int multiplier, int dyeColor) {
        return new BackpackOpenData(true, pos, tier.ordinal(), multiplier, dyeColor);
    }

    public BackpackTier tier() {
        return BackpackTier.byId(this.tierId);
    }
}
