package com.simplebuilding.modules.simplefarming;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Shared bodies (moved from Simple QoL); Fabric wraps them in ModuleGameTest, NeoForge/Forge register {@link #ALL}. */
public final class FarmingTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("launch",FarmingTests::launch);ALL.put("config",FarmingTests::config);ALL.put("qol_migration",FarmingTests::qolMigration);ALL.put("super_switch",FarmingTests::superSwitch);
  ALL.put("hoe_switch",FarmingTests::hoeSwitch);ALL.put("cross_mod",FarmingTests::crossMod);ALL.put("sync",FarmingTests::sync);
  ALL.put("farmland",FarmingTests::farmland);
  ALL.put("hoe_harvest",FarmingTests::hoeHarvest);
  ALL.put("vegetation",FarmingTests::vegetation);
  ALL.put("sharpness_action",FarmingTests::sharpnessAction);
  ALL.put("grass_through",FarmingTests::grassThrough);
  // ALL-END
 }
 private FarmingTests(){}
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath(SimpleFarming.MOD_ID,s);}
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key){var s=new ItemStack(item);s.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),3);return s;}
 private static void configured(Runnable body){var old=SimpleFarming.CONFIG;try{SimpleFarming.CONFIG=new FarmingConfig();body.run();}finally{SimpleFarming.CONFIG=old;SubMods.set(SimpleFarming.MOD_ID,true);}}
 public static void launch(GameTestHelper h){h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(SimpleFarming.MOD_ID)!=null,"/simplefarming registered");h.succeed();}
 public static void config(GameTestHelper h){var c=new FarmingConfig();h.assertTrue(c.preventFarmlandTrampleWithFeatherFalling&&c.sharpnessCutsGrass&&c.enableHoeHarvest,"defaults like Simple QoL");h.succeed();}
 /** First start: the old qOL values are taken over once; an existing own file is never overwritten. */
 public static void qolMigration(GameTestHelper h){try{var dir=Files.createTempDirectory("simplefarming-migration");Files.writeString(dir.resolve("simplequalityoflife.json"),"{\"qOL\":{\"enableHoeHarvest\":false,\"sharpnessCutsGrass\":false,\"enableAutowalk\":true}}");var taken=SimpleFarming.read(dir);h.assertTrue(!taken.enableHoeHarvest&&!taken.sharpnessCutsGrass&&taken.preventFarmlandTrampleWithFeatherFalling,"old QoL values taken over, others default");h.assertTrue(Files.isRegularFile(dir.resolve("simplefarming.json")),"own file written as marker");Files.writeString(dir.resolve("simplefarming.json"),"{\"enableHoeHarvest\":true}");var own=SimpleFarming.read(dir);h.assertTrue(own.enableHoeHarvest&&own.sharpnessCutsGrass,"own file wins over QoL");var empty=SimpleFarming.read(Files.createTempDirectory("simplefarming-empty"));h.assertTrue(empty.enableHoeHarvest&&empty.sharpnessCutsGrass,"no file: defaults");}catch(java.io.IOException e){h.fail("migration io: "+e);}h.succeed();}
 /** "Enable Simple Farming" off in the super-mod: hoe, farmland and grass behave like Vanilla. */
 public static void superSwitch(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FEATHER_FALLING));h.assertTrue(Protection.farmland(p),"protected while on");SubMods.set(SimpleFarming.MOD_ID,false);h.assertTrue(!Protection.farmland(p),"switched-off sub-mod: Vanilla trampling");var abs=h.absolutePos(new BlockPos(2,2,2));h.setBlock(new BlockPos(2,2,2),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_HOE));h.assertTrue(HoeHarvestHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,abs,Direction.UP)==InteractionResult.PASS,"switched-off sub-mod: no hoe harvest");});h.succeed();}
 public static void hoeSwitch(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var abs=h.absolutePos(new BlockPos(2,2,2));h.setBlock(new BlockPos(2,2,2),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_HOE));SimpleFarming.config().enableHoeHarvest=false;h.assertTrue(HoeHarvestHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,abs,Direction.UP)==InteractionResult.PASS,"Harvest toggle refuses action");});h.succeed();}
 public static void crossMod(GameTestHelper h){if(partnerMissing(h,"simplebuilding","Simple Farming cross-mod checks"))return;configured(()->{
  var farm=new BlockPos(3,2,2);h.setBlock(farm,Blocks.FARMLAND);var p=h.makeMockPlayer(GameType.CREATIVE);p.setPos(Vec3.atCenterOf(h.absolutePos(farm.above())));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:breeze_head"))));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FEATHER_FALLING));var absolute=h.absolutePos(farm);Blocks.FARMLAND.fallOn(h.getLevel(),h.getLevel().getBlockState(absolute),absolute,p,100);h.assertTrue(h.getLevel().getBlockState(absolute).is(Blocks.FARMLAND),"Both farmland protections coexist");SimpleFarming.config().preventFarmlandTrampleWithFeatherFalling=false;Blocks.FARMLAND.fallOn(h.getLevel(),h.getLevel().getBlockState(absolute),absolute,p,100);h.assertTrue(h.getLevel().getBlockState(absolute).is(Blocks.FARMLAND),"Disabling Feather Falling preserves Breeze head protection");
 });h.succeed();}
 /** The client predicts with the server values: a valid payload is kept, garbage ignored, leaving forgets it. */
 public static void sync(GameTestHelper h){configured(()->{try{SimpleFarming.config().enableHoeHarvest=false;SimpleFarming.receiveSync(SimpleFarming.syncJson());h.assertTrue(SimpleFarming.syncedConfig()!=null&&!SimpleFarming.syncedConfig().enableHoeHarvest,"server value arrives");SimpleFarming.receiveSync("{not json");h.assertTrue(!SimpleFarming.syncedConfig().enableHoeHarvest,"invalid payload ignored");h.assertTrue(SimpleFarming.configFor(h.getLevel())==SimpleFarming.config(),"server level reads the server values");}finally{SimpleFarming.clearSync();}});h.assertTrue(SimpleFarming.syncedConfig()==null,"cleared");h.succeed();}
// MOVED-BEGIN
 public static void farmland(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FEATHER_FALLING));h.assertTrue(Protection.farmland(p),"Feather Falling protects");SimpleFarming.config().preventFarmlandTrampleWithFeatherFalling=false;h.assertTrue(!Protection.farmland(p),"Farmland toggle");});h.succeed();}
 public static void hoeHarvest(GameTestHelper h){configured(()->{
  Block[] blocks={Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS,Blocks.NETHER_WART,Blocks.COCOA};
  for(int i=0;i<blocks.length;i++){var pos=new BlockPos(2,2,2);var b=blocks[i];var state=b instanceof CropBlock c?c.getStateForAge(c.getMaxAge()):b==Blocks.NETHER_WART?b.defaultBlockState().setValue(NetherWartBlock.AGE,3):b.defaultBlockState().setValue(CocoaBlock.AGE,2);
   h.setBlock(pos.below(),b==Blocks.NETHER_WART?Blocks.SOUL_SAND:Blocks.FARMLAND);if(b==Blocks.COCOA)h.setBlock(pos.south(),Blocks.JUNGLE_LOG);h.setBlock(pos,state);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_HOE));var absolute=h.absolutePos(pos);
   h.assertTrue(HoeHarvestHandler.wouldHarvest(p,InteractionHand.MAIN_HAND,absolute)&&!HoeHarvestHandler.wouldHarvest(p,InteractionHand.OFF_HAND,absolute),"Hand hint predicate matches the ripe "+b);
   h.assertTrue(com.simplebuilding.framework.api.TransformHints.any(new com.simplebuilding.framework.api.TransformHints.Query(h.getLevel(),p,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),Direction.UP,absolute,false),true)),"Framework hand hint published for "+b);
   h.assertTrue(HoeHarvestHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,absolute,Direction.UP)==InteractionResult.SUCCESS,"Harvest "+b);
   h.assertTrue(!HoeHarvestHandler.wouldHarvest(p,InteractionHand.MAIN_HAND,absolute),"No hand hint on the replanted "+b);
   var after=h.getLevel().getBlockState(absolute);h.assertTrue(after.is(b)&&!(b instanceof CropBlock c&&c.isMaxAge(after)),"Replanted "+b);h.assertTrue(p.getMainHandItem().getDamageValue()==1,"Exactly one durability cost");
   h.assertTrue(HoeHarvestHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,absolute,Direction.UP)==InteractionResult.PASS,"No repeated immature harvest");
  }
 });h.succeed();}
 public static void vegetation(GameTestHelper h){h.assertTrue(VegetationUtil.isCuttable(Blocks.SHORT_GRASS.defaultBlockState())&&VegetationUtil.isCuttable(Blocks.DANDELION.defaultBlockState()),"Grass/flowers supported");h.assertTrue(!VegetationUtil.isCuttable(Blocks.WATER.defaultBlockState())&&!VegetationUtil.isCuttable(Blocks.STONE.defaultBlockState()),"Fluids and building blocks excluded");h.succeed();}
 public static void sharpnessAction(GameTestHelper h){configured(()->{var pos=new BlockPos(2,2,2);h.setBlock(pos.below(),Blocks.DIRT);h.setBlock(pos,Blocks.SHORT_GRASS);var target=h.spawn(EntityTypes.COW,2,2,2);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,enchanted(h,Items.DIAMOND_SWORD,Enchantments.SHARPNESS));var old=InteractionGuard.permission;try{InteractionGuard.permission=(a,b)->false;p.attack(target);h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).is(Blocks.SHORT_GRASS),"Actual attack respects claim veto");p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,enchanted(h,Items.DIAMOND_SWORD,Enchantments.SHARPNESS));InteractionGuard.permission=(a,b)->true;p.attack(target);h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).isAir(),"Actual Sharpness attack cuts grass");}finally{InteractionGuard.permission=old;}});h.succeed();}
/** Swing-through chance 10 % (Sharpness I) to 90 % (V), linear; Sweeping Edge widens the cut area from 3x3 by a ring per level; the roll is stable and respects the percentage. */
 public static void grassThrough(GameTestHelper h){var c=new FarmingConfig();
  int[] want={10,30,50,70,90};for(int l=1;l<=5;l++)h.assertTrue(VegetationUtil.chancePercent(l,c)==want[l-1],"Sharpness "+l+" chance "+want[l-1]);
  h.assertTrue(VegetationUtil.chancePercent(0,c)==0&&VegetationUtil.chancePercent(7,c)==90,"none below I, capped at V");
  h.assertTrue(VegetationUtil.cutRadius(0,c)==1&&VegetationUtil.cutRadius(1,c)==2&&VegetationUtil.cutRadius(3,c)==4,"3x3 plus one ring per Sweeping Edge level");
  h.assertTrue(!VegetationUtil.passes(0,5L,1,100L)&&VegetationUtil.passes(100,5L,1,100L),"0 never, 100 always");
  int hits=0;for(int i=0;i<2000;i++)if(VegetationUtil.passes(30,i,1,100L))hits++;h.assertTrue(hits>480&&hits<720,"30 % rolls near 30 %, got "+hits);
  h.assertTrue(VegetationUtil.passes(30,42L,1,100L)==VegetationUtil.passes(30,42L,1,105L),"roll is stable inside a half second");
  c.grassThroughMinPercent=80;c.grassThroughMaxPercent=20;c.normalizeNumbers();h.assertTrue(c.grassThroughMaxPercent>=c.grassThroughMinPercent,"normalized");
  configured(()->{var pos=new BlockPos(2,2,2);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,enchanted(h,Items.DIAMOND_AXE,Enchantments.SHARPNESS));
   h.setBlock(pos.below(),Blocks.DIRT);h.setBlock(pos,Blocks.SHORT_GRASS);h.setBlock(pos.east(),Blocks.SHORT_GRASS);h.setBlock(pos.east().east(),Blocks.SHORT_GRASS);var target=h.spawn(EntityTypes.COW,2,2,2);
   var old=InteractionGuard.permission;try{InteractionGuard.permission=(a,b)->true;p.attack(target);}finally{InteractionGuard.permission=old;}
   h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos.east())).isAir(),"axe cuts the 3x3 around the target");
   h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos.east().east())).is(Blocks.SHORT_GRASS),"outside 3x3 without Sweeping Edge stays");});
  h.succeed();}
// MOVED-END
}
