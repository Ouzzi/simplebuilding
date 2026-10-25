package com.simplebuilding.modules.simpleearth.mixin.client;

import com.simplebuilding.modules.simpleearth.orbit.OrbitView;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WE25-1 (Simple Earth): the snowflakes in orbit (owner's picture at y 4367) are the biome's ambient particles, which
 * Vanilla spawns round the camera at any height. While the earth view is shown none are spawned (with the weather mixin:
 * no rain or snow up there).
 */
@Mixin(ClientLevel.class)
public abstract class EarthAmbientMixin {
    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void simpleearth$noAmbientInOrbit(int x, int y, int z, CallbackInfo ci) {
        if (OrbitView.active) ci.cancel();
    }
}
