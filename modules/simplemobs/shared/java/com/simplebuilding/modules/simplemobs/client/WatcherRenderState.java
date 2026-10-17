package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WatcherRenderState extends LivingEntityRenderState {
    /** 0 .. 1 arms reaching out (grip wind-up and hold, claw). */
    public float reach;
    /** 0 .. 1 head thrown back, arms spread (scream wind-up). */
    public float scream;
    /** 0 .. 1 eased: standing close behind the local player, arms stretched over the player's shoulders (jump scare). */
    public float shoulder;
    /** 0 .. 1 eased: watched or frozen; the sway fades out instead of snapping to zero. */
    public float calm;
    /** Index into FearLogic.WATCHER_POSES: the crooked pose it holds while watched or frozen. */
    public int pose;
    /** Frozen by a spyglass look: pale, eyes dim. */
    public boolean frozen;
    /** Someone looks at it: it holds its pose, the eyes narrow. */
    public boolean watched;
    /** Has a target, was hit or acts: only then do the eyes glow (N50 camouflage). */
    public boolean triggered;
}
