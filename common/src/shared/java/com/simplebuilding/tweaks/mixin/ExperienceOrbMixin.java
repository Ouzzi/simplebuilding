package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.xp.XpClumping;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** XP-Kugeln verklumpen und sofort aufheben (Simple Tweaks: ExperienceOrbMixin, Config enableXpClumps). */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void simplebuilding$instantPickup(Player player, CallbackInfo ci) {
        if (SimpleTweaks.config().optimization.enableXpClumps) {
            player.takeXpDelay = 0;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void simplebuilding$clumpOrbs(CallbackInfo ci) {
        XpClumping.tick((ExperienceOrb) (Object) this);
    }
}
