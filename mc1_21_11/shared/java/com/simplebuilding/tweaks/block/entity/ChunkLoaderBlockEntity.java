package com.simplebuilding.tweaks.block.entity;

import com.mojang.serialization.Codec;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.ChunkLoaderBlock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Chunk-Loader (Simple Tweaks): erzwingt seinen Chunk-Bereich und gibt ihn beim Abbau wieder frei.
 *
 * <p>Anders als in Simple Tweaks merkt er sich, welche Chunks ER erzwungen hat (gespeichert unter
 * {@code Forced}), und gibt nur diese frei: Simple Tweaks rief beim Entladen blind
 * {@code setChunkForced(false)} - das hob auch fremde Erzwingungen auf (/forceload, andere Loader,
 * die Spieltest-Umgebung), und weil es schon beim Entladen der Block-Entity (Serverstopp) geschah,
 * tickte der Loader nach einem Neustart nie wieder. Jetzt geschieht die Freigabe nur beim Abbau
 * ({@link #preRemoveSideEffects}) oder wenn der Config-Schalter aus ist.
 */
public class ChunkLoaderBlockEntity extends OwnedBlockEntity {
    public static final int CHECK_INTERVAL = 100;
    private static final Codec<List<Long>> FORCED_CODEC = Codec.LONG.listOf();

    private final Set<Long> ownForced = new LinkedHashSet<>();
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

    /** Chunks, die dieser Loader erzwungen hat (fuer Tests und die Freigabe). */
    public Set<Long> ownForced() {
        return ownForced;
    }

    public static long key(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity be) {
        if (level instanceof ServerLevel serverLevel && (!be.checked || level.getGameTime() % CHECK_INTERVAL == 0)) {
            be.checked = true;
            be.update(serverLevel, radiusOf(state));
        }
    }

    /** Erzwingt den Bereich (Config an) bzw. gibt die eigenen Chunks frei (Config aus). */
    public void update(ServerLevel level, int radius) {
        if (!enabled()) {
            release(level);
            return;
        }
        int cx = worldPosition.getX() >> 4;
        int cz = worldPosition.getZ() >> 4;
        boolean changed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // true = war vorher nicht erzwungen, also unsere Erzwingung.
                if (level.setChunkForced(cx + dx, cz + dz, true)) {
                    changed |= ownForced.add(key(cx + dx, cz + dz));
                }
            }
        }
        if (changed) {
            setChanged();
        }
    }

    public void release(ServerLevel level) {
        if (ownForced.isEmpty()) {
            return;
        }
        for (long key : ownForced) {
            level.setChunkForced((int) key, (int) (key >> 32), false);
        }
        ownForced.clear();
        setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            release(serverLevel);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!ownForced.isEmpty()) {
            output.store("Forced", FORCED_CODEC, new ArrayList<>(ownForced));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownForced.clear();
        input.read("Forced", FORCED_CODEC).ifPresent(ownForced::addAll);
    }
}
