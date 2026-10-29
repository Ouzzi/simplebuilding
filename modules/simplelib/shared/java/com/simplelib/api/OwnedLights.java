package com.simplelib.api;

import com.mojang.serialization.Codec;
import com.simplelib.SimpleLib;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Register of the invisible {@code minecraft:light} blocks that mods set as carried light (radiance armor, torches in hand).
 * The carriers keep their positions in memory, which a server crash loses, so standing lights would stay in the world for
 * good. Every light a mod sets is marked here (a {@link SavedData} per dimension); when a chunk loads, {@link #sweep}
 * removes the marked light blocks that no live carrier claims ({@link #claim}). A Vanilla light block nobody marked
 * (placed by a player or command) is never touched. Cost: one set lookup per chunk load, no world scan.
 * Server thread only.
 */
public final class OwnedLights extends SavedData {
    private static final Map<String, BiPredicate<Level, BlockPos>> CLAIMS = new ConcurrentHashMap<>();
    public static final Codec<OwnedLights> CODEC = Codec.LONG.listOf().xmap(OwnedLights::new, l -> new ArrayList<>(l.marks)).fieldOf("marks").codec();
    public static final SavedDataType<OwnedLights> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SimpleLib.MOD_ID, "owned_lights"), OwnedLights::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final java.util.Set<Long> marks = new java.util.LinkedHashSet<>();
    /** chunk key -> marks inside that chunk (so a chunk load looks at its own marks only, not at all of them). */
    private final java.util.Map<Long, java.util.Set<Long>> byChunk = new java.util.HashMap<>();

    public OwnedLights() {}

    private OwnedLights(List<Long> loaded) {
        loaded.forEach(this::add);
    }

    private static long chunkKey(long pos) {
        BlockPos p = BlockPos.of(pos);
        return ChunkPos.pack(p.getX() >> 4, p.getZ() >> 4);
    }

    private boolean add(long pos) {
        if (!marks.add(pos)) return false;
        byChunk.computeIfAbsent(chunkKey(pos), k -> new java.util.LinkedHashSet<>()).add(pos);
        return true;
    }

    private boolean remove(long pos) {
        if (!marks.remove(pos)) return false;
        long c = chunkKey(pos);
        var set = byChunk.get(c);
        if (set != null && set.remove(pos) && set.isEmpty()) byChunk.remove(c);
        return true;
    }

    /** A module registers (or replaces) who is still alive: true = a live carrier holds the light at {@code pos}, keep it. */
    public static void claim(String id, BiPredicate<Level, BlockPos> live) {
        CLAIMS.put(id, live);
    }

    private static OwnedLights of(Level level) {
        return level instanceof ServerLevel s ? s.getDataStorage().computeIfAbsent(TYPE) : null;
    }

    /** Call after a mod set a light block at {@code pos}. */
    public static void mark(Level level, BlockPos pos) {
        OwnedLights o = of(level);
        if (o != null && o.add(pos.asLong())) o.setDirty();
    }

    /** Call when a mod removed its light block at {@code pos}. */
    public static void unmark(Level level, BlockPos pos) {
        OwnedLights o = of(level);
        if (o != null && o.remove(pos.asLong())) o.setDirty();
    }

    public static boolean isMarked(Level level, BlockPos pos) {
        OwnedLights o = of(level);
        return o != null && o.marks.contains(pos.asLong());
    }

    /**
     * Loader hook for a loaded chunk (also callable directly): removes marked light blocks of this chunk that no claim keeps
     * and forgets marks whose block is no longer a light block. Returns how many light blocks it removed.
     */
    public static int sweep(ServerLevel level, ChunkPos chunk) {
        OwnedLights o = of(level);
        if (o == null || o.marks.isEmpty()) return 0;
        int removed = 0;
        var inChunk = o.byChunk.get(ChunkPos.pack(chunk.x(), chunk.z()));
        if (inChunk == null) return 0;
        for (long key : inChunk.toArray(new Long[0])) {
            BlockPos pos = BlockPos.of(key);
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.LIGHT)) {
                o.remove(key);
                o.setDirty();
            } else if (CLAIMS.values().stream().noneMatch(c -> c.test(level, pos))) {
                level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                o.remove(key);
                o.setDirty();
                removed++;
            }
        }
        return removed;
    }
}
