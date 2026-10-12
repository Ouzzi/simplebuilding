package com.simplebuilding.modules.simpleenddragonfight.mixin;

import com.simplebuilding.modules.simpleenddragonfight.DragonFightServer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Managed fight dragon only (factors are 1 otherwise): faster flight (forward thrust and turn rate in the flying
 * phases) and, above the 1024 max_health cap, damage scaled down so the dragon lasts its full player-scaled health.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE", target = "moveRelative(FLnet/minecraft/world/phys/Vec3;)V"), index = 0)
    private float simpleenddragonfight$thrust(float speed) {
        return speed * DragonFightServer.speedFactor((EnderDragon) (Object) this);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getTurnSpeed()F"))
    private float simpleenddragonfight$turn(DragonPhaseInstance phase) {
        return phase.getTurnSpeed() * DragonFightServer.speedFactor((EnderDragon) (Object) this);
    }

    @ModifyVariable(method = "reallyHurt", at = @At("HEAD"), argsOnly = true)
    private float simpleenddragonfight$damage(float damage) {
        return damage * DragonFightServer.damageFactor((EnderDragon) (Object) this);
    }
}
