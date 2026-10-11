package com.simplebuilding.modules.simpleloot;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simpleloot")
public final class LootForge {
    public LootForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simpleloot.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleLoot.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleLoot.load();
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> LootCommands.register(e.getDispatcher()));
    }
}
