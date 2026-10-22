package com.simpleshops.mixin;

import com.simpleshops.logic.ShopGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shop blocks are handled before anything else (low mixin priority = runs first, ahead of claims and other protections):
 * <ul>
 * <li>{@code destroyBlock}: a stranger breaks nothing (no tool, creative or mod gets around this), the owner and operators
 * remove the block with Vanilla's sequence without a claim check (the owner may always remove his shop).</li>
 * <li>{@code useItemOn}: buying needs no claim right (owner answer 11), so the click on a shop block is run here unless the
 * player sneaks with an item (then Vanilla places a block next to it, which needs the build right as usual).</li>
 * </ul>
 */
@Mixin(value = ServerPlayerGameMode.class, priority = 100)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void simpleshops$guardBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.player.level() instanceof ServerLevel level)) return;
        BlockState state = level.getBlockState(pos);
        if (!ShopGuard.isProtected(level, pos, state)) return;
        if (!ShopGuard.mayBreak(this.player, level, pos)) {
            level.sendBlockUpdated(pos, state, state, 3);
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(ShopGuard.ownerBreak(this.player, level, pos));
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simpleshops$use(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                                 CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel) || player.isSpectator()) return;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!ShopGuard.isShop(state)) return;
        boolean suppress = player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty());
        if (suppress) return;
        InteractionResult use = state.useItemOn(player.getItemInHand(hand), level, player, hand, hit);
        if (use.consumesAction()) {
            cir.setReturnValue(use);
            return;
        }
        if (use instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
            InteractionResult empty = state.useWithoutItem(level, player, hit);
            if (empty.consumesAction()) cir.setReturnValue(empty);
        }
    }
}
