package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.ChunkLoaderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Chunk-Loader; prueft alle 5 s, ob sein Chunk-Bereich dem Config-Schalter entspricht. */
public class ChunkLoaderBlockEntity extends OwnedBlockEntity {
    public static final int CHECK_INTERVAL = 100;

    private boolean lastEnabled;
    private boolean checked;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.CHUNK_LOADER, pos, state);
    }

    public static boolean enabled() {
        return SimpleTweaks.config().pads.enableChunkLoaders;
    }

    public static int radiusOf(BlockState state) {
        return state.getBlock() instanceof ChunkLoaderBlock loader ? loader.getRadius() : 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean enabled = enabled();
        if (!be.checked || enabled != be.lastEnabled || level.getGameTime() % CHECK_INTERVAL == 0) {
            setForced(serverLevel, pos, radiusOf(state), enabled);
            be.checked = true;
            be.lastEnabled = enabled;
        }
    }

    /** Erzwingt bzw. gibt die Chunks im Radius um {@code pos} frei. */
    public static void setForced(ServerLevel level, BlockPos pos, int radius, boolean forced) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setChunkForced(cx + dx, cz + dz, forced);
            }
        }
    }
}
