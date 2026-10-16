package com.simplebuilding.modules.simplecrucibles;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.crucible.CrucibleBarrelBlockEntity;
import com.simplecrucibles.crucible.CrucibleBlockEntity;
import com.simplecrucibles.registry.CrucibleBlockEntities;
import com.simplecrucibles.registry.CrucibleBlocks;
import com.simplecrucibles.registry.CrucibleItems;
import com.simplecrucibles.registry.CrucibleMenus;
import com.simplecrucibles.registry.CrucibleRegistry;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class CruciblesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleCrucibles.init(FabricLoader.getInstance().getConfigDir());
        CrucibleBlocks.register();
        CrucibleItems.register();
        // Valid for every crucible/barrel block, including partner tiers registered later (load order free).
        CrucibleBlockEntities.register(new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(CrucibleBlocks.crucibles())) {
            @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return CrucibleBlockEntities.isCrucible(state); }
        });
        CrucibleBlockEntities.registerBarrel(new BlockEntityType<>(CrucibleBarrelBlockEntity::new, Set.of(CrucibleBlocks.barrels())) {
            @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return CrucibleBlockEntities.isBarrel(state); }
        });
        CrucibleMenus.register();
        CrucibleRegistry.tab();
        // Crucibles and barrels also go into Vanilla's functional tab (per-mod switch) and SimpleBuilding's own.
        for (var tab : java.util.List.of(CreativeModeTabs.FUNCTIONAL_BLOCKS,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, CrucibleRegistry.SB_FUNCTIONAL_TAB))) {
            CreativeModeTabEvents.modifyOutputEvent(tab).register(entries -> {
                if (!CrucibleRegistry.wantsStacks(tab, com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleCrucibles.MOD_ID))) return;
                CrucibleRegistry.tabStacks().forEach(s -> entries.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            });
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleCrucibles.on()) return;
            var book = java.util.List.of(com.simplecrucibles.guide.CruciblesGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server ->
                com.simplecrucibles.village.VillageKitchen.inject(server.registryAccess()));
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplecrucibles.guide.CruciblesGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
