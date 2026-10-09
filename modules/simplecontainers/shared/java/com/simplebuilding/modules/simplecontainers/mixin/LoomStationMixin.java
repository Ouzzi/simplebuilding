package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StationScreens;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
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
 * red cross over the result slot. The empty-slot sprites (banner, dye, pattern) are tinted like all styled slot icons.
 */
@Mixin(LoomScreen.class)
public abstract class LoomStationMixin extends AbstractContainerScreen<LoomMenu> {
    private LoomStationMixin(LoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$sprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        String path = sprite.getPath();
        if (!StationScreens.active(this)) return true;
        if (path.startsWith("container/slot/")) {
            // the empty banner/dye/pattern slot sprites as engraved silhouettes, like every other styled slot icon
            int color = StyledScreens.slotIconColor(this, this.menu.getSlot(0), sprite, this.imageWidth, this.imageHeight, this.titleLabelY);
            if (color == -1) return true;
            if (color != 0) graphics.blitSprite(pipeline, sprite, x, y, w, h, color);
            return false;
        }
        if (path.startsWith("container/loom/scroller")) StationScreens.thumb(this, graphics, path, x, y, w, h);
        else if (path.startsWith("container/loom/pattern")) StationScreens.tile(this, graphics, path, x, y, w, h, 0.35);
        else if (path.equals("container/loom/error")) StationScreens.error(graphics, x, y, w, h);
        else return true;
        return false;
    }
}
