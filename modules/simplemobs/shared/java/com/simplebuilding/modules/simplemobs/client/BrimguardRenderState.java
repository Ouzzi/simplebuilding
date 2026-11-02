package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class BrimguardRenderState extends LivingEntityRenderState {
    /** 0 .. 1 eased: ducked under the hat (heights x 2/3 at 1). */
    public float duck;
    /** 0 .. 1 eased: the hat lifted for venting. */
    public float vent;
    /** 0 .. 1 hat slam wind-up. */
    public float slam;
    /** 0 .. 1 charge scrape and run. */
    public float charge;
}
