package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "work" (W1 G2): the brewing stand in the Simple style ({@link WorkScreens#brewing}): the Vanilla PNG and the
 * fuel bar, progress and bubble sprites are skipped; the blaze powder slot shows the fuel, the arrow the progress.
 */
@Mixin(BrewingStandScreen.class)
public abstract class BrewingBackgroundMixin extends AbstractContainerScreen<BrewingStandMenu> {
    @Unique
    private boolean simplecontainers$styled;

    private BrewingBackgroundMixin(BrewingStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void simplecontainers$title(CallbackInfo ci) {
        if (WorkScreens.palette(this, this.imageWidth, this.imageHeight, this.titleLabelY) != null) this.titleLabelX = 8;
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simplecontainers$styled = WorkScreens.brewing(this, this.menu, graphics, this.leftPos, this.topPos, this.imageWidth,
                this.imageHeight, this.titleLabelY);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simplecontainers$styled;
    }

    @WrapWithCondition(method = "extractBackground", require = 3, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"))
    private boolean simplecontainers$vanillaSprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height) {
        return !this.simplecontainers$styled;
    }
}
