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

/** Mit Spawn-Elytra keine XP-Stufe ueber der Leiste, dort steht der Timer (Simple Tweaks: InGameHudLevelMixin). */
@Mixin(Gui.class)
public abstract class HudExperienceLevelMixin {
    @Redirect(method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"))
    private void simplebuilding$hideLevel(GuiGraphics graphics, Font font, int level) {
        if (SpawnElytraHud.wearsSpawnElytra(Minecraft.getInstance().player)) {
            return;
        }
        ContextualBarRenderer.renderExperienceLevel(graphics, font, level);
    }
}
