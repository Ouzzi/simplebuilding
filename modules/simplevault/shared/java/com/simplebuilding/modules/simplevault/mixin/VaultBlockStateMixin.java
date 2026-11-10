package com.simplebuilding.modules.simplevault.mixin;

import com.simplebuilding.modules.simplevault.VaultEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** UNDER21: in the Bedrock Vault a Vault-bound player cannot mine the held blocks (tag {@code simplevault:vault_held}); client and server agree. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class VaultBlockStateMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void simplevault$holdVaultBlocks(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (VaultEffect.held((BlockState) (Object) this, player)) cir.setReturnValue(0.0F);
    }
}
