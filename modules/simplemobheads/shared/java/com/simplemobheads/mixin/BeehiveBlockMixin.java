package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Bee head: harvesting honey or combs without a campfire does not anger the bees - neither those around the hive nor the
 * ones it releases (they are let out without a target). Only the harvest; hitting a bee still angers it.
 */
@Mixin(BeehiveBlock.class)
public abstract class BeehiveBlockMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;angerNearbyBees(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void simplemobheads$beeHeadCalmsTheSwarm(BeehiveBlock hive, Level level, BlockPos pos, Operation<Void> original,
                                                    @Local(argsOnly = true) Player player) {
        if (!HeadAbilities.calmsBees(player)) {
            original.call(hive, level, pos);
        }
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BeehiveBlock;releaseBeesAndResetHoneyLevel(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$BeeReleaseStatus;)V"))
    private void simplemobheads$beeHeadReleasesCalmBees(BeehiveBlock hive, Level level, BlockState state, BlockPos pos, Player player,
                                                       BeehiveBlockEntity.BeeReleaseStatus status, Operation<Void> original) {
        original.call(hive, level, state, pos, player != null && HeadAbilities.calmsBees(player) ? null : player, status);
    }
}
