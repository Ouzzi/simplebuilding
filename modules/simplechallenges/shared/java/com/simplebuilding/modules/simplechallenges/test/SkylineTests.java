package com.simplebuilding.modules.simplechallenges.test;

import com.simplebuilding.modules.simplechallenges.core.Active;
import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.core.ChallengeService;
import com.simplebuilding.modules.simplechallenges.core.ChallengeState;
import com.simplebuilding.modules.simplechallenges.core.Gate;
import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import com.simplebuilding.modules.simplechallenges.lucky.BlockCatalog;
import com.simplebuilding.modules.simplechallenges.lucky.LuckyService;
import com.simplebuilding.modules.simplechallenges.lucky.LuckyTable;
import com.simplebuilding.modules.simplechallenges.preset.LuckyMode;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.PresetDraft;
import com.simplebuilding.modules.simplechallenges.preset.PresetParser;
import com.simplebuilding.modules.simplechallenges.preset.SkylineMode;
import com.simplebuilding.modules.simplechallenges.price.BlockValues;
import com.simplebuilding.modules.simplechallenges.skyline.Eggs;
import com.simplebuilding.modules.simplechallenges.skyline.Layout;
import com.simplebuilding.modules.simplechallenges.skyline.SkylinePlans;
import com.simplebuilding.modules.simplechallenges.skyline.SkylineService;
import com.simplebuilding.modules.simplechallenges.skyline.Support;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Checks of the Lucky Block (S5), Skyline (S6) and Easter egg (S7) stages, bodies for the adapter {@code ModuleGameTest}: the layouts and
 * sequences, the support rule, the odds and the drop hook, the 16 shipped presets, chunk filling, regrowth, the seal, the eggs.
 * World tests build in the test area high above the structure with their own small plan and always clean up.
 */
public final class SkylineTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private SkylineTests() {}

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    /** Removes blocks and entities (not players) of a box. */
    private static void wipe(ServerLevel level, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (Entity e : level.getEntities((Entity) null, new AABB(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1), x -> !(x instanceof net.minecraft.world.entity.player.Player))) e.discard();
    }

    private static SkylinePlans.Dim line(BlockPos o, boolean seal, int eggDistance, BlockCatalog.Dim kind, List<Block> blocks) {
        return new SkylinePlans.Dim(kind, Level.OVERWORLD, Layout.LINEAR, o.getX(), o.getY(), o.getZ(), blocks, seal, eggDistance);
    }

    private static SkylinePlans plans(SkylineMode mode, SkylinePlans.Dim overworld) {
        return new SkylinePlans(mode, overworld, overworld, overworld);
    }

    /** A survival mock player with a pickaxe. */
    private static ServerPlayer miner(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        return p;
    }

    static {
        ALL.put("skyline_layout", h -> {
            check(h, Layout.LINEAR.cell(7)[0] == 7 && Layout.LINEAR.index(7, 0) == 7 && Layout.LINEAR.index(-1, 0) == -1 && Layout.LINEAR.index(1, 1) == -1, "linear");
            int[][] grid = {{0, 0}, {1, 0}, {1, 1}, {0, 1}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {3, 0}};
            for (int i = 0; i < grid.length; i++) check(h, java.util.Arrays.equals(Layout.GRID.cell(i), grid[i]), "grid cell " + i + " is " + java.util.Arrays.toString(Layout.GRID.cell(i)));
            int[][] ring = {{0, 0}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {2, 0}, {2, 1}, {2, 2}};
            for (int i = 0; i < ring.length; i++) check(h, java.util.Arrays.equals(Layout.RING.cell(i), ring[i]), "ring cell " + i + " is " + java.util.Arrays.toString(Layout.RING.cell(i)));
            for (Layout l : Layout.values()) {
                Set<Long> seen = new HashSet<>();
                for (int n = 0; n < 3000; n++) {
                    int[] c = l.cell(n);
                    check(h, l.index(c[0], c[1]) == n, l + " index of cell " + n);
                    check(h, seen.add(((long) c[0] << 32) ^ (c[1] & 0xFFFFFFFFL)), l + " cell " + n + " is used twice");
                }
            }
            for (int k = 1; k <= 6; k++) check(h, Layout.RING.index(k, -k) == 4 * k * (k + 1) && Layout.RING.index(k, -(k - 1)) == 4 * k * (k - 1) + 1, "ring layer " + k + " has 8k cells");
            check(h, Layout.GRID.index(-1, 0) == -1 && Layout.GRID.index(0, -1) == -1 && Layout.RING.index(0, 0) == 0, "outside the grid");
            h.succeed();
        });

        ALL.put("skyline_sequence", h -> {
            List<Block> ow = BlockCatalog.sequence(BlockCatalog.Dim.OVERWORLD), nether = BlockCatalog.sequence(BlockCatalog.Dim.NETHER), end = BlockCatalog.sequence(BlockCatalog.Dim.END);
            check(h, ow.get(0) == Blocks.GRASS_BLOCK && ow.get(ow.size() - 1) == Blocks.OBSIDIAN && ow.size() > 300, "overworld: grass first, obsidian last, " + ow.size());
            check(h, nether.get(0) == Blocks.NETHERRACK && nether.contains(Blocks.ANCIENT_DEBRIS) && nether.size() > 50, "nether line " + nether.size());
            check(h, end.get(0) == Blocks.END_STONE && end.contains(Blocks.SHULKER_BOX) && end.contains(Blocks.PURPUR_BLOCK), "end line " + end.size());
            check(h, new HashSet<>(ow).size() == ow.size() && new HashSet<>(nether).size() == nether.size() && new HashSet<>(end).size() == end.size(), "no block twice in a line");
            check(h, ow.size() + nether.size() + end.size() == BlockCatalog.all().size(), "every survival block stands in exactly one line");
            check(h, ow.contains(Blocks.DIAMOND_ORE) && ow.contains(Blocks.OAK_LOG) && !ow.contains(Blocks.NETHERRACK) && !ow.contains(Blocks.END_STONE), "blocks are in their dimension");
            check(h, !ow.contains(Blocks.BEDROCK) && !ow.contains(Blocks.SPAWNER) && !ow.contains(Blocks.WATER) && !ow.contains(Blocks.COMMAND_BLOCK) && !ow.contains(Blocks.DRAGON_EGG), "technical and one-of-a-kind blocks are left out");
            check(h, BlockCatalog.sequence(BlockCatalog.Dim.OVERWORLD).equals(ow), "the order is stable");
            for (Block b : BlockCatalog.all()) {
                List<Support.Part> parts = Support.parts(b);
                check(h, !parts.isEmpty() && parts.get(0).dx() == 0 && parts.get(0).dy() == 0 && parts.get(0).dz() == 0, BlockCatalog.idOf(b) + ": first part is the block");
                for (Support.Part p : parts) check(h, p.dx() >= 0 && p.dx() <= 1 && p.dz() >= 0 && p.dz() <= 1 && p.dy() >= -1 && p.dy() <= 1, BlockCatalog.idOf(b) + ": parts stay in the chunk box");
            }
            check(h, Support.parts(Blocks.STONE).size() == 1 && Support.parts(Blocks.GRASS_BLOCK).size() == 1, "a solid block stands alone");
            check(h, Support.parts(Blocks.SAND).size() == 2 && Support.parts(Blocks.SAND).get(1).dy() == -1 && Support.parts(Blocks.SAND).get(1).state().is(Blocks.BEDROCK), "sand falls: bedrock below");
            check(h, Support.parts(Blocks.TORCH).stream().anyMatch(p -> p.state().is(Blocks.BEDROCK) && p.dy() == -1), "a torch stands on bedrock");
            check(h, Support.parts(Blocks.LADDER).stream().anyMatch(p -> p.state().is(Blocks.BEDROCK)), "a ladder hangs on bedrock");
            List<Support.Part> door = Support.parts(Blocks.OAK_DOOR);
            check(h, door.stream().anyMatch(p -> p.dy() == 1 && p.state().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) && door.stream().anyMatch(p -> p.state().is(Blocks.BEDROCK)), "a door has both halves and a floor");
            Block bed = BlockCatalog.all().stream().filter(b -> b.defaultBlockState().hasProperty(BlockStateProperties.BED_PART)).findFirst().orElse(null);
            check(h, bed != null && Support.parts(bed).size() == 2 && Support.parts(bed).get(1).dz() == 1, "a bed has a head");
            check(h, Support.parts(Blocks.OAK_LEAVES).get(0).state().getValue(BlockStateProperties.PERSISTENT), "leaves do not decay");
            h.succeed();
        });

        ALL.put("lucky_table", h -> {
            BlockValues v = BlockValues.current();
            check(h, LuckyTable.weight(v, "minecraft:stone", 1.3) > 50 * LuckyTable.weight(v, "minecraft:diamond_ore", 1.3), "stone is far more likely than diamond ore");
            check(h, LuckyTable.weight(v, "minecraft:netherite_block", 1.3) < LuckyTable.weight(v, "minecraft:oak_planks", 1.3) / 50, "rare blocks are rare");
            LuckyTable table = LuckyTable.build(1.3);
            check(h, table.size() > 500 && table.total() > 100, "table size " + table.size());
            java.util.Random random = new java.util.Random(7);
            int stone = 0, diamonds = 0, netherite = 0;
            for (int i = 0; i < 20_000; i++) {
                Block b = table.pick(random.nextDouble());
                if (b == Blocks.STONE) stone++;
                if (b == Blocks.DIAMOND_ORE) diamonds++;
                if (b == Blocks.NETHERITE_BLOCK) netherite++;
            }
            check(h, stone > 1500 && diamonds < 10 && netherite == 0, "20000 rolls: stone " + stone + ", diamond ore " + diamonds + ", netherite blocks " + netherite);
            check(h, LuckyTable.build(0.2).total() > 0 && LuckyTable.build(4.0).total() > 0, "alpha range");
            check(h, LuckyTable.starter(0, 6) == Blocks.OAK_LOG && LuckyTable.starter(1, 6) == Blocks.COBBLESTONE && LuckyTable.starter(6, 6) == null && LuckyTable.starter(0, 0) == null, "the pity gives wood and stone first");
            int different = 0;
            for (Block b : BlockCatalog.all().subList(0, 60)) {
                Block p1 = table.partner(5, b), p2 = table.partner(5, b);
                check(h, p1 == p2 && BlockCatalog.includable(p1), "a partner is fixed and a survival block");
                if (table.partner(6, b) != p1) different++;
            }
            check(h, different > 20, "another seed gives other partners: " + different);
            check(h, !BlockCatalog.includable(Blocks.BEDROCK) && !BlockCatalog.includable(Blocks.SPAWNER) && !BlockCatalog.includable(Blocks.WATER) && !BlockCatalog.includable(Blocks.WALL_TORCH)
                    && BlockCatalog.includable(Blocks.STONE), "which blocks count");
            for (Block b : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
                check(h, !(path.startsWith("waxed_") || path.startsWith("infested_")) || !BlockCatalog.includable(b), path + " is only a recipe variant");
            }
            h.succeed();
        });

        ALL.put("lucky_break", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            BlockPos pos = h.absolutePos(new BlockPos(0, 40, 0));
            AABB box = new AABB(pos).inflate(1.5);
            ServerPlayer creative = h.makeMockServerPlayerInLevel();
            Gate.creativeCheck = pl -> pl == creative; // mock players always report creative: only this one does
            try {
                ServerPlayer p = miner(h);
                // no challenge: Vanilla break
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                check(h, LuckyService.beforeBreak(level, p, pos, Blocks.STONE.defaultBlockState()), "without a challenge Vanilla breaks");
                check(h, ChallengeService.activate(server, "vanilla_unlock") == ChallengeService.Result.OK, "unlock activated");
                check(h, LuckyService.beforeBreak(level, p, pos, Blocks.STONE.defaultBlockState()), "a challenge without lucky leaves the break alone");
                ChallengeService.deactivate(server);
                check(h, ChallengeService.activate(server, "lucky_strike") == ChallengeService.Result.OK, "lucky activated: " + PresetRegistry.problems());
                Active a = ChallengeRuntime.server();
                check(h, a.lucky != null && a.preset.lucky().kind() == LuckyMode.Kind.RANDOM, "lucky table built");
                // the first block is the pity: oak log
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                check(h, !LuckyService.beforeBreak(level, p, pos, Blocks.STONE.defaultBlockState()), "the break is handled");
                check(h, level.getBlockState(pos).isAir() && a.state.luckyBreaks == 1, "block gone, counted");
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
                check(h, items.size() == 1 && items.get(0).getItem().is(Items.OAK_LOG), "the pity drop is an oak log: " + items);
                items.get(0).discard();
                // after the pity: some block, still exactly one item, not the stone itself necessarily
                a.state.luckyBreaks = 50;
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                check(h, !LuckyService.beforeBreak(level, p, pos, Blocks.STONE.defaultBlockState()) && a.state.luckyBreaks == 51, "a rolled break");
                items = level.getEntitiesOfClass(ItemEntity.class, box);
                check(h, items.size() == 1 && !items.get(0).getItem().isEmpty(), "one rolled block dropped");
                items.get(0).discard();
                // left to Vanilla: creative, no tool for drops, blocks with contents, non-survival blocks
                check(h, LuckyService.beforeBreak(level, creative, pos, Blocks.STONE.defaultBlockState()), "creative breaks normally");
                ServerPlayer hand = h.makeMockServerPlayerInLevel();
                hand.setGameMode(GameType.SURVIVAL);
                check(h, LuckyService.beforeBreak(level, hand, pos, Blocks.STONE.defaultBlockState()), "no pickaxe, no drop anyway: Vanilla");
                check(h, LuckyService.beforeBreak(level, p, pos, Blocks.CHEST.defaultBlockState()), "a chest keeps its contents");
                check(h, LuckyService.beforeBreak(level, p, pos, Blocks.BEDROCK.defaultBlockState()), "bedrock is no survival block");
                ChallengeService.deactivate(server);
                // random drops: the same partner every time
                check(h, ChallengeService.activate(server, "random_drops") == ChallengeService.Result.OK, "random drops activated");
                a = ChallengeRuntime.server();
                Block first = null;
                for (int i = 0; i < 2; i++) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                    check(h, !LuckyService.beforeBreak(level, p, pos, Blocks.STONE.defaultBlockState()), "break " + i);
                    items = level.getEntitiesOfClass(ItemEntity.class, box);
                    check(h, items.size() == 1, "one drop");
                    Block got = Block.byItem(items.get(0).getItem().getItem());
                    if (first == null) first = got;
                    check(h, got == first, "stone always gives the same block");
                    items.get(0).discard();
                }
            } finally {
                Gate.creativeCheck = net.minecraft.world.entity.player.Player::isCreative;
                ChallengeService.deactivate(server);
                wipe(level, pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
            }
            h.succeed();
        });

        ALL.put("preset_modes_new", h -> {
            String head = "{\"schemaVersion\":1,\"name\":\"X\",\"modes\":[";
            var lucky = PresetParser.parse("x", head + "{\"type\":\"lucky\"}]}");
            check(h, lucky.ok() && lucky.preset().runnable() && lucky.preset().lucky().alpha() == 1.3 && lucky.preset().lucky().pity() == 6 && lucky.warnings().isEmpty(), "lucky defaults: " + lucky.errors());
            var drops = PresetParser.parse("x", head + "{\"type\":\"randomdrops\"}]}");
            check(h, drops.ok() && drops.preset().lucky().kind() == LuckyMode.Kind.FIXED, "random drops");
            var sky = PresetParser.parse("x", head + "{\"type\":\"skyline\",\"layout\":\"ring\",\"easterEggs\":true}]}");
            check(h, sky.ok() && sky.preset().skyline().layout() == Layout.RING && !sky.preset().skyline().hasEggs(), "a ring never has eggs");
            var linear = PresetParser.parse("x", head + "{\"type\":\"skyline\"}]}");
            check(h, linear.ok() && linear.preset().skyline().layout() == Layout.LINEAR && linear.preset().skyline().hasEggs(), "skyline defaults: linear, eggs");
            String[] bad = {head + "{\"type\":\"lucky\",\"alpha\":9}]}", head + "{\"type\":\"lucky\",\"pity\":-1}]}", head + "{\"type\":\"lucky\"},{\"type\":\"randomdrops\"}]}",
                    head + "{\"type\":\"skyline\",\"layout\":\"spiral\"}]}", head + "{\"type\":\"skyline\"},{\"type\":\"skyline\"}]}",
                    head + "{\"type\":\"skyline\"},{\"type\":\"unlock\"}]}", head + "{\"type\":\"skyline\"},{\"type\":\"oneblock\"}]}"};
            for (String text : bad) check(h, !PresetParser.parse("x", text).ok(), "refused: " + text);
            var planned = PresetParser.parse("x", head + "{\"type\":\"pillarrace\"}]}");
            check(h, planned.ok() && !planned.preset().runnable() && planned.warnings().size() == 1, "a later mode loads with a note");
            // the 16 presets of the concept
            List<String> names = List.of("Vanilla Unlock", "Block by Block", "Quick Steps", "Trophy Land", "Island Unlock", "Classic Skyblock", "Sky One Block", "Lucky Strike", "Lucky Land",
                    "Skyline Block", "Skyline Grid", "Skyline Ring", "Lucky One Block", "Sky Grid", "One Line", "Random Drops");
            check(h, PresetRegistry.SHIPPED.size() == 16, "16 shipped presets");
            Set<String> got = new HashSet<>();
            for (String id : PresetRegistry.SHIPPED) {
                Preset p = PresetRegistry.get(id);
                check(h, p != null && p.runnable() && p.warnings().isEmpty(), id + " runnable without warnings: " + PresetRegistry.problems());
                got.add(p.name());
                var back = PresetParser.parse("user/copy", PresetDraft.from(p).toJson());
                check(h, back.ok(), id + " draft reads back: " + back.errors());
                check(h, java.util.Objects.equals(back.preset().lucky(), p.lucky()) && java.util.Objects.equals(back.preset().skyline(), p.skyline()), id + " draft keeps lucky and skyline");
            }
            check(h, got.equals(new HashSet<>(names)), "the names of the concept: " + got);
            check(h, PresetRegistry.get("lucky_land").unlock() != null && PresetRegistry.get("lucky_land").lucky() != null && PresetRegistry.get("lucky_one_block").world() != null
                    && PresetRegistry.get("lucky_one_block").lucky() != null, "lucky combines with unlock and one block");
            check(h, PresetRegistry.get("skyline_block").skyline().layout() == Layout.LINEAR && PresetRegistry.get("skyline_grid").skyline().layout() == Layout.GRID
                    && PresetRegistry.get("skyline_ring").skyline().layout() == Layout.RING && PresetRegistry.get("skyline_block").skyline().hasEggs() && !PresetRegistry.get("skyline_ring").skyline().hasEggs(), "skyline layouts");
            check(h, PresetRegistry.get("one_line").unlock().start().size() == 1 && PresetRegistry.get("one_line").unlock().start().get(0)[2] == 31, "one line is a corridor");
            // the state keeps what the new modes remember
            ChallengeState s = new ChallengeState();
            s.luckyBreaks = 7;
            s.sealOpened = true;
            s.eggs.add("minecraft:overworld|main");
            Tag tag = ChallengeState.CODEC.encodeStart(NbtOps.INSTANCE, s).getOrThrow();
            ChallengeState back = ChallengeState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
            check(h, back.luckyBreaks == 7 && back.sealOpened && back.eggs.equals(s.eggs), "the state is saved");
            h.succeed();
        });

        ALL.put("skyline_world", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            BlockPos at = h.absolutePos(new BlockPos(0, 40, 0));
            LevelChunk chunk = level.getChunkAt(at);
            BlockPos o = new BlockPos(chunk.getPos().getMinBlockX(), at.getY(), chunk.getPos().getMinBlockZ() + 2);
            BlockPos oldOverride = SkylineService.originOverride;
            SkylineService.originOverride = o;
            Gate.creativeCheck = pl -> false; // mock players always report creative
            try {
                check(h, ChallengeService.activate(server, "skyline_block") == ChallengeService.Result.OK, "activated: " + PresetRegistry.problems());
                Active a = ChallengeRuntime.server();
                check(h, a.skyline != null && a.skyline.overworld().blocks.get(0) == Blocks.GRASS_BLOCK && a.skyline.overworld().posOf(1).equals(o.offset(2, 0, 0)), "plan from the preset, origin overridden");
                check(h, level.getBlockState(o).isAir(), "nothing was built at activation (test origin)");
                // a small own plan: grass, sand (falls), torch (needs a floor), door (two halves), stone, then the seal
                SkylinePlans.Dim d = line(o, true, 96, BlockCatalog.Dim.OVERWORLD, List.of(Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.TORCH, Blocks.OAK_DOOR, Blocks.STONE));
                a.skyline = plans(new SkylineMode(Layout.LINEAR, false), d);
                check(h, SkylineService.fillChunk(d, chunk) == 6, "six places in this chunk");
                check(h, level.getBlockState(o).is(Blocks.GRASS_BLOCK) && level.getBlockState(o.offset(2, 0, 0)).is(Blocks.SAND) && level.getBlockState(o.offset(2, -1, 0)).is(Blocks.BEDROCK), "grass, then sand on bedrock");
                check(h, level.getBlockState(o.offset(4, 0, 0)).is(Blocks.TORCH) && level.getBlockState(o.offset(4, -1, 0)).is(Blocks.BEDROCK), "torch on bedrock");
                check(h, level.getBlockState(o.offset(6, 0, 0)).is(Blocks.OAK_DOOR) && level.getBlockState(o.offset(6, 1, 0)).is(Blocks.OAK_DOOR) && level.getBlockState(o.offset(6, -1, 0)).is(Blocks.BEDROCK), "door with both halves");
                check(h, level.getBlockState(o.offset(8, 0, 0)).is(Blocks.STONE) && level.getBlockState(o.offset(8, -1, 0)).isAir() && level.getBlockState(o.offset(10, 0, 0)).is(Blocks.BEDROCK), "stone alone, the seal is bedrock");
                check(h, level.getBlockState(o.offset(1, 0, 0)).isAir() && level.getBlockState(o.offset(0, 0, 2)).isAir(), "one block of air between, nothing off the line");
                // a broken block grows back within the tick (hook queues, tick puts it back); a replaced one too
                level.setBlock(o.offset(2, 0, 0), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(o.offset(8, 0, 0), Blocks.DIRT.defaultBlockState(), 3);
                SkylineService.tick(a);
                check(h, level.getBlockState(o.offset(2, 0, 0)).is(Blocks.SAND) && level.getBlockState(o.offset(8, 0, 0)).is(Blocks.STONE), "broken and replaced line blocks grew back");
                level.setBlock(o.offset(6, 0, 0), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(o.offset(6, 1, 0), Blocks.AIR.defaultBlockState(), 3);
                SkylineService.tick(a);
                check(h, level.getBlockState(o.offset(6, 0, 0)).is(Blocks.OAK_DOOR) && level.getBlockState(o.offset(6, 1, 0)).is(Blocks.OAK_DOOR), "the door grew back whole");
                // the seal: wrong tools do nothing, the right ones open it and build the End portal
                BlockPos seal = o.offset(10, 0, 0);
                ServerPlayer p = h.makeMockServerPlayerInLevel();
                p.setGameMode(GameType.SURVIVAL);
                check(h, !SkylineService.attack(p, level, seal) && !a.state.sealOpened && level.getBlockState(seal).is(Blocks.BEDROCK), "an empty hand does not open the seal");
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
                check(h, !SkylineService.attack(p, level, seal) && !a.state.sealOpened, "no eye, no opening");
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_EYE, 2));
                check(h, !SkylineService.attack(p, level, o.offset(8, 0, 0)), "other blocks are not the seal");
                check(h, SkylineService.attack(p, level, seal) && a.state.sealOpened && level.getBlockState(seal).isAir(), "the seal opens");
                BlockPos mid = seal.offset(4, 0, 0);
                check(h, level.getBlockState(mid).is(Blocks.END_PORTAL) && level.getBlockState(mid.offset(1, 0, 1)).is(Blocks.END_PORTAL), "portal blocks 3 x 3");
                check(h, level.getBlockState(mid.offset(-2, 0, 0)).is(Blocks.END_PORTAL_FRAME) && level.getBlockState(mid.offset(-2, 0, 0)).getValue(EndPortalFrameBlock.HAS_EYE)
                        && level.getBlockState(mid.offset(2, 0, 2)).isAir() && level.getBlockState(mid.offset(0, 0, -2)).is(Blocks.END_PORTAL_FRAME), "twelve frames with eyes, no corners");
                check(h, p.getOffhandItem().getCount() == 1, "one eye used up");
                SkylineService.tick(a);
                check(h, level.getBlockState(seal).isAir(), "an opened seal does not grow back");
                // the End arrival: Vanilla's obsidian platform goes, the line blocks of that spot stay
                BlockPos c = o.offset(2, 0, 0);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setBlock(c.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState(), 2);
                SkylineService.clearPlatform(level, d, c);
                check(h, level.getBlockState(c.offset(1, 0, 1)).isAir() && level.getBlockState(c.offset(-2, 0, -2)).isAir() && level.getBlockState(c).is(Blocks.SAND)
                        && level.getBlockState(c.offset(2, 0, 0)).is(Blocks.TORCH), "platform cleared, line blocks put back");
                // safety: without a challenge a broken line block stays broken
                ChallengeService.deactivate(server);
                level.setBlock(o, Blocks.AIR.defaultBlockState(), 3);
                ChallengeService.tick(server);
                check(h, level.getBlockState(o).isAir(), "inert without a challenge");
            } finally {
                Gate.creativeCheck = net.minecraft.world.entity.player.Player::isCreative;
                ChallengeService.deactivate(server);
                SkylineService.originOverride = oldOverride;
                wipe(level, o.offset(-3, -3, -6), o.offset(22, 4, 6));
            }
            h.succeed();
        });

        ALL.put("easter_eggs", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            // geometry: two sites at the same distance, the backup 30 degrees aside; none for a ring
            SkylinePlans.Dim endLine = new SkylinePlans.Dim(BlockCatalog.Dim.END, Level.END, Layout.LINEAR, 100, 48, 0, List.of(Blocks.END_STONE), false, SkylinePlans.END_EGG_DISTANCE);
            List<Eggs.Site> sites = Eggs.sites(endLine, true);
            double d1 = Math.hypot(sites.get(0).anchor().getX() - 100, sites.get(0).anchor().getZ()), d2 = Math.hypot(sites.get(1).anchor().getX() - 100, sites.get(1).anchor().getZ());
            check(h, sites.size() == 2 && sites.get(0).anchor().getX() == 100 - 190 && sites.get(0).anchor().getZ() == 0 && Math.abs(d1 - d2) < 1.5 && sites.get(1).anchor().getZ() > 90
                    && sites.get(0).kind() == Eggs.Kind.END_SHIP, "end sites " + sites);
            check(h, Eggs.sites(endLine, false).isEmpty(), "eggs off: no sites");
            check(h, Eggs.kindOf(BlockCatalog.Dim.NETHER) == Eggs.Kind.NETHER_BASTION && Eggs.kindOf(BlockCatalog.Dim.OVERWORLD) == Eggs.Kind.OVERWORLD_VAULT, "kinds by dimension");
            String loot = null;
            try (var in = SkylineTests.class.getResourceAsStream("/data/simplechallenges/loot_table/vault/overworld.json")) {
                loot = in == null ? null : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                loot = null;
            }
            check(h, loot != null && loot.contains("minecraft:heavy_core") && loot.contains("minecraft:vault"), "the vault loot table guarantees a Heavy Core");
            BlockPos a = h.absolutePos(new BlockPos(4, 40, 4));
            BlockPos oldOverride = SkylineService.originOverride;
            SkylineService.originOverride = a; // the activation below must not build at the real origin
            try {
                // end ship with the Elytra frame, refilled when someone takes the Elytra
                Eggs.build(level, new Eggs.Site("t", Eggs.Kind.END_SHIP, a));
                check(h, level.getBlockState(a).is(Blocks.PURPUR_BLOCK) && level.getBlockState(a.offset(-4, 1, 0)).is(Blocks.PURPUR_BLOCK) && level.getBlockState(a.offset(4, 1, 0)).isAir(), "ship: floor, back wall, open front");
                List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, new AABB(a.offset(-3, 1, 0)));
                check(h, frames.size() == 1 && frames.get(0).getItem().is(Items.ELYTRA), "the Elytra hangs in its frame");
                frames.get(0).setItem(ItemStack.EMPTY, false);
                check(h, Eggs.keep(level, new Eggs.Site("t", Eggs.Kind.END_SHIP, a)) == 1 && frames.get(0).getItem().is(Items.ELYTRA), "a taken Elytra comes back");
                check(h, Eggs.keep(level, new Eggs.Site("t", Eggs.Kind.END_SHIP, a)) == 0, "a full frame is left alone");
                wipe(level, a.offset(-5, -1, -3), a.offset(5, 5, 3));
                // mini bastion: the chest always holds the smithing template
                Eggs.build(level, new Eggs.Site("t", Eggs.Kind.NETHER_BASTION, a));
                check(h, level.getBlockEntity(a.offset(-2, 1, 0)) instanceof ChestBlockEntity c && c.getItem(0).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), "bastion chest has the smithing template");
                check(h, level.getBlockState(a.offset(3, 1, 0)).isAir() && level.getBlockState(a.offset(3, 1, 1)).is(Blocks.POLISHED_BLACKSTONE_BRICKS), "bastion door");
                wipe(level, a.offset(-4, -1, -4), a.offset(4, 5, 4));
                // vault: block entity points at the Heavy Core loot table, the key frame is kept
                Eggs.build(level, new Eggs.Site("t", Eggs.Kind.OVERWORLD_VAULT, a));
                BlockPos vault = a.offset(-1, 1, 0);
                check(h, level.getBlockState(vault).is(Blocks.VAULT) && level.getBlockEntity(vault) != null, "vault stands");
                String data = level.getBlockEntity(vault).saveWithFullMetadata(level.registryAccess()).toString();
                check(h, data.contains(Eggs.LOOT_VAULT) && data.contains("trial_key"), "vault is configured: " + data);
                List<ItemFrame> keys = level.getEntitiesOfClass(ItemFrame.class, new AABB(a.offset(0, 1, -1)));
                check(h, keys.size() == 1 && keys.get(0).getItem().is(Items.TRIAL_KEY), "the key frame");
                wipe(level, a.offset(-4, -1, -4), a.offset(4, 5, 4));
                // the generation hook queues a site whose chunk is generated, the tick builds it once
                BlockPos o = new BlockPos(a.getX() + 96, a.getY(), a.getZ());
                ChallengeService.deactivate(server);
                check(h, ChallengeService.activate(server, "skyline_block") == ChallengeService.Result.OK, "activated");
                Active act = ChallengeRuntime.server();
                SkylinePlans.Dim d = line(o, false, 96, BlockCatalog.Dim.OVERWORLD, List.of(Blocks.GRASS_BLOCK));
                act.skyline = plans(new SkylineMode(Layout.LINEAR, true), d);
                Eggs.Site main = Eggs.sites(d, true).get(0);
                check(h, main.anchor().equals(a), "main egg of the test plan lies at the test spot");
                SkylineService.chunkGenerated(level, level.getChunkAt(a));
                check(h, act.state.eggs.isEmpty() && level.getBlockState(a).isAir(), "queued, not built yet");
                SkylineService.tick(act);
                check(h, act.state.eggs.contains(Eggs.key(d, main)) && level.getBlockState(vault).is(Blocks.VAULT), "built by the tick and remembered");
                SkylineService.chunkGenerated(level, level.getChunkAt(a));
                SkylineService.tick(act);
                check(h, act.state.eggs.size() == 1, "a built site is not built twice");
            } finally {
                ChallengeService.deactivate(server);
                SkylineService.originOverride = oldOverride;
                wipe(level, a.offset(-6, -2, -6), a.offset(6, 6, 6));
            }
            h.succeed();
        });
    }
}
