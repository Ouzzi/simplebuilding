package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientBundleTooltip.class)
public abstract class BundleTooltipComponentMixin implements BundleTooltipAccessor {

    @Final
    @Shadow
    private BundleContents contents;

    @Unique
    private float capacityScale = 1.0f;

    @Unique
    private int slotTint = -1;

    @Override
    public void simplebuilding$setCapacityScale(float scale) {
        this.capacityScale = scale;
    }

    @Override
    public void simplebuilding$setSlotTint(int argb) {
        this.slotTint = argb;
    }

    @Override
    public int simplebuilding$getSlotTint() {
        return this.slotTint;
    }

    /**
     * Gefaerbte Buendel der Mod: der Hintergrund jedes Felds (zweiter {@code blitSprite} in
     * {@code renderSlot}, nach dem Auswahl-Hintergrund - am Bytecode geprueft) wird mit einer
     * aufgehellten Farbe multipliziert. Das ausgewaehlte Feld behaelt Vanillas Hervorhebung.
     */
    @WrapOperation(
            method = "renderSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 1
            )
    )
    private void simplebuilding$tintSlotBackground(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite,
                                                   int x, int y, int width, int height, Operation<Void> original) {
        if (this.slotTint == -1) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height, this.slotTint);
        }
    }

    // MC 26.2 reichte die Fuellmenge als Fraction-Argument von extractImage nach
    // extractBundleWithItemsTooltip -> extractProgressbar durch, weshalb dort ein einziges
    // @ModifyArg (index 6) genuegte. In 1.21.11 gibt es dieses Argument nicht: renderImage
    // ruft renderBundleWithItemsTooltip ohne Fraction auf, und die drei Verbraucher
    // getProgressBarFill / getProgressBarTexture / getProgressBarFillText holen sich die
    // Fuellmenge jeweils selbst ueber this.contents.weight(). Am Bytecode geprueft: genau
    // diese drei Aufrufstellen von BundleContents#weight() existieren in ClientBundleTooltip,
    // und in 26.2 waren es genau dieselben drei Verbraucher des modifizierten Arguments.
    @ModifyExpressionValue(
            method = {"getProgressBarFill", "getProgressBarTexture", "getProgressBarFillText"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BundleContents;weight()Lorg/apache/commons/lang3/math/Fraction;"
            )
    )
    private Fraction simplebuilding$scaleOccupancy(Fraction occupancy) {
        if (this.capacityScale <= 1.0f) {
            return occupancy;
        }
        return occupancy.divideBy(Fraction.getFraction((int) this.capacityScale, 1));
    }
}
