package com.simplebuilding.modules.simplemobs.forge.mixin;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplemobs.client.WingsLookModelProperty;
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

/** Forge has no item-property registration event: registers simplemobs:wings_look_v2 (ABN30-9) like SimpleBuilding's ConditionalItemModelPropertiesMixin. */
@Mixin(ConditionalItemModelProperties.class)
public abstract class WingsLookPropertyMixin {
    @Shadow @Final private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void simplemobs$registerWingsLook(CallbackInfo ci) {
        ID_MAPPER.put(WingsLookModelProperty.ID, WingsLookModelProperty.CODEC);
    }
}
