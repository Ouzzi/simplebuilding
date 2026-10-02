package com.simpleriding;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.Vec3;

/**
 * R1 horseshoes: tiers, registration and all server-authoritative rules (points, effects, wear,
 * mending, fall damage). Clients only read the synced code written by {@link #syncedValue}.
 */
public final class Horseshoes {
 private Horseshoes() {}
 /** Tier order is the owner's ranking; ordinal+1 is the stored tier (0 = no shoe). */
 public enum Tier {
  COPPER("copper",120,8,ItemTags.REPAIRS_COPPER_ARMOR,false),
  IRON("iron",180,9,ItemTags.REPAIRS_IRON_ARMOR,false),
  GOLDEN("golden",80,22,ItemTags.REPAIRS_GOLD_ARMOR,false),
  DIAMOND("diamond",400,10,ItemTags.REPAIRS_DIAMOND_ARMOR,false),
  NETHERITE("netherite",500,15,ItemTags.REPAIRS_NETHERITE_ARMOR,true),
  ENDERITE("enderite",620,15,TagKey.create(Registries.ITEM,Riding.id("repairs_enderite_horseshoe")),true);
  public final String material; public final int durability, enchantability; public final TagKey<Item> repair; public final boolean fireResistant;
  Tier(String material,int durability,int enchantability,TagKey<Item> repair,boolean fireResistant){this.material=material;this.durability=durability;this.enchantability=enchantability;this.repair=repair;this.fireResistant=fireResistant;}
  public String itemName(){return material+"_horseshoe";}
  /** 2^(tier-1): two shoes of the next tier score exactly like four of the tier below. */
  public int points(){return 1<<ordinal();}
 }
 public static final int SLOTS=4, MAX_POINTS=SLOTS*Tier.ENDERITE.points();
 /** Slot order: front left, front right, hind left, hind right. */
 public static final String[] SLOT_NAMES={"front_left","front_right","hind_left","hind_right"};
 public static final TagKey<EntityType<?>> CAN_WEAR=TagKey.create(Registries.ENTITY_TYPE,Riding.id("can_wear_horseshoes"));
 public static final TagKey<Item> TAG=TagKey.create(Registries.ITEM,Riding.id("horseshoes"));
 public static final Identifier TERRAIN=Riding.id("horseshoe_terrain"), SPEED=Riding.id("horseshoe_speed"), JUMP=Riding.id("horseshoe_jump");
 public static final Identifier EMPTY_SLOT_ICON=Riding.id("container/slot/horseshoe");
 public static final Map<Tier,Item> ITEMS=new EnumMap<>(Tier.class);
 private static final Map<Item,Tier> BY_ITEM=new HashMap<>();
 public static Item TEMPLATE;

 private static ResourceKey<Item> key(String name){return ResourceKey.create(Registries.ITEM,Riding.id(name));}
 private static net.minecraft.network.chat.MutableComponent line(String key){return Component.translatable("item.simpleriding.horseshoe_smithing_template."+key);}
 /** Registers the template and the tiers; the Enderite tier only exists alongside SimpleBuilding. */
 public static void register(boolean simplebuilding){
  if(TEMPLATE!=null)return;
  var tk=key("horseshoe_smithing_template");
  TEMPLATE=Registry.register(BuiltInRegistries.ITEM,tk,new SmithingTemplateItem(
   line("applies_to").withStyle(ChatFormatting.BLUE),line("ingredients").withStyle(ChatFormatting.BLUE),
   line("base_slot_description"),line("additions_slot_description"),
   List.of(Identifier.withDefaultNamespace("container/slot/ingot"),Identifier.withDefaultNamespace("container/slot/diamond"),EMPTY_SLOT_ICON),
   List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
   new Item.Properties().setId(tk).rarity(Rarity.UNCOMMON)));
  for(Tier t:Tier.values()){
   if(t==Tier.ENDERITE&&!simplebuilding)continue;
   var k=key(t.itemName());
   var p=new Item.Properties().setId(k).durability(t.durability).enchantable(t.enchantability).repairable(t.repair);
   if(t.fireResistant)p.fireResistant();
   if(t==Tier.ENDERITE)p.rarity(Rarity.EPIC);
   Item item=Registry.register(BuiltInRegistries.ITEM,k,new Item(p));
   ITEMS.put(t,item);BY_ITEM.put(item,t);
  }
 }
 public static Optional<Tier> tier(ItemStack stack){return Optional.ofNullable(stack.isEmpty()?null:BY_ITEM.get(stack.getItem()));}
 public static boolean isHorseshoe(ItemStack stack){return tier(stack).isPresent();}
 public static boolean canWear(Entity e){return e instanceof AbstractHorse&&e.is(CAN_WEAR);}
 public static SimpleContainer container(AbstractHorse horse){return ((HorseshoeHolder)horse).simpleriding$horseshoes();}

 // ---- synced code: bits 0..11 = 4 x 3-bit tier, bits 12..21 = handling share in permille ----
 public static int encode(SimpleContainer c){int code=0;for(int i=0;i<SLOTS;i++){var t=tier(c.getItem(i));if(t.isPresent())code|=(t.get().ordinal()+1)<<(3*i);}return code;}
 public static int tierAt(int code,int slot){return (code>>(3*slot))&7;}
 public static int shoes(int code){return code&0xFFF;}
 public static double handlingShare(int code){return Math.min(1000,(code>>>12)&0x3FF)/1000.0;}
 public static int points(int code){int p=0;for(int i=0;i<SLOTS;i++){int t=tierAt(code,i);if(t>0)p+=1<<(t-1);}return p;}
 public static boolean fullSet(int code){for(int i=0;i<SLOTS;i++)if(tierAt(code,i)==0)return false;return true;}
 /** Diminishing returns 0..1 (full Enderite set = 1); equal points always give equal effects. */
 public static double effect(int points){return points<=0?0:Math.min(1,Math.log1p(points)/Math.log1p(MAX_POINTS));}
 public static int code(Entity e){return e instanceof HorseshoeHolder h?h.simpleriding$code():0;}
 /** Server value for the synced code; the handling share comes from the server config only. */
 public static int syncedValue(AbstractHorse horse){
  int code=encode(container(horse));
  var c=Riding.CONFIG.horseshoes;
  double share=c.enableHorseshoes?RidingConfig.bounded(c.handlingBonus,0,RidingConfig.MAX_HANDLING)*effect(points(code)):0;
  return code|((int)Math.round(Math.max(0,Math.min(1,share))*1000)<<12);
 }

 /** Steering input (client and server): more sideways/backward control, never more forward speed. */
 public static Vec3 handling(AbstractHorse horse,Vec3 input){
  if(!canWear(horse))return input;
  double share=handlingShare(code(horse));
  if(share<=0||!Double.isFinite(input.x)||!Double.isFinite(input.z))return input;
  double sideways=(0.5+share)/0.5, backward=(0.25+share/2)/0.25;
  return new Vec3(input.x*sideways,input.y,input.z<0?input.z*backward:input.z);
 }

 /** Attribute bonuses for a player-ridden, shod horse; caps share the existing Tailwind/Leaping totals. */
 public static void effects(LivingEntity e,boolean ridden,double tailwind,double leaping){
  if(!(e instanceof AbstractHorse horse))return;
  var c=Riding.CONFIG.horseshoes; int code=canWear(horse)?shoes(code(horse)):0;
  boolean on=ridden&&c.enableHorseshoes&&code!=0;
  double f=effect(points(code));
  RidingEffects.apply(e,Attributes.MOVEMENT_EFFICIENCY,TERRAIN,on?RidingConfig.bounded(c.terrainBonus,0,1)*f:0,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
  boolean full=on&&fullSet(code);
  double speedRoom=Math.max(0,RidingConfig.bounded(Riding.CONFIG.safety.maximumSpeedBonus,0,3)-tailwind);
  double jumpRoom=Math.max(0,RidingConfig.bounded(Riding.CONFIG.safety.maximumJumpBonus,0,1.5f)-leaping);
  RidingEffects.apply(e,Attributes.MOVEMENT_SPEED,SPEED,full?Math.min(speedRoom,RidingConfig.bounded(c.fullSetSpeedBonus,0,RidingConfig.MAX_FULL_SET)):0,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
  RidingEffects.apply(e,Attributes.JUMP_STRENGTH,JUMP,full?Math.min(jumpRoom,RidingConfig.bounded(c.fullSetJumpBonus,0,RidingConfig.MAX_FULL_SET)):0,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
 }

 /** Fall damage of a horse wearing all four shoes: slightly more, rounded so short falls stay equal. */
 public static int fallDamage(LivingEntity e,int damage){
  var c=Riding.CONFIG.horseshoes;
  if(damage<=0||!c.enableHorseshoes||!canWear(e)||!fullSet(shoes(code(e))))return damage;
  return Math.max(damage,(int)Math.round(damage*(1+RidingConfig.bounded(c.fullSetFallDamageIncrease,0,RidingConfig.MAX_FALL_INCREASE))));
 }

 /** Wear while player-ridden on the ground: 1 durability per shoe every blocksPerDurability blocks. */
 public static void wear(AbstractHorse horse,double distance){
  if(!(horse.level() instanceof ServerLevel level)||!Double.isFinite(distance)||distance<=0)return;
  var holder=(HorseshoeHolder)horse;
  double total=holder.simpleriding$travel()+Math.min(distance,4096); // callers drop teleports (tick: > 4 blocks)
  int per=Math.max(RidingConfig.MIN_BLOCKS_PER_DURABILITY,Math.min(RidingConfig.MAX_BLOCKS_PER_DURABILITY,Riding.CONFIG.horseshoes.blocksPerDurability));
  var shoes=container(horse);boolean changed=false;
  while(total>=per){
   total-=per;
   for(int i=0;i<SLOTS;i++){
    var stack=shoes.getItem(i);if(stack.isEmpty()||!stack.isDamageableItem())continue;
    stack.hurtAndBreak(1,level,null,broken->level.playSound(null,horse.getX(),horse.getY(),horse.getZ(),SoundEvents.ITEM_BREAK.value(),SoundSource.NEUTRAL,0.8f,0.8f+level.getRandom().nextFloat()*0.4f));
    changed=true;
   }
  }
  holder.simpleriding$setTravel(total);
  if(changed)shoes.setChanged();
 }

 /** Per-tick server bookkeeping: travel distance for wear plus the synced code. */
 public static void tick(AbstractHorse horse,boolean ridden){
  var holder=(HorseshoeHolder)horse;
  double x=horse.getX(),z=horse.getZ();
  if(ridden&&holder.simpleriding$hasLast()&&horse.onGround()&&!horse.isInWater()&&Riding.CONFIG.horseshoes.enableHorseshoes){
   double dx=x-holder.simpleriding$lastX(),dz=z-holder.simpleriding$lastZ();double d=Math.sqrt(dx*dx+dz*dz);
   if(d<=4)wear(horse,d); // larger jumps are teleports, not riding
  }
  holder.simpleriding$setLast(x,z,ridden);
  holder.simpleriding$sync();
 }

 /** XP left over after Vanilla Mending also mends the ridden horse's Mending horseshoes. */
 public static int mend(ServerPlayer player,int amount){
  if(amount<=0||!(player.getVehicle() instanceof AbstractHorse horse)||!canWear(horse))return amount;
  var shoes=container(horse);var level=player.level();
  for(int pass=0;pass<SLOTS&&amount>0;pass++){
   ItemStack target=ItemStack.EMPTY;
   for(int i=0;i<SLOTS;i++){var s=shoes.getItem(i);if(s.isDamaged()&&EnchantmentHelper.has(s,EnchantmentEffectComponents.REPAIR_WITH_XP)){target=s;break;}}
   if(target.isEmpty())break;
   int durability=EnchantmentHelper.modifyDurabilityToRepairFromXp(level,target,amount);
   if(durability<=0)break;
   int repair=Math.min(durability,target.getDamageValue());
   target.setDamageValue(target.getDamageValue()-repair);
   amount=repair>0?amount-repair*amount/durability:amount;
   if(repair<=0)break;
  }
  shoes.setChanged();
  return Math.max(0,amount);
 }

 /** Drops all shoes (Curse of Vanishing excluded, like Vanilla equipment). */
 public static void drop(AbstractHorse horse,ServerLevel level){
  var shoes=container(horse);
  for(int i=0;i<SLOTS;i++){
   var s=shoes.removeItemNoUpdate(i);
   if(!s.isEmpty()&&!EnchantmentHelper.has(s,EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))horse.spawnAtLocation(level,s);
  }
  shoes.setChanged();
 }

 /** Four-slot container that only accepts single horseshoes and reports changes to its owner. */
 public static final class Container extends SimpleContainer {
  private final Runnable changed;
  public Container(Runnable changed){super(SLOTS);this.changed=changed;}
  @Override public int getMaxStackSize(){return 1;}
  @Override public boolean canPlaceItem(int slot,ItemStack stack){return isHorseshoe(stack);}
  @Override public void setChanged(){changed.run();}
 }
}
