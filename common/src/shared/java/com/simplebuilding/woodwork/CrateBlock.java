package com.simplebuilding.woodwork;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A crate (docs/ai/PLAN-HOLZWERK-2026-10-09.md): an open wooden box shaped like the composter that stores up to
 * eight stacks of food. Right-click with food puts the held stack in, an empty hand takes the top stack out
 * (sneaking: one item). Hoppers, comparators and breaking work through the Vanilla {@code Container} of
 * {@link CrateBlockEntity}; the crate itself drops as an item. The opening ({@link #FACING}) points to the player
 * when placed, like a barrel (Queue N31): laid on its side, the food lies on the lower inner wall (CrateRenderer).
 * The walls are slats with gaps (generator {@code tools/textures/woodwork_2026_10_09.py}).
 */
public class CrateBlock extends BaseEntityBlock {
    public static final MapCodec<CrateBlock> CODEC = BlockCodecs.simple(CrateBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.values()) {
            double[] in = interior(facing);
            SHAPES.put(facing, Shapes.join(Shapes.block(), Block.box(in[0], in[1], in[2], in[3], in[4], in[5]), BooleanOp.ONLY_FIRST));
        }
    }

    private final WoodKind wood;

    public CrateBlock(Properties properties) {
        this(WoodKind.OAK, properties);
    }

    public CrateBlock(WoodKind wood, Properties properties) {
        super(properties);
        this.wood = wood;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    /**
     * The open inside of a crate whose opening points to {@code facing}, in pixels {minX, minY, minZ, maxX, maxY, maxZ}:
     * 2-pixel walls and bottom, open on the facing side.
     */
    public static double[] interior(Direction facing) {
        double[] box = {2, 2, 2, 14, 14, 14};
        int axis = facing.getAxis().ordinal();
        if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box[3 + axis] = 16;
        } else {
            box[axis] = 0;
        }
        return box;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public WoodKind wood() {
        return this.wood;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!CrateBlockEntity.storable(stack)) {
            // Only an empty hand takes out; anything else keeps its own use (placing a block against the crate).
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack rest = crate.insert(stack);
            if (rest.getCount() == stack.getCount()) {
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, rest);
            }
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate) || crate.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack taken = crate.takeTop(player.isShiftKeyDown() ? 1 : Integer.MAX_VALUE);
            if (!player.getInventory().add(taken) && !taken.isEmpty()) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), taken);
            }
            level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
