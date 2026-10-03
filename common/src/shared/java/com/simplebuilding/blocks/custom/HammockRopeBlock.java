package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The ropes of a hammock ({@link HammockLayout}): {@code end} above each cloth block, fanning up to the anchor, and
 * {@code span} across the third block of a gap of 3. {@code facing} points towards the anchor. Uncoloured and shared by
 * all hammocks; no item, no drops, no collision. A click on the ropes rests in the hammock like a click on the cloth.
 */
public class HammockRopeBlock extends HorizontalDirectionalBlock {
    public enum Kind implements StringRepresentable {
        END("end"),
        SPAN("span");

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final MapCodec<HammockRopeBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(HammockRopeBlock::new);
    /** Shapes with the anchor to the north, turned like the block. */
    private static final Map<Direction, VoxelShape> ENDS = Util.make(() -> Shapes.rotateHorizontal(Block.box(1.0, 0.0, 0.0, 15.0, 10.0, 4.0)));
    private static final Map<Direction, VoxelShape> SPANS = Util.make(() -> Shapes.rotateHorizontal(Block.box(7.0, 6.0, 0.0, 9.0, 9.0, 16.0)));

    public HammockRopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, Kind.END));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<HammockRopeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(KIND) == Kind.END ? ENDS : SPANS).get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return HammockLayout.ropeHangs(level, pos, state) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        HammockLayout.quietlyRemoveLootOwner(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        BlockPos head = HammockLayout.clothHead(level, pos);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && (head == null || !HammockBlock.rest(serverPlayer, head))) {
            HammockLayout.refuse(serverPlayer, null);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
