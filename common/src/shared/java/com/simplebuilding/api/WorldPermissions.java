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
    /** What the player does. Providers without per-action rights see every action as {@link #ANY} (strict). */
    public enum Action {BUILD,DESTROY,USE,CONTAINER,REDSTONE,ENTITY,ANY}
    /** The 26.3 overlay supplies a framework bridge; other lines have no service provider. */
    public interface Bridge {
        boolean active(ServerLevel level);
        boolean allows(ServerPlayer player,ServerLevel level,BlockPos target);
        default boolean allows(ServerPlayer player,ServerLevel level,BlockPos target,Action action){return allows(player,level,target);}
        default boolean allowsAutomation(ServerLevel level,BlockPos source,BlockPos target){return false;}
    }
    private static final Bridge BRIDGE=ServiceLoader.load(Bridge.class,WorldPermissions.class.getClassLoader()).findFirst().orElse(null);
    public static boolean active(Level level) {return level instanceof ServerLevel server && BRIDGE!=null && BRIDGE.active(server);}
    /** No action given: strict, like every caller before actions existed. */
    public static boolean mayAct(Level level,Player player,BlockPos target) {return mayAct(level,player,target,Action.ANY);}
    public static boolean mayAct(Level level,Player player,BlockPos target,Action action) {
        return !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)
                || !active(level) || BRIDGE.allows(serverPlayer,serverLevel,target.immutable(),action);
    }
    /** Checks every occupied chunk, with a hard bound only while providers are installed. */
    public static boolean mayOccupy(Level level,Player player,AABB box) {return mayOccupy(level,player,box,Action.ANY);}
    public static boolean mayOccupy(Level level,Player player,AABB box,Action action) {
        if(!active(level))return true;
        int minX=net.minecraft.util.Mth.floor(box.minX)>>4,maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4;
        int minZ=net.minecraft.util.Mth.floor(box.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;
        if((long)(maxX-minX+1)*(maxZ-minZ+1)>256)return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!mayAct(level,player,new BlockPos(x*16,net.minecraft.util.Mth.floor(box.minY),z*16),action))return false;
        // Claims kleiner als ein Chunk: auch die wirklichen Eck- und Mittelpunkte der Box fragen
        int y=net.minecraft.util.Mth.floor(box.minY);
        for(var point:footprintPoints(box))if(!mayAct(level,player,new BlockPos(point[0],y,point[1]),action))return false;
        return true;
    }
    /** Eck- und Mittelpunkte (Blockspalten) der Box; die Chunk-Ecken fragt der Aufrufer weiterhin selbst. */
    private static int[][] footprintPoints(AABB box){
        int x0=net.minecraft.util.Mth.floor(box.minX),x1=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX));
        int z0=net.minecraft.util.Mth.floor(box.minZ),z1=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ));
        return new int[][]{{x0,z0},{x1,z0},{x0,z1},{x1,z1},{net.minecraft.util.Mth.floor(box.getCenter().x),net.minecraft.util.Mth.floor(box.getCenter().z)}};
    }
    public static boolean mayAffectEntity(Player player,Entity target) {return mayOccupy(target.level(),player,target.getBoundingBox(),Action.ENTITY);}
    /** Checks both machine and target footprints before any transfer, fuel use or destruction. */
    public static boolean mayAutomate(Level level,BlockPos source,BlockPos target){
        if(!active(level))return true;
        var server=(ServerLevel)level;
        for(var pos:List.of(source,target)){
            if(!BRIDGE.allowsAutomation(server,source,pos))return false;
            var state=level.getBlockState(pos);
            if(state.hasProperty(BedBlock.PART)){
                var direction=state.getValue(BedBlock.FACING);
                if(state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD)direction=direction.getOpposite();
                if(!BRIDGE.allowsAutomation(server,source,pos.relative(direction)))return false;
            }
            if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                    &&!BRIDGE.allowsAutomation(server,source,ChestBlock.getConnectedBlockPos(pos,state)))return false;
        }
        return true;
    }
    /** Includes the space between the machine and the entity, bounded to 256 chunks. */
    public static boolean mayAutomateEntity(Level level,BlockPos source,Entity entity){
        if(!active(level))return true;
        var box=entity.getBoundingBox().minmax(new AABB(source));
        int minX=net.minecraft.util.Mth.floor(box.minX)>>4,maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4;
        int minZ=net.minecraft.util.Mth.floor(box.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;
        if((long)(maxX-minX+1)*(maxZ-minZ+1)>256)return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!BRIDGE.allowsAutomation((ServerLevel)level,source,new BlockPos(x*16,source.getY(),z*16)))return false;
        for(var point:footprintPoints(box))if(!BRIDGE.allowsAutomation((ServerLevel)level,source,new BlockPos(point[0],source.getY(),point[1])))return false;
        for(var point:footprintPoints(entity.getBoundingBox()))if(!BRIDGE.allowsAutomation((ServerLevel)level,source,new BlockPos(point[0],source.getY(),point[1])))return false;
        return true;
    }
    public static boolean mayTeleport(ServerPlayer player,ServerLevel targetLevel,Vec3 target) {
        return mayOccupy(player.level(),player,player.getBoundingBox())&&mayOccupy(targetLevel,player,player.getBoundingBox().move(target.subtract(player.position())));
    }
    /** Extra cells changed by placement callbacks, before any block or inventory mutation. */
    public static boolean mayPlace(Level level,Player player,BlockPos pos,BlockState state) {
        if(!active(level))return true;
        if(!mayAct(level,player,pos,Action.BUILD))return false;
        if(state.hasProperty(BedBlock.PART)){
            var direction=state.getValue(BedBlock.FACING);
            if(state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD)direction=direction.getOpposite();
            if(!mayAct(level,player,pos.relative(direction),Action.BUILD))return false;
        }
        if(state.getBlock() instanceof ChestBlock)for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var neighbor=pos.relative(direction);if(level.getBlockState(neighbor).getBlock() instanceof ChestBlock&&!mayAct(level,player,neighbor,Action.BUILD))return false;}
        return true;
    }
    public static boolean mayChange(Level level,Player player,BlockPos pos) {return mayChange(level,player,pos,Action.ANY);}
    public static boolean mayChange(Level level,Player player,BlockPos pos,Action action) {
        if(!active(level))return true;
        if(!mayAct(level,player,pos,action))return false;
        var state=level.getBlockState(pos);
        if(state.hasProperty(BedBlock.PART)){var direction=state.getValue(BedBlock.FACING);if(state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD)direction=direction.getOpposite();return mayAct(level,player,pos.relative(direction),action);}
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE)return mayAct(level,player,ChestBlock.getConnectedBlockPos(pos,state),action);
        return true;
    }
    private WorldPermissions() {}
}
