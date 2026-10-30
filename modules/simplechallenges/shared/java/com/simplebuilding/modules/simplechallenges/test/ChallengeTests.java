package com.simplebuilding.modules.simplechallenges.test;

import com.google.gson.JsonParser;
import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.client.ClientView;
import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import com.simplebuilding.modules.simplechallenges.core.Active;
import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.core.ChallengeService;
import com.simplebuilding.modules.simplechallenges.core.ChallengeState;
import com.simplebuilding.modules.simplechallenges.core.Credits;
import com.simplebuilding.modules.simplechallenges.core.Enforcement;
import com.simplebuilding.modules.simplechallenges.core.Gate;
import com.simplebuilding.modules.simplechallenges.core.LevelProbe;
import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import com.simplebuilding.modules.simplechallenges.core.UnlockService;
import com.simplebuilding.modules.simplechallenges.net.UnlockSyncPayload;
import com.simplebuilding.modules.simplechallenges.core.LookInfo;
import com.simplebuilding.modules.simplechallenges.core.PendingChallenge;
import com.simplebuilding.modules.simplechallenges.core.RecentChallenges;
import com.simplebuilding.modules.simplechallenges.net.CellInfoPayload;
import com.simplebuilding.modules.simplechallenges.preset.PresetDraft;
import com.simplebuilding.modules.simplechallenges.preset.StartArea;
import com.simplebuilding.modules.simplechallenges.preset.WorldMode;
import com.simplebuilding.modules.simplechallenges.sky.IslandPlan;
import com.simplebuilding.modules.simplechallenges.sky.OneBlockPhases;
import com.simplebuilding.modules.simplechallenges.sky.SkyGridPlan;
import com.simplebuilding.modules.simplechallenges.sky.SkyService;
import com.simplebuilding.modules.simplechallenges.preset.Currency;
import com.simplebuilding.modules.simplechallenges.preset.Difficulty;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.PresetParser;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import com.simplebuilding.modules.simplechallenges.price.BiomeMaterials;
import com.simplebuilding.modules.simplechallenges.price.BlockValues;
import com.simplebuilding.modules.simplechallenges.price.CellInfo;
import com.simplebuilding.modules.simplechallenges.price.Price;
import com.simplebuilding.modules.simplechallenges.price.PriceEngine;
import com.simplebuilding.modules.simplechallenges.price.Solvability;
import com.simplebuilding.modules.simplechallenges.price.WeightedItem;
import com.simplebuilding.modules.simplechallenges.unlock.LongSet;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side checks of Simple Challenges S0-S2 (bodies for the Fabric adapter {@code ModuleGameTest}, all on simplechallenges:empty):
 * the safety lock, the preset loader, the unlock grid, the enforcement (collision, block changes, rescue), the price engine, credits,
 * payment and the deadlock protection. Tests that switch a challenge on do it inside one tick and always switch it off again.
 */
public final class ChallengeTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    private static final String OW = "minecraft:overworld";

    private ChallengeTests() {}

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    private static List<WeightedItem> region() {
        return List.of(new WeightedItem("minecraft:dirt", 10, 64), new WeightedItem("minecraft:oak_log", 3, 64));
    }

    private static UnlockMode mode(int cell, Currency c, Difficulty d) {
        return new UnlockMode(cell, List.of(new int[]{-1, -1, 0, 0}), c, d, List.of(OW), 30, 1, 2, 5);
    }

    /** Removes an island built by a test (blocks and chest) so the test area is empty again. */
    private static void clear(ServerLevel level, BlockPos origin, IslandPlan plan) {
        for (IslandPlan.Block b : plan.blocks()) level.setBlock(origin.offset(b.dx(), b.dy(), b.dz()), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(origin.offset(plan.chestX, plan.chestY, plan.chestZ), Blocks.AIR.defaultBlockState(), 2);
    }

    static {
        ALL.put("config_defaults", h -> {
            ChallengeServerConfig.reset();
            check(h, ChallengeServerConfig.enabled && !ChallengeServerConfig.allowAdminCommands, "admin commands are off by default (safety lock)");
            check(h, "none".equals(ChallengeServerConfig.defaultChallenge), "no default challenge");
            check(h, ChallengeServerConfig.exemptCreative && ChallengeServerConfig.pityEnabled, "defaults");
            ChallengeServerConfig.apply(JsonParser.parseString("{\"defaultChallenge\":\"\",\"enabled\":{}}").getAsJsonObject());
            check(h, "none".equals(ChallengeServerConfig.defaultChallenge) && ChallengeServerConfig.enabled, "broken values use the defaults");
            ChallengeServerConfig.reset();
            check(h, ChallengeServerConfig.toJson().keySet().containsAll(ChallengeServerConfig.KEYS), "all keys written");
            h.succeed();
        });

        ALL.put("config_command", h -> {
            var spec = com.simplelib.api.config.ConfigCommands.spec(SimpleChallenges.MOD_ID, false);
            check(h, spec != null && spec.options().size() >= ChallengeServerConfig.KEYS.size(), "/simplechallenges config not announced");
            com.simplelib.test.ConfigCommandTests.roundTrip(h, SimpleChallenges.MOD_ID, "exemptCreative", "false", "false", "x");
            check(h, ChallengeServerConfig.exemptCreative, "reset to default");
            h.succeed();
        });

        // The safety lock: nothing chosen, nothing happens.
        ALL.put("safety_lock", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            ChallengeState state = ChallengeState.peek(server);
            check(h, state == null || !state.activated(), "no challenge chosen");
            check(h, ChallengeRuntime.server() == null && !Gate.active(level), "gate closed");
            Pig pig = h.spawn(EntityTypes.PIG, new Vec3(1.5, 2, 1.5));
            Vec3 far = new Vec3(5_000_000, 70, 5_000_000);
            check(h, Enforcement.clampMove(pig, far) == far, "movement is returned untouched (same instance)");
            check(h, !Enforcement.blocksChange(level, new BlockPos(5_000_000, 70, 5_000_000)), "block changes are not refused");
            double x = pig.getX();
            pig.move(MoverType.SELF, new Vec3(0.4, 0, 0.4));
            check(h, Math.abs(pig.getX() - (x + 0.4)) < 1.0E-6, "an entity moves exactly as in Vanilla: " + (pig.getX() - x));
            BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
            check(h, level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3) && level.getBlockState(pos).is(Blocks.STONE), "setBlock works");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Enforcement.tick(server);
            ChallengeService.tick(server);
            check(h, ChallengeRuntime.server() == null, "ticks do nothing");
            // even with a challenge chosen the mod switch wins
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleChallenges.MOD_ID);
            try {
                com.simplebuilding.framework.api.SubMods.set(SimpleChallenges.MOD_ID, false);
                check(h, !SimpleChallenges.on() && !Gate.active(level), "switched off");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleChallenges.MOD_ID, before);
            }
            h.succeed();
        });

        ALL.put("grid_logic", h -> {
            UnlockGrid g = new UnlockGrid(16);
            g.addRect(-1, -1, 0, 0);
            check(h, g.size() == 4 && g.isFreeAtBlock(-0.5, 0.5) && g.isFreeAtBlock(15.9, 15.9) && !g.isFreeAtBlock(16, 0), "cells by block");
            double dx = g.limitX(14.7, 15.3, 5, 5.6, 2);
            check(h, Math.abs(dx - (16 - UnlockGrid.GAP - 15.3)) < 1.0E-9, "stops at the edge: " + dx);
            check(h, g.limitX(14.7, 15.3, 5, 5.6, -2) == -2 && g.limitX(14.7, 15.3, 5, 5.6, 0.2) == 0.2, "free directions untouched");
            double dn = g.limitX(-15.9, -15.3, 5, 5.6, -2);
            check(h, dn > -0.2 && dn <= 0, "negative side: " + dn);
            double dz = g.limitZ(5, 5.6, 14.7, 15.3, 2);
            check(h, Math.abs(dz - dx) < 1.0E-9, "same along Z");
            check(h, g.boxFree(15, 16, 0, 1), "a box flush to the edge is free");
            double[] slide = g.clamp(15.2, 15.8, 0.2, 0.8, 5, 3);
            check(h, slide[0] < 0.2 && slide[1] == 3, "slides along the wall: " + slide[0] + "," + slide[1]);
            double[] inside = g.clamp(20, 20.6, 20, 20.6, 1, 1);
            check(h, inside[0] == 1 && inside[1] == 1, "a box already inside a locked cell is not trapped");
            check(h, g.frontier().length == 8 && g.touchesFree(1, 0) && !g.touchesFree(2, 0), "frontier");
            check(h, g.nearestFree(5, 0, 10) == UnlockGrid.key(0, 0) && g.nearestFree(100, 100, 3) == UnlockGrid.NONE, "nearest free cell");
            check(h, g.borderEdges(-2, -2, 1, 1).size() == 8, "border edges");
            UnlockGrid again = UnlockGrid.fromKeys(16, g.keys());
            check(h, again.size() == 4 && again.isFree(-1, -1) && again.isFree(0, 0), "keys round trip");
            UnlockGrid one = new UnlockGrid(1);
            for (int i = 0; i < 100_000; i++) one.unlock(i % 400, i / 400);
            check(h, one.size() == 100_000 && one.isFree(399, 249) && !one.isFree(400, 0) && !one.unlock(5, 5), "100000 cells");
            LongSet set = new LongSet();
            check(h, set.add(Long.MIN_VALUE) && !set.add(Long.MIN_VALUE) && set.contains(Long.MIN_VALUE) && set.size() == 1, "long set edge key");
            boolean refused = false;
            try {
                new UnlockGrid(15);
            } catch (IllegalArgumentException e) {
                refused = true;
            }
            check(h, refused, "cell size 15 refused");
            long ahead = UnlockService.lookedCell(g, 10, 5, 1, 0, 48);
            check(h, ahead == UnlockGrid.key(1, 0), "looking east from inside finds the cell beyond the edge");
            check(h, UnlockService.lookedCell(g, 10, 5, -1, 0, 4) == UnlockGrid.NONE, "nothing in reach");
            h.succeed();
        });

        ALL.put("preset_parser", h -> {
            String ok = "{\"schemaVersion\":1,\"name\":\"X\",\"modes\":[{\"type\":\"unlock\",\"cell\":16,\"start\":[[-1,-1,0,0]]},{\"type\":\"lucky\"}]}";
            var r = PresetParser.parse("x", ok);
            check(h, r.ok() && r.preset().unlock().cell() == 16 && r.warnings().size() == 1, "valid preset with a planned mode: " + r.errors());
            check(h, r.preset().unlock().currency() == Currency.ITEMS && r.preset().unlock().difficulty() == Difficulty.MEDIUM, "defaults");
            String[] bad = {
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"unlock\",\"cell\":15}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"unlock\"},{\"type\":\"unlock\",\"cell\":1}]}",
                    "{\"schemaVersion\":2,\"modes\":[{\"type\":\"unlock\"}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"bogus\"}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"unlock\",\"start\":[[0,0,200,200]]}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"unlock\",\"currency\":\"gold\"}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"unlock\",\"currency\":\"credits\"}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"unlock\",\"start\":[[0,0,1.5,3]]}]}",
                    "{\"schemaVersion\":1,\"modes\":[]}",
                    "not json at all"};
            for (String text : bad) check(h, !PresetParser.parse("x", text).ok() && !PresetParser.parse("x", text).errors().isEmpty(), "refused with a message: " + text);
            check(h, !PresetParser.parse("Bad Id", ok).ok(), "bad id refused");
            Preset planned = PresetParser.parse("sky", "{\"schemaVersion\":1,\"modes\":[{\"type\":\"skyline\"}]}").preset();
            check(h, planned != null && !planned.runnable(), "a preset with only planned modes loads but is not runnable");
            PresetRegistry.add("zz_broken", "{\"schemaVersion\":1,\"modes\":[]}", "test");
            check(h, PresetRegistry.get("zz_broken") == null && PresetRegistry.problems().stream().anyMatch(p -> p.startsWith("test:")), "a broken file is a readable problem, not a crash");
            h.succeed();
        });

        ALL.put("presets_shipped", h -> {
            for (String id : PresetRegistry.SHIPPED) {
                Preset p = PresetRegistry.get(id);
                check(h, p != null && p.runnable(), "shipped preset " + id + " loaded: " + PresetRegistry.problems());
            }
            check(h, PresetRegistry.get("vanilla_unlock").unlock().cell() == 16 && PresetRegistry.get("block_by_block").unlock().cell() == 1
                    && PresetRegistry.get("quick_steps").unlock().cell() == 48, "chunk, block and three-chunk unlock");
            check(h, PresetRegistry.get("trophy_land").unlock().currency() == Currency.CREDITS && PresetRegistry.get("trophy_land").unlock().earnsCredits(), "credits preset");
            BlockValues v = BlockValues.current();
            check(h, v.value("minecraft:diamond_ore") > 20 && v.value("minecraft:stone") < 0.1 && v.value("minecraft:oak_log") >= 0.5, "block value table");
            check(h, !BiomeMaterials.itemsFor("desert").isEmpty() && BiomeMaterials.itemsFor("desert").get(0).id().equals("minecraft:sand"), "desert gives sand");
            h.succeed();
        });

        ALL.put("price_engine", h -> {
            PriceEngine e = new PriceEngine(42, mode(16, Currency.ITEMS, Difficulty.MEDIUM));
            Price a = e.price(OW, 3, 4, CellInfo.EMPTY, region(), 4), b = e.price(OW, 3, 4, CellInfo.EMPTY, region(), 4);
            check(h, a.equals(b), "same cell, same price");
            Set<Price> seen = new HashSet<>();
            for (int i = 0; i < 60; i++) {
                Price p = e.price(OW, i, 0, CellInfo.EMPTY, region(), 4);
                seen.add(p);
                check(h, p.currency() == Currency.ITEMS && p.count() >= 1 && p.count() <= 128, "within two stacks: " + p);
            }
            check(h, seen.size() > 1, "prices vary by cell");
            check(h, Price.decode(a.encode()).equals(a) && Price.decode("junk") == null, "price codec");
            // the cell's own surroundings decide the medium tier
            PriceEngine own = new PriceEngine(1, mode(16, Currency.ITEMS, Difficulty.MEDIUM));
            Price desert = own.price(OW, 9, 9, new CellInfo(List.of(new WeightedItem("minecraft:sand", 8, 64)), 0, 64), region(), 4);
            check(h, desert.item().equals("minecraft:sand"), "medium uses the cell's own sand: " + desert);
            Price easy = new PriceEngine(1, mode(16, Currency.ITEMS, Difficulty.EASY)).price(OW, 9, 9, new CellInfo(List.of(new WeightedItem("minecraft:sand", 8, 64)), 0, 64), region(), 4);
            check(h, easy.item().equals("minecraft:dirt") || easy.item().equals("minecraft:oak_log"), "easy uses the start area's items: " + easy);
            check(h, easy.count() <= own.price(OW, 9, 9, CellInfo.EMPTY, region(), 4).count(), "easy is cheaper than medium");
            PriceEngine hard = new PriceEngine(1, mode(16, Currency.ITEMS, Difficulty.HARD));
            check(h, hard.price(OW, 0, 0, CellInfo.EMPTY, region(), 0).item().equals("minecraft:cobblestone")
                    && hard.price(OW, 0, 0, CellInfo.EMPTY, region(), 30).item().equals("minecraft:iron_ingot")
                    && hard.price(OW, 0, 0, CellInfo.EMPTY, region(), 400).item().equals("minecraft:diamond"), "hard follows the ladder by progress");
            PriceEngine block = new PriceEngine(1, mode(1, Currency.ITEMS, Difficulty.MEDIUM));
            Price plain = block.price(OW, 0, 0, new CellInfo(region(), 2, 64), region(), 0), ore = block.price(OW, 0, 0, new CellInfo(region(), 40, 64), region(), 0);
            check(h, plain.item().equals("minecraft:dirt") || plain.item().equals("minecraft:oak_log"), "a plain column costs plain items: " + plain);
            check(h, ore.item().equals("minecraft:diamond"), "a column with ore costs by its value: " + ore);
            check(h, block.price(OW, 0, 0, CellInfo.EMPTY, List.of(), 0).item().equals("minecraft:dirt"), "nothing to probe: dirt");
            check(h, new PriceEngine(1, mode(16, Currency.CREDITS, Difficulty.MEDIUM)).price(OW, 0, 0, CellInfo.EMPTY, region(), 0).equals(Price.credits(1)), "credits cost one credit");
            Price xp = new PriceEngine(1, mode(16, Currency.XP, Difficulty.MEDIUM)).price(OW, 0, 0, CellInfo.EMPTY, region(), 0);
            check(h, xp.currency() == Currency.XP && xp.count() >= 1 && xp.count() <= 30, "xp levels " + xp);
            check(h, UnlockMode.MAX_STACKS == 2 && mode(48, Currency.ITEMS, Difficulty.MEDIUM).creditsFor("goal") == 2 && mode(48, Currency.ITEMS, Difficulty.MEDIUM).creditsFor("challenge") == 5, "caps and credit frames");
            h.succeed();
        });

        ALL.put("solvability", h -> {
            UnlockGrid g = new UnlockGrid(16);
            g.addRect(-1, -1, 0, 0);
            check(h, Solvability.payableFrontier(g, k -> Price.items("minecraft:dirt", 1), item -> false).isEmpty(), "no obtainable item: stuck");
            check(h, Solvability.payableFrontier(g, k -> Price.items("minecraft:dirt", 1), item -> true).size() == 8, "all payable");
            check(h, Solvability.payableFrontier(g, k -> Price.credits(1), item -> false).size() == 8, "credits are always payable");
            check(h, Solvability.due(true, 100, 50, true) && !Solvability.due(true, 10, 50, true) && !Solvability.due(false, 100, 50, true)
                    && !Solvability.due(true, 100, 50, false), "emergency unlock timing");
            h.succeed();
        });

        ALL.put("state_codec", h -> {
            MinecraftServer server = h.getLevel().getServer();
            ChallengeService.deactivate(server);
            try {
                check(h, ChallengeService.activate(server, "vanilla_unlock") == ChallengeService.Result.OK, "activated");
                Active a = ChallengeRuntime.server();
                check(h, a != null && a.state.activated() && a.state.initialized && a.state.presetJson.contains("Vanilla Unlock"), "snapshot stored");
                a.state.credits = 7;
                a.state.rewarded.add("minecraft:story/mine_stone");
                a.state.prices.put("minecraft:overworld|1|0", Price.items("minecraft:dirt", 5).encode());
                UnlockService.unlock(a, OW, 1, 0);
                Tag tag = ChallengeState.CODEC.encodeStart(NbtOps.INSTANCE, a.state).getOrThrow();
                ChallengeState back = ChallengeState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
                check(h, back.challenge.equals("vanilla_unlock") && back.credits == 7 && back.unlockCount == 1 && back.rewarded.contains("minecraft:story/mine_stone"), "fields");
                check(h, java.util.Arrays.equals(back.cells(OW), a.state.cells(OW)) && back.cells(OW).length == a.grid(OW).size(), "free cells survive: " + back.cells(OW).length);
                check(h, !back.prices.containsKey("minecraft:overworld|1|0"), "a paid cell's price is dropped");
            } finally {
                ChallengeService.deactivate(server);
            }
            check(h, ChallengeRuntime.server() == null, "off again");
            h.succeed();
        });

        ALL.put("sync_payload", h -> {
            long[] keys = {UnlockGrid.key(-3, 7), UnlockGrid.key(0, 0), UnlockGrid.key(12, -9)};
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            UnlockSyncPayload.CODEC.encode(buf, new UnlockSyncPayload(OW, 16, true, false, keys));
            UnlockSyncPayload in = UnlockSyncPayload.CODEC.decode(buf);
            check(h, in.dimension().equals(OW) && in.cell() == 16 && in.full() && !in.exemptCreative() && java.util.Arrays.equals(in.keys(), keys), "payload round trip");
            ClientView view = new ClientView();
            view.apply(in.dimension(), in.cell(), in.full(), in.exemptCreative(), in.keys());
            view.apply(OW, 16, false, false, new long[]{UnlockGrid.key(5, 5)});
            check(h, view.grid(OW).size() == 4 && view.grid(OW).isFree(5, 5) && view.grid(OW).isFree(-3, 7) && view.grid("minecraft:the_nether") == null && !view.exemptCreative(), "client view applies full and delta");
            view.apply(OW, 16, true, true, new long[]{UnlockGrid.key(1, 1)});
            check(h, view.grid(OW).size() == 1, "a full packet replaces");
            h.succeed();
        });

        ALL.put("probe_no_chunks", h -> {
            ServerLevel level = h.getLevel();
            int far = 4000;
            check(h, level.getChunkSource().getChunkNow(far, far) == null, "far chunk not loaded");
            CellInfo info = LevelProbe.cell(level, 16, far, far);
            check(h, !info.reachable().isEmpty() && info.reachable().stream().allMatch(w -> w.weight() > 0 && w.id().contains(":")), "an unloaded cell is estimated from its biome: " + info.reachable());
            CellInfo column = LevelProbe.cell(level, 1, far * 16, far * 16);
            check(h, column.columnValue() >= 0, "column value estimate");
            check(h, level.getChunkSource().getChunkNow(far, far) == null && level.getChunkSource().getChunkNow(far * 16 >> 4, far * 16 >> 4) == null, "the probe generated no chunk");
            BlockPos here = h.absolutePos(BlockPos.ZERO);
            CellInfo near = LevelProbe.cell(level, 16, Math.floorDiv(here.getX(), 16), Math.floorDiv(here.getZ(), 16));
            check(h, !near.reachable().isEmpty(), "a loaded cell gives items");
            h.succeed();
        });

        ALL.put("enforcement_in_world", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            boolean exempt = ChallengeServerConfig.exemptCreative;
            ChallengeServerConfig.exemptCreative = false;
            String dim = OW;
            try {
                // the test level may not be the Overworld: a preset that covers exactly its dimension
                dim = Gate.dim(level);
                PresetRegistry.add("zz_test", "{\"schemaVersion\":1,\"name\":\"T\",\"modes\":[{\"type\":\"unlock\",\"cell\":16,\"start\":[[-1,-1,0,0]],\"dimensions\":[\"" + dim + "\"]}]}", "test");
                check(h, ChallengeService.activate(server, "zz_test") == ChallengeService.Result.OK, "activated: " + PresetRegistry.problems());
                Active a = ChallengeRuntime.server();
                UnlockGrid g = a.grid(dim);
                BlockPos origin = h.absolutePos(BlockPos.ZERO);
                int cx0 = g.cellOf(origin.getX()), cz0 = g.cellOf(origin.getZ());
                g.addRect(cx0 - 2, cz0 - 2, cx0 + 2, cz0 + 2);
                check(h, Gate.active(level), "gate open for the test level " + dim);
                // high above any terrain, so only the borders can stop the moves
                double edge = (cx0 + 3) * 16.0, y = Math.min(origin.getY() + 90, level.getMaxY() - 8);
                Pig pig = h.spawn(EntityTypes.PIG, new Vec3(1.5, 2, 1.5));
                pig.setPos(edge - 1.0, y, origin.getZ() + 8.5);
                double x0 = pig.getX();
                pig.move(MoverType.SELF, new Vec3(3, 0, 0));
                double half = pig.getBoundingBox().getXsize() / 2;
                check(h, pig.getX() > x0 + 0.3 && pig.getX() + half <= edge, "stops at the edge instead of crossing: x " + pig.getX() + " edge " + edge);
                pig.move(MoverType.SELF, new Vec3(-2, 0, 0));
                check(h, pig.getX() < edge - 2.0, "moving away is free: x " + pig.getX() + " edge " + edge + " y " + pig.getY());
                // block changes: refused in a locked cell, allowed in a free one
                BlockPos locked = new BlockPos((cx0 + 6) * 16 + 3, origin.getY() + 2, origin.getZ() + 3);
                check(h, Enforcement.blocksChange(level, locked) && !level.setBlock(locked, Blocks.STONE.defaultBlockState(), 3), "locked cell refuses setBlock");
                BlockPos free = h.absolutePos(new BlockPos(1, 2, 1));
                check(h, !Enforcement.blocksChange(level, free) && level.setBlock(free, Blocks.STONE.defaultBlockState(), 3), "free cell takes setBlock");
                level.setBlock(free, Blocks.AIR.defaultBlockState(), 3);
                // rescue: an entity found in a locked cell is brought back
                pig.setPos((cx0 + 8) * 16 + 4.5, y, origin.getZ() + 8.5);
                check(h, !g.isFreeAtBlock(pig.getX(), pig.getZ()), "pig is in a locked cell");
                Enforcement.rescue(a, pig, false);
                check(h, g.isFreeAtBlock(pig.getX(), pig.getZ()), "rescued into a free cell: x " + pig.getX());
                // creative players may pass when the server allows it (the mock player is always in creative)
                ChallengeServerConfig.exemptCreative = true;
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                player.setPos(edge - 1.0, y, origin.getZ() + 8.5);
                Vec3 want = new Vec3(3, 0, 0);
                check(h, Enforcement.clampMove(player, want) == want, "creative is exempt");
                ChallengeServerConfig.exemptCreative = false;
                Vec3 limited = Enforcement.clampMove(player, want);
                check(h, limited != want && limited.x > 0 && limited.x < 1, "limited when the server does not exempt creative: " + limited);
                // the switches close the gate while the challenge stays chosen
                boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleChallenges.MOD_ID);
                try {
                    com.simplebuilding.framework.api.SubMods.set(SimpleChallenges.MOD_ID, false);
                    check(h, !Gate.active(level) && !Enforcement.blocksChange(level, locked), "mod switch off: borders gone");
                } finally {
                    com.simplebuilding.framework.api.SubMods.set(SimpleChallenges.MOD_ID, before);
                }
                pig.discard();
            } finally {
                ChallengeServerConfig.exemptCreative = exempt;
                ChallengeService.deactivate(server);
                PresetRegistry.remove("zz_test");
            }
            check(h, !Gate.active(level) && !Enforcement.blocksChange(h.getLevel(), new BlockPos(5_000_000, 70, 0)), "off again: nothing refused");
            h.succeed();
        });

        ALL.put("unlock_pay_and_credits", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            try {
                check(h, ChallengeService.activate(server, "vanilla_unlock") == ChallengeService.Result.OK, "activated");
                Active a = ChallengeRuntime.server();
                UnlockGrid g = a.grid(OW);
                check(h, g.size() == 4 && g.isFree(0, 0) && g.isFree(-1, -1), "start area from the preset");
                check(h, !a.region().isEmpty(), "the start area offers items");
                long[] frontier = g.frontier();
                check(h, frontier.length == 8, "frontier of the 2x2 start");
                check(h, !Solvability.payableFrontier(g, k -> UnlockService.priceOf(a, OW, UnlockGrid.keyX(k), UnlockGrid.keyZ(k)), a::obtainable).isEmpty(), "solvable at the start");
                // items
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                int cx = 1, cz = 0;
                a.state.prices.put(OW + "|" + cx + "|" + cz, Price.items("minecraft:dirt", 8).encode());
                check(h, UnlockService.tryUnlock(a, player, OW, cx, cz) == UnlockService.Outcome.CANNOT_PAY && !g.isFree(cx, cz), "no items: cannot pay");
                player.getInventory().add(new ItemStack(Items.DIRT, 20));
                check(h, UnlockService.tryUnlock(a, player, OW, cx, cz) == UnlockService.Outcome.OK && g.isFree(cx, cz), "paid with 8 dirt");
                check(h, UnlockService.count(player, "minecraft:dirt") == 12, "12 dirt left: " + UnlockService.count(player, "minecraft:dirt"));
                check(h, UnlockService.tryUnlock(a, player, OW, cx, cz) == UnlockService.Outcome.ALREADY, "already free");
                check(h, UnlockService.tryUnlock(a, player, OW, 40, 40) == UnlockService.Outcome.NOT_ADJACENT, "must touch the free area");
                check(h, a.state.unlockCount == 1 && a.state.lastUnlockTick == server.overworld().getGameTime(), "progress counted");
                // credits
                a.state.prices.put(OW + "|2|0", Price.credits(2).encode());
                a.state.credits = 1;
                check(h, UnlockService.tryUnlock(a, player, OW, 2, 0) == UnlockService.Outcome.CANNOT_PAY, "one credit is not enough");
                a.state.credits = 3;
                check(h, UnlockService.tryUnlock(a, player, OW, 2, 0) == UnlockService.Outcome.OK && a.state.credits == 1, "paid with 2 credits");
                // levels
                a.state.prices.put(OW + "|3|0", Price.xp(2).encode());
                player.giveExperienceLevels(1);
                check(h, UnlockService.tryUnlock(a, player, OW, 3, 0) == UnlockService.Outcome.CANNOT_PAY, "one level is not enough");
                player.giveExperienceLevels(2);
                check(h, UnlockService.tryUnlock(a, player, OW, 3, 0) == UnlockService.Outcome.OK && player.experienceLevel == 1, "paid with 2 levels");
                // emergency unlock: stuck (nothing payable) after the pity time
                a.state.lastUnlockTick = server.overworld().getGameTime() - 60L * 20L * 60L;
                int before = g.size();
                for (long k : g.frontier()) a.state.prices.put(OW + "|" + UnlockGrid.keyX(k) + "|" + UnlockGrid.keyZ(k), Price.items("minecraft:elytra", 1).encode());
                UnlockService.pity(a, false);
                check(h, g.size() == before, "no timer without players");
                UnlockService.pity(a, true);
                check(h, g.size() == before + 1, "the emergency unlock opened one cell: " + before + " -> " + g.size());
                check(h, !Solvability.payableFrontier(g, k -> UnlockService.priceOf(a, OW, UnlockGrid.keyX(k), UnlockGrid.keyZ(k)), a::obtainable).isEmpty(), "solvable again");
                // credits from advancements (Trophy Land): the first poll is the baseline, later ones pay once
                ChallengeService.deactivate(server);
                check(h, ChallengeService.activate(server, "trophy_land") == ChallengeService.Result.OK, "trophy land activated");
                Active t = ChallengeRuntime.server();
                Credits.poll(t, List.of());
                Credits.poll(t, List.of(player));
                check(h, t.state.baselined.contains(player.getStringUUID()) && t.state.credits == 0, "baseline pays nothing");
                var holder = server.getAdvancements().get(Identifier.withDefaultNamespace("story/mine_stone"));
                if (holder != null) {
                    for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
                    Credits.poll(t, List.of(player));
                    check(h, t.state.credits == 1 && t.state.rewarded.contains("minecraft:story/mine_stone"), "a task pays 1 credit: " + t.state.credits);
                    Credits.poll(t, List.of(player));
                    check(h, t.state.credits == 1, "once per advancement");
                }
            } finally {
                ChallengeService.deactivate(server);
            }
            h.succeed();
        });

        // ---- S3/S4 ----

        ALL.put("sky_plans", h -> {
            for (String style : WorldMode.ISLAND_STYLES) {
                IslandPlan p = IslandPlan.of(style);
                check(h, p.covers(0, 0), style + ": the island covers the spawn column");
                boolean top = false, free = true, chestFree = true;
                for (IslandPlan.Block b : p.blocks()) {
                    check(h, BuiltInRegistries.BLOCK.containsKey(Identifier.parse(b.block())), style + ": known block " + b.block());
                    if (b.dx() == 0 && b.dz() == 0 && b.dy() == 0) top = true;
                    if (b.dx() == 0 && b.dz() == 0 && b.dy() >= IslandPlan.standY() && b.dy() <= IslandPlan.standY() + 1) free = false;
                    if (b.dx() == p.chestX && b.dz() == p.chestZ && b.dy() == p.chestY) chestFree = false;
                }
                check(h, top && free && chestFree, style + ": top layer at the spawn, two free blocks to stand in, chest place free");
                check(h, !p.chest.isEmpty() && p.chest.stream().allMatch(c -> BuiltInRegistries.ITEM.containsKey(Identifier.parse(c.item())) && c.count() > 0), style + ": chest items");
                check(h, !IslandPlan.reachable(style).isEmpty(), style + ": price pool");
            }
            check(h, OneBlockPhases.block(7, 0).equals("minecraft:grass_block"), "the first block is grass");
            check(h, OneBlockPhases.block(7, 55).equals(OneBlockPhases.block(7, 55)), "same seed, same block");
            int differing = 0;
            for (int n = 1; n <= 200; n++) if (!OneBlockPhases.block(1, n).equals(OneBlockPhases.block(2, n))) differing++;
            check(h, differing > 20, "another seed gives another sequence: " + differing);
            check(h, OneBlockPhases.phaseOf(0) == 0 && OneBlockPhases.phaseOf(OneBlockPhases.PHASE_LENGTH) == 1 && OneBlockPhases.phaseOf(10_000_000) == OneBlockPhases.PHASES.size() - 1, "phases");
            for (int n = 1; n <= 1500; n++) {
                String id = OneBlockPhases.block(42, n);
                final int count = n;
                check(h, OneBlockPhases.PHASES.get(OneBlockPhases.phaseOf(count)).stream().anyMatch(e -> e.block().equals(id)), "block " + n + " belongs to its phase: " + id);
            }
            for (var phase : OneBlockPhases.PHASES) for (var e : phase) check(h, BuiltInRegistries.BLOCK.containsKey(Identifier.parse(e.block())), "known one block " + e.block());
            for (var e : SkyGridPlan.BLOCKS) check(h, BuiltInRegistries.BLOCK.containsKey(Identifier.parse(e.block())), "known grid block " + e.block());
            check(h, SkyGridPlan.blockAt(1, 4, 0, 64, 0) != null && SkyGridPlan.blockAt(1, 4, 1, 64, 0) == null && SkyGridPlan.blockAt(1, 4, -4, -64, 8) != null, "nodes every 4 blocks, also below zero");
            Set<String> kinds = new HashSet<>();
            int[] count = {0};
            int n = SkyGridPlan.nodes(9, 4, 1600, 1600, -64, 319, (x, y, z, id) -> {
                count[0]++;
                kinds.add(id);
            });
            check(h, n == count[0] && n == SkyGridPlan.nodesPerChunk(4, -64, 320) && n == 16 * 96, "1536 nodes in a chunk column: " + n);
            check(h, kinds.size() >= 12, "a varied grid: " + kinds.size());
            check(h, SkyGridPlan.isStarter(0, 64, 0, 64) && SkyGridPlan.isStarter(7, 70, -7, 64) && !SkyGridPlan.isStarter(8, 64, 0, 64) && !SkyGridPlan.isStarter(0, 80, 0, 64), "the start platform is left alone");
            h.succeed();
        });

        ALL.put("preset_world_modes", h -> {
            String both = "{\"schemaVersion\":1,\"name\":\"Both\",\"modes\":[{\"type\":\"skyblock\",\"style\":\"mini\"},{\"type\":\"unlock\",\"cell\":2,\"start\":[[-1,-1,0,0]]}]}";
            var r = PresetParser.parse("both", both);
            check(h, r.ok() && r.preset().runnable() && r.preset().world().kind() == WorldMode.Kind.ISLAND && r.preset().world().style().equals("mini") && r.preset().unlock().cell() == 2, "island + unlock: " + r.errors());
            String[] bad = {
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"skyblock\",\"style\":\"bogus\"}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"skygrid\",\"spacing\":1}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"skygrid\",\"spacing\":9}]}",
                    "{\"schemaVersion\":1,\"modes\":[{\"type\":\"oneblock\"},{\"type\":\"skygrid\"}]}"};
            for (String text : bad) check(h, !PresetParser.parse("x", text).ok(), "refused: " + text);
            check(h, PresetRegistry.get("classic_skyblock").world().kind() == WorldMode.Kind.ISLAND && PresetRegistry.get("classic_skyblock").unlock() == null, "classic skyblock has no borders");
            check(h, PresetRegistry.get("sky_one_block").world().kind() == WorldMode.Kind.ONEBLOCK && PresetRegistry.get("sky_grid").world().spacing() == 4, "one block and sky grid");
            check(h, PresetRegistry.get("island_unlock").world() != null && PresetRegistry.get("island_unlock").unlock().cell() == 2, "island unlock combines both");
            // the editor's draft writes a file that reads back identically
            for (String id : PresetRegistry.SHIPPED) {
                Preset p = PresetRegistry.get(id);
                var back = PresetParser.parse("user/copy", PresetDraft.from(p).toJson());
                check(h, back.ok(), id + " draft reads back: " + back.errors());
                Preset q = back.preset();
                check(h, q.name().equals(p.name()) && q.icon().equals(p.icon()) && (q.world() == null) == (p.world() == null) && (q.unlock() == null) == (p.unlock() == null), id + " draft keeps the modes");
                if (p.world() != null) check(h, q.world().kind() == p.world().kind() && q.world().style().equals(p.world().style()), id + " draft keeps the world mode");
                if (p.unlock() != null) {
                    check(h, q.unlock().cell() == p.unlock().cell() && q.unlock().currency() == p.unlock().currency() && q.unlock().difficulty() == p.unlock().difficulty()
                            && StartArea.cells(q.unlock().start()).equals(StartArea.cells(p.unlock().start())) && q.unlock().creditsChallenge() == p.unlock().creditsChallenge(), id + " draft keeps the unlock mode");
                }
            }
            PresetDraft draft = PresetDraft.from(PresetRegistry.get("vanilla_unlock"));
            draft.setCurrency(Currency.CREDITS);
            draft.cell = 8;
            var credits = PresetParser.parse("user/c", draft.toJson());
            check(h, credits.ok() && credits.preset().unlock().earnsCredits() && credits.preset().unlock().cell() == 8, "switching the draft to credits is a valid preset: " + credits.errors());
            // the start area: rectangles <-> cells
            var l = new java.util.ArrayList<int[]>(List.of(new int[]{-1, -1, 0, 0}, new int[]{1, -1, 3, -1}, new int[]{-1, 1, -1, 4}));
            Set<Long> cells = StartArea.cells(l);
            check(h, cells.size() == 4 + 3 + 4 && cells.contains(UnlockGrid.key(0, 0)), "L shape cells: " + cells.size());
            check(h, StartArea.cells(StartArea.cover(cells)).equals(cells), "cover gives exactly the cells");
            check(h, StartArea.cover(cells).size() <= 3, "few rectangles: " + StartArea.cover(cells).size());
            java.util.Random random = new java.util.Random(5);
            for (int round = 0; round < 20; round++) {
                Set<Long> set = new HashSet<>();
                set.add(UnlockGrid.key(0, 0));
                for (int i = 0; i < 60; i++) set.add(UnlockGrid.key(random.nextInt(9) - 4, random.nextInt(9) - 4));
                check(h, StartArea.cells(StartArea.cover(set)).equals(set), "random set " + round + " covered exactly");
            }
            check(h, StartArea.cells(List.of()).equals(Set.of(UnlockGrid.key(0, 0))), "the spawn cell is always there");
            int[] bounds = StartArea.bounds(cells);
            check(h, bounds[0] == -1 && bounds[1] == -1 && bounds[2] == 3 && bounds[3] == 4, "bounds");
            // recent challenges and the pending choice
            check(h, RecentChallenges.push(List.of("a", "b", "c"), "c").equals(List.of("c", "a", "b")) && RecentChallenges.push(List.of("a", "b", "c"), "d").equals(List.of("d", "a", "b")), "recent: front, no duplicates, three at most");
            check(h, RecentChallenges.shown(List.of()).equals(RecentChallenges.DEFAULTS) && RecentChallenges.shown(List.of("x")).equals(List.of("x", "classic_skyblock", "sky_one_block")), "recent: defaults fill the free places");
            PendingChallenge.set("sky_grid");
            check(h, "sky_grid".equals(PendingChallenge.peek()) && "sky_grid".equals(PendingChallenge.take()) && PendingChallenge.peek() == null && PendingChallenge.take() == null, "the choice is taken once");
            h.succeed();
        });

        ALL.put("sky_world_modes", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            BlockPos origin = h.absolutePos(new BlockPos(0, 40, 0));
            BlockPos oldOrigin = SkyService.origin;
            SkyService.origin = origin;
            SkyService.levelOverride = level;
            IslandPlan plan = IslandPlan.of("classic");
            try {
                // classic island: the plan stands, the chest holds its items, the world has no borders
                check(h, ChallengeService.activate(server, "classic_skyblock") == ChallengeService.Result.OK, "activated: " + PresetRegistry.problems());
                Active a = ChallengeRuntime.server();
                check(h, a.mode == null && a.grid(Gate.dim(level)) == null && !Gate.active(level), "a world mode alone has no borders");
                check(h, a.state.worldBuilt && level.getBlockState(origin).is(Blocks.GRASS_BLOCK) && level.getBlockState(origin.below()).is(Blocks.DIRT) && level.getBlockState(origin.below(3)).is(Blocks.BEDROCK), "island built");
                check(h, level.getBlockState(origin.offset(0, 1, 5)).is(Blocks.OAK_LOG), "the tree stands");
                BlockPos chest = origin.offset(plan.chestX, plan.chestY, plan.chestZ);
                check(h, level.getBlockEntity(chest) instanceof ChestBlockEntity c && c.getItem(0).is(Items.ICE) && c.getItem(0).getCount() == 2, "the chest holds the items");
                ChallengeService.tick(server);
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                check(h, LookInfo.of(a, player) == CellInfoPayload.NONE, "no crosshair line without borders");
                Credits.poll(a, List.of(player));
                ChallengeService.deactivate(server);
                check(h, ChallengeRuntime.server() == null, "off again");
                clear(level, origin, plan);
                // island + unlock: the island's items are the price pool, the cells lie over the island
                check(h, ChallengeService.activate(server, "island_unlock") == ChallengeService.Result.OK, "island unlock activated");
                a = ChallengeRuntime.server();
                String dim = a.dimensions().iterator().next();
                check(h, a.mode != null && a.region().stream().anyMatch(w -> w.id().equals("minecraft:dirt")), "the island offers dirt");
                check(h, a.grid(dim).size() == 4 && a.grid(dim).isFree(0, 0), "start area 2 x 2 cells");
                ChallengeService.deactivate(server);
                clear(level, origin, plan);
                // one block: grows back with the next block, the phases advance
                check(h, ChallengeService.activate(server, "sky_one_block") == ChallengeService.Result.OK, "one block activated");
                a = ChallengeRuntime.server();
                check(h, level.getBlockState(origin).is(Blocks.GRASS_BLOCK) && a.state.oneBlockCount == 0, "the first block stands");
                check(h, !SkyService.refill(a), "nothing to refill while the block stands");
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                check(h, SkyService.refill(a) && a.state.oneBlockCount == 1, "a mined block comes back");
                check(h, level.getBlockState(origin).getBlock() == SkyService.state(OneBlockPhases.block(a.state.seed, 1)).getBlock(), "it is the next block of the sequence");
                a.state.oneBlockCount = OneBlockPhases.PHASE_LENGTH - 1;
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                check(h, SkyService.refill(a) && OneBlockPhases.phaseOf(a.state.oneBlockCount) == 1, "the next phase starts after " + OneBlockPhases.PHASE_LENGTH + " blocks");
                ChallengeService.deactivate(server);
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
            } finally {
                ChallengeService.deactivate(server);
                SkyService.origin = oldOrigin;
                SkyService.levelOverride = null;
                clear(level, origin, plan);
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
            }
            h.succeed();
        });

        ALL.put("lean_spawn_look", h -> {
            ServerLevel level = h.getLevel();
            MinecraftServer server = level.getServer();
            ChallengeService.deactivate(server);
            boolean exempt = ChallengeServerConfig.exemptCreative;
            ChallengeServerConfig.exemptCreative = false;
            try {
                String dim = Gate.dim(level);
                PresetRegistry.add("zz_lean", "{\"schemaVersion\":1,\"name\":\"L\",\"modes\":[{\"type\":\"unlock\",\"cell\":16,\"start\":[[-1,-1,0,0]],\"dimensions\":[\"" + dim + "\"]}]}", "test");
                check(h, ChallengeService.activate(server, "zz_lean") == ChallengeService.Result.OK, "activated");
                Active a = ChallengeRuntime.server();
                UnlockGrid g = a.grid(dim);
                BlockPos origin = h.absolutePos(BlockPos.ZERO);
                int cx0 = g.cellOf(origin.getX()), cz0 = g.cellOf(origin.getZ());
                g.addRect(cx0 - 2, cz0 - 2, cx0 + 2, cz0 + 2);
                double edge = (cx0 + 3) * 16.0, y = Math.min(origin.getY() + 90, level.getMaxY() - 8);
                // leaning over the edge: a sneaking player's body reaches LEAN further
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                player.setPos(edge - 1.0, y, origin.getZ() + 8.5);
                Vec3 want = new Vec3(3, 0, 0);
                Vec3 plain = Enforcement.clampMove(player, want);
                player.setShiftKeyDown(true);
                Vec3 leaning = Enforcement.clampMove(player, want);
                check(h, Math.abs(leaning.x - plain.x - Enforcement.LEAN) < 1.0E-3 && plain.x > 0.3, "sneaking leans over the edge: " + plain.x + " -> " + leaning.x);
                player.setShiftKeyDown(false);
                // no new mob appears in a locked cell, one in a free cell does
                var pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
                pig.setPos((cx0 + 8) * 16 + 4.5, y, origin.getZ() + 8.5);
                check(h, Enforcement.refusesSpawn(level, pig) && !level.addFreshEntity(pig), "a mob is not added to a locked cell");
                var pig2 = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
                pig2.setPos(edge - 4.5, y, origin.getZ() + 8.5);
                check(h, !Enforcement.refusesSpawn(level, pig2) && level.addFreshEntity(pig2), "a mob is added to a free cell");
                pig2.discard();
                // the crosshair line: looking east at the border shows the first locked cell, its price and whether it can be paid
                player.setPos(edge - 2.0, y, origin.getZ() + 8.5);
                player.setYRot(-90);
                player.setXRot(0);
                CellInfoPayload info = LookInfo.of(a, player);
                check(h, info.show() && info.blockX() == (int) edge && info.count() > 0 && !info.currency().isEmpty(), "the looked cell has a price: " + info);
                player.setXRot(80);
                check(h, LookInfo.of(a, player) == CellInfoPayload.NONE, "looking down shows nothing");
                FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                CellInfoPayload.CODEC.encode(buf, info);
                check(h, CellInfoPayload.CODEC.decode(buf).equals(info), "cell info payload round trip");
                // a pot of credits per player
                ChallengeService.deactivate(server);
                check(h, ChallengeService.activate(server, "trophy_land") == ChallengeService.Result.OK, "trophy land activated");
                a = ChallengeRuntime.server();
                ServerPlayer p1 = h.makeMockServerPlayerInLevel(), p2 = h.makeMockServerPlayerInLevel();
                ChallengeServerConfig.perPlayerCredits = true;
                try {
                    Credits.add(a, p1, 3);
                    check(h, Credits.of(a, p1) == 3 && Credits.of(a, p2) == 0 && a.state.credits == 0, "each player has their own pot");
                    a.state.prices.put(OW + "|1|0", Price.credits(2).encode());
                    check(h, UnlockService.tryUnlock(a, p2, OW, 1, 0) == UnlockService.Outcome.CANNOT_PAY, "the other player cannot pay with it");
                    check(h, UnlockService.tryUnlock(a, p1, OW, 1, 0) == UnlockService.Outcome.OK && Credits.of(a, p1) == 1, "paid from the own pot");
                    Credits.add(a, p1, -5);
                    check(h, Credits.of(a, p1) == 0, "never below zero");
                    Tag tag = ChallengeState.CODEC.encodeStart(NbtOps.INSTANCE, a.state).getOrThrow();
                    check(h, ChallengeState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().playerCredits.equals(a.state.playerCredits), "the pots are saved");
                } finally {
                    ChallengeServerConfig.perPlayerCredits = false;
                }
                Credits.add(a, p1, 4);
                check(h, Credits.of(a, p1) == 4 && Credits.of(a, p2) == 4 && a.state.credits == 4, "off: one pot for the team");
            } finally {
                ChallengeServerConfig.exemptCreative = exempt;
                ChallengeServerConfig.perPlayerCredits = false;
                ChallengeService.deactivate(server);
                PresetRegistry.remove("zz_lean");
            }
            h.succeed();
        });
    }
}
