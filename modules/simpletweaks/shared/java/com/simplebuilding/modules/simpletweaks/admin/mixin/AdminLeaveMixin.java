package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** No "left the game" line for vanished players. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AdminLeaveMixin {
    @Shadow public ServerPlayer player;

    @WrapWithCondition(method = "removePlayerFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private boolean simpletweaks$leaveMessage(PlayerList list, Component message, boolean overlay) { return !Vanish.vanished(player); }
}
