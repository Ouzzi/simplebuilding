package com.simplebuilding.modules.simpleweather.mixin.client;

import com.simplebuilding.modules.simpleweather.SimpleWeather;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeatherEffectRenderer.class)
public class VisualRainMixin {
    /**
     * Scales the rain/snow drawn in the air by the rain level. tickRainParticles reads getRainLevel on ClientLevel and is
     * not hit here; ground particles and sound are thinned by ClientWeatherMixin instead.
     */
    @Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float simpleweather$density(net.minecraft.client.multiplayer.ClientLevel world, float delta) {
        float original = world.getRainLevel(delta);
        if (!SimpleWeather.active()) return original;
        int percent = SimpleWeather.config().clientRainParticleDensity;
        if (percent >= 100) return original;
        if (percent <= 0) return 0.0F;
        return original * (percent / 100.0F);
    }
}
