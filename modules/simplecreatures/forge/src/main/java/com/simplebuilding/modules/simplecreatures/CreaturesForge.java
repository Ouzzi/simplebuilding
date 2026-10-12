package com.simplebuilding.modules.simplecreatures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simplecreatures")
public final class CreaturesForge {
    public CreaturesForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplecreatures.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleCreatures.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleCreatures.load();
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> CreaturesCommands.register(e.getDispatcher()));
    }
}
