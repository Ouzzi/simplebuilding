package com.simpleriding.test;
import com.simpleriding.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import com.google.gson.*;
import java.io.*;
import java.util.*;
public final class RidingTests {
 public static final Map<String,java.util.function.Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static { ALL.put("launch",RidingTests::launch); ALL.put("armor_and_anvil",RidingTests::armorAndAnvil); ALL.put("horse_speed_and_cleanup",RidingTests::horseSpeedAndCleanup); ALL.put("pig_speed",RidingTests::pigSpeed); ALL.put("strider_speed",RidingTests::striderSpeed); ALL.put("camel_speed",RidingTests::camelSpeed); ALL.put("ghast_harness",RidingTests::ghastHarness); ALL.put("leaping_and_cleanup",RidingTests::leapingAndCleanup); ALL.put("armor_defense",RidingTests::armorDefense); ALL.put("trades",RidingTests::trades); ALL.put("loot_and_toggle",RidingTests::lootAndToggle); ALL.put("config_and_lang",RidingTests::configAndLang); ALL.put("cross_mod_storage_and_armor",RidingTests::crossModStorageAndArmor); }
 private static Holder.Reference<Enchantment> ench(GameTestHelper h,ResourceKey<Enchantment> key){return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key,int n){var s=new ItemStack(item);s.enchant(ench(h,key),n);return s;}
 private static net.minecraft.world.entity.player.Player rider(GameTestHelper h,LivingEntity e){var p=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);if(e instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse)horse.setTamed(true);if(e.getType()==EntityTypes.PIG)p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CARROT_ON_A_STICK));if(e.getType()==EntityTypes.STRIDER)p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));p.startRiding(e,true,false);return p;}
 private static void close(double a,double b,GameTestHelper h,String msg){h.assertTrue(Math.abs(a-b)<1e-5,msg+": "+a+" != "+b);}
 public static void launch(GameTestHelper h){
  h.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Riding.id("coordinates"))==Riding.COORDINATES,"Legacy coordinates component is registered");
  h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Riding.id("riding_items"))==Riding.TAB,"Creative tab registered");
  h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(i->i.getNamespace().equals("simpleriding")),"No invented module items");
  for(var k:List.of(Riding.TAILWIND,Riding.LEAPING)){h.assertTrue(ench(h,k).value().getMaxLevel()==3,"Enchantment levels preserved");}
  var lookup=h.getLevel().registryAccess();Riding.TAB.buildContents(new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS,true,lookup));
  h.assertTrue(Riding.TAB.getDisplayItems().size()==2,"Exactly two riding books");
  for(var k:RidingLoot.TABLES)h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+k)))!=LootTable.EMPTY,"Loot table loads: "+k);
  h.succeed();
 }
 public static void armorAndAnvil(GameTestHelper h){
  var armor=new ItemStack(Items.DIAMOND_HORSE_ARMOR);h.assertTrue(armor.is(Riding.ARMOR),"Armor tag loaded");
  for(var key:List.of(Enchantments.PROTECTION,Enchantments.FIRE_PROTECTION,Enchantments.BLAST_PROTECTION,Enchantments.PROJECTILE_PROTECTION,Enchantments.FEATHER_FALLING,Riding.LEAPING)){
   h.assertTrue(ench(h,key).value().canEnchant(armor),"Supported armor enchantment "+key);
   var menu=new AnvilMenu(0,h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE).getInventory());
   menu.getSlot(0).set(armor.copy());menu.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(ench(h,key),1)));menu.createResult();
   h.assertTrue(!menu.getSlot(2).getItem().isEmpty(),"Real anvil accepts "+key);
  }
  h.assertTrue(!ench(h,Enchantments.DEPTH_STRIDER).value().canEnchant(armor),"No player-only movement enchantments");
  h.assertTrue(!ench(h,Enchantments.MENDING).value().canEnchant(armor),"Source anvil whitelist excludes Mending");
  h.assertTrue(EnchantmentHelper.getEnchantmentCost(RandomSource.create(5),2,15,armor)>0,"Armor enchantability is 15");
  h.assertTrue(!EnchantmentHelper.selectEnchantment(RandomSource.create(6),armor,30,java.util.stream.Stream.of(ench(h,Enchantments.PROTECTION))).isEmpty(),"Armor works at enchanting table");
  h.succeed();
 }
 private static <T extends LivingEntity> void speed(GameTestHelper h,EntityType<T> type,double expected){
  var e=h.spawn(type,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,3));var p=rider(h,e);
  h.assertTrue(e.getControllingPassenger()==p,"Player controls saddled mount");
  h.runAfterDelay(2,()->{var mod=e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED);h.assertTrue(mod!=null,"Tick installs speed modifier");close(mod.amount(),expected,h,"Speed bonus");
   e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
   h.runAfterDelay(2,()->{h.assertTrue(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED)==null,"Removing enchantment clears speed while still ridden");p.stopRiding();h.succeed();});});
 }
 public static void horseSpeedAndCleanup(GameTestHelper h){speed(h,EntityTypes.HORSE,.9);}
 public static void pigSpeed(GameTestHelper h){speed(h,EntityTypes.PIG,.6);}
 public static void striderSpeed(GameTestHelper h){speed(h,EntityTypes.STRIDER,.6);}
 public static void camelSpeed(GameTestHelper h){speed(h,EntityTypes.CAMEL,.9);}
 public static void ghastHarness(GameTestHelper h){
  var harness=BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:white_harness"));var e=h.spawn(EntityTypes.HAPPY_GHAST,2,2,2);e.setItemSlot(EquipmentSlot.BODY,enchanted(h,harness,Riding.TAILWIND,3));var p=rider(h,e);
  h.assertTrue(e.getControllingPassenger()==p,"Ghast rider controls harness");RidingEffects.tick(e);
  var mod=e.getAttribute(Attributes.FLYING_SPEED).getModifier(RidingEffects.SPEED);h.assertTrue(mod!=null,"Harness applies flight boost");close(mod.amount(),2.55,h,"Ghast boost");
  e.setItemSlot(EquipmentSlot.BODY,new ItemStack(harness));RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.FLYING_SPEED).getModifier(RidingEffects.SPEED)==null,"No free ghast boost");p.stopRiding();h.succeed();
 }
 public static void leapingAndCleanup(GameTestHelper h){
  var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,Riding.LEAPING,3));var p=rider(h,e);RidingEffects.tick(e);
  close(e.getAttribute(Attributes.JUMP_STRENGTH).getModifier(RidingEffects.JUMP).amount(),.6,h,"Leaping III");
  e.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.JUMP_STRENGTH).getModifier(RidingEffects.JUMP)==null,"Removing armor clears jump");
  e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,Riding.LEAPING,3));p.stopRiding();RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.JUMP_STRENGTH).getModifier(RidingEffects.JUMP)==null,"Dismount clears jump");h.succeed();
 }
 public static void armorDefense(GameTestHelper h){
  var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,Enchantments.PROTECTION,4));
  close(EnchantmentHelper.getDamageProtection(h.getLevel(),e,e.damageSources().generic()),4,h,"Vanilla protection works once on BODY armor");
  var types=h.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
  for(var pair:List.of(Map.entry(Enchantments.FIRE_PROTECTION,net.minecraft.world.damagesource.DamageTypes.ON_FIRE),Map.entry(Enchantments.BLAST_PROTECTION,net.minecraft.world.damagesource.DamageTypes.EXPLOSION),Map.entry(Enchantments.PROJECTILE_PROTECTION,net.minecraft.world.damagesource.DamageTypes.ARROW))){
   e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,pair.getKey(),4));
   close(EnchantmentHelper.getDamageProtection(h.getLevel(),e,new net.minecraft.world.damagesource.DamageSource(types.getOrThrow(pair.getValue()))),8,h,"Special protection applies to matching damage once: "+pair.getKey());
  }
  e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,Enchantments.FEATHER_FALLING,4));
  e.setHealth(e.getMaxHealth());float before=e.getHealth();e.causeFallDamage(10,1,e.damageSources().fall());float feather=before-e.getHealth();
  e.setInvulnerableTime(0);e.damageCooldownTime=0;e.setHealth(e.getMaxHealth());e.setItemSlot(EquipmentSlot.BODY,new ItemStack(Items.DIAMOND_HORSE_ARMOR));before=e.getHealth();e.causeFallDamage(10,1,e.damageSources().fall());
  h.assertTrue(feather<before-e.getHealth(),"Feather Falling lowers real fall damage");h.succeed();
 }
 public static void trades(GameTestHelper h){
  var reg=h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
  for(int tier=2;tier<=4;tier++){
   var key=ResourceKey.create(Registries.VILLAGER_TRADE,Riding.id("librarian/"+tier+"/riding_book"));var trade=reg.getOrThrow(key);
   var pool=reg.getOrThrow(TagKey.create(Registries.VILLAGER_TRADE,Identifier.parse("minecraft:librarian/level_"+tier)));
   boolean rebalance=h.getLevel().getServer().getResourceManager().listPacks().anyMatch(p->p.packId().equals("trade_rebalance"));
   if(!rebalance)h.assertTrue(pool.contains(trade),"Trade is in librarian pool");
   boolean shipped=false;
   for(var resource:h.getLevel().getServer().getResourceManager().getResourceStack(Identifier.parse("minecraft:tags/villager_trade/librarian/level_"+tier+".json"))){try(var reader=resource.openAsReader()){
    var json=JsonParser.parseReader(reader).getAsJsonObject();if(json.toString().contains(key.identifier().toString())){h.assertTrue(!json.has("replace")||!json.get("replace").getAsBoolean(),"Module tag is additive");shipped=true;}
   }catch(IOException e){throw new IllegalStateException(e);}}
   h.assertTrue(shipped,"Module contributes its trade to the normal pool, even when the experimental rebalance pack replaces that pool");
   var entity=h.spawn(EntityTypes.VILLAGER,2,2,2);
   var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY,entity).withParameter(LootContextParams.ORIGIN,entity.position()).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,net.minecraft.util.Unit.INSTANCE).create(LootContextParamSets.VILLAGER_TRADE);
   for(int n=0;n<100;n++){var offer=trade.value().getOffer(new LootContext.Builder(params).withOptionalRandomSeed(n).create(Optional.empty()));h.assertTrue(offer!=null,"Trade creates offer");
    int cost=offer.getCostA().getCount();h.assertTrue(cost>=(tier==2?10:15)&&cost<=(tier==2?29:tier==3?34:49),"Original price interval");
    var books=EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());h.assertTrue(!books.isEmpty(),"Book has enchantments");for(var enchant:books.keySet())h.assertTrue((enchant.is(Riding.TAILWIND)||enchant.is(Riding.LEAPING))&&books.getLevel(enchant)==tier-1,"Book pool and levels");
   }
  } h.succeed();
 }
 public static void lootAndToggle(GameTestHelper h){
  for(String table:RidingLoot.TABLES){
   var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+table));var pools=new ArrayList<LootPool.Builder>();RidingLoot.apply(key,pools::add,h.getLevel().registryAccess());h.assertTrue(pools.size()==1,"One riding pool for "+table);
   var loot=LootTable.lootTable().withPool(pools.getFirst()).build();var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
   var stacks=new ArrayList<ItemStack>();for(int n=0;n<100;n++)loot.getRandomItemsRaw(new LootContext.Builder(params).withOptionalRandomSeed(n).create(Optional.empty()),stacks::add);
   h.assertTrue(!stacks.isEmpty(),"Loot can produce books");for(var stack:stacks)h.assertTrue(stack.is(Items.ENCHANTED_BOOK)&&!EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty(),"Loot book is enchanted");
   var loaded=h.getLevel().getServer().reloadableRegistries().getLootTable(key);int ridingBooks=0;
   for(int n=0;n<200;n++)for(var stack:loaded.getRandomItems(params,(long)n)){var enchants=EnchantmentHelper.getEnchantmentsForCrafting(stack);if(enchants.getLevel(ench(h,Riding.TAILWIND))>0||enchants.getLevel(ench(h,Riding.LEAPING))>0)ridingBooks++;}
   h.assertTrue(ridingBooks>0,"Real loaded table contains riding pool alongside SimpleBuilding: "+table);
  }
  var original=Riding.CONFIG;try {var disabled=new RidingConfig();disabled.worldGen.enableLootTableChanges=false;Riding.CONFIG=disabled;var pools=new ArrayList<LootPool.Builder>();RidingLoot.apply(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+RidingLoot.TABLES[0])),pools::add,h.getLevel().registryAccess());h.assertTrue(pools.isEmpty(),"Loot toggle disables injection");}finally{Riding.CONFIG=original;}h.succeed();
 }
 private static JsonObject lang(String l){try(var in=Riding.class.getResourceAsStream("/assets/simpleriding/lang/"+l+".json")){return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){throw new IllegalStateException(e);}}
 public static void configAndLang(GameTestHelper h){
  var en=lang("en_us");var de=lang("de_de");h.assertTrue(en.keySet().equals(de.keySet()),"Language keys are complete");
  String[] keys={"worldGen.enableVillagerTrades","worldGen.enableLootTableChanges","enchantments.swiftRide.ghastSpeedMultiplier","enchantments.swiftRide.horseSpeedMultiplier","enchantments.swiftRide.otherSpeedMultiplier","enchantments.horseJump.jumpStrengthMultiplier"};String[] defaults={"true","true","0.85","0.3","0.2","0.2"};
  for(int i=0;i<keys.length;i++){String k="text.autoconfig.simpleriding.option."+keys[i];for(var l:List.of(en,de)){h.assertTrue(l.has(k)&&l.has(k+".@Tooltip")&&l.get(k+".@Tooltip").getAsString().contains(defaults[i]),"Option name/tooltip/default: "+keys[i]);h.assertTrue(l.has("text.autoconfig.simpleriding.option."+keys[i].split("\\.")[0]),"Option tab");}}
  var c=new RidingConfig();c.enchantments.swiftRide.horseSpeedMultiplier=Float.POSITIVE_INFINITY;c.enchantments.swiftRide.ghastSpeedMultiplier=100;c.enchantments.swiftRide.otherSpeedMultiplier=-3;c.enchantments.horseJump.jumpStrengthMultiplier=10;c.normalize();
  close(c.enchantments.swiftRide.horseSpeedMultiplier,.3,h,"Nonfinite default");close(c.enchantments.swiftRide.ghastSpeedMultiplier,1,h,"Speed cap");close(c.enchantments.swiftRide.otherSpeedMultiplier,0,h,"Nonnegative speed");close(c.enchantments.horseJump.jumpStrengthMultiplier,.5,h,"Jump cap");
  var original=Riding.CONFIG;try{
   var disabled=new RidingConfig();disabled.worldGen.enableVillagerTrades=false;Riding.CONFIG=disabled;
   Class<?> type;try{type=Class.forName("com.simplebuilding.modules.simpleriding.RidingFabricData");}catch(ClassNotFoundException e){type=Class.forName("com.simplebuilding.modules.simpleriding.RidingNeoData");}
   var instance=type.getConstructor().newInstance();var method=Arrays.stream(type.getMethods()).filter(m->m.getName().equals("test")&&m.getParameterCount()==1).findFirst().orElseThrow();
   h.assertTrue(!(Boolean)method.invoke(instance,new Object[]{null}),"Actual loader condition disables trade loading");disabled.worldGen.enableVillagerTrades=true;
   h.assertTrue((Boolean)method.invoke(instance,new Object[]{null}),"Actual loader condition allows trades by default");
   var dir=java.nio.file.Files.createTempDirectory(h.getLevel().getServer().getServerDirectory(),"riding-config-test-");
   java.nio.file.Files.writeString(dir.resolve("simpleriding.json"),"{\"worldGen\":{\"enableVillagerTrades\":false},\"enchantments\":{\"swiftRide\":{\"horseSpeedMultiplier\":0.7}}}");
   var loaded=RidingConfig.load(dir);h.assertTrue(!loaded.worldGen.enableVillagerTrades&&loaded.worldGen.enableLootTableChanges,"Legacy switch and missing default survive JSON loading");close(loaded.enchantments.swiftRide.horseSpeedMultiplier,.7,h,"Legacy nested numeric key");
  }catch(Exception e){throw new IllegalStateException(e);}finally{Riding.CONFIG=original;}h.succeed();
 }
 public static void crossModStorageAndArmor(GameTestHelper h){
  var armor=BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_horse_armor"));h.assertTrue(armor!=Items.AIR&&new ItemStack(armor).is(Riding.ARMOR),"Enderite armor is supported through public ID");
  h.assertTrue(ench(h,Riding.LEAPING).value().canEnchant(new ItemStack(armor)),"Enderite armor accepts Leaping");
  var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:reinforced_hopper"));var pos=new BlockPos(2,2,2);h.setBlock(pos,block);var container=(net.minecraft.world.Container)h.getLevel().getBlockEntity(h.absolutePos(pos));
  var saddle=enchanted(h,Items.SADDLE,Riding.TAILWIND,3);saddle.set(Riding.COORDINATES,new BlockPos(1,2,3));container.setItem(0,saddle);var restored=container.removeItem(0,1);h.assertTrue(restored.get(Riding.COORDINATES).equals(new BlockPos(1,2,3))&&EnchantmentHelper.getItemEnchantmentLevel(ench(h,Riding.TAILWIND),restored)==3,"Foreign component and enchantment survive SimpleBuilding storage");
  var nautilus=BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_nautilus_armor"));h.assertTrue(!new ItemStack(nautilus).is(Riding.ARMOR),"Horse-only scope leaves nautilus armor unchanged");h.succeed();
 }
}
