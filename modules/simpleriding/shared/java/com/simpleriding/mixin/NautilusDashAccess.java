package com.simpleriding.mixin;

import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractNautilus.class)
public interface NautilusDashAccess {
    @Invoker("executeRidersJump") void simpleriding$executeDash(float charge, Player rider);
}
