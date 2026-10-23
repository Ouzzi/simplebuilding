package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig;
import com.simplebuilding.modules.simpletweaks.claims.ClaimStore;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The pictures of claimed chunks shown by the deed (SP22-8 §3.3): 16x16 Vanilla map colours (packed {@link MapColor}
 * id with brightness, one byte per pixel) and the overworld game time they were taken. Purely cosmetic and kept in its
 * own file {@code <world>/simpletweaks-claim-maps.dat}, apart from the ledger: a broken picture file never locks a
 * claim, it is renamed to {@code .broken-<ms>} and the deed starts without pictures.
 *
 * <p><b>Nothing is loaded for a picture.</b> Only chunks the server has loaded anyway ({@code getChunkNow}) are read,
 * at most {@value #PER_SCAN} every {@value #SCAN_TICKS} ticks, chunks without a picture first, then the oldest, and a
 * chunk at most every {@value #REFRESH_TICKS} ticks. Claiming through the deed takes the picture at once (the player
 * stands there).
 */
public final class ClaimPictures {
    public static final int SIZE = 16, PIXELS = SIZE * SIZE;
    public static final int SCAN_TICKS = 100, SAVE_TICKS = 1200, REFRESH_TICKS = 6000, PER_SCAN = 4;
    public static final String FILE = "simpletweaks-claim-maps.dat";
    private static final long MAX_BYTES = 16L * 1024 * 1024;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simpletweaks-claims");
    private static final Map<MinecraftServer,ClaimPictures> SERVERS = new IdentityHashMap<>();

    /** One picture: {@code pixels} = 256 packed map colours (row by row, north first), {@code time} = overworld game time. */
    public record Picture(long time, byte[] pixels) {}

    private final Path file;
    private final Map<ClaimStore.Key,Picture> pictures = new HashMap<>();
    private boolean dirty;

    ClaimPictures(Path file) { this.file = file; load(); }

    /** The pictures of this server (loaded on first use), or null while claims are off. */
    public static synchronized ClaimPictures get(MinecraftServer server) {
        if (!Claims.enabled(server)) return null;
        return SERVERS.computeIfAbsent(server, s -> new ClaimPictures(s.getWorldPath(LevelResource.ROOT).resolve(FILE)));
    }
    /** Tests: use pictures from another file for this server ({@code null} forgets them); returns the previous ones. */
    public static synchronized ClaimPictures install(MinecraftServer server, ClaimPictures pictures) {
        return pictures == null ? SERVERS.remove(server) : SERVERS.put(server, pictures);
    }
    /** Tests: pictures of a file without a server. */
    public static ClaimPictures open(Path file) { return new ClaimPictures(file); }
    /** Server stop: write what changed and forget the server. */
    public static synchronized void stop(MinecraftServer server) {
        var pictures = SERVERS.remove(server);
        if (pictures != null) pictures.save();
    }

    /** End of every server tick: scan every {@value #SCAN_TICKS} ticks, save every {@value #SAVE_TICKS}. */
    public static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        if (t % SCAN_TICKS != 0 || !Claims.enabled(server)) return;
        var claims = Claims.get(server);
        var pictures = get(server);
        if (claims == null || pictures == null || claims.locked()) return;
        try {
            pictures.scan(server, claims.view(), server.overworld().getGameTime());
            if (t % SAVE_TICKS == 0) pictures.save();
        } catch (RuntimeException e) { LOG.warn("Claim pictures: scan failed, skipped", e); }
    }

    public Picture picture(ClaimStore.Key key) { return pictures.get(key); }
    public int size() { return pictures.size(); }

    /**
     * Takes up to {@value #PER_SCAN} pictures of claimed chunks that are loaded right now and drops the pictures of
     * chunks that are no longer claimed. Returns how many pictures were taken.
     */
    public int scan(MinecraftServer server, Map<ClaimStore.Key,ClaimStore.Claim> claimed, long now) {
        if (pictures.keySet().removeIf(k -> !claimed.containsKey(k))) dirty = true;
        var due = new ArrayList<ClaimStore.Key>();
        var levels = new HashMap<String,ServerLevel>();
        for (var key : claimed.keySet()) {
            Picture old = pictures.get(key);
            if (old != null && now - old.time() < REFRESH_TICKS && now >= old.time()) continue;
            ServerLevel level = levels.computeIfAbsent(key.dimension(), d -> level(server, d));
            if (level == null || level.getChunkSource().getChunkNow(ChunkPos.getX(key.chunk()), ChunkPos.getZ(key.chunk())) == null) continue;
            due.add(key);
        }
        due.sort(Comparator.comparingLong(k -> pictures.containsKey(k) ? pictures.get(k).time() : Long.MIN_VALUE));
        int taken = 0;
        for (var key : due) {
            if (taken >= PER_SCAN) break;
            ServerLevel level = levels.get(key.dimension());
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key.chunk()), ChunkPos.getZ(key.chunk()));
            if (chunk != null && put(key, sample(level, chunk), now)) taken++;
        }
        return taken;
    }

    /** Takes the picture of a just claimed chunk if it is loaded (it is: the claimer stands there). */
    public static void takeNow(ServerLevel level, ClaimStore.Key key) {
        try {
            var pictures = get(level.getServer());
            if (pictures == null) return;
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key.chunk()), ChunkPos.getZ(key.chunk()));
            if (chunk != null) pictures.put(key, sample(level, chunk), level.getServer().overworld().getGameTime());
        } catch (RuntimeException e) { LOG.warn("Claim pictures: picture of {} failed", key, e); }
    }

    boolean put(ClaimStore.Key key, byte[] pixels, long now) {
        if (pixels == null || pixels.length != PIXELS) return false;
        if (!pictures.containsKey(key) && pictures.size() >= ClaimConfig.MAX_GLOBAL) return false;
        pictures.put(key, new Picture(now, pixels));
        dirty = true;
        return true;
    }

    private static ServerLevel level(MinecraftServer server, String dimension) {
        var id = Identifier.tryParse(dimension);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    // ------------------------------------------------------------------ sampling (Vanilla map rules, own copy, C10)

    /** The 16x16 map colours of a loaded chunk, shaded like a Vanilla map (brighter where the ground rises to the north). */
    public static byte[] sample(ServerLevel level, LevelChunk chunk) {
        byte[] out = new byte[PIXELS];
        boolean ceiling = level.dimensionType().hasCeiling();
        int minY = level.getMinY(), maxY = minY + level.getHeight() - 1;
        var pos = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < SIZE; x++) {
            int previous = Integer.MIN_VALUE;
            for (int z = 0; z < SIZE; z++) {
                int y = ceiling ? Math.min(maxY, level.getSeaLevel() + 32) : chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                pos.set(baseX + x, y, baseZ + z);
                if (ceiling) {
                    // under a roof: out of the rock first (at most 16 up), then down to the first floor
                    for (int up = 0; up < 16 && pos.getY() < maxY && chunk.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE; up++) pos.move(0, 1, 0);
                }
                MapColor color = MapColor.NONE;
                while (pos.getY() > minY) {
                    color = chunk.getBlockState(pos).getMapColor(level, pos);
                    if (color != MapColor.NONE) break;
                    pos.move(0, -1, 0);
                }
                if (color == MapColor.NONE) { out[z * SIZE + x] = 0; previous = Integer.MIN_VALUE; continue; }
                int surface = pos.getY(), depth = 0;
                if (color == MapColor.WATER) {
                    var below = pos.mutable();
                    while (depth < 10 && below.getY() > minY && chunk.getBlockState(below).getMapColor(level, below) == MapColor.WATER) { depth++; below.move(0, -1, 0); }
                }
                out[z * SIZE + x] = color.getPackedId(brightness(color, surface, previous, depth, baseX + x, baseZ + z));
                previous = surface;
            }
        }
        return out;
    }

    /** Vanilla's map shading: water by depth, land by the step to the pixel north of it, with the checker dither. */
    static MapColor.Brightness brightness(MapColor color, int y, int previous, int waterDepth, int x, int z) {
        int dither = (x + z) & 1;
        if (color == MapColor.WATER) {
            double diff = waterDepth * 0.1 + dither * 0.2;
            return diff < 0.5 ? MapColor.Brightness.HIGH : diff > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        }
        if (previous == Integer.MIN_VALUE) previous = y;
        double diff = (y - previous) * 4.0 / 5.0 + (dither - 0.5) * 0.4;
        return diff > 0.6 ? MapColor.Brightness.HIGH : diff < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    // ------------------------------------------------------------------ file

    private void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.isSymbolicLink(file) || Files.size(file) > MAX_BYTES) throw new IOException("Unsafe claim picture file");
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.create(MAX_BYTES));
            for (var tag : root.getListOrEmpty("pictures")) {
                if (!(tag instanceof CompoundTag c)) throw new IOException("Bad claim picture entry");
                var key = new ClaimStore.Key(c.getString("d").orElseThrow(() -> new IOException("Missing dimension")), c.getLong("c").orElseThrow(() -> new IOException("Missing chunk")));
                byte[] pixels = c.getByteArray("p").orElseThrow(() -> new IOException("Missing pixels"));
                if (pixels.length != PIXELS) throw new IOException("Bad picture size");
                if (pictures.size() < ClaimConfig.MAX_GLOBAL) pictures.put(key, new Picture(c.getLongOr("t", 0), pixels));
            }
        } catch (IOException | RuntimeException e) {
            pictures.clear();
            Path broken = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
            try { Files.move(file, broken); } catch (IOException moveFailed) { LOG.warn("Claim pictures: could not move the broken file aside", moveFailed); }
            LOG.warn("Claim pictures were unreadable and start empty (claims are not affected); kept as {}", broken.getFileName(), e);
        }
    }

    /** Writes the pictures if something changed (atomic: temporary file, then move). */
    public void save() {
        if (!dirty) return;
        var list = new ListTag();
        pictures.forEach((key, p) -> {
            var c = new CompoundTag();
            c.putString("d", key.dimension()); c.putLong("c", key.chunk()); c.putLong("t", p.time()); c.putByteArray("p", p.pixels());
            list.add(c);
        });
        var root = new CompoundTag();
        root.putInt("version", 1);
        root.put("pictures", list);
        try {
            Path absolute = file.toAbsolutePath();
            Files.createDirectories(absolute.getParent());
            Path temp = Files.createTempFile(absolute.getParent(), ".claim-maps-", ".tmp");
            try {
                NbtIo.writeCompressed(root, temp);
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temp); }
            dirty = false;
        } catch (IOException e) { LOG.warn("Claim pictures could not be saved (claims are not affected)", e); }
    }
}
