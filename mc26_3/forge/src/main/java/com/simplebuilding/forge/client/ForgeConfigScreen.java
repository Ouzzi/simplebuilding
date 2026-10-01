package com.simplebuilding.forge.client;

import com.simplebuilding.config.*;
import com.simplebuilding.forgenative.simplebuilding.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/** Local draft; only cosmetic options change immediately, gameplay requires a restart. */
public final class ForgeConfigScreen {
    public static Screen create(Screen parent) {
        var draft = AutoConfig.read(AutoConfig.path(SimplebuildingConfig.class), SimplebuildingConfig.class);
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.autoconfig.simplebuilding.title"));
        boolean remote = Minecraft.getInstance().level != null && !Minecraft.getInstance().hasSingleplayerServer();
        for (var option : ConfigOptions.all()) {
            String tab = "default";
            var chain = new java.util.ArrayList<>(option.parents()); chain.add(option.field());
            for (var field : chain) if (field.isAnnotationPresent(ConfigEntry.Category.class)) tab = field.getAnnotation(ConfigEntry.Category.class).value();
            var category = builder.getOrCreateCategory(Component.translatable("text.autoconfig.simplebuilding.category." + tab));
            Component name = Component.translatable("text.autoconfig.simplebuilding.option." + option.path());
            Component tip = Component.translatable("text.autoconfig.simplebuilding.option." + option.path() + ".@Tooltip");
            var value = option.get(draft);
            ConfigBuilder.Entry<?> entry;
            if (value instanceof Boolean b) entry = builder.startBooleanToggle(name, b).setDefaultValue((Boolean) option.defaultValue()).setSaveConsumer(v -> option.set(draft, v));
            else if (value instanceof Integer n) entry = builder.startIntField(name, n).setDefaultValue((Integer) option.defaultValue()).setMin(bound(option, false)).setMax(bound(option, true)).setSaveConsumer(v -> option.set(draft, v));
            else if (value instanceof Long n) entry = builder.startLongField(name, n).setDefaultValue((Long) option.defaultValue()).setMin(bound(option, false)).setMax(bound(option, true)).setSaveConsumer(v -> option.set(draft, v));
            else if (value instanceof Float n) entry = builder.startFloatField(name, n).setDefaultValue((Float) option.defaultValue()).setMin(bound(option, false)).setMax(bound(option, true)).setSaveConsumer(v -> option.set(draft, v));
            else if (value instanceof Double n) entry = builder.startDoubleField(name, n).setDefaultValue((Double) option.defaultValue()).setMin(bound(option, false)).setMax(bound(option, true)).setSaveConsumer(v -> option.set(draft, v));
            else if (value instanceof String s) entry = builder.startStrField(name, s).setDefaultValue((String) option.defaultValue()).setSaveConsumer(v -> option.set(draft, v));
            else throw new IllegalStateException("Unsupported configuration option: " + option.path());
            entry.setTooltip(tip, Component.translatable(remote && !option.clientSide() ? "simplemods.config.remote" : "simplemods.config.restart"));
            entry.setEditable(!remote || option.clientSide()); category.addEntry(entry.build());
        }
        builder.setSavingRunnable(() -> {
            AutoConfig.write(AutoConfig.path(SimplebuildingConfig.class), draft);
            // Preserve the live server object; no client-to-server gameplay write.
            var active = com.simplebuilding.Simplebuilding.getConfig();
            for (var option : ConfigOptions.all()) if (option.clientSide()) option.set(active, option.get(draft));
        });
        return builder.build();
    }
    private static double bound(ConfigOptions.Option option, boolean upper) {
        var annotation = option.field().getAnnotation(ConfigEntry.BoundedDiscrete.class);
        if (annotation != null) return upper ? annotation.max() : annotation.min();
        var probe = new SimplebuildingConfig();
        Object limit;
        if (option.type() == int.class) limit = upper ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        else if (option.type() == long.class) limit = upper ? Long.MAX_VALUE : Long.MIN_VALUE;
        else if (option.type() == float.class) limit = upper ? Float.MAX_VALUE : -Float.MAX_VALUE;
        else limit = upper ? Double.MAX_VALUE : -Double.MAX_VALUE;
        option.set(probe, limit); probe.validatePostLoad();
        return ((Number) option.get(probe)).doubleValue();
    }
}
