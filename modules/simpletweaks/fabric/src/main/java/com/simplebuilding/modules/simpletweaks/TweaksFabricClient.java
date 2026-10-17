package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.deed.client.DeedClient;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
/** Fabric client entry: the deed screen receivers and the sender of the editor's packets. */
public final class TweaksFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        DeedNet.toServer=payload->{if(ClientPlayNetworking.canSend(payload.type()))ClientPlayNetworking.send(payload);};
        ClientPlayNetworking.registerGlobalReceiver(DeedOpenPayload.TYPE,(payload,context)->context.client().execute(()->DeedClient.open(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DeedResultPayload.TYPE,(payload,context)->context.client().execute(()->DeedClient.result(payload)));
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload.TYPE,(payload,context)->context.client().execute(()->com.simplebuilding.modules.simpletweaks.table.client.TableClient.open(payload)));
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload.TYPE,(payload,context)->context.client().execute(()->com.simplebuilding.modules.simpletweaks.table.client.TableClient.area(payload)));
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.table.net.TableListPayload.TYPE,(payload,context)->context.client().execute(()->com.simplebuilding.modules.simpletweaks.table.client.TableClient.list(payload)));
    }
}
