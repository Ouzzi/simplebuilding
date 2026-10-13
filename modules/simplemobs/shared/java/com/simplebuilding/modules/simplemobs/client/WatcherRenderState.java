package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WatcherRenderState extends LivingEntityRenderState {
    /** 0 .. 1 arms reaching out (grip wind-up and hold, claw). */
    public float reach;
    /** 0 .. 1 head thrown back, arms spread (scream wind-up). */
    public float scream;
    /** Frozen by a spyglass look: pale, eyes dim. */
    public boolean frozen;
    /** Someone looks at it: it holds its pose, the eyes narrow. */
    public boolean watched;
    /** Has a target, was hit or acts: only then do the eyes glow (N50 camouflage). */
    public boolean triggered;
}
