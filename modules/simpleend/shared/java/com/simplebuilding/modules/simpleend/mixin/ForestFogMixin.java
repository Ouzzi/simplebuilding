package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.client.ForestFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pulls the atmospheric fog toward the forest core's fog when a forest lies ahead (same on every loader). */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class ForestFogMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private void simpleend$forestFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        ForestFog.apply(fog, camera, level);
    }
}
