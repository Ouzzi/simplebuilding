package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Empty-slot sprites (saddle, horse armor, potion, lapis ...) on styled screens: an engraved silhouette in the slot's
 * top-line colour at 80 % (README W0-B point 5, preview {@code sprite_mask}) instead of Vanilla's grey.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotIconMixin {
    @WrapOperation(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void simplecontainers$engravedIcon(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y,
            int width, int height, Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        int tint = StyledScreens.iconTint((AbstractContainerScreen<?>) (Object) this, slot);
        if (tint == -1) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height, tint);
        }
    }
}
