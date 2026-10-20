package com.simplestorage.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
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

/**
 * The tooltip of a Simple Storage bundle or quiver (split stage 8c-1, copy of SimpleBuilding's
 * {@code BundleTooltipComponentMixin}): Vanilla's bundle tooltip with the fill bar scaled to the capacity, the capacity as
 * text and, for a dyed bundle, slots tinted in its colour. Inert (-1) on every other bundle tooltip.
 */
@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin implements com.simplestorage.client.BundleTooltipTint {

    @Final
    @Shadow
    private BundleContents contents;

    @Unique
    private float simplestorage$capacityScale = 1.0f;

    @Unique
    private int simplestorage$slotTint = -1;

    @Override
    public void simplestorage$setCapacityScale(float scale) {
        this.simplestorage$capacityScale = scale;
    }

    @Override
    public void simplestorage$setSlotTint(int argb) {
        this.simplestorage$slotTint = argb;
    }

    @Override
    public int simplestorage$getSlotTint() {
        return this.simplestorage$slotTint;
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
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 1
            )
    )
    private void simplestorage$tintSlotBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                                   int x, int y, int width, int height, Operation<Void> original) {
        if (this.simplestorage$slotTint == -1) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height, this.simplestorage$slotTint);
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
    private Fraction simplestorage$scaleOccupancy(Fraction occupancy) {
        if (this.simplestorage$capacityScale <= 1.0f) {
            return occupancy;
        }
        return occupancy.divideBy(Fraction.getFraction((double) this.simplestorage$capacityScale));
    }

    @Unique private int simplestorage$progressColor = -1;
    @Override public void simplestorage$setProgressColor(int argb) { simplestorage$progressColor = argb; }

    @Shadow @Final private static Identifier PROGRESSBAR_BORDER_SPRITE;
    @Shadow private static Identifier getProgressBarTexture(Fraction occupancy) { throw new AssertionError(); }
    @Shadow private static int getProgressBarFill(Fraction occupancy) { throw new AssertionError(); }
    @Shadow private static void extractEmptyBundleDescriptionText(int x, int y, Font font, GuiGraphicsExtractor graphics) { throw new AssertionError(); }
    @Shadow private static int getEmptyBundleDescriptionTextHeight(Font font) { throw new AssertionError(); }

    @WrapOperation(method = "extractBundleWithItemsTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;extractProgressbar(IILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lorg/apache/commons/lang3/math/Fraction;)V"))
    private void simplestorage$progress(int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction occupancy, Operation<Void> original) {
        if (simplestorage$progressColor == -1) { original.call(x, y, font, graphics, occupancy); return; }
        simplestorage$drawProgress(x, y, font, graphics, occupancy);
    }

    @WrapOperation(method = "extractImage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;extractEmptyBundleTooltip(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void simplestorage$empty(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics, Operation<Void> original) {
        if (simplestorage$progressColor == -1) { original.call(font, x, y, width, height, graphics); return; }
        int left = x + (width - 96) / 2;
        extractEmptyBundleDescriptionText(left, y, font, graphics);
        simplestorage$drawProgress(left, y + getEmptyBundleDescriptionTextHeight(font) + 4, font, graphics, Fraction.ZERO);
    }

    @Unique private void simplestorage$drawProgress(int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction occupancy) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getProgressBarTexture(occupancy), x + 1, y, getProgressBarFill(occupancy), 13);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, x, y, 96, 13);
        var data = new com.simplestorage.bundle.BundleTooltipData(contents, Math.round(simplestorage$capacityScale * 64));
        graphics.centeredText(font, Component.literal(data.capacityText()), x + 48, y + 3, simplestorage$progressColor);
    }
}
