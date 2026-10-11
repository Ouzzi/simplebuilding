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
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Consumer;
public final class QolTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("guide_book",com.simplequalityoflife.guide.QolGuide::gameTest);
  ALL.put("launch",QolTests::launch);ALL.put("config_bounds",QolTests::configBounds);ALL.put("config_lang",QolTests::configLang);
  ALL.put("crawl",QolTests::crawl);ALL.put("climb_packets",QolTests::climbPackets);ALL.put("climb_mechanics",QolTests::climbMechanics);
  ALL.put("powder_snow",QolTests::powderSnow);
  
  ALL.put("sub_mod_switch",QolTests::subModSwitch);
  
  ALL.put("real_movement_packets",QolTests::realMovementPackets);
  ALL.put("feature_switches",QolTests::featureSwitches);
  ALL.put("linked_mark",ContainerTests::linkedMark);ALL.put("linked_range",ContainerTests::linkedRange);ALL.put("linked_transfer",ContainerTests::linkedTransfer);ALL.put("linked_inventory",ContainerTests::linkedInventory);ALL.put("portable_shulker",ContainerTests::portableShulker);ALL.put("portable_ender_chest",ContainerTests::portableEnderChest);
 }
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath("simplequalityoflife",s);}
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 private static void close(GameTestHelper h,double a,double b,String msg){h.assertTrue(Math.abs(a-b)<0.00001,msg+": "+a+" / "+b);}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static ItemStack enchanted(GameTestHelper h,Item item,ResourceKey<Enchantment> key){var s=new ItemStack(item);s.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),3);return s;}
 private static void configured(Runnable body){var c=Simplequalityoflife.getConfig();var old=c.qOL;boolean snow=c.frostWalkerWalkOnPowderSnow;try{c.qOL=new SimplequalityoflifeConfig.QOL();body.run();}finally{c.qOL=old;c.frostWalkerWalkOnPowderSnow=snow;}}
 public static void launch(GameTestHelper h){
  for(var registry:List.<net.minecraft.core.Registry<?>>of(BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK,BuiltInRegistries.ENTITY_TYPE))h.assertTrue(registry.keySet().stream().noneMatch(i->i.getNamespace().equals("simplequalityoflife")&&!(registry==BuiltInRegistries.ITEM&&i.getPath().equals("guide_book"))),"No invented registry content (only the guide item)");
  var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();h.assertTrue(root.getChild("crawl")!=null&&root.getChild("simplequalityoflife")!=null,"Both commands registered");
  if(isModLoaded("simplebuilding"))h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:reinforced_hopper")),"SimpleBuilding content visible when loaded");
  h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:blocks/wheat")))!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Crop loot loads");h.succeed();
 }
 public static void configBounds(GameTestHelper h){
  var c=new SimplequalityoflifeConfig();c.qOL.ladderClimbingSpeed=100;c.qOL.ladderSlideSpeed=Double.NaN;c.normalize();
  close(h,c.qOL.ladderClimbingSpeed,.4,"Climb cap");close(h,c.qOL.ladderSlideSpeed,.8,"Nonfinite slide default");
  c.qOL.ladderClimbingSpeed=-9;c.qOL.ladderSlideSpeed=99;c.normalize();close(h,c.qOL.ladderClimbingSpeed,.2,"Climb lower bound");close(h,c.qOL.ladderSlideSpeed,.8,"Slide hard cap");h.succeed();
 }
 public static void configLang(GameTestHelper h){
  try{var gson=new com.google.gson.Gson();var en=language("en_us");var de=language("de_de");h.assertTrue(en.keySet().equals(de.keySet()),"Both locales have exactly the same keys");var c=new SimplequalityoflifeConfig();
   for(Object object:List.of(c,c.qOL))for(var field:object.getClass().getFields()){
    if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||field.getName().equals("qOL"))continue;
    String p=(object==c?"":"qOL.")+field.getName();String key="text.autoconfig.simplequalityoflife.option."+p;
    for(var lang:List.of(en,de)){h.assertTrue(lang.has(key)&&lang.has(key+".@Tooltip"),"Name/tooltip "+p);h.assertTrue(lang.get(key+".@Tooltip").getAsString().replace(" ", "").contains(gson.toJson(field.get(object)).replace(" ", "")),"Default in tooltip "+p);}
   }
   for(String tab:List.of("submods","movement","interaction","mobs","vaults","containers"))h.assertTrue(en.has("simplequalityoflife.config.tab."+tab)&&de.has("simplequalityoflife.config.tab."+tab),"Tab "+tab);
   var loaded=gson.fromJson("{\"frostWalkerWalkOnPowderSnow\":false,\"qOL\":{\"ladderClimbingSpeed\":0.3}}",SimplequalityoflifeConfig.class);loaded.normalize();h.assertTrue(!loaded.frostWalkerWalkOnPowderSnow&&loaded.qOL.enableManualCrawl,"Legacy keys and missing defaults");close(h,loaded.qOL.ladderClimbingSpeed,.3,"Legacy value");
  }catch(Exception e){throw new IllegalStateException(e);}h.succeed();
 }
 private static com.google.gson.JsonObject language(String l)throws Exception{try(var in=Simplequalityoflife.class.getResourceAsStream("/assets/simplequalityoflife/lang/"+l+".json")){return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 public static void crawl(GameTestHelper h){var p=player(h,new BlockPos(2,2,2));var c=(CrawlAccessor)p;h.assertTrue(CrawlLimiter.allow(p),"Initial crawl permitted");c.simpleQualityOfLife$setCrawling(true);h.assertTrue(c.simpleQualityOfLife$isCrawling()&&p.getPose()==Pose.SWIMMING,"Server sets synchronized crawling pose");h.assertTrue(!CrawlLimiter.allow(p),"Repeat command refused");p.jumpFromGround();h.assertTrue(!c.simpleQualityOfLife$isCrawling(),"Jump clears crawling");h.succeed();}
 public static void climbPackets(GameTestHelper h){var c=new SimplequalityoflifeConfig();c.qOL.ladderClimbingSpeed=999;c.qOL.ladderSlideSpeed=999;h.assertTrue(!ClimbSecurity.allowed(5,1,c)&&!ClimbSecurity.allowed(-5,1,c),"Forged speeds refused");h.assertTrue(!ClimbSecurity.allowed(Double.NaN,1,c)&&!ClimbSecurity.allowed(Double.POSITIVE_INFINITY,1,c),"Nonfinite packets refused");h.assertTrue(ClimbSecurity.allowed(.4,1,c)&&ClimbSecurity.allowed(-.8,1,c),"Legal maxima accepted");h.assertTrue(!ClimbSecurity.allowed(2,999,c),"Latency budget cannot grow without limit");h.succeed();}
 public static void climbMechanics(GameTestHelper h){configured(()->{try{var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.LADDER.defaultBlockState());var p=player(h,pos);p.setXRot(80);p.setShiftKeyDown(true);var method=LivingEntity.class.getDeclaredMethod("handleOnClimbable",Vec3.class);method.setAccessible(true);var v=(Vec3)method.invoke(p,new Vec3(0,-.1,0));close(h,v.y,-.8,"Actual ladder slide injection");p.setShiftKeyDown(false);p.horizontalCollision=true;p.setJumping(true);var up=LivingEntity.class.getDeclaredMethod("handleRelativeFrictionAndCalculateMovement",Vec3.class,float.class);up.setAccessible(true);v=(Vec3)up.invoke(p,Vec3.ZERO,0.6f);close(h,v.y,.4,"Actual upward speed injection survives Vanilla override");p.setShiftKeyDown(true);Simplequalityoflife.getConfig().qOL.enableFastLadderSlide=false;v=(Vec3)method.invoke(p,new Vec3(0,-.1,0));close(h,v.y,0,"Disabled sliding uses Vanilla sneak brake");}catch(Exception e){throw new IllegalStateException(e);}});h.succeed();}
 public static void powderSnow(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.FEET,enchanted(h,Items.DIAMOND_BOOTS,Enchantments.FROST_WALKER));h.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Frost Walker supports snow");Simplequalityoflife.getConfig().frostWalkerWalkOnPowderSnow=false;h.assertTrue(!PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Snow toggle enforced");});h.succeed();}
 /** Super-mod: "Enable Simple Weather" reaches the bundled sub-mod through framework SubMods (no class import). */
 public static void subModSwitch(GameTestHelper h){var c=new SimplequalityoflifeConfig();h.assertTrue(c.enableSimpleWeather,"Sub-mods default on");try{c.enableSimpleWeather=false;Simplequalityoflife.subMods(c);h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled("simpleweather"),"Switch off reaches Simple Weather");}finally{Simplequalityoflife.subMods(Simplequalityoflife.getConfig());}h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled("simpleweather")==Simplequalityoflife.getConfig().enableSimpleWeather,"Switch restored");try{c.enableSimpleWeather=true;c.enableSimpleInterfaces=false;Simplequalityoflife.subMods(c);h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled("simpleinterfaces")&&com.simplebuilding.framework.api.SubMods.enabled("simpleweather"),"Switch off reaches only Simple Interfaces");}finally{Simplequalityoflife.subMods(Simplequalityoflife.getConfig());}h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled("simpleinterfaces")==Simplequalityoflife.getConfig().enableSimpleInterfaces,"Interfaces switch restored");h.succeed();}
 public static void realMovementPackets(GameTestHelper h){configured(()->{
  var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);var pos=new BlockPos(2,2,2);h.setBlock(pos.south(),Blocks.STONE);h.setBlock(pos,Blocks.LADDER);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));h.assertTrue(p.onClimbable(),"Server detects ladder before packet");var audit=(ClimbAudit)p.connection;long before=audit.qol$rejectedMovementPackets();double y=p.getY();
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+5,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+1,"Actual C2S out-of-range movement rejected by module");close(h,p.getY(),y,"Rejected packet cannot move player");
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+.4,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+1,"Legal packet accepted by module");
  p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(p.getX(),y+.8,p.getZ(),false,false));h.assertTrue(audit.qol$rejectedMovementPackets()==before+2,"Packet spam cannot buy another climb allowance");
 });h.succeed();}
 public static void featureSwitches(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var abs=h.absolutePos(new BlockPos(2,2,2));h.assertTrue(!Simplequalityoflife.getConfig().qOL.enableAutowalk,"Autowalk disabled by server default");Simplequalityoflife.getConfig().qOL.enableManualCrawl=false;h.assertTrue(!CrawlLimiter.allow(p),"Disabled crawl refused by server");});h.succeed();}
}
