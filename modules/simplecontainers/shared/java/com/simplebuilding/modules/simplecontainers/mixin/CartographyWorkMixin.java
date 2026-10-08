package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkDraw;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Cartography table in the Simple style: the error arrow gives way to the red cross; the map sprites stay Vanilla. */
@Mixin(CartographyTableScreen.class)
public abstract class CartographyWorkMixin extends AbstractContainerScreen<CartographyTableMenu> {
    private CartographyWorkMixin(CartographyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$error(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!sprite.getPath().equals("container/cartography_table/error") || !WorkScreens.active(this)) return true;
        WorkDraw.error(graphics, this.leftPos + 41, this.topPos + 37, WorkDraw.ARROW_SMALL.width, WorkDraw.ARROW_SMALL.height);
        return false;
    }
}
