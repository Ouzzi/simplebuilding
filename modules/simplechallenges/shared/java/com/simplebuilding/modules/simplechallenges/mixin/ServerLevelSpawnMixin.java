package com.simplebuilding.modules.simplechallenges.mixin;

import com.simplebuilding.modules.simplechallenges.core.Enforcement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unlock borders: no new mob appears inside a locked cell (natural spawns, spawners, eggs). Inert without a chosen challenge ({@code Gate}). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSpawnMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void simplechallenges$noSpawnInLockedCells(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Enforcement.refusesSpawn((ServerLevel) (Object) this, entity)) cir.setReturnValue(false);
    }
}
