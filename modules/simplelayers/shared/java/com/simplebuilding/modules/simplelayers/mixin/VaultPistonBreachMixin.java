package com.simplebuilding.modules.simplelayers.mixin;

import com.simplebuilding.modules.simplelayers.vault.VaultRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * UNDER21: Simple Machines' upgraded pistons push "unbreakable" blocks such as bedrock. Inside the Bedrock Vault they may not
 * (the way out is the altar, never the bedrock). {@code @Pseudo}, {@code require = 0}: without Simple Machines nothing applies
 * (module independence - no Simple Machines class is referenced).
 */
@Pseudo
@Mixin(targets = "com.simplemachines.upgrade.PistonBreach", remap = false)
public abstract class VaultPistonBreachMixin {
    @Inject(method = "isBreachable(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void simplelayers$holdVaultBedrock(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level l && VaultRegistry.inVault(l) && state.is(VaultRegistry.VAULT_HELD)) cir.setReturnValue(false);
    }
}
