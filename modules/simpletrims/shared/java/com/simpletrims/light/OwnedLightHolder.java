package com.simpletrims.light;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Armor stands, mobs and item frames remember the light block they set (saved with the entity, key {@code SimpleTrimsOwnedLight}). */
public interface OwnedLightHolder {
    String SAVE_KEY = "SimpleTrimsOwnedLight";

    @Nullable BlockPos simpletrims$getOwnedLight();

    void simpletrims$setOwnedLight(@Nullable BlockPos pos);
}
