package com.simpletrims.mixin;

import com.simpletrims.light.OwnedLightHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An item frame with emitting armor lights its block; it stores that light block with itself. */
@Mixin(ItemFrame.class)
public abstract class ItemFrameTrimsMixin implements OwnedLightHolder {
    @Unique
    private @Nullable BlockPos simpletrims$ownedLight;

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
