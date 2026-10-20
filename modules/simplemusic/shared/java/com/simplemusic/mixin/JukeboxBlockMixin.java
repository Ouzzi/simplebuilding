package com.simplemusic.mixin;

import com.simplemusic.disc.JukeboxDiscs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A custom disc goes into an empty Vanilla jukebox like a music disc (owner decision U1 b; guard in JukeboxDiscs). */
@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplemusic$insertCustomDisc(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getValue(JukeboxBlock.HAS_RECORD) || !JukeboxDiscs.isDisc(stack) || !JukeboxDiscs.on(level)) return;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
            jukebox.setTheItem(stack.consumeAndReturn(1, player));
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
