package com.simplebuilding.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Die erste Haelfte einer Doppeltruhe: {@code TieredChests#chestBehind} erkennt daran eine
 * Mod-Truhe hinter Vanillas {@code CompoundContainer} (fuer die Stapelgrenzen der Trichter).
 */
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {
    @Accessor("container1")
    Container simplebuilding$first();
}
