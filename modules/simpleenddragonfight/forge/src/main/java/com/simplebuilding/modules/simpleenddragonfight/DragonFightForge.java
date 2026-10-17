package com.simplebuilding.modules.simpleenddragonfight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

@Mod("simpleenddragonfight")
public final class DragonFightForge {
    public DragonFightForge(FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();
        com.simplebuilding.modules.simpleenddragonfight.forge.ModuleForgeTests.register(bus);
        DragonFightConfig.load(FMLPaths.CONFIGDIR.get());
        net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simpleenddragonfight.guide.DragonFightGuide.register();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) e.getEntries().put(com.simplebuilding.modules.simpleenddragonfight.guide.DragonFightGuide.stack(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebuilding.modules.simpleenddragonfight.guide.DragonFightGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(bus).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> r.register(DragonFightRegistry.DRAGONMITE_KEY, DragonFightRegistry.DRAGONMITE));
            event.register(Registries.ITEM, r -> {
                r.register(DragonFightRegistry.CHITIN_KEY, DragonFightRegistry.DRAGON_CHITIN);
                r.register(DragonFightRegistry.EGG_KEY, DragonFightRegistry.DRAGONMITE_EGG);
            });
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(DragonFightRegistry.DRAGONMITE, Dragonmite.createAttributes().build()));
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (!DragonFightRegistry.on()) return;
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) putAfter(e, new ItemStack(Items.DRAGON_BREATH), new ItemStack(DragonFightRegistry.DRAGON_CHITIN));
            if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS) && DragonFightConfig.exposeSpawnEgg)
                putAfter(e, new ItemStack(Items.ENDERMITE_SPAWN_EGG), new ItemStack(DragonFightRegistry.DRAGONMITE_EGG));
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> DragonFightServer.tick(e.server()));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) DragonFightForgeClient.init();
    }

    private static void putAfter(BuildCreativeModeTabContentsEvent e, ItemStack anchor, ItemStack stack) {
        if (e.getEntries().contains(anchor)) e.getEntries().putAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else e.accept(stack);
    }
}
