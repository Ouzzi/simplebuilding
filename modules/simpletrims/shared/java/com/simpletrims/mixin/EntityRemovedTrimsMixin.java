package com.simpletrims.mixin;

import com.simpletrims.light.TrimLight;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clears the light block of a removed carrier (player for every reason, stands/frames/mobs when destroyed). */
@Mixin(Entity.class)
public abstract class EntityRemovedTrimsMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void simpletrims$removeOwnedLight(Entity.RemovalReason reason, CallbackInfo ci) {
        TrimLight.onEntityRemoved((Entity) (Object) this, reason);
    }
}
