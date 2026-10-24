package com.simplebuilding.modules.simplerespawn.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While spectating, the mouse turns the camera around the downed body instead of the body itself. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void simplerespawn$orbit(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        if (RespawnClient.orbiting()) RespawnClient.turn(dx, dy);
        else original.call(player, dx, dy);
    }
}
