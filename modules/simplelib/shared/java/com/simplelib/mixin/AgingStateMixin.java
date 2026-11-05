package com.simplelib.mixin;

import com.simplelib.api.aging.Aging;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DW04B-1 aging chains ({@link Aging}) on the block state, so they work for every block class: Vanilla base blocks that
 * never random-tick (pumpkin) and classes that override {@code useItemOn} without calling super (pumpkin shears). Blocks
 * outside a chain cost one identity-map lookup.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class AgingStateMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void simplelib$age(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Aging.randomTick((BlockState) (Object) this, level, pos, random)) ci.cancel();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplelib$waxOrScrape(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = Aging.useItemOn((BlockState) (Object) this, stack, level, player, hand, hit);
        if (result != null) cir.setReturnValue(result);
    }
}
