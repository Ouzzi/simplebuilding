package com.simplestorage.chest;

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
 * Who may change a storage block in the world (waxing, scraping; the same check as Simple Hammers): framework {@link Protection} (claims of any Simple mod) plus every permission
 * a partner registered with SimpleLib {@link HammerActions#registerPermission} (SimpleBuilding: its WorldPermissions).
 * Asked on the server only; on the client everything is allowed, the server has the last word.
 */
public final class StorageProtection {
    private StorageProtection() {}

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

    /**
     * Whether {@code player} may change the clicked block at {@code pos} with {@code stack}: the question a block placement
     * asks (spectator, adventure mode, spawn protection) and the claims above.
     */
    public static boolean mayTransform(Level level, Player player, BlockPos pos, Direction face, ItemStack stack) {
        return !player.isSpectator() && player.mayBuild() && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, face, stack) && mayAct(level, player, pos);
    }
}
