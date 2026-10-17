package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.modules.simpletweaks.play.Play;
import com.simplebuilding.modules.simpletweaks.play.chat.Mentions;
import com.simplebuilding.modules.simpletweaks.play.welcome.Welcome;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Join and leave hooks, own join line (T19), chat mentions (T6). */
@Mixin(PlayerList.class)
public abstract class PlayPlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void simpletweaks$playJoined(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) { Play.joined(player); }

    @WrapWithCondition(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private boolean simpletweaks$playJoinLine(PlayerList self, Component message, boolean overlay, @Local(argsOnly = true) ServerPlayer player) {
        return Welcome.vanillaLine(player, true);
    }

    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void simpletweaks$playLeft(ServerPlayer player, CallbackInfo ci) { Play.left(player); }

    @ModifyVariable(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"), argsOnly = true)
    private PlayerChatMessage simpletweaks$playMentions(PlayerChatMessage message, @Local(argsOnly = true) ServerPlayer sender) {
        return Mentions.chat(message, sender);
    }
}
