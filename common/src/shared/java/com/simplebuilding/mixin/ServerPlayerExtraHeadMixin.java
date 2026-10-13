package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The extra head slot (owner N49) moves with the inventory when the server copies a player: back from the End, or
 * respawning with {@code keepInventory} - exactly when Vanilla copies the inventory.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExtraHeadMixin {
    @Inject(method = "transferInventoryXpAndScore", at = @At("TAIL"))
    private void simplebuilding$copyExtraHead(Player oldPlayer, CallbackInfo ci) {
        ExtraHead.set((Player) (Object) this, ExtraHead.get(oldPlayer).copy());
    }
}
