package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.EndRailPhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Astral/Nihil rails (docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md): once per tick, before vanilla moves a minecart
 * along its track, the End rail under it boosts or brakes the delta movement. Both behaviours: the default one and the
 * experimental "minecart improvements" (which applies its own boost/halt and speed clamp right after, once per tick).
 * Extends MinecartBehavior only to reach the protected {@code minecart} field and the behaviour's own top speed.
 */
@Mixin({OldMinecartBehavior.class, NewMinecartBehavior.class})
public abstract class MinecartBehaviorEndRailMixin extends MinecartBehavior {
    protected MinecartBehaviorEndRailMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void simplebuilding$endRail(ServerLevel level, CallbackInfo ci) {
        EndRailPhysics.beforeTrackMove(this.minecart, this.getMaxSpeed(level), (Object) this instanceof OldMinecartBehavior);
    }
}
