package com.simplebuilding.modules.simpleweather.mixin;

import com.simplebuilding.modules.simpleweather.SimpleWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Disable weather": turns the Vanilla game rule advance_weather off and clears active rain/thunder once. After that Vanilla
 * holds the weather (the rule is off), so /weather still works; turning the rule back on while the option is on repeats this.
 */
@Mixin(ServerLevel.class)
public class ServerWeatherMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void simpleweather$disable(CallbackInfo ci) {
        if (!SimpleWeather.active() || !SimpleWeather.config().disableWeather) return;
        ServerLevel world = (ServerLevel) (Object) this;
        // simpleweather-2: the switch drives the Vanilla game rule instead of a second weather logic. N132: only on the tick the
        // rule flips - clearing every tick also swallowed /weather rain, which the handbook promises keeps working.
        var rules = world.getGameRules();
        if (!rules.get(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER)) return;
        rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, world.getServer());
        var weather = world.getWeatherData();
        if (weather.isRaining() || weather.isThundering() || world.isRaining() || world.isThundering()) {
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
