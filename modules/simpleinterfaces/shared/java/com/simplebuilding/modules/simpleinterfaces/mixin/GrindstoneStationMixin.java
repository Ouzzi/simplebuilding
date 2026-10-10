package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplebuilding.modules.simpleinterfaces.client.StationScreens;
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
public abstract class GrindstoneStationMixin extends AbstractContainerScreen<GrindstoneMenu> {
    private GrindstoneStationMixin(GrindstoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simpleinterfaces$error(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.error(graphics, this.leftPos + 101, this.topPos + 35, UiSymbols.ARROW.width(), UiSymbols.ARROW.height());
        return false;
    }
}
