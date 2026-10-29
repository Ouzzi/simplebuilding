package com.simplemobheads.mixin.client;

import com.simplemobheads.client.HeadHelmetRender;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BS28b-1: block entity renderers get no equipment renderer, entity renderers do. A placed head's helmet is drawn with the
 * one Vanilla builds for the newest entity renderer context (same equipment assets, same trim palettes).
 */
@Mixin(EntityRendererProvider.Context.class)
public abstract class EntityRendererContextMixin {
    @Shadow
    @Final
    private EquipmentLayerRenderer equipmentRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplemobheads$keepEquipmentRenderer(CallbackInfo ci) {
        HeadHelmetRender.renderer(this.equipmentRenderer);
    }
}
