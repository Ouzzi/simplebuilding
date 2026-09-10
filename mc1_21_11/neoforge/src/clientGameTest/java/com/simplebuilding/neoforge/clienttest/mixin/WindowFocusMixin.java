package com.simplebuilding.neoforge.clienttest.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the test client believing its window is focused, whatever the desktop does.
 *
 * <p>Not a nicety. A run on a machine that had just come back from standby lost the window's focus
 * mid-run and everything after it failed for one reason wearing seven costumes: {@code Minecraft}
 * opens the pause screen half a second after focus is gone ({@code pauseIfInactive}), the mouse
 * cannot be grabbed while the window is inactive ({@code MouseHandler.grabMouse} checks
 * {@code isWindowActive} first), and vanilla only continues an attack while the mouse IS grabbed.
 * The trace read "attack binding down on 0 ticks, mouse grabbed false, window active false, screen
 * PauseScreen" - and the mod was about to be blamed for a mining preview it never got to draw.
 *
 * <p>The three GLFW callbacks are swallowed at the head, so {@code Window.focused} stays at the
 * {@code true} it starts with. This is exactly what Fabric's client test framework does in its own
 * {@code WindowMixin}; the two harnesses now agree on this too. Test source set only, never in a
 * shipped jar.
 */
@Mixin(Window.class)
public abstract class WindowFocusMixin {

    @Inject(method = {"onFocus", "onEnter", "onIconify"}, at = @At("HEAD"), cancellable = true)
    private void simplebuilding$keepTheWindowActive(CallbackInfo ci) {
        ci.cancel();
    }
}
