package com.simplebuilding.modules.simpletweaks.protect;

import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

/**
 * GameTests of the protection features (plan docs/ai/PLAN-TWEAKS-PROTECT-2026-10-17.md), registered per loader as
 * "protect_game_test_…". Tests that change the settings do so within one tick and restore them before returning.
 */
public final class ProtectTests {
    public static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("config_defaults_and_bounds", ProtectTests::config);
        TESTS.put("commands_follow_switches", ProtectTests::commands);
        TESTS.put("obfuscated_chunk_hides_enclosed_ore", ProtectTests::hides);
        TESTS.put("exempt_admin_sees_real_blocks", ProtectTests::exempt);
        TESTS.put("mining_reveals_neighbours", ProtectTests::reveals);
        TESTS.put("fake_ores_mode_two", ProtectTests::fakes);
        TESTS.put("bench_cost_logged", ProtectTests::bench);
        TESTS.put("freecam_rule_payload_and_kick", ProtectTests::freecam);
        TESTS.put("seed_hash_randomised", ProtectTests::seed);
        TESTS.put("hide_health_of_others", ProtectTests::health);
        TESTS.put("checks_flag_far_break_not_server_breaks", ProtectTests::reach);
        TESTS.put("fast_break_uses_real_speed", ProtectTests::fastBreak);
        TESTS.put("hammer_area_break_not_flagged", ProtectTests::hammer);
        TESTS.put("auto_off_with_partner_mods", ProtectTests::autoOff);
    }
    public static int maxTicks(String name) { return 100; }

    static void yes(GameTestHelper h, boolean ok, String why) { h.assertTrue(ok, why); }
    static ServerPlayer player(GameTestHelper h, BlockPos rel, GameType mode) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(mode);
        var pos = h.absolutePos(rel);
        p.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0f, 0f);
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(p));
        return p;
    }
    static void with(ProtectConfig c, Runnable body) {
        var old = Protect.config();
        Protect.configure(c);
        try { body.run(); } finally { Protect.configure(old); }
    }
    /** A 3x3x3 cube of {@code host} at {@code rel} with {@code core} in the middle; returns the absolute middle. */
    static BlockPos cube(GameTestHelper h, BlockPos rel, Block host, Block core) {
        for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) h.setBlock(rel.offset(x, y, z), host);
        h.setBlock(rel.offset(1, 1, 1), core);
        return h.absolutePos(rel.offset(1, 1, 1));
    }
    /** What the chunk packet for {@code viewer} says is at {@code pos}: the sections decoded like the client does. */
    static BlockState shown(ServerPlayer viewer, ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        var data = AntiXray.packetFor(viewer, chunk);
        var buf = data.getReadBuffer();
        var factory = PalettedContainerFactory.create(level.registryAccess());
        int wanted = level.getSectionIndex(pos.getY());
        for (int i = 0; i < level.getSectionsCount(); i++) {
            var section = new LevelChunkSection(factory);
            section.read(buf);
            if (i == wanted) return section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }
        throw new AssertionError("section missing");
    }
    static BlockState host(BlockPos pos) { return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState(); }

    // ------------------------------------------------------------------ autoOffWith (partner mods)
    static void autoOff(GameTestHelper h) {
        var log = org.slf4j.LoggerFactory.getLogger("simpletweaks-autooff-test");
        java.util.function.Predicate<String> none = id -> false, ledger = "ledger"::equals, chunky = "chunky"::equals,
                lite = "litematica"::equals, axiom = "axiom"::equals, free = "freecam"::equals, view = "inv_view"::equals, restore = "invrestore"::equals;
        var a = com.simplebuilding.modules.simpletweaks.admin.AdminConfig.DEFAULT;
        yes(h, a.withAutoOff(none, log).equals(a), "No partner: nothing changes");
        yes(h, !a.withAutoOff(ledger, log).blockLog().enabled() && a.withAutoOff(ledger, log).invsee().enabled() && a.withAutoOff(ledger, log).deathBackups().enabled(), "Ledger switches off only the block log");
        yes(h, !a.withAutoOff("minetracer"::equals, log).blockLog().enabled(), "MineTracer too");
        yes(h, !a.withAutoOff(view, log).invsee().enabled() && a.withAutoOff(view, log).blockLog().enabled(), "Inv View (alternative id) switches off /invsee");
        yes(h, !a.withAutoOff(restore, log).deathBackups().enabled() && a.withAutoOff(restore, log).invsee().enabled(), "InvRestore switches off death backups");
        var p = com.simplebuilding.modules.simpletweaks.play.PlayConfig.DEFAULT;
        yes(h, p.pregen().enabled() && !p.withAutoOff(chunky, log).pregen().enabled() && p.withAutoOff(ledger, log).equals(p), "Chunky switches off pregen only");
        var c = ProtectConfig.of("{\"actionChecks\":{\"enabled\":true},\"freecam\":{\"enabled\":true}}");
        yes(h, c.actionChecks().enabled() && c.withAutoOff(none, log).equals(c), "No partner: protect unchanged");
        yes(h, !c.withAutoOff(lite, log).actionChecks().enabled() && !c.withAutoOff(axiom, log).actionChecks().enabled(), "Litematica/Axiom switch off the action checks");
        yes(h, c.withAutoOff(lite, log).antiXray().enabled(), "Anti-x-ray stays on by default");
        yes(h, c.withAutoOff(free, log).equals(c), "Freecam: hint only, nothing switched off");
        var x = ProtectConfig.of("{\"autoOffWith\":{\"antiXray\":[\"axiom\",\"BAD ID\"],\"actionChecks\":[]}}");
        yes(h, x.autoOffWith().get("antiXray").equals(List.of("axiom")) && !x.withAutoOff(axiom, log).antiXray().enabled(),
                "Admin list: ids validated, antiXray follows Axiom, emptied actionChecks list keeps the feature");
        yes(h, ProtectConfig.of("{\"actionChecks\":{\"enabled\":true},\"autoOffWith\":{\"actionChecks\":[]}}").withAutoOff(lite, log).actionChecks().enabled(), "An emptied list keeps the feature on");
        var m = com.simplebuilding.modules.simpletweaks.play.AutoOff.match(java.util.Map.of("a", List.of("x", "y")), id -> id.equals("y"));
        yes(h, m.equals(java.util.Map.of("a", "y")), "match returns the first loaded partner per feature");
        h.succeed();
    }

    // ------------------------------------------------------------------ config
    static void config(GameTestHelper h) {
        var d = ProtectConfig.DEFAULT;
        yes(h, d.antiXray().enabled(), "Owner 17.10.: anti-x-ray on");
        yes(h, !d.freecam().enabled() && !d.freecam().requireClient() && !d.seedHash().enabled() && !d.hideHealth().enabled() && !d.actionChecks().enabled(),
                "Every other protection off");
        yes(h, d.antiXray().dimensions().get("minecraft:overworld").hidden().contains("minecraft:diamond_ore")
                && d.antiXray().dimensions().get("minecraft:overworld").hidden().contains("#c:ores")
                && d.antiXray().dimensions().get("minecraft:the_nether").hidden().contains("minecraft:ancient_debris"), "Vanilla ores, ancient debris and c:ores");
        var a = new ProtectConfig.AntiXray(true, 7, 9, true, true, 12, Map.of("bad id!", ProtectConfig.TEMPLATE));
        yes(h, a.engineMode() == 2 && a.fakeOreChance() == 1.0 && a.exemptPermission() == 4 && a.dimensions().isEmpty(), "Anti-x-ray values clamped, bad ids dropped");
        var ch = new ProtectConfig.ActionChecks(true, -1, 5, 9, -5, true);
        yes(h, ch.reachSlack() == 0 && ch.fastBreakRatio() == 0.7 && ch.notifyPermission() == 4 && ch.notifyCooldownSeconds() == 0, "Check values clamped");
        try {
            Path file = Files.createTempDirectory("st-protect-config").resolve("p.json");
            yes(h, ProtectConfig.load(file).equals(ProtectConfig.DEFAULT) && Files.isRegularFile(file), "A missing file is written with the defaults");
            Files.writeString(file, "{\"freecam\":{\"enabled\":true},\"antiXray\":{\"dimensions\":{\"minecraft:overworld\":{\"maxY\":64},\"mymod:mines\":{\"replacement\":\"minecraft:andesite\"}}}}");
            var loaded = ProtectConfig.load(file);
            var ow = loaded.antiXray().dimensions().get("minecraft:overworld");
            var mine = loaded.antiXray().dimensions().get("mymod:mines");
            yes(h, loaded.freecam().enabled() && !loaded.freecam().requireClient() && ow.maxY() == 64 && ow.hidden().contains("minecraft:diamond_ore")
                    && mine != null && mine.replacement().equals("minecraft:andesite") && mine.maxY() == 4096 && mine.hidden().equals(List.of("#c:ores"))
                    && !loaded.antiXray().dimensions().containsKey("minecraft:the_nether"), "Dimension entries merge onto their default or the template");
            Files.writeString(file, "{broken");
            yes(h, ProtectConfig.load(file).equals(ProtectConfig.DEFAULT) && Files.readString(file).equals("{broken"), "A broken file is kept, defaults are used");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }

    static void commands(GameTestHelper h) {
        var d = new CommandDispatcher<CommandSourceStack>();
        ProtectCommands.register(d, ProtectConfig.DEFAULT);
        var tweaks = d.getRoot().getChild("tweaks");
        yes(h, tweaks != null && tweaks.getRequirement().test(h.getLevel().getServer().createCommandSourceStack()) && tweaks.getChild("protect") != null,
                "/tweaks protect is a branch; the /tweaks root keeps no requirement");
        var protect = tweaks.getChild("protect");
        var mirror = d.getRoot().getChild("simpletweaks");
        yes(h, mirror != null && mirror.getChild("protect") == protect, "/simpletweaks protect is the same branch");
        yes(h, protect.getChild("status") != null && protect.getChild("reload") != null && protect.getChild("bench") != null && protect.getChild("checks") != null, "Sub-commands");
        var p = player(h, new BlockPos(1, 2, 1), GameType.SURVIVAL);
        yes(h, !protect.getRequirement().test(p.createCommandSourceStack()), "Players without level 3 may not use it");
        yes(h, ProtectCommands.status(h.getLevel().getServer().createCommandSourceStack()) == 1, "Status answers");
        h.succeed();
    }

    // ------------------------------------------------------------------ anti-x-ray
    static void hides(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(0, 6, 0), GameType.SURVIVAL);
        var ore = cube(h, new BlockPos(1, 2, 1), Blocks.STONE, Blocks.DIAMOND_ORE);
        var deep = cube(h, new BlockPos(5, 2, 1), Blocks.DEEPSLATE, Blocks.DEEPSLATE_DIAMOND_ORE);
        var top = ore.above();
        level.setBlock(top, Blocks.IRON_ORE.defaultBlockState(), 3); // touches the air above: exposed
        var gold = h.absolutePos(new BlockPos(9, 2, 1));
        level.setBlock(gold, Blocks.GOLD_ORE.defaultBlockState(), 3); // alone in the air
        with(ProtectConfig.DEFAULT, () -> {
            yes(h, AntiXray.applies(p, level), "Applies to a survival player in the overworld");
            yes(h, shown(p, level, ore).equals(host(ore)), "Enclosed diamond ore is sent as its host: " + shown(p, level, ore));
            yes(h, shown(p, level, deep).is(Blocks.DEEPSLATE), "Enclosed deepslate ore is sent as deepslate: " + shown(p, level, deep));
            yes(h, shown(p, level, top).is(Blocks.IRON_ORE), "Exposed ore stays: " + shown(p, level, top));
            yes(h, shown(p, level, gold).is(Blocks.GOLD_ORE), "Ore next to air stays");
            yes(h, shown(p, level, ore.below()).is(Blocks.STONE), "Host blocks are untouched");
            yes(h, level.getBlockState(ore).is(Blocks.DIAMOND_ORE), "The world keeps the real ore");
        });
        with(ProtectConfig.of("{\"antiXray\":{\"enabled\":false}}"), () -> yes(h, shown(p, level, ore).is(Blocks.DIAMOND_ORE), "Switch off: real blocks"));
        with(ProtectConfig.of("{\"antiXray\":{\"dimensions\":{\"minecraft:the_nether\":{}}}}"), () -> yes(h, shown(p, level, ore).is(Blocks.DIAMOND_ORE), "Uncovered dimension: real blocks"));
        with(ProtectConfig.of("{\"antiXray\":{\"dimensions\":{\"minecraft:overworld\":{\"maxY\":" + (ore.getY() - 20) + "}}}}"),
                () -> yes(h, shown(p, level, ore).is(Blocks.DIAMOND_ORE), "Above maxY: real blocks"));
        h.succeed();
    }

    static void exempt(GameTestHelper h) {
        var level = h.getLevel();
        var ore = cube(h, new BlockPos(1, 2, 1), Blocks.STONE, Blocks.EMERALD_ORE);
        var creative = player(h, new BlockPos(0, 6, 0), GameType.CREATIVE);
        var spectator = player(h, new BlockPos(0, 6, 0), GameType.SPECTATOR);
        var survival = player(h, new BlockPos(0, 6, 0), GameType.SURVIVAL);
        with(ProtectConfig.DEFAULT, () -> {
            yes(h, shown(creative, level, ore).is(Blocks.EMERALD_ORE), "Creative admins see real blocks");
            yes(h, shown(spectator, level, ore).is(Blocks.EMERALD_ORE), "Spectators see real blocks");
            yes(h, !shown(survival, level, ore).is(Blocks.EMERALD_ORE), "Survival players do not");
        });
        with(ProtectConfig.of("{\"antiXray\":{\"exemptCreative\":false,\"exemptPermission\":0}}"), () -> {
            yes(h, shown(survival, level, ore).is(Blocks.EMERALD_ORE), "Permission exemption (level 0 = everyone)");
        });
        with(ProtectConfig.of("{\"antiXray\":{\"exemptCreative\":false}}"), () -> yes(h, !shown(creative, level, ore).is(Blocks.EMERALD_ORE), "Creative exemption can be switched off"));
        h.succeed();
    }

    static void reveals(GameTestHelper h) {
        var level = h.getLevel();
        var ore = cube(h, new BlockPos(1, 2, 1), Blocks.STONE, Blocks.DIAMOND_ORE);
        var ore2 = cube(h, new BlockPos(6, 2, 1), Blocks.STONE, Blocks.REDSTONE_ORE);
        var seen = new ArrayList<BlockPos>();
        var old = AntiXray.reveal;
        AntiXray.reveal = (l, pos) -> seen.add(pos);
        try {
            with(ProtectConfig.DEFAULT, () -> {
                level.setBlock(ore.east(), Blocks.AIR.defaultBlockState(), 3); // mining a neighbour
                yes(h, seen.contains(ore), "Mining a neighbour re-sends the hidden ore: " + seen);
                seen.clear();
                level.setBlock(h.absolutePos(new BlockPos(1, 5, 1)), Blocks.STONE.defaultBlockState(), 3);
                yes(h, seen.isEmpty(), "A change without hidden neighbours sends nothing extra");
                level.explode(null, ore2.getX() + 0.5, ore2.getY() + 2.5, ore2.getZ() + 0.5, 2.5f, Level.ExplosionInteraction.TNT);
                yes(h, seen.contains(ore2) || !level.getBlockState(ore2).is(Blocks.REDSTONE_ORE), "An explosion re-sends ores next to the crater: " + seen);
            });
        } finally { AntiXray.reveal = old; }
        var p = player(h, new BlockPos(0, 6, 0), GameType.SURVIVAL);
        with(ProtectConfig.DEFAULT, () -> yes(h, shown(p, level, ore).is(Blocks.DIAMOND_ORE), "Once exposed, the ore is sent as it is"));
        h.succeed();
    }

    static void fakes(GameTestHelper h) {
        var level = h.getLevel();
        var center = cube(h, new BlockPos(1, 2, 1), Blocks.STONE, Blocks.STONE);
        var p = player(h, new BlockPos(0, 6, 0), GameType.SURVIVAL);
        with(ProtectConfig.of("{\"antiXray\":{\"engineMode\":2,\"fakeOreChance\":1.0}}"), () -> {
            var s = shown(p, level, center);
            var fakes = ProtectConfig.DEFAULT.antiXray().dimensions().get("minecraft:overworld").fakeOres();
            yes(h, fakes.contains(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()), "Mode 2: an enclosed host block becomes a fake ore: " + s);
            yes(h, shown(p, level, center.above()).is(Blocks.STONE), "Visible stone stays stone");
            yes(h, shown(p, level, center).equals(s), "The fake ore is the same on every send");
        });
        with(ProtectConfig.DEFAULT, () -> yes(h, shown(p, level, center).is(Blocks.STONE), "Mode 1 never adds ores"));
        h.succeed();
    }

    static void bench(GameTestHelper h) {
        var level = h.getLevel();
        // a synthetic, ore-rich underground chunk far away (not in the level; borders count as open)
        var chunk = new LevelChunk(level, new ChunkPos(200_000, 200_000));
        var rnd = new Random(17);
        var ores = List.of(Blocks.DIAMOND_ORE, Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE);
        int sections = Math.min(8, chunk.getSections().length);
        for (int i = 0; i < sections; i++) {
            var s = chunk.getSections()[i];
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
                s.setBlockState(x, y, z, (rnd.nextInt(100) < 3 ? ores.get(rnd.nextInt(ores.size())) : rnd.nextInt(100) < 4 ? Blocks.AIR : Blocks.STONE).defaultBlockState(), false);
        }
        with(ProtectConfig.DEFAULT, () -> {
            AntiXray.bench(chunk, 40); // warm up (a running server has the code compiled)
            var t = AntiXray.bench(chunk, 40);
            double vanilla = t[0] / 40 / 1e6, hidden = t[1] / 40 / 1e6;
            org.slf4j.LoggerFactory.getLogger("simpletweaks-protect").info("Anti-x-ray bench: {} sections, Vanilla {} ms, obfuscated {} ms per chunk",
                    sections, String.format(Locale.ROOT, "%.3f", vanilla), String.format(Locale.ROOT, "%.3f", hidden));
            yes(h, hidden < 50, "Obfuscating an ore-rich chunk stays cheap: " + hidden + " ms");
            var bytes = AntiXray.obfuscate(chunk);
            yes(h, bytes.length > 0, "Bench chunk serializes");
            yes(h, AntiXray.obfuscate(chunk) == bytes, "A second viewer in the same second shares the computed chunk");
            AntiXray.forget(level, chunk.getPos().getWorldPosition());
            yes(h, AntiXray.obfuscate(chunk) != bytes, "A block change in the chunk drops the shared result");
        });
        h.succeed();
    }

    // ------------------------------------------------------------------ freecam
    static void freecam(GameTestHelper h) {
        var kicked = new ArrayList<ServerPlayer>();
        var old = FreecamRule.kick;
        FreecamRule.kick = (pl, reason) -> kicked.add(pl);
        try {
            var p = player(h, new BlockPos(1, 2, 1), GameType.SURVIVAL);
            with(ProtectConfig.DEFAULT, () -> { FreecamRule.apply(p); yes(h, kicked.isEmpty(), "Rule off: nobody is kicked"); });
            with(ProtectConfig.of("{\"freecam\":{\"enabled\":true}}"), () -> {
                yes(h, !FreecamRule.apply(p), "A client without Simple Tweaks cannot receive the rule");
                yes(h, kicked.isEmpty(), "Without requireClient nobody is kicked");
            });
            with(ProtectConfig.of("{\"freecam\":{\"enabled\":true,\"requireClient\":true}}"), () -> {
                FreecamRule.apply(p);
                yes(h, kicked.contains(p), "requireClient kicks clients without Simple Tweaks");
                yes(h, FreecamRule.kickReason(Protect.config().freecam()).getString().contains("Simple Tweaks"), "The reason names the missing mod");
            });
        } finally { FreecamRule.kick = old; }
        var payload = new ProtectRulesPayload(true);
        var buf = io.netty.buffer.Unpooled.buffer();
        ProtectRulesPayload.CODEC.encode(buf, payload);
        yes(h, ProtectRulesPayload.CODEC.decode(buf).equals(payload), "Rule payload round trip");
        h.succeed();
    }

    // ------------------------------------------------------------------ further protections
    static void seed(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1), GameType.SURVIVAL);
        long real = BiomeManager.obfuscateSeed(level.getSeed());
        with(ProtectConfig.DEFAULT, () -> yes(h, p.createCommonSpawnInfo(level).seed() == real, "Off: Vanilla's hashed seed"));
        with(ProtectConfig.of("{\"seedHash\":{\"enabled\":true}}"), () -> {
            long sent = p.createCommonSpawnInfo(level).seed();
            yes(h, sent != real, "On: a different hashed seed");
            yes(h, p.createCommonSpawnInfo(level).seed() == sent, "Stable within one server run");
        });
        h.succeed();
    }

    static void health(GameTestHelper h) {
        var a = player(h, new BlockPos(1, 2, 1), GameType.SURVIVAL);
        var b = player(h, new BlockPos(2, 2, 1), GameType.SURVIVAL);
        b.setHealth(5f);
        var id = HideHealth.healthId;
        yes(h, id != null, "Health slot known after start");
        var packet = new ClientboundSetEntityDataPacket(b.getId(), List.of(SynchedEntityData.DataValue.create(id, 5f)));
        with(ProtectConfig.DEFAULT, () -> yes(h, HideHealth.filter(a, packet) == packet, "Off: unchanged"));
        with(ProtectConfig.of("{\"hideHealth\":{\"enabled\":true}}"), () -> {
            var f = HideHealth.filter(a, packet);
            yes(h, f != packet && ((Float) f.packedItems().get(0).value()) == b.getMaxHealth(), "Others see the maximum: " + f.packedItems().get(0).value());
            yes(h, HideHealth.filter(b, packet) == packet, "Own health is real");
            var dead = new ClientboundSetEntityDataPacket(b.getId(), List.of(SynchedEntityData.DataValue.create(id, 0f)));
            yes(h, HideHealth.filter(a, dead) == dead, "0 stays 0 (death animation)");
            var pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(3, 2, 3));
            var mob = new ClientboundSetEntityDataPacket(pig.getId(), List.of(SynchedEntityData.DataValue.create(id, 3f)));
            yes(h, HideHealth.filter(a, mob) == mob, "Mobs are unchanged");
            pig.discard();
        });
        h.succeed();
    }

    static final String CHECKS_ON = "{\"actionChecks\":{\"enabled\":true,\"log\":false}}";

    static List<ActionChecks.Hit> hits(Runnable body) {
        var out = new ArrayList<ActionChecks.Hit>();
        var old = ActionChecks.listener;
        ActionChecks.listener = out::add;
        try { body.run(); } finally { ActionChecks.listener = old; }
        return out;
    }

    static void reach(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1), GameType.SURVIVAL);
        var far = h.absolutePos(new BlockPos(13, 2, 1));
        var far2 = h.absolutePos(new BlockPos(13, 2, 3));
        level.setBlock(far, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(far2, Blocks.STONE.defaultBlockState(), 3);
        with(ProtectConfig.of(CHECKS_ON), () -> {
            var got = hits(() -> p.gameMode.handleBlockBreakAction(far, Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0));
            yes(h, got.size() == 1 && got.get(0).kind() == ActionChecks.Kind.REACH_BLOCK, "A break packet 12 blocks away is flagged: " + got);
            var mod = new AttributeModifier(Identifier.fromNamespaceAndPath("simpletweaks", "test_reach"), 14, AttributeModifier.Operation.ADD_VALUE);
            p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).addTransientModifier(mod);
            got = hits(() -> p.gameMode.handleBlockBreakAction(far, Action.ABORT_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0));
            got.addAll(hits(() -> p.gameMode.handleBlockBreakAction(far, Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0)));
            yes(h, got.isEmpty(), "Reach from attributes (Range enchantment, trims, other mods) is respected: " + got);
            p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).removeModifier(mod);
            got = hits(() -> {
                p.gameMode.destroyBlock(far2); // what hammers/vein miners do for their extra blocks
                level.setBlock(h.absolutePos(new BlockPos(13, 3, 3)), Blocks.OAK_PLANKS.defaultBlockState(), 3); // what wands and blueprints do
            });
            yes(h, got.isEmpty() && level.getBlockState(far2).isAir(), "Server-side breaks and placements are never flagged: " + got);
            var pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(16, 2, 1));
            got = hits(() -> p.connection.handleAttack(new ServerboundAttackPacket(pig.getId())));
            yes(h, got.size() == 1 && got.get(0).kind() == ActionChecks.Kind.REACH_ENTITY, "An attack 15 blocks away is flagged: " + got);
            pig.discard();
        });
        with(ProtectConfig.DEFAULT, () -> yes(h, hits(() -> p.gameMode.handleBlockBreakAction(far, Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0)).isEmpty(),
                "Checks off: nothing is measured"));
        h.succeed();
    }

    static void fastBreak(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1), GameType.SURVIVAL);
        p.setOnGround(true); // mock players are not ticked; in the air Vanilla divides the speed by 5
        var acc = (com.simplebuilding.modules.simpletweaks.protect.mixin.ProtectGameModeAccessor) p.gameMode;
        var stone = h.absolutePos(new BlockPos(2, 2, 2));
        var obsidian = h.absolutePos(new BlockPos(1, 2, 2));
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(obsidian, Blocks.OBSIDIAN.defaultBlockState(), 3);
        with(ProtectConfig.of(CHECKS_ON), () -> {
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var got = hits(() -> {
                p.gameMode.handleBlockBreakAction(stone, Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0);
                p.gameMode.handleBlockBreakAction(stone, Action.STOP_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 1);
            });
            yes(h, got.size() == 1 && got.get(0).kind() == ActionChecks.Kind.FAST_BREAK, "Stone by hand stopped at once is flagged: " + got);
            yes(h, level.getBlockState(stone).is(Blocks.STONE), "Vanilla keeps the block");
            // Haste II + netherite: faster than any fixed Vanilla tool limit would allow (mock players are not ticked, so
            // enchantment attributes would not be applied yet; effects work at once)
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            int plain = (int) Math.ceil(0.75 / level.getBlockState(obsidian).getDestroyProgress(p, level, obsidian));
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE, 600, 1));
            float perTick = level.getBlockState(obsidian).getDestroyProgress(p, level, obsidian);
            int ticks = (int) Math.ceil(0.75 / perTick);
            yes(h, ticks < plain, "Netherite + Haste II is faster than a plain diamond pickaxe (" + ticks + " < " + plain + " ticks)");
            got = hits(() -> {
                p.gameMode.handleBlockBreakAction(obsidian, Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 2);
                acc.simpletweaks$setGameTicks(acc.simpletweaks$gameTicks() + ticks);
                p.gameMode.handleBlockBreakAction(obsidian, Action.STOP_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 3);
            });
            yes(h, got.isEmpty(), "Breaking at the real speed of tool and enchantments is not flagged: " + got);
            yes(h, level.getBlockState(obsidian).isAir(), "The obsidian broke");
        });
        h.succeed();
    }

    /** With SimpleBuilding loaded: a real sledgehammer swing (3x3 server-side area break) is never flagged. */
    static void hammer(GameTestHelper h) {
        var level = h.getLevel();
        var item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("simplehammers", "iron_sledgehammer"));
        if (item.isEmpty()) { h.succeed(); return; } // standalone: nothing to check
        var center = h.absolutePos(new BlockPos(2, 1, 2));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlock(center.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
        var p = player(h, new BlockPos(2, 2, 2), GameType.SURVIVAL);
        p.snapTo(center.getX() + .5, center.getY() + 1, center.getZ() + .5, 0f, 90f); // looking down
        p.setShiftKeyDown(false);
        p.setOnGround(true);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(item.get()));
        var acc = (com.simplebuilding.modules.simpletweaks.protect.mixin.ProtectGameModeAccessor) p.gameMode;
        with(ProtectConfig.of(CHECKS_ON), () -> {
            var got = hits(() -> {
                p.gameMode.handleBlockBreakAction(center, Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0);
                float perTick = level.getBlockState(center).getDestroyProgress(p, level, center);
                acc.simpletweaks$setGameTicks(acc.simpletweaks$gameTicks() + (int) Math.ceil(0.75 / Math.max(perTick, 1e-4f)));
                p.gameMode.handleBlockBreakAction(center, Action.STOP_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 1);
            });
            int broken = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (level.getBlockState(center.offset(dx, 0, dz)).isAir()) broken++;
            yes(h, broken >= 5, "The hammer broke its area (" + broken + " of 9)");
            yes(h, got.isEmpty(), "Hammer area breaking is not flagged: " + got);
        });
        h.succeed();
    }
    private ProtectTests() {}
}
