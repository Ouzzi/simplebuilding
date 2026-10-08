package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "work" (W1 G2): the enchanting table in the Simple style ({@link WorkScreens#enchanting}): the Vanilla PNG is
 * skipped, the three offer rows become sunk fields with lapis gems instead of the level icons and light runes; the
 * cost numbers, tooltips, clicks and the animated 3D book stay Vanilla.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentBackgroundMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Unique
    private UiPalette simplecontainers$palette;

    private EnchantmentBackgroundMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        int top = (this.height - this.imageHeight) / 2;
        int left = (this.width - this.imageWidth) / 2;
        this.simplecontainers$palette = WorkScreens.enchanting(this, graphics, left, top, this.imageWidth, this.imageHeight, this.titleLabelY)
                ? WorkScreens.palette(this, this.imageWidth, this.imageHeight, this.titleLabelY) : null;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return this.simplecontainers$palette == null;
    }

    @WrapWithCondition(method = "extractBackground", require = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$offerSprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int x, int y, int width, int height) {
        UiPalette p = this.simplecontainers$palette;
        return p == null || !WorkScreens.enchantingSprite(graphics, p, sprite, x, y, width, height);
    }

    @ModifyArg(method = "extractBackground", require = 2, index = 5, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)I"))
    private int simplecontainers$runeColor(int color) {
        return this.simplecontainers$palette == null ? color : WorkScreens.runeColor(color);
    }
}
