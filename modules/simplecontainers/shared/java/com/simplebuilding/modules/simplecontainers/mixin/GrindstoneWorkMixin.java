package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkDraw;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Grindstone in the Simple style: the error arrow gives way to the red cross over the engraved arrow. */
@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneWorkMixin extends AbstractContainerScreen<GrindstoneMenu> {
    private GrindstoneWorkMixin(GrindstoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$error(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!WorkScreens.active(this)) return true;
        WorkDraw.error(graphics, this.leftPos + 101, this.topPos + 35, WorkDraw.ARROW.width, WorkDraw.ARROW.height);
        return false;
    }
}
