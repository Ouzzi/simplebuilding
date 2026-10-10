package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simpleinterfaces.client.StationScreens;
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
public abstract class StonecutterStationMixin extends AbstractContainerScreen<StonecutterMenu> {
    private StonecutterStationMixin(StonecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simpleinterfaces$scroller(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.thumb(this, graphics, sprite.getPath(), x, y, w, h);
        return false;
    }

    @WrapWithCondition(method = "extractButtons", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simpleinterfaces$recipeTile(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.tile(this, graphics, sprite.getPath(), x, y, w, h, 0.18);
        return false;
    }
}
