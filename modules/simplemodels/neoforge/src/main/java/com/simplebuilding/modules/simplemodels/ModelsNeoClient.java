package com.simplebuilding.modules.simplemodels;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="simplemodels", value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class ModelsNeoClient {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simplemodels").orElseThrow()
                .registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (container, parent) -> new com.simplebuilding.modules.simplemodels.client.ModelBrowser(parent));
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        com.simplebuilding.modules.simplemodels.client.ModelBrowser.clear();
    }
}
