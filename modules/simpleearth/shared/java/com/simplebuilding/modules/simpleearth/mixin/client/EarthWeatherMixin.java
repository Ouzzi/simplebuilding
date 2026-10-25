package com.simplebuilding.modules.simpleearth.mixin.client;

import com.simplebuilding.modules.simpleearth.orbit.OrbitView;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WE25-1 (Simple Earth): in orbit there is no weather - the client level reports rain and thunder 0 while the earth view is
 * shown, so neither rain nor snow nor their sounds play up there. The server's weather is untouched.
 */
@Mixin(Level.class)
public abstract class EarthWeatherMixin {
    @Inject(method = {"getRainLevel", "getThunderLevel"}, at = @At("HEAD"), cancellable = true)
    private void simpleearth$noWeatherInOrbit(float partial, CallbackInfoReturnable<Float> cir) {
        if (OrbitView.active && ((Level) (Object) this).isClientSide()) cir.setReturnValue(0f);
    }
}
