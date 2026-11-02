package com.simplestorage.pipe;

import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** What the pipe needs outside its block and block entity: the output order, the direction names, the upgrade in the world. */
public final class Pipes {
    /** The order the output turns through: down first (the hopper's way), then around, up last. */
    public static final Direction[] ORDER = {Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};

    /** The three states of a side (ROHR33): takes items in (normal), pushes items out (light red), neither (greyed out). */
    public static final int IN = 0, OUT = 1, OFF = 2;

    private Pipes() {
    }

    /** The next state of a side on a click: in - out - off - in. A side the step may not point to (up on the first step) skips out. */
    public static int nextMode(PipeTier tier, Direction side, int mode) {
        int next = (mode + 1) % 3;
        return next == OUT && !allowed(tier, side) ? OFF : next;
    }

    /** The six states packed two bits each, in {@link Direction#values()} order (the menu data slot and the save). */
    public static int pack(int[] modes) {
        int packed = 0;
        for (int i = 0; i < 6; i++) packed |= (modes[i] & 3) << (2 * i);
        return packed;
    }

    public static int[] unpack(int packed) {
        int[] modes = new int[6];
        for (int i = 0; i < 6; i++) modes[i] = Math.min((packed >> (2 * i)) & 3, OFF);
        return modes;
    }

    /** The old world format had one output (the block state) and every other side took in: the new states for that. */
    public static int[] fromSingleOutput(Direction output) {
        int[] modes = new int[6];
        modes[output.ordinal()] = OUT;
        return modes;
    }

    public static boolean allowed(PipeTier tier, Direction direction) {
        return direction != Direction.UP || tier.canPointUp();
    }

    /** The next allowed output after {@code current} in {@link #ORDER}. */
    public static Direction next(PipeTier tier, Direction current) {
        int at = 0;
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == current) at = i;
        }
        for (int step = 1; step <= ORDER.length; step++) {
            Direction candidate = ORDER[(at + step) % ORDER.length];
            if (allowed(tier, candidate)) return candidate;
        }
        return Direction.DOWN;
    }

    public static Component directionName(Direction direction) {
        return Component.translatable("direction.simplestorage." + direction.getName());
    }

    /**
     * Upgrade in the world ({@code in_world_upgrades/pipes.json}): the pipe keeps its item, filter and output (the properties
     * move with {@code withPropertiesOf}); the old block entity is emptied first so that nothing spills when the block changes.
     */
    public static int upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        BlockState old = level.getBlockState(pos);
        ItemStack held = ItemStack.EMPTY;
        int mode = 0;
        int[] sides = null;
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            held = pipe.getItem(0).copy();
            mode = pipe.filterMode();
            sides = pipe.sideModes();
            pipe.clearContent();
        }
        level.setBlock(pos, to.withPropertiesOf(old), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity upgraded) {
            upgraded.receiveUpgraded(held, mode);
            if (sides != null) upgraded.setSideModes(sides);
        } else if (!held.isEmpty()) {
            Block.popResource(level, pos, held);
        }
        return 1;
    }

    /** Whether the block entity type is the pipe's (tests). */
    public static boolean isPipeEntity(Object entity) {
        return entity instanceof ItemPipeBlockEntity && ((ItemPipeBlockEntity) entity).getType() == StorageBlockEntities.PIPE;
    }
}
