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

/** Mit Spawn-Elytra keine XP-Stufe ueber der Leiste, dort steht der Timer (Simple Tweaks: InGameHudLevelMixin). */
@Mixin(Hud.class)
public abstract class HudExperienceLevelMixin {
    @Redirect(method = "extractHotbarAndDecorations",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void simplebuilding$hideLevel(GuiGraphicsExtractor graphics, Font font, int level) {
        if (SpawnElytraHud.wearsSpawnElytra(Minecraft.getInstance().player)) {
            return;
        }
        ContextualBar.extractExperienceLevel(graphics, font, level);
    }
}
