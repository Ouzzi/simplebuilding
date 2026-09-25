package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// MC 26.4-snapshot twin of mc26_3/overlay/java/.../BundleTooltipComponentMixin.java: RenderPipeline is
// back in com.mojang.blaze3d.pipeline (as on 26.2); everything else is the 26.3 version.
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
     * {@code extractSlot}, nach dem Auswahl-Hintergrund) wird mit einer aufgehellten Farbe
     * multipliziert - eine leichte Toenung, das Item darueber bleibt unberuehrt. Das
     * ausgewaehlte Feld behaelt Vanillas Hervorhebung.
     */
    @WrapOperation(
            method = "extractSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 1
            )
    )
    private void simplebuilding$tintSlotBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                                   int x, int y, int width, int height, Operation<Void> original) {
        if (this.slotTint == -1) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height, this.slotTint);
        }
    }

    @ModifyArg(
            method = "extractImage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;extractBundleWithItemsTooltip(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/GuiGraphicsExtractor;Lorg/apache/commons/lang3/math/Fraction;)V"
            ),
            index = 6
    )
    private Fraction simplebuilding$scaleOccupancy(Fraction occupancy) {
        if (this.capacityScale <= 1.0f) {
            return occupancy;
        }
        return occupancy.divideBy(Fraction.getFraction((int) this.capacityScale, 1));
    }
}
