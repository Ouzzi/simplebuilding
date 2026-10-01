package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=net.minecraft.world.level.block.entity.HopperBlockEntity.class,priority=1100)
public abstract class ClaimHopperMixin {
 @Inject(method="ejectItems",at=@At("HEAD"),cancellable=true)
 private static void claims$eject(Level level,BlockPos pos,net.minecraft.world.level.block.entity.HopperBlockEntity hopper,CallbackInfoReturnable<Boolean> cir){if(level instanceof ServerLevel server&&!Claims.transfer(server,pos,pos.relative(hopper.getBlockState().getValue(net.minecraft.world.level.block.HopperBlock.FACING))))cir.setReturnValue(false);}
 @Inject(method="suckInItems",at=@At("HEAD"),cancellable=true)
 private static void claims$intake(Level level,net.minecraft.world.level.block.entity.Hopper hopper,CallbackInfoReturnable<Boolean> cir){var pos=BlockPos.containing(hopper.getLevelX(),hopper.getLevelY(),hopper.getLevelZ());if(level instanceof ServerLevel server&&!Claims.transfer(server,pos,pos.above()))cir.setReturnValue(false);}
 @Unique private static final ThreadLocal<net.minecraft.world.entity.item.ItemEntity> claims$drop=new ThreadLocal<>();
 @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z")
 private static boolean claims$pickup(net.minecraft.world.Container into,net.minecraft.world.entity.item.ItemEntity entity,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
  if(!Claims.anyEnabled())return original.call(into,entity);
  if(!com.simplebuilding.modules.simpletweaks.claims.ClaimAutomation.transfer(entity,into))return false;
  var previous=claims$drop.get();claims$drop.set(entity);try{return original.call(into,entity);}finally{if(previous==null)claims$drop.remove();else claims$drop.set(previous);}
 }
 @Inject(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true)
 private static void claims$transfer(net.minecraft.world.Container from,net.minecraft.world.Container into,net.minecraft.world.item.ItemStack stack,Direction direction,CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir){
  if(Claims.anyEnabled()&&!com.simplebuilding.modules.simpletweaks.claims.ClaimAutomation.transfer(from==null?claims$drop.get():from,into))cir.setReturnValue(stack);
 }
}
