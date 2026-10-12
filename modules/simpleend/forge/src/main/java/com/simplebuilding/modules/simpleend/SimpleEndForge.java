package com.simplebuilding.modules.simpleend;

import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleEnd.ID)
public final class SimpleEndForge {
    public SimpleEndForge(FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();
        com.simplebuilding.modules.simpleend.forge.ModuleForgeTests.register(bus);
        SimpleEnd.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(bus).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) EndRegistry.blocks();
            if (key.equals(Registries.ITEM)) EndRegistry.items();
            if (key.equals(Registries.ENTITY_TYPE)) EndRegistry.entities();
            if (key.equals(Registries.FEATURE_TYPE)) EndRegistry.featureTypes();
            if (key.equals(Registries.SOUND_EVENT)) EndRegistry.sounds();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) EndRegistry.tab();
        });
        FMLCommonSetupEvent.getBus(bus).addListener(event -> event.enqueueWork(EndRegistry::common));
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebuilding.modules.simpleend.guide.EndGuide.installQuests(FMLPaths.CONFIGDIR.get());
        net.minecraftforge.event.server.ServerAboutToStartEvent.BUS.addListener(event -> { SimpleEnd.applySubSwitches(); com.simplebuilding.modules.simpleend.world.EndBiomes.serverStarting(event.getServer()); });
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) SimpleEndForgeClient.init();
    }
}
