package com.simplebuilding.modules.simpleweather.mixin.client;

import com.simplebuilding.modules.simpleweather.SimpleWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ground rain particles and rain sounds thinned to the local density (the air rain is VisualRainMixin). */
@Mixin(ClientLevel.class)
public abstract class ClientWeatherMixin {
    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void simpleweather$rain(CallbackInfo ci) {
        if (!SimpleWeather.active()) return;
        int d = SimpleWeather.config().clientRainParticleDensity;
        if (((ClientLevel) (Object) this).getRandom().nextInt(100) >= Math.clamp(d, 0, 100)) ci.cancel();
    }
}
