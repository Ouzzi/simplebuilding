package com.simplelib.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Config screen of SimpleLib (Fabric, Mod Menu): the legendary moments (SP22-2). Cloth Config stays optional for SimpleLib - the
 * entry only offers this screen while Cloth is loaded (owner 22.10.: only 26.3 Fabric for now). The warm
 * food values stay in the file (server bounds, see the wiki).
 */
public final class LibConfigScreen {
    private LibConfigScreen() {}

    public static Screen create(Screen parent) {
        boolean[] effects = {LibConfig.legendaryEffects};
        boolean[] allChallenges = {LibConfig.legendaryAllChallenges};
        List<List<String>> lists = new ArrayList<>(List.of(strings(LibConfig.legendaryAdvancements), strings(LibConfig.legendaryEnchantments)));
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplelib.title"));
        var e = b.entryBuilder();
        var legendary = b.getOrCreateCategory(Component.translatable("simplelib.tab.legendary"));
        legendary.addEntry(e.startBooleanToggle(name("legendaryEffects"), effects[0]).setDefaultValue(true)
                .setTooltip(tip("legendaryEffects")).setSaveConsumer(v -> effects[0] = v).build());
        legendary.addEntry(e.startBooleanToggle(name("legendaryAllChallenges"), allChallenges[0]).setDefaultValue(false)
                .setTooltip(tip("legendaryAllChallenges")).setSaveConsumer(v -> allChallenges[0] = v).build());
        legendary.addEntry(e.startStrList(name("legendaryAdvancements"), lists.get(0)).setDefaultValue(List.of())
                .setTooltip(tip("legendaryAdvancements")).setSaveConsumer(v -> lists.set(0, v)).build());
        legendary.addEntry(e.startStrList(name("legendaryEnchantments"), lists.get(1)).setDefaultValue(List.of())
                .setTooltip(tip("legendaryEnchantments")).setSaveConsumer(v -> lists.set(1, v)).build());
        b.setSavingRunnable(() -> {
            LibConfig.legendaryEffects = effects[0];
            LibConfig.legendaryAllChallenges = allChallenges[0];
            LibConfig.legendaryAdvancements = LibConfig.ids(lists.get(0));
            LibConfig.legendaryEnchantments = LibConfig.ids(lists.get(1));
            LibConfig.save();
        });
        return b.build();
    }

    private static List<String> strings(List<Identifier> ids) {
        return new ArrayList<>(ids.stream().map(Identifier::toString).toList());
    }

    private static Component name(String key) {
        return Component.translatable("text.autoconfig.simplelib.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("text.autoconfig.simplelib.option." + key + ".@Tooltip");
    }
}
