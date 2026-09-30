package com.simplebuilding.modules.simpleriding;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
@EventBusSubscriber(modid="simpleriding",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class RidingNeoClient {
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){net.neoforged.fml.ModList.get().getModContainerById("simpleriding").orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(container,parent)->com.simpleriding.client.RidingConfigScreen.create(parent));}
}
