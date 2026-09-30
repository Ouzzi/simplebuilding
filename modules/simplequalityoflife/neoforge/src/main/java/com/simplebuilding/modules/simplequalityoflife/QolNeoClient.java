package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.client.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="simplequalityoflife",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class QolNeoClient {
 @net.neoforged.bus.api.SubscribeEvent public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e){SimplequalityoflifeClient.initClient();e.register(SimplequalityoflifeClient.autoWalkKey);e.register(SimplequalityoflifeClient.crawlKey);}
 @net.neoforged.bus.api.SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){net.neoforged.fml.ModList.get().getModContainerById("simplequalityoflife").orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(c,p)->QolConfigScreen.create(p));}
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){SimplequalityoflifeClient.tick(net.minecraft.client.Minecraft.getInstance());}
}
