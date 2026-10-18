package com.simplebuilding.mixin.client;

import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sledgehammer exploit fix (owner N65): sneaking (1 block) and releasing (area) change the
 * mining divisor mid-break, but vanilla keeps the accumulated progress. The progress is therefore
 * converted at every switch so that the elapsed mining time stays the same
 * ({@link SledgehammerUtils#rescaleProgress}).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeHammerProgressMixin {

    @Shadow private float destroyProgress;
    @Shadow @org.spongepowered.asm.mixin.Final private Minecraft minecraft;

    @Unique private float simplebuilding$lastDivisor = 0.0F;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void simplebuilding$rescaleHammerProgress(BlockPos pos, Direction direction,
                                                      CallbackInfoReturnable<Boolean> cir) {
        // 26.3 (split stage 7): Simple Hammers carries this fix itself.
        if (!com.simplebuilding.version.McVersion.HAMMERS_IN_SB) return;
        var player = this.minecraft.player;
        if (player == null || !(com.simplebuilding.items.custom.SledgehammerItem.isHammer(player.getMainHandItem().getItem()))) {
            this.simplebuilding$lastDivisor = 0.0F;
            return;
        }
        float divisor = SledgehammerUtils.miningSpeedDivisor(player, pos);
        if (this.destroyProgress > 0.0F) {
            this.destroyProgress = SledgehammerUtils.rescaleProgress(
                    this.destroyProgress, this.simplebuilding$lastDivisor, divisor);
        }
        this.simplebuilding$lastDivisor = divisor;
    }
}
