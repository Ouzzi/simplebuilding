package com.simplebuilding.woodwork;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * Carved wood (docs/ai/PLAN-HOLZWERK-2026-10-09.md): a standing stripped log with the motif of a pottery sherd on
 * one side ({@link #FACING}), carved with a chisel while the sherd is in the off hand (PotteryCarving). Drops itself
 * with its motif (loot {@code copy_state}); placed again, the motif faces the player.
 */
public class CarvedLogBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CarvedLogBlock> CODEC = BlockCodecs.simple(CarvedLogBlock::new);
    public static final EnumProperty<SherdMotif> MOTIF = EnumProperty.create("motif", SherdMotif.class);

    private final WoodKind wood;

    public CarvedLogBlock(Properties properties) {
        this(WoodKind.OAK, properties);
    }

    public CarvedLogBlock(WoodKind wood, Properties properties) {
        super(properties);
        this.wood = wood;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(MOTIF, SherdMotif.ANGLER));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public WoodKind wood() {
        return this.wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MOTIF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
