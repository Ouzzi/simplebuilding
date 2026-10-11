package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class HowlerRenderState extends LivingEntityRenderState {
    /** 0 closed .. 1 wide open (bite wind-up, shriek; half open while hungry). */
    public float jawOpen;
    /** 0 still under ground .. 1 fully emerged. */
    public float emerge = 1.0f;
    /** 0 .. 1 while shrieking (arms spread, head thrown back). */
    public float shriek;
}
