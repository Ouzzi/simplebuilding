package com.simplebuilding.modules.simpleshops;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.SimpleShops;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlockEntity;
import com.simpleshops.logic.ShopGuard;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleShops.MOD_ID)
public final class ShopsNeoForge {
    public ShopsNeoForge(IEventBus bus, Dist dist) {
        SimpleShops.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) ShopsRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) ShopsRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) ShopsRegistry.registerTab();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                ShopsRegistry.registerBlockEntities(new BlockEntityType<>(SalesBlockEntity::new, ShopsRegistry.salesBlocks(), false),
                        new BlockEntityType<>(DisplayCaseBlockEntity::new, ShopsRegistry.caseBlocks(), false),
                        new BlockEntityType<>(SalesStorageBlockEntity::new, ShopsRegistry.storageBlocks(), false));
            }
            if (key.equals(Registries.MENU)) ShopsRegistry.registerMenus();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) ->
                com.simpleshops.logic.ServerShopCommands.register(e.getDispatcher(), e.getBuildContext()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ShopGuard.register(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ShopGuard.unregister(e.getServer()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleShops.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                ShopsRegistry.tabStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleShops.on()) {
                event.accept(com.simpleshops.guide.ShopsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleshops.guide.ShopsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) ShopsNeoClient.init(bus);
    }
}
