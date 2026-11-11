package com.simplelib.api.client;

import com.simplelib.net.PortalPulsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The fog pulse after going through a portal door (BACK22 core §5.5, stage S1): the screen is covered in the target's fog colour
 * and clears within the pulse time. Drawn as a HUD fill, so it works with any renderer (Sodium, shader packs).
 */
public final class PortalPulse {
    private static int color;
    private static long start, length;

    private PortalPulse() {}

    public static void receive(PortalPulsePayload payload) {
        color = payload.color() & 0xFFFFFF;
        length = Math.max(1, payload.ticks()) * 50_000_000L;
        start = System.nanoTime();
    }

    /** Leaving a world or server: a running pulse ends (REV2-B-11). */
    public static void reset() {
        length = 0;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (length == 0) return;
        int a = PortalPulsePayload.alpha(System.nanoTime() - start, length);
        if (a <= 0) {
            length = 0;
            return;
        }
        var window = Minecraft.getInstance().getWindow();
        graphics.fill(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), a << 24 | color);
    }
}
