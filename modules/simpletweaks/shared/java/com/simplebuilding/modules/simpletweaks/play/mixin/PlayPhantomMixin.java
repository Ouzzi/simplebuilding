package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.simplebuilding.modules.simpletweaks.play.phantoms.Phantoms;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Players with /phantoms off are skipped by the phantom spawner like spectators (T30). */
@Mixin(PhantomSpawner.class)
public abstract class PlayPhantomMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean simpletweaks$playPhantoms(ServerPlayer player, Operation<Boolean> original) {
        return original.call(player) || Phantoms.off(player);
    }
}
