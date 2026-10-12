package com.simplebuilding.modules.simpleend.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The registry behind a Vanilla biome holder, so the End biome source can look up the Simple End biomes. */
@Mixin(Holder.Reference.class)
public interface HolderReferenceAccessor {
    @Accessor("owner")
    HolderOwner<?> simpleend$owner();
}
