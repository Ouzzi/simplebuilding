package com.simplebuilding.modules.simpleend.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * End sticks: the wood of the End (owner 2026-10-13). A bundle of sticks thicker than a placed stick: thickness 1 (twig)
 * to 3 (trunk). Upright ({@code axis=y}) a placed column follows the block above like dripstone; lying ({@code axis=x/z})
 * it is a branch. Sticks grown on a tree are {@code natural}: like chorus, a piece of a stick tree that no longer reaches
 * solid ground through sticks breaks as a whole (see {@link #collapse}). Placed sticks never fall.
 */
public class EndSticksBlock extends Block {
    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 1, 3);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");
    /** Upper bound of one collapse search; a colossal stick tree stays well below it. */
    public static final int MAX_COLLAPSE = 8192;
    private static final int MAX_PARTICLES = 48;
    private static final VoxelShape[][] SHAPES = new VoxelShape[3][3];

    static {
        double[] widths = {6.0, 10.0, 14.0};
        for (int t = 0; t < 3; t++) {
            double a = 8 - widths[t] / 2, b = 8 + widths[t] / 2;
            SHAPES[0][t] = Block.box(0, a, a, 16, b, b);
            SHAPES[1][t] = Block.box(a, 0, a, b, 16, b);
            SHAPES[2][t] = Block.box(a, a, 0, b, b, 16);
        }
    }

    /** Set while a collapse removes blocks, so their own removal does not start further searches. */
    private static boolean collapsing;

    public EndSticksBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(THICKNESS, 1).setValue(AXIS, Direction.Axis.Y).setValue(NATURAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, AXIS, NATURAL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AXIS).ordinal()][state.getValue(THICKNESS) - 1];
    }

    /** Upright thickness from the block above: open top = tip, under a tip = middle, under a middle or base = base. */
    public BlockState shaped(BlockState above) {
        return shaped(defaultBlockState(), above);
    }

    private BlockState shaped(BlockState self, BlockState above) {
        int t = above.is(this) ? Math.min(3, above.getValue(THICKNESS) + 1) : 1;
        return self.setValue(THICKNESS, t);
    }

    /** A grown stick: natural, with the given axis and thickness. */
    public BlockState grown(Direction.Axis axis, int thickness) {
        return defaultBlockState().setValue(AXIS, axis).setValue(THICKNESS, thickness).setValue(NATURAL, true);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        if (axis != Direction.Axis.Y) return defaultBlockState().setValue(AXIS, axis).setValue(THICKNESS, 2);
        return shaped(context.getLevel().getBlockState(context.getClickedPos().above()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction != Direction.UP || state.getValue(AXIS) != Direction.Axis.Y || state.getValue(NATURAL)) return state;
        return shaped(state, neighbourState);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (state.getValue(NATURAL) && !collapsing && !level.getBlockState(pos).is(this)) collapse(level, pos);
    }

    private boolean natural(BlockState s) {
        return s.is(this) && s.getValue(NATURAL);
    }

    /**
     * A tree stands on its trunk: a natural upright stick of full thickness (trunk or scrub base) on a solid block that
     * is not part of a tree. Roots and branches (thinner) never hold a tree up, so cutting the trunk fells it.
     */
    private boolean grounded(BlockGetter level, BlockPos p) {
        BlockState self = level.getBlockState(p);
        if (self.getValue(AXIS) != Direction.Axis.Y || self.getValue(THICKNESS) != 3) return false;
        BlockState below = level.getBlockState(p.below());
        return !below.is(this) && !below.isAir() && !below.canBeReplaced() && !below.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    /**
     * Chorus-like fall after a natural stick at {@code removed} is gone: every neighbouring group of natural sticks that
     * no longer reaches ground breaks at once. Drops are merged into a few stacks at the removed position; only the
     * first {@value #MAX_PARTICLES} blocks show break particles. Returns the number of sticks that fell.
     */
    public int collapse(ServerLevel level, BlockPos removed) {
        Set<BlockPos> seen = new HashSet<>();
        int fallen = 0;
        for (Direction d : Direction.values()) {
            BlockPos start = removed.relative(d);
            if (seen.contains(start) || !natural(level.getBlockState(start))) continue;
            List<BlockPos> group = new ArrayList<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            boolean grounded = false;
            while (!queue.isEmpty() && group.size() < MAX_COLLAPSE) {
                BlockPos p = queue.poll();
                group.add(p);
                if (grounded(level, p)) {
                    grounded = true;
                    break;
                }
                for (Direction n : Direction.values()) {
                    BlockPos q = p.relative(n);
                    if (!seen.contains(q) && level.isLoaded(q) && natural(level.getBlockState(q))) {
                        seen.add(q);
                        queue.add(q);
                    }
                }
            }
            if (!grounded && queue.isEmpty()) fallen += fell(level, removed, group);
        }
        return fallen;
    }

    private int fell(ServerLevel level, BlockPos at, List<BlockPos> group) {
        Map<Item, Integer> drops = new LinkedHashMap<>();
        collapsing = true;
        try {
            int shown = 0;
            for (BlockPos p : group) {
                BlockState s = level.getBlockState(p);
                for (ItemStack stack : Block.getDrops(s, level, p, null)) drops.merge(stack.getItem(), stack.getCount(), Integer::sum);
                if (shown++ < MAX_PARTICLES) level.levelEvent(2001, p, Block.getId(s));
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        } finally {
            collapsing = false;
        }
        drops.forEach((item, count) -> {
            for (int left = count; left > 0; ) {
                int n = Math.min(left, new ItemStack(item).getMaxStackSize());
                Block.popResource(level, at, new ItemStack(item, n));
                left -= n;
            }
        });
        level.playSound(null, at, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 0.7F);
        return group.size();
    }
}
