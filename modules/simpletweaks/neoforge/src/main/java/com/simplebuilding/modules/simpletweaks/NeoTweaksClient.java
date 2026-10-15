package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
/** NeoForge client: the sender of the deed editor's packets. Loaded only on the client. */
final class NeoTweaksClient {
    static void init() { DeedNet.toServer = net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer; }
    private NeoTweaksClient() {}
}
