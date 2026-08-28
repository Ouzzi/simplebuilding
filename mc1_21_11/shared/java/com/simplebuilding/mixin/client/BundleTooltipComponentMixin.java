package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
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

    @Override
    public void simplebuilding$setCapacityScale(float scale) {
        this.capacityScale = scale;
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
