package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.version.McVersion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** A live sound predicate, never an entity flag or a saved aura. */
public final class SilentDandelions {
    private SilentDandelions() {}

    private static boolean isFlower(BlockState state) {
        return state.is(ModBlocks.SILENT_DANDELION) || state.is(ModBlocks.POTTED_SILENT_DANDELION);
    }

    public static boolean affects(Entity entity) {
        if (!McVersion.SILENT_DANDELION || !(entity instanceof Mob)
                || !ServerTuning.get().silentDandelion.enabled) return false;
        int radius = ServerTuning.silentDandelionRadius();
        Level level = entity.level();
        int minX = Mth.floor(entity.getX() - radius), maxX = Mth.floor(entity.getX() + radius);
        int minY = Math.max(level.getMinY(), Mth.floor(entity.getY() - radius));
        int maxY = Math.min(level.getMaxY(), Mth.floor(entity.getY() + radius));
        int minZ = Mth.floor(entity.getZ() - radius), maxZ = Mth.floor(entity.getZ() + radius);
        // Never load chunks. Palette checks skip sections without either flower, including empty air.
        // No position cache: placing/removing a flower or moving across the radius takes effect immediately.
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(sy << 4));
                    if (!section.maybeHas(SilentDandelions::isFlower)) continue;
                    for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++) {
                        for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++) {
                            for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                                if (isFlower(section.getBlockState(x & 15, y & 15, z & 15))
                                        && entity.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= radius * radius) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
