package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
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

// MC 26.3 twin of common/src/mc26_2/java/.../BundleTooltipComponentMixin.java: only the
// RenderPipeline package (com.mojang.renderpearl.api.pipeline) differs.
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
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
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
        return occupancy.divideBy(Fraction.getFraction((double) this.capacityScale));
    }

    @Unique private int simplebuilding$progressColor = -1;
    @Override public void simplebuilding$setProgressColor(int argb) { simplebuilding$progressColor = argb; }

    @Shadow @Final private static Identifier PROGRESSBAR_BORDER_SPRITE;
    @Shadow private static Identifier getProgressBarTexture(Fraction occupancy) { throw new AssertionError(); }
    @Shadow private static int getProgressBarFill(Fraction occupancy) { throw new AssertionError(); }
    @Shadow private static void extractEmptyBundleDescriptionText(int x, int y, Font font, GuiGraphicsExtractor graphics) { throw new AssertionError(); }
    @Shadow private static int getEmptyBundleDescriptionTextHeight(Font font) { throw new AssertionError(); }

    @WrapOperation(method = "extractBundleWithItemsTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;extractProgressbar(IILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lorg/apache/commons/lang3/math/Fraction;)V"))
    private void simplebuilding$progress(int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction occupancy, Operation<Void> original) {
        if (simplebuilding$progressColor == -1) { original.call(x, y, font, graphics, occupancy); return; }
        simplebuilding$drawProgress(x, y, font, graphics, occupancy);
    }

    @WrapOperation(method = "extractImage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;extractEmptyBundleTooltip(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void simplebuilding$empty(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics, Operation<Void> original) {
        if (simplebuilding$progressColor == -1) { original.call(font, x, y, width, height, graphics); return; }
        int left = x + (width - 96) / 2;
        extractEmptyBundleDescriptionText(left, y, font, graphics);
        simplebuilding$drawProgress(left, y + getEmptyBundleDescriptionTextHeight(font) + 4, font, graphics, Fraction.ZERO);
    }

    @Unique private void simplebuilding$drawProgress(int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction occupancy) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getProgressBarTexture(occupancy), x + 1, y, getProgressBarFill(occupancy), 13);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, x, y, 96, 13);
        var data = new ReinforcedBundleTooltipData(contents, Math.round(capacityScale * 64));
        graphics.centeredText(font, Component.literal(data.capacityText()), x + 48, y + 3, simplebuilding$progressColor);
    }
}
