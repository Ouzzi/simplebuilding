package com.simplebuilding.modules.simplemovement;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Shared bodies (moved from Simple QoL); Fabric wraps them in ModuleGameTest, NeoForge/Forge register {@link #ALL}. */
public final class MovementTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("launch",MovementTests::launch);ALL.put("config",MovementTests::config);ALL.put("qol_migration",MovementTests::qolMigration);ALL.put("super_switch",MovementTests::superSwitch);
  ALL.put("key_migration",MovementTests::keyMigration);ALL.put("sync",MovementTests::sync);
  ALL.put("config_bounds",MovementTests::configBounds);
  ALL.put("crawl",MovementTests::crawl);
  ALL.put("climb_packets",MovementTests::climbPackets);
  ALL.put("climb_mechanics",MovementTests::climbMechanics);
  ALL.put("powder_snow",MovementTests::powderSnow);
  ALL.put("real_movement_packets",MovementTests::realMovementPackets);
  ALL.put("feature_switches",MovementTests::featureSwitches);
  ALL.put("enchant_step_height",MovementTests::enchantStepHeight);ALL.put("enchant_sprint",MovementTests::enchantSprint);ALL.put("enchant_treasure_rules",MovementTests::enchantTreasureRules);ALL.put("jump_boost_step",MovementTests::jumpBoostStep);
  // ALL-END
 }
 private MovementTests(){}
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath(SimpleMovement.MOD_ID,s);}
 private static void close(GameTestHelper h,double a,double b,String msg){h.assertTrue(Math.abs(a-b)<0.00001,msg+": "+a+" / "+b);}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key){var s=new ItemStack(item);s.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),3);return s;}
 private static void configured(Runnable body){var old=SimpleMovement.CONFIG;try{SimpleMovement.CONFIG=new MovementConfig();body.run();}finally{SimpleMovement.CONFIG=old;SubMods.set(SimpleMovement.MOD_ID,true);}}
 public static void launch(GameTestHelper h){var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();h.assertTrue(root.getChild("crawl")!=null&&root.getChild(SimpleMovement.MOD_ID)!=null,"/crawl and /simplemovement registered");h.succeed();}
 public static void config(GameTestHelper h){var c=new MovementConfig();h.assertTrue(c.enableManualCrawl&&!c.enableAutowalk&&c.enableFastLadderSlide&&c.frostWalkerWalkOnPowderSnow&&c.jumpBoostStepUp&&c.ladderSlideActivation==MovementConfig.SlideActivationMode.CAMERA,"defaults like Simple QoL");close(h,c.ladderClimbingSpeed,.4,"climb default");close(h,c.ladderSlideSpeed,.8,"slide default");h.succeed();}
 /** First start: the old values (qOL.* and the top-level Frost Walker switch) are taken over once. */
 public static void qolMigration(GameTestHelper h){try{var dir=Files.createTempDirectory("simplemovement-migration");Files.writeString(dir.resolve("simplequalityoflife.json"),"{\"frostWalkerWalkOnPowderSnow\":false,\"qOL\":{\"enableAutowalk\":true,\"ladderClimbingSpeed\":0.3,\"ladderSlideActivation\":\"ALWAYS\",\"enableHoeHarvest\":false}}");var taken=SimpleMovement.read(dir);h.assertTrue(!taken.frostWalkerWalkOnPowderSnow&&taken.enableAutowalk&&taken.ladderSlideActivation==MovementConfig.SlideActivationMode.ALWAYS&&taken.enableManualCrawl,"old QoL values taken over, others default");close(h,taken.ladderClimbingSpeed,.3,"old climb speed");h.assertTrue(Files.isRegularFile(dir.resolve("simplemovement.json")),"own file written as marker");Files.writeString(dir.resolve("simplemovement.json"),"{\"enableAutowalk\":false}");var own=SimpleMovement.read(dir);h.assertTrue(!own.enableAutowalk&&own.frostWalkerWalkOnPowderSnow,"own file wins over QoL");var empty=SimpleMovement.read(Files.createTempDirectory("simplemovement-empty"));h.assertTrue(!empty.enableAutowalk&&empty.enableManualCrawl,"no file: defaults");}catch(java.io.IOException e){h.fail("migration io: "+e);}h.succeed();}
 /** "Enable Simple Movement" off in the super-mod: no crawl, Vanilla powder snow. */
 public static void superSwitch(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FROST_WALKER));h.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Frost Walker supports snow while on");SubMods.set(SimpleMovement.MOD_ID,false);h.assertTrue(!PowderSnowBlock.canEntityWalkOnPowderSnow(p),"switched-off sub-mod: Vanilla powder snow");h.assertTrue(!CrawlLimiter.allow(p),"switched-off sub-mod: crawl refused");});h.succeed();}
 /** The old Simple QoL key lines in options.txt are renamed once; existing new lines win. */
 public static void keyMigration(GameTestHelper h){var old="key_key.simplequalityoflife.autowalk:key.keyboard.g\nkey_key.simplequalityoflife.crawl:key.keyboard.c\n";var migrated=KeyMigration.options(old);h.assertTrue(migrated.contains("key_key.simplemovement.autowalk:key.keyboard.g")&&migrated.contains("key_key.simplemovement.crawl:key.keyboard.c")&&!migrated.contains("simplequalityoflife"),"both keys renamed");var both=old+"key_key.simplemovement.autowalk:key.keyboard.r\n";h.assertTrue(KeyMigration.options(both).contains("key_key.simplequalityoflife.autowalk:key.keyboard.g"),"an existing new line is never overwritten");h.succeed();}
 /** The client predicts with the server values: a valid payload is kept, garbage ignored, leaving forgets it. */
 public static void sync(GameTestHelper h){configured(()->{try{SimpleMovement.config().enableAutowalk=true;SimpleMovement.receiveSync(SimpleMovement.syncJson());h.assertTrue(SimpleMovement.syncedConfig()!=null&&SimpleMovement.syncedConfig().enableAutowalk,"server value arrives");SimpleMovement.receiveSync("{not json");h.assertTrue(SimpleMovement.syncedConfig().enableAutowalk,"invalid payload ignored");h.assertTrue(SimpleMovement.configFor(h.getLevel())==SimpleMovement.config(),"server level reads the server values");}finally{SimpleMovement.clearSync();}});h.assertTrue(SimpleMovement.syncedConfig()==null,"cleared");h.succeed();}
// ---- Step Height (boots) and Sprint (leggings): data-driven treasure enchantments (queue N59) ----
 private static final ResourceKey<Enchantment> STEP_HEIGHT=ResourceKey.create(Registries.ENCHANTMENT,id("step_height")),SPRINT=ResourceKey.create(Registries.ENCHANTMENT,id("sprint"));
 private static Holder<Enchantment> ench(GameTestHelper h,ResourceKey<Enchantment> key){return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
 private static ItemStack worn(GameTestHelper h,Item item,ResourceKey<Enchantment> key,int level){var s=new ItemStack(item);s.enchant(ench(h,key),level);return s;}
 /** Step Height I/II: the player's step_height attribute rises by 0.4 / 0.9 (0.6 base -> 1.0 / 1.5 blocks), gone again without the boots. */
 public static void enchantStepHeight(GameTestHelper h){
  h.assertValueEqual(ench(h,STEP_HEIGHT).value().getMaxLevel(),2,"Step Height max level");
  var p=h.makeMockServerPlayerInLevel();p.connection.tick();var base=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
  double[] expected={1.0,1.5};
  for(int level=1;level<=2;level++){p.setItemSlot(EquipmentSlot.FEET,worn(h,Items.DIAMOND_BOOTS,STEP_HEIGHT,level));p.connection.tick();
   close(h,p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT),expected[level-1],"Step Height "+level+" total (base "+base+")");}
  p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));p.connection.tick();close(h,p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT),base,"boots off: step height back to base");
  h.succeed();
 }
 /** Sprint I-III: +10/20/30 % of the base speed (add_multiplied_base) only while sprinting; walking, and sprinting after the leggings are off, stay vanilla. */
 public static void enchantSprint(GameTestHelper h){
  var speed=net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
  h.assertValueEqual(ench(h,SPRINT).value().getMaxLevel(),3,"Sprint max level");
  var p=h.makeMockServerPlayerInLevel();p.connection.tick();p.setSprinting(true);var sprintPlain=p.getAttributeValue(speed);p.setSprinting(false);var walkPlain=p.getAttributeValue(speed);
  for(int level=1;level<=3;level++){
   p.setSprinting(true);p.setItemSlot(EquipmentSlot.LEGS,worn(h,Items.DIAMOND_LEGGINGS,SPRINT,level));p.connection.tick();
   close(h,p.getAttributeValue(speed)/sprintPlain,1+.1*level,"Sprint "+level+" while sprinting, relative to plain sprint speed");
   p.setSprinting(false);p.setPos(p.getX()+level*2,p.getY(),p.getZ());p.connection.tick();
   close(h,p.getAttributeValue(speed),walkPlain,"Sprint "+level+" walking: Vanilla speed");
   p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);p.connection.tick();
  }
  p.setSprinting(true);p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));p.connection.tick();close(h,p.getAttributeValue(speed),sprintPlain,"plain leggings: Vanilla sprint speed");
  h.succeed();
 }
 /** Treasure rules like Mending / Frost Walker: treasure (so double trade price), tradeable, random chest loot, never in the enchanting table (or the Astral Enchanter, which reads the same tag); only on their own slot. */
 public static void enchantTreasureRules(GameTestHelper h){
  var vanilla=List.of("treasure","tradeable","on_random_loot");var absent=List.of("in_enchanting_table","non_treasure");
  for(var key:List.of(STEP_HEIGHT,SPRINT)){var holder=ench(h,key);
   for(var t:vanilla)h.assertTrue(holder.is(TagKey.create(Registries.ENCHANTMENT,Identifier.withDefaultNamespace(t))),key.identifier()+" is in #minecraft:"+t);
   for(var t:absent)h.assertTrue(!holder.is(TagKey.create(Registries.ENCHANTMENT,Identifier.withDefaultNamespace(t))),key.identifier()+" is not in #minecraft:"+t);
   h.assertTrue(holder.value().exclusiveSet().size()==0,key.identifier()+" has no exclusive set (compatible with everything)");}
  var step=ench(h,STEP_HEIGHT).value();var sprint=ench(h,SPRINT).value();
  h.assertTrue(step.canEnchant(new ItemStack(Items.DIAMOND_BOOTS))&&!step.canEnchant(new ItemStack(Items.DIAMOND_LEGGINGS))&&!step.canEnchant(new ItemStack(Items.DIAMOND_HELMET)),"Step Height: boots only");
  h.assertTrue(sprint.canEnchant(new ItemStack(Items.DIAMOND_LEGGINGS))&&!sprint.canEnchant(new ItemStack(Items.DIAMOND_BOOTS))&&!sprint.canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE)),"Sprint: leggings only");
  h.assertTrue(Enchantment.areCompatible(ench(h,SPRINT),ench(h,Enchantments.SWIFT_SNEAK))&&Enchantment.areCompatible(ench(h,STEP_HEIGHT),ench(h,Enchantments.FROST_WALKER)),"compatible with Swift Sneak / Frost Walker");
  h.succeed();
 }
// MOVED-BEGIN
 public static void configBounds(GameTestHelper h){
  var c=new MovementConfig();c.ladderClimbingSpeed=100;c.ladderSlideSpeed=Double.NaN;c.normalize();
  close(h,c.ladderClimbingSpeed,.4,"Climb cap");close(h,c.ladderSlideSpeed,.8,"Nonfinite slide default");
  c.ladderClimbingSpeed=-9;c.ladderSlideSpeed=99;c.normalize();close(h,c.ladderClimbingSpeed,.2,"Climb lower bound");close(h,c.ladderSlideSpeed,.8,"Slide hard cap");h.succeed();
 }
 public static void crawl(GameTestHelper h){var p=player(h,new BlockPos(2,2,2));var c=(CrawlAccessor)p;h.assertTrue(CrawlLimiter.allow(p),"Initial crawl permitted");c.simpleQualityOfLife$setCrawling(true);h.assertTrue(c.simpleQualityOfLife$isCrawling()&&p.getPose()==Pose.SWIMMING,"Server sets synchronized crawling pose");h.assertTrue(!CrawlLimiter.allow(p),"Repeat command refused");p.jumpFromGround();h.assertTrue(!c.simpleQualityOfLife$isCrawling(),"Jump clears crawling");h.succeed();}
 public static void climbPackets(GameTestHelper h){var c=new MovementConfig();c.ladderClimbingSpeed=999;c.ladderSlideSpeed=999;h.assertTrue(!ClimbSecurity.allowed(5,1,c)&&!ClimbSecurity.allowed(-5,1,c),"Forged speeds refused");h.assertTrue(!ClimbSecurity.allowed(Double.NaN,1,c)&&!ClimbSecurity.allowed(Double.POSITIVE_INFINITY,1,c),"Nonfinite packets refused");h.assertTrue(ClimbSecurity.allowed(.4,1,c)&&ClimbSecurity.allowed(-.8,1,c),"Legal maxima accepted");h.assertTrue(!ClimbSecurity.allowed(2,999,c),"Latency budget cannot grow without limit");h.succeed();}
 public static void climbMechanics(GameTestHelper h){configured(()->{try{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.LADDER.defaultBlockState());var p=player(h,pos);p.setXRot(80);p.setShiftKeyDown(true);var method=LivingEntity.class.getDeclaredMethod("handleOnClimbable",Vec3.class);method.setAccessible(true);var v=(Vec3)method.invoke(p,new Vec3(0,-.1,0));close(h,v.y,-.8,"Actual ladder slide injection");p.setShiftKeyDown(false);p.horizontalCollision=true;p.setJumping(true);var up=LivingEntity.class.getDeclaredMethod("handleRelativeFrictionAndCalculateMovement",Vec3.class,float.class);up.setAccessible(true);v=(Vec3)up.invoke(p,Vec3.ZERO,0.6f);close(h,v.y,.4,"Actual upward speed injection survives Vanilla override");p.setShiftKeyDown(true);SimpleMovement.config().enableFastLadderSlide=false;v=(Vec3)method.invoke(p,new Vec3(0,-.1,0));close(h,v.y,0,"Disabled sliding uses Vanilla sneak brake");}catch(Exception e){throw new IllegalStateException(e);}});h.succeed();}
 public static void powderSnow(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FROST_WALKER));h.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Frost Walker supports snow");SimpleMovement.config().frostWalkerWalkOnPowderSnow=false;h.assertTrue(!PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Snow toggle enforced");});h.succeed();}
 public static void realMovementPackets(GameTestHelper h){configured(()->{
  var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);var pos=new BlockPos(2,2,2);h.setBlock(pos.south(),Blocks.STONE);h.setBlock(pos,Blocks.LADDER);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));h.assertTrue(p.onClimbable(),"Server detects ladder before packet");var audit=(ClimbAudit)p.connection;long before=audit.qol$rejectedMovementPackets();double y=p.getY();
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+5,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+1,"Actual C2S out-of-range movement rejected by module");close(h,p.getY(),y,"Rejected packet cannot move player");
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+.4,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+1,"Legal packet accepted by module");
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+.8,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+2,"Packet spam cannot buy another climb allowance");
 });h.succeed();}
 public static void featureSwitches(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var abs=h.absolutePos(new BlockPos(2,2,2));h.assertTrue(!SimpleMovement.config().enableAutowalk,"Autowalk disabled by server default");SimpleMovement.config().enableManualCrawl=false;h.assertTrue(!CrawlLimiter.allow(p),"Disabled crawl refused by server");});h.succeed();}

 /** Jump Boost: step_height is at least 1.0 while the effect runs (any source), Step Height boots are not added on top (max), the switch and the effect ending remove it. */
 public static void jumpBoostStep(GameTestHelper h){configured(()->{
  var attr=net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT;
  var p=h.makeMockServerPlayerInLevel();p.connection.tick();var base=p.getAttributeValue(attr);
  h.assertTrue(base<1.0,"vanilla step height below one block: "+base);
  var boost=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP_BOOST,600,0);
  JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),base,"no effect: untouched");
  p.addEffect(boost);JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),1.0,"Jump Boost: one block");
  JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),1.0,"stable on the next tick");
  p.setItemSlot(EquipmentSlot.FEET,worn(h,Items.DIAMOND_BOOTS,STEP_HEIGHT,1));p.connection.tick();JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),1.0,"Step Height I + Jump Boost: max, not 1.4");
  p.setItemSlot(EquipmentSlot.FEET,worn(h,Items.DIAMOND_BOOTS,STEP_HEIGHT,2));p.connection.tick();JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),1.5,"Step Height II + Jump Boost: 1.5");
  p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));p.connection.tick();JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),1.0,"boots off, effect on: one block");
  SimpleMovement.config().jumpBoostStepUp=false;JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),base,"switch off: vanilla");
  SimpleMovement.config().jumpBoostStepUp=true;JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),1.0,"switch on again");
  p.removeEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST);JumpBoostStep.tick(p);close(h,p.getAttributeValue(attr),base,"effect gone: vanilla");
  h.assertTrue(p.getAttribute(attr).getModifier(JumpBoostStep.ID)==null,"modifier removed");
 });h.succeed();}
// MOVED-END
}
