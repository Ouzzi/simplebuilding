package com.simplebuilding.mixin.forge;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.client.property.TransformHintModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge counterpart of NeoForge's {@code RegisterConditionalItemModelPropertyEvent} (see
 * {@link SelectItemModelPropertiesMixin}): registers {@code simplebuilding:transform_hint}, the 26.3
 * Rotator's turning animation, before the item models load - without it the Rotator's model fails with
 * "Unknown element id" and shows the missing texture.
 */
@Mixin(ConditionalItemModelProperties.class)
public abstract class ConditionalItemModelPropertiesMixin {

    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void simplebuilding$registerTransformHint(CallbackInfo ci) {
        ID_MAPPER.put(TransformHintModelProperty.ID, TransformHintModelProperty.CODEC);
    }
}
