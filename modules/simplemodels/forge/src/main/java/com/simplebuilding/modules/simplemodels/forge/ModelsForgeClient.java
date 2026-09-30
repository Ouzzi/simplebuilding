package com.simplebuilding.modules.simplemodels.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplemodels",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ModelsForgeClient {
 public static void receive(com.simplebuilding.modules.simplemodels.CataloguePayload payload){com.simplebuilding.modules.simplemodels.client.ModelBrowser.accept(payload);}
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent
 public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){com.simplebuilding.modules.simplemodels.client.ModelBrowser.clear();}
}
