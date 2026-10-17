package com.simplebuilding.modules.simpletweaks.play.crowd;

import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * T31 (off by default): mobs per chunk. Every {@code scanSeconds} the mobs of every loaded chunk are counted; a chunk
 * over {@code perChunk} is reported (once per hour) to the claim owner (Simple Tweaks claims) or else to players nearby
 * and to operators, and with {@code blockSpawns} no new mob joins that chunk (breeding, spawners, natural spawns, also
 * /summon) until it is under the limit again. Nothing is deleted silently.
 */
public final class EntityLimit {
    record Key(ResourceKey<Level> dimension, long chunk) {}
    static final Map<Key, Integer> OVER = new HashMap<>();
    static final Map<Key, Long> TOLD = new HashMap<>();

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().entityLimit().enabled(); }

    /** May this entity be added to the level (mixin on {@code ServerLevel.addFreshEntity})? */
    public static boolean admit(ServerLevel level, Entity e) {
        if (OVER.isEmpty() || !(e instanceof Mob) || !active() || !Play.config().entityLimit().blockSpawns()) return true;
        return !OVER.containsKey(new Key(level.dimension(), ChunkPos.pack(e.blockPosition())));
    }
    public static boolean over(ServerLevel level, BlockPos pos) { return OVER.containsKey(new Key(level.dimension(), ChunkPos.pack(pos))); }

    public static void tick(MinecraftServer server) {
        if (!active()) { OVER.clear(); return; }
        if (server.getTickCount() % (Play.config().entityLimit().scanSeconds() * 20) != 0) return;
        scan(server);
    }
    /** Counts all loaded levels now; returns the chunks over the limit. */
    public static Map<Key, Integer> scan(MinecraftServer server) {
        int limit = Play.config().entityLimit().perChunk();
        OVER.clear();
        for (var level : server.getAllLevels()) {
            var counts = new HashMap<Long, Integer>();
            for (var e : level.getAllEntities()) if (e instanceof Mob && e.isAlive()) counts.merge(ChunkPos.pack(e.blockPosition()), 1, Integer::sum);
            counts.forEach((chunk, n) -> { if (n > limit) OVER.put(new Key(level.dimension(), chunk), n); });
        }
        long now = System.currentTimeMillis();
        OVER.forEach((key, n) -> {
            Long told = TOLD.get(key);
            if (told != null && now - told < 3_600_000L) return;
            TOLD.put(key, now);
            notify(server, key, n, limit);
        });
        return OVER;
    }
    static void notify(MinecraftServer server, Key key, int n, int limit) {
        var level = server.getLevel(key.dimension());
        if (level == null) return;
        int cx = ChunkPos.getX(key.chunk()), cz = ChunkPos.getZ(key.chunk());
        var center = new BlockPos(cx * 16 + 8, 64, cz * 16 + 8);
        var msg = PlayText.t("crowd.over", n, limit, PlayText.coords(center.getX(), center.getY(), center.getZ()), PlayText.dimension(key.dimension())).withStyle(ChatFormatting.YELLOW);
        var told = new HashSet<ServerPlayer>();
        if (com.simplebuilding.modules.simpletweaks.claims.Claims.enabled(server)) {
            com.simplebuilding.modules.simpletweaks.claims.Claims.owner(level, center).map(id -> server.getPlayerList().getPlayer(id)).ifPresent(told::add);
        }
        if (told.isEmpty()) for (var p : level.players()) {
            int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
            if (Math.abs(pcx - cx) <= 4 && Math.abs(pcz - cz) <= 4) told.add(p);
        }
        for (var p : server.getPlayerList().getPlayers()) if (PlayPerms.has(p, 2)) told.add(p);
        for (var p : told) p.sendSystemMessage(msg);
        org.slf4j.LoggerFactory.getLogger("simpletweaks-play").info("Chunk {} {} in {} has {} mobs (limit {})", cx, cz, key.dimension().identifier(), n, limit);
    }
    public static void reset() { OVER.clear(); TOLD.clear(); }
    private EntityLimit() {}
}
