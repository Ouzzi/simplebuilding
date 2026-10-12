package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DreadRenderState extends LivingEntityRenderState {
    /** 0 .. 1 while the mace is raised for a swing (1 = top, then it strikes). */
    public float swing;
    /** 0 .. 1 while both arms lift the mace for the stomp. */
    public float stomp;
    /** 0 still in the ground .. 1 fully risen. */
    public float rise = 1.0f;
    /** In bright light: dimmer soul flames. */
    public boolean weak;
}
