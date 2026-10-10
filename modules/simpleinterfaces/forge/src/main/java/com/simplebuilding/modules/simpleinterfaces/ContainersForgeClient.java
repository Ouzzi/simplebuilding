package com.simplebuilding.modules.simpleinterfaces;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simpleinterfaces",
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ContainersForgeClient {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        com.simplebuilding.modules.simpleinterfaces.client.LegacyMigration.run(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get());
        com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient.development =
                !net.minecraftforge.fml.loading.FMLEnvironment.production;
        event.register(com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient.KEY);
    }

    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent.Post event) {
        com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient.tick(net.minecraft.client.Minecraft.getInstance());
    }
}
