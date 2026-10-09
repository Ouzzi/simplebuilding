package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3 reads keys from the SDL keyboard state by scancode and no longer treats -1 as "unbound" (UNKNOWN is 0).
 * A key saved as -1 by an older build (still in options.txt) crashed the client when the loading screen closed;
 * a negative key is never down. 26.2 has no isKeyDown(int), so the injection is optional there.
 */
@Mixin(InputConstants.class)
public abstract class InputConstantsGuardMixin {
    @Inject(method = "isKeyDown(I)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void simplebuilding$negativeKeyIsUp(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key < 0) cir.setReturnValue(false);
    }
}
