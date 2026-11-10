package com.simplebuilding.modules.simplelayers.backrooms.level.pipes;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.LegacyBand;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;

/**
 * Level "pipes" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-pipes): everything in this package. Start content:
 * the BACK18/20 generator through {@link LegacyBand} (core §7); replace with the level concept's own generator, pieces and palette.
 */
public final class PipesBand extends LegacyBand {
    public PipesBand() {
        super("pipes", BackroomsLevel.PIPES, DoorRule.BREAK_ON_TRANSIT, 0x7A4A32, () -> net.minecraft.world.level.block.Blocks.TUFF_BRICKS.defaultBlockState(), true);
    }
}
