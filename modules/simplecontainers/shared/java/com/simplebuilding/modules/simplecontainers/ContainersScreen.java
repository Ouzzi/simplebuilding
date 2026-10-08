package com.simplebuilding.modules.simplecontainers;

import com.simplebuilding.modules.simplecontainers.client.ContainersClient;
import com.simplebuilding.modules.simplecontainers.style.ContainerStyles;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class ContainersScreen {
    private ContainersScreen() {}

    public static Screen create(Screen parent) {
        ContainersClient.load();
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(ContainersClient.CONFIG), ContainersConfig.class);
        c.normalize();
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplecontainers.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simplecontainers.tab.general"));
        general.addEntry(e.startBooleanToggle(name("enabled"), c.enabled).setDefaultValue(true).setTooltip(tip("enabled"))
                .setSaveConsumer(v -> c.enabled = v).build());
        for (ScreenStyle style : ContainerStyles.all()) {
            String key = "screen." + style.id();
            general.addEntry(e.startBooleanToggle(name(key), c.screens.getOrDefault(style.id(), true)).setDefaultValue(true)
                    .setTooltip(tip(key)).setSaveConsumer(v -> c.screens.put(style.id(), v)).build());
        }
        b.setSavingRunnable(() -> {
            c.normalize();
            ContainersClient.CONFIG = c;
            ContainersClient.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simplecontainers.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simplecontainers.option." + key + ".tooltip");
    }
}
