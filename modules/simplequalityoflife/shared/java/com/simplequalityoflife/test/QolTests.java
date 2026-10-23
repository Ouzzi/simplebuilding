package com.simplequalityoflife.test;
import com.simplequalityoflife.*;
import com.simplequalityoflife.config.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import java.util.*;
import java.util.function.Consumer;
/** Super-mod tests: guide, launch, config/lang and the sub-mod switches; the feature tests moved with the sub-mods. */
public final class QolTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 /** Super-mod switch field and the sub-mod id it reaches through framework SubMods. */
 static final String[][] SWITCHES={{"enableSimpleWeather","simpleweather"},{"enableSimpleInterfaces","simpleinterfaces"},{"enableSimpleLoot","simpleloot"},
  {"enableSimpleMobManners","simplemobmanners"},{"enableSimpleFarming","simplefarming"},{"enableSimpleTools","simpletools"},{"enableSimpleMovement","simplemovement"}};
 static {
  ALL.put("guide_book",com.simplequalityoflife.guide.QolGuide::gameTest);
  ALL.put("launch",QolTests::launch);ALL.put("config_lang",QolTests::configLang);
  ALL.put("sub_mod_switch",QolTests::subModSwitch);
 }
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath("simplequalityoflife",s);}
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 public static void launch(GameTestHelper h){
  for(var registry:List.<net.minecraft.core.Registry<?>>of(BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK,BuiltInRegistries.ENTITY_TYPE))h.assertTrue(registry.keySet().stream().noneMatch(i->i.getNamespace().equals("simplequalityoflife")&&!(registry==BuiltInRegistries.ITEM&&i.getPath().equals("guide_book"))),"No invented registry content (only the guide item)");
  var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();h.assertTrue(root.getChild("simplequalityoflife")!=null,"/simplequalityoflife registered");
  // The bundled sub-mods bring their own commands (and /crawl); Thrift keeps its old id through Simple Tools.
  for(String sub:List.of("simpleloot","simplemobmanners","simplecreatures","simplefarming","simpletools","simplemovement","crawl"))h.assertTrue(root.getChild(sub)!=null,"bundled sub-mod command /"+sub);
  h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse("simplequalityoflife:thrift"))).isPresent(),"Thrift id unchanged");
  if(isModLoaded("simplebuilding"))h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:reinforced_hopper")),"SimpleBuilding content visible when loaded");
  h.succeed();
 }
 public static void configLang(GameTestHelper h){
  try{var en=language("en_us");var de=language("de_de");h.assertTrue(en.keySet().equals(de.keySet()),"Both locales have exactly the same keys");var c=new SimplequalityoflifeConfig();
   for(var field:c.getClass().getFields()){
    if(java.lang.reflect.Modifier.isStatic(field.getModifiers()))continue;
    String key="text.autoconfig.simplequalityoflife.option."+field.getName();
    for(var lang:List.of(en,de)){h.assertTrue(lang.has(key)&&lang.has(key+".@Tooltip"),"Name/tooltip "+field.getName());h.assertTrue(lang.get(key+".@Tooltip").getAsString().replace(" ","").contains(String.valueOf(field.get(c))),"Default in tooltip "+field.getName());}
   }
   h.assertTrue(en.has("text.autoconfig.simplequalityoflife.category.submods")&&de.has("text.autoconfig.simplequalityoflife.category.submods"),"Tab submods");
   // An old file with qOL.* options: the moved keys are ignored here (each sub-mod takes its values over), the switches default on.
   var loaded=new com.google.gson.Gson().fromJson("{\"enableSimpleTools\":false,\"frostWalkerWalkOnPowderSnow\":false,\"qOL\":{\"linkedContainerRange\":32,\"ladderClimbingSpeed\":0.3}}",SimplequalityoflifeConfig.class);loaded.normalize();
   h.assertTrue(!loaded.enableSimpleTools&&loaded.enableSimpleMovement&&loaded.enableSimpleInterfaces,"Legacy keys ignored, switches kept or defaulted");
   // Renamed sub-mod (Simple Creatures -> Simple Mob Manners): the old switch key is taken over once.
   var file=java.nio.file.Files.createTempDirectory("qol-rename").resolve("simplequalityoflife.json");java.nio.file.Files.writeString(file,"{\"enableSimpleCreatures\":false,\"enableSimpleTools\":false}");
   h.assertTrue(Simplequalityoflife.adoptRenamedSwitches(file),"old switch key renamed");var renamed=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(file),SimplequalityoflifeConfig.class);
   h.assertTrue(!renamed.enableSimpleMobManners&&!renamed.enableSimpleTools&&renamed.enableSimpleLoot&&!java.nio.file.Files.readString(file).contains("enableSimpleCreatures"),"Enable Simple Mob Manners keeps the old off value");
   h.assertTrue(!Simplequalityoflife.adoptRenamedSwitches(file),"second start: nothing left to rename");
  }catch(Exception e){throw new IllegalStateException(e);}h.succeed();
 }
 private static com.google.gson.JsonObject language(String l)throws Exception{try(var in=Simplequalityoflife.class.getResourceAsStream("/assets/simplequalityoflife/lang/"+l+".json")){return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 /** Super-mod: each "Enable Simple XY" reaches exactly its bundled sub-mod through framework SubMods (no class import). */
 public static void subModSwitch(GameTestHelper h){
  try{
   for(String[] pair:SWITCHES){
    var c=new SimplequalityoflifeConfig();var field=SimplequalityoflifeConfig.class.getField(pair[0]);
    h.assertTrue(field.getBoolean(c),pair[0]+" defaults on");
    field.setBoolean(c,false);Simplequalityoflife.subMods(c);
    h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled(pair[1]),pair[0]+" off reaches "+pair[1]);
    for(String[] other:SWITCHES)if(other!=pair)h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled(other[1]),pair[0]+" leaves "+other[1]+" alone");
   }
  }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  finally{Simplequalityoflife.subMods(Simplequalityoflife.getConfig());}
  for(String[] pair:SWITCHES)h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled(pair[1])==switchOf(pair[0]),"Switch restored: "+pair[1]);
  h.succeed();
 }
 private static boolean switchOf(String field){try{return SimplequalityoflifeConfig.class.getField(field).getBoolean(Simplequalityoflife.getConfig());}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplequalityoflife.discover.QolDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplequalityoflife.discover.QolDiscover::behaviourHints);
    }
}
