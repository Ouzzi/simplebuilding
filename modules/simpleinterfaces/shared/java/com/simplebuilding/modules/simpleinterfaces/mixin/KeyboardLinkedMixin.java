package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simpleinterfaces.client.LinkedHud;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Linked GUIs: Escape in the world ends the container mark first; the pause menu opens on the next press. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardLinkedMixin {
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V"))
    private void qol$escape(Minecraft minecraft, boolean suppressPauseMenu, Operation<Void> original) {
        if (!suppressPauseMenu && minecraft.gui.screen() == null && LinkedHud.cancel()) return;
        original.call(minecraft, suppressPauseMenu);
    }
}
