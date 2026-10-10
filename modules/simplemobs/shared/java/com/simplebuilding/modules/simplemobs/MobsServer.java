package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/** Natural spawning: a rare roll per player every 30 s, decided by {@link DeceiverLogic#mayNaturallySpawn}. */
public final class MobsServer {
    private static final int ROLL_INTERVAL = 600;

    private MobsServer() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % ROLL_INTERVAL != 0) return;
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            trySpawn(level, p);
        }
    }

    static void trySpawn(ServerLevel level, ServerPlayer player) {
        var rnd = level.getRandom();
        double a = rnd.nextDouble() * Math.PI * 2, r = 24 + rnd.nextDouble() * 16;
        BlockPos pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(player.getX() + Math.cos(a) * r, 0, player.getZ() + Math.sin(a) * r));
        if (!level.hasChunkAt(pos)) return;
        boolean dark = level.getBiome(pos).is(Biomes.DARK_FOREST);
        var sm = level.structureManager();
        boolean outpost = sm.getStructureWithPieceAt(pos, h -> h.is(BuiltinStructures.PILLAGER_OUTPOST)).isValid();
        boolean village = sm.getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid();
        int light = level.getMaxLocalRawBrightness(pos);
        int near = level.getEntitiesOfClass(DeceiverEntity.class, player.getBoundingBox().inflate(128.0)).size();
        if (!DeceiverLogic.mayNaturallySpawn(level.isDarkOutside(), dark, outpost, village, light, near, rnd.nextFloat())) return;
        MobsRegistry.DECEIVER.spawn(level, pos, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
    }
}
