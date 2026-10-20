package com.simplestorage.mixin;

import com.simplestorage.chest.StorageUse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's shulker boxes (all 17 colors) are the first step of the shulker box upgrade (in_world_upgrades: two cracked
 * diamonds in the off hand). Vanilla would open the box menu on the right-click and the axe or hammer never get their turn, so in
 * the upgrade stance the click goes to them ({@link StorageUse}), on both sides. The tier shulker boxes override the method
 * and ask themselves.
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxUpgradeClickMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplestorage$upgradeClick(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = StorageUse.upgradeClick(state, level, pos, player, InteractionHand.MAIN_HAND);
        if (result != null) cir.setReturnValue(result);
    }
}
