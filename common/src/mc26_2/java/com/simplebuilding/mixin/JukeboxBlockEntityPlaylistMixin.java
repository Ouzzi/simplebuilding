package com.simplebuilding.mixin;

import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** The chest playlist is only on the 26.3 line until the port run (McVersion.JUKEBOX_CHEST_PLAYLIST). */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityPlaylistMixin {}
