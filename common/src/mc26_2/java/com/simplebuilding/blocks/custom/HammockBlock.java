package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 26.2 twin of the hammock (the real one extends 26.3's {@code AbstractBedBlock}, which 26.2 lacks). Same state
 * properties and public surface so the shared code compiles; never registered ({@code McVersion.HAMMOCK} is false).
 */
public class HammockBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    public static final double SLEEP_HEIGHT = 0.25;
    public static final int CHECK_TICKS = 10;
    public static final MapCodec<HammockBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(p -> new HammockBlock(DyeColor.WHITE, p));

    private final DyeColor color;

    public HammockBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.HEAD)
                .setValue(OCCUPIED, false).setValue(HammockLayout.STRAIGHT, true));
    }

    public MapCodec<HammockBlock> codec() {
        return CODEC;
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED, HammockLayout.STRAIGHT);
    }

    /** No hammocks on 26.2. */
    public static boolean rest(ServerPlayer player, @org.jspecify.annotations.Nullable BlockPos head) {
        return false;
    }
}
