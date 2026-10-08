package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Stonecutter in the Simple style: scroller thumb and recipe tiles drawn as raised/sunk tiles in the block's colours. */
@Mixin(StonecutterScreen.class)
public abstract class StonecutterWorkMixin extends AbstractContainerScreen<StonecutterMenu> {
    private StonecutterWorkMixin(StonecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$scroller(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!WorkScreens.active(this)) return true;
        WorkScreens.thumb(this, graphics, sprite.getPath(), x, y, w, h);
        return false;
    }

    @WrapWithCondition(method = "extractButtons", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$recipeTile(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!WorkScreens.active(this)) return true;
        WorkScreens.tile(this, graphics, sprite.getPath(), x, y, w, h, 0.18);
        return false;
    }
}
