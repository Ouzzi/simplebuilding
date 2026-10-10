package com.simplebuilding.entity.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stacks above 99 in a tiered chest cart or chest boat (Netherite x2, Enderite x4), saved the way
 * {@link TieredChestBlockEntity} saves them: vanilla's item codec sees a copy of at most 99 per slot,
 * the real count is stored besides under {@value TieredChestBlockEntity#EXTRA_COUNTS} and put back after loading.
 */
final class OversizedStacks {
    private record ExtraCount(int slot, int count) {
        static final Codec<ExtraCount> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("Slot").forGetter(ExtraCount::slot),
                Codec.INT.fieldOf("Count").forGetter(ExtraCount::count)
        ).apply(i, ExtraCount::new));
    }

    private OversizedStacks() {
    }

    /** The list with every stack above 99 as a copy of 99 (the other stacks are the same objects). */
    static NonNullList<ItemStack> codecSafeCopy(NonNullList<ItemStack> items) {
        NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            copy.set(i, stack.getCount() > TieredChestBlockEntity.CODEC_MAX_COUNT
                    ? stack.copyWithCount(TieredChestBlockEntity.CODEC_MAX_COUNT) : stack);
        }
        return copy;
    }

    static void write(ValueOutput output, NonNullList<ItemStack> real) {
        ValueOutput.TypedOutputList<ExtraCount> extras = output.list(TieredChestBlockEntity.EXTRA_COUNTS, ExtraCount.CODEC);
        for (int i = 0; i < real.size(); i++) {
            if (real.get(i).getCount() > TieredChestBlockEntity.CODEC_MAX_COUNT) {
                extras.add(new ExtraCount(i, real.get(i).getCount()));
            }
        }
        if (extras.isEmpty()) {
            output.discard(TieredChestBlockEntity.EXTRA_COUNTS);
        }
    }

    static void read(ValueInput input, NonNullList<ItemStack> items) {
        for (ExtraCount extra : input.listOrEmpty(TieredChestBlockEntity.EXTRA_COUNTS, ExtraCount.CODEC)) {
            if (extra.slot() >= 0 && extra.slot() < items.size() && !items.get(extra.slot()).isEmpty()) {
                items.get(extra.slot()).setCount(extra.count());
            }
        }
    }
}
