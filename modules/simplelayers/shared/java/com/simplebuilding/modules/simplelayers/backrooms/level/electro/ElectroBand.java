package com.simplebuilding.modules.simplelayers.backrooms.level.electro;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import net.minecraft.world.level.block.Blocks;

/**
 * Level "electro" (BACK22, R9 order in Bands.ORDER). Owned by its level branch (b-back22-electro): everything in this package. Stub: a room
 * grid of Vanilla blocks in the level's colours (core §6); replace with the level concept's own generator, pieces and palette.
 */
public final class ElectroBand extends StubBand {
    public ElectroBand() {
        super("electro", DoorRule.BREAK_ON_TRANSIT, 0x3A5A8C, true, () -> Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState(), () -> Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY).defaultBlockState(),
                () -> Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState(), () -> Blocks.SEA_LANTERN.defaultBlockState());
    }
}
