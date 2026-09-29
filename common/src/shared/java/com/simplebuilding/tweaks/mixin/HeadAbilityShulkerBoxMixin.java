package com.simplebuilding.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.tweaks.heads.HeadAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Shulkerkopf ({@link HeadAbilities#opensBlockedShulkerBoxes}): eine Shulkerkiste, deren Deckel ein Block
 * versperrt, geht fuer den Traeger trotzdem auf (Vanilla: {@code canOpen} verweigert).
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class HeadAbilityShulkerBoxMixin {

    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/ShulkerBoxBlock;canOpen(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;)Z"))
    private boolean simplebuilding$shulkerHeadOpensBlockedBoxes(BlockState state, Level level, BlockPos pos, ShulkerBoxBlockEntity box,
                                                              Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return original.call(state, level, pos, box) || HeadAbilities.opensBlockedShulkerBoxes(player);
    }
}
