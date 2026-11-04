package com.simplepads.util;

import com.simplebuilding.framework.api.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Claims for the pads through the framework's {@link Protection} (until 04.11. SimpleBuilding's {@code WorldPermissions} and
 * {@code TransformTargets}, which ask the same providers).
 */
public final class PadPermissions {
    private PadPermissions() {
    }

    /** May the player stand where he is and where the teleporter sends him (both footprints, any action). */
    public static boolean mayTeleport(ServerPlayer player, ServerLevel targetLevel, Vec3 target) {
        return allows(player, (ServerLevel) player.level(), BlockPos.containing(player.position()), Protection.Action.ANY)
                && allows(player, targetLevel, BlockPos.containing(target), Protection.Action.ANY);
    }

    /** The question a block placement asks (spectator, adventure mode, spawn protection, claims) before changing the block. */
    public static boolean mayTransform(Level level, Player player, BlockPos pos, Direction face, ItemStack stack) {
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return false;
        }
        return !(level instanceof ServerLevel server) || allows(player, server, pos, Protection.Action.BUILD);
    }

    private static boolean allows(Player player, ServerLevel level, BlockPos pos, Protection.Action action) {
        var server = level.getServer();
        if (server == null || !Protection.active(server)) return true;
        boolean admin = player instanceof ServerPlayer sp && server.getPlayerList().isOp(sp.nameAndId());
        return Protection.allows(server, new Protection.Target(player.getUUID(), level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), admin), action);
    }
}
