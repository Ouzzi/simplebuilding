package com.simplequalityoflife.test;
import com.simplequalityoflife.*;
import com.simplequalityoflife.config.*;
import com.simplequalityoflife.event.*;
import com.simplequalityoflife.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.entity.vault.*;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Consumer;
public final class QolTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("guide_book",com.simplequalityoflife.guide.QolGuide::gameTest);
  ALL.put("launch",QolTests::launch);ALL.put("config_bounds",QolTests::configBounds);ALL.put("config_lang",QolTests::configLang);
  ALL.put("crawl",QolTests::crawl);ALL.put("climb_packets",QolTests::climbPackets);ALL.put("climb_mechanics",QolTests::climbMechanics);
  ALL.put("powder_snow",QolTests::powderSnow);ALL.put("farmland",QolTests::farmland);ALL.put("hoe_harvest",QolTests::hoeHarvest);
  ALL.put("furnace_lava",QolTests::furnaceLava);ALL.put("permissions_spam",QolTests::permissionsSpam);ALL.put("durability",QolTests::durability);
  ALL.put("muting",QolTests::muting);ALL.put("baby",QolTests::baby);ALL.put("piglins",QolTests::piglins);ALL.put("weather",QolTests::weather);
  ALL.put("vault",QolTests::vault);ALL.put("vegetation",QolTests::vegetation);ALL.put("cross_mod",QolTests::crossMod);
  ALL.put("real_movement_packets",QolTests::realMovementPackets);ALL.put("vault_persistence",QolTests::vaultPersistence);
  ALL.put("gold_trim",QolTests::goldTrim);ALL.put("anvil_repair_cost",QolTests::anvilRepairCost);ALL.put("thrift",QolTests::thrift);ALL.put("feature_switches",QolTests::featureSwitches);ALL.put("sharpness_action",QolTests::sharpnessAction);
  ALL.put("linked_mark",ContainerTests::linkedMark);ALL.put("linked_range",ContainerTests::linkedRange);ALL.put("linked_transfer",ContainerTests::linkedTransfer);ALL.put("portable_shulker",ContainerTests::portableShulker);ALL.put("portable_ender_chest",ContainerTests::portableEnderChest);
 }
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath("simplequalityoflife",s);}
 private static void close(GameTestHelper h,double a,double b,String msg){h.assertTrue(Math.abs(a-b)<0.00001,msg+": "+a+" / "+b);}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key){var s=new ItemStack(item);s.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),3);return s;}
 private static void configured(Runnable body){var c=Simplequalityoflife.getConfig();var old=c.qOL;boolean snow=c.frostWalkerWalkOnPowderSnow;try{c.qOL=new SimplequalityoflifeConfig.QOL();body.run();}finally{c.qOL=old;c.frostWalkerWalkOnPowderSnow=snow;}}
 public static void launch(GameTestHelper h){
  for(var registry:List.<net.minecraft.core.Registry<?>>of(BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK,BuiltInRegistries.ENTITY_TYPE))h.assertTrue(registry.keySet().stream().noneMatch(i->i.getNamespace().equals("simplequalityoflife")&&!(registry==BuiltInRegistries.ITEM&&i.getPath().equals("guide_book"))),"No invented registry content (only the guide item)");
  var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();h.assertTrue(root.getChild("crawl")!=null&&root.getChild("simplequalityoflife")!=null,"Both commands registered");
  h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:reinforced_hopper")),"SimpleBuilding is loaded in module instance");
  h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:blocks/wheat")))!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Crop loot loads");h.succeed();
 }
 public static void configBounds(GameTestHelper h){
  var c=new SimplequalityoflifeConfig();c.qOL.ladderClimbingSpeed=100;c.qOL.ladderSlideSpeed=Double.NaN;c.qOL.fullDurabilityBonusMultiplier=Double.POSITIVE_INFINITY;c.qOL.fullDurabilityThreshold=-5;c.qOL.vaultCooldownDays=0;c.qOL.clientRainParticleDensity=500;c.qOL.nametagMuteSuffixes=new ArrayList<>(Collections.nCopies(100,"x"));c.normalize();
  close(h,c.qOL.ladderClimbingSpeed,.4,"Climb cap");close(h,c.qOL.ladderSlideSpeed,.8,"Nonfinite slide default");close(h,c.qOL.fullDurabilityBonusMultiplier,1,"Nonfinite bonus default");close(h,c.qOL.fullDurabilityThreshold,.8,"Threshold lower bound");h.assertTrue(c.qOL.vaultCooldownDays==1&&c.qOL.clientRainParticleDensity==100&&c.qOL.nametagMuteSuffixes.size()==1,"Integer/list bounds");
  c.qOL.ladderClimbingSpeed=-9;c.qOL.ladderSlideSpeed=99;c.qOL.fullDurabilityBonusMultiplier=999;c.qOL.vaultCooldownDays=Integer.MAX_VALUE;c.qOL.clientRainParticleDensity=-1;c.normalize();close(h,c.qOL.ladderClimbingSpeed,.2,"Climb lower bound");close(h,c.qOL.ladderSlideSpeed,.8,"Slide hard cap");close(h,c.qOL.fullDurabilityBonusMultiplier,1.5,"Bonus hard cap");h.assertTrue(c.qOL.vaultCooldownDays==36500&&c.qOL.clientRainParticleDensity==0,"Upper days/lower rain");h.succeed();
 }
 public static void configLang(GameTestHelper h){
  try{var gson=new com.google.gson.Gson();var en=language("en_us");var de=language("de_de");h.assertTrue(en.keySet().equals(de.keySet()),"Both locales have exactly the same keys");var c=new SimplequalityoflifeConfig();
   for(Object object:List.of(c,c.qOL))for(var field:object.getClass().getFields()){
    if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||field.getName().equals("qOL"))continue;
    String p=(object==c?"":"qOL.")+field.getName();String key="text.autoconfig.simplequalityoflife.option."+p;
    for(var lang:List.of(en,de)){h.assertTrue(lang.has(key)&&lang.has(key+".@Tooltip"),"Name/tooltip "+p);h.assertTrue(lang.get(key+".@Tooltip").getAsString().replace(" ", "").contains(gson.toJson(field.get(object)).replace(" ", "")),"Default in tooltip "+p);}
   }
   for(String tab:List.of("movement","interaction","mobs","weather","vaults","containers"))h.assertTrue(en.has("simplequalityoflife.config.tab."+tab)&&de.has("simplequalityoflife.config.tab."+tab),"Tab "+tab);
   var loaded=gson.fromJson("{\"frostWalkerWalkOnPowderSnow\":false,\"qOL\":{\"ladderClimbingSpeed\":0.3}}",SimplequalityoflifeConfig.class);loaded.normalize();h.assertTrue(!loaded.frostWalkerWalkOnPowderSnow&&loaded.qOL.enableHoeHarvest,"Legacy keys and missing defaults");close(h,loaded.qOL.ladderClimbingSpeed,.3,"Legacy value");
  }catch(Exception e){throw new IllegalStateException(e);}h.succeed();
 }
 private static com.google.gson.JsonObject language(String l)throws Exception{try(var in=Simplequalityoflife.class.getResourceAsStream("/assets/simplequalityoflife/lang/"+l+".json")){return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 public static void crawl(GameTestHelper h){var p=player(h,new BlockPos(2,2,2));var c=(CrawlAccessor)p;h.assertTrue(CrawlLimiter.allow(p),"Initial crawl permitted");c.simpleQualityOfLife$setCrawling(true);h.assertTrue(c.simpleQualityOfLife$isCrawling()&&p.getPose()==Pose.SWIMMING,"Server sets synchronized crawling pose");h.assertTrue(!CrawlLimiter.allow(p),"Repeat command refused");p.jumpFromGround();h.assertTrue(!c.simpleQualityOfLife$isCrawling(),"Jump clears crawling");h.succeed();}
 public static void climbPackets(GameTestHelper h){var c=new SimplequalityoflifeConfig();c.qOL.ladderClimbingSpeed=999;c.qOL.ladderSlideSpeed=999;h.assertTrue(!ClimbSecurity.allowed(5,1,c)&&!ClimbSecurity.allowed(-5,1,c),"Forged speeds refused");h.assertTrue(!ClimbSecurity.allowed(Double.NaN,1,c)&&!ClimbSecurity.allowed(Double.POSITIVE_INFINITY,1,c),"Nonfinite packets refused");h.assertTrue(ClimbSecurity.allowed(.4,1,c)&&ClimbSecurity.allowed(-.8,1,c),"Legal maxima accepted");h.assertTrue(!ClimbSecurity.allowed(2,999,c),"Latency budget cannot grow without limit");h.succeed();}
 public static void climbMechanics(GameTestHelper h){configured(()->{try{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.LADDER.defaultBlockState());var p=player(h,pos);p.setXRot(80);p.setShiftKeyDown(true);var method=LivingEntity.class.getDeclaredMethod("handleOnClimbable",Vec3.class);method.setAccessible(true);var v=(Vec3)method.invoke(p,new Vec3(0,-.1,0));close(h,v.y,-.8,"Actual ladder slide injection");p.setShiftKeyDown(false);p.horizontalCollision=true;p.setJumping(true);var up=LivingEntity.class.getDeclaredMethod("handleRelativeFrictionAndCalculateMovement",Vec3.class,float.class);up.setAccessible(true);v=(Vec3)up.invoke(p,Vec3.ZERO,0.6f);close(h,v.y,.4,"Actual upward speed injection survives Vanilla override");p.setShiftKeyDown(true);Simplequalityoflife.getConfig().qOL.enableFastLadderSlide=false;v=(Vec3)method.invoke(p,new Vec3(0,-.1,0));close(h,v.y,0,"Disabled sliding uses Vanilla sneak brake");}catch(Exception e){throw new IllegalStateException(e);}});h.succeed();}
 public static void powderSnow(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FROST_WALKER));h.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Frost Walker supports snow");Simplequalityoflife.getConfig().frostWalkerWalkOnPowderSnow=false;h.assertTrue(!PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Snow toggle enforced");});h.succeed();}
 public static void farmland(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FEATHER_FALLING));h.assertTrue(Protection.farmland(p),"Feather Falling protects");Simplequalityoflife.getConfig().qOL.preventFarmlandTrampleWithFeatherFalling=false;h.assertTrue(!Protection.farmland(p),"Farmland toggle");});h.succeed();}
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
 public static void furnaceLava(GameTestHelper h){configured(()->{for(Block b:List.of(Blocks.FURNACE,Blocks.BLAST_FURNACE,Blocks.SMOKER)){var pos=new BlockPos(2,2,2);h.setBlock(pos,b);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));var abs=h.absolutePos(pos);var furnace=(AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(abs);h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,abs,Direction.UP)==InteractionResult.SUCCESS,"Lava fills "+b);h.assertTrue(furnace.getItem(1).is(Items.LAVA_BUCKET)&&p.getMainHandItem().isEmpty(),"One lava bucket transferred, no early empty bucket");
  p=player(h,pos);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.LAVA_BUCKET));furnace.setItem(1,new ItemStack(Items.BUCKET));h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.OFF_HAND,abs,Direction.UP)==InteractionResult.SUCCESS,"Offhand leftover exchange");h.assertTrue(p.getOffhandItem().is(Items.BUCKET)&&p.getOffhandItem().getCount()==1,"Exactly one leftover returned");
 }});h.succeed();}
 public static void permissionsSpam(GameTestHelper h){configured(()->{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.FURNACE);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));var old=InteractionGuard.permission;try{InteractionGuard.permission=(a,b)->false;h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,h.absolutePos(pos),Direction.UP)==InteractionResult.PASS,"Claim refusal honored");h.assertTrue(p.getMainHandItem().is(Items.LAVA_BUCKET),"Refusal consumes nothing");}finally{InteractionGuard.permission=old;}
  h.assertTrue(!InteractionGuard.allow(p,h.absolutePos(new BlockPos(20,2,2))),"Forged distant action refused");var other=player(h,pos);h.assertTrue(InteractionGuard.action(other)&&!InteractionGuard.action(other),"Auto-action spam refused");
 });h.succeed();}
 public static void durability(GameTestHelper h){configured(()->{
  var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);
  var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);
  var hammer=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:diamond_sledgehammer")));
  // A component-defined tool models third-party stacks without a Vanilla tool subclass.
  var foreign=new ItemStack(Items.STICK);foreign.set(DataComponents.MAX_STACK_SIZE,1);
  foreign.set(DataComponents.MAX_DAMAGE,400);foreign.set(DataComponents.DAMAGE,0);
  foreign.set(DataComponents.TOOL,vanilla.get(DataComponents.TOOL));
  var c=Simplequalityoflife.getConfig().qOL;
  close(h,c.fullDurabilityBonusMultiplier,1,"Fresh server default has no bonus");
  for(var stack:List.of(vanilla,new ItemStack(Items.DIAMOND_SWORD),hammer,foreign)){
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
  for(String json:List.of("{}","{\"qOL\":{}}","{\"qOL\":{\"fullDurabilityBonusMultiplier\":1.5}}")){
   var loaded=gson.fromJson(json,SimplequalityoflifeConfig.class);loaded.normalize();
   close(h,loaded.qOL.fullDurabilityBonusMultiplier,json.contains("1.5")?1.5:1,"Saved values retained; absent key defaults to one");
  }
 });h.succeed();}
 public static void muting(GameTestHelper h){configured(()->{var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_mute"));h.assertTrue(cow.isSilent(),"Mute suffix");cow.setCustomName(null);h.assertTrue(!cow.isSilent(),"No suffix means normal sound");Simplequalityoflife.getConfig().qOL.mutedEntities.add("minecraft:cow");h.assertTrue(cow.isSilent(),"Type mute");});h.succeed();}
 public static void baby(GameTestHelper h){var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_baby"));cow.setAge(-1);cow.tickCount=99;h.runAfterDelay(3,()->{h.assertTrue(cow.isBaby()&&cow.getAge()<-23000,"Actual age tick keeps named baby young");cow.setCustomName(null);cow.setAge(-1);h.runAfterDelay(3,()->{h.assertTrue(!cow.isBaby(),"Removing suffix permits aging");h.succeed();});});}
 public static void piglins(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_HOE));h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Offhand gold is safe");Simplequalityoflife.getConfig().qOL.piglinsIgnoreGoldTools=false;h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Gold tool toggle");});h.succeed();}
 public static void weather(GameTestHelper h){configured(()->{try{var world=h.getLevel();var data=world.getWeatherData();data.setRaining(true);data.setThundering(true);world.setRainLevel(1);world.setThunderLevel(1);Simplequalityoflife.getConfig().qOL.disableWeather=true;var method=world.getClass().getDeclaredMethod("advanceWeatherCycle");method.setAccessible(true);method.invoke(world);h.assertTrue(!data.isRaining()&&!data.isThundering(),"Actual server weather hook clears both");}catch(Exception e){throw new IllegalStateException(e);}});h.succeed();}
 public static void vault(GameTestHelper h){configured(()->{var data=new VaultServerData();var cooldown=(IVaultCooldown)data;var uuid=UUID.randomUUID();h.assertTrue(cooldown.hasLootedRecently(uuid,100),"Missing legacy timestamp starts cooldown");cooldown.markLooted(uuid,100);h.assertTrue(cooldown.hasLootedRecently(uuid,99)&&cooldown.hasLootedRecently(uuid,100+23999),"Rollback/early repeat refused");Simplequalityoflife.getConfig().qOL.vaultCooldownDays=1;h.assertTrue(!cooldown.hasLootedRecently(uuid,100+24000),"Exact cooldown expiry");var copy=new VaultServerData();((IVaultCooldown)copy).setLootTimesMap(cooldown.getLootTimesMap());h.assertTrue(((IVaultCooldown)copy).getLootTimesMap().get(uuid)==100,"UUID/time map preserved");});h.succeed();}
 public static void vegetation(GameTestHelper h){h.assertTrue(VegetationUtil.isCuttable(Blocks.SHORT_GRASS.defaultBlockState())&&VegetationUtil.isCuttable(Blocks.DANDELION.defaultBlockState()),"Grass/flowers supported");h.assertTrue(!VegetationUtil.isCuttable(Blocks.WATER.defaultBlockState())&&!VegetationUtil.isCuttable(Blocks.STONE.defaultBlockState()),"Fluids and building blocks excluded");h.succeed();}
 public static void crossMod(GameTestHelper h){configured(()->{
  var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:reinforced_furnace"));h.assertTrue(block!=Blocks.AIR,"Public SimpleBuilding furnace ID");var pos=new BlockPos(2,2,2);h.setBlock(pos,block);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,h.absolutePos(pos),Direction.UP)==InteractionResult.SUCCESS,"Foreign furnace follows Vanilla fuel interface");h.assertTrue(((AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos))).getItem(1).is(Items.LAVA_BUCKET),"Foreign fuel slot updated");
  var hammer=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:diamond_sledgehammer")));h.assertTrue(hammer.isDamageableItem(),"Public foreign damageable tool ID");p.setItemSlot(EquipmentSlot.MAINHAND,hammer);Simplequalityoflife.getConfig().qOL.enableFullDurabilityBonus=false;double base=p.getAttributeValue(Attributes.ATTACK_DAMAGE);Simplequalityoflife.getConfig().qOL.enableFullDurabilityBonus=true;Simplequalityoflife.getConfig().qOL.fullDurabilityBonusMultiplier=1.5;close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),base*1.5,"Foreign tool receives exactly one capped bonus");
  var farm=new BlockPos(3,2,2);h.setBlock(farm,Blocks.FARMLAND);p=h.makeMockPlayer(GameType.CREATIVE);p.setPos(Vec3.atCenterOf(h.absolutePos(farm.above())));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:breeze_head"))));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FEATHER_FALLING));var absolute=h.absolutePos(farm);Blocks.FARMLAND.fallOn(h.getLevel(),h.getLevel().getBlockState(absolute),absolute,p,100);h.assertTrue(h.getLevel().getBlockState(absolute).is(Blocks.FARMLAND),"Both farmland protections coexist");Simplequalityoflife.getConfig().qOL.preventFarmlandTrampleWithFeatherFalling=false;Blocks.FARMLAND.fallOn(h.getLevel(),h.getLevel().getBlockState(absolute),absolute,p,100);h.assertTrue(h.getLevel().getBlockState(absolute).is(Blocks.FARMLAND),"Disabling Feather Falling preserves Breeze head protection");
 });h.succeed();}
 public static void realMovementPackets(GameTestHelper h){configured(()->{
  var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);var pos=new BlockPos(2,2,2);h.setBlock(pos.south(),Blocks.STONE);h.setBlock(pos,Blocks.LADDER);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));h.assertTrue(p.onClimbable(),"Server detects ladder before packet");var audit=(ClimbAudit)p.connection;long before=audit.qol$rejectedMovementPackets();double y=p.getY();
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+5,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+1,"Actual C2S out-of-range movement rejected by module");close(h,p.getY(),y,"Rejected packet cannot move player");
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+.4,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+1,"Legal packet accepted by module");
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+.8,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+2,"Packet spam cannot buy another climb allowance");
 });h.succeed();}
 public static void vaultPersistence(GameTestHelper h){var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.VAULT);var v=(VaultBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));var uuid=UUID.randomUUID();((IVaultCooldown)v.getServerData()).markLooted(uuid,12345);var tag=v.saveWithFullMetadata(h.getLevel().registryAccess());h.assertTrue(tag.contains("SimpleBuildingLootTimes"),"Legacy persistent map key written");var loaded=(VaultBlockEntity)BlockEntity.loadStatic(v.getBlockPos(),v.getBlockState(),tag,h.getLevel().registryAccess());loaded.setLevel(h.getLevel());h.assertTrue(((IVaultCooldown)loaded.getServerData()).getLootTimesMap().get(uuid)==12345,"Actual block-entity save/reload keeps UUID and timestamp");h.succeed();}
 /** Sparsamkeit: +1/3 Haltbarkeit je Stufe (III = doppelt), nicht mit Reparatur kombinierbar, Materialreparatur je Stueck ein Viertel. */
 public static void thrift(GameTestHelper h){var reg=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var thrift=reg.getOrThrow(com.simplequalityoflife.registry.Thrift.KEY);
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
  boolean sb;try{Class.forName("com.simplebuilding.Simplebuilding");sb=true;}catch(ClassNotFoundException e){sb=false;}
  if(!sb){Simplequalityoflife.getConfig().qOL.anvilRepairKeepsCost=false;menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
  h.assertTrue(menu.getSlot(2).getItem().getOrDefault(DataComponents.REPAIR_COST,0)==7,"switched off: vanilla raises the repair cost to 7");}
 });h.succeed();}
 public static void goldTrim(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);var lookup=h.getLevel().registryAccess();armor.set(DataComponents.TRIM,new net.minecraft.world.item.equipment.trim.ArmorTrim(lookup.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.GOLD),lookup.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));p.setItemSlot(EquipmentSlot.CHEST,armor);h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Actual gold trim accepted");Simplequalityoflife.getConfig().qOL.piglinsIgnoreGoldTrims=false;h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Gold trim toggle enforced");});h.succeed();}
 public static void featureSwitches(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var abs=h.absolutePos(new BlockPos(2,2,2));h.setBlock(new BlockPos(2,2,2),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_HOE));Simplequalityoflife.getConfig().qOL.enableHoeHarvest=false;h.assertTrue(HoeHarvestHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,abs,Direction.UP)==InteractionResult.PASS,"Harvest toggle refuses action");h.setBlock(new BlockPos(2,2,2),Blocks.FURNACE);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.LAVA_BUCKET));Simplequalityoflife.getConfig().qOL.enableFurnaceLavaFill=false;h.assertTrue(FurnaceLavaFillHandler.onRightClickBlock(p,InteractionHand.MAIN_HAND,abs,Direction.UP)==InteractionResult.PASS,"Lava toggle refuses action");h.assertTrue(!Simplequalityoflife.getConfig().qOL.enableAutowalk,"Autowalk disabled by server default");Simplequalityoflife.getConfig().qOL.enableManualCrawl=false;h.assertTrue(!CrawlLimiter.allow(p),"Disabled crawl refused by server");});h.succeed();}
 public static void sharpnessAction(GameTestHelper h){configured(()->{var pos=new BlockPos(2,2,2);h.setBlock(pos.below(),Blocks.DIRT);h.setBlock(pos,Blocks.SHORT_GRASS);var target=h.spawn(EntityTypes.COW,2,2,2);var p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,enchanted(h,Items.DIAMOND_SWORD,Enchantments.SHARPNESS));var old=InteractionGuard.permission;try{InteractionGuard.permission=(a,b)->false;p.attack(target);h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).is(Blocks.SHORT_GRASS),"Actual attack respects claim veto");p=player(h,pos);p.setItemSlot(EquipmentSlot.MAINHAND,enchanted(h,Items.DIAMOND_SWORD,Enchantments.SHARPNESS));InteractionGuard.permission=(a,b)->true;p.attack(target);h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).isAir(),"Actual Sharpness attack cuts grass");}finally{InteractionGuard.permission=old;}});h.succeed();}
}
