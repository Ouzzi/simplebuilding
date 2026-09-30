package com.simplebuilding.api;
import com.simplebuilding.framework.api.Protection;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
/** Minecraft-specific adapter; only the 26.3 overlay publishes this service. */
public final class FrameworkProtection implements WorldPermissions.Bridge {
    @Override public boolean active(ServerLevel level){return Protection.active(level.getServer());}
    @Override public boolean allows(ServerPlayer player,ServerLevel level,BlockPos pos){return Protection.allows(level.getServer(),new Protection.Target(player.getUUID(),level.dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack())));}
}
