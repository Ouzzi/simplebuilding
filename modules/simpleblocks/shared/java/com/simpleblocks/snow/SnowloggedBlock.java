package com.simpleblocks.snow;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * SN30-1 Snowlogging (docs/ai/PLAN-B-SNOW32-2026-10-31.md): a snow layer (1-7 layers, Vanilla's snow models) that shares its
 * cell with a fence, flower or grass ({@link SnowloggedBlockEntity#held}), like Bedrock. Nothing of Vanilla's blocks changes:
 * the shared block is kept as a state in the block entity and drawn by {@code SnowloggedRenderer}. Shape and collision are
 * the snow and the held block together (dynamic, so a snowlogged fence stays 1.5 high). A shovel takes the snow first and
 * leaves the held block standing ({@link #playerDestroy}); any other way of breaking drops the held block's loot (snow only
 * with a shovel, like Vanilla). Light above 11 melts the snow, the held block stays. {@link #FENCE} tells neighbouring fences
 * whether to connect ({@code FenceConnectsSnowloggedMixin}).
 */
public class SnowloggedBlock extends Block implements EntityBlock {
    public static final int MAX_LAYERS = 7;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX_LAYERS);
    public static final EnumProperty<Fence> FENCE = EnumProperty.create("fence", Fence.class);
    private static final VoxelShape[] SHAPES = Block.boxes(8, height -> Block.column(16.0, 0.0, height * 2));

    /** Which fences connect to this cell: none (a plant), wooden fences, or the other fences (nether brick). */
    public enum Fence implements StringRepresentable {
        NONE("none"), WOODEN("wooden"), OTHER("other");

        private final String name;

        Fence(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static Fence of(BlockState held) {
            if (held == null || !held.is(BlockTags.FENCES)) return NONE;
            return held.is(BlockTags.WOODEN_FENCES) ? WOODEN : OTHER;
        }
    }

    public SnowloggedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1).setValue(FENCE, Fence.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, FENCE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SnowloggedBlockEntity(pos, state);
    }

    /** The block this cell holds, or null (no block entity yet, e.g. in shape caches with an empty level). */
    public static @Nullable BlockState held(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SnowloggedBlockEntity be ? be.held() : null;
    }

    /** The Vanilla snow layer with the same number of layers (loot, sounds, particles). */
    public static BlockState snow(BlockState state) {
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, state.getValue(LAYERS));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState held = held(level, pos);
        VoxelShape snow = SHAPES[state.getValue(LAYERS)];
        return held == null ? snow : Shapes.or(snow, held.getShape(level, pos, context));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState held = held(level, pos);
        VoxelShape snow = SHAPES[state.getValue(LAYERS) - 1];
        return held == null ? snow : Shapes.or(snow, held.getCollisionShape(level, pos, context));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND && state.getValue(FENCE) == Fence.NONE && state.getValue(LAYERS) < 5;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    /** Snow needs a full face below (Vanilla's rule); the held block keeps its own rule. */
    static boolean snowSurvives(LevelReader level, BlockPos pos) {
        return Blocks.SNOW.defaultBlockState().canSurvive(level, pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState held = held(level, pos);
        return snowSurvives(level, pos) && (held == null || held.canSurvive(level, pos));
    }

    /**
     * The held block reacts to its neighbours as if it stood alone (a fence re-connects); without support for the snow the
     * held block stays alone, without support for the held block the cell breaks (its loot drops).
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SnowloggedBlockEntity be) || be.held() == null) {
            return snowSurvives(level, pos) ? state : Blocks.AIR.defaultBlockState();
        }
        BlockState held = be.held().updateShape(level, ticks, pos, direction, neighbourPos, neighbourState, random);
        if (held.isAir() || !held.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (!snowSurvives(level, pos)) {
            return held;
        }
        if (held != be.held()) {
            be.setHeld(held);
        }
        return state.setValue(FENCE, Fence.of(held));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Melts in bright block light like Vanilla snow (no snowballs); the held block stays. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 11) {
            unsnow(level, pos);
        }
    }

    /** Takes the snow away: the held block stands alone again (or air). */
    public static void unsnow(Level level, BlockPos pos) {
        BlockState held = held(level, pos);
        level.setBlock(pos, held == null ? Blocks.AIR.defaultBlockState() : held, Block.UPDATE_ALL);
    }

    /**
     * Loot of the held block, plus the snow's loot when broken with a shovel (Vanilla's snow loot also wants an entity, so
     * melting or explosions give no snowballs).
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> out = new ArrayList<>();
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        boolean shovel = params.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack tool
                && tool.isCorrectToolForDrops(Blocks.SNOW.defaultBlockState());
        if (shovel) {
            out.addAll(snow(state).getDrops(params));
        }
        if (be instanceof SnowloggedBlockEntity carrier && carrier.held() != null) {
            out.addAll(carrier.held().getDrops(params));
        }
        return out;
    }

    /** A shovel takes the snow first: snow loot, then the held block is put back in place (the tool was already used). */
    @Override
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                              ItemStack destroyedWith) {
        BlockState held = blockEntity instanceof SnowloggedBlockEntity be ? be.held() : null;
        if (held != null && destroyedWith.isCorrectToolForDrops(Blocks.SNOW.defaultBlockState())) {
            player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            Block.dropResources(snow(state), level, pos, null, player, destroyedWith);
            if (level.getBlockState(pos).isAir() && held.canSurvive(level, pos)) {
                level.setBlock(pos, held, Block.UPDATE_ALL);
            } else {
                Block.dropResources(held, level, pos, null, player, destroyedWith);
            }
            return;
        }
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
    }

    /** Pick block gives the held block (the snow cell has no item of its own). */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockState held = held(level, pos);
        return held == null ? new ItemStack(Blocks.SNOW) : held.getCloneItemStack(level, pos, includeData);
    }
}
