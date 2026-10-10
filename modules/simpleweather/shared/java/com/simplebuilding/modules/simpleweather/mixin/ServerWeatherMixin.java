package com.simplebuilding.modules.simpleweather.mixin;

import com.simplebuilding.modules.simpleweather.SimpleWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Disable weather": clears active rain/thunder and skips the weather cycle (moved from Simple QoL). */
@Mixin(ServerLevel.class)
public class ServerWeatherMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void simpleweather$disable(CallbackInfo ci) {
        if (!SimpleWeather.active() || !SimpleWeather.config().disableWeather) return;
        ServerLevel world = (ServerLevel) (Object) this;
        // Only touch the data when it actually rains: no needless writes or packets.
        if (world.isRaining() || world.isThundering()) {
            var weather = world.getWeatherData();
            weather.setRaining(false); weather.setThundering(false); weather.setRainTime(0); weather.setThunderTime(0);
            world.setRainLevel(0); world.setThunderLevel(0);
        }
        ci.cancel();
    }
}
