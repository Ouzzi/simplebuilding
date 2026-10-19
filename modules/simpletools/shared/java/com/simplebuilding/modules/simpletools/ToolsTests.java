package com.simplebuilding.modules.simpletools;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3;

/** Shared bodies (moved from Simple QoL); Fabric wraps them in ModuleGameTest, NeoForge/Forge register {@link #ALL}. */
public final class ToolsTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("launch",ToolsTests::launch);ALL.put("config",ToolsTests::config);ALL.put("qol_migration",ToolsTests::qolMigration);ALL.put("super_switch",ToolsTests::superSwitch);
  ALL.put("lava_switch",ToolsTests::lavaSwitch);ALL.put("sync",ToolsTests::sync);
  ALL.put("durability",ToolsTests::durability);
  ALL.put("thrift",ToolsTests::thrift);
  ALL.put("anvil_repair_cost",ToolsTests::anvilRepairCost);
  ALL.put("furnace_lava",ToolsTests::furnaceLava);
  ALL.put("permissions_spam",ToolsTests::permissionsSpam);
  ALL.put("cross_mod",ToolsTests::crossMod);
  ALL.put("soulbound_registered",DeathTests::soulboundRegistered);ALL.put("soulbound_death",DeathTests::soulboundDeath);ALL.put("soulbound_switch",DeathTests::soulboundSwitch);ALL.put("soulbound_hardcore",DeathTests::soulboundHardcore);ALL.put("death_durability_cap",DeathTests::durabilityCap);ALL.put("soulbound_anvil",DeathTests::soulboundAnvil);ALL.put("soulbound_grindstone",DeathTests::soulboundGrindstone);ALL.put("soulbound_sources",DeathTests::soulboundSources);ALL.put("vault_lying",DeathTests::vaultLying);ALL.put("vault_lost",DeathTests::vaultLost);ALL.put("vault_taken",DeathTests::vaultTaken);ALL.put("vault_pvp",DeathTests::vaultPvp);ALL.put("vault_weights",DeathTests::vaultWeights);ALL.put("vault_dupe",DeathTests::vaultDupe);ALL.put("vault_persistence",DeathTests::vaultPersistence);ALL.put("vault_fates",DeathTests::vaultFates);ALL.put("vault_last_death",DeathTests::vaultLastDeath);ALL.put("vault_menu_rows",DeathTests::vaultMenuRows);
  ALL.put("frame_invisible",FrameTests::invisible);ALL.put("frame_empty",FrameTests::empty);ALL.put("frame_reverse",FrameTests::reverse);ALL.put("frame_lock",FrameTests::lock);ALL.put("frame_glow",FrameTests::glow);ALL.put("frame_glass",FrameTests::glass);ALL.put("frame_migrate",FrameTests::migrate);ALL.put("frame_reveal",FrameTests::reveal);ALL.put("frame_config_off",FrameTests::configOff);
  // ALL-END
 }
 private ToolsTests(){}
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID,s);}
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 private static void close(GameTestHelper h,double a,double b,String msg){h.assertTrue(Math.abs(a-b)<0.00001,msg+": "+a+" / "+b);}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key){var s=new ItemStack(item);s.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),3);return s;}
 private static void configured(Runnable body){var old=SimpleTools.CONFIG;try{SimpleTools.CONFIG=new ToolsConfig();body.run();}finally{SimpleTools.CONFIG=old;SubMods.set(SimpleTools.MOD_ID,true);}}
 public static void launch(GameTestHelper h){h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(SimpleTools.MOD_ID)!=null,"/simpletools registered");h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Thrift.KEY).isPresent(),"Thrift keeps its id simplequalityoflife:thrift");h.succeed();}
 public static void config(GameTestHelper h){var c=new ToolsConfig();h.assertTrue(c.enableFullDurabilityBonus&&c.anvilRepairKeepsCost&&c.enableFurnaceLavaFill,"defaults like Simple QoL");close(h,c.fullDurabilityThreshold,.8,"threshold default");close(h,c.fullDurabilityBonusMultiplier,1,"bonus default");c.fullDurabilityBonusMultiplier=Double.POSITIVE_INFINITY;c.fullDurabilityThreshold=-5;c.normalize();close(h,c.fullDurabilityBonusMultiplier,1,"Nonfinite bonus default");close(h,c.fullDurabilityThreshold,.8,"Threshold lower bound");c.fullDurabilityBonusMultiplier=999;c.normalize();close(h,c.fullDurabilityBonusMultiplier,1.5,"Bonus hard cap");h.succeed();}
 /** First start: the old qOL values are taken over once; an existing own file is never overwritten. */
 public static void qolMigration(GameTestHelper h){try{var dir=Files.createTempDirectory("simpletools-migration");Files.writeString(dir.resolve("simplequalityoflife.json"),"{\"qOL\":{\"fullDurabilityBonusMultiplier\":1.25,\"anvilRepairKeepsCost\":false,\"enableAutowalk\":true}}");var taken=SimpleTools.read(dir);close(h,taken.fullDurabilityBonusMultiplier,1.25,"old bonus taken over");h.assertTrue(!taken.anvilRepairKeepsCost&&taken.enableFurnaceLavaFill,"old QoL values taken over, others default");h.assertTrue(Files.isRegularFile(dir.resolve("simpletools.json")),"own file written as marker");Files.writeString(dir.resolve("simpletools.json"),"{\"anvilRepairKeepsCost\":true}");var own=SimpleTools.read(dir);h.assertTrue(own.anvilRepairKeepsCost,"own file wins over QoL");close(h,own.fullDurabilityBonusMultiplier,1,"missing key defaults");var empty=SimpleTools.read(Files.createTempDirectory("simpletools-empty"));h.assertTrue(empty.enableFurnaceLavaFill,"no file: defaults");}catch(java.io.IOException e){h.fail("migration io: "+e);}h.succeed();}
 /** "Enable Simple Tools" off in the super-mod: furnace and anvil behave like Vanilla; Thrift stays (bound to item data). */
 public static void superSwitch(GameTestHelper h){configured(()->{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.FURNACE);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));SubMods.set(SimpleTools.MOD_ID,false);h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,h.absolutePos(pos),Direction.UP)==InteractionResult.PASS,"switched-off sub-mod: no lava fill");var thrift=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Thrift.KEY);var s=new ItemStack(Items.IRON_PICKAXE);int base=s.getMaxDamage();s.enchant(thrift,3);h.assertTrue(s.getMaxDamage()==2*base,"Thrift keeps working: enchanted items never lose capacity");});h.succeed();}
 public static void lavaSwitch(GameTestHelper h){configured(()->{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.FURNACE);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));SimpleTools.config().enableFurnaceLavaFill=false;h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,h.absolutePos(pos),Direction.UP)==InteractionResult.PASS,"Lava toggle refuses action");});h.succeed();}
 /** The client predicts with the server values: a valid payload is kept, garbage ignored, leaving forgets it. */
 public static void sync(GameTestHelper h){configured(()->{try{SimpleTools.config().fullDurabilityBonusMultiplier=1.25;SimpleTools.receiveSync(SimpleTools.syncJson());h.assertTrue(SimpleTools.syncedConfig()!=null,"server value arrives");close(h,SimpleTools.syncedConfig().fullDurabilityBonusMultiplier,1.25,"synced bonus");SimpleTools.receiveSync("{not json");close(h,SimpleTools.syncedConfig().fullDurabilityBonusMultiplier,1.25,"invalid payload ignored");h.assertTrue(SimpleTools.configFor(h.getLevel())==SimpleTools.config(),"server level reads the server values");}finally{SimpleTools.clearSync();}});h.assertTrue(SimpleTools.syncedConfig()==null,"cleared");h.succeed();}
// MOVED-BEGIN
 public static void durability(GameTestHelper h){configured(()->{
  var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);
  var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);
  var hammer=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplehammers:diamond_sledgehammer")));
  // A component-defined tool models third-party stacks without a Vanilla tool subclass.
  var foreign=new ItemStack(Items.STICK);foreign.set(DataComponents.MAX_STACK_SIZE,1);
  foreign.set(DataComponents.MAX_DAMAGE,400);foreign.set(DataComponents.DAMAGE,0);
  foreign.set(DataComponents.TOOL,vanilla.get(DataComponents.TOOL));
  var c=SimpleTools.config();
  close(h,c.fullDurabilityBonusMultiplier,1,"Fresh server default has no bonus");
  // The sledgehammer joins only when Simple Hammers is loaded (standalone: Vanilla and component tools).
  for(var stack:isModLoaded("simplehammers")?List.of(vanilla,new ItemStack(Items.DIAMOND_SWORD),hammer,foreign):List.of(vanilla,new ItemStack(Items.DIAMOND_SWORD),foreign)){
   h.assertTrue(stack.isDamageableItem(),"All tool kinds are damageable");
   p.setItemSlot(EquipmentSlot.MAINHAND,stack);c.enableFullDurabilityBonus=false;
   double damage=p.getAttributeValue(Attributes.ATTACK_DAMAGE);float mining=p.getDestroySpeed(Blocks.STONE.defaultBlockState());
   int max=stack.getMaxDamage(),wear=stack.getDamageValue();c.enableFullDurabilityBonus=true;
   double[] inputs={1,1.25,1.5,999,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1};
   double[] expected={1,1.25,1.5,1.5,1,1,1,1};
   for(int i=0;i<inputs.length;i++){
    c.fullDurabilityBonusMultiplier=inputs[i];
    close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),damage*expected[i],"Actual server damage hook "+stack+" / "+inputs[i]);
    close(h,p.getDestroySpeed(Blocks.STONE.defaultBlockState()),mining*expected[i],"Actual server mining hook "+stack+" / "+inputs[i]);
    h.assertTrue(stack.getMaxDamage()==max&&stack.getDamageValue()==wear,"Bonus never changes durability capacity or wear");
   }
   c.fullDurabilityBonusMultiplier=1.5;stack.setDamageValue(max/2);
   close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),damage,"Worn item loses damage bonus");
   close(h,p.getDestroySpeed(Blocks.STONE.defaultBlockState()),mining,"Worn item loses mining bonus");
  }
  var gson=new com.google.gson.Gson();
  for(String json:List.of("{}","{\"anvilRepairKeepsCost\":true}","{\"fullDurabilityBonusMultiplier\":1.5}")){
   var loaded=gson.fromJson(json,ToolsConfig.class);loaded.normalize();
   close(h,loaded.fullDurabilityBonusMultiplier,json.contains("1.5")?1.5:1,"Saved values retained; absent key defaults to one");
  }
 });h.succeed();}
 /** Sparsamkeit: +1/3 Haltbarkeit je Stufe (III = doppelt), nicht mit Reparatur kombinierbar, Materialreparatur je Stueck ein Viertel. */
 public static void thrift(GameTestHelper h){var reg=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var thrift=reg.getOrThrow(Thrift.KEY);
  var plain=new ItemStack(Items.IRON_PICKAXE);int base=plain.getMaxDamage();
  for(int lvl=1;lvl<=3;lvl++){var s=new ItemStack(Items.IRON_PICKAXE);s.enchant(thrift,lvl);h.assertTrue(s.getMaxDamage()==base+base*lvl/3,"thrift "+lvl+" max damage "+s.getMaxDamage()+" from "+base);}
  var three=new ItemStack(Items.IRON_PICKAXE);three.enchant(thrift,3);h.assertTrue(three.getMaxDamage()==2*base,"thrift III doubles the durability");
  h.assertTrue(!Enchantment.areCompatible(thrift,reg.getOrThrow(Enchantments.MENDING)),"thrift and mending are exclusive");
  h.assertTrue(Enchantment.areCompatible(thrift,reg.getOrThrow(Enchantments.UNBREAKING)),"thrift goes with unbreaking");
  h.assertTrue(thrift.value().getMaxLevel()==3,"three levels");
  var p=h.makeMockPlayer(GameType.CREATIVE);var menu=new net.minecraft.world.inventory.AnvilMenu(1,p.getInventory());three.setDamageValue(three.getMaxDamage()-1);
  menu.getSlot(0).set(three.copy());menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT,4));var fixed=menu.getSlot(2).getItem();
  h.assertTrue(!fixed.isEmpty()&&fixed.getDamageValue()==0,"four ingots fully repair the thrift III pickaxe like an unenchanted one: damage "+fixed.getDamageValue());
  h.succeed();}
 /** Reparieren ohne neue Verzauberung behaelt die Ambosskosten, Verzaubern erhoeht sie wie Vanilla (2026-10-02). */
 public static void anvilRepairCost(GameTestHelper h){configured(()->{var p=h.makeMockPlayer(GameType.CREATIVE);var menu=new net.minecraft.world.inventory.AnvilMenu(1,p.getInventory());
  var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(sword.getMaxDamage()-1);sword.set(DataComponents.REPAIR_COST,3);
  menu.getSlot(0).set(sword.copy());menu.getSlot(1).set(new ItemStack(Items.DIAMOND));var repaired=menu.getSlot(2).getItem();
  h.assertTrue(!repaired.isEmpty()&&repaired.getDamageValue()<sword.getDamageValue(),"diamond repairs the sword: "+repaired);
  h.assertTrue(repaired.getOrDefault(DataComponents.REPAIR_COST,0)==3,"repair keeps the anvil cost 3, got "+repaired.getOrDefault(DataComponents.REPAIR_COST,0));
  var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(DataComponents.STORED_ENCHANTMENTS,enchanted(h,Items.DIAMOND_SWORD,Enchantments.SHARPNESS).get(DataComponents.ENCHANTMENTS));
  menu.getSlot(1).set(book);var enchantedSword=menu.getSlot(2).getItem();
  h.assertTrue(enchantedSword.getOrDefault(DataComponents.REPAIR_COST,0)==7,"enchanting raises the cost like vanilla (3 -> 7), got "+enchantedSword.getOrDefault(DataComponents.REPAIR_COST,0));
  // SimpleBuilding has the same rule under its own switch; with it loaded the vanilla fallback is not observable here.
  boolean sb=isModLoaded("simplebuilding");
  if(!sb){SimpleTools.config().anvilRepairKeepsCost=false;menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
  h.assertTrue(menu.getSlot(2).getItem().getOrDefault(DataComponents.REPAIR_COST,0)==7,"switched off: vanilla raises the repair cost to 7");}
 });h.succeed();}
 public static void furnaceLava(GameTestHelper h){configured(()->{for(Block b:List.of(Blocks.FURNACE,Blocks.BLAST_FURNACE,Blocks.SMOKER)){var pos=new BlockPos(2,2,2);h.setBlock(pos,b);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));var abs=h.absolutePos(pos);var furnace=(AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(abs);h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,abs,Direction.UP)==InteractionResult.SUCCESS,"Lava fills "+b);h.assertTrue(furnace.getItem(1).is(Items.LAVA_BUCKET)&&p.getMainHandItem().isEmpty(),"One lava bucket transferred, no early empty bucket");
  p=player(h,pos);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.LAVA_BUCKET));furnace.setItem(1,new ItemStack(Items.BUCKET));h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.OFF_HAND,abs,Direction.UP)==InteractionResult.SUCCESS,"Offhand leftover exchange");h.assertTrue(p.getOffhandItem().is(Items.BUCKET)&&p.getOffhandItem().getCount()==1,"Exactly one leftover returned");
 }});h.succeed();}
 public static void permissionsSpam(GameTestHelper h){configured(()->{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.FURNACE);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));var old=InteractionGuard.permission;try{InteractionGuard.permission=(a,b)->false;h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,h.absolutePos(pos),Direction.UP)==InteractionResult.PASS,"Claim refusal honored");h.assertTrue(p.getMainHandItem().is(Items.LAVA_BUCKET),"Refusal consumes nothing");}finally{InteractionGuard.permission=old;}
  h.assertTrue(!InteractionGuard.allow(p,h.absolutePos(new BlockPos(20,2,2))),"Forged distant action refused");var other=player(h,pos);h.assertTrue(InteractionGuard.action(other)&&!InteractionGuard.action(other),"Auto-action spam refused");
 });h.succeed();}
 public static void crossMod(GameTestHelper h){if(partnerMissing(h,"simplebuilding","Simple Tools cross-mod checks"))return;configured(()->{
  var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:reinforced_furnace"));h.assertTrue(block!=Blocks.AIR,"Public SimpleBuilding furnace ID");var pos=new BlockPos(2,2,2);h.setBlock(pos,block);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,h.absolutePos(pos),Direction.UP)==InteractionResult.SUCCESS,"Foreign furnace follows Vanilla fuel interface");h.assertTrue(((AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).getItem(1).is(Items.LAVA_BUCKET),"Foreign fuel slot updated");
  var hammer=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplehammers:diamond_sledgehammer")));h.assertTrue(hammer.isDamageableItem(),"Public foreign damageable tool ID");p.setItemSlot(EquipmentSlot.MAINHAND,hammer);SimpleTools.config().enableFullDurabilityBonus=false;double base=p.getAttributeValue(Attributes.ATTACK_DAMAGE);SimpleTools.config().enableFullDurabilityBonus=true;SimpleTools.config().fullDurabilityBonusMultiplier=1.5;close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),base*1.5,"Foreign tool receives exactly one capped bonus");
 });h.succeed();}
// MOVED-END

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplebuilding.modules.simpletools.discover.ToolsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplebuilding.modules.simpletools.discover.ToolsDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplebuilding.modules.simpletools.discover.ToolsDiscover::firstPickupTips);
        ALL.put("guide_book", com.simplebuilding.modules.simpletools.guide.ToolsGuide::gameTest);
    }
}
