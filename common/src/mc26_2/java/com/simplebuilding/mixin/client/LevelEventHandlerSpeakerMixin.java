package com.simplebuilding.mixin.client;

import net.minecraft.client.renderer.LevelEventHandler;
import org.spongepowered.asm.mixin.Mixin;

/** The speakers are only available on the 26.3 line. */
@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerSpeakerMixin {}
