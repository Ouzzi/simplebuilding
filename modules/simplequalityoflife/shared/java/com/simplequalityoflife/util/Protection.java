package com.simplequalityoflife.util;
public final class Protection {
 public static boolean farmland(net.minecraft.world.entity.Entity e){if(!com.simplequalityoflife.Simplequalityoflife.getConfig().qOL.preventFarmlandTrampleWithFeatherFalling||!(e instanceof net.minecraft.world.entity.LivingEntity l))return false;
  var h=e.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING);
  return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h,l.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET))>0;
 }
}
