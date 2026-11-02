package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class VoidRunnerRenderState extends LivingEntityRenderState {
    /** 0 .. 1 blade slash after a melee hit. */
    public float strike;
    /** 0 .. 1 lunge telegraph crouch. */
    public float crouch;
    /** 1 while in the air on a lunge. */
    public float lunge;
    /** 1 while staggered by a shield. */
    public float stagger;
}
