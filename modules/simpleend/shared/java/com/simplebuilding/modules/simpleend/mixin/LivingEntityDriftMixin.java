package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.DriftingEffect;
import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drifting (B23-M14): the random pull of {@link DriftingEffect} is added right before the entity moves. {@code travel}
 * runs only on the side that controls the entity, so a player's pull happens on his own client and a mob's on the server.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDriftMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void simpleend$drift(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        var drifting = EndRegistry.drifting();
        if (drifting != null && SimpleEnd.on() && self.hasEffect(drifting)) DriftingEffect.drift(self);
    }
}
