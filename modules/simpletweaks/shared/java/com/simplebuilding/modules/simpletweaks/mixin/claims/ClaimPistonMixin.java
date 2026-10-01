package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=net.minecraft.world.level.block.piston.PistonBaseBlock.class,priority=1100)
public abstract class ClaimPistonMixin {
 private static boolean claims$allowed(Level level,BlockPos pos,Direction direction,boolean extending){
  if(!(level instanceof ServerLevel server)||!Claims.enabled(server.getServer()))return true;
  if(!Claims.transfer(server,pos,pos.relative(direction)))return false;
  var resolver=new net.minecraft.world.level.block.piston.PistonStructureResolver(level,pos,direction,extending);
  var context=com.simplebuilding.modules.simpletweaks.claims.ClaimAutomation.RETRACTION_HEAD;
  var previous=context.get();
  boolean resolved;
  try {
   if(!extending&&level.getBlockState(pos.relative(direction)).getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock)context.set(pos.relative(direction));
   else context.remove();
   resolved=resolver.resolve();
  } finally {if(previous==null)context.remove();else context.set(previous);}
  if(!resolved)return true;
  var move=extending?direction:direction.getOpposite();
  for(var at:resolver.getToPush())if(!Claims.transfer(server,pos,at)||!Claims.transfer(server,pos,at.relative(move)))return false;
  for(var at:resolver.getToDestroy())if(!Claims.transfer(server,pos,at))return false;
  return true;
 }
 @Inject(method="triggerEvent",at=@At("HEAD"),cancellable=true)
 private void claims$event(BlockState state,Level level,BlockPos pos,int event,int data,CallbackInfoReturnable<Boolean> cir){if(!claims$allowed(level,pos,state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING),event==0))cir.setReturnValue(false);}
 @Inject(method="moveBlocks",at=@At("HEAD"),cancellable=true)
 private void claims$move(Level level,BlockPos pos,Direction direction,boolean extending,CallbackInfoReturnable<Boolean> cir){if(!claims$allowed(level,pos,direction,extending))cir.setReturnValue(false);}
}
