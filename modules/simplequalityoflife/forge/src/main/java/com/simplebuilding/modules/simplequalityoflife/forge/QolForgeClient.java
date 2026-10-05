package com.simplebuilding.modules.simplequalityoflife.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplequalityoflife",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class QolForgeClient {
 public static void receive(com.simplequalityoflife.network.ConfigSyncPayload p){com.simplequalityoflife.client.ClientNetworking.receive(p.json());}
 public static void receive(com.simplequalityoflife.network.CrawlStatePayload p){com.simplequalityoflife.client.ClientNetworking.receive(p);}
 public static void receive(com.simplequalityoflife.network.LinkedOpenPayload p){com.simplequalityoflife.client.LinkedPanel.receive(p);}
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent.Post e){com.simplequalityoflife.client.SimplequalityoflifeClient.tick(net.minecraft.client.Minecraft.getInstance());}
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){com.simplequalityoflife.client.ClientNetworking.clear();}
}
