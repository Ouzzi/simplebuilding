package com.simplebuilding.mixin;

import com.simplebuilding.util.DynamicLightHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Radiance: Lichtblock eines entfernten Traegers (Staender, Rahmen, Spieler) aufraeumen. */
@Mixin(Entity.class)
public abstract class EntityLightRemovalMixin {

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void simplebuilding$removeOwnedLight(Entity.RemovalReason reason, CallbackInfo ci) {
        DynamicLightHandler.onEntityRemoved((Entity) (Object) this, reason);
    }
}
