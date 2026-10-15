package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The end of a server tick writes the collected changes into the items (one write per item, however many events). */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void simplestats$tick(BooleanSupplier haveTime, CallbackInfo ci) {
        StatsRuntime.tick((MinecraftServer) (Object) this);
    }
}
