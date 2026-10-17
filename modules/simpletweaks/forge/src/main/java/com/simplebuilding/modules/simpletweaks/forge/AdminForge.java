package com.simplebuilding.modules.simpletweaks.forge;

import com.simplebuilding.modules.simpletweaks.admin.*;
import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;

/** Forge wiring of the admin features: menus, lifecycle, commands, client screens. */
final class AdminForge {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.MENU)) Invsee.registerMenus();
        });
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> Admin.start(e.getServer()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e -> Admin.stop(e.getServer()));
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> AdminCommands.register(e.getDispatcher()));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) AdminForgeClient.init(context);
    }
    private AdminForge() {}
}
