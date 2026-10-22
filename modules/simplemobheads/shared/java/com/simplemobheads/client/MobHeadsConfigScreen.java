package com.simplemobheads.client;

import com.google.gson.Gson;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.config.MobHeadsConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge builds native widgets from this file): drops, heads, one switch per ability. */
public final class MobHeadsConfigScreen {
    private MobHeadsConfigScreen() {
    }

    public static Screen build(Screen parent) {
        Gson gson = new Gson();
        MobHeadsConfig config = gson.fromJson(gson.toJson(SimpleMobHeads.config()), MobHeadsConfig.class);
        config.normalize(SimpleMobHeads.abilityIds());
        MobHeadsConfig defaults = new MobHeadsConfig();
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("config.simplemobheads.title"));
        var entries = builder.entryBuilder();
        var drops = builder.getOrCreateCategory(Component.translatable("config.simplemobheads.category.drops"));
        for (Field field : MobHeadsConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("abilities")) {
                continue;
            }
            String key = "config.simplemobheads." + field.getName();
            try {
                if (field.getType() == boolean.class) {
                    drops.addEntry(entries.startBooleanToggle(Component.translatable(key), field.getBoolean(config))
                            .setDefaultValue(field.getBoolean(defaults)).setTooltip(Component.translatable(key + ".tooltip"))
                            .setSaveConsumer(value -> set(field, config, value)).build());
                } else if (field.getType() == float.class) {
                    float[] range = MobHeadsConfig.range(field.getName());
                    drops.addEntry(entries.startFloatField(Component.translatable(key), field.getFloat(config))
                            .setDefaultValue(field.getFloat(defaults)).setMin(range[0]).setMax(range[1]).setTooltip(Component.translatable(key + ".tooltip"))
                            .setSaveConsumer(value -> set(field, config, value)).build());
                } else if (field.getType() == int.class) {
                    float[] range = MobHeadsConfig.range(field.getName());
                    drops.addEntry(entries.startIntField(Component.translatable(key), field.getInt(config))
                            .setDefaultValue(field.getInt(defaults)).setMin((int) range[0]).setMax((int) range[1]).setTooltip(Component.translatable(key + ".tooltip"))
                            .setSaveConsumer(value -> set(field, config, value)).build());
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        var abilities = builder.getOrCreateCategory(Component.translatable("config.simplemobheads.category.abilities"));
        for (String id : SimpleMobHeads.abilityIds()) {
            abilities.addEntry(entries.startBooleanToggle(Component.translatable("ability.simplemobheads." + id), config.ability(id))
                    .setDefaultValue(true).setTooltip(Component.translatable("ability.simplemobheads." + id + ".tooltip"))
                    .setSaveConsumer(value -> config.abilities.put(id, value)).build());
        }
        builder.setSavingRunnable(() -> SimpleMobHeads.setConfig(config));
        return builder.build();
    }

    private static void set(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
