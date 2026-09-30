package com.simplefun.mixin.fabric;

import com.simplefun.entity.PiggyTracked;
import com.simplefun.registry.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Syncs a player's piggy state to all tracking clients via synced entity data
 * (player status effects are not sent to remote trackers in vanilla).
 *
 * Fabric only: adding a synced data id to Player via mixin breaks NeoForge's registry init.
 * NeoForge broadcasts the same state through a synced data attachment instead
 * (see SimplefunAttachments), so remote players show the pig head on both loaders.
 */
@Mixin(Player.class)
public abstract class PlayerEntityMixin implements PiggyTracked {

    @Unique
    private static final EntityDataAccessor<Boolean> SIMPLEFUN_PIGGY =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void simplefun$initPiggyData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SIMPLEFUN_PIGGY, false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplefun$syncPiggy(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            boolean piggy = self.hasEffect(ModEffects.holder());
            if (self.getEntityData().get(SIMPLEFUN_PIGGY) != piggy) {
                self.getEntityData().set(SIMPLEFUN_PIGGY, piggy);
            }
        }
    }

    @Override
    public boolean simplefun$isPiggyTracked() {
        return ((LivingEntity) (Object) this).getEntityData().get(SIMPLEFUN_PIGGY);
    }
}
