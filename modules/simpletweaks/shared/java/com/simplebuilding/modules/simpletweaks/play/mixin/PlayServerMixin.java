package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.Play;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ticks of the player/world/event features and of the claim pictures. */
@Mixin(MinecraftServer.class)
public abstract class PlayServerMixin {
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void simpletweaks$playTick(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) {
        Play.tick((MinecraftServer) (Object) this);
        // claim pictures for the deed (SP22-8): only loaded chunks, a few per 100 ticks; independent of the play features
        com.simplebuilding.modules.simpletweaks.deed.ClaimPictures.tick((MinecraftServer) (Object) this);
    }
}
