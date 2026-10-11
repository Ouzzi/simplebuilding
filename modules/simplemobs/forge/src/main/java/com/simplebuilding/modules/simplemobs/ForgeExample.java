package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

@Mod("simplemobs")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplemobs.forge.ModuleForgeTests.register(context.getModBusGroup());
        var bus = context.getModBusGroup();
        RegisterEvent.getBus(bus).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> r.register(MobsRegistry.DECEIVER_KEY, MobsRegistry.DECEIVER));
            event.register(Registries.ITEM, r -> {
                r.register(MobsRegistry.CLOTH_KEY, MobsRegistry.CLOTH);
                r.register(MobsRegistry.EGG_KEY, MobsRegistry.EGG);
            });
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes().build()));
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) e.accept(MobsRegistry.EGG);
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) e.accept(MobsRegistry.CLOTH);
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> MobsServer.tick(e.server()));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) ForgeClientExample.init();
    }
}
