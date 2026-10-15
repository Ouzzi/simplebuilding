package com.simplebuilding.modules.simplemobmanners;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simplemobmanners")
public final class MobMannersForge {
    public MobMannersForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplemobmanners.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleMobManners.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleMobManners.load();
        net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> e.register(net.minecraft.core.registries.Registries.ITEM, r -> r.register(FrogBucket.KEY, FrogBucket.item())));
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (SimpleMobManners.active() && e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                e.getEntries().putAfter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TADPOLE_BUCKET), new net.minecraft.world.item.ItemStack(FrogBucket.item()),
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> MobMannersCommands.register(e.getDispatcher()));
    }
}
