package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.Protect;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Join hook of the protection features (Freecam rule). */
@Mixin(PlayerList.class)
public abstract class ProtectPlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void simpletweaks$protectJoined(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) { Protect.joined(player); }
}
