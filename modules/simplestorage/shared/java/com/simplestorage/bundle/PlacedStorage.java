package com.simplestorage.bundle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;

/**
 * Where a set-down bag may stand (B23-M12, owner 2026-10-23) - bundles, quivers ({@link PlacedBundleBlock}) and backpacks
 * ({@code BackpackBlock}) alike:
 * <ul>
 *   <li><b>floor</b> (clicked the top of a block): a gravity block like sand or the dragon egg - without a block below it
 *       falls as a {@link FallingBlockEntity} and keeps everything it holds ({@link #fall}); if it breaks on landing
 *       (a torch, a slab gap), the stored item drops, not an empty bag ({@code FallingStorageDropMixin});</li>
 *   <li><b>wall</b> ({@link #WALL}, clicked the side of a block): hangs on that wall like an item frame - back against
 *       the wall, a little raised; it stays as long as the block behind is solid ({@link #canSurvive}), else it drops;</li>
 *   <li><b>ceiling</b>: not placeable ({@link #attach} returns null).</li>
 * </ul>
 */
public final class PlacedStorage {
    /** Hangs on the wall behind its back (opposite of its facing) instead of standing on the floor. */
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    /** Ticks between losing the floor and starting to fall (sand: 2). */
    public static final int FALL_DELAY = 2;

    private PlacedStorage() {
    }

    /** A block of a set-down bag: builds the item stack back from its block entity (for drops after a broken fall). */
    public interface Falling {
        ItemStack storedStack(BlockEntity blockEntity);
    }

    /**
     * The placement state: on the floor facing the player, on a wall facing away from it (the clicked side), null for
     * the ceiling. The state still has to survive there ({@link #canSurvive}).
     */
    public static @Nullable BlockState attach(BlockState state, BlockPlaceContext context, EnumProperty<Direction> facing) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) return null;
        BlockState placed = face == Direction.UP
                ? state.setValue(WALL, false).setValue(facing, context.getHorizontalDirection().getOpposite())
                : state.setValue(WALL, true).setValue(facing, face);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    /** Floor: always (gravity does the rest). Wall: the block behind must be solid, like for an item frame. */
    public static boolean canSurvive(BlockState state, LevelReader level, BlockPos pos, EnumProperty<Direction> facing) {
        if (!state.getValue(WALL)) return true;
        return level.getBlockState(pos.relative(state.getValue(facing).getOpposite())).isSolid();
    }

    /** True while a floor bag has nothing to stand on (air, fire, fluid, replaceable plants). */
    public static boolean shouldFall(BlockState state, LevelReader level, BlockPos pos) {
        return !state.getValue(WALL) && pos.getY() > level.getMinY() && FallingBlock.isFree(level.getBlockState(pos.below()));
    }

    /** Starts the fall of a floor bag: the falling block carries the block entity data and puts it back on landing. */
    public static void fall(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag data = be == null ? null : be.saveWithoutMetadata(level.registryAccess());
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
        falling.blockData = data;
    }

    /** The stored item of a bag that broke while falling, rebuilt from the carried block entity data. */
    public static ItemStack fallenStack(ServerLevel level, BlockState state, @Nullable CompoundTag data) {
        if (data == null || !(state.getBlock() instanceof Falling falling) || !(state.getBlock() instanceof EntityBlock entityBlock)) {
            return ItemStack.EMPTY;
        }
        BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, state);
        if (be == null) return ItemStack.EMPTY;
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        return falling.storedStack(be);
    }

    /** Is {@code block} one of the set-down bags (for the falling-block drop hook)? */
    public static boolean isFalling(Block block) {
        return block instanceof Falling;
    }
}
