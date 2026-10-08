package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Group "work" (W1 G2): the beacon's confirm/cancel icons as a green check and a red cross (W0-B preview). */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconSpriteScreenButton")
public abstract class BeaconIconMixin extends AbstractButton {
    private BeaconIconMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @WrapWithCondition(method = "extractIcon", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$icon(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!(Minecraft.getInstance().gui.screen() instanceof BeaconScreen screen)) return true;
        UiPalette p = WorkScreens.beaconPalette(screen);
        return p == null || !WorkScreens.beaconIcon(graphics, p, sprite, x, y, this.active);
    }
}
