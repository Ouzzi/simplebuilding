package com.simpleriding.mixin;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin({AbstractHorse.class, AbstractNautilus.class})
public abstract class RidingJumpInputMixin {
    @ModifyVariable(method="onPlayerJump", at=@At("HEAD"), argsOnly=true)
    private int simpleriding$charge(int charge) { return Math.max(0, Math.min(100, charge)); }
}
