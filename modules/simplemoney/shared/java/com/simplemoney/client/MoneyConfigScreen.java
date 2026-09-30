package com.simplemoney.client;
import com.simplemoney.SimpleMoney;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
public final class MoneyConfigScreen {
 public static Screen create(Screen parent,Path configDir) {
  var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.autoconfig.simplemoney.title"));
  var tab=builder.getOrCreateCategory(Component.translatable("text.autoconfig.simplemoney.category.default"));
  var fields=builder.entryBuilder();
  String prefix="text.autoconfig.simplemoney.option.trades.";
  tab.addEntry(fields.startBooleanToggle(Component.translatable(prefix+"enableVillagerTrades"),SimpleMoney.config.trades.enableVillagerTrades).setDefaultValue(true).setTooltip(Component.translatable(prefix+"enableVillagerTrades.tooltip")).setSaveConsumer(v->SimpleMoney.config.trades.enableVillagerTrades=v).build());
  tab.addEntry(fields.startBooleanToggle(Component.translatable(prefix+"enableWanderingTrades"),SimpleMoney.config.trades.enableWanderingTrades).setDefaultValue(true).setTooltip(Component.translatable(prefix+"enableWanderingTrades.tooltip")).setSaveConsumer(v->SimpleMoney.config.trades.enableWanderingTrades=v).build());
  builder.setSavingRunnable(()-> { try { Files.writeString(configDir.resolve("simplemoney.json"),new com.google.gson.Gson().toJson(SimpleMoney.config)); } catch(Exception e) { throw new IllegalStateException(e); } });
  return builder.build();
 }
}
