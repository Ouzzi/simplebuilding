package com.simplequalityoflife.mixin;

import com.simplequalityoflife.util.CrawlAccessor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityCrawlMixin extends LivingEntity implements CrawlAccessor {

    @Unique private boolean simplequalityoflife$crawling;

    protected PlayerEntityCrawlMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }



    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void forceCrawlPose(CallbackInfo ci) {
        if (!this.level().isClientSide() && !com.simplequalityoflife.Simplequalityoflife.getConfig().qOL.enableManualCrawl && simplequalityoflife$crawling) simpleQualityOfLife$setCrawling(false);
        if (simplequalityoflife$crawling) {
            this.setPose(Pose.SWIMMING);
            ci.cancel();
        }
    }

    // -------------------------------------

    @Override
    public void simpleQualityOfLife$setCrawling(boolean crawling) {
        if (simplequalityoflife$crawling == crawling) return;
        simplequalityoflife$crawling = crawling;
        if (crawling) {
            this.setPose(Pose.SWIMMING);
        }
        this.refreshDimensions();
        if (!this.level().isClientSide()) com.simplequalityoflife.Simplequalityoflife.crawlSync.accept((Player)(Object)this, crawling);
    }

    @Override
    public boolean simpleQualityOfLife$isCrawling() {
        return simplequalityoflife$crawling;
    }
}
