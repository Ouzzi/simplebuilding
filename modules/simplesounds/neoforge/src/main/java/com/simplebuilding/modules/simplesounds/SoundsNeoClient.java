package com.simplebuilding.modules.simplesounds;
@net.neoforged.fml.common.EventBusSubscriber(modid="simplesounds",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class SoundsNeoClient {
 @net.neoforged.bus.api.SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){net.neoforged.fml.ModList.get().getModContainerById("simplesounds").orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(container,parent)->SoundScreen.create(parent));}
}
