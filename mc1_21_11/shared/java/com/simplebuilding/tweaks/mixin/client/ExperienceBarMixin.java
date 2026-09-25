package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.client.SpawnElytraHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mit Spawn-Elytra zeigt die XP-Leiste Boosts und Flugzeit (Simple Tweaks: InGameHudMixin). */
@Mixin(ExperienceBarRenderer.class)
public class ExperienceBarMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$spawnElytraBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SpawnElytraHud.render(graphics)) {
            ci.cancel();
        }
    }
}
