package com.simplebuilding.modules.simplemobmanners;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simplemobmanners")
public final class MobMannersForge {
    public MobMannersForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplemobmanners.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleMobManners.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleMobManners.load();
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> MobMannersCommands.register(e.getDispatcher()));
    }
}
