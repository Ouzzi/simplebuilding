package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.HammockTime;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md): a resting player's sleep counter stays at
 * {@link HammockTime#DOZE_TICKS}, on both sides. Server: never "sleeping long enough", so hammocks can never trigger
 * vanilla's skip to the next morning. Client: the sleep overlay only dims the screen a little.
 */
@Mixin(Player.class)
public abstract class HammockPlayerMixin {
    @Shadow
    private int sleepCounter;

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$hammockDoze(CallbackInfo ci) {
        if (this.sleepCounter > HammockTime.DOZE_TICKS && HammockTime.inHammock((Player) (Object) this)) {
            this.sleepCounter = HammockTime.DOZE_TICKS;
        }
    }
}
