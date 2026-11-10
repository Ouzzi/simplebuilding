package com.simplebuilding.modules.simplelayers.backrooms.level.suburb;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import net.minecraft.world.level.block.Blocks;

/**
 * Level "suburb" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-suburb): everything in this package. Stub: a room
 * grid of Vanilla blocks in the level's colours (core §6); replace with the level concept's own generator, pieces and palette.
 */
public final class SuburbBand extends StubBand {
    public SuburbBand() {
        super("suburb", DoorRule.BREAK_ON_TRANSIT, 0x14183A, true, () -> Blocks.STONE.defaultBlockState(), () -> Blocks.BRICKS.defaultBlockState(),
                () -> Blocks.DEEPSLATE_TILES.defaultBlockState(), () -> Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
    }
}
