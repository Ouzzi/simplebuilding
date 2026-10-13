package com.simplemobheads.mixin;

import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wolf head: an untamed wolf never targets the wearer (a tamed one follows its owner as always). */
@Mixin(Mob.class)
public abstract class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$wolvesSpareWolfHeads(LivingEntity target, CallbackInfo ci) {
        if (target != null && (Object) this instanceof Wolf wolf && !wolf.isTame() && HeadAbilities.wolvesSpare(target)) {
            ci.cancel();
        }
    }
}
