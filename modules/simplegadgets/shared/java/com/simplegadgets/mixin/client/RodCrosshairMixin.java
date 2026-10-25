package com.simplegadgets.mixin.client;

import com.simplegadgets.client.RodClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the Resonance Rod beams, the crosshair steps aside (owner 2026-09-29): the laser dot sits exactly where its centre was
 * ({@code RodRenderer#dotCentre}) and would otherwise hide under it. Vanilla draws the crosshair in {@code Hud#extractCrosshair}.
 */
@Mixin(Hud.class)
public abstract class RodCrosshairMixin {

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void simplegadgets$hideCrosshairWhileBeaming(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && RodClient.isAimingLaser(client.player)) {
            ci.cancel();
        }
    }
}
