package com.simplequalityoflife.test;
import com.simplequalityoflife.*;
import com.simplequalityoflife.config.*;
import com.simplequalityoflife.event.*;
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
  ALL.put("launch",QolTests::launch);ALL.put("config_lang",QolTests::configLang);
  
  
  
  ALL.put("sub_mod_switch",QolTests::subModSwitch);
  
  
  
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
 private static void configured(Runnable body){var c=Simplequalityoflife.getConfig();var old=c.qOL;try{c.qOL=new SimplequalityoflifeConfig.QOL();body.run();}finally{c.qOL=old;}}
 public static void launch(GameTestHelper h){
  for(var registry:List.<net.minecraft.core.Registry<?>>of(BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK,BuiltInRegistries.ENTITY_TYPE))h.assertTrue(registry.keySet().stream().noneMatch(i->i.getNamespace().equals("simplequalityoflife")&&!(registry==BuiltInRegistries.ITEM&&i.getPath().equals("guide_book"))),"No invented registry content (only the guide item)");
  var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();h.assertTrue(root.getChild("simplequalityoflife")!=null,"/simplequalityoflife registered");
  if(isModLoaded("simplebuilding"))h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:reinforced_hopper")),"SimpleBuilding content visible when loaded");
  h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:blocks/wheat")))!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Crop loot loads");h.succeed();
 }
 public static void configLang(GameTestHelper h){
  try{var gson=new com.google.gson.Gson();var en=language("en_us");var de=language("de_de");h.assertTrue(en.keySet().equals(de.keySet()),"Both locales have exactly the same keys");var c=new SimplequalityoflifeConfig();
   for(Object object:List.of(c,c.qOL))for(var field:object.getClass().getFields()){
    if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||field.getName().equals("qOL"))continue;
    String p=(object==c?"":"qOL.")+field.getName();String key="text.autoconfig.simplequalityoflife.option."+p;
    for(var lang:List.of(en,de)){h.assertTrue(lang.has(key)&&lang.has(key+".@Tooltip"),"Name/tooltip "+p);h.assertTrue(lang.get(key+".@Tooltip").getAsString().replace(" ", "").contains(gson.toJson(field.get(object)).replace(" ", "")),"Default in tooltip "+p);}
   }
   for(String tab:List.of("submods","containers"))h.assertTrue(en.has("simplequalityoflife.config.tab."+tab)&&de.has("simplequalityoflife.config.tab."+tab),"Tab "+tab);
   var loaded=gson.fromJson("{\"enableSimpleTools\":false,\"frostWalkerWalkOnPowderSnow\":false,\"qOL\":{\"linkedContainerRange\":32,\"ladderClimbingSpeed\":0.3}}",SimplequalityoflifeConfig.class);loaded.normalize();h.assertTrue(!loaded.enableSimpleTools&&loaded.enableSimpleMovement&&loaded.qOL.linkedContainerRange==32&&loaded.qOL.enableEasyShulkers,"Legacy keys of moved options are ignored, missing defaults");
  }catch(Exception e){throw new IllegalStateException(e);}h.succeed();
 }
 private static com.google.gson.JsonObject language(String l)throws Exception{try(var in=Simplequalityoflife.class.getResourceAsStream("/assets/simplequalityoflife/lang/"+l+".json")){return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 /** Super-mod: "Enable Simple Weather" reaches the bundled sub-mod through framework SubMods (no class import). */
 public static void subModSwitch(GameTestHelper h){var c=new SimplequalityoflifeConfig();h.assertTrue(c.enableSimpleWeather,"Sub-mods default on");try{c.enableSimpleWeather=false;Simplequalityoflife.subMods(c);h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled("simpleweather"),"Switch off reaches Simple Weather");}finally{Simplequalityoflife.subMods(Simplequalityoflife.getConfig());}h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled("simpleweather")==Simplequalityoflife.getConfig().enableSimpleWeather,"Switch restored");try{c.enableSimpleWeather=true;c.enableSimpleInterfaces=false;Simplequalityoflife.subMods(c);h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled("simpleinterfaces")&&com.simplebuilding.framework.api.SubMods.enabled("simpleweather"),"Switch off reaches only Simple Interfaces");}finally{Simplequalityoflife.subMods(Simplequalityoflife.getConfig());}h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled("simpleinterfaces")==Simplequalityoflife.getConfig().enableSimpleInterfaces,"Interfaces switch restored");h.succeed();}
}
