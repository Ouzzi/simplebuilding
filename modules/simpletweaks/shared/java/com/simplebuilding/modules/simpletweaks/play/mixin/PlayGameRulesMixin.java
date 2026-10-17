package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.grief.MobGriefing;
import net.minecraft.world.level.gamerules.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mob griefing per mob (T8): the Vanilla rule reads false for listed mobs while they tick; never true where Vanilla says false. */
@Mixin(GameRules.class)
public abstract class PlayGameRulesMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$playGrief(GameRule<?> rule, CallbackInfoReturnable<Object> cir) {
        if (rule == GameRules.MOB_GRIEFING && Boolean.TRUE.equals(cir.getReturnValue()) && !MobGriefing.allowed()) cir.setReturnValue(Boolean.FALSE);
    }
}
