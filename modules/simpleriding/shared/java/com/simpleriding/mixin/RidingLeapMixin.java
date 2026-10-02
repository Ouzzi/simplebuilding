package com.simpleriding.mixin;

import com.simpleriding.RidingEffects;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The server starts the rider's jump here (after vanilla's jump sound); Leaping adds a small puff. */
@Mixin(AbstractHorse.class)
public abstract class RidingLeapMixin {
    @Inject(method="handleStartJump", at=@At("RETURN"))
    private void simpleriding$leapCue(int charge, CallbackInfo ci) {
        RidingEffects.leapCue((AbstractHorse)(Object)this, charge);
    }
}
