package com.simplebuilding.modules.simplelib.forge;

import com.simplelib.warped.WarpedWartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.PlantType;

/**
 * Forge decides what a plant may stand on by its plant type, and IPlantable only gives the nether type to the Vanilla nether
 * wart itself (identity check). Without this the warped wart would count as a plains plant and grow on dirt.
 */
public final class WarpedWartBlockForge extends WarpedWartBlock {
    public WarpedWartBlockForge(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public PlantType getPlantType(BlockGetter level, BlockPos pos) {
        return PlantType.NETHER;
    }
}
