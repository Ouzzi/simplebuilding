package com.simpletrims.mixin;

import com.simpletrims.light.OwnedLightHolder;
import com.simpletrims.light.TrimLight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Radiance of worn armor: light block (server) and glints (client); armor stands and mobs store their light block. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTrimsMixin implements OwnedLightHolder {
    @Unique
    private @Nullable BlockPos simpletrims$ownedLight;

    @Inject(method = "tick", at = @At("TAIL"))
    private void simpletrims$radiance(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) TrimLight.tickGlowMotes(entity, () -> TrimLight.wornEmission(entity));
        else TrimLight.tickLiving(entity);
    }

    @Override
    public @Nullable BlockPos simpletrims$getOwnedLight() {
        return simpletrims$ownedLight;
    }

    @Override
    public void simpletrims$setOwnedLight(@Nullable BlockPos pos) {
        simpletrims$ownedLight = pos;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simpletrims$writeOwnedLight(ValueOutput output, CallbackInfo ci) {
        output.storeNullable(SAVE_KEY, BlockPos.CODEC, simpletrims$ownedLight);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simpletrims$readOwnedLight(ValueInput input, CallbackInfo ci) {
        simpletrims$ownedLight = input.read(SAVE_KEY, BlockPos.CODEC).orElse(null);
    }
}
