package com.simplemachines.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The loader's item transfer interface for a hopper whose front is no Vanilla {@code Container} (machines of other mods), like
 * SimpleBuilding's {@code ItemAutomation}: Fabric installs the transfer API ({@code ItemStorage.SIDED}). Without a loader hook
 * nothing is inserted there.
 */
public final class HopperAutomation {
    /** {@link Inserter#insert} when there is no handler at all. */
    public static final int NO_HANDLER = -1;

    public interface Inserter {
        /** How many of {@code stack} went into the handler at {@code pos} through {@code side}, or {@link #NO_HANDLER}. */
        int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack);
    }

    private static Inserter inserter;

    private HopperAutomation() {}

    public static void install(Inserter loader) {
        inserter = loader;
    }

    /** The installed hook (tests wrap it and put it back). */
    public static Inserter current() {
        return inserter;
    }

    public static boolean available() {
        return inserter != null;
    }

    public static int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        return inserter == null ? NO_HANDLER : inserter.insert(level, pos, side, stack);
    }
}
