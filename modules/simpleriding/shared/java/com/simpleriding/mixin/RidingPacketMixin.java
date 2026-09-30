package com.simpleriding.mixin;

import com.simpleriding.RidingSecurity;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RidingPacketMixin {
    @Shadow public ServerPlayer player;
    @Unique private final RidingSecurity simpleriding$security = new RidingSecurity();
    // AFTER the thread handoff: never read or mutate world state on the network thread.
    @Inject(method="handleMoveVehicle", at=@At(value="INVOKE", target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift=At.Shift.AFTER), cancellable=true)
    private void simpleriding$move(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        var mount = player.getRootVehicle();
        if (RidingSecurity.supported(mount) && !simpleriding$security.acceptMove(player, packet.movingTo())) {
            if (simpleriding$security.shouldCorrect(player)) player.connection.send(ClientboundMoveVehiclePacket.fromEntity(mount));
            ci.cancel();
        }
    }
    @Inject(method="handlePlayerCommand", at=@At(value="INVOKE", target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift=At.Shift.AFTER), cancellable=true)
    private void simpleriding$jump(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        var mount = player.getControlledVehicle();
        if (packet.getAction() == ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP
            && mount != null && RidingSecurity.supported(mount)) {
            if (!simpleriding$security.acceptJump(player, packet.getId(), packet.getData())) { ci.cancel(); return; }
            if (mount instanceof net.minecraft.world.entity.animal.nautilus.AbstractNautilus nautilus) {
                float scale=nautilus.getPlayerJumpPendingScale(packet.getData());
                simpleriding$security.recordDash(player,nautilus,scale);
                nautilus.handleStartJump(packet.getData());
                ((NautilusDashAccess)nautilus).simpleriding$executeDash(scale,player);
                player.connection.send(new ClientboundSetEntityMotionPacket(nautilus));
                ci.cancel();
            }
        }
    }
}
