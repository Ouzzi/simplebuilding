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
 /** Block-backed wrappers without a block entity (composter): position recorded where the hopper resolved them. */
 private static final Map<Object,Location> WORLDLY=Collections.synchronizedMap(new WeakHashMap<>());
 public static void resolved(Object container,ServerLevel level,BlockPos pos){if(container!=null&&Claims.anyEnabled())WORLDLY.put(container,new Location(level,pos.immutable()));}
 private static boolean locations(Object container,int depth,List<Location> result){
  if(depth>8||result.size()>=256)return false;
  var known=container==null?null:WORLDLY.get(container);
  if(known!=null){result.add(known);return true;}
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
 private static final String ORIGIN="simpletweaks.claim_origin:";
 /** Single land owner of a whole container footprint: empty = wilderness, null = mixed, unknown or locked. */
 private static Optional<UUID> land(Object container){
  var positions=new ArrayList<Location>();
  if(!locations(container,0,positions)||positions.isEmpty())return null;
  Optional<UUID> result=null;
  for(var location:positions){var owner=Claims.owner(location.level(),location.pos());if(owner==null||result!=null&&!result.equals(owner))return null;result=owner;}
  return result;
 }
 /**
  * Mobile carriers (copper golems) have no trusted owner, so carried items remember the land they came from
  * as a persistent entity tag. Items may only be put into containers on that same land, so no golem can move
  * an owner's items off a claim or into a stranger's claim. Untagged loads predate protection and may only be
  * put down on the land the carrier currently stands on.
  */
 public static boolean carry(Entity body,Object container,boolean pickup){
  if(!Claims.anyEnabled())return true;
  var land=land(container);if(land==null)return false;
  String tag=ORIGIN+land.map(UUID::toString).orElse("wild");
  var origin=body.entityTags().stream().filter(t->t.startsWith(ORIGIN)).findFirst();
  if(pickup){origin.ifPresent(body::removeTag);body.addTag(tag);return true;}
  if(origin.isPresent())return origin.get().equals(tag);
  if(!(body.level() instanceof ServerLevel level))return false;
  return land.equals(Claims.owner(level,body.blockPosition()));
 }
 private ClaimAutomation(){}
}
