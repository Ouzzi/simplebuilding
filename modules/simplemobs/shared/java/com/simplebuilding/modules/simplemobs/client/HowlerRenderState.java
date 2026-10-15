package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class HowlerRenderState extends LivingEntityRenderState {
    /** 0 closed .. 1 wide open (bite wind-up, shriek; half open while hungry). */
    public float jawOpen;
    /** 0 still under ground .. 1 fully emerged (eased with holds, see HowlerLogic.emergeHeight). */
    public float emerge = 1.0f;
    /** 0 .. 1 while shrieking (arms spread, head thrown back). */
    public float shriek;
    /** 0 .. 1 right after it heard a vibration (antennae twitch and glow, fading slowly). */
    public float tendril;
    /** Glow brightness in the rhythm of the heartbeat (never below 0.45), and zero once it dies. */
    public float heartGlow = 1.0f;
    /** Per-mob phase offset so several Howlers do not idle in step. */
    public float seed;
    /** Head twitch while it holds still under ground. */
    public float emergeTwitch;
    /** Running charge (not the crouch): body forward, head low, arms back. */
    public boolean charging;
    /** Ticks since it hit the wall (stagger), -1 when not staggered. */
    public float staggerAge = -1.0f;
    /** 0 .. 1 over the 2 s death animation. */
    public float death;
}
