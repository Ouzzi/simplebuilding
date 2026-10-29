package com.simpleredstone.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.simpleredstone.RedstoneRegistry;
import com.simpleredstone.SimpleRedstone;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Logic of the Double Piston (BS28-3, variant B: 1x2). Cells in the push direction {@code f} from the base cell {@code P}:
 * {@code P} base, {@code P+f} barrel, then the head at {@code P+2f} (ext 1) or arm at {@code P+2f} and head at {@code P+3f}
 * (ext 2). Signal 1-7 aims at ext 1, 8-15 at ext 2, 0 at ext 0. A move is always one block at a time over Vanilla's
 * moving_piston (animation, entity push, collision) and Vanilla's {@link PistonStructureResolver} (limit 12, slime, no block
 * entities); after each step the block looks at the signal again and goes one more step or back. No block entity of its own.
 */
public final class DoublePistons {
    /** How far the head is out: 0 in, 1 one block, 2 two blocks. */
    public static final IntegerProperty EXT = IntegerProperty.create("ext", 0, 2);
    /** Game ticks until a step has settled and the next one may start (a Vanilla block move takes 2 to 3). */
    public static final int SETTLE_TICKS = 3;

    private DoublePistons() {}

    /** The extension a signal strength asks for: 0 none, 1-7 one block, 8-15 two blocks. */
    public static int targetExt(int signal) {
        return signal <= 0 ? 0 : signal < 8 ? 1 : 2;
    }

    /**
     * The strongest signal that reaches the base like it reaches a Vanilla piston: every side but the front, the cell itself
     * from below and the cell above from all sides but below (quasi-connectivity, so Vanilla machines work the same).
     */
    public static int signal(Level level, BlockPos pos, Direction facing) {
        int best = 0;
        for (Direction d : Direction.values()) {
            if (d != facing) best = Math.max(best, level.getSignal(pos.relative(d), d));
        }
        best = Math.max(best, level.getSignal(pos, Direction.DOWN));
        BlockPos above = pos.above();
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN) best = Math.max(best, level.getSignal(above.relative(d), d));
        }
        return best;
    }

    /** True while the head still has a block move running (the next step waits for it). */
    public static boolean moving(Level level, BlockPos base, Direction f) {
        for (int i = 2; i <= 3; i++) {
            if (level.getBlockState(base.relative(f, i)).is(Blocks.MOVING_PISTON)) return true;
        }
        return false;
    }

    public static void schedule(Level level, BlockPos pos, Block block, int delay) {
        if (!level.getBlockTicks().hasScheduledTick(pos, block)) level.scheduleTick(pos, block, delay);
    }

    /** Whether the parts around the base match its recorded extension. */
    public static boolean consistent(Level level, BlockPos base, Direction f, int ext) {
        if (ext == 0) return true;
        if (!isPart(level.getBlockState(base.relative(f, 2)), ext == 1 ? RedstoneRegistry.PISTON_HEAD : RedstoneRegistry.PISTON_ARM, f)) return false;
        return ext == 1 || isPart(level.getBlockState(base.relative(f, 3)), RedstoneRegistry.PISTON_HEAD, f);
    }

    public static boolean isPart(BlockState state, Block block, Direction f) {
        return state.is(block) && state.getValue(DirectionalBlock.FACING) == f;
    }

    /** One step of the base at {@code pos} towards the extension its signal asks for; schedules the next look. */
    public static void step(ServerLevel level, BlockPos pos, BlockState base) {
        if (!SimpleRedstone.doublePistonOn()) return;
        Direction f = base.getValue(DirectionalBlock.FACING);
        int cur = base.getValue(EXT);
        if (!isPart(level.getBlockState(pos.relative(f)), RedstoneRegistry.PISTON_BARREL, f)) return;
        if (!level.hasChunkAt(pos.relative(f, 4))) return;
        if (moving(level, pos, f)) {
            schedule(level, pos, base.getBlock(), 1);
            return;
        }
        if (!consistent(level, pos, f, cur)) {
            repair(level, pos, f);
            return;
        }
        int target = targetExt(signal(level, pos, f));
        if (target == cur) return;
        boolean moved = target > cur ? extend(level, pos, f, cur) : retract(level, pos, f, cur);
        if (moved) schedule(level, pos, base.getBlock(), SETTLE_TICKS);
    }

    /** A lost head or arm (explosion, command): clear what is left of them and pull the base back to 0. */
    private static void repair(ServerLevel level, BlockPos base, Direction f) {
        for (int i = 2; i <= 3; i++) {
            BlockPos cell = base.relative(f, i);
            BlockState state = level.getBlockState(cell);
            if (state.is(RedstoneRegistry.PISTON_HEAD) || state.is(RedstoneRegistry.PISTON_ARM)) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        setExt(level, base, f, 0);
    }

    private static boolean extend(ServerLevel level, BlockPos base, Direction f, int cur) {
        BlockPos pistonPos = base.relative(f, cur + 1);
        BlockPos headPos = pistonPos.relative(f);
        PistonStructureResolver resolver = new PistonStructureResolver(level, pistonPos, f, true);
        if (!resolver.resolve()) return false;
        setExt(level, base, f, cur + 1);
        if (cur == 1) level.setBlock(pistonPos, RedstoneRegistry.PISTON_ARM.defaultBlockState().setValue(DirectionalBlock.FACING, f), 2 | 16);
        pushLine(level, resolver, f, headPos, RedstoneRegistry.PISTON_HEAD.defaultBlockState().setValue(DirectionalBlock.FACING, f));
        level.playSound(null, base, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
        return true;
    }

    private static boolean retract(ServerLevel level, BlockPos base, Direction f, int cur) {
        BlockPos headPos = base.relative(f, cur + 1);
        BlockState head = level.getBlockState(headPos);
        setExt(level, base, f, cur - 1);
        if (cur == 2) {
            // The head slides back one block over the arm's cell, which becomes the moving block.
            BlockPos armPos = base.relative(f, 2);
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 2 | 16 | 64);
            BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, f);
            level.setBlock(armPos, moving, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(armPos, moving, head, f, false, false));
        } else {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 3);
        }
        level.playSound(null, base, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.15F + 0.6F);
        return true;
    }

    /** Writes the extension into base and barrel (no shape updates: the parts are mid-change). */
    public static void setExt(Level level, BlockPos base, Direction f, int ext) {
        BlockState a = level.getBlockState(base);
        if (a.is(RedstoneRegistry.DOUBLE_PISTON)) level.setBlock(base, a.setValue(EXT, ext), 2 | 16);
        BlockPos barrelPos = base.relative(f);
        BlockState b = level.getBlockState(barrelPos);
        if (b.is(RedstoneRegistry.PISTON_BARREL)) level.setBlock(barrelPos, b.setValue(EXT, ext), 2 | 16);
    }

    /** The base cell behind a part ({@code pos}, facing {@code f}), or null. */
    public static BlockPos findBase(Level level, BlockPos pos, Direction f) {
        for (int k = 1; k <= 3; k++) {
            BlockPos p = pos.relative(f.getOpposite(), k);
            if (isPart(level.getBlockState(p), RedstoneRegistry.DOUBLE_PISTON, f)) return p;
        }
        return null;
    }

    /**
     * Vanilla's {@code PistonBaseBlock.moveBlocks} for extending, with the head as a moving block of its own: everything the
     * resolver found moves one cell, the head takes the cell the first block left.
     */
    private static void pushLine(Level level, PistonStructureResolver resolver, Direction direction, BlockPos headPos, BlockState headState) {
        Map<BlockPos, BlockState> deleteAfterMove = Maps.newHashMap();
        List<BlockPos> toPush = resolver.getToPush();
        List<BlockState> toPushShapes = Lists.newArrayList();
        for (BlockPos pos : toPush) {
            BlockState state = level.getBlockState(pos);
            toPushShapes.add(state);
            deleteAfterMove.put(pos, state);
        }
        List<BlockPos> toDestroy = resolver.getToDestroy();
        BlockState[] toUpdate = new BlockState[toPush.size() + toDestroy.size()];
        int updateIndex = 0;
        for (int i = toDestroy.size() - 1; i >= 0; i--) {
            BlockPos pos = toDestroy.get(i);
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(state, level, pos, blockEntity);
            state.onDestroyedByPushReaction(level, pos, direction, level.getFluidState(pos));
            toUpdate[updateIndex++] = state;
        }
        for (int i = toPush.size() - 1; i >= 0; i--) {
            BlockPos pos = toPush.get(i);
            BlockState blockState = level.getBlockState(pos);
            pos = pos.relative(direction);
            deleteAfterMove.remove(pos);
            BlockState actualState = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction);
            level.setBlock(pos, actualState, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(pos, actualState, toPushShapes.get(i), direction, true, false));
            toUpdate[updateIndex++] = blockState;
        }
        BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction);
        deleteAfterMove.remove(headPos);
        level.setBlock(headPos, moving, 324);
        level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(headPos, moving, headState, direction, true, false));
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : deleteAfterMove.keySet()) {
            level.setBlock(pos, air, 82);
        }
        for (Map.Entry<BlockPos, BlockState> entry : deleteAfterMove.entrySet()) {
            BlockPos pos = entry.getKey();
            entry.getValue().updateIndirectNeighbourShapes(level, pos, 2);
            air.updateNeighbourShapes(level, pos, 2);
            air.updateIndirectNeighbourShapes(level, pos, 2);
        }
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, resolver.getPushDirection(), null);
        updateIndex = 0;
        for (int i = toDestroy.size() - 1; i >= 0; i--) {
            BlockState state = toUpdate[updateIndex++];
            BlockPos pos = toDestroy.get(i);
            if (level instanceof ServerLevel serverLevel) state.affectNeighborsAfterRemoval(serverLevel, pos, false);
            state.updateIndirectNeighbourShapes(level, pos, 2);
            level.updateNeighborsAt(pos, state.getBlock(), orientation);
        }
        for (int i = toPush.size() - 1; i >= 0; i--) {
            level.updateNeighborsAt(toPush.get(i), toUpdate[updateIndex++].getBlock(), orientation);
        }
        level.updateNeighborsAt(headPos, headState.getBlock(), orientation);
    }
}
