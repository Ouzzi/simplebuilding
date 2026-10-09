package com.simplebuilding.mixin.client;

import com.simplebuilding.client.effect.PerceptionClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shivering (queue N20): the crosshair trembles. The offset is pushed right before Vanilla draws the crosshair
 * (after its first-person/spectator checks and after the laser's HEAD cancel) and popped on every return.
 */
@Mixin(Hud.class)
public abstract class ShiveringCrosshairMixin {
    @Unique
    private boolean simplebuilding$shaking;

    @Inject(method = "extractCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;nextStratum()V", shift = At.Shift.AFTER))
    private void simplebuilding$shake(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float[] offset = PerceptionClient.crosshairOffset();
        if (offset != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(offset[0], offset[1]);
            this.simplebuilding$shaking = true;
        }
    }

    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void simplebuilding$steady(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.simplebuilding$shaking) {
            graphics.pose().popMatrix();
            this.simplebuilding$shaking = false;
        }
    }
}
