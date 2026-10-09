package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StationScreens;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The smithing table's cycling empty-slot icons (templates, armour pieces, ingots) as engraved silhouettes like every
 * other styled slot icon ({@link StyledScreens#slotIconColor}), keeping Vanilla's fade between two icons.
 */
@Mixin(CyclingSlotBackground.class)
public abstract class CyclingIconStationMixin {
    @WrapOperation(method = "extractIcon", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void simplecontainers$tint(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier icon, int x, int y, int w, int h,
            int color, Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        if (Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen && StationScreens.active(screen)) {
            int tint = StyledScreens.slotIconColor(screen, slot, icon, 176, 166, 6);
            if (tint != -1 && tint != 0) {
                int alpha = (tint >>> 24) * (color >>> 24) / 255;
                original.call(graphics, pipeline, icon, x, y, w, h, alpha << 24 | tint & 0xFFFFFF);
                return;
            }
        }
        original.call(graphics, pipeline, icon, x, y, w, h, color);
    }
}
