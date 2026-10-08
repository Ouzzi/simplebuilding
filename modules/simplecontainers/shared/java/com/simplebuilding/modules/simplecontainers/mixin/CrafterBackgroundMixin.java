package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StorageDecors;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "storage" (W1 G1): the crafter (preview g1-crafter.png). Background like {@link StorageBackgroundMixin} plus the
 * crafter's own parts ({@link StorageDecors#crafter}): big result slot, redstone sign, arrow. Vanilla's redstone arrow
 * sprite (drawn over everything in {@code extractRenderState}) and the red disabled-slot sprite are replaced while
 * styled; the disabled slot shows an engraved cross instead.
 */
@Mixin(CrafterScreen.class)
public abstract class CrafterBackgroundMixin extends AbstractContainerScreen<CrafterMenu> {
    @Unique
    private boolean simplecontainers$styled;

    private CrafterBackgroundMixin(CrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simplecontainers$styled = StyledScreens.drawBackground(this, graphics, this.leftPos, this.topPos,
                this.imageWidth, this.imageHeight, this.titleLabelY, StorageDecors.crafter(this.menu));
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simplecontainers$styled;
    }

    @WrapWithCondition(method = "extractRedstone", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$vanillaRedstone(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int x, int y, int width, int height) {
        return !StyledScreens.isStyled(this);
    }

    @WrapWithCondition(method = "extractDisabledSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$disabledSlot(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int x, int y, int width, int height, @Local(argsOnly = true) CrafterSlot slot) {
        ScreenStyle style = StyledScreens.isStyled(this) ? StyledScreens.style(this) : null;
        if (style == null) return true;
        StorageDecors.disabledSlot(graphics, slot, StyledScreens.palette(this, style));
        return false;
    }
}
