package com.simplelib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSlotIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Empty-slot icons in the slot colours of screens a {@link UiSlotIcons.Source} styles (owner N37: Vanilla's grey icons
 * vanished on the brown backpack). Every other screen draws Vanilla's icon unchanged.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotIconTintMixin {
    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void simplelib$slotIcon(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier icon, int x, int y, int w, int h,
            Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        UiPalette p = UiSlotIcons.palette((AbstractContainerScreen<?>) (Object) this, slot);
        if (p == null) original.call(graphics, pipeline, icon, x, y, w, h);
        else graphics.blitSprite(pipeline, icon, x, y, w, h, UiSlotIcons.tint(p, icon));
    }
}
