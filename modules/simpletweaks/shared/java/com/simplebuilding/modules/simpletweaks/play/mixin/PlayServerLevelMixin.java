package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.crowd.EntityLimit;
import com.simplebuilding.modules.simpletweaks.play.grief.MobGriefing;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Which entity ticks (mob griefing per mob, T8) and new mobs in crowded chunks (T31). */
@Mixin(ServerLevel.class)
public abstract class PlayServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void simpletweaks$playTicking(Entity entity, CallbackInfo ci) { MobGriefing.enter(entity); }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void simpletweaks$playTicked(Entity entity, CallbackInfo ci) { MobGriefing.exit(); }

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$playCrowd(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!EntityLimit.admit((ServerLevel) (Object) this, entity)) cir.setReturnValue(false);
    }
}
