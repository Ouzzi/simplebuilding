package com.simplebuildingtools.util;

import com.simplebuilding.framework.api.Protection;
import com.simplelib.api.HammerActions;
import com.simplelib.api.InteractionGuard;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The one question every multi-block tool of the mod asks before it changes a block the player did not click (until 07.11.2026
 * SimpleBuilding's {@code BuildPermissions}/{@code WorldPermissions}): Vanilla spawn protection and the world border, framework
 * {@link Protection} (claims of any Simple mod), the permissions partners registered with SimpleLib
 * ({@link HammerActions#mayChange}, SimpleBuilding's WorldPermissions) and the loader's own event ({@link #setPlaceProbe}, Fabric:
 * the block break event, as SimpleBuilding fired it). Asked on the server only; on the client everything is allowed.
 */
public final class BuildPermissions {
    /** The loader's place/break probe; default: allowed. */
    public interface Probe {
        boolean allows(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);
    }

    private static Probe probe = (level, player, pos, state) -> true;

    private BuildPermissions() {
    }

    public static void setPlaceProbe(Probe loaderProbe) {
        probe = loaderProbe != null ? loaderProbe : (level, player, pos, state) -> true;
    }

    /** Claims and partner permissions for {@code pos}. */
    public static boolean mayAct(Level level, Player player, BlockPos pos) {
        return mayAct(level, player, pos, Protection.Action.ANY);
    }

    public static boolean mayAct(Level level, Player player, BlockPos pos, Protection.Action action) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (Protection.active(server.getServer())) {
            boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(serverPlayer.createCommandSourceStack());
            var target = new Protection.Target(serverPlayer.getUUID(), server.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), admin);
            if (!Protection.allows(server.getServer(), target, action)) return false;
        }
        return HammerActions.mayChange(level, player, pos);
    }

    /** {@link #mayAct} for the block and the other half of a bed or double chest. */
    public static boolean mayChange(Level level, Player player, BlockPos pos) {
        if (!mayAct(level, player, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BedBlock.PART)) {
            var direction = state.getValue(BedBlock.FACING);
            if (state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) direction = direction.getOpposite();
            return mayAct(level, player, pos.relative(direction));
        }
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
            return mayAct(level, player, ChestBlock.getConnectedBlockPos(pos, state));
        }
        return true;
    }

    /** Whether {@code player} may place {@code state} at {@code pos} (the other bed half included). */
    public static boolean mayPlace(Level level, Player player, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos) || !mayAct(level, player, pos, Protection.Action.BUILD)) {
            return false;
        }
        if (state.hasProperty(BedBlock.PART)) {
            var direction = state.getValue(BedBlock.FACING);
            if (state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) direction = direction.getOpposite();
            if (!mayAct(level, player, pos.relative(direction), Protection.Action.BUILD)) return false;
        }
        // The probe fires the loader's event: hammers and miners must not take it for a real break (SimpleLib counter).
        InteractionGuard.probing++;
        try {
            return probe.allows(serverLevel, serverPlayer, pos.immutable(), state);
        } finally {
            InteractionGuard.probing--;
        }
    }
}
