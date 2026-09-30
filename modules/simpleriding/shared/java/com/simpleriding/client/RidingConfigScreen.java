package com.simpleriding.client;
import com.simpleriding.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import java.nio.file.*;
public final class RidingConfigScreen {
 public static Screen create(Screen parent){
  var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simpleriding.config.server"));
  var entries=builder.entryBuilder();var world=builder.getOrCreateCategory(Component.translatable("text.autoconfig.simpleriding.option.worldGen"));
  var ench=builder.getOrCreateCategory(Component.translatable("text.autoconfig.simpleriding.option.enchantments")); var gson=new com.google.gson.Gson();var c=gson.fromJson(gson.toJson(Riding.CONFIG),RidingConfig.class);
  world.addEntry(entries.startBooleanToggle(name("worldGen.enableVillagerTrades"),c.worldGen.enableVillagerTrades).setDefaultValue(true).setTooltip(tip("worldGen.enableVillagerTrades")).setSaveConsumer(v->c.worldGen.enableVillagerTrades=v).build());
  world.addEntry(entries.startBooleanToggle(name("worldGen.enableLootTableChanges"),c.worldGen.enableLootTableChanges).setDefaultValue(true).setTooltip(tip("worldGen.enableLootTableChanges")).setSaveConsumer(v->c.worldGen.enableLootTableChanges=v).build());
  ench.addEntry(entries.startFloatField(name("enchantments.swiftRide.ghastSpeedMultiplier"),c.enchantments.swiftRide.ghastSpeedMultiplier).setDefaultValue(.85f).setMin(0).setMax(1).setTooltip(tip("enchantments.swiftRide.ghastSpeedMultiplier")).setSaveConsumer(v->c.enchantments.swiftRide.ghastSpeedMultiplier=v).build());
  ench.addEntry(entries.startFloatField(name("enchantments.swiftRide.horseSpeedMultiplier"),c.enchantments.swiftRide.horseSpeedMultiplier).setDefaultValue(.3f).setMin(0).setMax(1).setTooltip(tip("enchantments.swiftRide.horseSpeedMultiplier")).setSaveConsumer(v->c.enchantments.swiftRide.horseSpeedMultiplier=v).build());
  ench.addEntry(entries.startFloatField(name("enchantments.swiftRide.otherSpeedMultiplier"),c.enchantments.swiftRide.otherSpeedMultiplier).setDefaultValue(.2f).setMin(0).setMax(1).setTooltip(tip("enchantments.swiftRide.otherSpeedMultiplier")).setSaveConsumer(v->c.enchantments.swiftRide.otherSpeedMultiplier=v).build());
  ench.addEntry(entries.startFloatField(name("enchantments.horseJump.jumpStrengthMultiplier"),c.enchantments.horseJump.jumpStrengthMultiplier).setDefaultValue(.2f).setMin(0).setMax(.5f).setTooltip(tip("enchantments.horseJump.jumpStrengthMultiplier")).setSaveConsumer(v->c.enchantments.horseJump.jumpStrengthMultiplier=v).build());
  builder.setSavingRunnable(()->{c.normalize();try{Files.writeString(Path.of("config/simpleriding.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(c));}catch(Exception e){throw new IllegalStateException(e);}});
  return builder.build();
 }
 private static Component name(String p){return Component.translatable("text.autoconfig.simpleriding.option."+p);}
 private static Component tip(String p){return Component.translatable("text.autoconfig.simpleriding.option."+p+".@Tooltip");}
}
