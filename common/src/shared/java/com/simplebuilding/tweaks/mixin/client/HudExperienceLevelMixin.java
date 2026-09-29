package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.client.SpawnElytraHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mit Spawn-Elytra keine XP-Stufe ueber der Leiste, dort steht der Timer (Simple Tweaks: InGameHudLevelMixin).
 *
 * <p>Zwei Ziele, weil NeoForge den Aufruf verlegt: Vanilla/Fabric/Forge zeichnen die Stufe in
 * {@code extractHotbarAndDecorations}, NeoForge hat {@code Hud} in GUI-Layer zerlegt und ruft
 * denselben {@code ContextualBar.extractExperienceLevel} aus der privaten Layer-Methode
 * {@code extractExperienceLevel} (Layer {@code VanillaGuiLayers.EXPERIENCE_LEVEL}). Auf jedem Loader
 * existiert genau eine der beiden Methoden; Mixin ueberspringt den fehlenden Namen, und
 * {@code require = 1} prueft weiter, dass die Umleitung irgendwo greift.
 */
@Mixin(Hud.class)
public abstract class HudExperienceLevelMixin {
    @Redirect(method = {"extractHotbarAndDecorations", "extractExperienceLevel"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void simplebuilding$hideLevel(GuiGraphicsExtractor graphics, Font font, int level) {
        if (SpawnElytraHud.wearsSpawnElytra(Minecraft.getInstance().player)) {
            return;
        }
        ContextualBar.extractExperienceLevel(graphics, font, level);
    }
}
