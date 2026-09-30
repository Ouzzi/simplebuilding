package com.simpleriding;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
public final class RidingEffects {
 public static final Identifier SPEED=Riding.id("tailwind_boost"), JUMP=Riding.id("leaping_boost");
 public static int level(LivingEntity e,ItemStack stack,ResourceKey<Enchantment> key){return Math.min(3,Math.max(0,EnchantmentHelper.getItemEnchantmentLevel(e.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),stack)));}
 public static void tick(LivingEntity e){
  if(e.level().isClientSide())return;
  boolean ridden=e.getControllingPassenger() instanceof Player;
  ItemStack saddle=e.getItemBySlot(EquipmentSlot.SADDLE);
  boolean ghast=e.getType()==EntityTypes.HAPPY_GHAST;
  if(ghast)saddle=e.getItemBySlot(EquipmentSlot.BODY);
  int speed=ridden&&saddle.is(Riding.SADDLE)?level(e,saddle,Riding.TAILWIND):0;
  var c=Riding.CONFIG.enchantments.swiftRide;
  float per=ghast?c.ghastSpeedMultiplier:e instanceof AbstractHorse?c.horseSpeedMultiplier:c.otherSpeedMultiplier;
  double boost=speed*RidingConfig.bounded(per,0,RidingConfig.MAX_SPEED_PER_LEVEL);
  apply(e,Attributes.MOVEMENT_SPEED,SPEED,boost); apply(e,Attributes.FLYING_SPEED,SPEED,boost);
  int jump=ridden&&e instanceof AbstractHorse?level(e,e.getItemBySlot(EquipmentSlot.BODY),Riding.LEAPING):0;
  apply(e,Attributes.JUMP_STRENGTH,JUMP,jump*RidingConfig.bounded(Riding.CONFIG.enchantments.horseJump.jumpStrengthMultiplier,0,RidingConfig.MAX_JUMP_PER_LEVEL));
 }
 private static void apply(LivingEntity e,Holder<Attribute> key,Identifier id,double boost){
  var a=e.getAttribute(key); if(a==null)return; if(boost<=0){a.removeModifier(id);return;}
  var old=a.getModifier(id); if(old==null||Math.abs(old.amount()-boost)>1e-6)a.addOrUpdateTransientModifier(new AttributeModifier(id,boost,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
 }
}
