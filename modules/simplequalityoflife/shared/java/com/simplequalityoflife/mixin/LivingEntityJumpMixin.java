package com.simplequalityoflife.mixin;

import com.simplequalityoflife.util.CrawlAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void stopCrawlOnJump(CallbackInfo ci) {
        // "this" ist hier LivingEntity, also casten wir
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof Player player) {
            // Wir nutzen unser Interface
            if (player instanceof CrawlAccessor crawler) {
                if (crawler.simpleQualityOfLife$isCrawling()) {
                    crawler.simpleQualityOfLife$setCrawling(false);
                }
            }
        }
    }
}
