package com.simplebuilding.api;
import com.simplebuilding.framework.api.Protection;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
/** Minecraft-specific adapter; only the 26.3 overlay publishes this service. */
public final class FrameworkProtection implements WorldPermissions.Bridge {
    @Override public boolean active(ServerLevel level){return Protection.active(level.getServer());}
    @Override public boolean allows(ServerPlayer player,ServerLevel level,BlockPos pos){return allows(player,level,pos,WorldPermissions.Action.ANY);}
    @Override public boolean allows(ServerPlayer player,ServerLevel level,BlockPos pos,WorldPermissions.Action action){return Protection.allows(level.getServer(),new Protection.Target(player.getUUID(),level.dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack())),Protection.Action.valueOf(action.name()));}
    @Override public boolean allowsAutomation(ServerLevel level,BlockPos source,BlockPos target){
        String dimension=level.dimension().identifier().toString();
        return Protection.allowsAutomation(level.getServer(),new Protection.Target(null,dimension,source.getX(),source.getY(),source.getZ(),false),new Protection.Target(null,dimension,target.getX(),target.getY(),target.getZ(),false));
    }
}
