package com.simplebuilding.modules.simplequalityoflife.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplequalityoflife",value=net.minecraftforge.api.distmarker.Dist.CLIENT,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class QolForgeKeys {
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent e){com.simplequalityoflife.client.SimplequalityoflifeClient.initClient();e.register(com.simplequalityoflife.client.SimplequalityoflifeClient.autoWalkKey);e.register(com.simplequalityoflife.client.SimplequalityoflifeClient.crawlKey);}
}
