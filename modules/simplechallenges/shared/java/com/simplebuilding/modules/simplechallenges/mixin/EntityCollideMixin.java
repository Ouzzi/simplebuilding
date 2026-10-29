package com.simplebuilding.modules.simplechallenges.mixin;

import com.simplebuilding.modules.simplechallenges.core.Enforcement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unlock borders (S1): the move Vanilla allowed is limited again so no entity enters a locked cell. Inert without a chosen challenge ({@code Gate}). */
@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void simplechallenges$limit(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Vec3 allowed = cir.getReturnValue();
        Vec3 limited = Enforcement.clampMove((Entity) (Object) this, allowed);
        if (limited != allowed) cir.setReturnValue(limited);
    }
}
