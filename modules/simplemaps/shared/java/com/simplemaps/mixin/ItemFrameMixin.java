package com.simplemaps.mixin;

import com.simplemaps.MapsItems;
import com.simplemaps.SimpleMaps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feature 4 (owner 2026-10-09): right-clicking a framed wayfinder map opens the map screen instead of rotating it;
 * sneaking keeps Vanilla's behaviour.
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplemaps$openFramedMap(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        ItemFrame self = (ItemFrame) (Object) this;
        if (player.isSecondaryUseActive() || !MapsItems.isWayfinder(self.getItem())) return;
        if (self.level().isClientSide() && hand == InteractionHand.MAIN_HAND) SimpleMaps.openFrame.accept(self.getId());
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
