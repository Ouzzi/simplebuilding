package com.simpleriding.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class) public abstract class RidingFallMixin {
 @Inject(method="calculateFallDamage",at=@At("RETURN"),cancellable=true)
 private void simpleriding$feather(double distance,float multiplier,CallbackInfoReturnable<Integer> cir){
  var entity=(LivingEntity)(Object)this;
  if(entity.level().isClientSide()||!com.simpleriding.Riding.CONFIG.safety.enableArmorUtilities||!(entity instanceof AbstractHorse)||!com.simpleriding.Riding.armor(entity.getItemBySlot(EquipmentSlot.BODY)))return;
  int level=Math.min(4,Math.max(0,EnchantmentHelper.getItemEnchantmentLevel(entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FEATHER_FALLING),entity.getItemBySlot(EquipmentSlot.BODY))));
  if(level>0)cir.setReturnValue(Math.max(0,(int)(cir.getReturnValue()*(1f-com.simpleriding.RidingConfig.bounded(com.simpleriding.Riding.CONFIG.enchantments.horseJump.featherFallingReduction,0,.12f)*level))));
 }
}
