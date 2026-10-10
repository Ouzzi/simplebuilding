package com.simplequalityoflife.mixin.client;

import com.simplequalityoflife.client.LinkedHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Linked GUIs: the marked container's HUD slot. */
@Mixin(Hud.class)
public abstract class HudLinkedMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void qol$linkedHud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        LinkedHud.render(graphics);
    }
}
