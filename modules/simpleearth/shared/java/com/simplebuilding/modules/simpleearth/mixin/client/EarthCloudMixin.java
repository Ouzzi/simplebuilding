package com.simplebuilding.modules.simpleearth.mixin.client;

import com.simplebuilding.modules.simpleearth.orbit.OrbitView;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Simple Earth: no Vanilla clouds while the earth is shown - the cloud layer sits far below in the real world and showed up
 * as big pale squares in front of the earth (client screenshots 26.10.).
 */
@Mixin(CloudRenderer.class)
public abstract class EarthCloudMixin {
    @Inject(method = {"render", "renderOit"}, at = @At("HEAD"), cancellable = true)
    private void simpleearth$noCloudsInOrbit(CallbackInfo ci) {
        if (OrbitView.active) ci.cancel();
    }
}
