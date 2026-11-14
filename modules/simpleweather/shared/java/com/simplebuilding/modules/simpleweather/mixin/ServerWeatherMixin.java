package com.simplebuilding.modules.simpleweather.mixin;

import com.simplebuilding.modules.simpleweather.SimpleWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Disable weather": sets the Vanilla game rule advance_weather=false, clears active rain/thunder and skips the weather cycle. */
@Mixin(ServerLevel.class)
public class ServerWeatherMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void simpleweather$disable(CallbackInfo ci) {
        if (!SimpleWeather.active() || !SimpleWeather.config().disableWeather) return;
        ServerLevel world = (ServerLevel) (Object) this;
        // simpleweather-2: the switch drives the Vanilla game rule instead of a second weather logic.
        var rules = world.getGameRules();
        if (rules.get(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER)) {
            rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, world.getServer());
        }
        // Only touch the data when it actually rains: no needless writes or packets.
        if (world.isRaining() || world.isThundering()) {
            var weather = world.getWeatherData();
            weather.setRaining(false); weather.setThundering(false); weather.setRainTime(0); weather.setThunderTime(0);
            world.setRainLevel(0); world.setThunderLevel(0);
        }
        ci.cancel();
    }

    /** Frequency factors: after the Vanilla step the dry-spell countdowns run faster or slower; durations stay Vanilla. */
    @Inject(method = "advanceWeatherCycle", at = @At("TAIL"))
    private void simpleweather$factor(CallbackInfo ci) {
        var config = SimpleWeather.config();
        if (!SimpleWeather.active() || config.rainFrequencyFactor == 1f && config.thunderFrequencyFactor == 1f) return;
        ServerLevel world = (ServerLevel) (Object) this;
        if (!world.canHaveWeather() || !world.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER)) return;
        var weather = world.getWeatherData();
        if (weather.getClearWeatherTime() > 0) return; // /weather clear <time> is exact
        long time = world.getGameTime();
        if (!weather.isRaining()) weather.setRainTime(SimpleWeather.scaled(weather.getRainTime(), SimpleWeather.shift(time, config.rainFrequencyFactor)));
        if (!weather.isThundering()) weather.setThunderTime(SimpleWeather.scaled(weather.getThunderTime(), SimpleWeather.shift(time, config.thunderFrequencyFactor)));
    }
}
