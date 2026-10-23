package com.simplequalityoflife.client;
import com.simplequalityoflife.*;
import com.simplequalityoflife.config.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
/** Super-mod screen: one switch per bundled sub-mod; each sub-mod has its own config screen for its options. */
public final class QolConfigScreen {
 public static Screen create(Screen parent){
  var b=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.autoconfig.simplequalityoflife.serverTitle"));
  var c=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(Simplequalityoflife.getLocalConfig()),SimplequalityoflifeConfig.class);
  var defaults=new SimplequalityoflifeConfig();
  var cat=b.getOrCreateCategory(Component.translatable("text.autoconfig.simplequalityoflife.category.submods"));var e=b.entryBuilder();
  for(var f:c.getClass().getFields()){
   if(java.lang.reflect.Modifier.isStatic(f.getModifiers())||f.getType()!=boolean.class)continue;
   var name=Component.translatable("text.autoconfig.simplequalityoflife.option."+f.getName());var tip=Component.translatable("text.autoconfig.simplequalityoflife.option."+f.getName()+".@Tooltip");
   try{cat.addEntry(e.startBooleanToggle(name,f.getBoolean(c)).setDefaultValue(f.getBoolean(defaults)).setTooltip(tip).setSaveConsumer(x->{try{f.setBoolean(c,x);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}}).build());}
   catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
  }
  b.setSavingRunnable(()->{c.normalize();me.shedaniel.autoconfig.AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).setConfig(c);me.shedaniel.autoconfig.AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).save();Simplequalityoflife.subMods(c);});return b.build();
 }
}
