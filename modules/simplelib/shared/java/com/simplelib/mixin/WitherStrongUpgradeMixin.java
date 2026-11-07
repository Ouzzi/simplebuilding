package com.simplelib.mixin;

import com.simplelib.material.StrongUpgradeDrops;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WF25-10: next to its Nether Star the Vanilla Wither drops a Strong Upgrade shard or template ({@link StrongUpgradeDrops#witherDied}). */
@Mixin(WitherBoss.class)
public abstract class WitherStrongUpgradeMixin {
    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void simplelib$strongUpgrade(ServerLevel level, DamageSource source, boolean killedByPlayer, CallbackInfo ci) {
        StrongUpgradeDrops.witherDied((WitherBoss) (Object) this, level);
    }
}
