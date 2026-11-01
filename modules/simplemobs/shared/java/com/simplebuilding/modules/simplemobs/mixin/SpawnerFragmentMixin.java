package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.SpawnerParts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WF25-6: a mined Vanilla spawner may drop a Spawner Fragment (one mixin for all loaders instead of a loot modifier per loader). */
@Mixin(SpawnerBlock.class)
public abstract class SpawnerFragmentMixin {
    @Inject(method = "spawnAfterBreak", at = @At("HEAD"))
    private void simplemobs$fragment(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp, CallbackInfo ci) {
        if (state.is(Blocks.SPAWNER)) SpawnerParts.onVanillaSpawnerBroken(level, pos, tool);
    }
}
