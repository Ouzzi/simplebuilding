package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.client.*;
import net.fabricmc.api.ClientModInitializer;
public final class QolFabricClient implements ClientModInitializer {
 public void onInitializeClient(){SimplequalityoflifeClient.initClient();
  net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(SimplequalityoflifeClient.autoWalkKey);
  net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(SimplequalityoflifeClient.crawlKey);
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(SimplequalityoflifeClient::tick);
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplequalityoflife.network.ConfigSyncPayload.TYPE,(p,c)->c.client().execute(()->ClientNetworking.receive(p.json())));
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplequalityoflife.network.CrawlStatePayload.TYPE,(p,c)->c.client().execute(()->ClientNetworking.receive(p)));
  // Runs on the client thread in packet order: the slots must exist before the menu's first content packet.
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplequalityoflife.network.LinkedOpenPayload.TYPE,(p,c)->LinkedPanel.receive(p));
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h,c)->ClientNetworking.clear());
 }
}
