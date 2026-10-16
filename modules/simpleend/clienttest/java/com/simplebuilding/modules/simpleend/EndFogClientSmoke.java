package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.world.EndBiomes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * N1 A/B/C screenshots (docs/ai/BERICHT-CLAUDE-ENDFOG.md): the rim of a forest in the End seen from outside at ground level,
 * from high above and from just inside, each with the client fog wall off ("before") and on ("after").
 */
public final class EndFogClientSmoke implements FabricClientGameTest {
    private static void cmd(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static boolean forest(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> b) {
        return b.is(EndBiomes.WATCHER_CORE) || b.is(EndBiomes.ASH_VALLEY_FRINGE) || b.is(EndBiomes.DAWNLANDS_FRINGE);
    }

    @Override public void runTest(ClientGameTestContext context) {
        // [edgeX, edgeZ, outwardDirX, outwardDirZ] of a forest rim (outward = away from the forest)
        double[] rim = new double[4];
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getServer().runOnServer(server -> {
                cmd(server, "gamemode creative @a");
                ServerLevel end = server.getLevel(Level.END);
                var resolver = end.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(end.getChunkSource().randomState());
                int[] hit = null;
                for (int r = 0; r <= 12000 && hit == null; r += 64)
                    for (int a = 0; a < Math.max(1, r / 16) && hit == null; a++) {
                        double ang = a * Math.PI * 2 / Math.max(1, r / 16);
                        int x = 1600 + (int) (Math.cos(ang) * r), z = (int) (Math.sin(ang) * r);
                        if (forest(resolver.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z)))) hit = new int[] {x, z};
                    }
                if (hit == null) throw new AssertionError("no forest biome within 12000 blocks");
                for (int k = 0; k < 8; k++) {
                    double dx = Math.cos(k * Math.PI / 4), dz = Math.sin(k * Math.PI / 4);
                    int x = hit[0], z = hit[1];
                    for (int s = 0; s < 1500; s += 8) {
                        int nx = (int) (hit[0] + dx * s), nz = (int) (hit[1] + dz * s);
                        if (!forest(resolver.getNoiseBiome(QuartPos.fromBlock(nx), QuartPos.fromBlock(64), QuartPos.fromBlock(nz)))) break;
                        x = nx; z = nz;
                    }
                    if (Math.hypot(x - hit[0], z - hit[1]) > 40 || k == 7) {
                        rim[0] = x; rim[1] = z; rim[2] = dx; rim[3] = dz;
                        break;
                    }
                }
                System.out.println("[simpleend fog] rim " + java.util.Arrays.toString(rim));
                cmd(server, "execute in minecraft:the_end run tp @a " + rim[0] + " 120 " + rim[1]);
            });
            context.waitTicks(120);
            world.getConnection().waitForChunksRender();
            int[] y = new int[1];
            world.getServer().runOnServer(server -> {
                ServerLevel end = server.getLevel(Level.END);
                int best = 40;
                for (int dx = -30; dx <= 30; dx += 6)
                    for (int dz = -30; dz <= 30; dz += 6) best = Math.max(best, end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) rim[0] + dx, (int) rim[1] + dz));
                y[0] = best;
            });
            // look direction toward the forest = -outward; yaw 0 = +Z, -90 = +X
            float yaw = (float) Math.toDegrees(Math.atan2(rim[2], -rim[3]));
            shot(context, world, rim, y[0] + 3, 90, yaw, 0, false, "simpleend-fogwall-ground-before");
            shot(context, world, rim, y[0] + 3, 90, yaw, 0, true, "simpleend-fogwall-ground-after");
            shot(context, world, rim, y[0] + 70, 60, yaw, 35, false, "simpleend-fogwall-above-before");
            shot(context, world, rim, y[0] + 70, 60, yaw, 35, true, "simpleend-fogwall-above-after");
            shot(context, world, rim, y[0] + 6, -10, yaw, 5, true, "simpleend-fogwall-rim-inside");
            SimpleEndConfig.forestFogWall = true;
        }
    }

    private static void shot(ClientGameTestContext context, TestSingleplayerContext world, double[] rim, int y, double out, float yaw, float pitch,
                             boolean wall, String name) {
        SimpleEndConfig.forestFogWall = wall;
        world.getServer().runOnServer(server -> cmd(server, "execute in minecraft:the_end run tp @a " + (rim[0] + rim[2] * out) + " " + y + " "
                + (rim[1] + rim[3] * out) + " " + yaw + " " + pitch));
        context.waitTicks(90);
        world.getConnection().waitForChunksRender();
        context.takeScreenshot(name);
    }
}
