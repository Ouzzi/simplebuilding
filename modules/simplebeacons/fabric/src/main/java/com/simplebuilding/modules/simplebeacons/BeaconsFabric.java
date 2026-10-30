package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.beacon.StrongBeaconBlockEntity;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class BeaconsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleBeacons.init(FabricLoader.getInstance().getConfigDir());
        BeaconsRegistry.registerBlocks();
        BeaconsRegistry.registerItems();
        BeaconsRegistry.registerBlockEntities(new BlockEntityType<>(StrongBeaconBlockEntity::new, BeaconsRegistry.beaconBlocks()),
                new BlockEntityType<>(StrongConduitBlockEntity::new, BeaconsRegistry.conduitBlocks()));
        BeaconsRegistry.registerMenus();
        BeaconsRegistry.registerTab();
        // IG31-3: the items stand in the module's own tab only (no Vanilla tabs, see CreativeTabPolicy)
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebeacons.guide.BeaconsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
