package com.simplebuilding.modules.simplerespawn.mixin.client;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** While spectating, the mouse turns the camera around the downed body instead of the body itself. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void simplerespawn$orbit(LocalPlayer player, double dx, double dy) {
        if (RespawnClient.orbiting()) RespawnClient.turn(dx, dy);
        else player.turn(dx, dy);
    }
}
