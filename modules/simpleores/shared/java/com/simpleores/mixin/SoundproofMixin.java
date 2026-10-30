package com.simpleores.mixin;

import com.simpleores.sound.Soundproofing;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every packet to a player passes here; sound packets go through {@link Soundproofing#filter} (lead walls muffle them). */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class SoundproofMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void simpleores$soundproof(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (Soundproofing.sending() || !((Object) this instanceof ServerGamePacketListenerImpl game) || game.player == null) return;
        Packet<?>[] replacement = Soundproofing.filter(game.player, packet);
        if (replacement == null) return;
        ci.cancel();
        Soundproofing.sending(true);
        try {
            for (Packet<?> p : replacement) game.send(p, listener);
        } finally {
            Soundproofing.sending(false);
        }
    }
}
