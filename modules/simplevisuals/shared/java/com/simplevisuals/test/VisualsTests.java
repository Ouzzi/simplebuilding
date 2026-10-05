package com.simplevisuals.test;
import com.simplevisuals.*;import com.simplevisuals.config.*;import com.simplevisuals.effects.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.resources.Identifier;
import java.util.*;

public final class VisualsTests {
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 public static void launch(GameTestHelper h){
  if(isModLoaded("simplebuilding"))h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:enderite_ingot")),"SimpleBuilding content visible when loaded");
  h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("simplevisuals")!=null,"Command registered");
  h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id->id.getNamespace().equals("simplevisuals")),"No invented persistent token or duplicate gameplay items");
  for(var e:EffectRegistry.ALL)h.assertTrue(BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(e.particle())) instanceof net.minecraft.core.particles.SimpleParticleType,"Vanilla particle loads: "+e.id());
  h.succeed();
 }
 public static void config(GameTestHelper h){
  var saved=Visuals.CONFIG;
  try {
   Visuals.CONFIG=new SimplevisualsConfig();
   for(var level:Intensity.values()){
    Visuals.CONFIG.particles.globalLevel=level;
    h.assertTrue(com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals").name().equals(level.name()),"Loaded provider follows all five live levels");
   }
   // Explicit level mapping: one-to-one and order-preserving (no valueOf(name()) by accident).
   var mapped=new HashSet<com.simplebuilding.framework.api.CosmeticIntensity.Level>();
   for(var level:Intensity.values()){mapped.add(level.shared());h.assertTrue(level.shared().ordinal()==level.ordinal(),"Mapping keeps the order: "+level);}
   h.assertTrue(mapped.size()==Intensity.values().length&&mapped.size()==com.simplebuilding.framework.api.CosmeticIntensity.Level.values().length,"Mapping is one-to-one");
   // Per-effect overrides are published too; an effect without override reads the global level.
   var effect=EffectRegistry.ALL.getFirst().id();var other=EffectRegistry.ALL.get(1).id();
   Visuals.CONFIG.particles.globalLevel=Intensity.NORMAL;
   for(var level:Intensity.values()){
    Visuals.CONFIG.particles.overrides.put(effect,level);
    h.assertTrue(com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals",effect)==level.shared(),"Effect override published: "+level);
    h.assertTrue(com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals",other)==com.simplebuilding.framework.api.CosmeticIntensity.Level.NORMAL,"Effect without override follows global");
   }
   Visuals.CONFIG.particles.overrides.clear();
   h.assertTrue(com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals",effect)==com.simplebuilding.framework.api.CosmeticIntensity.Level.NORMAL,"Removed override falls back to global");
   Visuals.CONFIG=new SimplevisualsConfig();
   h.assertTrue(com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals")==com.simplebuilding.framework.api.CosmeticIntensity.Level.SUBTLE,"Provider follows replaced config without saving");
  } finally { Visuals.CONFIG=saved; }
  var c=new SimplevisualsConfig();h.assertTrue(ConfigOptions.all(c).size()==50,"All original config fields plus global level");
  for(var o:ConfigOptions.all(c))if(o.get() instanceof Number){
   boolean integer=o.field().getType()==int.class;
   if(integer)o.set(Integer.MAX_VALUE);else o.set(Float.POSITIVE_INFINITY);ConfigOptions.normalize(c);
   double v=((Number)o.get()).doubleValue();h.assertTrue(Double.isFinite(v)&&v>=o.min()&&v<=o.max(),"Upper/finite bound "+o.path());
   if(integer)o.set(Integer.MIN_VALUE);else o.set(-Float.MAX_VALUE);ConfigOptions.normalize(c);v=((Number)o.get()).doubleValue();h.assertTrue(v>=o.min()&&v<=o.max(),"Lower bound "+o.path());
   if(!integer){o.set(Float.NaN);ConfigOptions.normalize(c);h.assertTrue(o.get().equals(o.defaultValue()),"NaN uses default "+o.path());}
  }
  String legacy="{\"visuals\":{\"enableChatHeads\":false,\"speedLines\":{\"speedLinesAmount\":1.5},\"deathCoordsMode\":\"APPEND\"}}";
  c=new com.google.gson.Gson().fromJson(legacy,SimplevisualsConfig.class);ConfigOptions.normalize(c);
  h.assertTrue(!c.visuals.enableChatHeads&&c.visuals.speedLines.speedLinesAmount==1.5f&&c.visuals.deathCoordsMode==SimplevisualsConfig.Visuals.DeathCoordsMode.APPEND,"Original paths and enum data retained");
  c.particles.globalLevel=null;c.particles.overrides.put("unknown",Intensity.MAXIMUM);c.particles.overrides.put("cold_breath",null);ConfigOptions.normalize(c);
  h.assertTrue(c.particles.globalLevel==Intensity.SUBTLE&&c.particles.overrides.isEmpty(),"Corrupt levels and unknown effect ids rejected");h.succeed();
 }
 public static void language(GameTestHelper h){
  var json=new HashMap<String,com.google.gson.JsonObject>();
  for(String locale:List.of("en_us","de_de"))try(var input=VisualsTests.class.getResourceAsStream("/assets/simplevisuals/lang/"+locale+".json")){
   h.assertTrue(input!=null,"Language exists");var object=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();json.put(locale,object);
   for(var o:ConfigOptions.all(new SimplevisualsConfig())){
    String key="text.autoconfig.simplevisuals.option."+o.path();h.assertTrue(object.has(key)&&object.has(key+".@Tooltip")&&object.has("simplevisuals.tab."+o.tab()),"Name, tooltip and tab "+key);
    h.assertTrue(object.get(key+".@Tooltip").getAsString().contains(o.defaultValue().toString()),"Default stated "+key);
   }
   for(var e:EffectRegistry.ALL)h.assertTrue(object.has("simplevisuals.effect."+e.id())&&object.has("simplevisuals.effect."+e.id()+".tooltip")&&object.has("simplevisuals.category."+e.category()),"Effect UI complete");
   for(var level:Intensity.values())h.assertTrue(object.has("simplevisuals.level."+level.name().toLowerCase(Locale.ROOT)),"Named levels");
  }catch(java.io.IOException e){h.fail("Language read failed: "+e);}
  h.assertTrue(json.get("en_us").keySet().equals(json.get("de_de").keySet()),"Bilingual key parity");h.succeed();
 }
 public static void budgets(GameTestHelper h){
  var b=new ParticleBudget();b.begin(0);
  h.assertTrue(b.claim(0,Integer.MAX_VALUE)==4&&b.claim(0,4)==0,"Per player flood cap");
  for(int p=1;p<10000;p++)b.claim(p,Integer.MAX_VALUE);
  h.assertTrue(b.used()==24,"Aggregate flood cap");
  b.begin(1);for(int p=0;p<10;p++)b.claim(p,4);h.assertTrue(b.used()==8,"Decreased setting cap");
  b.begin(2);h.assertTrue(b.claim(0,4)==0&&b.used()==0,"Minimal setting refuses all extra particles");
  b.begin(0);h.assertTrue(b.claim(0,Integer.MIN_VALUE)==0,"Negative count refused");for(int p=0;p<8;p++)h.assertTrue(b.claim(p,1)==1,"Bounded tracking slot");h.assertTrue(b.claim(8,1)==0,"Ninth player refused");
  b.begin(0);h.assertTrue(b.claim(8,4)==4,"New tick resets budgets");h.succeed();
 }
 public static void assetBounds(GameTestHelper h){
  byte[] bytes=new byte[24];byte[] signature={(byte)137,80,78,71,13,10,26,10};System.arraycopy(signature,0,bytes,0,8);
  var buffer=java.nio.ByteBuffer.wrap(bytes);buffer.putInt(8,13);buffer.putInt(12,0x49484452);buffer.putInt(16,256);buffer.putInt(20,256);h.assertTrue(AssetBounds.pngHeader(bytes),"Maximum safe image admitted");
  buffer.putInt(12,0);h.assertTrue(!AssetBounds.pngHeader(bytes),"Forged first chunk cannot hide oversized IHDR");buffer.putInt(12,0x49484452);
  buffer.putInt(16,Integer.MAX_VALUE);h.assertTrue(!AssetBounds.pngHeader(bytes),"PNG decompression bomb refused before native allocation");buffer.putInt(16,-1);h.assertTrue(!AssetBounds.pngHeader(bytes),"Negative size refused");h.assertTrue(!AssetBounds.pngHeader(new byte[4]),"Truncated image refused");
  try{AssetBounds.readRule(new java.io.ByteArrayInputStream(new byte[AssetBounds.MAX_RULE_BYTES+1]));h.fail("Oversized JSON admitted");}catch(java.io.IOException expected){}
  h.succeed();
 }
 public static void effect(GameTestHelper h,String id){
  var e=EffectRegistry.ALL.stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();
  h.assertTrue(e.interval()>=1&&e.particle().startsWith("minecraft:"),"Bounded Vanilla effect");
  var c=new SimplevisualsConfig();h.assertTrue(EffectRegistry.level(c,id)==Intensity.SUBTLE,"Safe global default");
  for(var global:Intensity.values())for(var override:Intensity.values()){
   c.particles.globalLevel=global;c.particles.overrides.put(id,override);h.assertTrue(EffectRegistry.level(c,id)==override,"Override wins including Off");
   var b=new ParticleBudget();b.begin(0);h.assertTrue(b.claim(0,EffectRegistry.level(c,id).count())==override.ordinal(),"Intensity policy "+id);
  }
  c.particles.overrides.clear();c.particles.globalLevel=Intensity.OFF;h.assertTrue(EffectRegistry.level(c,id)==Intensity.OFF,"Global Off inherited");h.succeed();
 }
 public static void formatting(GameTestHelper h){
  h.assertTrue(Visuals.formatName("&cTest&z",true).equals("§cTest&z"),"Only valid codes converted");
  h.assertTrue(Visuals.formatName("§cTest",false).equals("Test"),"Server off rejects bypassed client formatting");
  h.assertTrue(Visuals.formatName("a\nb\u007fc",true).equals("abc"),"Control characters removed");
  h.assertTrue(Visuals.formatName("x".repeat(51),true)==null&&Visuals.formatName("x".repeat(50),true).length()==50,"Vanilla length cap enforced");
  var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
  var menu=new net.minecraft.world.inventory.AnvilMenu(0,player.getInventory());
  // A SimpleBuilding stack when present, otherwise a Vanilla stack: the anvil rules are the same.
  var item=isModLoaded("simplebuilding")?BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_ingot")):net.minecraft.world.item.Items.IRON_INGOT;
  var stack=new net.minecraft.world.item.ItemStack(item,3);menu.getSlot(0).set(stack);
  boolean previous=Visuals.serverFormatting();boolean localPrevious=Visuals.CONFIG.visuals.enableAnvilFormatting;
  try{Visuals.setServerFormatting(true);menu.setItemName("&cCross-mod");
   h.assertTrue(menu.getSlot(2).getItem().is(item)&&menu.getSlot(2).getItem().getCount()==3,"Cross-mod stack unchanged");
   h.assertTrue(menu.getSlot(2).getItem().getHoverName().getString().equals("§cCross-mod"),"Server validates real anvil output");
   h.assertTrue(menu.getCost()==1,"Vanilla rename cost retained");
   Visuals.setServerFormatting(false);Visuals.CONFIG.visuals.enableAnvilFormatting=true;menu.setItemName("§aNo bypass");h.assertTrue(menu.getSlot(2).getItem().getHoverName().getString().equals("No bypass"),"Real menu rejects forged client name when server off");
  h.assertTrue(!Visuals.serverFormatting(),"Client config cannot change integrated server policy");
  }finally{Visuals.setServerFormatting(previous);Visuals.CONFIG.visuals.enableAnvilFormatting=localPrevious;}
  h.succeed();
 }
}
