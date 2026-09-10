package com.simplebuilding.neoforge.clienttest.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.simplebuilding.neoforge.clienttest.HeldKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a key the test harness holds be visible to code that asks the window directly.
 *
 * <p>{@code InputConstants.isKeyDown} calls {@code glfwGetKey}, which knows nothing about a key
 * mapping the harness set. The mod's {@code MouseMixin} asks it for Control and Alt, so without
 * this a scroll-with-modifier test would scroll unmodified and report the mod as broken.
 *
 * <p>Only ever true for keys the harness is actually holding - a real key press still answers the
 * real way, and with nothing held the injection returns immediately. The test mod only exists in
 * the client test run, so nothing of this reaches a shipped jar.
 */
@Mixin(InputConstants.class)
public class InputConstantsMixin {

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$reportHarnessHeldKeys(Window window, int key,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (HeldKeys.anyHeld() && HeldKeys.isHeld(key)) {
            cir.setReturnValue(true);
        }
    }
}
