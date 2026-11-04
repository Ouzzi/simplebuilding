package com.simplelib.mixin.client;

import com.simplelib.api.client.FreeLook;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Free look zoom (DW04A-2): while free look is on, the mouse wheel zooms instead of scrolling the hotbar. */
@Mixin(MouseHandler.class)
public abstract class FreeLookZoomMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void simplelib$freeLookWheel(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (FreeLook.scroll(yoffset)) ci.cancel();
    }
}
