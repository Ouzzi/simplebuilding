package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Empty-slot icons (potion, lapis, saddle ...) on styled screens: engraved silhouettes in the slot's top-line colour at
 * 80 % (W0-B README, point 5) instead of Vanilla's grey; the brewing stand's blaze powder icon is left out (its slot
 * shows the fuel level). Vanilla screens are untouched ({@link StyledScreens#slotIcon}).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotIconMixin extends Screen {
    @Shadow protected int titleLabelY;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;

    private SlotIconMixin(Component title) {
        super(title);
    }

    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void simplecontainers$slotIcon(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier icon, int x, int y, int w, int h,
            Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        int color = StyledScreens.slotIconColor((AbstractContainerScreen<?>) (Object) this, slot, icon, this.imageWidth, this.imageHeight,
                this.titleLabelY);
        if (color == -1) original.call(graphics, pipeline, icon, x, y, w, h);
        else if (color != 0) graphics.blitSprite(pipeline, icon, x, y, w, h, color);
    }
}
