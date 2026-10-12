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
    private FearServer() {}

    public static void tick(MinecraftServer server) {
        if (!MobsRegistry.on()) return;
        int now = server.getTickCount();
        if (now % 10 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) FearAura.cure(p);
        if (now % FearLogic.WATCHER_ROLL_INTERVAL != 0 || !MobsConfig.hollowWatcherSpawns) return;
        ServerLevel end = server.getLevel(Level.END);
        if (end == null || end.getDifficulty() == Difficulty.PEACEFUL) return;
        for (ServerPlayer p : end.players()) {
            if (!p.isSpectator()) trySpawn(end, p);
        }
    }

    static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        var rnd = level.getRandom();
        double a = rnd.nextDouble() * Math.PI * 2, r = 24 + rnd.nextDouble() * 16;
        BlockPos column = BlockPos.containing(player.getX() + Math.cos(a) * r, 0, player.getZ() + Math.sin(a) * r);
        if (!level.hasChunkAt(column)) return false;
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        boolean endStone = level.getBlockState(pos.below()).is(Blocks.END_STONE);
        boolean room = level.noCollision(FearMobs.WATCHER.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        int near = level.getEntitiesOfClass(HollowWatcherEntity.class, player.getBoundingBox().inflate(128.0)).size();
        double fromOrigin = Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ());
        if (!FearLogic.mayWatcherSpawn(true, true, fromOrigin, endStone, room, near, rnd.nextFloat(), MobsConfig.hollowWatcherChance)) return false;
        return FearMobs.WATCHER.spawn(level, pos, EntitySpawnReason.NATURAL) != null;
    }
}
