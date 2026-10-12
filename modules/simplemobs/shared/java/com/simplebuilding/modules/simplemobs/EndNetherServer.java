package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;

/** Shellker gateway guards and the Soulfire Blaze's natural spawning. */
public final class EndNetherServer {
    private EndNetherServer() {}

    /** Puts four Shellkers into the open cells around a freshly placed End gateway (they sit on the bedrock below). */
    public static int guardGateway(ServerLevelAccessor level, BlockPos gateway) {
        if (!MobsConfig.shellkerGatewayGuards) return 0;
        ServerLevel server = level.getLevel();
        if (server == null || server.dimension() != Level.END) return 0;
        return placeGuards(level, gateway);
    }

    /** The guard placement itself (any dimension; tests use the Overworld). */
    public static int placeGuards(ServerLevelAccessor level, BlockPos gateway) {
        ServerLevel server = level.getLevel();
        int placed = 0;
        for (BlockPos cell : EndNetherLogic.gatewayGuardCells(gateway)) {
            if (!level.getBlockState(cell).isAir()) continue;
            ShellkerEntity guard = EndNetherMobs.SHELLKER.create(server, EntitySpawnReason.STRUCTURE);
            if (guard == null) continue;
            guard.attachTo(Direction.DOWN);
            guard.snapTo(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, 0.0F, 0.0F);
            guard.setPersistenceRequired();
            level.addFreshEntity(guard);
            placed++;
        }
        return placed;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % EndNetherLogic.SOUL_ROLL_INTERVAL != 0 || !MobsConfig.soulfireBlazeSpawns) return;
        ServerLevel level = server.getLevel(Level.NETHER);
        if (level == null || level.getDifficulty() == Difficulty.PEACEFUL) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            trySoulSpawn(level, p);
        }
    }

    static void trySoulSpawn(ServerLevel level, ServerPlayer player) {
        var rnd = level.getRandom();
        double a = rnd.nextDouble() * Math.PI * 2, r = 16 + rnd.nextDouble() * 24;
        BlockPos column = BlockPos.containing(player.getX() + Math.cos(a) * r, player.getY(), player.getZ() + Math.sin(a) * r);
        if (!level.hasChunkAt(column)) return;
        BlockPos ground = null;
        for (int dy = 8; dy >= -16 && ground == null; dy--) {
            BlockPos below = column.offset(0, dy - 1, 0);
            BlockPos at = column.offset(0, dy, 0);
            if (level.getBlockState(below).is(BlockTags.SOUL_FIRE_BASE_BLOCKS) && level.isEmptyBlock(at) && level.isEmptyBlock(at.above())) ground = at;
        }
        boolean valley = level.getBiome(ground != null ? ground : column).is(Biomes.SOUL_SAND_VALLEY);
        int near = level.getEntitiesOfClass(SoulfireBlaze.class, player.getBoundingBox().inflate(48.0)).size();
        if (!EndNetherLogic.maySoulSpawn(true, valley, ground != null, near, rnd.nextFloat(), MobsConfig.soulfireBlazeChance)) return;
        int pack = 1 + rnd.nextInt(2);
        for (int i = 0; i < pack; i++) EndNetherMobs.SOULFIRE_BLAZE.spawn(level, ground, EntitySpawnReason.NATURAL);
    }
}
