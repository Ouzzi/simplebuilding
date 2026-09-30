package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerWeatherMixin {

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void disableWeather(CallbackInfo ci) {
        // Performance-Check: Erst Konfig prüfen
        if (Simplequalityoflife.getConfig().qOL.disableWeather) {
            ServerLevel world = (ServerLevel) (Object) this;

            // Optimierung: Nur eingreifen, wenn das Wetter tatsächlich schlecht ist!
            // Das verhindert unnötige Server-Operationen und Netzwerk-Spam.
            if (world.isRaining() || world.isThundering()) {
                var weather = world.getWeatherData();
                weather.setRaining(false); weather.setThundering(false); weather.setRainTime(0); weather.setThunderTime(0);
                world.setRainLevel(0); world.setThunderLevel(0);
            }

            // WICHTIG: Die originale Methode abbrechen.
            // Das spart CPU, da Minecraft keine Zufallszahlen für Blitz/Regen berechnen muss.
            ci.cancel();
        }
    }
}
