package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StationDraw;
import com.simplebuilding.modules.simplecontainers.client.StationScreens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anvil in the Simple style: the name field sprite gives way to the sunk field of the background, the error arrow to
 * the red cross, and the cost box (dark field at the bottom right, which would sit on the box frame) to the XP symbol
 * with the level count in the box ({@link StationScreens#anvilCost}). Only the Vanilla calls are switched off; other mods'
 * injections into these methods keep running.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilStationMixin extends ItemCombinerScreen<AnvilMenu> {
    private AnvilStationMixin(AnvilMenu menu, Inventory inventory, Component title, Identifier texture) {
        super(menu, inventory, title, texture);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$nameField(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        return !StationScreens.active(this);
    }

    @WrapWithCondition(method = "extractErrorIcon", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$error(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationDraw.error(graphics, this.leftPos + 98, this.topPos + 48, StationDraw.ARROW.width, StationDraw.ARROW.height);
        return false;
    }

    @WrapWithCondition(method = "extractLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private boolean simplecontainers$costField(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        return !StationScreens.active(this);
    }

    @WrapWithCondition(method = "extractLabels", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean simplecontainers$costText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        return !StationScreens.active(this);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void simplecontainers$cost(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (StationScreens.active(this)) StationScreens.anvilCost(this.menu, graphics, this.font, this.minecraft.player);
    }
}
