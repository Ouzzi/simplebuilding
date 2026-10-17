package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanish: no item pickup unless /vanish pickup is on. */
@Mixin(net.minecraft.world.entity.item.ItemEntity.class)
public abstract class AdminPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$pickup(Player player, CallbackInfo ci) { if (player instanceof ServerPlayer p && !Vanish.pickup(p)) ci.cancel(); }
}
