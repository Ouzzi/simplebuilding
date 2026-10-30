package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.beacon.StrongBeaconBlockEntity;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
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
                        new BlockEntityType<>(StrongConduitBlockEntity::new, BeaconsRegistry.conduitBlocks(), false));
            }
            if (key.equals(Registries.MENU)) BeaconsRegistry.registerMenus();
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebeacons.guide.BeaconsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) BeaconsNeoClient.init(bus);
    }
}
