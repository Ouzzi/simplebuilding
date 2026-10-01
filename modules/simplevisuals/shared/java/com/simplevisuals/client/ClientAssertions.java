package com.simplevisuals.client;
import com.simplevisuals.*;import com.simplevisuals.effects.*;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.*;import net.minecraft.client.gui.screens.TitleScreen;import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;import net.minecraft.world.item.*;import net.minecraft.core.component.DataComponents;

/** Executed by the module-owned integration client smoke. Assertions exercise the real client. */
public final class ClientAssertions {
 private static com.simplevisuals.config.SimplevisualsConfig saved;
 public static void title(Minecraft mc){require(mc.gui.screen() instanceof TitleScreen,"Title reached");saved=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(Visuals.CONFIG),com.simplevisuals.config.SimplevisualsConfig.class);}
 public static void world(Minecraft mc){
  require(mc.level!=null&&mc.player!=null,"Title-to-world joined");
  for(var option:ConfigOptions.all(Visuals.CONFIG))require(!Component.translatable("text.autoconfig.simplevisuals.option."+option.path()).getString().startsWith("text.autoconfig"),"Config translation resolves");
  var recorder=new Recording(mc);
  VisualsHud.reset();for(int i=0;i<100;i++){var stack=new ItemStack(Items.STICK);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Pickup "+i));VisualsHud.addPickup(stack,1000000,false);}
  require(VisualsHud.notificationCount()==8,"Pickup queue bound");
  for(var layout:com.simplevisuals.config.SimplevisualsConfig.PickupLayout.values()){Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierLayout=layout;VisualsHud.render(recorder);}
  require(recorder.items==32&&recorder.texts>=64,"All four pickup layouts submit icons/names/counts");
  var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(5);VisualsHud.held(recorder,sword,40);require(recorder.components.stream().anyMatch(t->t.getString().contains("1556")),"Actual held durability rendered");
  var effect=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,100);
  mc.player.addEffect(effect);VisualsHud.tick(mc);require(VisualsHud.effectProgress(effect)==1,"Effect starts full");
  var shorter=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,50);require(VisualsHud.effectProgress(shorter)==.5f,"Effect fraction uses observed max");
  var map=new MapPreview(new net.minecraft.world.level.saveddata.maps.MapId(1),net.minecraft.world.level.saveddata.maps.MapItemSavedData.createFresh(0,0,(byte)0,true,false,net.minecraft.world.level.Level.OVERWORLD));
  require(map.getWidth(mc.font)==128&&map.getHeight(mc.font)==152,"Map image and coordinate size");map.extractImage(mc.font,0,0,128,152,recorder);
  var previousParticle=mc.options.particles().get();var previousLevel=Visuals.CONFIG.particles.globalLevel;Visuals.CONFIG.particles.globalLevel=Intensity.MAXIMUM;Visuals.CONFIG.particles.overrides.clear();
  mc.options.particles().set(net.minecraft.server.level.ParticleStatus.ALL);
  for(var e:EffectRegistry.ALL){Immersion.BUDGET.begin(0);Immersion.emit(mc,e,mc.player.getId(),mc.player.position());require(Immersion.BUDGET.used()==4,"Real Vanilla particle emission "+e.id());}
  Immersion.BUDGET.begin(2);Immersion.emit(mc,EffectRegistry.ALL.getFirst(),mc.player.getId(),mc.player.position());require(Immersion.BUDGET.used()==0,"Minimal refuses particles in emitter");
  Visuals.CONFIG.particles.globalLevel=previousLevel;mc.options.particles().set(previousParticle);
  Visuals.CONFIG.visuals.biomeInfo.enable=true;Visuals.CONFIG.visuals.speedLines.speedThreshold=.05f;mc.player.setDeltaMovement(1,0,0);for(int i=0;i<10;i++)VisualsHud.tick(mc);
  int before=recorder.fills;VisualsHud.render(recorder);require(recorder.fills>before+8,"Real speed overlay draws bounded radial segments");
  VisualsHud.damage(mc.player,3);VisualsHud.render(recorder);
  var commands=new com.mojang.brigadier.CommandDispatcher<Object>();LocalCommands.register(commands,(s,text)->{});
  try{require(commands.execute("simplevisuals config visuals speedLines speedLinesAmount -100",new Object())==1,"Client command registered");require(Visuals.CONFIG.visuals.speedLines.speedLinesAmount==0,"Client command clamps unsafe input");
   require(commands.execute("simplevisuals elytraHelper angles -999 999",new Object())==1&&Visuals.CONFIG.visuals.elytraTargetAngleUp==-90&&Visuals.CONFIG.visuals.elytraTargetAngleDown==90,"Legacy paired angle command clamps");
   require(commands.execute("simplevisuals visuals pickupNotifier offset -99 99999",new Object())==1&&Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierOffsetX==0&&Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierOffsetY==4096,"Legacy offset command clamps");
   require(commands.execute("simplevisuals visuals pickupNotifier elements name false",new Object())==1&&!Visuals.CONFIG.visuals.pickupNotifier.pickupShowName,"Legacy element alias");
  }catch(Exception e){throw new AssertionError(e);}
  Visuals.CONFIG=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(saved),com.simplevisuals.config.SimplevisualsConfig.class);VisualsHud.reset();VisualsHud.addPickup(new ItemStack(Items.DIAMOND),3,false);VisualsHud.addPickup(new ItemStack(Items.EXPERIENCE_BOTTLE),8,true);
 }
 public static void prepareModels(Minecraft mc){Visuals.CONFIG.visuals.enableRenamedItemTextures=true;mc.reloadResourcePacks();}
 public static void models(Minecraft mc,boolean modelsPresent){
  require(RenamedModels.modelsLoaded==modelsPresent,"Legacy guard matches actual Simple Models presence");
  var stack=new ItemStack(Items.STICK);stack.set(DataComponents.CUSTOM_NAME,Component.literal("JsonTest"));
  var originalModel=stack.get(DataComponents.ITEM_MODEL);var result=RenamedModels.renderStack(stack);
  if(modelsPresent){
   require(RenamedModels.entries(Items.STICK).isEmpty(),"Simple Models owns renamed models; Legacy rules stay inactive");
   require(result==stack&&java.util.Objects.equals(stack.get(DataComponents.ITEM_MODEL),originalModel),"Legacy bridge leaves Simple Models rendering untouched");
  }else{
   require(RenamedModels.entries(Items.STICK).size()>=2,"Legacy CIT resources loaded");
   require(result!=stack&&!java.util.Objects.equals(result.get(DataComponents.ITEM_MODEL),originalModel)&&java.util.Objects.equals(stack.get(DataComponents.ITEM_MODEL),originalModel),"Only render copy receives model selection");
   require(RenamedModels.entries(Items.STICK).getFirst().weight()==10,"Stable weighted match");
  }
  mc.setScreenAndShow(new ModelBrowser(mc.gui.screen(),Items.STICK));
 }
 public static void anvil(Minecraft mc){var menu=new net.minecraft.world.inventory.AnvilMenu(0,mc.player.getInventory());menu.getSlot(0).set(new ItemStack(Items.STICK));mc.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.AnvilScreen(menu,mc.player.getInventory(),Component.translatable("container.repair")));}
 public static void config(Minecraft mc){mc.setScreenAndShow(VisualsConfigScreen.create(mc.gui.screen()));}
 public static void restore(Minecraft mc){Visuals.CONFIG=saved;Visuals.save();VisualsHud.reset();mc.setScreenAndShow(null);}
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private static final class Recording extends GuiGraphicsExtractor {
  int items,texts,fills;final java.util.List<Component> components=new java.util.ArrayList<>();
  Recording(Minecraft mc){super(mc,new GuiRenderState(),0,0);}
  @Override public void item(ItemStack stack,int x,int y){items++;super.item(stack,x,y);}
  @Override public void fill(int x,int y,int x2,int y2,int color){fills++;super.fill(x,y,x2,y2,color);}
  @Override public void text(net.minecraft.client.gui.Font f,Component c,int x,int y,int color){texts++;components.add(c);super.text(f,c,x,y,color);}
  @Override public void text(net.minecraft.client.gui.Font f,String c,int x,int y,int color){texts++;super.text(f,c,x,y,color);}
 }
}
