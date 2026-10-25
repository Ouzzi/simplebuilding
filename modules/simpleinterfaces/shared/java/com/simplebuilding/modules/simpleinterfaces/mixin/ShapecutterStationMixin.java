package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simpleinterfaces.client.StationScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Simple Blocks' Shapecutter in the stonecutter's Simple style (VIS24-6): its screen copies Vanilla's StonecutterScreen
 * (same image, slots, tiles and scroller, own menu type), so it is drawn exactly like the stonecutter
 * ({@link StationBackgroundMixin}, {@link StonecutterStationMixin}): the background blit gives way to
 * {@link StationScreens#draw}, the scroller and recipe tile sprites to raised/sunk tiles. {@code @Pseudo}: applies only
 * when Simple Blocks is installed; without it nothing happens.
 */
@Pseudo
@Mixin(targets = "com.simpleblocks.saw.client.ShapecutterScreen")
public abstract class ShapecutterStationMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    private ShapecutterStationMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simpleinterfaces$background(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !StationScreens.draw(this, graphics, x, y, this.imageWidth, this.imageHeight);
    }

    @WrapWithCondition(method = "extractBackground", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simpleinterfaces$sprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        if (sprite.getPath().startsWith("container/stonecutter/scroller")) StationScreens.thumb(this, graphics, sprite.getPath(), x, y, w, h);
        else StationScreens.tile(this, graphics, sprite.getPath(), x, y, w, h, 0.18);
        return false;
    }
}
