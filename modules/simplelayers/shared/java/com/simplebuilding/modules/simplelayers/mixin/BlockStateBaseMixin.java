package com.simplebuilding.modules.simplelayers.mixin;

import com.simplebuilding.modules.simplelayers.block.BackroomBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BACK18: the deeper Backrooms levels are built from Vanilla blocks (concrete, tuff, quartz, copper ...). Inside the Backrooms a
 * player with the Backroom effect cannot mine those either (block tag {@code simplelayers:backrooms_structure}); the same blocks
 * anywhere else, and every supply block, break as always. Client and server agree (the effect is synced), so no ghost progress.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void simplelayers$holdLevelBlocks(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (BackroomBlock.heldFor((BlockState) (Object) this, player)) cir.setReturnValue(0.0F);
    }
}
