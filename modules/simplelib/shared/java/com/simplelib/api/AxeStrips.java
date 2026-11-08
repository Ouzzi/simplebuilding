package com.simplelib.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Extra ways an axe strips a block, for mods whose wood the Vanilla axe table does not list (Simple End). Machines that
 * apply the axe without a click on a placed block (the Simple Tools strip station) ask {@link #stripped}; a mod that adds
 * wood registers a function from a state to its stripped state (null for every other state). Content-free: Vanilla types only.
 */
public final class AxeStrips {
    private static final List<UnaryOperator<BlockState>> WAYS = new CopyOnWriteArrayList<>();

    private AxeStrips() {}

    public static void register(UnaryOperator<BlockState> way) {
        WAYS.add(way);
    }

    /** The stripped state of the first registered way that knows this block, or null. */
    public static @Nullable BlockState stripped(BlockState state) {
        for (UnaryOperator<BlockState> way : WAYS) {
            BlockState out = way.apply(state);
            if (out != null) return out;
        }
        return null;
    }
}
