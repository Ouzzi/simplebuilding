package com.simplebuilding.modules.simplechallenges.mixin;

import com.simplebuilding.modules.simplechallenges.core.Enforcement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Unlock borders: {@code Level.setBlock} refuses every change inside a locked cell, but many Vanilla paths drop first and
 * change afterwards (explosions, {@code Level.destroyBlock}, fluids, honeycomb, sweet berries...), so a refused change would still
 * hand out the drops and the block would stay - a farm. Nothing pops out of a block in a locked cell. Inert without a chosen challenge ({@code Gate}).
 */
@Mixin(Block.class)
public abstract class LockedDropsMixin {
    @Inject(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private static void simplechallenges$nothingFromLockedCells(Level level, BlockPos pos, ItemStack stack, CallbackInfo ci) {
        if (Enforcement.blocksChange(level, pos)) ci.cancel();
    }

    @Inject(method = "popResourceFromFace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private static void simplechallenges$nothingFromLockedCellsFace(Level level, BlockPos pos, Direction face, ItemStack stack, CallbackInfo ci) {
        if (Enforcement.blocksChange(level, pos)) ci.cancel();
    }
}
