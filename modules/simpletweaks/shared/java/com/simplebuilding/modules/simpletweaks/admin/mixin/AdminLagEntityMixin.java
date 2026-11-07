package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.lag.LagFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lag finder: time per entity tick (hook). */
@Mixin(ServerLevel.class)
public abstract class AdminLagEntityMixin {
    @Unique private long simpletweaks$start;
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void simpletweaks$start(Entity e, CallbackInfo ci) { if (LagFinder.sampling) simpletweaks$start = System.nanoTime(); }
    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void simpletweaks$end(Entity e, CallbackInfo ci) { if (LagFinder.sampling && simpletweaks$start != 0) { LagFinder.entity(e, System.nanoTime() - simpletweaks$start); simpletweaks$start = 0; } }
}
