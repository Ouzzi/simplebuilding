package com.simplebuilding.modules.simpleenddragonfight.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DragonmiteRenderState extends LivingEntityRenderState {
    /** Per-mob phase offset (radians) from the entity id, so neighbors do not buzz in step. */
    public float phase;
    /** 0..1..0 over a bite (the swing): the front segments lunge forward and the wings lie back. */
    public float lunge;
}
