package com.simplebuilding.mixin;

import com.simplebuilding.util.BetterChests;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Festung und Netherfestung: eine frisch gesetzte Loot-Truhe wird selten zur Stufen-Truhe ({@link BetterChests}). */
@Mixin(StructurePiece.class)
public abstract class StructurePieceBetterChestMixin {
    @Inject(method = "createChest(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("RETURN"))
    private void simplebuilding$betterChest(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos,
                                            ResourceKey<LootTable> lootTable, BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            BetterChests.upgradePlaced(level, pos);
        }
    }
}
