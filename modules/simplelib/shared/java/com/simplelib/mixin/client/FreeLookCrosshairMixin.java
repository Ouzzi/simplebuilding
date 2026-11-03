package com.simplelib.mixin.client;

import com.simplelib.api.client.FreeLook;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Free look (QOL34, owner 03.11.): the centred crosshair only marks the camera. While free look is on (and the client option
 * "Real Crosshair" is on) it steps aside and a grey, outlined crosshair is drawn where the body really aims - the start point.
 */
@Mixin(Hud.class)
public abstract class FreeLookCrosshairMixin {
    private static final int GREY = 0xFFB4B4B4, OUTLINE = 0xFF000000, ARM = 5;

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void simplelib$realCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        int[] p = FreeLook.crosshairPoint(mc.player.getXRot(), graphics.guiWidth(), graphics.guiHeight(), mc.options.fov().get());
        if (p == null) return;
        graphics.nextStratum();
        int x = p[0], y = p[1];
        graphics.fill(x - ARM - 1, y - 1, x + ARM + 2, y + 2, OUTLINE);
        graphics.fill(x - 1, y - ARM - 1, x + 2, y + ARM + 2, OUTLINE);
        graphics.fill(x - ARM, y, x + ARM + 1, y + 1, GREY);
        graphics.fill(x, y - ARM, x + 1, y + ARM + 1, GREY);
        ci.cancel();
    }
}
