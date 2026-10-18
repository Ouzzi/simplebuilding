package com.simplebuilding.modules.simpletweaks.protect.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Hidden health: the data slot of a living entity's health. */
@Mixin(LivingEntity.class)
public interface ProtectLivingAccessor {
    @Accessor("DATA_HEALTH_ID") static EntityDataAccessor<Float> simpletweaks$healthId() { throw new AssertionError(); }
}
