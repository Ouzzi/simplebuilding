package com.simplebuilding.mixin;

import com.simplebuilding.util.OwnedLightHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Radiance: ein Ruestungsstaender mit emittierender Ruestung merkt sich seinen Lichtblock,
 * auch ueber das Entladen des Chunks hinweg (Tick-Logik in LivingEntityMixin/DynamicLightHandler).
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandLightMixin implements OwnedLightHolder {

    @Unique
    private @Nullable BlockPos simplebuilding$ownedLight;

    @Override
    public @Nullable BlockPos simplebuilding$getOwnedLight() {
        return this.simplebuilding$ownedLight;
    }

    @Override
    public void simplebuilding$setOwnedLight(@Nullable BlockPos pos) {
        this.simplebuilding$ownedLight = pos;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$writeOwnedLight(ValueOutput output, CallbackInfo ci) {
        output.storeNullable("SimpleBuildingOwnedLight", BlockPos.CODEC, this.simplebuilding$ownedLight);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$readOwnedLight(ValueInput input, CallbackInfo ci) {
        this.simplebuilding$ownedLight = input.read("SimpleBuildingOwnedLight", BlockPos.CODEC).orElse(null);
    }
}
