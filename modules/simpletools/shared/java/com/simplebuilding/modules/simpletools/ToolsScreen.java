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
        var death = b.getOrCreateCategory(Component.translatable("simpletools.tab.death"));
        death.addEntry(e.startBooleanToggle(name("enableSoulbound"), c.enableSoulbound).setDefaultValue(true)
                .setTooltip(tip("enableSoulbound")).setSaveConsumer(v -> c.enableSoulbound = v).build());
        death.addEntry(e.startBooleanToggle(name("enableDeathVault"), c.enableDeathVault).setDefaultValue(true)
                .setTooltip(tip("enableDeathVault")).setSaveConsumer(v -> c.enableDeathVault = v).build());
        death.addEntry(e.startIntField(name("deathDurabilityCap"), c.deathDurabilityCap).setMin(1).setMax(2031).setDefaultValue(50)
                .setTooltip(tip("deathDurabilityCap")).setSaveConsumer(v -> c.deathDurabilityCap = v).build());
        death.addEntry(e.startIntField(name("soulboundAnvilCost"), c.soulboundAnvilCost).setMin(1).setMax(39).setDefaultValue(35)
                .setTooltip(tip("soulboundAnvilCost")).setSaveConsumer(v -> c.soulboundAnvilCost = v).build());
        death.addEntry(e.startDoubleField(name("soulboundTraderChance"), c.soulboundTraderChance).setMin(0).setMax(1).setDefaultValue(0.05)
                .setTooltip(tip("soulboundTraderChance")).setSaveConsumer(v -> c.soulboundTraderChance = v).build());
        death.addEntry(e.startDoubleField(name("soulboundEndCityChance"), c.soulboundEndCityChance).setMin(0).setMax(1).setDefaultValue(0.02)
                .setTooltip(tip("soulboundEndCityChance")).setSaveConsumer(v -> c.soulboundEndCityChance = v).build());
        death.addEntry(e.startIntField(name("deathVaultCostPerItem"), c.deathVaultCostPerItem).setMin(1).setMax(64).setDefaultValue(4)
                .setTooltip(tip("deathVaultCostPerItem")).setSaveConsumer(v -> c.deathVaultCostPerItem = v).build());
        death.addEntry(e.startIntField(name("deathVaultMaxPerOffering"), c.deathVaultMaxPerOffering).setMin(1).setMax(27).setDefaultValue(3)
                .setTooltip(tip("deathVaultMaxPerOffering")).setSaveConsumer(v -> c.deathVaultMaxPerOffering = v).build());
        death.addEntry(e.startIntField(name("deathVaultMaxDeaths"), c.deathVaultMaxDeaths).setMin(1).setMax(100).setDefaultValue(20)
                .setTooltip(tip("deathVaultMaxDeaths")).setSaveConsumer(v -> c.deathVaultMaxDeaths = v).build());
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
