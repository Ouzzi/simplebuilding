package com.simplebuilding.blocks.custom;

import net.minecraft.world.level.block.state.BlockBehaviour;

/** Compile-only twin; trapped tier chests are not registered until the 26.2 port run. */
public class TieredTrappedChestBlock extends TieredChestBlock {
    public TieredTrappedChestBlock(TieredChestBlock normal, BlockBehaviour.Properties properties) {
        super(normal.tier(), normal.getOpenChestSound(), normal.getCloseChestSound(), properties);
    }
}
