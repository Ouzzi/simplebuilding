package com.simplebuilding.modules.simplelayers.backrooms.level.hotel;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import net.minecraft.world.level.block.Blocks;

/**
 * Level "hotel" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-hotel): everything in this package. Stub: a room
 * grid of Vanilla blocks in the level's colours (core §6); replace with the level concept's own generator, pieces and palette.
 */
public final class HotelBand extends StubBand {
    public HotelBand() {
        super("hotel", DoorRule.BREAK_ON_TRANSIT, 0x4A2A30, true, () -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(), () -> Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(),
                () -> Blocks.DARK_OAK_PLANKS.defaultBlockState(), () -> Blocks.OCHRE_FROGLIGHT.defaultBlockState());
    }
}
