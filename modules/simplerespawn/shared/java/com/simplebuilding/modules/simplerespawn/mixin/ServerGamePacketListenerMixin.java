package com.simplebuilding.modules.simplerespawn.mixin;

import com.simplebuilding.modules.simplerespawn.RespawnServer;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server authority: a downed player's attack, use, interaction, block, drop/swap and inventory packets are dropped,
 * whatever the client sends (the mod's own client never sends them; this stops modified clients).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    /** After the packet moved to the main thread (REV2-C-4): the check reads player state, which belongs to that thread. */
    private static final String SAME_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    @Shadow public ServerPlayer player;

    private boolean simplerespawn$blocked() {
        return RespawnServer.blocksActions(player);
    }

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$interact(ServerboundInteractPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }

    @Inject(method = "handleAttack", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$attack(ServerboundAttackPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }

    @Inject(method = "handleUseItem", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$useItem(ServerboundUseItemPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$useItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$action(ServerboundPlayerActionPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }

    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$click(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!simplerespawn$blocked()) return;
        ci.cancel();
        player.containerMenu.sendAllDataToRemote(); // the client moved items on its own screen: show the real state again
    }

    @Inject(method = "handlePickItemFromBlock", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$pickBlock(ServerboundPickItemFromBlockPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }

    @Inject(method = "handlePickItemFromEntity", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void simplerespawn$pickEntity(ServerboundPickItemFromEntityPacket packet, CallbackInfo ci) { if (simplerespawn$blocked()) ci.cancel(); }
}
