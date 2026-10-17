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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleCrucibles.MOD_ID)
public final class CruciblesNeoForge {
    public CruciblesNeoForge(IEventBus bus, Dist dist) {
        SimpleCrucibles.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        // Split stage 5b: soul lava needs a NeoForge fluid type (swaps the fluid factory before registration).
        CruciblesNeoSoulLava.install(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) com.simplecrucibles.registry.CrucibleComponents.register();
            if (key.equals(Registries.MOB_EFFECT)) com.simplecrucibles.effect.CrucibleEffects.register();
            if (key.equals(Registries.FLUID)) com.simplecrucibles.fluid.CrucibleFluids.registerFluids();
            if (key.equals(Registries.BLOCK)) {
                com.simplecrucibles.fluid.CrucibleFluids.registerFluids(); // no-op when the FLUID event came first
                CrucibleBlocks.register();
                com.simplecrucibles.fluid.CrucibleFluids.registerBlocks();
            }
            if (key.equals(Registries.ITEM)) {
                CrucibleItems.register();
                com.simplecrucibles.fluid.CrucibleFluids.registerItems();
                com.simplecrucibles.fluid.BucketDispensing.register();
            }
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                CrucibleBlockEntities.register(new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(CrucibleBlocks.crucibles()), false) {
                    @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return CrucibleBlockEntities.isCrucible(state); }
                });
                CrucibleBlockEntities.registerBarrel(new BlockEntityType<>(CrucibleBarrelBlockEntity::new, Set.of(CrucibleBlocks.barrels()), false) {
                    @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return CrucibleBlockEntities.isBarrel(state); }
                });
            }
            if (key.equals(Registries.MENU)) CrucibleMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) CrucibleRegistry.tab();
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    && com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleCrucibles.MOD_ID)) {
                // The buckets after Vanilla's powder snow bucket.
                net.minecraft.world.item.ItemStack previous = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET);
                for (net.minecraft.world.item.ItemStack stack : CrucibleRegistry.bucketStacks()) {
                    if (event.getParentEntries().contains(stack)) continue;
                    event.insertAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    previous = stack;
                }
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleCrucibles.on()) {
                event.accept(com.simplecrucibles.guide.CruciblesGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            if (!CrucibleRegistry.wantsStacks(event.getTabKey(),
                    com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleCrucibles.MOD_ID))) return;
            CrucibleRegistry.tabStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) ->
                com.simplecrucibles.village.VillageKitchen.inject(event.getServer().registryAccess()));
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplecrucibles.guide.CruciblesGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) CruciblesNeoClient.init(bus);
    }
}
