package com.simplebuilding.modules.simpleloot;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class LootScreen {
    private LootScreen() {}

    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(SimpleLoot.CONFIG), LootConfig.class);
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simpleloot.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simpleloot.tab.general"));
        general.addEntry(e.startBooleanToggle(name("enableVaultCooldown"), c.enableVaultCooldown).setDefaultValue(true)
                .setTooltip(tip("enableVaultCooldown")).setSaveConsumer(v -> c.enableVaultCooldown = v).build());
        general.addEntry(e.startIntField(name("vaultCooldownDays"), c.vaultCooldownDays).setMin(1).setMax(36500)
                .setDefaultValue(LootConfig.DEFAULT_VAULT_COOLDOWN_DAYS).setTooltip(tip("vaultCooldownDays"))
                .setSaveConsumer(v -> c.vaultCooldownDays = v).build());
        b.setSavingRunnable(() -> {
            SimpleLoot.CONFIG = c;
            SimpleLoot.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simpleloot.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simpleloot.option." + key + ".tooltip");
    }
}
