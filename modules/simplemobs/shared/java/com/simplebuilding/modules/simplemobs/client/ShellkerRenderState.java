package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.state.ShulkerRenderState;

/** Shulker state plus how far the Shellker's slit is open (0..1), for the eye glow. */
public class ShellkerRenderState extends ShulkerRenderState {
    public float openness;
}
