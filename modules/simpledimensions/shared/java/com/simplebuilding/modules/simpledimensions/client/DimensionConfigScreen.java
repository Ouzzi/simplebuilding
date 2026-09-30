package com.simplebuilding.modules.simpledimensions.client;
import com.simplebuilding.modules.simpledimensions.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import java.nio.file.*;
public final class DimensionConfigScreen {
 private static Component text(String key){return Component.translatable("simpledimension.config."+key);}
 public static Screen create(Screen parent){
  var mc=Minecraft.getInstance();boolean editable=mc.level==null||mc.hasSingleplayerServer();
  var b=ConfigBuilder.create().setParentScreen(parent).setTitle(text(editable?"title":"remote"));var e=b.entryBuilder();
  if(!editable){b.getOrCreateCategory(text("safety")).addEntry(e.startTextDescription(text("remoteNotice")).build());return b.build();}
  var source=DimensionSettings.load(DimensionRuntime.CONFIG_ROOT);
  var access=b.getOrCreateCategory(text("access"));var safety=b.getOrCreateCategory(text("safety"));
  var enabled=e.startBooleanToggle(text("accessEnabled"),source.accessEnabled).setDefaultValue(true).setTooltip(text("accessEnabled.tooltip")).setSaveConsumer(v->source.accessEnabled=v).build();enabled.setEditable(editable);access.addEntry(enabled);
  var automatic=e.startBooleanToggle(text("automaticDestination"),source.automaticDestination).setDefaultValue(true).setTooltip(text("automaticDestination.tooltip")).setSaveConsumer(v->source.automaticDestination=v).build();automatic.setEditable(editable);safety.addEntry(automatic);
  var delay=e.startIntField(text("portalDelayTicks"),source.portalDelayTicks).setMin(0).setMax(200).setDefaultValue(0).setTooltip(text("portalDelayTicks.tooltip")).setSaveConsumer(v->source.portalDelayTicks=v).build();delay.setEditable(editable);access.addEntry(delay);
  var cooldown=e.startIntField(text("teleportCooldownTicks"),source.teleportCooldownTicks).setMin(20).setMax(1200).setDefaultValue(60).setTooltip(text("teleportCooldownTicks.tooltip")).setSaveConsumer(v->source.teleportCooldownTicks=v).build();cooldown.setEditable(editable);safety.addEntry(cooldown);
  var entities=e.startBooleanToggle(text("nonPlayerTravel"),false).setDefaultValue(false).setTooltip(text("nonPlayerTravel.tooltip")).build();entities.setEditable(false);safety.addEntry(entities);
  b.setSavingRunnable(()->{if(!editable)return;source.normalize();try{Files.writeString(DimensionRuntime.CONFIG_ROOT.resolve("server.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(source));}catch(Exception ex){throw new IllegalStateException(ex);}
   var server=mc.getSingleplayerServer();if(server!=null)server.execute(()->{var c=DimensionRuntime.get(server).settings;c.accessEnabled=source.accessEnabled;c.automaticDestination=source.automaticDestination;c.portalDelayTicks=source.portalDelayTicks;c.teleportCooldownTicks=source.teleportCooldownTicks;c.normalize();});
  });return b.build();
 }
}
