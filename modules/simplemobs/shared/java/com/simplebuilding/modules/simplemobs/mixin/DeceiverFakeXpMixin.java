package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.DeceiverEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Owner AUDSB31B F7 = A: the Deceiver's fakes (illusions with one health and no damage) give no experience - no risk-free farm;
 * real summons keep Vanilla's. {@code getExperienceReward} is final (no subclass overrides it) and the fake tag is saved with
 * the mob, so this also holds after a restart.
 */
@Mixin(LivingEntity.class)
public abstract class DeceiverFakeXpMixin {
    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void simplemobs$fakesGiveNoExperience(ServerLevel level, @Nullable Entity killer, CallbackInfoReturnable<Integer> cir) {
        if (((LivingEntity) (Object) this).entityTags().contains(DeceiverEntity.TAG_FAKE)) cir.setReturnValue(0);
    }
}
