package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.HowlerEntity;
import com.simplebuilding.modules.simplemobs.HowlerLogic;
import com.simplebuilding.modules.simplemobs.MobsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Camera shake of the Howler's shockwave and shriek wind-up (owner H5). The view of the local player is nudged by a small
 * oscillating offset; each Howler remembers what it applied and takes it back with the next nudge, so the aim returns.
 * The client option "reduceMotion" in simplemobs-client.json (default off = shake on) turns it off.
 */
public final class HowlerShake {
    private HowlerShake() {}

    public static void tick(HowlerEntity howler) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        float amp = MobsConfig.reduceMotion ? 0.0f : HowlerLogic.shakeAmplitude(howler.attack(), howler.distanceTo(p));
        float y = amp * (float) Math.sin(howler.tickCount * 2.4), x = amp * 0.6f * (float) Math.cos(howler.tickCount * 3.1);
        float dy = y - howler.shakeApplied[0], dx = x - howler.shakeApplied[1];
        if (dy == 0.0f && dx == 0.0f) return;
        p.setYRot(p.getYRot() + dy);
        p.yRotO += dy;
        p.setXRot(p.getXRot() + dx);
        p.xRotO += dx;
        howler.shakeApplied[0] = y;
        howler.shakeApplied[1] = x;
    }
}
