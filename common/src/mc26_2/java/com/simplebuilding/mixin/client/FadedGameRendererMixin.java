package com.simplebuilding.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** The perception effects (queue N20) are 26.3 only (McVersion.BREWING_EFFECTS). */
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class FadedGameRendererMixin {}
