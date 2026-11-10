package com.simplebuilding.modules.simplelayers.backrooms.level.warehouse;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.LegacyBand;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;

/**
 * Level "warehouse" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-warehouse): everything in this package. Start content:
 * the BACK18/20 generator through {@link LegacyBand} (core §7); replace with the level concept's own generator, pieces and palette.
 */
public final class WarehouseBand extends LegacyBand {
    public WarehouseBand() {
        super("warehouse", BackroomsLevel.WAREHOUSE, DoorRule.BREAK_ON_TRANSIT, 0x555A62, () -> net.minecraft.world.level.block.Blocks.GRAY_CONCRETE.defaultBlockState(), true);
    }
}
