package com.simplebuilding.modules.simpletweaks.claims;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimCompoundAccessor;
/** Automation belongs to the land, never to a client-supplied item owner. */
public final class ClaimAutomation {
 /** Scoped resolver-only view: Vanilla removes the head before resolving a retraction. */
 public static final ThreadLocal<BlockPos> RETRACTION_HEAD=new ThreadLocal<>();
 private record Location(ServerLevel level,BlockPos pos){}
 private static boolean locations(Object container,int depth,List<Location> result){
  if(depth>8||result.size()>=256)return false;
  if(container instanceof ClaimCompoundAccessor doubleChest)return locations(doubleChest.claims$first(),depth+1,result)&&locations(doubleChest.claims$second(),depth+1,result);
  if(container instanceof BlockEntity block && block.getLevel() instanceof ServerLevel level){result.add(new Location(level,block.getBlockPos()));return true;}
  if(container instanceof Entity entity && entity.level() instanceof ServerLevel level){var box=entity.getBoundingBox();int minX=net.minecraft.util.Mth.floor(box.minX)>>4,maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4,minZ=net.minecraft.util.Mth.floor(box.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;if((long)(maxX-minX+1)*(maxZ-minZ+1)>256-result.size())return false;for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)result.add(new Location(level,new BlockPos(x*16,entity.blockPosition().getY(),z*16)));return true;}
  return false;
 }
 public static boolean transfer(Object source,Object target){
  if(!Claims.anyEnabled())return true;
  var locations=new ArrayList<Location>();
  if(!locations(source,0,locations)||!locations(target,0,locations)||locations.isEmpty())return false; // Unknown automation is never authority.
  var origin=locations.getFirst();
  for(var location:locations)if(origin.level()!=location.level()||!Claims.transfer(origin.level(),origin.pos(),location.pos()))return false;
  return true;
 }
 /** Mobile automation has no trusted owner: neither endpoint may touch a claim. */
 public static boolean unowned(Object source,Object target){
  if(!Claims.anyEnabled())return true;
  var positions=new ArrayList<Location>();
  if(!locations(source,0,positions)||!locations(target,0,positions)||positions.isEmpty())return false;
  for(var location:positions)if(!Claims.environment(location.level(),location.pos()))return false;
  return true;
 }
 private ClaimAutomation(){}
}
