package com.simplelib.warped;

import com.simplelib.registry.LibItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Warped wart: Vanilla's nether wart in turquoise (owner N71). Planting on soul sand, the four growth stages, the random-tick
 * growth and the missing bone meal use are all inherited from {@link NetherWartBlock}; only the pick-block item differs.
 */
public class WarpedWartBlock extends NetherWartBlock {
    public WarpedWartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(LibItems.WARPED_WART);
    }
}
