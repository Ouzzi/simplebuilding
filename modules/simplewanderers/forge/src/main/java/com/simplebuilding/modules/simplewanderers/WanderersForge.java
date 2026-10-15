package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.WandererRegistry;
import com.simplewanderers.logic.WandererSpawner;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration as Fabric/NeoForge. */
@Mod(SimpleWanderers.MOD_ID)
public final class WanderersForge {
    public WanderersForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplewanderers.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleWanderers.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> WandererRegistry.entityTypes(r::register));
            event.register(Registries.ITEM, r -> WandererRegistry.items(r::register));
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(WandererRegistry.WANDERER, WandererRegistry.wandererAttributes().build());
            event.put(WandererRegistry.HORSE, WandererRegistry.horseAttributes().build());
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> WandererSpawner.tick(e.server()));
        BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (!SimpleWanderers.on()) return;
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                WandererRegistry.eggStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.getEntries().put(com.simplewanderers.guide.WanderersGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplewanderers.guide.WanderersGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) WanderersForgeClient.init();
    }
}
