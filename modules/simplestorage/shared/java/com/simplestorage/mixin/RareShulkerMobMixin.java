package com.simplestorage.mixin;

import com.simplestorage.rare.RareShulkers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rare shulkers drop 0-2 shells of their tier ({@link RareShulkers#dropShells}) and take the nugget of the next tier. */
@Mixin(Mob.class)
public abstract class RareShulkerMobMixin {
    /** Easter egg: a right-click with the matching nugget lifts a living shulker ({@link RareShulkers#upgradeLiving}). */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void simplestorage$upgradeShulker(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof Shulker shulker && com.simplestorage.SimpleStorage.on() && RareShulkers.upgradeLiving(shulker, player, hand)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void simplestorage$dropTierShells(ServerLevel level, DamageSource source, boolean killedByPlayer, CallbackInfo ci) {
        if ((Object) this instanceof Shulker shulker) {
            RareShulkers.dropShells(level, shulker);
        }
    }
}
