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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleBeacons.MOD_ID)
public final class BeaconsNeoForge {
    public BeaconsNeoForge(IEventBus bus, Dist dist) {
        SimpleBeacons.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) BeaconsRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) BeaconsRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) BeaconsRegistry.registerTab();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                BeaconsRegistry.registerBlockEntities(new BlockEntityType<>(StrongBeaconBlockEntity::new, BeaconsRegistry.beaconBlocks(), false),
                        new BlockEntityType<>(StrongConduitBlockEntity::new, Set.of(BeaconsRegistry.STRONG_CONDUIT), false));
            }
            if (key.equals(Registries.MENU)) BeaconsRegistry.registerMenus();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleBeacons.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                BeaconsRegistry.tabStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS) && vanillaTabs && SimpleBeacons.on()) {
                BeaconsRegistry.templateStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleBeacons.on()) {
                event.accept(com.simplebeacons.guide.BeaconsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebeacons.guide.BeaconsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) BeaconsNeoClient.init(bus);
    }
}
