package com.simplemobheads.mixin;

import com.simplemobheads.easter.KillerBunny;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The killer bunny (EH21 KO-2): after Vanilla picked the rabbit's variant, the rare roll for a killer bunny. */
@Mixin(Rabbit.class)
public abstract class RabbitSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void simplemobheads$killerBunny(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                            @Nullable SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        KillerBunny.onSpawn((Rabbit) (Object) this, level, reason);
    }
}
