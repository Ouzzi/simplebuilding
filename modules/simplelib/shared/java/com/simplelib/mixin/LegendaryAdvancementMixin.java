package com.simplelib.mixin;

import com.simplelib.api.LegendaryMoments;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legendary moments (SP22-2): Vanilla grants an advancement's rewards exactly once, when this award makes it done - the
 * place to celebrate the legendary ones ({@link LegendaryMoments#advancementDone}).
 */
@Mixin(PlayerAdvancements.class)
public abstract class LegendaryAdvancementMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void simplelib$legendary(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
        LegendaryMoments.advancementDone(this.player, holder);
    }
}
