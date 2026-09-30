package com.simpleriding;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
public final class RidingEffects {
 public static final Identifier SPEED=Riding.id("tailwind_boost"), JUMP=Riding.id("leaping_boost");
 public static int level(LivingEntity e,ItemStack stack,ResourceKey<Enchantment> key){return Math.min(3,Math.max(0,EnchantmentHelper.getItemEnchantmentLevel(e.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),stack)));}
 public static void tick(LivingEntity e){
  if(e.level().isClientSide())return;
  boolean ridden=RidingSecurity.supported(e) && e.isAlive() && e.getControllingPassenger() instanceof Player && (!(e instanceof AbstractNautilus)||Riding.CONFIG.safety.enableNautilus);
  ItemStack saddle=e.getItemBySlot(EquipmentSlot.SADDLE);
  boolean ghast=e.getType()==EntityTypes.HAPPY_GHAST;
  if(ghast)saddle=e.getItemBySlot(EquipmentSlot.BODY);
  int speed=ridden&&Riding.CONFIG.safety.enableTailwind&&saddle.is(Riding.SADDLE)?level(e,saddle,Riding.TAILWIND):0;
  var c=Riding.CONFIG.enchantments.swiftRide;
  float per=e instanceof AbstractNautilus?c.nautilusSpeedMultiplier:ghast?c.ghastSpeedMultiplier:e instanceof AbstractHorse?c.horseSpeedMultiplier:c.otherSpeedMultiplier;
  double boost=Math.min(RidingConfig.bounded(Riding.CONFIG.safety.maximumSpeedBonus,0,3),speed*RidingConfig.bounded(per,0,RidingConfig.MAX_SPEED_PER_LEVEL));
  apply(e,Attributes.MOVEMENT_SPEED,SPEED,boost);
  // Happy Ghast uses flying speed in both ridden input and acceleration. Square-root
  // scaling keeps the resulting flight amplification within the server's total cap.
  apply(e,Attributes.FLYING_SPEED,SPEED,ghast?Math.sqrt(1+boost)-1:boost);
  int jump=ridden&&Riding.CONFIG.safety.enableLeaping&&e instanceof AbstractHorse&&e.getItemBySlot(EquipmentSlot.BODY).is(Riding.ARMOR)?level(e,e.getItemBySlot(EquipmentSlot.BODY),Riding.LEAPING):0;
  apply(e,Attributes.JUMP_STRENGTH,JUMP,Math.min(RidingConfig.bounded(Riding.CONFIG.safety.maximumJumpBonus,0,1.5f),jump*RidingConfig.bounded(Riding.CONFIG.enchantments.horseJump.jumpStrengthMultiplier,0,RidingConfig.MAX_JUMP_PER_LEVEL)));
 }
 public static float dashScale(LivingEntity e,float claimed){
  float scale=RidingConfig.bounded(claimed,0,1);
  if(e.level().isClientSide()||!Riding.CONFIG.safety.enableNautilus||!Riding.CONFIG.safety.enableLeaping||!(e.getControllingPassenger() instanceof Player))return scale;
  var armor=e.getItemBySlot(EquipmentSlot.BODY);
  int n=armor.is(Riding.NAUTILUS_ARMOR)?level(e,armor,Riding.LEAPING):0;
  double bonus=Math.min(RidingConfig.bounded(Riding.CONFIG.safety.maximumJumpBonus,0,1.5f),n*RidingConfig.bounded(Riding.CONFIG.enchantments.horseJump.nautilusDashMultiplier,0,.5f));
  var speed=e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED);
  double tailwind=speed==null?0:Math.max(0,speed.amount());
  double factor=Math.min(1+bonus,(1+RidingConfig.bounded(Riding.CONFIG.safety.maximumSpeedBonus,0,3))/(1+tailwind));
  return (float)(scale*Math.max(1,factor));
 }
 private static void apply(LivingEntity e,Holder<Attribute> key,Identifier id,double boost){
  var a=e.getAttribute(key); if(a==null)return; if(boost<=0){a.removeModifier(id);return;}
  var old=a.getModifier(id); if(old==null||Math.abs(old.amount()-boost)>1e-6)a.addOrUpdateTransientModifier(new AttributeModifier(id,boost,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
 }
}
