package com.simplebuilding.modules.simplelayers.backrooms.level.wheatfield;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import net.minecraft.world.level.block.Blocks;

/**
 * Level "wheatfield" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-wheatfield): everything in this package. Stub: a room
 * grid of Vanilla blocks in the level's colours (core §6); replace with the level concept's own generator, pieces and palette.
 */
public final class WheatfieldBand extends StubBand {
    public WheatfieldBand() {
        super("wheatfield", DoorRule.BREAK_ON_TRANSIT, 0xD98A3E, false, () -> Blocks.HAY_BLOCK.defaultBlockState(), () -> Blocks.DIRT.defaultBlockState(),
                () -> Blocks.DIRT.defaultBlockState(), () -> Blocks.SHROOMLIGHT.defaultBlockState());
    }
}
