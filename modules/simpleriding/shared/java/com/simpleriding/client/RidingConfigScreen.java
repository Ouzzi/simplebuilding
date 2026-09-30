package com.simpleriding.client;

import com.simpleriding.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import java.nio.file.*;

public final class RidingConfigScreen {
 public static Screen create(Screen parent) {
  var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simpleriding.config.server"));
  var entries=builder.entryBuilder();
  var gson=new com.google.gson.Gson();
  var c=gson.fromJson(gson.toJson(Riding.CONFIG),RidingConfig.class);
  c.normalize();
  for(var option:RidingOptions.ALL) {
   var category=builder.getOrCreateCategory(Component.translatable(option.tabKey()));
   var name=Component.translatable(option.nameKey());
   var tip=Component.translatable(option.nameKey()+".@Tooltip");
   switch(option.kind()) {
    case BOOLEAN -> category.addEntry(entries.startBooleanToggle(name,(Boolean)option.get(c)).setDefaultValue((Boolean)option.defaultValue()).setTooltip(tip).setSaveConsumer(v->option.set(c,v)).build());
    case INTEGER -> category.addEntry(entries.startIntField(name,(Integer)option.get(c)).setDefaultValue((Integer)option.defaultValue()).setMin((int)option.minimum()).setMax((int)option.maximum()).setTooltip(tip).setSaveConsumer(v->option.set(c,v)).build());
    case FLOAT -> category.addEntry(entries.startFloatField(name,(Float)option.get(c)).setDefaultValue((Float)option.defaultValue()).setMin((float)option.minimum()).setMax((float)option.maximum()).setTooltip(tip).setSaveConsumer(v->option.set(c,v)).build());
   }
  }
  // Editing the local file never mutates the running integrated or remote server's values.
  builder.setSavingRunnable(()->{c.normalize();try{Files.createDirectories(Path.of("config"));Files.writeString(Path.of("config/simpleriding.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(c));}catch(Exception e){throw new IllegalStateException(e);}});
  return builder.build();
 }
}
