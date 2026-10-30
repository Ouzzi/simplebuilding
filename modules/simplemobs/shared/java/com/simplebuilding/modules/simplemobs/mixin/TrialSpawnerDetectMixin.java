package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trials.EvilOmen;
import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Where Vanilla looks for a player with Bad Omen, a Weathered Trial also looks for one with Evil / Void Omen. */
@Mixin(TrialSpawnerStateData.class)
public abstract class TrialSpawnerDetectMixin {
    @Inject(method = "tryDetectPlayers", at = @At("HEAD"), cancellable = true)
    private void simplemobs$evilOmen(ServerLevel level, BlockPos pos, TrialSpawner spawner, CallbackInfo ci) {
        if (TrialsExtras.capReached(level, pos, spawner)) {   // TR31-R: at most N active Weathered Spawners per structure
            ci.cancel();
            return;
        }
        EvilOmen.tryTrigger(level, pos, spawner);
    }
}
