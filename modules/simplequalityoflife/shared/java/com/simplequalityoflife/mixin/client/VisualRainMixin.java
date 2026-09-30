package com.simplequalityoflife.mixin.client;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeatherEffectRenderer.class)
public class VisualRainMixin {

    /**
     * Skaliert die visuelle Regen-/Schneedarstellung in der Luft über den RainLevel.
     * Hinweis: tickRainParticles ruft getRainLevel auf ClientLevel (nicht Level) auf und würde von einem
     * Level-Redirect NICHT getroffen. Bodenpartikel/Sound werden daher stattdessen von ClientWeatherMixin reduziert.
     */
    @Redirect(
        method = "extractRenderState", // Zuständig für die Regen-Textur in der Luft
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"
        )
    )
    private float modifyRainDensity(net.minecraft.client.multiplayer.ClientLevel world, float delta) {
        // Holen des originalen Regen-Wertes (0.0 bis 1.0)
        float originalGradient = world.getRainLevel(delta);

        // Holen unserer Config (0 bis 100)
        int densityPercent = Simplequalityoflife.getLocalConfig().qOL.clientRainParticleDensity;

        // Wenn die Einstellung auf 100 steht, geben wir den originalen Wert zurück
        if (densityPercent >= 100) {
            return originalGradient;
        }

        // Wenn die Einstellung auf 0 steht, sagen wir dem Renderer: "Es regnet nicht"
        if (densityPercent <= 0) {
            return 0.0F;
        }

        // Dazwischen: Wir skalieren die Intensität herunter.
        // Beispiel: Original 1.0 (starker Regen) * 50% Einstellung = 0.5 (sieht aus wie leichter Regen)
        return originalGradient * (densityPercent / 100.0F);
    }
}
