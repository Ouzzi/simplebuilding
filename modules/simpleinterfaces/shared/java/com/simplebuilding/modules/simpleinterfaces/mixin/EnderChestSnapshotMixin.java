package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ender chest tooltip: a player closing the own ender inventory (block, Easy Ender Chest) gets a fresh snapshot. */
@Mixin(PlayerEnderChestContainer.class)
public abstract class EnderChestSnapshotMixin {
    @Inject(method = "stopOpen", at = @At("TAIL"))
    private void simpleinterfaces$snapshot(ContainerUser user, CallbackInfo ci) {
        if (user instanceof ServerPlayer player && player.getEnderChestInventory() == (Object) this) ContainerTooltips.send(player);
    }
}
