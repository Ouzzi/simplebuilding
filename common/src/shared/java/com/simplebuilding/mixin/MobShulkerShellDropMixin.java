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
    /** Easter Egg: Rechtsklick mit dem passenden Klumpen wertet einen lebenden Shulker auf ({@link RareShulkers#upgradeLiving}). */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$upgradeShulker(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
                                               org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS && (Object) this instanceof Shulker shulker
                && RareShulkers.upgradeLiving(shulker, player, hand)) {
            cir.setReturnValue(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void simplebuilding$dropTierShells(ServerLevel level, DamageSource source, boolean killedByPlayer, CallbackInfo ci) {
        if ((Object) this instanceof Shulker shulker) {
            RareShulkers.dropShells(level, shulker);
        }
    }
}
