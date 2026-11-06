package com.simplechisels.item;

import com.simplebuilding.framework.api.Protection;
import com.simplelib.api.HammerActions;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Who may reshape a block with the chisel (copy of Simple Hammers' rule): the question a block placement asks (spectator,
 * adventure mode, spawn protection), framework {@link Protection} (claims of any Simple mod) and every permission a partner
 * registered with SimpleLib {@link HammerActions#registerPermission} (SimpleBuilding: its WorldPermissions, shared by every
 * reshaping tool). The claims are asked on the server only; the server has the last word.
 */
public final class ChiselProtection {
    private ChiselProtection() {}

    public static boolean mayTransform(Level level, Player player, BlockPos pos, Direction face, ItemStack stack) {
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return false;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (Protection.active(server.getServer())) {
            boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(serverPlayer.createCommandSourceStack());
            var target = new Protection.Target(serverPlayer.getUUID(), server.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), admin);
            if (!Protection.allows(server.getServer(), target, Protection.Action.ANY)) return false;
        }
        return HammerActions.mayChange(level, player, pos);
    }
}
