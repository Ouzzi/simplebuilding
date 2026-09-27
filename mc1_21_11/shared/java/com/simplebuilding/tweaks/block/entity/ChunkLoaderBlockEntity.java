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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Chunk-Loader (Simple Tweaks): erzwingt seinen Chunk-Bereich und gibt ihn beim Abbau wieder frei.
 *
 * <p>Anders als in Simple Tweaks merkt er sich, welche Chunks ER erzwungen hat (gespeichert unter
 * {@code Forced}), und gibt nur diese frei: Simple Tweaks rief beim Entladen blind
 * {@code setChunkForced(false)} - das hob auch fremde Erzwingungen auf (/forceload, andere Loader,
 * die Spieltest-Umgebung), und weil es schon beim Entladen der Block-Entity (Serverstopp) geschah,
 * tickte der Loader nach einem Neustart nie wieder. Jetzt geschieht die Freigabe nur beim Abbau
 * ({@link #preRemoveSideEffects}, und in {@link #setRemoved} fuer {@code /setblock}/{@code /fill},
 * die mit Flag 256 das Erste ueberspringen) oder wenn der Config-Schalter aus ist.
 *
 * <p>Ueberlappen sich zwei Loader, gehoert ein Chunk dem, der ihn zuerst erzwang; wird der abgebaut,
 * uebernimmt ein anderer Loader, der den Chunk ebenfalls abdeckt, statt dass der Chunk freikommt
 * (Audit 2026-09-26 #32).
 */
public class ChunkLoaderBlockEntity extends OwnedBlockEntity {
    public static final int CHECK_INTERVAL = 100;
    /** Groesster Radius eines Loaders in Chunks (Enderit: 1); so weit sucht die Uebergabe nach Nachbarn. */
    public static final int MAX_RADIUS = 1;
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

    /**
     * Gibt die eigenen Chunks frei; ein Chunk, den ein anderer (eingeschalteter) Loader ebenfalls
     * abdeckt, bleibt erzwungen und geht an diesen ueber.
     */
    public void release(ServerLevel level) {
        if (ownForced.isEmpty()) {
            return;
        }
        for (long key : ownForced) {
            int chunkX = (int) key;
            int chunkZ = (int) (key >> 32);
            ChunkLoaderBlockEntity heir = enabled() ? coveringNeighbour(level, chunkX, chunkZ) : null;
            if (heir != null) {
                heir.ownForced.add(key);
                heir.setChanged();
            } else {
                level.setChunkForced(chunkX, chunkZ, false);
            }
        }
        ownForced.clear();
        setChanged();
    }

    /** Ein anderer geladener Loader, dessen Bereich den Chunk abdeckt, oder null. */
    private @Nullable ChunkLoaderBlockEntity coveringNeighbour(ServerLevel level, int chunkX, int chunkZ) {
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
            for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be != this && !be.isRemoved() && be instanceof ChunkLoaderBlockEntity other && other.covers(chunkX, chunkZ)) {
                        return other;
                    }
                }
            }
        }
        return null;
    }

    /** Ob der Chunk im Bereich dieses Loaders liegt. */
    public boolean covers(int chunkX, int chunkZ) {
        int radius = radiusOf(getBlockState());
        return Math.abs((worldPosition.getX() >> 4) - chunkX) <= radius && Math.abs((worldPosition.getZ() >> 4) - chunkZ) <= radius;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            release(serverLevel);
        }
        super.preRemoveSideEffects(pos, state);
    }

    /**
     * {@code /setblock} und {@code /fill} setzen mit Flag 256 und ueberspringen
     * {@link #preRemoveSideEffects} - die Tickets blieben fuer immer (Audit #5). Hier wird
     * freigegeben, wenn an der Stelle schon ein anderer Block steht; beim Entladen des Chunks
     * (Serverstopp) steht der Loader noch da, dann bleibt alles erzwungen wie gewollt.
     */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && !ownForced.isEmpty() && replacedInWorld(serverLevel)) {
            release(serverLevel);
        }
        super.setRemoved();
    }

    private boolean replacedInWorld(ServerLevel level) {
        // getChunkNow laedt nichts nach; waehrend des Entladens liefert es null oder noch den Loader.
        LevelChunk chunk = level.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && !(chunk.getBlockState(worldPosition).getBlock() instanceof ChunkLoaderBlock);
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
