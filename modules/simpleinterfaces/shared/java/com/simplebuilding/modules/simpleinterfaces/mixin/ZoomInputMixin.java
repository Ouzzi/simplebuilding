package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.ZoomState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spyglass and zoom key (BS29-3): the wheel zooms steplessly instead of scrolling the hotbar; the mouse slows down with the zoom. */
@Mixin(MouseHandler.class)
public abstract class ZoomInputMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void sb$zoomWheel(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (ZoomState.scroll(yoffset)) ci.cancel();
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void sb$zoomSensitivity(double mousea, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || !ZoomState.overridesFov(player)) return;
        double scale = ZoomState.sensitivity(player);
        accumulatedDX *= scale;
        accumulatedDY *= scale;
    }
}
