package com.simpleriding.test;

import com.simpleriding.*;
import com.simpleriding.Horseshoes.Tier;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** R1 horseshoe game tests (registered through {@link RidingTests#ALL}). */
public final class HorseshoeTests {
 private HorseshoeTests() {}
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("horseshoe_items",HorseshoeTests::items); ALL.put("horseshoe_recipes",HorseshoeTests::recipes);
  ALL.put("horseshoe_points",HorseshoeTests::points); ALL.put("horseshoe_effects",HorseshoeTests::effects);
  ALL.put("horseshoe_handling",HorseshoeTests::handling); ALL.put("horseshoe_menu",HorseshoeTests::menu);
  ALL.put("horseshoe_save_and_drop",HorseshoeTests::saveAndDrop); ALL.put("horseshoe_wear",HorseshoeTests::wear);
  ALL.put("horseshoe_mending",HorseshoeTests::mending); ALL.put("horseshoe_fall",HorseshoeTests::fall);
  ALL.put("horseshoe_loot",HorseshoeTests::loot);
 }
 private static Item shoe(Tier t){return Horseshoes.ITEMS.get(t);}
 private static Holder.Reference<Enchantment> ench(GameTestHelper h,ResourceKey<Enchantment> key){return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key,int level){var s=new ItemStack(item);s.enchant(ench(h,key),level);return s;}
 private static void close(double a,double b,GameTestHelper h,String msg){h.assertTrue(Math.abs(a-b)<1e-5,msg+": "+a+" != "+b);}
 private static AbstractHorse shod(GameTestHelper h,Tier... tiers){
  var horse=h.spawn(EntityTypes.HORSE,2,2,2);horse.setTamed(true);
  for(int i=0;i<tiers.length;i++)if(tiers[i]!=null)Horseshoes.container(horse).setItem(i,new ItemStack(shoe(tiers[i])));
  return horse;
 }
 private static Player rider(GameTestHelper h,AbstractHorse horse){
  horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
  var p=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);p.startRiding(horse,true,false);return p;
 }
 private static double modifier(LivingEntity e,Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,Identifier id){var m=e.getAttribute(attribute).getModifier(id);return m==null?0:m.amount();}

 public static void items(GameTestHelper h){
  // Principle 5: Enderite shoes are registered only with SimpleBuilding.
  h.assertTrue(Horseshoes.ITEMS.size()==(Riding.SIMPLEBUILDING?6:5)&&Horseshoes.TEMPLATE!=null,"Every available tier and the template are registered");
  h.assertTrue(Horseshoes.ITEMS.containsKey(Tier.ENDERITE)==Riding.SIMPLEBUILDING,"Enderite tier follows SimpleBuilding");
  int[] durability={120,180,80,400,500,620};
  for(Tier t:Horseshoes.ITEMS.keySet()){
   var stack=new ItemStack(shoe(t));
   h.assertTrue(BuiltInRegistries.ITEM.getKey(shoe(t)).equals(Riding.id(t.itemName())),"Registry id "+t);
   h.assertTrue(stack.getMaxDamage()==durability[t.ordinal()]&&stack.getMaxStackSize()==1,"Durability and stack size "+t);
   h.assertTrue(stack.is(Horseshoes.TAG)&&stack.is(ItemTags.DURABILITY_ENCHANTABLE),"Horseshoe and durability tags "+t);
   h.assertTrue(ench(h,Enchantments.UNBREAKING).value().canEnchant(stack)&&ench(h,Enchantments.MENDING).value().canEnchant(stack),"Unbreaking and Mending apply "+t);
   h.assertTrue(!ench(h,Riding.TAILWIND).value().canEnchant(stack)&&!ench(h,Enchantments.PROTECTION).value().canEnchant(stack),"No unrelated enchantments "+t);
   h.assertTrue(stack.has(net.minecraft.core.component.DataComponents.ENCHANTABLE),"Enchanting table works "+t);
   boolean fireproof=stack.has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT);
   h.assertTrue(fireproof==(t==Tier.NETHERITE||t==Tier.ENDERITE),"Fire resistance only for Netherite/Enderite "+t);
  }
  h.assertTrue(new ItemStack(shoe(Tier.IRON)).isValidRepairItem(new ItemStack(Items.IRON_INGOT)),"Iron repairs iron horseshoes");
  if(Riding.SIMPLEBUILDING)h.assertTrue(new ItemStack(shoe(Tier.ENDERITE)).isValidRepairItem(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_ingot")))),"Enderite ingot repairs Enderite horseshoes");
  var menu=new AnvilMenu(0,h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE).getInventory());
  menu.getSlot(0).set(new ItemStack(shoe(Tier.DIAMOND)));menu.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(ench(h,Enchantments.MENDING),1)));menu.createResult();
  h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(ench(h,Enchantments.MENDING),menu.getSlot(2).getItem())==1,"Real anvil applies Mending");
  h.assertTrue(!EnchantmentHelper.selectEnchantment(net.minecraft.util.RandomSource.create(3),new ItemStack(shoe(Tier.IRON)),30,java.util.stream.Stream.of(ench(h,Enchantments.UNBREAKING))).isEmpty(),"Enchanting table offers Unbreaking");
  h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().filter(i->i.getNamespace().equals("simpleriding")).count()==(Riding.SIMPLEBUILDING?7:6),"Exactly the R1 items (Enderite only with SimpleBuilding)");
  h.succeed();
 }

 private static ItemStack smith(GameTestHelper h,String recipe,ItemStack base,ItemStack addition){
  var holder=h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Riding.id(recipe))).orElseThrow(()->new IllegalStateException("missing recipe "+recipe));
  @SuppressWarnings("unchecked") var r=(Recipe<SmithingRecipeInput>)holder.value();
  var input=new SmithingRecipeInput(new ItemStack(Horseshoes.TEMPLATE),base,addition);
  h.assertTrue(r.matches(input,h.getLevel()),"Recipe matches "+recipe);
  return r.assemble(input);
 }
 public static void recipes(GameTestHelper h){
  var materials=Map.of(Tier.COPPER,Items.COPPER_INGOT,Tier.IRON,Items.IRON_INGOT,Tier.GOLDEN,Items.GOLD_INGOT,Tier.DIAMOND,Items.DIAMOND);
  for(var e:materials.entrySet())h.assertTrue(smith(h,e.getKey().itemName()+"_smithing",new ItemStack(e.getValue()),new ItemStack(Items.IRON_NUGGET)).is(shoe(e.getKey())),"Smithing makes "+e.getKey());
  var diamond=new ItemStack(shoe(Tier.DIAMOND));diamond.enchant(ench(h,Enchantments.UNBREAKING),3);diamond.setDamageValue(7);
  var netherite=smith(h,"netherite_horseshoe_smithing",diamond,new ItemStack(Items.NETHERITE_INGOT));
  h.assertTrue(netherite.is(shoe(Tier.NETHERITE))&&EnchantmentHelper.getItemEnchantmentLevel(ench(h,Enchantments.UNBREAKING),netherite)==3&&netherite.getDamageValue()==7,"Netherite upgrade keeps enchantments and wear");
  if(Riding.SIMPLEBUILDING){
  var enderite=smith(h,"enderite_horseshoe_smithing",netherite,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_ingot"))));
  h.assertTrue(enderite.is(shoe(Tier.ENDERITE))&&EnchantmentHelper.getItemEnchantmentLevel(ench(h,Enchantments.UNBREAKING),enderite)==3,"Enderite upgrade with SimpleBuilding");
  }else h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Riding.id("enderite_horseshoe_smithing"))).isEmpty(),"Without SimpleBuilding the Enderite recipe stays unloaded");
  var wrong=new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_NUGGET));
  h.assertTrue(h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,wrong,h.getLevel()).isEmpty(),"Other templates do not make horseshoes");
  ItemStack c=new ItemStack(Items.COPPER_INGOT),t=new ItemStack(Horseshoes.TEMPLATE),i=new ItemStack(Items.IRON_INGOT);
  var grid=CraftingInput.of(3,3,List.of(c,t,c,c,i,c,c,c,c));
  var dup=h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
  var out=dup.value().assemble(grid);
  h.assertTrue(out.is(Horseshoes.TEMPLATE)&&out.getCount()==2,"Duplication: template top, iron middle, copper around gives two");
  h.succeed();
 }

 public static void points(GameTestHelper h){
  for(int t=1;t<Tier.values().length;t++){
   int two=((t+1)<<0)|((t+1)<<3), four=t|(t<<3)|(t<<6)|(t<<9);
   h.assertTrue(Horseshoes.points(two)==Horseshoes.points(four),"Two of tier "+(t+1)+" equal four of tier "+t);
   close(Horseshoes.effect(Horseshoes.points(two)),Horseshoes.effect(Horseshoes.points(four)),h,"Equal points, equal effect");
  }
  double last=0;
  for(Tier tier:Tier.values()){int code=0;for(int s=0;s<4;s++)code|=(tier.ordinal()+1)<<(3*s);double f=Horseshoes.effect(Horseshoes.points(code));h.assertTrue(f>last,"Higher full sets are stronger");last=f;}
  close(last,1,h,"Full Enderite set is the maximum");
  close(Horseshoes.effect(4),Math.log(5)/Math.log(129),h,"Full copper set");
  h.assertTrue(Horseshoes.effect(0)==0&&Horseshoes.effect(1000)==1,"Effect is bounded");
  h.assertTrue(Horseshoes.fullSet(0b001_001_001_001)&&!Horseshoes.fullSet(0b001_000_001_001),"Full set needs all four hooves");
  h.succeed();
 }

 public static void effects(GameTestHelper h){
  var original=Riding.CONFIG;
  try{
   var c=new RidingConfig();Riding.CONFIG=c;
   var horse=shod(h,Tier.IRON,Tier.IRON,Tier.IRON,Tier.IRON);var p=rider(h,horse);RidingEffects.tick(horse);
   double f=Horseshoes.effect(8);
   close(modifier(horse,Attributes.MOVEMENT_EFFICIENCY,Horseshoes.TERRAIN),.5*f,h,"Terrain bonus of an iron set");
   close(modifier(horse,Attributes.MOVEMENT_SPEED,Horseshoes.SPEED),.05f,h,"Full set speed bonus");
   close(modifier(horse,Attributes.JUMP_STRENGTH,Horseshoes.JUMP),.05f,h,"Full set jump bonus");
   Horseshoes.container(horse).setItem(3,ItemStack.EMPTY);RidingEffects.tick(horse);
   h.assertTrue(modifier(horse,Attributes.MOVEMENT_SPEED,Horseshoes.SPEED)==0&&modifier(horse,Attributes.JUMP_STRENGTH,Horseshoes.JUMP)==0,"Three shoes: no full-set bonus");
   close(modifier(horse,Attributes.MOVEMENT_EFFICIENCY,Horseshoes.TERRAIN),.5*Horseshoes.effect(6),h,"Terrain scales with the shoes left");
   Horseshoes.container(horse).setItem(3,new ItemStack(shoe(Tier.IRON)));
   horse.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,3));c.safety.maximumSpeedBonus=.92f;RidingEffects.tick(horse);
   double total=modifier(horse,Attributes.MOVEMENT_SPEED,RidingEffects.SPEED)+modifier(horse,Attributes.MOVEMENT_SPEED,Horseshoes.SPEED);
   h.assertTrue(total<=.92+1e-6,"Tailwind and horseshoes share the total speed cap: "+total);
   c.horseshoes.fullSetSpeedBonus=Float.MAX_VALUE;c.normalize();h.assertTrue(c.horseshoes.fullSetSpeedBonus<=RidingConfig.MAX_FULL_SET,"Hard cap");
   c.horseshoes.enableHorseshoes=false;RidingEffects.tick(horse);
   h.assertTrue(modifier(horse,Attributes.MOVEMENT_EFFICIENCY,Horseshoes.TERRAIN)==0&&modifier(horse,Attributes.MOVEMENT_SPEED,Horseshoes.SPEED)==0,"Switch removes bonuses");
   c.horseshoes.enableHorseshoes=true;p.stopRiding();RidingEffects.tick(horse);
   h.assertTrue(modifier(horse,Attributes.MOVEMENT_EFFICIENCY,Horseshoes.TERRAIN)==0,"Unridden horses get nothing");
   var llama=h.spawn(EntityTypes.LLAMA,2,2,2);h.assertTrue(!Horseshoes.canWear(llama)&&Horseshoes.canWear(h.spawn(EntityTypes.MULE,2,2,2)),"Wearer tag");
  }finally{Riding.CONFIG=original;}
  h.succeed();
 }

 public static void handling(GameTestHelper h){
  // The expected shares are calibrated on the full Enderite set, which exists only with SimpleBuilding.
  if(!Riding.SIMPLEBUILDING){com.mojang.logging.LogUtils.getLogger().info("[standalone] simplebuilding not loaded - skipping Enderite handling calibration");h.succeed();return;}
  var original=Riding.CONFIG;
  try{
   var c=new RidingConfig();Riding.CONFIG=c;
   var bare=h.spawn(EntityTypes.HORSE,2,2,2);var in=new Vec3(1,0,-1);
   h.assertTrue(Horseshoes.handling(bare,in).equals(in),"No shoes, Vanilla steering");
   var horse=shod(h,Tier.ENDERITE,Tier.ENDERITE,Tier.ENDERITE,Tier.ENDERITE);var p=rider(h,horse);RidingEffects.tick(horse);
   close(Horseshoes.handlingShare(Horseshoes.code(horse)),.3,h,"Server publishes the handling share");
   var out=Horseshoes.handling(horse,new Vec3(.5,0,-.25));
   close(out.x,.8,h,"Sideways share 0.5 -> 0.8");close(out.z,-.4,h,"Backward share 0.25 -> 0.4");
   close(Horseshoes.handling(horse,new Vec3(0,0,.98)).z,.98,h,"Forward input unchanged");
   c.horseshoes.handlingBonus=0;
   close(Horseshoes.handling(horse,new Vec3(.5,0,0)).x,.8,h,"Only the synced server value counts");
   RidingEffects.tick(horse);close(Horseshoes.handling(horse,new Vec3(.5,0,0)).x,.5,h,"Server config change reaches the synced value");
   c.horseshoes.handlingBonus=.3f;RidingEffects.tick(horse);
   p.xxa=1;p.zza=-1;
   var method=AbstractHorse.class.getDeclaredMethod("getRiddenInput",Player.class,Vec3.class);method.setAccessible(true);
   var real=(Vec3)method.invoke(horse,p,Vec3.ZERO);
   close(real.x,.8,h,"Real ridden input uses the mixin");close(real.z,-.4,h,"Real backward input");
   h.assertTrue(Double.isNaN(Horseshoes.handling(horse,new Vec3(Double.NaN,0,0)).x),"Nonfinite input is not amplified");
  }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}finally{Riding.CONFIG=original;}
  h.succeed();
 }

 public static void menu(GameTestHelper h){
  var horse=shod(h);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
  var menu=new HorseInventoryMenu(0,player.getInventory(),new SimpleContainer(horse.getInventorySize()),horse,horse.getInventoryColumns());
  int first=menu.slots.size()-4;
  h.assertTrue(first==2+horse.getInventorySize()+36,"Hoof slots come after the player inventory");
  for(int i=0;i<4;i++)h.assertTrue(menu.slots.get(first+i) instanceof HorseshoeSlot,"Hoof slot "+i);
  var slot=menu.slots.get(first);
  h.assertTrue(slot.mayPlace(new ItemStack(shoe(Tier.IRON)))&&!slot.mayPlace(new ItemStack(Items.IRON_INGOT))&&slot.getMaxStackSize()==1,"Only single horseshoes");
  h.assertTrue(HorseshoePanel.hasShoeSlots(menu),"Client panel sees the slots");
  player.getInventory().setItem(9,new ItemStack(shoe(Tier.GOLDEN)));
  int playerStart=2+horse.getInventorySize();
  menu.quickMoveStack(player,playerStart);
  h.assertTrue(Horseshoes.container(horse).getItem(0).is(shoe(Tier.GOLDEN))&&player.getInventory().getItem(9).isEmpty(),"Shift-click puts the shoe on");
  h.assertTrue(Horseshoes.tierAt(Horseshoes.code(horse),0)==Tier.GOLDEN.ordinal()+1,"Synced code follows the slot");
  player.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,5));menu.quickMoveStack(player,playerStart);
  h.assertTrue(Horseshoes.container(horse).getItem(1).isEmpty(),"Non-shoes never enter hoof slots");
  menu.quickMoveStack(player,first);
  h.assertTrue(Horseshoes.container(horse).getItem(0).isEmpty()&&player.getInventory().contains(new ItemStack(shoe(Tier.GOLDEN))),"Shift-click takes the shoe off");
  var llama=h.spawn(EntityTypes.LLAMA,2,2,2);llama.setTamed(true);
  var llamaMenu=new HorseInventoryMenu(1,player.getInventory(),new SimpleContainer(llama.getInventorySize()),llama,llama.getInventoryColumns());
  h.assertTrue(llamaMenu.slots.stream().noneMatch(s->s instanceof HorseshoeSlot)&&!HorseshoePanel.hasShoeSlots(llamaMenu),"Llamas have no hooves to shoe");
  var donkey=h.spawn(EntityTypes.DONKEY,2,2,2);donkey.setTamed(true);donkey.setChest(true);
  var chest=new SimpleContainer(AbstractMountInventoryMenu.getInventorySize(5));chest.setItem(0,new ItemStack(shoe(Tier.COPPER)));
  var donkeyMenu=new HorseInventoryMenu(2,player.getInventory(),chest,donkey,5);
  donkeyMenu.quickMoveStack(player,2);
  h.assertTrue(Horseshoes.container(donkey).isEmpty()&&chest.getItem(0).isEmpty(),"Chest items go to the player inventory, not onto the hooves");
  h.assertTrue(HorseshoePanel.contains(-10,40)&&!HorseshoePanel.contains(5,40),"Panel bounds");
  h.succeed();
 }

 public static void saveAndDrop(GameTestHelper h){
  var horse=shod(h,Tier.COPPER,null,Tier.DIAMOND,null);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());horse.saveWithoutId(out);
  var tag=out.buildResult();tag.remove("UUID");
  var copy=h.spawn(EntityTypes.HORSE,2,2,2);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
  var shoes=Horseshoes.container(copy);
  h.assertTrue(shoes.getItem(0).is(shoe(Tier.COPPER))&&shoes.getItem(1).isEmpty()&&shoes.getItem(2).is(shoe(Tier.DIAMOND)),"Shoes survive saving");
  h.assertTrue(Horseshoes.code(copy)==Horseshoes.code(horse)&&Horseshoes.code(copy)!=0,"Loaded horse publishes its code");
  var cursed=new ItemStack(shoe(Tier.IRON));cursed.enchant(ench(h,Enchantments.VANISHING_CURSE),1);shoes.setItem(3,cursed);
  copy.kill(h.getLevel());
  h.runAfterDelay(1,()->{
   var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(new BlockPos(2,2,2))).inflate(4)).stream().map(ItemEntity::getItem).filter(Horseshoes::isHorseshoe).toList();
   h.assertTrue(drops.size()==2,"Two shoes drop: "+drops);
   h.assertTrue(drops.stream().noneMatch(s->s.is(shoe(Tier.IRON))),"Curse of Vanishing shoe does not drop");
   h.succeed();
  });
 }

 public static void wear(GameTestHelper h){
  var original=Riding.CONFIG;
  try{
   var c=new RidingConfig();Riding.CONFIG=c;
   var horse=shod(h,Tier.IRON,Tier.GOLDEN,null,null);
   Horseshoes.wear(horse,39.5);h.assertTrue(Horseshoes.container(horse).getItem(0).getDamageValue()==0,"No wear before 40 blocks");
   Horseshoes.wear(horse,.5);h.assertTrue(Horseshoes.container(horse).getItem(0).getDamageValue()==1&&Horseshoes.container(horse).getItem(1).getDamageValue()==1,"One point per shoe after 40 blocks");
   Horseshoes.container(horse).getItem(1).setDamageValue(79);
   for(int i=0;i<10;i++)Horseshoes.wear(horse,4);
   h.assertTrue(Horseshoes.container(horse).getItem(1).isEmpty()&&Horseshoes.tierAt(Horseshoes.code(horse),1)==0,"Worn-out shoe breaks and the model updates");
   var diamond=new ItemStack(shoe(Tier.DIAMOND));diamond.enchant(ench(h,Enchantments.UNBREAKING),3);Horseshoes.container(horse).setItem(2,diamond);
   int before=Horseshoes.container(horse).getItem(0).getDamageValue();
   for(int i=0;i<1000;i++)Horseshoes.wear(horse,4);
   int plain=Horseshoes.container(horse).getItem(0).getDamageValue()-before, unbreaking=Horseshoes.container(horse).getItem(2).getDamageValue();
   h.assertTrue(plain==100&&unbreaking<60,"Unbreaking reduces wear: "+plain+" vs "+unbreaking);
   var p=rider(h,horse);Horseshoes.tick(horse,true);
   horse.setPos(horse.getX()+3,horse.getY(),horse.getZ());horse.setOnGround(true);
   int start=Horseshoes.container(horse).getItem(0).getDamageValue();((HorseshoeHolder)horse).simpleriding$setTravel(39);
   Horseshoes.tick(horse,true);h.assertTrue(Horseshoes.container(horse).getItem(0).getDamageValue()==start+1,"Ridden ground travel wears shoes");
   horse.setPos(horse.getX()+100,horse.getY(),horse.getZ());((HorseshoeHolder)horse).simpleriding$setTravel(39);
   Horseshoes.tick(horse,true);h.assertTrue(Horseshoes.container(horse).getItem(0).getDamageValue()==start+1,"Teleports are not travel");
   c.horseshoes.blocksPerDurability=-5;c.normalize();h.assertTrue(c.horseshoes.blocksPerDurability==RidingConfig.MIN_BLOCKS_PER_DURABILITY,"Wear interval lower bound");
   p.stopRiding();
  }finally{Riding.CONFIG=original;}
  h.succeed();
 }

 public static void mending(GameTestHelper h){
  var horse=shod(h);horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
  var mend=new ItemStack(shoe(Tier.IRON));mend.enchant(ench(h,Enchantments.MENDING),1);mend.setDamageValue(50);
  var plain=new ItemStack(shoe(Tier.IRON));plain.setDamageValue(50);
  Horseshoes.container(horse).setItem(0,mend);Horseshoes.container(horse).setItem(1,plain);
  var player=h.makeMockServerPlayerInLevel();h.runBeforeTestEnd(()->h.getLevel().getServer().getPlayerList().remove(player));
  h.assertTrue(Horseshoes.mend(player,10)==10,"Not riding: XP untouched");
  player.startRiding(horse,true,false);
  int left=Horseshoes.mend(player,10);
  h.assertTrue(left==0&&Horseshoes.container(horse).getItem(0).getDamageValue()==30,"Mending shoe repaired with rider XP: left "+left+", damage "+Horseshoes.container(horse).getItem(0).getDamageValue());
  h.assertTrue(Horseshoes.container(horse).getItem(1).getDamageValue()==50,"Shoes without Mending stay worn");
  var orb=new ExperienceOrb(h.getLevel(),player.getX(),player.getY(),player.getZ(),5);h.getLevel().addFreshEntity(orb);
  player.takeXpDelay=0;orb.playerTouch(player);
  h.assertTrue(Horseshoes.container(horse).getItem(0).getDamageValue()==20,"Real XP orb pickup mends the ridden horse's shoe: "+Horseshoes.container(horse).getItem(0).getDamageValue());
  h.succeed();
 }

 public static void fall(GameTestHelper h){
  try{
   var method=LivingEntity.class.getDeclaredMethod("calculateFallDamage",double.class,float.class);method.setAccessible(true);
   var bare=h.spawn(EntityTypes.HORSE,2,2,2);var full=shod(h,Tier.IRON,Tier.IRON,Tier.IRON,Tier.IRON);var three=shod(h,Tier.IRON,Tier.IRON,Tier.IRON,null);
   int base=(int)method.invoke(bare,23.0,1F), shod=(int)method.invoke(full,23.0,1F);
   h.assertTrue(base>0&&shod==Math.round(base*1.1f),"Full set: minimal extra fall damage "+base+" -> "+shod);
   h.assertTrue((int)method.invoke(three,23.0,1F)==base,"Three shoes: unchanged");
   int small=(int)method.invoke(bare,6.0,1F);h.assertTrue((int)method.invoke(full,6.0,1F)==small,"Short falls unchanged");
  }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  h.succeed();
 }

 public static void loot(GameTestHelper h){
  var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
  for(String table:RidingLoot.VILLAGE_TABLES){
   var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+table));
   var pools=new ArrayList<LootPool.Builder>();RidingLoot.apply(key,pools::add,h.getLevel().registryAccess());h.assertTrue(pools.size()==1,"One horseshoe pool for "+table);
   var loot=LootTable.lootTable().withPool(pools.getFirst()).build();int templates=0,shoes=0,rolls=2000;
   for(int n=0;n<rolls;n++){var found=new ArrayList<ItemStack>();loot.getRandomItemsRaw(new LootContext.Builder(params).withOptionalRandomSeed(n).create(Optional.empty()),found::add);for(var s:found){if(s.is(Horseshoes.TEMPLATE))templates++;else if(Horseshoes.isHorseshoe(s)){shoes++;h.assertTrue(s.isDamaged()&&!s.is(shoe(Tier.DIAMOND)),"Found shoes are worn copper/iron");}}}
   h.assertTrue(templates>rolls*.10&&templates<rolls*.20,"Template about 15%: "+templates);
   h.assertTrue(shoes>rolls*.06&&shoes<rolls*.14,"Shoes about 10%: "+shoes);
   var loaded=h.getLevel().getServer().reloadableRegistries().getLootTable(key);boolean found=false;
   for(int n=0;n<400&&!found;n++)found=loaded.getRandomItems(params,(long)n).stream().anyMatch(s->s.is(Horseshoes.TEMPLATE));
   h.assertTrue(found,"Real loaded "+table+" contains the template");
  }
  var rare=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+RidingLoot.TRAIL_RARE));
  var common=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+RidingLoot.TRAIL_COMMON));
  int templates=0,shoes=0;long hit=-1;
  for(long seed=0;seed<13000;seed++){
   var r=RidingLoot.archaeology(rare,seed,new ItemStack(Items.BRICK),h.getLevel().registryAccess());if(r.is(Horseshoes.TEMPLATE)){templates++;if(hit<0)hit=seed;}else h.assertTrue(r.is(Items.BRICK),"Unselected find kept");
   if(Horseshoes.isHorseshoe(RidingLoot.archaeology(common,seed,new ItemStack(Items.BRICK),h.getLevel().registryAccess())))shoes++;
  }
  h.assertTrue(templates>800&&templates<1200,"Trail ruins rare: about 1 in 13 ("+templates+")");
  h.assertTrue(shoes>200&&shoes<370,"Trail ruins common: about 1 in 46 ("+shoes+")");
  h.assertTrue(RidingLoot.archaeology(rare,hit,ItemStack.EMPTY,h.getLevel().registryAccess()).is(Horseshoes.TEMPLATE),"Deterministic per seed");
  var pos=new BlockPos(1,2,1);h.setBlock(pos,Blocks.SUSPICIOUS_GRAVEL);
  var be=(BrushableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));be.setLootTable(rare,hit);
  be.brush(h.getLevel().getGameTime(),h.getLevel(),h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL),net.minecraft.core.Direction.UP,new ItemStack(Items.BRUSH));
  h.assertTrue(be.getItem().is(Horseshoes.TEMPLATE),"Real brushing yields the template");
  var original=Riding.CONFIG;
  try{var off=new RidingConfig();off.worldGen.enableLootTableChanges=false;Riding.CONFIG=off;
   var pools=new ArrayList<LootPool.Builder>();RidingLoot.apply(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+RidingLoot.VILLAGE_TABLES[0])),pools::add,h.getLevel().registryAccess());
   h.assertTrue(pools.isEmpty()&&RidingLoot.archaeology(rare,hit,new ItemStack(Items.BRICK),h.getLevel().registryAccess()).is(Items.BRICK),"Loot switch disables horseshoe loot");
  }finally{Riding.CONFIG=original;}
  h.succeed();
 }
}
