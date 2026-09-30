package com.simplefun.mixin.client;

import com.simplefun.client.PiggyStateExtension;
import com.simplefun.platform.Services;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void simplefun$updatePiggyState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof PiggyStateExtension piggyState) {
            // The loader-specific synced state (entity data on Fabric, data attachment on NeoForge).
            piggyState.simplefun$setPiggy(Services.PLATFORM.isPiggySynced(entity));
        }
    }
}
