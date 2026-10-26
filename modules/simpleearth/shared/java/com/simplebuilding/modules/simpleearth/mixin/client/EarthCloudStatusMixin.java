package com.simplebuilding.modules.simpleearth.mixin.client;

import com.simplebuilding.modules.simpleearth.orbit.OrbitView;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Simple Earth: clouds count as off while the earth is shown (the cloud layer still showed as dark squares in front of the
 * earth in the 26.10. screenshots although the cloud renderer was skipped; the player's own setting is untouched).
 */
@Mixin(Options.class)
public abstract class EarthCloudStatusMixin {
    @Inject(method = "getCloudStatus", at = @At("HEAD"), cancellable = true)
    private void simpleearth$noCloudsInOrbit(CallbackInfoReturnable<CloudStatus> cir) {
        if (OrbitView.active) cir.setReturnValue(CloudStatus.OFF);
    }
}
