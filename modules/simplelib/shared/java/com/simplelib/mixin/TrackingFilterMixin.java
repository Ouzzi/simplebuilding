package com.simplelib.mixin;

import com.simplelib.api.TrackingFilters;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BACK22 core §3.4: {@link TrackingFilters} may hide an entity from a player (asked by the entity tracker for every pairing). */
@Mixin(Entity.class)
public abstract class TrackingFilterMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void simplelib$trackingFilter(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!TrackingFilters.allows(player, (Entity) (Object) this)) cir.setReturnValue(false);
    }
}
