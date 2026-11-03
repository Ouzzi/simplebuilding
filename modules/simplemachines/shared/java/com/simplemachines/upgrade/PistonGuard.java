package com.simplemachines.upgrade;

import com.simplebuilding.framework.api.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Who may let a piston upgrade destroy a block (SimpleBuilding's {@code PistonBreakGuard} + {@code WorldPermissions.mayAutomate},
 * MACH32-MOD stage 2): claims through framework {@link Protection} (automation: the piston and its target must both allow it),
 * the world border, then the loader's side ({@link Loader}): Fabric fires {@code PlayerBlockBreakEvents} with its fake player
 * while {@code pistons.fireBreakEvents} is on. Without a loader side everything the claims allow is allowed.
 */
public final class PistonGuard {
    /** The loader's events: the piston event once per action, the block break event once per block. */
    public interface Loader {
        default boolean mayMove(ServerLevel level, BlockPos piston, Direction facing) {
            return true;
        }

        default boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
            return true;
        }
    }

    /** Lets everything through (no loader side installed). */
    public static final Loader ALLOW = new Loader() {};

    private static volatile Loader loader = ALLOW;

    private PistonGuard() {}

    public static void install(Loader guard) {
        loader = guard != null ? guard : ALLOW;
    }

    /** The installed loader side (game tests swap it briefly). */
    public static Loader loader() {
        return loader;
    }

    /** The piston event for one piston action. */
    public static boolean mayMove(ServerLevel level, BlockPos piston, Direction facing) {
        return loader.mayMove(level, piston, facing);
    }

    /** Whether the piston at {@code piston} may destroy {@code target}: world border, claims (automation), then the loader. */
    public static boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        if (!level.getWorldBorder().isWithinBounds(target) || !mayAutomate(level, piston, target)) {
            return false;
        }
        return loader.mayBreak(level, piston, facing, target, state);
    }

    /**
     * Whether a machine at {@code source} may act on {@code target} (and the other half of a bed or double chest there): framework
     * {@link Protection#allowsAutomation}, no player. Always true on the client and without a protection provider.
     */
    public static boolean mayAutomate(Level level, BlockPos source, BlockPos target) {
        if (!(level instanceof ServerLevel server) || !Protection.active(server.getServer())) return true;
        if (!allows(server, source, source) || !allows(server, source, target)) return false;
        BlockState state = level.getBlockState(target);
        if (state.hasProperty(BedBlock.PART)) {
            Direction direction = state.getValue(BedBlock.FACING);
            if (state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) direction = direction.getOpposite();
            if (!allows(server, source, target.relative(direction))) return false;
        }
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && !allows(server, source, ChestBlock.getConnectedBlockPos(target, state))) {
            return false;
        }
        return true;
    }

    private static boolean allows(ServerLevel level, BlockPos source, BlockPos target) {
        String dimension = level.dimension().identifier().toString();
        return Protection.allowsAutomation(level.getServer(),
                new Protection.Target(null, dimension, source.getX(), source.getY(), source.getZ(), false),
                new Protection.Target(null, dimension, target.getX(), target.getY(), target.getZ(), false));
    }
}
