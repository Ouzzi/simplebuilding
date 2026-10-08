package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkDraw;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Loom in the Simple style: scroller thumb and pattern tiles as raised/sunk tiles, the "too many patterns" error as the
 * red cross over the result slot. The empty-slot sprites (banner, dye, pattern) stay Vanilla.
 */
@Mixin(LoomScreen.class)
public abstract class LoomWorkMixin extends AbstractContainerScreen<LoomMenu> {
    private LoomWorkMixin(LoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$sprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        String path = sprite.getPath();
        if (!path.startsWith("container/loom/") || !WorkScreens.active(this)) return true;
        if (path.startsWith("container/loom/scroller")) WorkScreens.thumb(this, graphics, path, x, y, w, h);
        else if (path.startsWith("container/loom/pattern")) WorkScreens.tile(this, graphics, path, x, y, w, h, 0.35);
        else if (path.equals("container/loom/error")) WorkDraw.error(graphics, x, y, w, h);
        else return true;
        return false;
    }
}
