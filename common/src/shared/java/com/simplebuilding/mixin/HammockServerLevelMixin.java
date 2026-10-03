package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.HammockTime;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md): speeds the level's clock up while enough players rest
 * ({@link HammockTime#tick}) and keeps vanilla's "x/y players sleeping" action bar quiet for hammocks.
 */
@Mixin(ServerLevel.class)
public abstract class HammockServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$hammockTime(BooleanSupplier haveTime, CallbackInfo ci) {
        HammockTime.tick((ServerLevel) (Object) this);
    }

    @Inject(method = "announceSleepStatus", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$quietHammocks(CallbackInfo ci) {
        if (HammockTime.silenceSleepAnnouncement((ServerLevel) (Object) this)) {
            ci.cancel();
        }
    }
}
