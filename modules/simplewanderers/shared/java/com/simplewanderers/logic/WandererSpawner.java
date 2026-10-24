package com.simplewanderers.logic;

import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.WandererRegistry;
import com.simplewanderers.config.WandererConfig;
import com.simplewanderers.entity.VillageWanderer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/**
 * Spawns, respawns and counts the Village Wanderers (concept section 3). Every 200 ticks each player at a village bell is a
 * visit: a village without a record gets its wanderer at once (W6); a village whose wanderer died waits
 * {@code respawnDays} - counted only here, so only while a player stands at the bell (W3). Exactly one per village is
 * kept three ways: the record, a count of loaded wanderers with the village id, and the duplicate check on the entity.
 */
public final class WandererSpawner {
    public static final int INTERVAL = 200;
    /** Misses (visits without finding the recorded wanderer) before the record is declared lost. */
    public static final int LOST_AFTER = 5;

    public enum Result { DISABLED, NO_VILLAGE, EXISTS, WAITING, RAID, NO_SPACE, SPAWNED }

    /** A village found near a player: its id (start chunk), the bell, and its villager type. */
    public record Village(long id, BlockPos bell, String type) {}

    /** Finds the village around a position; replaceable so tests can place a village without generating one. */
    @FunctionalInterface
    public interface Locator {
        Optional<Village> find(ServerLevel level, BlockPos near, int radius);
    }

    public static volatile Locator locator = WandererSpawner::locateVanilla;
    private static final Map<String, Integer> MISSES = new HashMap<>();
    /** Per player: the 8-block cell and game time of the last visit, so a standing or slowly moving player costs one POI search per minute. */
    private static final Map<java.util.UUID, long[]> LAST = new HashMap<>();
    private static final int REVISIT = 1200;

    /** Vanilla villages: a bell (meeting POI) that is part of a village structure; player-built bells do not count. */
    public static Optional<Village> locateVanilla(ServerLevel level, BlockPos near, int radius) {
        Optional<BlockPos> bell = level.getPoiManager().find(p -> p.is(PoiTypes.MEETING), pos -> true, near, radius, PoiManager.Occupancy.ANY);
        // the structure lookup reads chunks: never for a bell whose chunk is not loaded (it would be loaded synchronously)
        if (bell.isEmpty() || !level.hasChunkAt(bell.get())) return Optional.empty();
        StructureStart start = level.structureManager().getStructureWithPieceAt(bell.get(), StructureTags.VILLAGE);
        if (start == null || !start.isValid()) return Optional.empty();
        String type = VillagerType.byBiome(level.getBiome(bell.get())).identifier().getPath();
        return Optional.of(new Village(start.getChunkPos().pack(), bell.get(), type));
    }

    /** Server tick hook of every loader. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 0 || !SimpleWanderers.on()) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || !player.isAlive()) continue;
                long cell = net.minecraft.world.level.ChunkPos.pack(player.blockPosition().getX() >> 3, player.blockPosition().getZ() >> 3);
                long[] last = LAST.get(player.getUUID());
                long now = level.getGameTime();
                if (last != null && last[0] == cell && now - last[1] < REVISIT) continue;
                LAST.put(player.getUUID(), new long[] {cell, now});
                try {
                    visit(level, player.blockPosition(), level.getGameTime());
                } catch (RuntimeException e) {
                    SimpleWanderers.LOG.warn("Village Wanderer visit failed: {}", e.toString());
                }
            }
        }
    }

    /** One visit of a player at {@code near}: spawns, respawns or does nothing. */
    public static Result visit(ServerLevel level, BlockPos near, long gameTime) {
        if (!SimpleWanderers.on()) return Result.DISABLED;
        Optional<Village> found = locator.find(level, near, WandererConfig.villageRadius);
        if (found.isEmpty()) return Result.NO_VILLAGE;
        Village village = found.get();
        WandererVillages data = WandererVillages.get(level.getServer());
        String key = WandererVillages.key(level, village.id());
        WandererVillages.Entry entry = data.get(key);
        List<VillageWanderer> loaded = level.getEntitiesOfClass(VillageWanderer.class, new AABB(village.bell()).inflate(WandererConfig.villageRadius + 32.0),
                w -> w.villageId() == village.id() && w.isAlive());
        if (entry != null && entry.alive()) {
            Entity recorded = level.getEntity(entry.wanderer().get());
            if (recorded != null || !loaded.isEmpty()) {
                MISSES.remove(key);
                return Result.EXISTS;
            }
            // Recorded alive, but nowhere to be seen with a player at the bell: after a few visits it counts as lost.
            if (near.closerThan(village.bell(), 24.0) && MISSES.merge(key, 1, Integer::sum) >= LOST_AFTER) {
                MISSES.remove(key);
                data.markLost(key, gameTime);
            }
            return Result.EXISTS;
        }
        if (!loaded.isEmpty()) {
            // A wanderer without a (valid) record: adopt the first one instead of spawning a second.
            data.setAlive(key, loaded.get(0).getUUID());
            return Result.EXISTS;
        }
        if (entry != null && entry.deadSince() >= 0L) {
            long wait = WandererConfig.respawnTicks();
            if (wait <= 0L || gameTime - entry.deadSince() < wait) return Result.WAITING;
        }
        if (WandererConfig.raidBlocksSpawn && level.getRaids().getNearbyRaid(village.bell(), 9216) != null) return Result.RAID;
        BlockPos at = findSpot(level, village.bell(), 8, level.getRandom());
        if (at == null) return Result.NO_SPACE;
        VillageWanderer wanderer = WandererRegistry.WANDERER.spawn(level, at, EntitySpawnReason.EVENT);
        if (wanderer == null) return Result.NO_SPACE;
        wanderer.bind(village.id(), village.type(), village.bell());
        if (!data.setAlive(key, wanderer.getUUID())) {
            wanderer.discard();
            return Result.NO_SPACE;
        }
        if (WandererConfig.horse && !wanderer.spawnHorse(level)) wanderer.horseLostAt(gameTime - WandererConfig.respawnTicks());
        return Result.SPAWNED;
    }

    /** A standing spot near {@code around}: solid floor, two free blocks above, no liquid. Null after 12 tries. */
    public static BlockPos findSpot(ServerLevel level, BlockPos around, int radius, RandomSource random) {
        for (int i = 0; i < 12; i++) {
            int x = around.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = around.getZ() + random.nextInt(radius * 2 + 1) - radius;
            if (!level.hasChunkAt(new BlockPos(x, around.getY(), z))) continue; // reading it would load the chunk
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos feet = new BlockPos(x, around.getY() + dy, z);
                BlockState floor = level.getBlockState(feet.below());
                if (floor.isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP) && level.getFluidState(feet).isEmpty()
                        && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && level.getFluidState(feet.above()).isEmpty()) {
                    return feet;
                }
            }
        }
        return null;
    }

    /** Forgets transient state (tests). */
    public static void reset() {
        MISSES.clear();
        LAST.clear();
        locator = WandererSpawner::locateVanilla;
    }

    private WandererSpawner() {}
}
