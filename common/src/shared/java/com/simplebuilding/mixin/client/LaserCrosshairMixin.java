package com.simplebuilding.mixin.client;

import com.simplebuilding.tweaks.client.TweaksClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the Amethyst Resonance Rod beams, the crosshair steps aside (owner 2026-09-29): the laser
 * dot sits exactly where its centre was ({@code LaserRenderer#dotCentre}) and would otherwise hide
 * under it. Vanilla draws the crosshair in {@code Hud#extractCrosshair} on every loader (NeoForge's
 * crosshair layer calls the same method), so one injection covers all of them.
 */
@Mixin(Hud.class)
public abstract class LaserCrosshairMixin {

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$hideCrosshairWhileBeaming(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && TweaksClient.isAimingLaser(client.player)) {
            ci.cancel();
        }
    }
}
