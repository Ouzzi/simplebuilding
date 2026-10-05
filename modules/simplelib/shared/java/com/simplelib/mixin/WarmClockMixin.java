package com.simplelib.mixin;

import com.simplelib.warm.WarmMerge;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps {@link WarmMerge#now()} on the server's game time (merges happen without a level at hand). */
@Mixin(ServerLevel.class)
public abstract class WarmClockMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void simplelib$clock(BooleanSupplier hasTime, CallbackInfo ci) {
        WarmMerge.tick(((ServerLevel) (Object) this).getGameTime());
    }
}
