package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simplebuilding.modules.simpletweaks.admin.Admin;
import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;
import com.simplebuilding.modules.simpletweaks.admin.maintenance.Maintenance;
import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import java.net.SocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Join hook, silent join of vanished players, maintenance login check, closing offline views before a login loads data. */
@Mixin(PlayerList.class)
public abstract class AdminPlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void simpletweaks$joined(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) { Admin.joined(player); }

    @WrapWithCondition(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private boolean simpletweaks$joinMessage(PlayerList self, Component message, boolean overlay, @Local(argsOnly = true) ServerPlayer player) {
        return !Vanish.vanished(player);
    }

    @Inject(method = "canPlayerLogin", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$maintenance(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> cir) {
        if (cir.getReturnValue() == null) { var refuse = Maintenance.refuse(server, profile); if (refuse != null) cir.setReturnValue(refuse); }
    }

    @Inject(method = "loadPlayerData", at = @At("HEAD"))
    private void simpletweaks$loading(NameAndId profile, CallbackInfoReturnable<java.util.Optional<net.minecraft.nbt.CompoundTag>> cir) { Invsee.loading(server, profile.id()); }
}
