package com.simpleriding.mixin;

import com.simpleriding.RidingEffects;
import com.simpleriding.Riding;
import com.simpleriding.RidingConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractNautilus.class)
public abstract class RidingNautilusMixin {
    @ModifyVariable(method="executeRidersJump", at=@At("HEAD"), argsOnly=true)
    private float simpleriding$dash(float charge) {
        return RidingEffects.dashScale((LivingEntity)(Object)this, charge);
    }
    @Inject(method="executeRidersJump", at=@At("RETURN"))
    private void simpleriding$impulse(float charge, net.minecraft.world.entity.player.Player rider, CallbackInfo ci) {
        var entity=(LivingEntity)(Object)this;
        if (entity.level().isClientSide()) return;
        var motion=entity.getDeltaMovement();
        double cap=Math.min(3.9,Math.max(.5,RidingConfig.bounded(Riding.CONFIG.safety.movementDistancePerTick,4,4)));
        if (!Double.isFinite(motion.lengthSqr())) entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        else if (motion.length()>cap) entity.setDeltaMovement(motion.normalize().scale(cap));
    }
}
