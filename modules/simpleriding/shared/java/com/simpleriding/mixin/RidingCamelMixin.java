package com.simpleriding.mixin;

import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla executes the rider's dash on the client; retain its cooldown on the server too. */
@Mixin(Camel.class)
public abstract class RidingCamelMixin {
    @Shadow private int dashCooldown;
    @Inject(method="handleStartJump", at=@At("RETURN"))
    private void simpleriding$cooldown(int charge, CallbackInfo ci) {
        if (!((Camel)(Object)this).level().isClientSide()) dashCooldown=55;
    }
}
