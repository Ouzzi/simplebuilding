package com.simplemobheads.mixin;

import com.simplemobheads.helmet.HeadHelmets;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BS28b-1: a helmet in hand on a head puts it on, an empty hand takes it off again. Heads (Vanilla and Mobheads) override
 * neither use method, so this runs for them as the first thing the click does - before the held item gets to place a block
 * (except the Wither crown helmets, which {@link HeadHelmets#accepts} leaves to the Wither summoning).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class HeadHelmetUseMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$helmetOn(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof BlockState state && HeadHelmets.accepts(state, stack) && HeadHelmets.put(level, hit.getBlockPos(), player, stack)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$helmetOff(Level level, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        // the click also arrives here after a held item found nothing to do (TRY_WITH_EMPTY_HAND): only a really empty hand takes
        // the helmet off, so a block can still be placed against a helmeted head
        if ((Object) this instanceof BlockState state && player.getMainHandItem().isEmpty() && HeadHelmets.size(state) > 0.0F
                && HeadHelmets.take(level, hit.getBlockPos(), player)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
