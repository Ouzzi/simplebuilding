package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.world.EndBiomes;
import com.simplebuilding.modules.simpleend.world.EndTrees;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Screenshots for the owner review (docs/ai/PLAN-SIMPLE-END-2026-10-12.md): a garden of both woods (stick trees,
 * scrub, the full block set, End grass), the gigantic wood trees (big, huge, colossal), a close look at the leaves and
 * visits to the foggy cores of Ash Valley and Dawnlands and to a fringe in the End.
 */
public final class EndClientSmoke implements FabricClientGameTest {
    private static void cmd(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static void fly(MinecraftServer server) {
        ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
        cmd(server, "gamemode creative @a");
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    @Override public void runTest(ClientGameTestContext context) {
        BlockPos[] garden = new BlockPos[1];
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                fly(server);
                cmd(server, "time set noon");
                cmd(server, "gamerule doDaylightCycle false");
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = server.getLevel(Level.OVERWORLD);
                BlockPos o = p.blockPosition().offset(6, 0, 6);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, o.getX(), o.getZ());
                BlockPos base = new BlockPos(o.getX(), y, o.getZ());
                garden[0] = base;
                for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(30, -1, 20))) level.setBlockAndUpdate(q, Blocks.END_STONE.defaultBlockState());
                for (BlockPos q : BlockPos.betweenClosed(base.offset(40, -1, -10), base.offset(110, -1, 50))) level.setBlock(q, Blocks.END_STONE.defaultBlockState(), 18);
                RandomSource r = RandomSource.create(7);
                // Umbau 13.10.: wood trees (outside the End) big / huge / colossal, stick trees as in the End, scrub
                EndTrees.grow(level, base.offset(52, 0, 22), EndWood.ASHWOOD, EndTrees.Size.BIG, false, r);
                EndTrees.grow(level, base.offset(66, 0, 4), EndWood.DUSKWOOD, EndTrees.Size.HUGE, false, r);
                EndTrees.grow(level, base.offset(92, 0, 20), EndWood.ASHWOOD, EndTrees.Size.COLOSSAL, false, r);
                EndTrees.grow(level, base.offset(4, 0, 14), EndWood.ASHWOOD, EndTrees.Size.BIG, true, r);
                EndTrees.grow(level, base.offset(24, 0, 14), EndWood.DUSKWOOD, EndTrees.Size.BIG, true, r);
                EndTrees.growScrub(level, base.offset(22, 0, 4), EndWood.ASHWOOD, r, false);
                EndTrees.growScrub(level, base.offset(26, 0, 6), EndWood.DUSKWOOD, r, false);
                int x = 0;
                for (EndWood w : EndWood.ALL) {
                    for (var b : new net.minecraft.world.level.block.Block[] {w.log, w.wood, w.strippedLog, w.planks, w.stairs, w.slab, w.fence,
                            w.fenceGate, w.door, w.trapdoor, w.sign, w.leaves, w.sticks, w.bud}) {
                        BlockState s = b.defaultBlockState();
                        level.setBlockAndUpdate(base.offset(x % 16, 0, -1 - x / 16 * 2), s);
                        x++;
                    }
                    x = 16;
                }
                for (int i = 0; i < 30; i++) {
                    BlockPos g = base.offset(r.nextInt(28), 0, r.nextInt(18));
                    if (level.getBlockState(g).isAir()) level.setBlockAndUpdate(g, (i % 5 == 0 ? EndRegistry.END_BUSH : EndRegistry.END_GRASS).defaultBlockState());
                }
                for (int i = 0; i < 6; i++) {
                    var mite = EndRegistry.NIBBLEMITE.create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
                    mite.setVariant(i % 2);
                    mite.snapTo(base.getX() + 12 + i, base.getY(), base.getZ() + 1 + (i % 3), 180f, 0f);
                    mite.setNoAi(true);
                    level.addFreshEntity(mite);
                    level.setBlockAndUpdate(base.offset(12 + i, -1, 2 + (i % 2)), EndRegistry.BURROWED_END_STONE.defaultBlockState());
                }
                cmd(server, "tp @a " + (base.getX() + 14) + " " + (base.getY() + 9) + " " + (base.getZ() - 14) + " 0 25");
            });
            context.waitTicks(60);
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("simpleend-garden");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                var mite = server.getLevel(Level.OVERWORLD).getEntities(EndRegistry.NIBBLEMITE, e -> true).getFirst();
                cmd(server, "tp @a " + mite.getX() + " " + (mite.getY() + 1.6) + " " + (mite.getZ() - 3.2) + " 0 30");
            });
            context.waitTicks(30);
            context.takeScreenshot("simpleend-nibblemite");
            world.getServer().runOnServer(server -> {
                BlockPos b = garden[0];
                cmd(server, "tp @a " + (b.getX() + 75) + " " + (b.getY() + 20) + " " + (b.getZ() - 45) + " 0 -10");
            });
            context.waitTicks(80);
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("simpleend-giant-trees");
            world.getServer().runOnServer(server -> {
                BlockPos b = garden[0];
                ServerLevel level = server.getLevel(Level.OVERWORLD);
                // close to a leaf cluster of the big Ashwood tree
                BlockPos best = null;
                for (BlockPos q : BlockPos.betweenClosed(b.offset(40, 6, 10), b.offset(64, 40, 34)))
                    if (level.getBlockState(q).is(EndWood.ASHWOOD.leaves) && level.getBlockState(q.north()).isAir() && level.getBlockState(q.north(2)).isAir()) { best = q.immutable(); break; }
                if (best != null) cmd(server, "tp @a " + (best.getX() + 0.5) + " " + (best.getY() - 0.5) + " " + (best.getZ() - 2.6) + " 0 10");
            });
            context.waitTicks(40);
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("simpleend-leaves");
            for (var biome : new net.minecraft.resources.ResourceKey[] {EndBiomes.ASH_VALLEY, EndBiomes.DAWNLANDS, EndBiomes.ASH_VALLEY_FRINGE}) {
                @SuppressWarnings("unchecked") var key = (net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>) biome;
                BlockPos[] spot = new BlockPos[1];
                world.getServer().runOnServer(server -> {
                    ServerLevel end = server.getLevel(Level.END);
                    // own grid search on the End's biome resolver (logs what it saw, for diagnosis)
                    var source = end.getChunkSource().getGenerator().getBiomeSource();
                    var resolver = source.createUncachedResolver(end.getChunkSource().randomState());
                    java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
                    BlockPos hit = null;
                    for (int r = 0; r <= 12000 && hit == null; r += 64)
                        for (int a = 0; a < Math.max(1, r / 16) && hit == null; a++) {
                            double ang = a * Math.PI * 2 / Math.max(1, r / 16);
                            int x = 1600 + (int) (Math.cos(ang) * r), z = (int) (Math.sin(ang) * r);
                            var b = resolver.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(64), net.minecraft.core.QuartPos.fromBlock(z));
                            seen.merge(b.unwrapKey().map(k -> k.identifier().toString()).orElse("?"), 1, Integer::sum);
                            if (b.is(key)) hit = new BlockPos(x, 64, z);
                        }
                    System.out.println("[simpleend smoke] " + source.getClass().getSimpleName() + " biomes " + SimpleEndConfig.biomes + " chance "
                            + SimpleEndConfig.biomeChance + " on " + SimpleEnd.on() + " seen " + seen);
                    if (hit == null) throw new AssertionError("no " + key.identifier() + " within 12000 blocks, seen " + seen);
                    spot[0] = hit;
                    cmd(server, "execute in minecraft:the_end run tp @a " + spot[0].getX() + " 140 " + spot[0].getZ() + " -45 60");
                });
                context.waitTicks(100);
                world.getServer().runOnServer(server -> {
                    ServerLevel end = server.getLevel(Level.END);
                    // chunks are loaded around the player now: find the island surface and step back from it
                    int best = 0;
                    BlockPos at = spot[0];
                    for (int dx = -24; dx <= 24; dx += 4)
                        for (int dz = -24; dz <= 24; dz += 4) {
                            int y = end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + dx, at.getZ() + dz);
                            if (y > best) { best = y; }
                        }
                    cmd(server, "execute in minecraft:the_end run tp @a " + (at.getX() - 14) + " " + (best + 12) + " " + (at.getZ() - 14) + " -45 30");
                });
                context.waitTicks(80);
                world.getConnection().waitForChunksRender();
                context.takeScreenshot("simpleend-" + key.identifier().getPath());
            }
        }
    }
}
