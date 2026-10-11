package com.simplebuilding.modules.simplequalityoflife.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplequalityoflife",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class QolForgeClient {
 public static void receive(com.simplequalityoflife.network.ConfigSyncPayload p){com.simplequalityoflife.client.ClientNetworking.receive(p.json());}
 public static void receive(com.simplequalityoflife.network.LinkedOpenPayload p){com.simplequalityoflife.client.LinkedPanel.receive(p);}
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){com.simplequalityoflife.client.ClientNetworking.clear();}
}
