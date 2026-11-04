package com.simplelib.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Config screen of SimpleLib (Fabric, Mod Menu): the legendary moments (SP22-2), free look (IG31-29) and the hang watchdog (BS27-1). Cloth Config stays optional for SimpleLib - the
 * entry only offers this screen while Cloth is loaded (owner 22.10.: only 26.3 Fabric for now). The warm
 * food values stay in the file (server bounds, see the wiki).
 */
public final class LibConfigScreen {
    private LibConfigScreen() {}

    public static Screen create(Screen parent) {
        boolean[] effects = {LibConfig.legendaryEffects};
        boolean[] allChallenges = {LibConfig.legendaryAllChallenges};
        boolean[] muteToast = {LibConfig.legendaryMuteToast};
        boolean[] hang = {LibConfig.hangWatchdog};
        int[] hangSeconds = {LibConfig.hangWatchdogSeconds};
        List<List<String>> lists = new ArrayList<>(List.of(strings(LibConfig.legendaryAdvancements), strings(LibConfig.legendaryEnchantments)));
        com.simplelib.api.FreeLookRules.Release[] lookClient = {com.simplelib.api.FreeLookRules.client()};
        com.simplelib.api.FreeLookRules.Release[] lookServer = {com.simplelib.api.FreeLookRules.Release.parse(LibConfig.freeLookRelease)};
        boolean[] lookEnforce = {LibConfig.freeLookEnforce};
        boolean[] lookCrosshair = {com.simplelib.api.FreeLookRules.crosshair()};
        boolean[] lookZoom = {com.simplelib.api.FreeLookRules.zoom()};
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplelib.title"));
        var e = b.entryBuilder();
        var legendary = b.getOrCreateCategory(Component.translatable("simplelib.tab.legendary"));
        legendary.addEntry(e.startBooleanToggle(name("legendaryEffects"), effects[0]).setDefaultValue(true)
                .setTooltip(tip("legendaryEffects")).setSaveConsumer(v -> effects[0] = v).build());
        legendary.addEntry(e.startBooleanToggle(name("legendaryAllChallenges"), allChallenges[0]).setDefaultValue(false)
                .setTooltip(tip("legendaryAllChallenges")).setSaveConsumer(v -> allChallenges[0] = v).build());
        legendary.addEntry(e.startBooleanToggle(name("legendaryMuteToast"), muteToast[0]).setDefaultValue(true)
                .setTooltip(tip("legendaryMuteToast")).setSaveConsumer(v -> muteToast[0] = v).build());
        legendary.addEntry(e.startStrList(name("legendaryAdvancements"), lists.get(0)).setDefaultValue(List.of())
                .setTooltip(tip("legendaryAdvancements")).setSaveConsumer(v -> lists.set(0, v)).build());
        legendary.addEntry(e.startStrList(name("legendaryEnchantments"), lists.get(1)).setDefaultValue(List.of())
                .setTooltip(tip("legendaryEnchantments")).setSaveConsumer(v -> lists.set(1, v)).build());
        // IG31-29: free look - the client's own release mode, the server's mode and whether the server enforces it.
        var look = b.getOrCreateCategory(Component.translatable("simplelib.tab.freelook"));
        look.addEntry(e.startEnumSelector(name("freeLookRelease"), com.simplelib.api.FreeLookRules.Release.class, lookClient[0])
                .setDefaultValue(com.simplelib.api.FreeLookRules.Release.CAMERA).setEnumNameProvider(LibConfigScreen::releaseName)
                .setTooltip(tip("freeLookRelease")).setSaveConsumer(v -> lookClient[0] = v).build());
        look.addEntry(e.startBooleanToggle(name("freeLookCrosshair"), lookCrosshair[0]).setDefaultValue(true)
                .setTooltip(tip("freeLookCrosshair")).setSaveConsumer(v -> lookCrosshair[0] = v).build());
        look.addEntry(e.startBooleanToggle(name("freeLookZoom"), lookZoom[0]).setDefaultValue(true)
                .setTooltip(tip("freeLookZoom")).setSaveConsumer(v -> lookZoom[0] = v).build());
        look.addEntry(e.startEnumSelector(name("freeLookServerRelease"), com.simplelib.api.FreeLookRules.Release.class, lookServer[0])
                .setDefaultValue(com.simplelib.api.FreeLookRules.Release.CAMERA).setEnumNameProvider(LibConfigScreen::releaseName)
                .setTooltip(tip("freeLookServerRelease")).setSaveConsumer(v -> lookServer[0] = v).build());
        look.addEntry(e.startBooleanToggle(name("freeLookEnforce"), lookEnforce[0]).setDefaultValue(false)
                .setTooltip(tip("freeLookEnforce")).setSaveConsumer(v -> lookEnforce[0] = v).build());
        var diagnosis = b.getOrCreateCategory(Component.translatable("simplelib.tab.diagnosis"));
        diagnosis.addEntry(e.startBooleanToggle(name("hangWatchdog"), hang[0]).setDefaultValue(true)
                .setTooltip(tip("hangWatchdog")).setSaveConsumer(v -> hang[0] = v).build());
        diagnosis.addEntry(e.startIntField(name("hangWatchdogSeconds"), hangSeconds[0]).setDefaultValue(LibConfig.HANG_SECONDS_DEFAULT)
                .setMin(LibConfig.HANG_SECONDS_MIN).setMax(LibConfig.HANG_SECONDS_MAX)
                .setTooltip(tip("hangWatchdogSeconds")).setSaveConsumer(v -> hangSeconds[0] = v).build());
        b.setSavingRunnable(() -> {
            LibConfig.hangWatchdog = hang[0];
            LibConfig.hangWatchdogSeconds = Math.max(LibConfig.HANG_SECONDS_MIN, Math.min(LibConfig.HANG_SECONDS_MAX, hangSeconds[0]));
            LibConfig.legendaryEffects = effects[0];
            LibConfig.legendaryAllChallenges = allChallenges[0];
            LibConfig.legendaryMuteToast = muteToast[0];
            LibConfig.legendaryAdvancements = LibConfig.ids(lists.get(0));
            LibConfig.legendaryEnchantments = LibConfig.ids(lists.get(1));
            LibConfig.freeLookRelease = lookServer[0].id();
            LibConfig.freeLookEnforce = lookEnforce[0];
            LibConfig.save();
            com.simplelib.api.FreeLookRules.setClient(lookClient[0]);
            com.simplelib.api.FreeLookRules.setCrosshair(lookCrosshair[0]);
            com.simplelib.api.FreeLookRules.setZoom(lookZoom[0]);
            com.simplelib.api.client.ui.UiBrightness.save();
        });
        return b.build();
    }

    private static Component releaseName(Enum<?> release) {
        return Component.translatable("simplelib.freelook.release." + ((com.simplelib.api.FreeLookRules.Release) release).id());
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
