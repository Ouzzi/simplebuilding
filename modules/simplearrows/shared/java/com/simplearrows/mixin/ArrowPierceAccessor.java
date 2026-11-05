package com.simplearrows.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Netherite tips and diamond rod shafts pierce one more target ({@code CraftedArrow}): Vanilla's setter is private. */
@Mixin(AbstractArrow.class)
public interface ArrowPierceAccessor {
    @Invoker("setPierceLevel")
    void simplearrows$setPierceLevel(byte level);
}
