package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.client.ContainersClient;
import com.simplebuilding.modules.simpleinterfaces.style.ContainerStyles;
import com.simplebuilding.modules.simpleinterfaces.style.ScreenStyle;
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
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simpleinterfaces.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simpleinterfaces.tab.general"));
        general.addEntry(e.startBooleanToggle(name("enabled"), c.enabled).setDefaultValue(true).setTooltip(tip("enabled"))
                .setSaveConsumer(v -> c.enabled = v).build());
        general.addEntry(e.startBooleanToggle(name("showStyleToggle"), c.showStyleToggle).setDefaultValue(false)
                .setTooltip(tip("showStyleToggle")).setSaveConsumer(v -> c.showStyleToggle = v).build());
        // N49: brightness of the mod inventory / container boxes (SimpleLib, config/simplelib-client.json)
        com.simplelib.api.client.ui.UiBrightness[] brightness = {com.simplelib.api.client.ui.UiBrightness.current()};
        general.addEntry(e.startEnumSelector(name("inventoryBrightness"), com.simplelib.api.client.ui.UiBrightness.class, brightness[0])
                .setDefaultValue(com.simplelib.api.client.ui.UiBrightness.LIGHT)
                .setEnumNameProvider(v -> Component.translatable("simpleinterfaces.option.inventoryBrightness."
                        + ((com.simplelib.api.client.ui.UiBrightness) v).id()))
                .setTooltip(tip("inventoryBrightness")).setSaveConsumer(v -> brightness[0] = v).build());
        for (ScreenStyle style : ContainerStyles.all()) {
            String key = "screen." + style.id();
            general.addEntry(e.startBooleanToggle(name(key), c.screens.getOrDefault(style.id(), true)).setDefaultValue(true)
                    .setTooltip(tip(key)).setSaveConsumer(v -> c.screens.put(style.id(), v)).build());
        }
        // Server values (formerly Simple QoL): linked containers and Easy Shulkers / Ender Chests.
        var s = gson.fromJson(gson.toJson(SimpleInterfaces.CONFIG), LinkedConfig.class);
        var server = b.getOrCreateCategory(Component.translatable("simpleinterfaces.tab.server"));
        server.addEntry(e.startBooleanToggle(name("enableLinkedContainers"), s.enableLinkedContainers).setDefaultValue(true)
                .setTooltip(tip("enableLinkedContainers")).setSaveConsumer(v -> s.enableLinkedContainers = v).build());
        server.addEntry(e.startIntField(name("linkedContainerRange"), s.linkedContainerRange).setMin(LinkedConfig.MIN_LINKED_RANGE)
                .setMax(LinkedConfig.MAX_LINKED_RANGE).setDefaultValue(LinkedConfig.DEFAULT_LINKED_RANGE).setTooltip(tip("linkedContainerRange"))
                .setSaveConsumer(v -> s.linkedContainerRange = v).build());
        server.addEntry(e.startBooleanToggle(name("enableEasyShulkers"), s.enableEasyShulkers).setDefaultValue(true)
                .setTooltip(tip("enableEasyShulkers")).setSaveConsumer(v -> s.enableEasyShulkers = v).build());
        server.addEntry(e.startBooleanToggle(name("enableEasyEnderChests"), s.enableEasyEnderChests).setDefaultValue(true)
                .setTooltip(tip("enableEasyEnderChests")).setSaveConsumer(v -> s.enableEasyEnderChests = v).build());
        b.setSavingRunnable(() -> {
            c.normalize();
            ContainersClient.CONFIG = c;
            ContainersClient.save();
            com.simplelib.api.client.ui.UiBrightness.set(brightness[0]);
            com.simplelib.api.client.ui.UiBrightness.save();
            SimpleInterfaces.CONFIG = s;
            SimpleInterfaces.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simpleinterfaces.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simpleinterfaces.option." + key + ".tooltip");
    }
}
