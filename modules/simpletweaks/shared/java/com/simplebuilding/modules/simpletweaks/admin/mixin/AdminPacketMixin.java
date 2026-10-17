package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.Admin;
import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tab list: player info sent to someone who may not see vanished players leaves them out. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class AdminPacketMixin {
    @Shadow public abstract void send(Packet<?> packet, ChannelFutureListener listener);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$tabList(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info) || Admin.state().vanished.isEmpty() || !Vanish.active()) return;
        if (!((Object) this instanceof ServerGamePacketListenerImpl game) || game.player == null || Vanish.sees(game.player)) return;
        var server = game.player.level().getServer();
        boolean hides = false;
        var keep = new ArrayList<ServerPlayer>();
        for (var e : info.entries()) {
            var p = server.getPlayerList().getPlayer(e.profileId());
            if (p != null && p != game.player && Vanish.vanished(p)) hides = true;
            else if (p != null) keep.add(p);
        }
        if (!hides) return;
        ci.cancel();
        if (!keep.isEmpty()) send(new ClientboundPlayerInfoUpdatePacket(info.actions(), keep), listener);
    }
}
