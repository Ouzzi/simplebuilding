package com.simplebuilding.modules.simplestats;

import net.fabricmc.api.ModInitializer;

public final class StatsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleStats.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleStats.load();
        // simplestats-4 (PLAN40): Mod Menu screen and /simplestats config from SimpleLib (RELSB31 f2=A); saved to the same file.
        com.simplelib.api.config.ConfigCommands.server(SimpleStats.MOD_ID).gson(StatsConfig.class, SimpleStats::config, SimpleStats::restore, StatsConfig::new)
                .file(SimpleStats.configDir, SimpleStats.MOD_ID + ".json").range("maxBytes", 2048, 262144).register();
        StatsComponents.register();
        com.simplebuilding.modules.simplestats.guide.StatsGuide.register();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            var book = java.util.List.of(com.simplebuilding.modules.simplestats.guide.StatsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.WRITABLE_BOOK))) out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK, book, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuilding.modules.simplestats.guide.StatsGuide.installQuests(SimpleStats.configDir);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> StatsCommands.register(d));
    }
}
