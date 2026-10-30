package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trials.TrialVaultBlock;
import com.simplebuilding.modules.simplemobs.trials.WeatheredTrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Weathered Trial Spawner and the three vaults run on Vanilla's trial spawner / vault block entities (their whole logic), whose type
 * only knows the Vanilla blocks as valid. One mixin for all loaders (the same way as the skulls of Simple Tweaks) instead of
 * Fabric's {@code addSupportedBlock} or NeoForge's event.
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeTrialsMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void simplemobs$trialBlocks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this == BlockEntityTypes.TRIAL_SPAWNER && state.getBlock() instanceof WeatheredTrialSpawnerBlock)
                || ((Object) this == BlockEntityTypes.VAULT && state.getBlock() instanceof TrialVaultBlock)) {
            cir.setReturnValue(true);
        }
    }
}
