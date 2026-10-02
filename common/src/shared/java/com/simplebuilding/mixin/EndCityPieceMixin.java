package com.simplebuilding.mixin;

import com.simplebuilding.util.BetterChests;
import com.simplebuilding.util.RareShulkers;
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
 * End-Stadt und End-Schiff: Loot-Truhen der Marker "Chest" werden selten zur Enderit-Truhe ({@link BetterChests}),
 * Wachposten-Shulker selten verstaerkt oder Enderit ({@link RareShulkers}).
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.EndCityPieces$EndCityPiece")
public abstract class EndCityPieceMixin {
    @Inject(method = "handleDataMarker", at = @At("TAIL"))
    private void simplebuilding$betterChest(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random,
                                            BoundingBox chunkBB, CallbackInfo ci) {
        if (markerId.startsWith("Chest") && chunkBB.isInside(position.below())) {
            BetterChests.upgradePlaced(level, position.below());
        }
    }

    @ModifyArg(method = "handleDataMarker", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity simplebuilding$rareShulker(Entity entity) {
        if (entity instanceof Shulker shulker) {
            RareShulkers.onEndCitySpawn(shulker, shulker.getRandom());
        }
        return entity;
    }
}
