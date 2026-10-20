package com.simplestorage.mixin.client;

import com.simplestorage.bundle.BundleNet;
import com.simplestorage.bundle.PlacedBundleBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sneak + mouse wheel on a placed bundle chooses the shown item (split stage 8c-1, the bundle part of SimpleBuilding's
 * {@code MouseMixin}); the server owns the index, the hotbar stays put.
 */
@Mixin(MouseHandler.class)
public abstract class BundleScrollMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void simplestorage$scrollPlacedBundle(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null || vertical == 0 || !client.player.isShiftKeyDown()
                || client.player.isSpectator() || client.level == null) return;
        if (client.hitResult instanceof BlockHitResult blockHit && client.hitResult.getType() == HitResult.Type.BLOCK
                && client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlacedBundleBlock) {
            // Wheel down = next item, as in the hotbar.
            BundleNet.toServer.accept(new BundleNet.ScrollPayload(blockHit.getBlockPos(), vertical < 0 ? 1 : -1));
            ci.cancel();
        }
    }
}
