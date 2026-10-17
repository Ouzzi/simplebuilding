package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.ActionChecks;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Action checks for use-on-block, attack and interact packets, on the server thread (after the thread hand-over). */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ProtectGamePacketMixin {
    @Shadow public ServerPlayer player;

    private static final String SAME_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void simpletweaks$useOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        ActionChecks.block(player, packet.hitResult().getBlockPos(), "use on");
    }
    @Inject(method = "handleAttack", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void simpletweaks$attack(ServerboundAttackPacket packet, CallbackInfo ci) {
        ActionChecks.entity(player, player.level().getEntityOrPart(packet.entityId()), "attack");
    }
    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void simpletweaks$interact(ServerboundInteractPacket packet, CallbackInfo ci) {
        ActionChecks.entity(player, player.level().getEntityOrPart(packet.entityId()), "interact");
    }
}
