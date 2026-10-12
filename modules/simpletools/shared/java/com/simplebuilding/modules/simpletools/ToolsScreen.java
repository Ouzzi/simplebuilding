package com.simplebuilding.modules.simpletools;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class ToolsScreen {
    private ToolsScreen() {}

    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(SimpleTools.CONFIG), ToolsConfig.class);
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simpletools.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simpletools.tab.general"));
        general.addEntry(e.startBooleanToggle(name("enableFullDurabilityBonus"), c.enableFullDurabilityBonus).setDefaultValue(true)
                .setTooltip(tip("enableFullDurabilityBonus")).setSaveConsumer(v -> c.enableFullDurabilityBonus = v).build());
        general.addEntry(e.startDoubleField(name("fullDurabilityThreshold"), c.fullDurabilityThreshold).setMin(0.8).setMax(1)
                .setDefaultValue(ToolsConfig.DEFAULT_DURABILITY_THRESHOLD).setTooltip(tip("fullDurabilityThreshold"))
                .setSaveConsumer(v -> c.fullDurabilityThreshold = v).build());
        general.addEntry(e.startDoubleField(name("fullDurabilityBonusMultiplier"), c.fullDurabilityBonusMultiplier).setMin(1).setMax(ToolsConfig.MAX_BONUS)
                .setDefaultValue(ToolsConfig.DEFAULT_DURABILITY_BONUS_MULTIPLIER).setTooltip(tip("fullDurabilityBonusMultiplier"))
                .setSaveConsumer(v -> c.fullDurabilityBonusMultiplier = v).build());
        general.addEntry(e.startBooleanToggle(name("anvilRepairKeepsCost"), c.anvilRepairKeepsCost).setDefaultValue(true)
                .setTooltip(tip("anvilRepairKeepsCost")).setSaveConsumer(v -> c.anvilRepairKeepsCost = v).build());
        general.addEntry(e.startBooleanToggle(name("enableFurnaceLavaFill"), c.enableFurnaceLavaFill).setDefaultValue(true)
                .setTooltip(tip("enableFurnaceLavaFill")).setSaveConsumer(v -> c.enableFurnaceLavaFill = v).build());
        b.setSavingRunnable(() -> {
            SimpleTools.CONFIG = c;
            SimpleTools.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simpletools.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simpletools.option." + key + ".tooltip");
    }
}
