package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cloak of deception: mobs pick a cloaked player as combat target only close up (or never, if he wears their shape). */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsCloakMixin {
    @Shadow @Final private boolean isCombat;

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void simplemobs$cloak(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (isCombat && cir.getReturnValueZ() && targeter instanceof Mob && target instanceof Player p && Disguise.hides(targeter, p)) cir.setReturnValue(false);
    }
}
