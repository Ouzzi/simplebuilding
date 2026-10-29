package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Enderite shield: no Enderman spawns naturally within 64 blocks (cube) of a powered Enderite block. Mixed into Monster
 * (Endermen use {@code checkMonsterSpawnRules}).
 * <p>BS27-1: the check used to read all 129^3 (2.1 million) blocks of the cube on every Enderman spawn attempt - at night
 * and in the End many times per tick, so ticks took seconds and chunk loading and saving starved. Now it walks the loaded
 * chunks of the cube and skips every section whose palette holds no Enderite block (a few dozen palette checks).
 */
@Mixin(Monster.class)
public abstract class EndermanSpawnMixin {

    @Inject(method = "checkMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void checkEnderiteShield(EntityType<? extends Monster> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        // MC 26.2: die EntityType-Konstanten liegen in EntityTypes.
        // Nur natuerliches Spawnen in der echten Welt (Monster spawnen nicht beim Generieren; WorldGenRegion = Worker-Thread).
        if (type == EntityTypes.ENDERMAN && spawnReason == EntitySpawnReason.NATURAL && world instanceof ServerLevel level
                && simplebuilding$shielded(level, pos, 64)) {
            cir.setReturnValue(false);
        }
    }

    /** A powered Enderite block within the cube of {@code radius} round {@code pos} (only loaded chunks, never loads one). */
    @Unique
    private static boolean simplebuilding$shielded(ServerLevel level, BlockPos pos, int radius) {
        int minY = Math.max(pos.getY() - radius, level.getMinY());
        int maxY = Math.min(pos.getY() + radius, level.getMaxY());
        if (minY > maxY) {
            return false;
        }
        java.util.function.Predicate<BlockState> enderite = state -> state.is(ModBlocks.ENDERITE_BLOCK);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int cx = (pos.getX() - radius) >> 4; cx <= (pos.getX() + radius) >> 4; cx++) {
            for (int cz = (pos.getZ() - radius) >> 4; cz <= (pos.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(enderite)) {
                        continue;
                    }
                    for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++) {
                        for (int x = Math.max(pos.getX() - radius, cx << 4); x <= Math.min(pos.getX() + radius, (cx << 4) + 15); x++) {
                            for (int z = Math.max(pos.getZ() - radius, cz << 4); z <= Math.min(pos.getZ() + radius, (cz << 4) + 15); z++) {
                                if (enderite.test(section.getBlockState(x & 15, y & 15, z & 15))
                                        && level.getBestNeighborSignal(at.set(x, y, z)) > 0) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
