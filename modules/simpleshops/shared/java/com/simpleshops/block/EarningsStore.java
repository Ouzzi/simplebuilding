package com.simpleshops.block;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Something that holds the earnings of a shop: the sales block itself or a sales storage below it (27 slots each). */
public interface EarningsStore {
    int EARNINGS_SIZE = 27;

    NonNullList<ItemStack> earnings();

    @Nullable UUID owner();

    boolean isRemoved();

    BlockPos getBlockPos();

    void changed();
}
