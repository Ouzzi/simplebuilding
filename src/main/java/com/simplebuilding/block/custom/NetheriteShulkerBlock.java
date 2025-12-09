package com.simplebuilding.block.custom;

import com.simplebuilding.block.entity.ModBlockEntities;
import com.simplebuilding.block.entity.custom.NetheriteShulkerBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NetheriteShulkerBlock extends ShulkerBoxBlock {

    public NetheriteShulkerBlock(AbstractBlock.Settings settings) {
        // null als Farbe, da wir keine Standard-Wollfarbe nutzen
        super(DyeColor.MAGENTA, settings);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Wir delegieren an den standardmäßigen ShulkerBoxBlockEntity Ticker,
        // da unser BlockEntity davon erbt.
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.NETHERITE_SHULKER_BE, ShulkerBoxBlockEntity::tick);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NetheriteShulkerBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

}