package com.simplestorage.mixin;

import com.simplestorage.rare.BetterChests;
import com.simplestorage.rare.RareShulkers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * End city and end ship: loot chests of the "Chest" markers rarely become the Enderite Chest ({@link BetterChests}), sentry
 * shulkers rarely reinforced, Netherite or Enderite ({@link RareShulkers}).
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.EndCityPieces$EndCityPiece")
public abstract class EndCityPieceMixin {
    @Inject(method = "handleDataMarker", at = @At("TAIL"))
    private void simplestorage$betterChest(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random,
                                           BoundingBox chunkBB, CallbackInfo ci) {
        if (markerId.startsWith("Chest") && chunkBB.isInside(position.below())) {
            BetterChests.upgradePlaced(level, position.below());
        }
    }

    @ModifyArg(method = "handleDataMarker", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity simplestorage$rareShulker(Entity entity) {
        if (entity instanceof Shulker shulker) {
            RareShulkers.onEndCitySpawn(shulker, shulker.getRandom());
        }
        return entity;
    }
}
