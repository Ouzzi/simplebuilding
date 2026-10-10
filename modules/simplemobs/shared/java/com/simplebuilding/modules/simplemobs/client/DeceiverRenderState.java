package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class DeceiverRenderState extends LivingEntityRenderState {
    public boolean armored;
    public boolean hostile;
    public final AnimationState idle = new AnimationState();
    public final AnimationState summon = new AnimationState();
    public final AnimationState teleport = new AnimationState();
    public final AnimationState drink = new AnimationState();
    public final AnimationState unmask = new AnimationState();
}
