package com.simplebuilding.modules.simpleoptserver.mixin.activation_range;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simpleoptserver.ActivationRange;
import com.simplebuilding.modules.simpleoptserver.ServerOpt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** S5b: the full tick of a far, idle mob runs only every interval-th tick (ActivationRange); applied only when the area is on. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void simpleoptserver$activationRange(Entity entity, Operation<Void> original) {
        if (ServerOpt.active(ServerOpt.ACTIVATION_RANGE) && ActivationRange.skip(entity, (ServerLevel) (Object) this, ServerOpt.activation())) return;
        original.call(entity);
    }
}
