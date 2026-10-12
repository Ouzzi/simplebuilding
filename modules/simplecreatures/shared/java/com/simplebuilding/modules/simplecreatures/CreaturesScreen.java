package com.simplebuilding.modules.simplecreatures;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class CreaturesScreen {
    private CreaturesScreen() {}

    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(SimpleCreatures.CONFIG), CreaturesConfig.class);
        var d = new CreaturesConfig();
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplecreatures.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simplecreatures.tab.general"));
        general.addEntry(e.startStrList(name("mutedEntities"), c.mutedEntities).setDefaultValue(d.mutedEntities)
                .setTooltip(tip("mutedEntities")).setSaveConsumer(v -> c.mutedEntities = new java.util.ArrayList<>(v)).build());
        general.addEntry(e.startStrList(name("nametagMuteSuffixes"), c.nametagMuteSuffixes).setDefaultValue(d.nametagMuteSuffixes)
                .setTooltip(tip("nametagMuteSuffixes")).setSaveConsumer(v -> c.nametagMuteSuffixes = new java.util.ArrayList<>(v)).build());
        general.addEntry(e.startStrList(name("nametagBabySuffixes"), c.nametagBabySuffixes).setDefaultValue(d.nametagBabySuffixes)
                .setTooltip(tip("nametagBabySuffixes")).setSaveConsumer(v -> c.nametagBabySuffixes = new java.util.ArrayList<>(v)).build());
        general.addEntry(e.startBooleanToggle(name("piglinsIgnoreGoldTrims"), c.piglinsIgnoreGoldTrims).setDefaultValue(true)
                .setTooltip(tip("piglinsIgnoreGoldTrims")).setSaveConsumer(v -> c.piglinsIgnoreGoldTrims = v).build());
        general.addEntry(e.startBooleanToggle(name("piglinsIgnoreGoldTools"), c.piglinsIgnoreGoldTools).setDefaultValue(true)
                .setTooltip(tip("piglinsIgnoreGoldTools")).setSaveConsumer(v -> c.piglinsIgnoreGoldTools = v).build());
        b.setSavingRunnable(() -> {
            SimpleCreatures.CONFIG = c;
            SimpleCreatures.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simplecreatures.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simplecreatures.option." + key + ".tooltip");
    }
}
