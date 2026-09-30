package com.simplebuilding.modules.simplesounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class SoundScreen {
 public enum Override { INHERIT, OFF, SUBTLE, NORMAL, STRONG, MAXIMUM }
 public static Screen create(Screen parent){
  SoundClient.load();var gson=new com.google.gson.Gson();var c=gson.fromJson(gson.toJson(SoundClient.CONFIG),SoundConfig.class);c.normalize();
  var b=me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplesounds.title"));var e=b.entryBuilder();
  var general=b.getOrCreateCategory(Component.translatable("simplesounds.tab.general"));
  general.addEntry(e.startEnumSelector(name("globalLevel"),SoundConfig.Level.class,c.globalLevel).setDefaultValue(SoundConfig.Level.SUBTLE).setEnumNameProvider(SoundScreen::level).setTooltip(tip("globalLevel")).setSaveConsumer(v->c.globalLevel=v).build());
  general.addEntry(e.startFloatField(name("volumeCap"),c.volumeCap).setDefaultValue(.25f).setMin(0).setMax(.25f).setTooltip(tip("volumeCap")).setSaveConsumer(v->c.volumeCap=v).build());
  general.addEntry(e.startIntField(name("soundsPerTick"),c.soundsPerTick).setDefaultValue(2).setMin(0).setMax(4).setTooltip(tip("soundsPerTick")).setSaveConsumer(v->c.soundsPerTick=v).build());
  general.addEntry(e.startIntField(name("soundsPerPlayer"),c.soundsPerPlayer).setDefaultValue(1).setMin(0).setMax(2).setTooltip(tip("soundsPerPlayer")).setSaveConsumer(v->c.soundsPerPlayer=v).build());
  general.addEntry(e.startIntField(name("cooldownTicks"),c.cooldownTicks).setDefaultValue(40).setMin(20).setMax(1200).setTooltip(tip("cooldownTicks")).setSaveConsumer(v->c.cooldownTicks=v).build());
  for(var effect:SoundsRegistry.ALL){var cat=b.getOrCreateCategory(Component.translatable("simplesounds.tab."+effect.category()));var initial=c.overrides.containsKey(effect.id())?Override.valueOf(c.overrides.get(effect.id()).name()):Override.INHERIT;
   cat.addEntry(e.startEnumSelector(name(effect.id()),Override.class,initial).setDefaultValue(Override.INHERIT).setEnumNameProvider(SoundScreen::level).setTooltip(tip(effect.id())).setSaveConsumer(v->{if(v==Override.INHERIT)c.overrides.remove(effect.id());else c.overrides.put(effect.id(),SoundConfig.Level.valueOf(v.name()));}).build());
  }
  b.setSavingRunnable(()->{c.normalize();SoundClient.CONFIG=c;SoundClient.save();});return b.build();
 }
 private static Component name(String s){return Component.translatable("simplesounds.option."+s);}
 private static Component tip(String s){return Component.translatable("simplesounds.option."+s+".tooltip");}
 private static Component level(Enum<?> e){return Component.translatable("simplesounds.level."+e.name().toLowerCase(java.util.Locale.ROOT));}
}
