package com.simplebuilding.modules.simplelayers.backrooms.level.lightless;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import net.minecraft.world.level.block.Blocks;

/**
 * Level "lightless" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-lightless): everything in this package. Stub: a room
 * grid of Vanilla blocks in the level's colours (core §6); replace with the level concept's own generator, pieces and palette.
 */
public final class LightlessBand extends StubBand {
    public LightlessBand() {
        super("lightless", DoorRule.BREAK_ON_TRANSIT, 0x050507, true, () -> Blocks.BLACK_CONCRETE.defaultBlockState(), () -> Blocks.BLACKSTONE.defaultBlockState(),
                () -> Blocks.BLACK_CONCRETE.defaultBlockState(), () -> Blocks.BLACK_CONCRETE.defaultBlockState());
    }
}
