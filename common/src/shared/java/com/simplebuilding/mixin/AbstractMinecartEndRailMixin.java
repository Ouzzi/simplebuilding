package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.EndRailPhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raised top speed on flat Astral rails (docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md): {@code max(vanilla, configured)}
 * while the cart stands on one, vanilla's everywhere else - so a fast cart meets the normal limit again on curves, slopes
 * and plain rails. Never lowers vanilla's value (an experimental game rule set higher stays).
 */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartEndRailMixin {
    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$astralRailTopSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        double raised = EndRailPhysics.raisedMaxSpeed((AbstractMinecart) (Object) this);
        if (raised > cir.getReturnValueD()) cir.setReturnValue(raised);
    }
}
