package com.simplebuilding.mixin.forge;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.client.property.GaugeNeedleModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge counterpart of NeoForge's {@code RegisterRangeSelectItemModelPropertyEvent} (see
 * {@link SelectItemModelPropertiesMixin}): registers {@code simplebuilding:gauge_needle}, the
 * Gauge's needle, before the item models load - without it the Gauge's model fails with "Unknown
 * element id" and shows the missing texture.
 */
@Mixin(RangeSelectItemModelProperties.class)
public abstract class RangeSelectItemModelPropertiesMixin {

    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void simplebuilding$registerGaugeNeedle(CallbackInfo ci) {
        ID_MAPPER.put(GaugeNeedleModelProperty.ID, GaugeNeedleModelProperty.CODEC);
    }
}
