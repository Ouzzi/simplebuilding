package com.simplebuilding.modules.simplelayers.backrooms.level.pools;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.LegacyBand;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;

/**
 * Level "pools" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-pools): everything in this package. Start content:
 * the BACK18/20 generator through {@link LegacyBand} (core §7); replace with the level concept's own generator, pieces and palette.
 */
public final class PoolsBand extends LegacyBand {
    public PoolsBand() {
        super("pools", BackroomsLevel.POOLS, DoorRule.ACTIVATE_ONCE, 0xBFE4E8, () -> net.minecraft.world.level.block.Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), true);
    }
}
