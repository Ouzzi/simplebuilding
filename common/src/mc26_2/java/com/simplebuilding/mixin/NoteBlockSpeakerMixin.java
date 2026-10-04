package com.simplebuilding.mixin;

import net.minecraft.world.level.block.NoteBlock;
import org.spongepowered.asm.mixin.Mixin;

/** The speakers are only available on the 26.3 line. */
@Mixin(NoteBlock.class)
public abstract class NoteBlockSpeakerMixin {}
