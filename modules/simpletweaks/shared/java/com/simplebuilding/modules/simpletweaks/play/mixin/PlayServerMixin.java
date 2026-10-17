package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.Play;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ticks of the player/world/event features. */
@Mixin(MinecraftServer.class)
public abstract class PlayServerMixin {
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void simpletweaks$playTick(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) { Play.tick((MinecraftServer) (Object) this); }
}
