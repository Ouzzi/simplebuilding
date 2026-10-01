package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.level.block.DispenserBlock.class)
public abstract class ClaimDispenserMixin {
 @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="dispenseFrom",at=@At(value="INVOKE",target="Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
 private net.minecraft.world.item.ItemStack claims$dispense(net.minecraft.core.dispenser.DispenseItemBehavior behavior,net.minecraft.core.dispenser.BlockSource source,net.minecraft.world.item.ItemStack stack,com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.item.ItemStack> original){
  var level=source.level();var pos=source.pos();
  if(!Claims.enabled(level.getServer()))return original.call(behavior,source,stack);
  if(!Claims.transfer(level,pos,pos.relative(source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING))))return stack;
  // Only the exact Vanilla plain-item ejection has a bounded, audited footprint.
  // Other registered behaviors remain unsupported near claims, including within one owner's land.
  if(behavior.getClass()!=net.minecraft.core.dispenser.DefaultDispenseItemBehavior.class)
   for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(!Claims.environment(level,pos.offset(x*16,0,z*16)))return stack;
  return original.call(behavior,source,stack);
 }
}
