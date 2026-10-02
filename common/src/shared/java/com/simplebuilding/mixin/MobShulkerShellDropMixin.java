package com.simplebuilding.mixin;

import com.simplebuilding.util.RareShulkers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Verstaerkte und Enderit-Shulker lassen 0-2 Schalen ihrer Stufe fallen ({@link RareShulkers#dropShells}). */
@Mixin(Mob.class)
public abstract class MobShulkerShellDropMixin {
    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void simplebuilding$dropTierShells(ServerLevel level, DamageSource source, boolean killedByPlayer, CallbackInfo ci) {
        if ((Object) this instanceof Shulker shulker) {
            RareShulkers.dropShells(level, shulker);
        }
    }
}
