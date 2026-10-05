package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibItems;
import com.simplelib.registry.LibMenus;
import com.simplelib.registry.LibRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class SimpleLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleLib.init(FabricLoader.getInstance().getConfigDir());
        LibComponents.register();
        LibBlocks.register();
        LibItems.register();
        // Valid for every crucible/barrel block, including partner tiers registered later (load order free).
        LibBlockEntities.register(new net.minecraft.world.level.block.entity.BlockEntityType<>(CrucibleBlockEntity::new, java.util.Set.of(LibBlocks.crucibles())) {
            @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return LibBlockEntities.isCrucible(state); }
        });
        LibBlockEntities.registerBarrel(new net.minecraft.world.level.block.entity.BlockEntityType<>(com.simplelib.crucible.CrucibleBarrelBlockEntity::new, java.util.Set.of(LibBlocks.barrels())) {
            @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return LibBlockEntities.isBarrel(state); }
        });
        LibMenus.register();
        LibRegistry.tab();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server ->
                com.simplelib.village.VillageKitchen.inject(server.registryAccess()));
    }
}
