package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freeze: no attacks. */
@Mixin(Player.class)
public abstract class AdminPlayerMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$attack(Entity target, CallbackInfo ci) { if (Freeze.frozen((Player) (Object) this)) ci.cancel(); }
}
