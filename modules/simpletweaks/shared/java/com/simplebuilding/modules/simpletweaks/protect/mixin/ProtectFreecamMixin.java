package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.client.ProtectClient;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam rule, client side: while the server's rule is on, Freecam 1.4+/1.5 (the builds for 26.x) may not ignore block
 * collision. {@code @Pseudo}: applies only when Freecam is installed; nothing happens without it.
 */
@Pseudo
@Mixin(targets = "net.xolt.freecam.config.model.ModConfigImpl", remap = false)
public abstract class ProtectFreecamMixin {
    @Inject(method = "ignoreAllCollision()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void simpletweaks$collideAll(CallbackInfoReturnable<Boolean> cir) {
        if (ProtectClient.forceFreecamCollision()) cir.setReturnValue(false);
    }
    @Inject(method = "ignoreCollisionWith(Lnet/minecraft/world/level/block/Block;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void simpletweaks$collideWith(Block block, CallbackInfoReturnable<Boolean> cir) {
        if (ProtectClient.forceFreecamCollision()) cir.setReturnValue(false);
    }
}
