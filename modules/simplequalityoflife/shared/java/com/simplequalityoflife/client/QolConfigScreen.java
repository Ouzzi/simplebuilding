package com.simplequalityoflife.client;
import com.simplequalityoflife.*;
import com.simplequalityoflife.config.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
public final class QolConfigScreen {
 public static Screen create(Screen parent){
  var b=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplequalityoflife.config.title"));
  var c=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(Simplequalityoflife.getLocalConfig()),SimplequalityoflifeConfig.class);
  var defaults=new SimplequalityoflifeConfig();
  add(b,c,defaults,"");add(b,c.qOL,defaults.qOL,"qOL.");
  b.setSavingRunnable(()->{c.normalize();me.shedaniel.autoconfig.AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).setConfig(c);me.shedaniel.autoconfig.AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).save();});return b.build();
 }
 private static void add(ConfigBuilder b,Object c,Object defaults,String prefix){
  for(var f:c.getClass().getFields()){
   if(java.lang.reflect.Modifier.isStatic(f.getModifiers())||f.getName().equals("qOL"))continue;
   var id=prefix+f.getName();var cat=b.getOrCreateCategory(Component.translatable("simplequalityoflife.config.tab."+tab(f.getName())));var e=b.entryBuilder();
   var name=Component.translatable("text.autoconfig.simplequalityoflife.option."+id);var tip=Component.translatable("text.autoconfig.simplequalityoflife.option."+id+".@Tooltip");
   try{Object v=f.get(c),d=f.get(defaults);
    java.util.function.Consumer<Object> save=x->{try{f.set(c,x);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}};
    if(v instanceof Boolean x)cat.addEntry(e.startBooleanToggle(name,x).setDefaultValue((Boolean)d).setTooltip(tip).setSaveConsumer(save::accept).build());
    else if(v instanceof Double x)cat.addEntry(e.startDoubleField(name,x).setMin(min(f.getName())).setMax(max(f.getName())).setDefaultValue((Double)d).setTooltip(tip).setSaveConsumer(save::accept).build());
    else if(v instanceof Integer x)cat.addEntry(e.startIntField(name,x).setMin((int)min(f.getName())).setMax((int)max(f.getName())).setDefaultValue((Integer)d).setTooltip(tip).setSaveConsumer(save::accept).build());
    else if(v instanceof java.util.List<?> x)cat.addEntry(e.startStrList(name,x.stream().map(Object::toString).toList()).setDefaultValue(((java.util.List<?>)d).stream().map(Object::toString).toList()).setTooltip(tip).setSaveConsumer(save::accept).build());
    else if(v instanceof SimplequalityoflifeConfig.SlideActivationMode x)cat.addEntry(e.startEnumSelector(name,SimplequalityoflifeConfig.SlideActivationMode.class,x).setDefaultValue((SimplequalityoflifeConfig.SlideActivationMode)d).setTooltip(tip).setSaveConsumer(save::accept).build());
   }catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
  }
 }
 private static double min(String key){return switch(key){case "ladderClimbingSpeed"->0.2;case "ladderSlideSpeed"->0.15;case "fullDurabilityThreshold"->0.8;case "fullDurabilityBonusMultiplier","vaultCooldownDays"->1;case "linkedContainerRange"->SimplequalityoflifeConfig.MIN_LINKED_RANGE;default->0;};}
 private static double max(String key){return switch(key){case "ladderClimbingSpeed"->0.4;case "ladderSlideSpeed"->0.8;case "fullDurabilityThreshold"->1;case "fullDurabilityBonusMultiplier"->1.5;case "vaultCooldownDays"->36500;case "linkedContainerRange"->SimplequalityoflifeConfig.MAX_LINKED_RANGE;default->100;};}
 public static String tab(String key){return key.contains("Linked")||key.contains("linked")||key.contains("Easy")?"containers":key.contains("ladder")||key.contains("Ladder")||key.contains("Autowalk")||key.contains("Crawl")?"movement":key.contains("Suffix")||key.contains("muted")||key.contains("piglin")?"mobs":key.contains("Weather")||key.contains("Rain")?"weather":(key.contains("vault")||key.contains("Vault"))?"vaults":"interaction";}
}
