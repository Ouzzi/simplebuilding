package com.simplestorage.mixin;

import com.simplestorage.chest.StorageUse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's copper chests (all eight) are the first step of the chest upgrade (in_world_upgrades: a cracked diamond in
 * the off hand). Vanilla would open the chest menu on the right-click and the axe or hammer never get their turn, so in
 * the upgrade stance the click goes to them ({@link StorageUse}), on both sides. The tiered chests override the method
 * and ask themselves.
 */
@Mixin(ChestBlock.class)
public abstract class ChestUpgradeClickMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplestorage$upgradeClick(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = StorageUse.upgradeClick(state, level, pos, player, InteractionHand.MAIN_HAND);
        if (result != null) cir.setReturnValue(result);
    }
}
