package com.simplebuilding.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** 26.2 twin of the 26.3 extra-head visibility mixin (owner N49): empty until the port run (no MOB_VISIBILITY on 26.2). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityExtraHeadMixin {
}
