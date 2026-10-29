package com.simplebuilding.modules.simplemobmanners;

import net.fabricmc.api.ModInitializer;

public final class MobMannersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMobManners.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleMobManners.load();
        // CMD24: /simplemobmanners config for every option; save() normalises, writes and syncs the clients (Fabric entry: SimpleLib is bundled here).
        com.simplelib.api.config.ConfigCommands.server(SimpleMobManners.MOD_ID).gson(MobMannersConfig.class, () -> SimpleMobManners.CONFIG, c -> { c.normalize(); SimpleMobManners.CONFIG = c; },
                () -> { MobMannersConfig d = new MobMannersConfig(); d.normalize(); return d; }).save(SimpleMobManners::save).register();
        FrogBucket.register();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            if (SimpleMobManners.active()) {
                entries.insertAfter(net.minecraft.world.item.Items.TADPOLE_BUCKET, java.util.List.of(new net.minecraft.world.item.ItemStack(FrogBucket.item())),
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> MobMannersCommands.register(d));
    }
}
