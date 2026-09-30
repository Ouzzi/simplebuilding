package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.portal.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;
public final class PortalActivation {
 public static InteractionResult ignite(ServerPlayer player,InteractionHand hand,BlockHitResult hit){
  var level=player.level();var runtime=DimensionRuntime.get(level.getServer());
  var item=player.getItemInHand(hand);if(!(item.is(Items.FLINT_AND_STEEL)||item.is(Items.FIRE_CHARGE))||player.isSpectator()||!runtime.settings.accessEnabled)return InteractionResult.PASS;
  var pos=hit.getBlockPos().relative(hit.getDirection());
  if(player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>36||!level.mayInteract(player,pos))return InteractionResult.PASS;
  for(var c:runtime.configs){
   var dim=level.dimension().identifier().toString();boolean origin=dim.equals(c.sourceDimensionId)||c.openFromDimensions.contains(dim);
   if(!c.enabled||!(origin&&c.allowIgniteFromSource||dim.equals(c.targetDimensionId)&&c.allowIgniteFromTarget))continue;
   var view=new PortalWorld(level,c);var found=PortalActivationService.match(view,c,pos.getX(),pos.getY(),pos.getZ());if(found.isEmpty())continue;
   var shape=found.get();if(c.requireSeparateLight&&!view.separateLight(shape)){DimensionRuntime.signal(level,pos,false);return InteractionResult.FAIL;}
   if(!runtime.reserve(level,shape.anchor())){DimensionRuntime.signal(level,pos,false);return InteractionResult.FAIL;}
   for(var cell:shape.interior())if(!level.mayInteract(player,new BlockPos(cell.x(),cell.y(),cell.z()))||!runtime.permitted(player,level,new BlockPos(cell.x(),cell.y(),cell.z())))return InteractionResult.FAIL;
   // Entire plan is checked before the first mutation; never consume the igniter on rejection.
   for(var cell:shape.interior()){
    var p=new BlockPos(cell.x(),cell.y(),cell.z());if(DimensionRegistry.portal(level.getBlockState(p)))continue;
    level.setBlock(p,DimensionRegistry.PORTAL.defaultBlockState().setValue(SkyPortalBlock.AXIS,shape.axis()==PortalAxis.X?Direction.Axis.X:Direction.Axis.Z),3);
    if(level.getBlockEntity(p) instanceof SkyPortalBlockEntity be){be.configure(c.portalColorRgb(),origin?c.targetDimensionId:c.sourceDimensionId);be.define(c.id,new BlockPos(shape.anchor().x(),shape.anchor().y(),shape.anchor().z()),false);}
   }
   runtime.recordPortal(level,shape.anchor());
   if(!player.hasInfiniteMaterials()) {if(item.is(Items.FIRE_CHARGE))item.shrink(1);else item.hurtAndBreak(1,player,hand);}
   DimensionRuntime.signal(level,pos,true);return InteractionResult.SUCCESS;
  }
  return InteractionResult.PASS;
 }
}
