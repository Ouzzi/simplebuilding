package com.simplemusic.mixin.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.PocketScrollPayload;
import com.simplemusic.pocket.PocketJukeboxBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shift + mouse wheel on a placed Pocket Jukebox goes through its discs like on a placed bundle (PJ23-1); the server owns
 * the shown disc, the hotbar stays put.
 */
@Mixin(MouseHandler.class)
public abstract class PocketScrollMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void simplemusic$scrollPocketJukebox(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!SimpleMusic.pocketScroll || client.player == null || client.gui.screen() != null || vertical == 0 || !client.player.isShiftKeyDown()
                || client.player.isSpectator() || client.level == null) return;
        if (client.hitResult instanceof BlockHitResult blockHit && client.hitResult.getType() == HitResult.Type.BLOCK
                && client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PocketJukeboxBlock) {
            // wheel down = next disc, as in the hotbar
            SimpleMusic.toServer.accept(new PocketScrollPayload(blockHit.getBlockPos(), vertical < 0 ? 1 : -1));
            ci.cancel();
        }
    }
}
