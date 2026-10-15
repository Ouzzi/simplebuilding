package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Fear mobs, server side: immune players lose Shivering, and the Hollow Watcher's rare spawn on the outer End islands. */
public final class FearServer {
    /** Biomes where it prefers to spawn; Simple End provides the tag, without it nothing matches (no hard dependency). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> WATCHER_CORE = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleend", "watcher_core"));

    /** Ticks each player has been in a core biome without a break (N66; reset on leaving, dying, other dimension, logout). */
    private static final java.util.Map<java.util.UUID, Integer> FOREST_TICKS = new java.util.HashMap<>();

    private FearServer() {}

    /** Adds {@code ticks} to the player's uninterrupted time in a core biome, or resets it; returns the new value. */
    static int trackForest(java.util.Map<java.util.UUID, Integer> map, java.util.UUID id, boolean inCore, int ticks) {
        if (!inCore) { map.remove(id); return 0; }
        return map.merge(id, ticks, Integer::sum);
    }

    /** Seconds the player has been in a core biome without a break. */
    public static int forestSeconds(java.util.UUID id) {
        return FOREST_TICKS.getOrDefault(id, 0) / 20;
    }

    public static void tick(MinecraftServer server) {
        if (!MobsRegistry.on()) return;
        int now = server.getTickCount();
        if (now % 10 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!FearAura.cure(p)) FearAura.decay(p);
        }
        ServerLevel end = server.getLevel(Level.END);
        if (now % 20 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean inCore = end != null && p.level() == end && p.isAlive() && !p.isSpectator() && end.getBiome(p.blockPosition()).is(WATCHER_CORE);
            trackForest(FOREST_TICKS, p.getUUID(), inCore, 20);
        }
        if (now % 1200 == 0) FOREST_TICKS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        if (now % FearLogic.WATCHER_ROLL_INTERVAL != 0 || !MobsConfig.hollowWatcherSpawns) return;
        if (end == null || end.getDifficulty() == Difficulty.PEACEFUL) return;
        for (ServerPlayer p : end.players()) {
            if (p.isSpectator()) continue;
            boolean dwell = FOREST_TICKS.getOrDefault(p.getUUID(), 0) >= MobsConfig.hollowWatcherForestSeconds * 20;
            // a dwelling player gets several tries per roll (one spot is often void or occupied)
            for (int i = dwell ? 6 : 1; i > 0; i--) if (trySpawn(end, p, dwell)) break;
        }
    }

    static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        return trySpawn(level, player, false);
    }

    static boolean trySpawn(ServerLevel level, ServerPlayer player, boolean dwell) {
        var rnd = level.getRandom();
        double a = rnd.nextDouble() * Math.PI * 2, r = 24 + rnd.nextDouble() * 16;
        BlockPos column = BlockPos.containing(player.getX() + Math.cos(a) * r, 0, player.getZ() + Math.sin(a) * r);
        if (!level.hasChunkAt(column)) return false;
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        boolean endStone = level.getBlockState(pos.below()).is(Blocks.END_STONE);
        boolean room = level.noCollision(FearMobs.WATCHER.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        int near = level.getEntitiesOfClass(HollowWatcherEntity.class, player.getBoundingBox().inflate(128.0)).size();
        boolean core = level.getBiome(pos).is(WATCHER_CORE);
        double fromOrigin = Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ());
        if (!FearLogic.mayWatcherSpawn(true, true, fromOrigin, core, endStone, room, near, rnd.nextFloat(), MobsConfig.hollowWatcherChance,
                dwell, MobsConfig.hollowWatcherForestChance)) return false;
        if (FearMobs.WATCHER.spawn(level, pos, EntitySpawnReason.NATURAL) == null) return false;
        if (dwell) FOREST_TICKS.put(player.getUUID(), 0);   // it came: the next wait starts anew
        return true;
    }
}
