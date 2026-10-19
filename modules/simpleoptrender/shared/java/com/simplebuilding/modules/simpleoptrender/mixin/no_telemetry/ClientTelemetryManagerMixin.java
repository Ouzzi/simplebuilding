package com.simplebuilding.modules.simpleoptrender.mixin.no_telemetry;

import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M2: every telemetry sender is the Vanilla DISABLED one, so no telemetry session starts and nothing is sent. */
@Mixin(ClientTelemetryManager.class)
abstract class ClientTelemetryManagerMixin {
    @Inject(method = "createEventSender", at = @At("HEAD"), cancellable = true)
    private void simpleoptrender$disabled(CallbackInfoReturnable<TelemetryEventSender> cir) {
        if (RenderOpt.live(RenderOpt.NO_TELEMETRY)) cir.setReturnValue(TelemetryEventSender.DISABLED);
    }
}
