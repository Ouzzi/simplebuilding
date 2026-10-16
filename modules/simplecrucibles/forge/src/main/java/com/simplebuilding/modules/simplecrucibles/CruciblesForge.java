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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleCrucibles.MOD_ID)
public final class CruciblesForge {
    public CruciblesForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplecrucibles.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleCrucibles.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) CrucibleBlocks.register();
            if (key.equals(Registries.ITEM)) CrucibleItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                CrucibleBlockEntities.register(new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(CrucibleBlocks.crucibles())) {
                    @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return CrucibleBlockEntities.isCrucible(state); }
                });
                CrucibleBlockEntities.registerBarrel(new BlockEntityType<>(CrucibleBarrelBlockEntity::new, Set.of(CrucibleBlocks.barrels())) {
                    @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return CrucibleBlockEntities.isBarrel(state); }
                });
            }
            if (key.equals(Registries.MENU)) CrucibleMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) CrucibleRegistry.tab();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleCrucibles.on()) {
                event.getEntries().put(com.simplecrucibles.guide.CruciblesGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            if (!CrucibleRegistry.wantsStacks(event.getTabKey(),
                    com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleCrucibles.MOD_ID))) return;
            CrucibleRegistry.tabStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        net.minecraftforge.event.server.ServerAboutToStartEvent.BUS.addListener(event ->
                com.simplecrucibles.village.VillageKitchen.inject(event.getServer().registryAccess()));
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplecrucibles.guide.CruciblesGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) CruciblesForgeClient.init(context);
    }
}
