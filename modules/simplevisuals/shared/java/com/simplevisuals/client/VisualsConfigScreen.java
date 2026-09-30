package com.simplevisuals.client;

import com.simplevisuals.*;
import com.simplevisuals.config.SimplevisualsConfig;
import com.simplevisuals.effects.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

/** Local cosmetic settings. Anvil formatting is additionally enforced by the server. */
public final class VisualsConfigScreen {
    public enum OverrideLevel { INHERIT, OFF, SUBTLE, NORMAL, STRONG, MAXIMUM }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var copy = gson.fromJson(gson.toJson(Visuals.CONFIG), SimplevisualsConfig.class);
        ConfigOptions.normalize(copy);
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.autoconfig.simplevisuals.title"));
        var entries = builder.entryBuilder();
        for (var option : ConfigOptions.all(copy)) {
            var category = builder.getOrCreateCategory(Component.translatable("simplevisuals.tab." + option.tab()));
            var name = Component.translatable("text.autoconfig.simplevisuals.option." + option.path());
            var tip = Component.translatable("text.autoconfig.simplevisuals.option." + option.path() + ".@Tooltip");
            Object value = option.get();
            if (value instanceof Boolean b) { var entry=entries.startBooleanToggle(name, b).setDefaultValue((Boolean) option.defaultValue()).setTooltip(tip).setSaveConsumer(option::set).build(); entry.setEditable(!option.path().endsWith("enableAnvilFormatting")); category.addEntry(entry); }
            else if (value instanceof Integer i) category.addEntry(entries.startIntField(name, i).setDefaultValue((Integer) option.defaultValue()).setMin((int) option.min()).setMax((int) option.max()).setTooltip(tip).setSaveConsumer(option::set).build());
            else if (value instanceof Float f) category.addEntry(entries.startFloatField(name, f).setDefaultValue((Float) option.defaultValue()).setMin((float) option.min()).setMax((float) option.max()).setTooltip(tip).setSaveConsumer(option::set).build());
            else if (value instanceof Enum e) category.addEntry(entries.startEnumSelector(name, e.getDeclaringClass(), e).setDefaultValue((Enum) option.defaultValue()).setEnumNameProvider(v -> enumName((Enum) v)).setTooltip(tip).setSaveConsumer(option::set).build());
        }
        for (var effect : EffectRegistry.ALL) {
            var category = builder.getOrCreateCategory(Component.translatable("simplevisuals.category." + effect.category()));
            var level = copy.particles.overrides.get(effect.id());
            var initial = level == null ? OverrideLevel.INHERIT : OverrideLevel.valueOf(level.name());
            category.addEntry(entries.startEnumSelector(Component.translatable("simplevisuals.effect." + effect.id()), OverrideLevel.class, initial)
                .setDefaultValue(OverrideLevel.INHERIT).setEnumNameProvider(VisualsConfigScreen::enumName)
                .setTooltip(Component.translatable("simplevisuals.effect." + effect.id() + ".tooltip"))
                .setSaveConsumer(v -> { if (v == OverrideLevel.INHERIT) copy.particles.overrides.remove(effect.id()); else copy.particles.overrides.put(effect.id(), Intensity.valueOf(v.name())); }).build());
        }
        builder.setSavingRunnable(() -> { boolean reload = Visuals.CONFIG.visuals.enableRenamedItemTextures != copy.visuals.enableRenamedItemTextures; Visuals.CONFIG = copy; Visuals.save(); if (reload) net.minecraft.client.Minecraft.getInstance().reloadResourcePacks(); });
        return builder.build();
    }
    private static Component enumName(Enum<?> e) {
        if (e instanceof Intensity || e instanceof OverrideLevel) return Component.translatable("simplevisuals.level." + e.name().toLowerCase(java.util.Locale.ROOT));
        return Component.translatable("simplevisuals.enum." + e.name());
    }
}
