package com.simplebuilding.clienttest.mixin;

import com.simplebuilding.clienttest.PacketBarrier;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remembers the id of the last ping the client has handled on its own thread - the far end of the
 * packet barrier in {@code SharedScriptClientGameTest.FabricHarness#packetsSettled}.
 *
 * <p>The client handles play packets strictly in arrival order on its own thread, so once a ping
 * the server sent after a given tick has been handled here, everything that tick sent (inventory
 * slots, block changes, teleports) has been handled too. At {@code TAIL}, because the first call on
 * the netty thread leaves through {@code ensureRunningOnSameThread}'s exception and only the
 * rescheduled call on the client thread reaches the end. Test source set only.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class PingHandledMixin {

    @Inject(method = "handlePing", at = @At("TAIL"))
    private void simplebuilding$rememberThePing(ClientboundPingPacket packet, CallbackInfo ci) {
        PacketBarrier.pingHandled(packet.getId());
    }
}
