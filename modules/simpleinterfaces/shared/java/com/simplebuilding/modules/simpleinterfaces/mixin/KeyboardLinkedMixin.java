package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.LinkedHud;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Linked GUIs: Escape in the world ends the container mark first; the pause menu opens on the next press. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardLinkedMixin {
    @Redirect(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V"))
    private void qol$escape(Minecraft minecraft, boolean suppressPauseMenu) {
        if (!suppressPauseMenu && minecraft.gui.screen() == null && LinkedHud.cancel()) return;
        minecraft.pauseGame(suppressPauseMenu);
    }
}
