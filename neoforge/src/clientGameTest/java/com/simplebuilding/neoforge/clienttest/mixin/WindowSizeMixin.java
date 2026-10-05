package com.simplebuilding.neoforge.clienttest.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pins the test client's window and framebuffer to the size the run asked for
 * ({@code --width 854 --height 480}), whatever the desktop does with the real window.
 *
 * <p>Not a nicety. The renderer tests measure pixels, and their geometry depends on the frame's
 * size and aspect ratio. On MC 26.3 the SDL window is placed by the desktop after it was created:
 * one run got 1902x1112 (maximised), the night run of 2026-10-05 got 627x1112 (snapped into a third
 * of the screen, portrait). In that frame the GUI scale dropped to 1 (the hopper whitelist icon drew
 * 128 instead of 512 pixels), the first person hand left the picture (the chisel tilt "drew nothing")
 * and the wand ghosts touched the frame edge - four failures that had nothing to do with the mod.
 *
 * <p>This is exactly what Fabric's client test framework does in its own {@code WindowMixin}: keep
 * width, height and framebuffer at the requested size and drop the resize callbacks. Screenshots
 * read the main render target, which is created from these values; the OpenGL blit to a window of
 * another size only crops or letterboxes what the desktop shows. Test source set only, never in a
 * shipped jar. The handlers take no target arguments, so the GLFW callbacks of 26.2 and the SDL
 * ones of 26.3 match alike.
 */
@Mixin(Window.class)
public abstract class WindowSizeMixin {

    @Shadow private int width;
    @Shadow private int height;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;

    /** windowedWidth/Height still hold the requested size here: only the resize callbacks change them. */
    @Inject(method = "<init>*", at = @At("TAIL"))
    private void simplebuilding$pinTheRequestedSize(CallbackInfo ci) {
        this.width = this.framebufferWidth = this.windowedWidth;
        this.height = this.framebufferHeight = this.windowedHeight;
    }

    @Inject(method = {"onResize", "onFramebufferResize", "refreshFramebufferSize"}, at = @At("HEAD"), cancellable = true)
    private void simplebuilding$ignoreTheDesktopsSize(CallbackInfo ci) {
        ci.cancel();
    }
}
