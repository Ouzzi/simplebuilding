package com.simplebuilding.api;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Public server protection extension. No provider means unchanged existing behavior. */
public final class WorldPermissions {
    /** The 26.3 overlay supplies a framework bridge; other lines have no service provider. */
    public interface Bridge {
        boolean active(ServerLevel level);
        boolean allows(ServerPlayer player,ServerLevel level,BlockPos target);
    }
    private static final Bridge BRIDGE=ServiceLoader.load(Bridge.class,WorldPermissions.class.getClassLoader()).findFirst().orElse(null);
    public static boolean active(Level level) {return level instanceof ServerLevel server && BRIDGE!=null && BRIDGE.active(server);}
    public static boolean mayAct(Level level,Player player,BlockPos target) {
        return !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)
                || !active(level) || BRIDGE.allows(serverPlayer,serverLevel,target.immutable());
    }
    /** Checks every occupied chunk, with a hard bound only while providers are installed. */
    public static boolean mayOccupy(Level level,Player player,AABB box) {
        if(!active(level))return true;
        int minX=net.minecraft.util.Mth.floor(box.minX)>>4,maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4;
        int minZ=net.minecraft.util.Mth.floor(box.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;
        if((long)(maxX-minX+1)*(maxZ-minZ+1)>256)return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!mayAct(level,player,new BlockPos(x*16,net.minecraft.util.Mth.floor(box.minY),z*16)))return false;
        return true;
    }
    public static boolean mayAffectEntity(Player player,Entity target) {return mayOccupy(target.level(),player,target.getBoundingBox());}
    public static boolean mayTeleport(ServerPlayer player,ServerLevel targetLevel,Vec3 target) {
        return mayOccupy(player.level(),player,player.getBoundingBox())&&mayOccupy(targetLevel,player,player.getBoundingBox().move(target.subtract(player.position())));
    }
    /** Extra cells changed by placement callbacks, before any block or inventory mutation. */
    public static boolean mayPlace(Level level,Player player,BlockPos pos,BlockState state) {
        if(!active(level))return true;
        if(!mayAct(level,player,pos))return false;
        if(state.hasProperty(BedBlock.PART)&&!mayAct(level,player,pos.relative(state.getValue(BedBlock.FACING))))return false;
        if(state.getBlock() instanceof ChestBlock)for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var neighbor=pos.relative(direction);if(level.getBlockState(neighbor).getBlock() instanceof ChestBlock&&!mayAct(level,player,neighbor))return false;}
        return true;
    }
    public static boolean mayChange(Level level,Player player,BlockPos pos) {
        if(!active(level))return true;
        if(!mayAct(level,player,pos))return false;
        var state=level.getBlockState(pos);
        if(state.hasProperty(BedBlock.PART)){var direction=state.getValue(BedBlock.FACING);if(state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD)direction=direction.getOpposite();return mayAct(level,player,pos.relative(direction));}
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE)return mayAct(level,player,ChestBlock.getConnectedBlockPos(pos,state));
        return true;
    }
    private WorldPermissions() {}
}
