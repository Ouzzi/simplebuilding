package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simpleinterfaces.client.WorkScreens;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "work" (W1 G2): the beacon's effect, confirm and cancel buttons in the Simple style when the beacon screen is
 * styled: raised tiles in the box colour (lighter when hovered, sunk when selected, dull when locked) instead of
 * Vanilla's button sprites; the effect icons stay; the "II" upgrade gets a small label as in the W0-B preview.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconScreenButton")
public abstract class BeaconButtonMixin extends AbstractButton {
    @Shadow
    public abstract boolean isSelected();

    private BeaconButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    /** The palette of the open beacon screen when it is styled, else {@code null}. */
    @Unique
    private static @Nullable UiPalette simpleinterfaces$palette() {
        if (Minecraft.getInstance().gui.screen() instanceof BeaconScreen screen) {
            return WorkScreens.beaconPalette(screen);
        }
        return null;
    }

    @WrapWithCondition(method = "extractContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simpleinterfaces$face(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        UiPalette p = simpleinterfaces$palette();
        if (p == null) return true;
        WorkScreens.beaconButton(graphics, p, x, y, w, h, this.active, this.isSelected(), this.isHoveredOrFocused());
        return false;
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void simpleinterfaces$upgradeLabel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!this.getClass().getName().endsWith("$BeaconUpgradePowerButton") || simpleinterfaces$palette() == null) return;
        graphics.text(Minecraft.getInstance().font, "II", this.getX() + 12, this.getY() + 13, 0xFFFFFFFF, true);
    }
}
