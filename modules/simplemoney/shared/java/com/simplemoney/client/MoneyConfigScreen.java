package com.simplemoney.client;
import com.simplemoney.SimpleMoney;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
public final class MoneyConfigScreen {
 public static Screen create(Screen parent,Path configDir) {
  // A local restart draft; opening/saving a client screen never mutates server gameplay.
  var draft=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(SimpleMoney.config),SimpleMoney.Config.class);
  var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.autoconfig.simplemoney.title"));
  var tab=builder.getOrCreateCategory(Component.translatable("text.autoconfig.simplemoney.category.default"));
  var fields=builder.entryBuilder();
  String prefix="text.autoconfig.simplemoney.option.trades.";
  tab.addEntry(fields.startBooleanToggle(Component.translatable(prefix+"enableVillagerTrades"),draft.trades.enableVillagerTrades).setDefaultValue(true).setTooltip(Component.translatable(prefix+"enableVillagerTrades.tooltip")).setSaveConsumer(v->draft.trades.enableVillagerTrades=v).build());
  tab.addEntry(fields.startBooleanToggle(Component.translatable(prefix+"enableWanderingTrades"),draft.trades.enableWanderingTrades).setDefaultValue(true).setTooltip(Component.translatable(prefix+"enableWanderingTrades.tooltip")).setSaveConsumer(v->draft.trades.enableWanderingTrades=v).build());
  var links=builder.getOrCreateCategory(Component.translatable("text.autoconfig.simplemoney.category.links"));
  String linkPrefix="text.autoconfig.simplemoney.option.links.";
  links.addEntry(fields.startBooleanToggle(Component.translatable(linkPrefix+"enabled"),draft.links.enabled).setDefaultValue(true).setTooltip(Component.translatable(linkPrefix+"enabled.tooltip")).setSaveConsumer(v->draft.links.enabled=v).build());
  for(String key:java.util.List.of("billsPerHour","rarityStep","craftWeight","stock","dailyLimit","cooldownTicks")) {
   try {
    var field=SimpleMoney.Links.class.getField(key);int value=field.getInt(draft.links),def=field.getInt(new SimpleMoney.Links());
    int min=key.equals("cooldownTicks")?20:key.equals("rarityStep")?2:1;
    int max=switch(key){case "billsPerHour"->2;case "rarityStep"->8;case "dailyLimit"->16;case "cooldownTicks"->1200;default->4;};
    links.addEntry(fields.startIntField(Component.translatable(linkPrefix+key),value).setDefaultValue(def).setMin(min).setMax(max).setTooltip(Component.translatable(linkPrefix+key+".tooltip")).setSaveConsumer(v->{try{field.setInt(draft.links,v);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}).build());
   }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  builder.setSavingRunnable(()-> { try { draft.links.normalize(); Files.writeString(configDir.resolve("simplemoney.json"),new com.google.gson.Gson().toJson(draft)); } catch(Exception e) { throw new IllegalStateException(e); } });
  return builder.build();
 }
}
