package com.simpleblocks;

import com.simplebuilding.framework.api.Protection;
import com.simplelib.api.HammerActions;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Who may change a block with a Simple Blocks use-block event (Shapecutter upgrade, metal finish, snowlogging): the framework
 * {@link Protection} (claims of any Simple mod) plus every permission a partner registered with SimpleLib
 * {@link HammerActions#registerPermission}. Same rule as Simple Hammers' and Simple Chisels' protection classes. These events run
 * as Fabric {@code UseBlockCallback}s, which may answer before a claim mod's own hook on {@code useItemOn}, so the module asks
 * itself (a-audsb31, simpleblocks-P2-1). Asked on the server only; on the client everything is allowed, the server has the last word.
 */
public final class BlocksProtection {
    private BlocksProtection() {}

    public static boolean mayBuild(Level level, Player player, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (Protection.active(server.getServer())) {
            boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(serverPlayer.createCommandSourceStack());
            var target = new Protection.Target(serverPlayer.getUUID(), server.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), admin);
            if (!Protection.allows(server.getServer(), target, Protection.Action.BUILD)) return false;
        }
        return HammerActions.mayChange(level, player, pos);
    }
}
