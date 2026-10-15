package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.FearMobs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Shivering on the client: the view wavers around where the player aims, so bows and crossbows lose precision.
 * Level I up to about 0.6 degrees, level II about 1.8 degrees: a slow sway plus a small fast tremble, applied as the
 * change since the last frame, so it never drifts away from where the player aims.
 * Simple Building's own Shivering also moves the crosshair; this adds the aim.
 */
public final class AimSway {
    private static double lastX, lastY;
    private static boolean active;

    private AimSway() {}

    /** Sway offset in degrees (yaw, pitch) for a level (0 = I) at time t seconds. */
    public static double[] offset(int amplifier, double t) {
        double amp = amplifier <= 0 ? 0.6 : amplifier >= 4 ? 4.2 : Math.min(3.0, 1.8 + 0.6 * (amplifier - 1));
        double x = amp * (0.5 * Math.sin(t * 2.3) + 0.3 * Math.sin(t * 3.9 + 1.3) + 0.2 * Math.sin(t * 17.0 + 0.7));
        double y = amp * 0.7 * (0.5 * Math.sin(t * 2.9 + 2.1) + 0.3 * Math.sin(t * 4.3 + 0.4) + 0.2 * Math.sin(t * 19.0 + 2.2));
        return new double[] {x, y};
    }

    public static void apply() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        Holder<MobEffect> shivering = player == null ? null : FearMobs.shivering();
        MobEffectInstance inst = shivering == null ? null : player.getEffect(shivering);
        if (inst == null || mc.gui.screen() != null || mc.isPaused()) {
            active = false;
            return;
        }
        double[] o = offset(inst.getAmplifier(), (Util.getMillis() % 10_000_000L) / 1000.0);
        if (active) player.turn((o[0] - lastX) / 0.15, (o[1] - lastY) / 0.15);
        lastX = o[0];
        lastY = o[1];
        active = true;
    }
}
