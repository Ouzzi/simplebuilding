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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleShops.MOD_ID)
public final class ShopsForge {
    public ShopsForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simpleshops.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleShops.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) ShopsRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) ShopsRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) ShopsRegistry.registerTab();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                ShopsRegistry.registerBlockEntities(new BlockEntityType<>(SalesBlockEntity::new, ShopsRegistry.salesBlocks()),
                        new BlockEntityType<>(DisplayCaseBlockEntity::new, ShopsRegistry.caseBlocks()),
                        new BlockEntityType<>(SalesStorageBlockEntity::new, ShopsRegistry.storageBlocks()));
            }
            if (key.equals(Registries.MENU)) ShopsRegistry.registerMenus();
        });
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e ->
                com.simpleshops.logic.ServerShopCommands.register(e.getDispatcher(), e.getBuildContext()));
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> ShopGuard.register(e.getServer()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e -> ShopGuard.unregister(e.getServer()));
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleShops.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                ShopsRegistry.tabStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleShops.on()) {
                event.getEntries().put(com.simpleshops.guide.ShopsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simpleshops.guide.ShopsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) ShopsForgeClient.init(context);
    }
}
