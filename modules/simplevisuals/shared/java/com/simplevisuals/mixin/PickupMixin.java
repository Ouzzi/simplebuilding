package com.simplevisuals.mixin;
import net.minecraft.client.multiplayer.ClientPacketListener;import net.minecraft.network.protocol.game.*;import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class PickupMixin {
 @Inject(method="handleRemoveEntities",at=@At("HEAD")) private void visuals$cache(ClientboundRemoveEntitiesPacket p,CallbackInfo ci){var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.level==null)return;for(int id:p.entityIds())com.simplevisuals.client.VisualsHud.cache(id,mc.level.getEntity(id));}
 @Inject(method="handleTakeItemEntity",at=@At("HEAD")) private void visuals$pickup(ClientboundTakeItemEntityPacket p,CallbackInfo ci){var mc=Minecraft.getInstance();if(mc.isSameThread())com.simplevisuals.client.VisualsHud.pickup(mc,p.getItemId(),p.getPlayerId(),p.getAmount());}
}
