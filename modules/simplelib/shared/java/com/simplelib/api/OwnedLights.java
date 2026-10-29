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
import net.minecraft.server.MinecraftServer;
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
 * (at the end of that server tick, {@link #chunkLoaded}) removes the marked light blocks that no live carrier claims ({@link #claim}). A Vanilla light block nobody marked
 * (placed by a player or command) is never touched. Cost: one set lookup per chunk load, no world scan.
 * Server thread only.
 */
public final class OwnedLights extends SavedData {
    private static final Map<String, BiPredicate<Level, BlockPos>> CLAIMS = new ConcurrentHashMap<>();
    public static final Codec<OwnedLights> CODEC = Codec.LONG.listOf().xmap(OwnedLights::new, l -> new ArrayList<>(l.marks)).fieldOf("marks").codec();
    public static final SavedDataType<OwnedLights> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SimpleLib.MOD_ID, "owned_lights"), OwnedLights::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final java.util.Set<Long> marks = new java.util.LinkedHashSet<>();

    public OwnedLights() {}

    private OwnedLights(List<Long> loaded) {
        marks.addAll(loaded);
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
        if (o != null && o.marks.add(pos.asLong())) o.setDirty();
    }

    /** Call when a mod removed its light block at {@code pos}. */
    public static void unmark(Level level, BlockPos pos) {
        OwnedLights o = of(level);
        if (o != null && o.marks.remove(pos.asLong())) o.setDirty();
    }

    public static boolean isMarked(Level level, BlockPos pos) {
        OwnedLights o = of(level);
        return o != null && o.marks.contains(pos.asLong());
    }

    /** A loaded chunk waiting for its sweep at the end of the server tick. */
    private record Pending(ServerLevel level, ChunkPos chunk) {}
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final int PENDING_MAX = 4096;

    /**
     * Loader hook for a chunk that is just loading (Fabric CHUNK_LOAD): only remembers it. BS27-1: the event runs inside the
     * chunk's FULL step, before its future is done, so {@code level.getBlockState} on that chunk waits for itself forever
     * ({@code ServerChunkCache.getChunk} -> {@code managedBlock}) - the server thread hangs, nothing loads, saving hangs.
     * {@link #tick} sweeps it once it is fully loaded.
     */
    public static void chunkLoaded(ServerLevel level, ChunkPos chunk) {
        OwnedLights o = of(level);
        if (o != null && !o.marks.isEmpty() && PENDING.size() < PENDING_MAX) PENDING.add(new Pending(level, chunk));
    }

    /** End of a server tick: sweeps the remembered chunks that are fully loaded now (an unloaded one keeps its marks for the next load). */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        List<Pending> now = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Pending p : now) {
            if (p.level().getServer() == server && p.level().getChunkSource().getChunkNow(p.chunk().x(), p.chunk().z()) != null) sweep(p.level(), p.chunk());
        }
    }

    /** Server stop: forget the remembered chunks. */
    public static void stopped() {
        PENDING.clear();
    }

    /** Chunks waiting for their sweep (tests). */
    public static int pending() {
        return PENDING.size();
    }

    /**
     * Sweep of a fully loaded chunk (from {@link #tick}, also callable directly; never from inside a chunk load): removes marked light blocks of this chunk that no claim keeps
     * and forgets marks whose block is no longer a light block. Returns how many light blocks it removed.
     */
    public static int sweep(ServerLevel level, ChunkPos chunk) {
        OwnedLights o = of(level);
        if (o == null || o.marks.isEmpty()) return 0;
        int removed = 0;
        for (long key : o.marks.toArray(new Long[0])) {
            BlockPos pos = BlockPos.of(key);
            if (pos.getX() >> 4 != chunk.x() || pos.getZ() >> 4 != chunk.z()) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.LIGHT)) {
                o.marks.remove(key);
                o.setDirty();
            } else if (CLAIMS.values().stream().noneMatch(c -> c.test(level, pos))) {
                level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                o.marks.remove(key);
                o.setDirty();
                removed++;
            }
        }
        return removed;
    }
}
