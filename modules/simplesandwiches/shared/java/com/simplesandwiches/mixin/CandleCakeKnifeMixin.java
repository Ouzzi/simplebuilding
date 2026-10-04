package com.simplesandwiches.mixin;

import com.simplesandwiches.item.KnifeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Knife on a candle cake: the candle drops, then one slice is cut like from a full cake. */
@Mixin(CandleCakeBlock.class)
public abstract class CandleCakeKnifeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplesandwiches$knife(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (stack.getItem() instanceof KnifeItem) cir.setReturnValue(KnifeItem.cutCake(state, level, pos, player, hand, stack));
    }
}
