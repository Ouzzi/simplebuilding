package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.beacon.StrongBeaconBlockEntity;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleBeacons.MOD_ID)
public final class BeaconsForge {
    public BeaconsForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplebeacons.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleBeacons.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) BeaconsRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) BeaconsRegistry.registerItems();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                BeaconsRegistry.registerBlockEntities(new BlockEntityType<>(StrongBeaconBlockEntity::new, Set.of(BeaconsRegistry.STRONG_BEACON)),
                        new BlockEntityType<>(StrongConduitBlockEntity::new, Set.of(BeaconsRegistry.STRONG_CONDUIT)));
            }
            if (key.equals(Registries.MENU)) BeaconsRegistry.registerMenus();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleBeacons.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                BeaconsRegistry.tabStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS) && vanillaTabs && SimpleBeacons.on()) {
                event.getEntries().put(new ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleBeacons.on()) {
                event.getEntries().put(com.simplebeacons.guide.BeaconsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebeacons.guide.BeaconsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) BeaconsForgeClient.init(context);
    }
}
