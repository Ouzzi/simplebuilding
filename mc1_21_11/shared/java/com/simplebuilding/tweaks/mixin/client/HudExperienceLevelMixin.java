package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.client.SpawnElytraHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mit Spawn-Elytra keine XP-Stufe ueber der Leiste, dort steht der Timer (Simple Tweaks: InGameHudLevelMixin).
 *
 * <p>Zwei Ziele, weil NeoForge den Aufruf verlegt: Vanilla/Fabric zeichnen die Stufe in
 * {@code renderHotbarAndDecorations}, NeoForge hat {@code Gui} in GUI-Layer zerlegt und ruft
 * denselben {@code ContextualBarRenderer.renderExperienceLevel} aus der privaten Layer-Methode
 * {@code renderExperienceLevel}. Auf jedem Loader existiert genau eine der beiden Methoden.
 */
@Mixin(Gui.class)
public abstract class HudExperienceLevelMixin {
    @Redirect(method = {"renderHotbarAndDecorations", "renderExperienceLevel"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"))
    private void simplebuilding$hideLevel(GuiGraphics graphics, Font font, int level) {
        if (SpawnElytraHud.wearsSpawnElytra(Minecraft.getInstance().player)) {
            return;
        }
        ContextualBarRenderer.renderExperienceLevel(graphics, font, level);
    }
}
