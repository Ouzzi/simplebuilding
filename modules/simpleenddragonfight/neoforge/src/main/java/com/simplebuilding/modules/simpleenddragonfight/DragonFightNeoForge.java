package com.simplebuilding.modules.simpleenddragonfight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("simpleenddragonfight")
public final class DragonFightNeoForge {
    public DragonFightNeoForge(IEventBus bus, Dist dist) {
        DragonFightConfig.load(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> h.register(DragonFightRegistry.DRAGONMITE_KEY, DragonFightRegistry.DRAGONMITE));
            event.register(Registries.ITEM, h -> {
                h.register(DragonFightRegistry.CHITIN_KEY, DragonFightRegistry.DRAGON_CHITIN);
                h.register(DragonFightRegistry.EGG_KEY, DragonFightRegistry.DRAGONMITE_EGG);
            });
        });
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(DragonFightRegistry.DRAGONMITE, Dragonmite.createAttributes().build()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!DragonFightRegistry.on()) return;
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) insertAfter(event, new ItemStack(Items.DRAGON_BREATH), new ItemStack(DragonFightRegistry.DRAGON_CHITIN));
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS) && DragonFightConfig.exposeSpawnEgg)
                insertAfter(event, new ItemStack(Items.ENDERMITE_SPAWN_EGG), new ItemStack(DragonFightRegistry.DRAGONMITE_EGG));
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> DragonFightServer.tick(event.getServer()));
        if (dist == Dist.CLIENT) DragonFightNeoClient.init(bus);
    }

    private static void insertAfter(BuildCreativeModeTabContentsEvent event, ItemStack anchor, ItemStack stack) {
        if (event.getParentEntries().contains(anchor)) event.insertAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else event.accept(stack);
    }
}
