package com.simplebuilding.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;

/** The Silent Dandelion is only available on the 26.3 line. */
@Mixin(Mob.class)
public abstract class SilentDandelionMixin {}
