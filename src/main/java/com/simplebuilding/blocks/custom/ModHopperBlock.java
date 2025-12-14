// ModHopperBlock.java
package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ModHopperBlock extends HopperBlock {
    private final int speedMultiplier; // 1 = Double Speed (Cooldown 4), 2 = Quadruple (Cooldown 2)

    public ModHopperBlock(Settings settings, int speedMultiplier) {
        super(settings);
        this.speedMultiplier = speedMultiplier;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ModHopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.MOD_HOPPER_BE, ModHopperBlockEntity::serverTick);
    }
}