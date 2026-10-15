package com.simplebuilding.modules.simplemobmanners;

import net.neoforged.fml.common.Mod;

@Mod("simplemobmanners")
public final class MobMannersNeoForge {
    public MobMannersNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleMobManners.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleMobManners.load();
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> e.register(net.minecraft.core.registries.Registries.ITEM, h -> h.register(FrogBucket.KEY, FrogBucket.item())));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (SimpleMobManners.active() && e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                e.insertAfter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TADPOLE_BUCKET), new net.minecraft.world.item.ItemStack(FrogBucket.item()),
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> MobMannersCommands.register(e.getDispatcher()));
    }
}
