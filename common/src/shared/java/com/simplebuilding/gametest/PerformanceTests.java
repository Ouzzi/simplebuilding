package com.simplebuilding.gametest;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.util.OctantShape;
import com.simplebuilding.util.OctantSurface;
import com.simplebuilding.util.PlayerScan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spieltests der Leistungsarbeit (docs/PERFORMANCE.md): die Abkuerzungen muessen genau dasselbe
 * liefern wie der Weg, den sie ersetzen. Die gemessenen Zahlen (Praedikataufrufe, Zeiten) stehen im
 * Testprotokoll unter "simplebuilding-perf".
 */
public final class PerformanceTests {
    private static final Logger LOG = LoggerFactory.getLogger("simplebuilding-perf");

    private PerformanceTests() {
    }

    /**
     * {@link PlayerScan} (Spielerliste des Levels) findet genau die Spieler, die die Suche ueber die
     * Entity-Sektionen findet: innen, auf der Kante, knapp daneben, mit Filter und im groessten
     * Pad-Bereich (Elytra-Pad Stufe V, letzte Easter-Stufe).
     */
    public static void playerScanFindsExactlyThePlayersTheSectionSearchFinds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ServerPlayer> players = new ArrayList<>();
        try {
            ServerPlayer inside = place(helper, players, new Vec3(2.5, 2.0, 2.5));
            ServerPlayer edge = place(helper, players, new Vec3(5.3, 2.0, 2.5));
            ServerPlayer spectator = place(helper, players, new Vec3(2.5, 2.0, 4.5));
            spectator.setGameMode(GameType.SPECTATOR);
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
            List<AABB> boxes = List.of(
                    new AABB(origin).inflate(1.0, 2.0, 1.0),
                    // Kante: die Hitbox (halbe Breite 0,3) von "edge" reicht bis x = 5,6 (relativ) ...
                    new AABB(helper.absoluteVec(new Vec3(5.5, 1.0, 1.0)), helper.absoluteVec(new Vec3(7.0, 4.0, 4.0))),
                    // ... und hier knapp nicht mehr.
                    new AABB(helper.absoluteVec(new Vec3(5.7, 1.0, 1.0)), helper.absoluteVec(new Vec3(7.0, 4.0, 4.0))),
                    new AABB(origin).inflate(0.1, 1.5, 0.1),
                    PadTiers.elytraArea(origin, PadTiers.MAX, true));
            for (AABB box : boxes) {
                assertSame(helper, "players in " + box,
                        level.getEntitiesOfClass(ServerPlayer.class, box, p -> inList(level, p)), PlayerScan.playersIn(level, box, ServerPlayer.class));
                assertSame(helper, "non-spectators in " + box,
                        level.getEntitiesOfClass(Player.class, box, p -> inList(level, p) && !p.isSpectator()),
                        PlayerScan.playersIn(level, box, Player.class, p -> !p.isSpectator()));
                helper.assertTrue(PlayerScan.anyPlayerIn(level, box) == !level.getEntitiesOfClass(Player.class, box, p -> inList(level, p)).isEmpty(),
                        "anyPlayerIn disagrees with the section search for " + box);
            }
            helper.assertTrue(PlayerScan.playersIn(level, boxes.get(0), ServerPlayer.class).contains(inside), "the player on the pad was not found");
            helper.assertTrue(PlayerScan.playersIn(level, boxes.get(1), ServerPlayer.class).contains(edge), "the player touching the edge was not found");
            helper.assertFalse(PlayerScan.playersIn(level, boxes.get(2), ServerPlayer.class).contains(edge), "a player just outside was found");
            logScanTimes(level, boxes.get(4));
        } finally {
            for (ServerPlayer player : players) {
                level.getServer().getPlayerList().remove(player);
            }
        }
        helper.succeed();
    }

    /**
     * Nur Spieler aus {@code level.players()} zaehlen: die Elytra-Flaeche ist 256 Bloecke breit und faengt
     * Mock-Spieler parallel laufender Tests ein, die nie in der Spielerliste stehen (echte Spieler immer).
     */
    private static boolean inList(ServerLevel level, Player player) {
        return level.players().contains(player);
    }

    /**
     * Ein Spawn-Teleporter fuehrt Wartezeit und Position eines Spielers, solange er darauf steht, und
     * vergisst beides im ersten Tick, nachdem er gegangen ist; danach laeuft sein Tick leer.
     */
    public static void anIdleSpawnTeleporterStopsTrackingOncePlayersLeave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.SPAWN_TELEPORTER_TIER_2);
        BlockPos abs = helper.absolutePos(pad);
        if (!(level.getBlockEntity(abs) instanceof SpawnTeleporterBlockEntity teleporter)) {
            helper.fail("no spawn teleporter block entity at " + abs);
            return;
        }
        List<ServerPlayer> players = new ArrayList<>();
        try {
            helper.assertFalse(teleporter.isTracking(), "a fresh spawn teleporter already tracks someone");
            SpawnTeleporterBlockEntity.serverTick(level, abs, level.getBlockState(abs), teleporter);
            helper.assertFalse(teleporter.isTracking(), "an empty spawn teleporter started tracking");
            ServerPlayer player = place(helper, players, new Vec3(2.5, 2.0, 2.5));
            SpawnTeleporterBlockEntity.serverTick(level, abs, level.getBlockState(abs), teleporter);
            helper.assertTrue(teleporter.isTracking(), "the spawn teleporter does not track the player standing on it");
            Vec3 away = helper.absoluteVec(new Vec3(6.5, 2.0, 6.5));
            player.snapTo(away.x, away.y, away.z, 0.0F, 0.0F);
            SpawnTeleporterBlockEntity.serverTick(level, abs, level.getBlockState(abs), teleporter);
            helper.assertFalse(teleporter.isTracking(), "the spawn teleporter still tracks a player who left");
            SpawnTeleporterBlockEntity.serverTick(level, abs, level.getBlockState(abs), teleporter);
            helper.assertFalse(teleporter.isTracking(), "an idle spawn teleporter started tracking again");
        } finally {
            for (ServerPlayer player : players) {
                level.getServer().getPlayerList().remove(player);
            }
        }
        helper.succeed();
    }

    /**
     * Die zwischengespeicherte Huelle einer Oktant-Figur ({@link OctantSurface}) enthaelt dieselben
     * Seiten und Kanten in derselben Reihenfolge wie die Schleife, die bis 2026-09-28 in jedem Bild lief
     * - fuer jede Form und Ausrichtung, auch bei ungerader Groesse.
     */
    public static void theCachedOctantSurfaceMatchesThePerFrameScanItReplaced(GameTestHelper helper) {
        AABB[] sizes = {new AABB(0, 0, 0, 7, 5, 9), new AABB(-3, 60, 10, 9, 72, 22)};
        for (AABB bounds : sizes) {
            for (OctantItem.SelectionShape shape : OctantItem.SelectionShape.values()) {
                for (Direction orientation : Direction.values()) {
                    Predicate<BlockPos> predicate = OctantShape.predicate(shape, orientation, bounds);
                    long[] calls = {0};
                    List<int[]> expected = oldScan(bounds, p -> {
                        calls[0]++;
                        return predicate.test(p);
                    });
                    OctantSurface surface = OctantSurface.compute(bounds, predicate);
                    String what = shape + "/" + orientation + " in " + bounds;
                    helper.assertTrue(surface.faces() == expected.size(),
                            "surface of " + what + " has " + surface.faces() + " faces, the old scan drew " + expected.size());
                    for (int face = 0; face < surface.faces(); face++) {
                        int[] old = expected.get(face);
                        int mask = 0;
                        for (Direction edge : Direction.values()) {
                            if (surface.hasEdge(face, edge)) {
                                mask |= 1 << edge.ordinal();
                            }
                        }
                        helper.assertTrue(surface.x(face) == old[0] && surface.y(face) == old[1] && surface.z(face) == old[2]
                                        && surface.side(face).ordinal() == old[3] && mask == old[4],
                                "face " + face + " of " + what + " differs from the old scan");
                    }
                    helper.assertTrue(surface.predicateCalls() == calls[0],
                            "the surface of " + what + " counted " + surface.predicateCalls() + " shape tests, the old scan made " + calls[0]);
                }
            }
        }
        AABB sphere = new AABB(0, 0, 0, 32, 32, 32);
        OctantSurface big = OctantSurface.compute(sphere, OctantShape.predicate(OctantItem.SelectionShape.SPHERE, Direction.UP, sphere));
        LOG.info("Octant sphere 32^3: {} faces, {} edges, {} shape tests per recomputation (previously per frame)",
                big.faces(), big.edges(), big.predicateCalls());
        helper.succeed();
    }

    /** Die Schleife aus {@code BlockHighlightRenderer#renderVoxelShape} bis 2026-09-28, statt zu zeichnen protokolliert. */
    private static List<int[]> oldScan(AABB bounds, Predicate<BlockPos> inShape) {
        List<int[]> out = new ArrayList<>();
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos diagPos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    if (inShape.test(pos)) {
                        for (Direction dir : Direction.values()) {
                            neighborPos.set(pos).move(dir);
                            if (!inShape.test(neighborPos)) {
                                int mask = 0;
                                for (Direction edgeDir : Direction.values()) {
                                    if (edgeDir == dir || edgeDir == dir.getOpposite()) continue;
                                    BlockPos sideNeighbor = new BlockPos(pos.getX(), pos.getY(), pos.getZ()).offset(edgeDir.getUnitVec3i());
                                    diagPos.set(neighborPos).move(edgeDir);
                                    boolean sideIsShape = inShape.test(sideNeighbor);
                                    boolean diagIsShape = inShape.test(diagPos);
                                    if (!sideIsShape || diagIsShape) {
                                        mask |= 1 << edgeDir.ordinal();
                                    }
                                }
                                out.add(new int[]{x, y, z, dir.ordinal(), mask});
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private static void logScanTimes(ServerLevel level, AABB area) {
        int rounds = 2000;
        long t0 = System.nanoTime();
        int found = 0;
        for (int i = 0; i < rounds; i++) {
            found += level.getEntitiesOfClass(ServerPlayer.class, area, p -> true).size();
        }
        long t1 = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            found += PlayerScan.playersIn(level, area, ServerPlayer.class).size();
        }
        long t2 = System.nanoTime();
        LOG.info("Player search in {} ({} rounds, {} hits): entity sections {} us/call, player list {} us/call",
                area, rounds, found, (t1 - t0) / 1000.0 / rounds, (t2 - t1) / 1000.0 / rounds);
    }

    private static ServerPlayer place(GameTestHelper helper, List<ServerPlayer> players, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        players.add(player);
        return player;
    }

    private static void assertSame(GameTestHelper helper, String what, List<? extends Player> expected, List<? extends Player> actual) {
        Set<Player> a = new HashSet<>(expected);
        Set<Player> b = new HashSet<>(actual);
        helper.assertTrue(a.equals(b) && expected.size() == actual.size(),
                what + ": the section search found " + expected + ", the player list " + actual);
    }
}
