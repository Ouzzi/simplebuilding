package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simplebuilding.modules.simpletweaks.play.welcome.Welcome;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Own leave line instead of the Vanilla one (T19). */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayLeaveMixin {
    @Shadow public ServerPlayer player;

    @WrapWithCondition(method = "removePlayerFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private boolean simpletweaks$playLeaveLine(PlayerList list, Component message, boolean overlay) { return Welcome.vanillaLine(player, false); }
}
