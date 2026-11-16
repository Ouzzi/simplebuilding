package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simpleinterfaces.client.LinkedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Linked GUIs (N65): the panel's layout after a container screen's whole {@code init} (recipe book screens move
 * {@code leftPos} after {@code AbstractContainerScreen.init}); and the screen's own background clipped to the band the
 * panel leaves (A: above the marked box, B's marked tab: below the storage area) - the scissor starts after the dimmed
 * world ({@code Screen.extractBackground}, which every container screen calls first) and ends with the background.
 */
@Mixin(Screen.class)
public abstract class ScreenLinkedMixin {
    @Unique private boolean qol$inBackground;
    @Unique private boolean qol$clipped;

    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void qol$afterInit(int width, int height, CallbackInfo ci) {
        if (this instanceof LinkedScreen screen) screen.qol$layout();
    }

    @Inject(method = "rebuildWidgets()V", at = @At("TAIL"))
    private void qol$afterRebuild(CallbackInfo ci) {
        if (this instanceof LinkedScreen screen) screen.qol$layout();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void qol$clip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!this.qol$inBackground || this.qol$clipped || !(this instanceof LinkedScreen screen)) return;
        int[] band = screen.qol$background();
        if (band == null) return;
        graphics.enableScissor(band[0], band[1], band[2], band[3]);
        this.qol$clipped = true;
    }

    @WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void qol$background(Screen self, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, Operation<Void> original) {
        this.qol$inBackground = true;
        try {
            original.call(self, graphics, mouseX, mouseY, a);
        } finally {
            this.qol$inBackground = false;
            if (this.qol$clipped) {
                graphics.disableScissor();
                this.qol$clipped = false;
            }
        }
    }
}
