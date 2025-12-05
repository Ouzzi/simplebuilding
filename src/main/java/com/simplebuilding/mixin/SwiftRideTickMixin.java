package com.simplebuilding.mixin;

import com.simplebuilding.util.SwiftRideHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SwiftRideTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.hasControllingPassenger() && entity.getControllingPassenger() instanceof PlayerEntity) {SwiftRideHelper.applyBoost(entity);}
        else {SwiftRideHelper.removeBoost(entity);}
    }
}