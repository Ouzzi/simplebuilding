package com.simplebuilding.blocks.custom;

import com.simplebuilding.util.ModTags;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What an Astral piston (pushes) or a Nihil piston (pulls) does when it fires
 * (docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md): in all six directions at once, at most one block per
 * direction, exactly one cell, never a chain.
 * <ul>
 *   <li>Astral: source {@code pos+d}, target {@code pos+2d}.</li>
 *   <li>Nihil: source {@code pos+2d}, target {@code pos+d} - the block across a one-cell gap moves next
 *       to the piston, a block already touching it stays.</li>
 * </ul>
 * A move needs a movable source ({@link #movable}) and a free target ({@link #free}). Everything is
 * planned against the current world first ({@link #plan}); {@link #fire} then applies the moves, each
 * only if its source and target are still what was planned and neither cell was already a source or
 * target of any End piston in this game tick (one move per block and tick, no races, no duplication).
 */
public final class EndPistonMoves {

    /** One planned move: {@code state} goes from {@code from} to {@code to}, in direction {@code facing} of the piston. */
    public record Move(BlockPos from, BlockPos to, Direction facing, BlockState state) {
    }

    private static final class Lock {
        long tick = Long.MIN_VALUE;
        final LongSet cells = new LongOpenHashSet();
    }

    private static final Map<Level, Lock> LOCKS = new WeakHashMap<>();

    private EndPistonMoves() {
    }

    /** All moves this piston would make right now; changes nothing. */
    public static List<Move> plan(Level level, BlockPos pos, boolean astral) {
        List<Move> moves = new ArrayList<>(6);
        for (Direction d : Direction.values()) {
            BlockPos from = astral ? pos.relative(d) : pos.relative(d, 2);
            BlockPos to = astral ? pos.relative(d, 2) : pos.relative(d);
            if (!level.isLoaded(from) || !level.isLoaded(to)) continue;
            BlockState state = level.getBlockState(from);
            Direction moveDir = astral ? d : d.getOpposite();
            if (!movable(state, level, from, moveDir, d) || !free(level, to)) continue;
            if (!com.simplebuilding.api.WorldPermissions.mayAutomate(level, pos, from)
                    || !com.simplebuilding.api.WorldPermissions.mayAutomate(level, pos, to)) continue;
            moves.add(new Move(from.immutable(), to.immutable(), d, state));
        }
        return moves;
    }

    /**
     * Whether a single block may be moved one cell in {@code moveDir} by an End piston facing
     * {@code facing}. Vanilla's own rule without breaking anything (build height, world border,
     * obsidian and the like, hardness -1, push reaction BLOCK/DESTROY, PUSH_ONLY only away from the
     * piston, block entities, extended pistons), plus: any piston, head or moving block, the End signal
     * blocks themselves (a piston never kicks away its own switch or powder), two-part blocks,
     * liquids, anything unbreakable and the tag {@code simplebuilding:end_piston_immovable}.
     */
    public static boolean movable(BlockState state, Level level, BlockPos pos, Direction moveDir, Direction facing) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.hasBlockEntity()) return false;
        Block block = state.getBlock();
        if (block instanceof PistonBaseBlock || block instanceof PistonHeadBlock || block instanceof MovingPistonBlock
                || block instanceof EndSignalBlock || state.is(Blocks.MOVING_PISTON)) return false;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) return false;
        if (state.is(ModTags.Blocks.END_PISTON_IMMOVABLE) || state.getDestroySpeed(level, pos) < 0.0F) return false;
        return PistonBaseBlock.isPushable(state, level, pos, moveDir, false, facing);
    }

    /** A target cell: inside the world and border, air or replaceable, but never a liquid source. */
    public static boolean free(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        return state.canBeReplaced() && !state.getFluidState().isSource() && !state.hasBlockEntity();
    }

    /** Plans and applies; returns how many blocks moved. */
    public static int fire(ServerLevel level, BlockPos pos, boolean astral) {
        Lock lock = LOCKS.computeIfAbsent(level, l -> new Lock());
        if (lock.tick != level.getGameTime()) {
            lock.tick = level.getGameTime();
            lock.cells.clear();
        }
        int moved = 0;
        for (Move move : plan(level, pos, astral)) {
            long from = move.from().asLong(), to = move.to().asLong();
            if (lock.cells.contains(from) || lock.cells.contains(to)) continue;
            if (level.getBlockState(move.from()) != move.state() || !free(level, move.to())) continue;
            lock.cells.add(from);
            lock.cells.add(to);
            apply(level, move, astral);
            level.blockEvent(pos, level.getBlockState(pos).getBlock(), move.facing().get3DDataValue(), Block.getId(move.state()));
            moved++;
        }
        if (moved > 0) {
            level.playSound(null, pos, astral ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS,
                    0.5F, 1.2F + level.getRandom().nextFloat() * 0.1F);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    6, 0.4, 0.4, 0.4, 0.02);
        }
        return moved;
    }

    /**
     * Vanilla's moveBlocks for a single block: the target becomes a moving piston carrying it, the source
     * air. The piston then sends one block event per move ({@link EndPistonBlock#triggerEvent}) so clients
     * build the same moving block and show the slide ({@link #showOnClient}); the event carries the moved
     * state, because the source is already air on the client when the event arrives.
     */
    private static void apply(ServerLevel level, Move move, boolean astral) {
        BlockState target = level.getBlockState(move.to());
        if (!target.isAir() && target.getFluidState().isEmpty()) {
            Block.dropResources(target, level, move.to());
        }
        BlockState moving = movingState(move.facing(), astral);
        level.setBlock(move.from(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
        level.setBlock(move.to(), moving, Block.UPDATE_CLIENTS | Block.UPDATE_MOVE_BY_PISTON);
        level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(move.to(), moving, move.state(), move.facing(), astral, false));
        level.updateNeighborsAt(move.from(), Blocks.AIR);
        level.updateNeighborsAt(move.to(), Blocks.MOVING_PISTON);
    }

    private static BlockState movingState(Direction facing, boolean astral) {
        return Blocks.MOVING_PISTON.defaultBlockState()
                .setValue(MovingPistonBlock.FACING, facing)
                .setValue(MovingPistonBlock.TYPE, astral ? PistonType.DEFAULT : PistonType.STICKY);
    }

    /** Client side of one move: the same moving block the server made, so the block visibly slides. */
    public static void showOnClient(Level level, BlockPos pos, boolean astral, Direction facing, BlockState carried) {
        if (carried.isAir()) return;
        BlockPos from = astral ? pos.relative(facing) : pos.relative(facing, 2);
        BlockPos to = astral ? pos.relative(facing, 2) : pos.relative(facing);
        BlockState moving = movingState(facing, astral);
        level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_CLIENTS);
        level.setBlock(to, moving, Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_INVISIBLE);
        level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(to, moving, carried, facing, astral, false));
    }
}
