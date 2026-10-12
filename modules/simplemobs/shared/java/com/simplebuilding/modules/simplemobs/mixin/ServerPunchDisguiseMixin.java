package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cloak of deception (owner 12.10.): every attack attempt, also a swing into the air, ends the disguise. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPunchDisguiseMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePunch", at = @At("TAIL"))
    private void simplemobs$punchEndsDisguise(ServerboundPunchPacket packet, CallbackInfo ci) {
        Disguise.punch(player);
    }
}
