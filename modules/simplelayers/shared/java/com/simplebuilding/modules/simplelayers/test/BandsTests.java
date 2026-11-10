package com.simplebuilding.modules.simplelayers.test;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.CorePiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.backrooms.door.DoorNet;
import com.simplebuilding.modules.simplelayers.backrooms.door.Entrances;
import com.simplebuilding.modules.simplelayers.backrooms.gen.BuildRules;
import com.simplebuilding.modules.simplelayers.backrooms.gen.DoorPlacement;
import com.simplebuilding.modules.simplelayers.backrooms.gen.Pointers;
import com.simplebuilding.modules.simplelayers.backrooms.gen.Puzzle;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StairRule;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StairRun;
import com.simplebuilding.modules.simplelayers.backrooms.gen.StubBand;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BackroomsStackGenerator;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandBiomeSource;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack;
import com.simplebuilding.modules.simplelayers.backrooms.stack.Bands;
import com.simplebuilding.modules.simplelayers.block.BackroomsDoorBlock;
import com.simplebuilding.modules.simplelayers.block.MaintenanceBoxBlock;
import com.simplebuilding.modules.simplelayers.world.BackroomsBlocks;
import com.simplebuilding.modules.simplelayers.world.BackroomsEntries;
import com.simplebuilding.modules.simplelayers.world.BackroomsRescue;
import com.simplebuilding.modules.simplelayers.world.EntranceShaftFeature;
import com.simplelib.api.TrackingFilters;
import com.simplelib.api.portal.FrameRule;
import com.simplelib.api.portal.PortalDoorBlock;
import com.simplelib.api.portal.PortalLinks;
import com.simplelib.api.portal.PortalLook;
import com.simplelib.api.portal.PortalPreload;
import com.simplelib.api.portal.PortalRule;
import com.simplelib.api.portal.PortalShape;
import com.simplelib.api.portal.PortalState;
import com.simplelib.api.portal.PortalStates;
import com.simplelib.api.portal.PortalTarget;
import com.simplelib.api.portal.PortalTransit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * BACK22 core tests (docs/ai/PLAN-B-BACK22-KERN-2026-11-10.md §2): the 112 grid, the hull rule, single-valued bedrock between levels,
 * generation budget and the S0 server measurement, tracking filter, stair rule, riddles, door net (pairs, frames, boxes, rules),
 * passage through doors with loaded target chunk (FIX172), the cellar double door both ways and the migration rescue. Tests that
 * need the stack dimension check only registries/pure functions in the standalone run (no data pack dimensions there).
 */
final class BandsTests {
    private BandsTests() {}

    static void register() {
        LayersTests.ALL.put("bands_layout", BandsTests::layout);
        LayersTests.ALL.put("bands_hull", BandsTests::hull);
        LayersTests.ALL.put("bands_separators_bedrock", BandsTests::separators);
        LayersTests.ALL.put("bands_gen_budget", BandsTests::budget);
        LayersTests.ALL.put("bands_tracking_filter", BandsTests::trackingFilter);
        LayersTests.ALL.put("bands_stairs_rule", BandsTests::stairsRule);
        LayersTests.ALL.put("bands_puzzle_pointer", BandsTests::puzzlePointer);
        LayersTests.ALL.put("bands_doors_paired", BandsTests::doorsPaired);
        LayersTests.ALL.put("bands_doors_framed", BandsTests::doorsFramed);
        LayersTests.ALL.put("portal_rules_per_level", BandsTests::rulesPerLevel);
        LayersTests.ALL.put("portal_transit_loaded", BandsTests::transitLoaded);
        LayersTests.ALL.put("cellar_door_roundtrip", BandsTests::cellarRoundtrip);
        LayersTests.ALL.put("migration_rescue", BandsTests::migrationRescue);
        // one test per level (adapter level/<Id>GameTest, owned by the level branch): the level stands in the stack with its biome
        for (Band band : Bands.ORDER) LayersTests.ALL.put(band.id() + "_registered", h -> levelRegistered(h, band));
    }

    static ServerLevel stack(GameTestHelper h) {
        return LayersTests.levelOrRegistryOnly(h, SimpleLayers.STACK);
    }

    static long seed(ServerLevel stack) {
        return BackroomsStackGenerator.seed(stack.getChunkSource().randomState());
    }

    /** Grid 112: borders, separators on section edges, R9 order, ids unique; loaded: one biome per level by height. */
    static void layout(GameTestHelper h) {
        h.assertTrue(BandStack.baseY(1) == 208 && BandStack.baseY(10) == -800 && BandStack.MIN_Y + BandStack.HEIGHT == 320, "grid 112");
        for (int b = 1; b <= BandStack.BANDS; b++) {
            int base = BandStack.baseY(b);
            h.assertTrue(base % 16 == 0, "level " + b + " starts on a section edge");
            h.assertTrue(BandStack.bandAt(base) == b && BandStack.bandAt(base + 95) == b && BandStack.localY(base + 95) == 95, "level " + b + " open 0..95");
            h.assertTrue(BandStack.bandAt(base - 1) == 0 && BandStack.bandAt(base - 16) == 0 && BandStack.bandAt(base + 96) == 0, "bedrock around level " + b);
            h.assertTrue(BandStack.nearestBand(base - 8) == Math.min(BandStack.BANDS, b + 1) || b == BandStack.BANDS, "separator below " + b + " belongs to the next level");
        }
        List<String> ids = Bands.ORDER.stream().map(Band::id).toList();
        h.assertTrue(ids.equals(List.of("lobby", "pools", "archive", "warehouse", "pipes", "electro", "hotel", "lightless", "suburb", "wheatfield")), "R9 order " + ids);
        h.assertTrue(Bands.get(1).doorRule() == DoorRule.TWO_WAY && Bands.get(2).doorRule() == DoorRule.ACTIVATE_ONCE, "rules 1, 2");
        for (int b = 3; b <= BandStack.BANDS; b++) h.assertTrue(Bands.get(b).doorRule() == DoorRule.BREAK_ON_TRANSIT, "rule " + b + " breaks");
        ServerLevel stack = stack(h);
        if (stack == null) return;
        h.assertTrue(stack.getChunkSource().getGenerator().getBiomeSource() instanceof BandBiomeSource, "band biome source");
        BandBiomeSource source = (BandBiomeSource) stack.getChunkSource().getGenerator().getBiomeSource();
        for (int b = 1; b <= BandStack.BANDS; b++) {
            String want = "backrooms_" + Bands.get(b).id();
            h.assertTrue(source.biome(BandStack.worldY(b, 40)).is(SimpleLayers.id(want)), "biome of level " + b);
        }
        h.succeed();
    }

    static void levelRegistered(GameTestHelper h, Band band) {
        int index = Bands.indexOf(band);
        h.assertTrue(index >= 1 && Bands.byId(band.id()) == band, band.id() + " in the stack");
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .containsKey(SimpleLayers.id("backrooms_" + band.id())), "biome of " + band.id());
        h.assertTrue(BuildRules.hull(band.layout(), 5L, 3, -4).isEmpty(), "hull rule " + band.id());
        int[] a = band.layout().arrival(5L, 100, 100);
        h.assertTrue(band.layout().at(5L, a[0], a[1], a[2]).passable() && band.layout().at(5L, a[0], a[1] + 1, a[2]).passable(), "free arrival " + band.id());
        h.succeed();
    }

    /** Every level: ly 0 and 95 never passable, hull sections are hull (sampled chunks). */
    static void hull(GameTestHelper h) {
        java.util.Random r = new java.util.Random(31);
        List<String> bad = new ArrayList<>();
        for (Band b : Bands.ORDER) {
            for (int i = 0; i < 12; i++) {
                List<String> p = BuildRules.hull(b.layout(), r.nextLong(), r.nextInt(4000) - 2000, r.nextInt(4000) - 2000);
                if (!p.isEmpty()) bad.add(b.id() + ": " + p.get(0));
            }
            h.assertFalse(b.state(CorePiece.HULL).isAir(), b.id() + " hull is a block");
        }
        h.assertTrue(bad.isEmpty(), "hull rule: " + bad);
        h.succeed();
    }

    /**
     * A generated column: deck, separators and floor single-valued bedrock, the hull sections single-valued too, and the blocks equal the
     * levels' generators (outside door chunks). Logs the S0 server numbers (single/mixed sections, bytes per column).
     */
    static void separators(GameTestHelper h) {
        ServerLevel stack = stack(h);
        if (stack == null) return;
        long seed = seed(stack);
        int cx = 7001, cz = -7001;
        while (anyDoor(seed, cx, cz)) cx++;
        ChunkAccess chunk = stack.getChunk(cx, cz);
        LevelChunkSection[] sections = chunk.getSections();
        int single = 0, mixed = 0, bytes = 0;
        for (int i = 0; i < sections.length; i++) {
            int y0 = (chunk.getMinSectionY() + i) * 16;
            LevelChunkSection s = sections[i];
            bytes += s.getStates().getSerializedSize();
            if (s.getStates().bitsPerEntry() == 0) single++;
            else mixed++;
            if (BandStack.bandAt(y0) == 0) {
                h.assertTrue(s.getStates().bitsPerEntry() == 0 && s.getBlockState(3, 5, 7).is(Blocks.BEDROCK), "bedrock section at y " + y0);
            }
        }
        List<String> wrong = new ArrayList<>();
        for (int x = 0; x < 16; x += 5) {
            for (int z = 0; z < 16; z += 3) {
                for (int y = BandStack.MIN_Y; y <= BandStack.MAX_Y; y += 7) {
                    BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                    BlockState got = stack.getBlockState(p), want = BackroomsStackGenerator.block(seed, p.getX(), y, p.getZ());
                    if (got.getBlock() != want.getBlock() && !got.is(Blocks.CHEST)) wrong.add(p.toShortString() + " " + got + " != " + want);
                }
            }
        }
        h.assertTrue(wrong.isEmpty(), "column differs: " + wrong.subList(0, Math.min(4, wrong.size())));
        SimpleLayers.LOG.info("[S0] stack column {} {}: {} single-valued + {} mixed sections, {} bytes block data (network size)", cx, cz, single, mixed, bytes);
        h.assertTrue(single >= 11, "at least deck, 9 separators and floor single-valued: " + single);
        h.succeed();
    }

    static boolean anyDoor(long seed, int cx, int cz) {
        for (int b = 1; b < BandStack.BANDS; b++) if (DoorNet.pair(seed, b, cx, cz)) return true;
        return Entrances.houseChunk(seed, cx, cz);
    }

    /**
     * Budget (concept §3.1): the ten generators of one column in the mean ≤ 10 ms (warning), fails above 5x (sb-test times vary). Pure
     * fill into a counting writer = the generator cost the stack adds; logged as S0 numbers per level.
     */
    static void budget(GameTestHelper h) {
        long seed = 77L;
        int columns = 24;
        long total = 0;
        StringBuilder per = new StringBuilder();
        for (Band b : Bands.ORDER) {
            long[] count = {0};
            for (int warm = 0; warm < 2; warm++) b.layout().fill(seed, warm, 9, (x, ly, z, p) -> count[0]++);
            long t0 = System.nanoTime();
            for (int i = 0; i < columns; i++) b.layout().fill(seed, 100 + i * 3, -50 + i, (x, ly, z, p) -> count[0]++);
            long ns = (System.nanoTime() - t0) / columns;
            total += ns;
            per.append(String.format(java.util.Locale.ROOT, " %s %.2f ms", b.id(), ns / 1e6));
        }
        double ms = total / 1e6;
        SimpleLayers.LOG.info("[S0] generator cost per column {} ms:{}", String.format(java.util.Locale.ROOT, "%.2f", ms), per);
        if (ms > 10.0) SimpleLayers.LOG.warn("[S0] over the 10 ms budget per column: {} ms", ms);
        h.assertTrue(ms < 50.0, "generator cost " + ms + " ms per column (budget 10, fail at 50):" + per);
        h.succeed();
    }

    /** Inside the stack only the same level is seen (nearest level for the bedrock between); elsewhere nothing is filtered. */
    static void trackingFilter(GameTestHelper h) {
        h.assertTrue(TrackingFilters.size() >= 1, "Simple Layers registered its filter");
        ServerLevel over = h.getLevel();
        h.assertTrue(BackroomsRescue.sameLevel(over, 250, over, -500), "outside the stack nothing is hidden");
        ServerLevel stack = stack(h);
        if (stack == null) return;
        h.assertTrue(BackroomsRescue.sameLevel(stack, 210, stack, 300), "same level 1");
        h.assertFalse(BackroomsRescue.sameLevel(stack, 210, stack, 110), "level 1 does not see level 2");
        h.assertTrue(BackroomsRescue.sameLevel(stack, 200, stack, 110), "the separator under level 1 counts to level 2");
        h.assertTrue(BackroomsRescue.sameLevel(stack, 210, over, 110), "other dimension: Vanilla decides");
        h.succeed();
    }

    /** STAIRS22 rule as checker: a good flight passes, missing headroom/base and a flight of 9 fail; ladder exit 3. */
    static void stairsRule(GameTestHelper h) {
        // a staircase going east from (0, 1, 0): step i at x i, ly 1 + i on solid ground, air above
        BandLayout good = new BandLayout() {
            @Override
            public BandPiece at(long seed, int x, int ly, int z) {
                if (z != 0 || x < 0 || x > 9) return CorePiece.HULL;
                return ly <= x ? CorePiece.HULL : CorePiece.AIR;
            }

            @Override
            public int[] arrival(long seed, int x, int z) {
                return new int[] {x, 1, z};
            }
        };
        h.assertTrue(StairRule.check(good, 0, new StairRun(0, 1, 0, 3, 8, false)).isEmpty(), "good flight: " + StairRule.check(good, 0, new StairRun(0, 1, 0, 3, 8, false)));
        h.assertFalse(StairRule.check(good, 0, new StairRun(0, 1, 0, 3, 9, false)).isEmpty(), "9 steps without landing");
        h.assertFalse(StairRule.check(good, 0, new StairRun(0, 1, 1, 3, 3, false)).isEmpty(), "no headroom beside the stair");
        BandLayout low = (BandLayout) new BandLayout() {
            @Override
            public BandPiece at(long seed, int x, int ly, int z) {
                return ly <= 0 || ly == 4 ? CorePiece.HULL : CorePiece.AIR;
            }

            @Override
            public int[] arrival(long seed, int x, int z) {
                return new int[] {x, 1, z};
            }
        };
        h.assertFalse(StairRule.check(low, 0, new StairRun(0, 1, 0, 0, 2, true)).isEmpty(), "ladder exit under a 3-high ceiling");
        h.succeed();
    }

    /** The switch panel riddle is solvable from all 16 states in ≤ 4 moves; pointers turn in 16 steps toward the goal. */
    static void puzzlePointer(GameTestHelper h) {
        List<Integer> all = new ArrayList<>();
        for (int s = 0; s < 16; s++) all.add(s);
        h.assertTrue(Puzzle.solvable(Puzzle.SWITCHES, all, 64), "every start state solvable");
        for (int s : all) h.assertTrue(Puzzle.SWITCHES.distance(s, 64) <= 4, "≤ 4 moves from " + s);
        h.assertTrue(Pointers.toward(0, 0, 0, 10) == 0 && Pointers.toward(0, 0, -10, 0) == 4 && Pointers.toward(0, 0, 0, -10) == 8
                && Pointers.toward(0, 0, 10, 0) == 12 && Pointers.toward(0, 0, -10, 10) == 2, "16-step pointer");
        h.succeed();
    }

    /**
     * Door net (pure): density near the level's value, both ends of a pair in the same chunk and inside it, the down end found at its
     * position with the right target, a free spot in front, the link id shared; one-way pairs only by the up-door share.
     */
    static void doorsPaired(GameTestHelper h) {
        long seed = 4711L;
        int pairs = 0, twoSided = 0, n = 3000;
        Set<Long> links = new HashSet<>();
        for (int i = 0; i < n; i++) {
            int cx = i * 7 - 9000, cz = (i * 13) % 5000 - 2500;
            for (int upper = 1; upper < BandStack.BANDS; upper++) {
                DoorEnd down = DoorNet.down(seed, upper, cx, cz);
                if (!DoorNet.pair(seed, upper, cx, cz)) continue;
                if (upper == 1) pairs++;
                h.assertTrue(down != null, "pair " + upper + " at " + cx + " " + cz + " has a door down");
                h.assertTrue(DoorPlacement.fits(down, cx, cz), "door down inside its chunk");
                BandLayout l = Bands.get(upper).layout();
                h.assertTrue(l.at(seed, down.frontX(), down.ly(), down.frontZ()).passable() && l.at(seed, down.frontX(), down.ly() + 1, down.frontZ()).passable()
                        && l.at(seed, down.frontX(), down.ly() - 1, down.frontZ()).solid(), "free spot before the door " + upper);
                DoorNet.Found f = DoorNet.at(seed, down.x(), BandStack.worldY(upper, down.ly()), down.z());
                h.assertTrue(f != null && f.down() && f.target() == upper + 1 && f.upper() == upper, "found the door down of " + upper);
                h.assertTrue(links.add(DoorNet.link(upper, cx, cz)), "unique link id");
                DoorEnd up = DoorNet.up(seed, upper + 1, cx, cz);
                if (up != null) {
                    twoSided++;
                    h.assertTrue(DoorPlacement.fits(up, cx, cz), "door up inside the same chunk");
                    DoorEnd own = upper + 1 < BandStack.BANDS ? DoorNet.down(seed, upper + 1, cx, cz) : null;
                    h.assertTrue(own == null || !DoorPlacement.overlaps(own, up), "door up and door down of level " + (upper + 1) + " apart");
                    DoorNet.Found g = DoorNet.at(seed, up.x(), BandStack.worldY(upper + 1, up.ly()), up.z());
                    h.assertTrue(g != null && !g.down() && g.target() == upper && g.upper() == upper, "found the door up of " + (upper + 1));
                }
            }
        }
        double share = pairs / (double) n;
        h.assertTrue(share > 0.025 && share < 0.06, "pair share level 1: " + share);
        h.assertTrue(twoSided > 0, "two-sided pairs exist");
        h.succeed();
    }

    /** First chunk (scanning from (cx, cz)) with a two-sided pair below {@code upper}. */
    static int[] pairChunk(long seed, int upper, int cx, int cz) {
        for (int i = 0; i < 20000; i++) {
            int x = cx + i % 200, z = cz + i / 200;
            if (DoorNet.down(seed, upper, x, z) != null && DoorNet.up(seed, upper + 1, x, z) != null) return new int[] {x, z};
        }
        throw new IllegalStateException("no pair below " + upper);
    }

    static BlockPos lower(int band, DoorEnd e) {
        return new BlockPos(e.x(), BandStack.worldY(band, e.ly()), e.z());
    }

    /** Generated pair 1↔2 and 2↔3: framed doors, symbols = target, look from the rule, the box in the frame of non-two-way doors. */
    static void doorsFramed(GameTestHelper h) {
        ServerLevel stack = stack(h);
        if (stack == null) return;
        long seed = seed(stack);
        for (int upper = 1; upper <= 3; upper++) {
            int[] c = pairChunk(seed, upper, 5000 + upper * 300, 5000);
            stack.getChunk(c[0], c[1]);
            DoorEnd down = DoorNet.down(seed, upper, c[0], c[1]), up = DoorNet.up(seed, upper + 1, c[0], c[1]);
            PortalRule rule = Bands.get(upper).doorRule().portalRule();
            PortalStates.set(stack.getServer(), DoorNet.link(upper, c[0], c[1]), rule, rule.initial());
            for (int side = 0; side < 2; side++) {
                int band = side == 0 ? upper : upper + 1;
                DoorEnd e = side == 0 ? down : up;
                BlockPos at = lower(band, e);
                BlockState s = stack.getBlockState(at);
                h.assertTrue(s.is(LayersRegistry.BACKROOMS_DOOR), "door of level " + band + " at " + at.toShortString() + ": " + s);
                h.assertTrue(s.getValue(BackroomsDoorBlock.SYMBOL) == (side == 0 ? upper + 1 : upper), "symbol = target");
                PortalShape shape = PortalShape.of(stack, at);
                h.assertTrue(shape != null && FrameRule.framed(stack, shape), "framed door of level " + band);
                h.assertTrue(PortalDoorBlock.look(stack, shape) == PortalLook.of(rule.initial()), "look of level " + band + " " + PortalDoorBlock.look(stack, shape));
                BlockPos box = at.relative(shape.facing().getCounterClockWise()).above();
                boolean wantBox = rule != PortalRule.TWO_WAY;
                h.assertTrue(stack.getBlockState(box).is(LayersRegistry.MAINTENANCE_BOX) == wantBox, "box at " + box.toShortString() + " for rule " + rule.id());
                PortalTarget target = ((PortalDoorBlock) s.getBlock()).resolve(stack, shape);
                h.assertTrue(target != null && target.linkId() == DoorNet.link(upper, c[0], c[1]) && target.rule() == rule, "target of level " + band);
                h.assertTrue(stack.getBlockState(target.front(0)).getCollisionShape(stack, target.front(0)).isEmpty(), "free arrival before the other door");
            }
        }
        h.succeed();
    }

    /** Rules on real doors: two-way stays open, activate-once is dormant until its box, break-on-transit breaks and its box repairs. */
    static void rulesPerLevel(GameTestHelper h) {
        ServerLevel stack = stack(h);
        if (stack == null) return;
        MinecraftServer server = stack.getServer();
        long seed = seed(stack);
        // level 2 (Poolrooms) -> 3: dormant, the box activates it once, a second use does nothing
        int[] c = pairChunk(seed, 2, 6000, 6000);
        stack.getChunk(c[0], c[1]);
        PortalStates.set(server, DoorNet.link(2, c[0], c[1]), PortalRule.ACTIVATE_ONCE, PortalRule.ACTIVATE_ONCE.initial());
        BlockPos door = lower(2, DoorNet.down(seed, 2, c[0], c[1]));
        PortalShape shape = PortalShape.of(stack, door);
        BlockPos box = door.relative(shape.facing().getCounterClockWise()).above();
        stack.setBlock(box, LayersRegistry.MAINTENANCE_BOX.defaultBlockState(), 2);
        h.assertTrue(PortalStates.get(server, DoorNet.link(2, c[0], c[1]), PortalRule.ACTIVATE_ONCE) == PortalState.DORMANT, "starts dormant");
        PortalShape near = MaintenanceBoxBlock.doorNear(stack, box);
        h.assertTrue(near != null && near.anchor().equals(shape.anchor()), "the box finds its door");
        h.assertTrue(PortalTransit.mend(stack, near, null), "box activates");
        h.assertTrue(PortalStates.get(server, DoorNet.link(2, c[0], c[1]), PortalRule.ACTIVATE_ONCE) == PortalState.OPEN, "open for good");
        h.assertFalse(PortalTransit.mend(stack, near, null), "nothing left to activate");
        BlockPos up = lower(3, DoorNet.up(seed, 3, c[0], c[1]));
        h.assertTrue(stack.getBlockState(up).getValue(PortalDoorBlock.PORTAL) == PortalLook.ACTIVE, "the other end shows it too");
        PortalStates.set(server, DoorNet.link(2, c[0], c[1]), PortalRule.ACTIVATE_ONCE, PortalRule.ACTIVATE_ONCE.initial());
        PortalDoorBlock.refresh(stack, door);
        h.assertTrue(stack.getBlockState(door).getValue(PortalDoorBlock.PORTAL) == PortalLook.DORMANT, "reset for later runs");
        // unframed: remove a side block, the look says so, put it back
        BlockPos side = door.relative(shape.facing().getClockWise());
        BlockState keep = stack.getBlockState(side);
        stack.setBlock(side, Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(stack.getBlockState(door).getValue(PortalDoorBlock.PORTAL) == PortalLook.UNFRAMED, "unframed without its right side");
        stack.setBlock(side, keep, 3);
        h.assertTrue(stack.getBlockState(door).getValue(PortalDoorBlock.PORTAL) == PortalLook.DORMANT, "framed again");
        h.succeed();
    }

    static void open(ServerLevel level, BlockPos lower) {
        for (BlockPos p : new BlockPos[] {lower, lower.above()}) {
            BlockState s = level.getBlockState(p);
            level.setBlock(p, s.setValue(DoorBlock.OPEN, Boolean.TRUE), 2);
        }
    }

    /**
     * A player goes through the Lobby's door down: arrives before the door up of the Poolrooms (same chunk, loaded), turned by the
     * doors' quarter turns, tracked in the level next tick (FIX172); then through a breaking door (level 3 -> 4): the link breaks.
     */
    static void transitLoaded(GameTestHelper h) {
        ServerLevel stack = stack(h);
        if (stack == null) return;
        MinecraftServer server = stack.getServer();
        long seed = seed(stack);
        int[] c = pairChunk(seed, 1, 7000, -3000);
        stack.getChunk(c[0], c[1]);
        DoorEnd down = DoorNet.down(seed, 1, c[0], c[1]), up = DoorNet.up(seed, 2, c[0], c[1]);
        BlockPos from = lower(1, down), to = lower(2, up);
        open(stack, from);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        float yaw = Direction.from2DDataValue(down.facing()).toYRot();
        player.teleportTo(stack, from.getX() + 0.5, from.getY(), from.getZ() + 0.5, Set.of(), yaw, 0.0F, false);
        Entity moved = PortalTransit.transit(stack, player, from);
        h.assertTrue(moved != null && moved.level() == stack, "went through");
        BlockPos front = to.relative(Direction.from2DDataValue(up.facing()).getOpposite());
        h.assertTrue(moved.blockPosition().equals(front), "arrived before the door up " + front.toShortString() + " but " + moved.blockPosition().toShortString());
        h.assertTrue(BandStack.bandAt(moved.getBlockY()) == 2 && stack.hasChunk(front.getX() >> 4, front.getZ() >> 4), "level 2, chunk loaded");
        float want = Direction.from2DDataValue(up.facing()).getOpposite().toYRot();
        h.assertTrue(Math.floorMod(Math.round(moved.getYRot() - want), 360) == 0, "looks away from the door: " + moved.getYRot() + " want " + want);
        h.assertTrue(PortalStates.get(server, DoorNet.link(1, c[0], c[1]), PortalRule.TWO_WAY) == PortalState.OPEN, "two-way stays open");
        h.assertTrue(PortalTransit.transit(stack, (ServerPlayer) moved, front) == null, "no door at the arrival spot");
        // a breaking pair (level 3 -> 4)
        int[] d = pairChunk(seed, 3, 7400, -3000);
        stack.getChunk(d[0], d[1]);
        long link = DoorNet.link(3, d[0], d[1]);
        PortalStates.set(server, link, PortalRule.BREAK_ON_TRANSIT, PortalState.OPEN);
        BlockPos b3 = lower(3, DoorNet.down(seed, 3, d[0], d[1]));
        open(stack, b3);
        ServerPlayer second = h.makeMockServerPlayerInLevel();
        second.teleportTo(stack, b3.getX() + 0.5, b3.getY(), b3.getZ() + 0.5, Set.of(), 0.0F, 0.0F, false);
        Entity through = PortalTransit.transit(stack, second, b3);
        h.assertTrue(through != null && BandStack.bandAt(through.getBlockY()) == 4, "breaking door passed once");
        h.assertTrue(PortalStates.get(server, link, PortalRule.BREAK_ON_TRANSIT) == PortalState.BROKEN, "broken after the passage");
        h.assertTrue(stack.getBlockState(b3).getValue(PortalDoorBlock.PORTAL) == PortalLook.BROKEN, "shows broken");
        BlockPos box = b3.relative(PortalShape.of(stack, b3).facing().getCounterClockWise()).above();
        h.assertTrue(PortalTransit.mend(stack, MaintenanceBoxBlock.doorNear(stack, box), null), "the box repairs it");
        h.assertTrue(PortalStates.get(server, link, PortalRule.BREAK_ON_TRANSIT) == PortalState.OPEN, "open again");
        UUID id = moved.getUUID();
        h.succeedWhen(() -> h.assertTrue(stack.getEntity(id) != null && stack.getEntity(id).blockPosition().equals(front), "tracked at the arrival"));
    }

    /**
     * Cellar double door both ways: a house in a house chunk links its door (PortalLinks); stepping in leads to the Lobby's entrance
     * double door (same leaf, loaded), stepping back in leads to the cellar; preloading near the cellar door ticks the Lobby chunk.
     */
    static void cellarRoundtrip(GameTestHelper h) {
        ServerLevel stack = stack(h);
        if (stack == null) return;
        ServerLevel over = h.getLevel();
        MinecraftServer server = over.getServer();
        long seed = seed(stack);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        int cx = (o.getX() + 2400) >> 4, cz = o.getZ() >> 4;
        while (!Entrances.houseChunk(seed, cx, cz)) cx++;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) over.getChunk(cx + dx, cz + dz);
        BlockPos ground = new BlockPos(cx * 16 + 8, Math.max(o.getY() + 20, over.getMinY() + 10), cz * 16 + 8);
        BlockPos anchor = EntranceShaftFeature.build(over, ground, BackroomsBlocks.Wood.OAK, over.getRandom());
        long link = Entrances.link(cx, cz);
        PortalLinks.put(server, link, new PortalLinks.End(Level.OVERWORLD, anchor, Direction.WEST, 2));
        PortalDoorBlock.refresh(over, anchor);
        h.assertTrue(over.getBlockState(anchor).getValue(PortalDoorBlock.PORTAL) == PortalLook.ACTIVE, "cellar door active");
        h.assertTrue(PortalPreload.scan(over, anchor.east(2)) >= 1, "preload finds the cellar door");
        DoorEnd lobby = Entrances.lobbyEnd(seed, cx, cz);
        h.assertTrue(lobby != null && lobby.width() == 2, "Lobby entrance is a double door");
        BlockPos right = anchor.relative(Direction.NORTH); // leaf 1 of the cellar door
        open(over, anchor);
        open(over, right);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.teleportTo(over, right.getX() + 0.5, right.getY(), right.getZ() + 0.5, Set.of(), 90.0F, 0.0F, false);
        Entity in = PortalTransit.transit(over, player, right);
        BlockPos lobbyLeaf1 = new BlockPos(lobby.leafX(1), BandStack.worldY(1, lobby.ly()), lobby.leafZ(1));
        BlockPos lobbyFront = lobbyLeaf1.relative(Direction.from2DDataValue(lobby.facing()).getOpposite());
        h.assertTrue(in != null && in.level() == stack && in.blockPosition().equals(lobbyFront), "in the Lobby before leaf 1: " + (in == null ? null : in.blockPosition()));
        h.assertTrue(BackroomsEntries.get(server).get(player.getUUID()) != null, "entry remembered at the cellar door");
        h.assertTrue(stack.getBlockState(lobbyLeaf1).is(LayersRegistry.BACKROOMS_DOOR) && stack.getBlockState(lobbyLeaf1).getValue(BackroomsDoorBlock.SYMBOL) == 0, "Lobby door, symbol 0 = house");
        // and back out (after the cooldown)
        BlockPos lobbyAnchor = new BlockPos(lobby.x(), BandStack.worldY(1, lobby.ly()), lobby.z());
        open(stack, lobbyAnchor);
        open(stack, lobbyLeaf1);
        ServerPlayer back = (ServerPlayer) in;
        h.runAfterDelay(PortalTransit.COOLDOWN + 2, () -> {
            back.teleportTo(stack, lobbyAnchor.getX() + 0.5, lobbyAnchor.getY(), lobbyAnchor.getZ() + 0.5, Set.of(), 0.0F, 0.0F, false);
            Entity out = PortalTransit.transit(stack, back, lobbyAnchor);
            BlockPos cellarFront = anchor.relative(Direction.EAST);
            h.assertTrue(out != null && out.level() == over && out.blockPosition().equals(cellarFront), "back in the cellar: " + (out == null ? null : out.blockPosition()));
            h.assertTrue(BackroomsEntries.get(server).get(back.getUUID()) == null, "entry forgotten after leaving");
            h.succeed();
        });
    }

    /** A player left in a removed BACK18 dimension lands in the Overworld at y 0..63 inside the ground: moved onto the surface. */
    static void migrationRescue(GameTestHelper h) {
        ServerLevel over = h.getLevel();
        MinecraftServer server = over.getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        if (player.level() != server.overworld()) {
            h.succeed(); // the test level is not the Overworld here
            return;
        }
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        int x = o.getX() + 3000, z = o.getZ() + 40;
        over.getChunk(x >> 4, z >> 4);
        int top = over.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = Math.min(40, top - 10);
        if (y < BackroomsRescue.OLD_MIN_Y) {
            h.succeed(); // flat test world without depth: nothing to rescue from
            return;
        }
        player.teleportTo(over, x + 0.5, y, z + 0.5, Set.of(), 0.0F, 0.0F, false);
        BackroomsEntries.get(server).put(player.getUUID(), new BlockPos(x, top, z));
        h.assertTrue(BackroomsRescue.stranded(player), "stranded at y " + y + " (surface " + top + ")");
        h.assertTrue(BackroomsRescue.onJoin(player), "rescued");
        h.assertTrue(player.getY() >= top - 1 && BackroomsEntries.get(server).get(player.getUUID()) == null, "on the surface, entry gone: " + player.getY());
        h.assertFalse(BackroomsRescue.stranded(player), "not stranded any more");
        BackroomsEntries.get(server).put(player.getUUID(), new BlockPos(x, top, z));
        player.teleportTo(over, x + 0.5, y, z + 0.5, Set.of(), 0.0F, 0.0F, false);
        h.assertFalse(BackroomsRescue.onJoin(player), "only the first join after the update rescues");
        BackroomsEntries.get(server).remove(player.getUUID());
        h.succeed();
    }
}
