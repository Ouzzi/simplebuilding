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
 * Free look (FL05, owner 05.11.): exactly one crosshair - the vanilla one. While free look is on (and the client option is on)
 * it is moved to the screen spot the body/player direction points at. Off screen: clamped to the edge (so it still shows the
 * way); behind the camera: hidden.
 */
@Mixin(Hud.class)
public abstract class FreeLookCrosshairMixin {
    private static final int MARGIN = 8;
    private boolean simplelib$moved;

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void simplelib$moveCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        simplelib$moved = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || !FreeLook.crosshairOn()) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        int[] p = FreeLook.crosshairPoint(mc.player.getViewXRot(deltaTracker.getGameTimeDeltaPartialTick(true)), w, h, mc.gameRenderer.mainCamera().getFov());
        if (p == null) {
            ci.cancel();
            return;
        }
        int x = Math.max(MARGIN, Math.min(w - MARGIN, p[0])), y = Math.max(MARGIN, Math.min(h - MARGIN, p[1]));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x - w / 2, y - h / 2);
        simplelib$moved = true;
    }

    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void simplelib$restore(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (simplelib$moved) {
            graphics.pose().popMatrix();
            simplelib$moved = false;
        }
    }
}
