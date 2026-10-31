package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.ZoneSpawns;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Danger zones (WP-22): after Vanilla's {@code finalizeSpawn} a new monster may get status effects by its spawn place (see {@link ZoneSpawns}). */
@Mixin(Mob.class)
public abstract class MobZoneSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void simplemobs$zoneEffects(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        ZoneSpawns.onSpawn((Mob) (Object) this, reason);
    }
}
