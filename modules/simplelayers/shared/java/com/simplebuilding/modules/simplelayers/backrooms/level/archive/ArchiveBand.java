package com.simplebuilding.modules.simplelayers.backrooms.level.archive;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import net.minecraft.world.level.block.Blocks;

/**
 * Level "archive" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-archive): everything in this package. Stub: a room
 * grid of Vanilla blocks in the level's colours (core §6); replace with the level concept's own generator, pieces and palette.
 */
public final class ArchiveBand extends StubBand {
    public ArchiveBand() {
        super("archive", DoorRule.BREAK_ON_TRANSIT, 0x5A4630, true, () -> Blocks.SPRUCE_PLANKS.defaultBlockState(), () -> Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState(),
                () -> Blocks.SPRUCE_PLANKS.defaultBlockState(), () -> Blocks.OCHRE_FROGLIGHT.defaultBlockState());
    }
}
