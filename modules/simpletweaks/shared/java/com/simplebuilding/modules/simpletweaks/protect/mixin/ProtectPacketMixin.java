package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.HideHealth;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Hidden health: entity data about other players (also inside bundles) is filtered per receiver. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ProtectPacketMixin {
    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> simpletweaks$health(Packet<?> packet) {
        if (HideHealth.healthId == null || !((Object) this instanceof ServerGamePacketListenerImpl game) || game.player == null) return packet;
        if (packet instanceof ClientboundSetEntityDataPacket data) return HideHealth.filter(game.player, data);
        if (packet instanceof ClientboundBundlePacket bundle) {
            boolean changed = false;
            var out = new ArrayList<Packet<? super ClientGamePacketListener>>();
            for (var p : bundle.subPackets()) {
                if (p instanceof ClientboundSetEntityDataPacket data) {
                    var f = HideHealth.filter(game.player, data);
                    changed |= f != data;
                    out.add(f);
                } else out.add(p);
            }
            return changed ? new ClientboundBundlePacket(out) : packet;
        }
        return packet;
    }
}
