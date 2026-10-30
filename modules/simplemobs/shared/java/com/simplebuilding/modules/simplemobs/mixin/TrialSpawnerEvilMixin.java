package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trials.EvilOmen;
import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Evil / Void Trial (BS27-3/5/6) on the Vanilla trial spawner logic: stronger mobs, the key of the stage as reward, red / violet particles. */
@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerEvilMixin {
    @Inject(method = "spawnMob", at = @At("RETURN"))
    private void simplemobs$buff(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Optional<UUID>> cir) {
        cir.getReturnValue().ifPresent(id -> EvilOmen.onSpawned(level, pos, level.getEntity(id)));
    }

    @Inject(method = "ejectReward", at = @At("HEAD"), cancellable = true)
    private void simplemobs$reward(ServerLevel level, BlockPos pos, ResourceKey<LootTable> table, CallbackInfo ci) {
        ResourceKey<LootTable> own = EvilOmen.rewardTable(level, pos);
        if (own != null && !own.equals(table)) {
            ((TrialSpawner) (Object) this).ejectReward(level, pos, own);
            ci.cancel();
        }
    }

    @Inject(method = "removeOminous", at = @At("HEAD"))
    private void simplemobs$reset(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        EvilOmen.reset(level, pos);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void simplemobs$particles(ServerLevel level, BlockPos pos, boolean ominous, CallbackInfo ci) {
        EvilOmen.particles(level, pos);
        TrialsExtras.markTick(level, pos, (TrialSpawner) (Object) this);
    }
}
