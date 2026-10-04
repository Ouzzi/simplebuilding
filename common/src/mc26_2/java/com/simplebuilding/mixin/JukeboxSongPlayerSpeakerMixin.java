package com.simplebuilding.mixin;

import net.minecraft.world.item.JukeboxSongPlayer;
import org.spongepowered.asm.mixin.Mixin;

/** The speakers are only available on the 26.3 line. */
@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerSpeakerMixin {}
