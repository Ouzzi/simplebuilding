package com.simplebuilding.mixin;

import com.simplebuilding.woodwork.HollowLogCrawl;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hollow logs (docs/ai/PLAN-HOLZWERK-2026-10-09.md): sneaking into a lying tube makes the player crawl. */
@Mixin(Player.class)
public abstract class HollowLogCrawlMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$crawlIntoHollowLog(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (HollowLogCrawl.shouldCrawl(self)) {
            self.setPose(Pose.SWIMMING);
            ci.cancel();
        }
    }
}
