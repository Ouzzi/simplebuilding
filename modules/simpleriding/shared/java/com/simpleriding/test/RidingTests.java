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
 static { ALL.put("ground_flags",RidingTests::groundFlags); ALL.put("camel_dash_bounds",RidingTests::camelDashBounds); ALL.put("weighted_data_bounds",RidingTests::weightedDataBounds); ALL.put("steering_and_border",RidingTests::steeringAndBorder); ALL.put("all_mount_speed_caps",RidingTests::allMountSpeedCaps); ALL.put("nautilus_speed_and_armor",RidingTests::nautilusSpeedAndArmor); ALL.put("nautilus_dash",RidingTests::nautilusDash); ALL.put("attribute_caps",RidingTests::attributeCaps); ALL.put("feature_switches",RidingTests::featureSwitches); ALL.put("all_config_bounds",RidingTests::allConfigBounds); ALL.put("movement_packets",RidingTests::movementPackets); ALL.put("jump_packets",RidingTests::jumpPackets); ALL.put("movement_budget",RidingTests::movementBudget); ALL.put("launch",RidingTests::launch); ALL.put("armor_and_anvil",RidingTests::armorAndAnvil); ALL.put("horse_speed_and_cleanup",RidingTests::horseSpeedAndCleanup); ALL.put("pig_speed",RidingTests::pigSpeed); ALL.put("strider_speed",RidingTests::striderSpeed); ALL.put("camel_speed",RidingTests::camelSpeed); ALL.put("ghast_harness",RidingTests::ghastHarness); ALL.put("leaping_and_cleanup",RidingTests::leapingAndCleanup); ALL.put("armor_defense",RidingTests::armorDefense); ALL.put("trades",RidingTests::trades); ALL.put("loot_and_toggle",RidingTests::lootAndToggle); ALL.put("config_and_lang",RidingTests::configAndLang); ALL.put("cross_mod_storage_and_armor",RidingTests::crossModStorageAndArmor); ALL.put("vanilla_tab_placement",RidingTests::vanillaTabPlacement); ALL.putAll(HorseshoeTests.ALL); }
 static { ALL.put("book_models", RidingBookTests::models); }
 static { ALL.put("guide_book", com.simpleriding.guide.RidingGuide::gameTest); }
 private static Holder.Reference<Enchantment> ench(GameTestHelper h,ResourceKey<Enchantment> key){return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key,int n){var s=new ItemStack(item);s.enchant(ench(h,key),n);return s;}
 private static net.minecraft.world.entity.player.Player rider(GameTestHelper h,LivingEntity e){var p=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);if(e instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse)horse.setTamed(true);if(e instanceof net.minecraft.world.entity.TamableAnimal tame)tame.tame(p);if(e.getType()==EntityTypes.PIG)p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CARROT_ON_A_STICK));if(e.getType()==EntityTypes.STRIDER)p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));p.startRiding(e,true,false);return p;}
 private static void close(double a,double b,GameTestHelper h,String msg){h.assertTrue(Math.abs(a-b)<1e-5,msg+": "+a+" != "+b);}
 public static void launch(GameTestHelper h){
  h.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Riding.id("coordinates"))==Riding.COORDINATES,"Legacy coordinates component is registered");
  h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Riding.id("riding_items"))==Riding.TAB,"Creative tab registered");
  h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().filter(i->i.getNamespace().equals("simpleriding")).map(Identifier::getPath).collect(java.util.stream.Collectors.toSet()).equals(Set.of("horseshoe_smithing_template","copper_horseshoe","iron_horseshoe","golden_horseshoe","diamond_horseshoe","netherite_horseshoe","enderite_horseshoe","guide_book")),"Only the R1 horseshoe items and the guide");
  for(var k:List.of(Riding.TAILWIND,Riding.LEAPING)){h.assertTrue(ench(h,k).value().getMaxLevel()==3,"Enchantment levels preserved");}
  var lookup=h.getLevel().registryAccess();Riding.TAB.buildContents(new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS,true,lookup));
  h.assertTrue(Riding.TAB.getDisplayItems().size()==1+2+1+Horseshoes.ITEMS.size(),"The guide, two riding books, the template and every horseshoe");
  for(var k:RidingLoot.TABLES)h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:"+k)))!=LootTable.EMPTY,"Loot table loads: "+k);
  var advancement=h.getLevel().getServer().getAdvancements().get(Riding.id("nautilus_equipment"));h.assertTrue(advancement!=null&&advancement.value().display().isPresent(),"Nautilus advancement and hints load");
  h.assertTrue(!advancement.value().display().get().announceToChat(),"Advancement does not announce in chat");
  var player=h.makeMockServerPlayerInLevel();h.runBeforeTestEnd(()->h.getLevel().getServer().getPlayerList().remove(player));
  player.getInventory().setItem(9,new ItemStack(Items.DIAMOND_NAUTILUS_ARMOR));player.inventoryMenu.broadcastChanges();
  h.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),"Real nautilus armor inventory trigger grants hint");
  h.succeed();
 }
 /**
  * The horseshoes stand in vanilla's Combat tab right before the wolf armor (after every horse armor) and the
  * template in Ingredients right before the bottle o' enchanting (owner 2026-10-02), so the search tab lists them
  * there too. Breaks when a loader hook is gone or an anchor moves.
  */
 public static void vanillaTabPlacement(GameTestHelper h){
  CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess());
  for(var key:List.of(CreativeModeTabs.COMBAT,CreativeModeTabs.INGREDIENTS)){
   var content=new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(key).getDisplayItems());
   var stacks=Riding.vanillaTabStacks(key);h.assertTrue(!stacks.isEmpty(),"Something to place in "+key);
   int anchor=-1;for(int i=0;i<content.size();i++)if(content.get(i).is(Riding.vanillaTabAnchor(key))){anchor=i;break;}
   h.assertTrue(anchor>=stacks.size(),"Anchor present in "+key);
   for(int i=0;i<stacks.size();i++)h.assertTrue(content.get(anchor-stacks.size()+i).is(stacks.get(i).getItem()),key+": "+stacks.get(i)+" right before the anchor, found "+content.get(anchor-stacks.size()+i));
  }
  var search=CreativeModeTabs.searchTab().getDisplayItems();
  for(var item:Horseshoes.ITEMS.values())h.assertTrue(search.stream().anyMatch(s->s.is(item)),"Search tab lists "+item);
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
  var mod=e.getAttribute(Attributes.FLYING_SPEED).getModifier(RidingEffects.SPEED);h.assertTrue(mod!=null,"Harness applies flight boost");close(Math.pow(1+mod.amount(),2)-1,2.55,h,"Ghast actual quadratic flight boost");
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
  String template="item.simpleriding.horseshoe_smithing_template";
  h.assertTrue(Horseshoes.TEMPLATE instanceof SmithingTemplateItem,"Vanilla template supplies Applies to / Ingredients headings and slot hints");
  h.assertTrue(en.get(template).getAsString().equals("Horseshoe Upgrade")&&de.get(template).getAsString().equals("Hufeisen-Aufwertung"),"Template follows Basic Upgrade / Basis-Aufwertung naming");
  for(var l:List.of(en,de))for(String part:List.of("applies_to","ingredients","base_slot_description","additions_slot_description"))
   h.assertTrue(l.has(template+"."+part)&&!l.get(template+"."+part).getAsString().isBlank(),"Template hint translated: "+part);
  h.assertTrue(en.get(template+".applies_to").getAsString().equals("Copper Ingots, Iron Ingots, Gold Ingots, Diamonds, Diamond Horseshoes, Netherite Horseshoes"),"Template names exactly the six recipe bases");
  h.assertTrue(de.get(template+".applies_to").getAsString().equals("Kupferbarren, Eisenbarren, Goldbarren, Diamanten, Diamanthufeisen, Netherithufeisen"),"German template names exactly the six recipe bases");
  h.assertTrue(en.get(template+".ingredients").getAsString().equals("Iron Nuggets, Netherite Ingots or Enderite Ingots")&&de.get(template+".ingredients").getAsString().equals("Eisennuggets, Netheritbarren oder Enderitbarren"),"Template names all three recipe additions in EN and DE");
  for(var option:RidingOptions.ALL)for(var l:List.of(en,de)){
   String k=option.nameKey();h.assertTrue(l.has(k)&&l.has(k+".@Tooltip")&&l.get(k+".@Tooltip").getAsString().contains(option.defaultValue().toString()),"Option name/tooltip/default: "+option.path());h.assertTrue(l.has(option.tabKey()),"Option tab: "+option.path());
  }
  var c=new RidingConfig();c.enchantments.swiftRide.horseSpeedMultiplier=Float.POSITIVE_INFINITY;c.enchantments.swiftRide.ghastSpeedMultiplier=100;c.enchantments.swiftRide.otherSpeedMultiplier=-3;c.enchantments.horseJump.jumpStrengthMultiplier=10;c.normalize();
  close(c.enchantments.swiftRide.horseSpeedMultiplier,.3,h,"Nonfinite default");close(c.enchantments.swiftRide.ghastSpeedMultiplier,1,h,"Speed cap");close(c.enchantments.swiftRide.otherSpeedMultiplier,0,h,"Nonnegative speed");close(c.enchantments.horseJump.jumpStrengthMultiplier,.5,h,"Jump cap");
  var original=Riding.CONFIG;try{
   var disabled=new RidingConfig();disabled.worldGen.enableVillagerTrades=false;Riding.CONFIG=disabled;
   Class<?> type;try{type=Class.forName("com.simplebuilding.modules.simpleriding.RidingFabricData");}catch(ClassNotFoundException e){try{type=Class.forName("com.simplebuilding.modules.simpleriding.RidingNeoData");}catch(ClassNotFoundException absent){type=Class.forName("com.simplebuilding.modules.simpleriding.forge.RidingCondition");}}
   var instance=type.getConstructor().newInstance();var method=Arrays.stream(type.getMethods()).filter(m->m.getName().equals("test")&&(m.getParameterCount()==1||m.getParameterCount()==2)).findFirst().orElseThrow();
   h.assertTrue(!(Boolean)method.invoke(instance,new Object[method.getParameterCount()]),"Actual loader condition disables trade loading");disabled.worldGen.enableVillagerTrades=true;
   h.assertTrue((Boolean)method.invoke(instance,new Object[method.getParameterCount()]),"Actual loader condition allows trades by default");
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
  var nautilus=BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_nautilus_armor"));h.assertTrue(new ItemStack(nautilus).is(Riding.NAUTILUS_ARMOR)&&ench(h,Riding.LEAPING).value().canEnchant(new ItemStack(nautilus)),"Enderite nautilus armor accepts Leaping through public ID");h.succeed();
 }
 private static net.minecraft.server.level.ServerPlayer serverRider(GameTestHelper h,LivingEntity mount){
  var p=h.makeMockServerPlayerInLevel();
  var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
  new io.netty.channel.embedded.EmbeddedChannel(connection);
  int[] teleportId={0};
  p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false)){
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet){super.send(packet);if(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket position)teleportId[0]=position.id();}
  };
  p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
  if(mount instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse)horse.setTamed(true);
  if(mount instanceof net.minecraft.world.entity.TamableAnimal tame)tame.tame(p);
  p.startRiding(mount,true,false);h.assertTrue(mount.getControllingPassenger()==p,"Server player controls mount");
  p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleportId[0],p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
  h.runBeforeTestEnd(()->h.getLevel().getServer().getPlayerList().remove(p));
  return p;
 }
 public static void nautilusSpeedAndArmor(GameTestHelper h){
  for(var type:List.of(EntityTypes.NAUTILUS,EntityTypes.ZOMBIE_NAUTILUS)){
   var e=h.spawn(type,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,3));var p=rider(h,e);RidingEffects.tick(e);
   close(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED).amount(),.6,h,"Nautilus Tailwind III");
   var armor=BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_nautilus_armor"));e.setItemSlot(EquipmentSlot.BODY,enchanted(h,armor,Riding.LEAPING,3));
   h.assertTrue(ench(h,Riding.LEAPING).value().canEnchant(e.getItemBySlot(EquipmentSlot.BODY)),"Enderite nautilus Leaping");
   close(RidingEffects.dashScale(e,1),1.6,h,"Nautilus Leaping III dash");
   e.setItemSlot(EquipmentSlot.BODY,enchanted(h,armor,Enchantments.PROTECTION,4));close(EnchantmentHelper.getDamageProtection(h.getLevel(),e,e.damageSources().generic()),4,h,"Nautilus protection exactly once");
   p.stopRiding();RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED)==null,"Nautilus dismount removes speed");close(RidingEffects.dashScale(e,1),1,h,"Dismount removes dash bonus");
   for(String material:List.of("copper","iron","golden","diamond","netherite"))h.assertTrue(Riding.armor(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(material+"_nautilus_armor")))),"Vanilla nautilus armor tagged: "+material);
  }h.succeed();
 }
 public static void nautilusDash(GameTestHelper h){
  var e=h.spawn(EntityTypes.NAUTILUS,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_NAUTILUS_ARMOR,Riding.LEAPING,3));var p=serverRider(h,e);
  e.setDeltaMovement(Vec3.ZERO);p.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(p,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,100));
  h.assertTrue(e.getJumpCooldown()==40&&e.isDashing(),"Server executes dash with Vanilla cooldown");
  close(e.getDeltaMovement().length(),.5*e.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.6,h,"Server impulse includes armor bonus on land");
  var before=e.getDeltaMovement();p.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(p,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,100));h.assertTrue(e.getDeltaMovement().equals(before),"Repeated dash cannot stack impulse");p.stopRiding();e.discard();
  for(int x=1;x<=3;x++)for(int y=2;y<=4;y++)for(int z=1;z<=3;z++)h.setBlock(x,y,z,net.minecraft.world.level.block.Blocks.WATER);
  var swimmer=h.spawn(EntityTypes.NAUTILUS,2,2,2);swimmer.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,3));swimmer.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_NAUTILUS_ARMOR,Riding.LEAPING,3));var pilot=serverRider(h,swimmer);
  h.runAfterDelay(2,()->{
   h.assertTrue(swimmer.isInWater(),"Aquatic dash fixture is actually underwater");RidingEffects.tick(swimmer);swimmer.setDeltaMovement(Vec3.ZERO);
   pilot.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(pilot,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,100));
   double expected=pilot.getLookAngle().length()*(double)(1.2f*1.6f)*swimmer.getAttributeValue(Attributes.MOVEMENT_SPEED);
   close(swimmer.getDeltaMovement().length(),expected,h,"Real underwater dash combines Tailwind and Leaping within server caps");
   h.assertTrue(swimmer.getDeltaMovement().length()<=3.9&&swimmer.getJumpCooldown()==40,"Aquatic impulse and cooldown remain bounded");pilot.stopRiding();h.succeed();
  });
 }
 public static void attributeCaps(GameTestHelper h){
  var original=Riding.CONFIG;try{
   var c=new RidingConfig();Riding.CONFIG=c;c.enchantments.swiftRide.horseSpeedMultiplier=Float.MAX_VALUE;c.enchantments.horseJump.jumpStrengthMultiplier=Float.MAX_VALUE;
   var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,255));e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,Riding.LEAPING,255));var p=rider(h,e);RidingEffects.tick(e);
   close(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED).amount(),3,h,"Forged level/per-level speed bounded");close(e.getAttribute(Attributes.JUMP_STRENGTH).getModifier(RidingEffects.JUMP).amount(),1.5,h,"Forged jump level/per-level bounded");
   c.safety.maximumSpeedBonus=.4f;c.safety.maximumJumpBonus=.3f;RidingEffects.tick(e);close(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED).amount(),.4,h,"Server total speed cap");close(e.getAttribute(Attributes.JUMP_STRENGTH).getModifier(RidingEffects.JUMP).amount(),.3,h,"Server total jump cap");
   c.enchantments.swiftRide.horseSpeedMultiplier=Float.NaN;RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED)==null,"Nonfinite runtime speed refused");p.stopRiding();
   var n=h.spawn(EntityTypes.NAUTILUS,2,2,2);n.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,255));n.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_NAUTILUS_ARMOR,Riding.LEAPING,255));var r=rider(h,n);c.enchantments.swiftRide.nautilusSpeedMultiplier=100;c.enchantments.horseJump.nautilusDashMultiplier=100;RidingEffects.tick(n);
   double tail=n.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED).amount();h.assertTrue((1+tail)*RidingEffects.dashScale(n,100)<=1.40001,"Combined dash and Tailwind respects total cap");close(RidingEffects.dashScale(n,Float.NaN),0,h,"Nonfinite charge refused");n.setDeltaMovement(new Vec3(100,100,100));((com.simpleriding.mixin.NautilusDashAccess)n).simpleriding$executeDash(100,r);h.assertTrue(n.getDeltaMovement().length()<=3.90001,"Server dash clamps existing momentum plus forged scale");
   n.setDeltaMovement(Vec3.ZERO);n.setDeltaMovement(new Vec3(Double.NaN,0,0));((com.simpleriding.mixin.NautilusDashAccess)n).simpleriding$executeDash(Float.NaN,r);h.assertTrue(n.getDeltaMovement().equals(Vec3.ZERO),"Vanilla refuses nonfinite momentum and server refuses nonfinite dash charge");r.stopRiding();
  }finally{Riding.CONFIG=original;}h.succeed();
 }
 public static void featureSwitches(GameTestHelper h){
  var original=Riding.CONFIG;try{
   var c=new RidingConfig();Riding.CONFIG=c;var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,3));e.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_HORSE_ARMOR,Riding.LEAPING,3));var p=rider(h,e);RidingEffects.tick(e);
   c.safety.enableTailwind=false;c.safety.enableLeaping=false;RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED)==null&&e.getAttribute(Attributes.JUMP_STRENGTH).getModifier(RidingEffects.JUMP)==null,"Feature switches clear existing bonuses");
   c.safety.enableArmorUtilities=false;h.assertTrue(!ench(h,Enchantments.PROTECTION).value().canEnchant(new ItemStack(Items.DIAMOND_HORSE_ARMOR)),"Armor utility switch rejects new utility enchants");p.stopRiding();
   var n=h.spawn(EntityTypes.NAUTILUS,2,2,2);n.setItemSlot(EquipmentSlot.SADDLE,enchanted(h,Items.SADDLE,Riding.TAILWIND,3));n.setItemSlot(EquipmentSlot.BODY,enchanted(h,Items.DIAMOND_NAUTILUS_ARMOR,Riding.LEAPING,3));var r=rider(h,n);c.safety.enableTailwind=true;c.safety.enableLeaping=true;c.safety.enableNautilus=false;RidingEffects.tick(n);h.assertTrue(n.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED)==null,"Nautilus switch refuses speed");close(RidingEffects.dashScale(n,1),1,h,"Nautilus switch refuses dash bonus");r.stopRiding();
  }finally{Riding.CONFIG=original;}h.succeed();
 }
 private static void leaves(Class<?> type,String prefix,Set<String> out){for(var f:type.getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){String p=prefix+f.getName();if(f.getType().isPrimitive())out.add(p);else leaves(f.getType(),p+".",out);}}
 public static void allConfigBounds(GameTestHelper h){
  Set<String> fields=new HashSet<>();leaves(RidingConfig.class,"",fields);h.assertTrue(fields.equals(RidingOptions.ALL.stream().map(RidingOptions.Option::path).collect(java.util.stream.Collectors.toSet())),"Every config leaf appears in GUI catalogue");
  for(var option:RidingOptions.ALL){
   var c=new RidingConfig();h.assertTrue(option.get(c).equals(option.defaultValue()),"Real default matches GUI: "+option.path());
   if(option.kind()==RidingOptions.Kind.BOOLEAN)continue;
   if(option.kind()==RidingOptions.Kind.INTEGER){option.set(c,Integer.MAX_VALUE);c.normalize();close(((Number)option.get(c)).doubleValue(),option.maximum(),h,"Integer upper cap "+option.path());option.set(c,Integer.MIN_VALUE);}else{option.set(c,Float.MAX_VALUE);c.normalize();close(((Number)option.get(c)).doubleValue(),option.maximum(),h,"Float upper cap "+option.path());option.set(c,-Float.MAX_VALUE);}
   c.normalize();close(((Number)option.get(c)).doubleValue(),option.minimum(),h,"Lower cap "+option.path());
   if(option.kind()==RidingOptions.Kind.FLOAT)for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){option.set(c,bad);c.normalize();h.assertTrue(option.get(c).equals(option.defaultValue()),"Nonfinite fallback "+option.path());}
  }
  var c=new RidingConfig();c.safety=null;c.worldGen=null;c.enchantments=null;c.normalize();h.assertTrue(c.safety!=null&&c.enchantments.horseJump!=null,"Null sections restored");h.succeed();
 }
 private static net.minecraft.core.PositionAndRotation claim(Vec3 v){return net.minecraft.core.PositionAndRotation.of(v,0,0);}
 public static void movementPackets(GameTestHelper h){
  var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));var p=serverRider(h,e);p.connection.tick();var before=e.position();
  p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(claim(before.add(.1,0,0)),true));
  close(e.getX(),before.x+.1,h,"Real handler accepts normal movement");before=e.position();
  for(var v:List.of(before.add(1000,0,0),before.add(0,100,0),new Vec3(Double.NaN,0,0),new Vec3(Double.POSITIVE_INFINITY,0,0))){p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(claim(v),true));h.assertTrue(e.position().equals(before),"Real packet handler refuses invalid movement");}
  p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(net.minecraft.core.PositionAndRotation.of(before,Float.NaN,0),false));h.assertTrue(e.position().equals(before),"Nonfinite steering refused");
  var security=new RidingSecurity();h.assertTrue(security.acceptMove(p,claim(before.add(.1,0,0))),"Normal movement accepted");h.assertTrue(!security.acceptMove(p,claim(before.add(3,0,0))),"Attribute envelope refuses speed spoof below absolute limit");p.stopRiding();h.assertTrue(!security.acceptJump(p,p.getId(),100),"Dismounted jump refused");h.succeed();
 }
 public static void jumpPackets(GameTestHelper h){
  var e=h.spawn(EntityTypes.NAUTILUS,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));var p=serverRider(h,e);
  for(int charge:new int[]{-1,0,101,Integer.MAX_VALUE,Integer.MIN_VALUE}){p.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(p,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,charge));h.assertTrue(!e.isDashing()&&e.getJumpCooldown()==0,"Real packet refuses invalid charge "+charge);}
  p.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(e,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,100));h.assertTrue(!e.isDashing(),"Forged command entity ID refused");
  var security=new RidingSecurity();h.assertTrue(security.acceptJump(p,p.getId(),100),"Valid server-controlled jump accepted");h.assertTrue(!security.acceptJump(p,p.getId(),100),"Same-tick replay refused");e.setItemSlot(EquipmentSlot.SADDLE,ItemStack.EMPTY);h.assertTrue(!new RidingSecurity().acceptJump(p,p.getId(),100),"Unsaddled jump refused");p.stopRiding();h.succeed();
 }
 public static void movementBudget(GameTestHelper h){
  var original=Riding.CONFIG;try{
   Riding.CONFIG=new RidingConfig();Riding.CONFIG.safety.movementDistancePerTick=.5f;Riding.CONFIG.safety.movementPacketsPerTick=2;
   var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));var p=serverRider(h,e);var guard=new RidingSecurity();
   h.assertTrue(guard.acceptMove(p,claim(e.position().add(.3,0,0))),"First packet within budget");h.assertTrue(!guard.acceptMove(p,claim(e.position().add(.3,0,0))),"Cumulative movement cap refuses second packet");h.assertTrue(!guard.acceptMove(p,claim(e.position())),"Packet spam cap refuses third packet");
   p.stopRiding();p.startRiding(e,true,false);h.assertTrue(!guard.acceptMove(p,claim(e.position())),"Dismount/remount does not refill budget");h.assertTrue(guard.shouldCorrect(p)&&!guard.shouldCorrect(p),"Rejected packet spam produces at most one correction per server tick");p.stopRiding();
  }finally{Riding.CONFIG=original;}h.succeed();
 }

 public static void weightedDataBounds(GameTestHelper h){
  var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
  String valid="{\"pool\":[{\"enchantment\":\"simpleriding:tailwind\",\"level\":3,\"weight\":20}],\"second_chance\":0.5}";
  var codec=WeightedEnchantFunction.MAP_CODEC.codec();h.assertTrue(codec.parse(ops,JsonParser.parseString(valid)).result().isPresent(),"Valid weighted data loads");
  for(String bad:List.of(valid.replace("\"level\":3","\"level\":255"),valid.replace("\"weight\":20","\"weight\":2147483647"),valid.replace("\"weight\":20","\"weight\":-1"),valid.replace("0.5","2.0"),"{\"pool\":[]}"))h.assertTrue(codec.parse(ops,JsonParser.parseString(bad)).error().isPresent(),"Out-of-range loot/trade data refused");
  var oversized=JsonParser.parseString(valid).getAsJsonObject();var pool=oversized.getAsJsonArray("pool");var entry=pool.get(0);for(int i=1;i<65;i++)pool.add(entry);h.assertTrue(codec.parse(ops,oversized).error().isPresent(),"Bounded pool prevents oversized selections");h.succeed();
 }
 public static void steeringAndBorder(GameTestHelper h){
  var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));var p=serverRider(h,e);var guard=new RidingSecurity();
  h.assertTrue(!guard.acceptMove(p,net.minecraft.core.PositionAndRotation.of(e.position(),0,91)),"Impossible steering pitch refused");
  h.assertTrue(!guard.acceptMove(p,claim(e.position().add(0,1.5,0))),"Uncommanded leap/flying claim refused");
  var border=h.getLevel().getWorldBorder();double x=border.getCenterX(),z=border.getCenterZ(),size=border.getSize();try{
   border.setCenter(e.getX(),e.getZ());border.setSize(e.getBbWidth()+.05);
   h.assertTrue(!guard.acceptMove(p,claim(e.position().add(.1,0,0))),"Full mount bounding box cannot cross world border");
  }finally{border.setCenter(x,z);border.setSize(size);}
  p.stopRiding();
  var ghast=h.spawn(EntityTypes.HAPPY_GHAST,2,2,2);ghast.setItemSlot(EquipmentSlot.BODY,enchanted(h,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_harness")),Riding.TAILWIND,3));var pilot=serverRider(h,ghast);RidingEffects.tick(ghast);
  h.assertTrue(new RidingSecurity().acceptMove(pilot,claim(ghast.position().add(.6,.9,0))),"Combined ascent and strafe remains legal within server flight cap");pilot.stopRiding();h.succeed();
 }
 public static void allMountSpeedCaps(GameTestHelper h){
  var original=Riding.CONFIG;try{
   var c=new RidingConfig();Riding.CONFIG=c;c.enchantments.swiftRide.horseSpeedMultiplier=100;c.enchantments.swiftRide.otherSpeedMultiplier=100;c.enchantments.swiftRide.ghastSpeedMultiplier=100;c.enchantments.swiftRide.nautilusSpeedMultiplier=100;
   for(var type:List.of(EntityTypes.HORSE,EntityTypes.CAMEL,EntityTypes.PIG,EntityTypes.STRIDER,EntityTypes.HAPPY_GHAST,EntityTypes.NAUTILUS,EntityTypes.ZOMBIE_NAUTILUS)){
    var e=h.spawn(type,2,2,2);var equipment=type==EntityTypes.HAPPY_GHAST?EquipmentSlot.BODY:EquipmentSlot.SADDLE;var item=type==EntityTypes.HAPPY_GHAST?BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_harness")):Items.SADDLE;
    e.setItemSlot(equipment,enchanted(h,item,Riding.TAILWIND,255));var p=rider(h,e);RidingEffects.tick(e);close(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED).amount(),3,h,"Each mount caps forged Tailwind: "+type);
    if(type==EntityTypes.HAPPY_GHAST)close(Math.pow(1+e.getAttribute(Attributes.FLYING_SPEED).getModifier(RidingEffects.SPEED).amount(),2),4,h,"Actual quadratic flight cap");
    p.stopRiding();RidingEffects.tick(e);h.assertTrue(e.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(RidingEffects.SPEED)==null,"Dismount speed cleanup for "+type);e.discard();
   }
  }finally{Riding.CONFIG=original;}h.succeed();
 }


 public static void camelDashBounds(GameTestHelper h){
  h.setBlock(2,1,2,net.minecraft.world.level.block.Blocks.STONE);
  var e=h.spawn(EntityTypes.CAMEL,2,2,2);e.setNoAi(true);e.setNoGravity(true);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));var p=serverRider(h,e);var guard=new RidingSecurity();
  // A fresh Camel starts in Vanilla's 52-tick standing transition at game time zero.
  h.runAfterDelay(55,()->{
   h.assertTrue(guard.acceptJump(p,p.getId(),100),"Grounded camel dash authorized");
   h.assertTrue(guard.acceptMove(p,claim(e.position().add(1.5,.3,0))),"Server envelope accepts normal camel dash");
   h.assertTrue(!guard.acceptMove(p,claim(e.position().add(3.9,.3,0))),"Forged camel dash beyond server impulse refused");
   p.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(p,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,100));
   h.assertTrue(e.getJumpCooldown()==55,"Real camel packet retains Vanilla cooldown on server");
   h.runAfterDelay(1,()->{h.assertTrue(!guard.acceptJump(p,p.getId(),100)&&e.getJumpCooldown()>0,"Later replay during camel cooldown refused");p.stopRiding();h.succeed();});
  });
 }

 public static void groundFlags(GameTestHelper h){
  h.setBlock(2,1,2,net.minecraft.world.level.block.Blocks.STONE);
  var e=h.spawn(EntityTypes.HORSE,2,2,2);e.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));var p=serverRider(h,e);p.connection.tick();
  p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(claim(e.position().add(.1,0,0)),false));
  h.assertTrue(e.onGround(),"Real landing is server-owned even when client claims airborne");
  e.snapTo(e.getX(),e.getY()+3,e.getZ(),0,0);e.setOnGround(false);p.connection.tick();
  p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(claim(e.position().add(.1,0,0)),true));
  h.assertTrue(!e.onGround(),"Forged on-ground flag cannot reset riding fall state in midair");p.stopRiding();h.succeed();
 }
}
