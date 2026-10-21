package com.simplebeacons.mixin;

import com.simplebeacons.BeaconsRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owner 21.10.: next to its Nether Star the Vanilla Wither drops a Strong Upgrade Shard ({@link BeaconsRegistry#witherDied}). */
@Mixin(WitherBoss.class)
public abstract class WitherShardMixin {
    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void simplebeacons$shard(ServerLevel level, DamageSource source, boolean killedByPlayer, CallbackInfo ci) {
        BeaconsRegistry.witherDied((WitherBoss) (Object) this, level);
    }
}
