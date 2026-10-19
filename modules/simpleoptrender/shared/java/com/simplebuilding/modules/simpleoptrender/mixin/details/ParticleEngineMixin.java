package com.simplebuilding.modules.simpleoptrender.mixin.details;

import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O9 particles false: no particle is added to the engine (nothing to tick or draw). */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void simpleoptrender$noParticles(Particle particle, CallbackInfo ci) {
        if (RenderOpt.live(RenderOpt.DETAILS) && !RenderOpt.details().particles()) ci.cancel();
    }
}
