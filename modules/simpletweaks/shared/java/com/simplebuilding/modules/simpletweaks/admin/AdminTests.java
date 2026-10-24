package com.simplebuilding.modules.simpletweaks.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.admin.backup.Backups;
import com.simplebuilding.modules.simpletweaks.admin.blocklog.*;
import com.simplebuilding.modules.simpletweaks.admin.deaths.DeathBackups;
import com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze;
import com.simplebuilding.modules.simpletweaks.admin.invsee.*;
import com.simplebuilding.modules.simpletweaks.admin.lag.LagFinder;
import com.simplebuilding.modules.simpletweaks.admin.maintenance.Maintenance;
import com.simplebuilding.modules.simpletweaks.admin.restart.Restart;
import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** GameTests of the admin features (plan docs/ai/PLAN-TWEAKS-ADMIN-2026-10-17.md). Registered per loader under "admin_…". */
public final class AdminTests {
    public static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("config_defaults_and_bounds", AdminTests::config);
        TESTS.put("simpletweaks_root_mirrors_all_commands", AdminTests::simpletweaksRoot);
        TESTS.put("commands_follow_switches", AdminTests::commands);
        TESTS.put("blocklog_records_breaks_places_containers", AdminTests::blockLog);
        TESTS.put("blocklog_inspect_does_not_break", AdminTests::inspect);
        TESTS.put("blocklog_rollback", AdminTests::rollback);
        TESTS.put("blocklog_retention_and_format", AdminTests::retention);
        TESTS.put("blocklog_signs_explosions_fire_hoppers", AdminTests::worldEvents);
        TESTS.put("blocklog_lookup_filters", AdminTests::lookupFilters);
        TESTS.put("blocklog_preview_and_restore", AdminTests::previewRestore);
        TESTS.put("blocklog_purge_player", AdminTests::purgePlayer);
        TESTS.put("blocklog_command_permissions", AdminTests::blockLogPermissions);
        TESTS.put("blocklog_legacy_lines_readable", AdminTests::legacyLines);
        TESTS.put("deaths_backup_on_death_and_restore", AdminTests::deaths);
        TESTS.put("deaths_offline_restore", AdminTests::deathsOffline);
        TESTS.put("invsee_live_take_and_rules", AdminTests::invseeLive);
        TESTS.put("invsee_offline_write_back", AdminTests::invseeOffline);
        TESTS.put("vanish_hides_and_ignores", AdminTests::vanish);
        TESTS.put("vanish_no_vibrations", AdminTests::vibrations);
        TESTS.put("freeze_holds_player", AdminTests::freeze);
        TESTS.put("restart_countdown", AdminTests::restart);
        TESTS.put("backup_zip_and_rotation", AdminTests::backup);
        TESTS.put("maintenance_login_and_motd", AdminTests::maintenance);
        TESTS.put("lag_finder_samples", AdminTests::lag);
    }

    /** Time limits of the tests that wait (async IO, countdowns, sculk); all others finish in their first tick. */
    public static int maxTicks(String name) {
        return switch (name) {
            case "blocklog_inspect_does_not_break", "lag_finder_samples", "freeze_holds_player" -> 100;
            case "vanish_no_vibrations", "restart_countdown" -> 200;
            case "backup_zip_and_rotation" -> 1200;
            default -> 100;
        };
    }

    static void yes(GameTestHelper h, boolean ok, String why) { h.assertTrue(ok, why); }
    static ServerPlayer player(GameTestHelper h, BlockPos rel) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        var pos = h.absolutePos(rel);
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return p;
    }
    static ServerPlayer op(GameTestHelper h, BlockPos rel, LevelBasedPermissionSet level) {
        var p = player(h, rel);
        h.getLevel().getServer().getPlayerList().op(p.nameAndId(), Optional.of(level), Optional.empty());
        return p;
    }
    static void deop(GameTestHelper h, ServerPlayer... players) { for (var p : players) h.getLevel().getServer().getPlayerList().deop(p.nameAndId()); }
    static <T> T get(java.util.concurrent.CompletableFuture<T> f) {
        try { return f.get(30, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception e) { throw new AssertionError(e); }
    }

    // ------------------------------------------------------------------ config + commands
    static void config(GameTestHelper h) {
        var d = AdminConfig.DEFAULT;
        yes(h, d.blockLog().enabled() && d.deathBackups().enabled() && d.invsee().enabled() && d.vanish().enabled() && d.freeze().enabled()
                && d.restart().enabled() && d.backups().enabled() && d.maintenance().enabled() && d.lagFinder().enabled(), "Owner defaults: T10 T11 T13-T18 and T34 on");
        yes(h, !new AdminState().maintenance, "Maintenance mode itself starts off");
        var b = new AdminConfig.BlockLog(true, 9, -3, 0, 0, 999);
        yes(h, b.permission() == 4 && b.rollbackPermission() == 0 && b.retentionDays() == 1 && b.rollbackMaxChanges() == 1 && b.flushSeconds() == 60, "Block log values are clamped");
        var r = new AdminConfig.Restart(true, 3, List.of("04:00", "25:00", "x", "7:30"), List.of(1, 15, 15, 500), 999);
        yes(h, r.times().equals(List.of("04:00", "7:30")) && r.warnMinutes().equals(List.of(120, 15, 1)) && r.exitCode() == 255, "Restart times/warnings/exit code are validated");
        try {
            Path dir = Files.createTempDirectory("st-admin-config");
            Path file = dir.resolve("a.json");
            yes(h, AdminConfig.load(file).equals(AdminConfig.DEFAULT) && Files.isRegularFile(file), "A missing file is written with the defaults");
            Files.writeString(file, "{\"lagFinder\":{\"enabled\":false},\"blockLog\":{\"retentionDays\":3},\"unknown\":1}");
            var loaded = AdminConfig.load(file);
            yes(h, !loaded.lagFinder().enabled() && loaded.lagFinder().permission() == 2 && loaded.blockLog().retentionDays() == 3 && loaded.blockLog().enabled(), "Partial files merge onto the defaults");
            Files.writeString(file, "{broken");
            yes(h, AdminConfig.load(file).equals(AdminConfig.DEFAULT) && Files.readString(file).equals("{broken"), "A broken file is kept, defaults are used");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }
    /** Every short command and every /tweaks branch has a /simpletweaks twin with the same permission, and help lists only the allowed ones. */
    static void simpletweaksRoot(GameTestHelper h) {
        var d = new CommandDispatcher<CommandSourceStack>();
        AdminCommands.register(d, AdminConfig.DEFAULT);
        com.simplebuilding.modules.simpletweaks.play.PlayCommands.register(d, com.simplebuilding.modules.simpletweaks.play.PlayConfig.DEFAULT);
        com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(d, com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.DEFAULT);
        com.simplebuilding.modules.simpletweaks.protect.ProtectCommands.register(d, com.simplebuilding.modules.simpletweaks.protect.ProtectConfig.DEFAULT);
        var root = d.getRoot().getChild("simpletweaks");
        yes(h, root != null && root.getRequirement().test(null) , "/simpletweaks exists without a requirement");
        var weak = player(h, new BlockPos(1, 2, 1)).createCommandSourceStack();
        var owner = op(h, new BlockPos(2, 2, 1), LevelBasedPermissionSet.OWNER);
        var strong = owner.createCommandSourceStack();
        int checked = 0;
        for (var n : d.getRoot().getChildren()) {
            if (n.getName().equals("simpletweaks")) continue;
            var twins = n.getName().equals("tweaks") ? n.getChildren() : List.of(n);
            for (var t : twins) {
                var twin = root.getChild(t.getName());
                yes(h, twin != null, "Missing /simpletweaks " + t.getName());
                for (var s : List.of(weak, strong)) yes(h, twin.canUse(s) == t.canUse(s), "Permission differs for " + t.getName());
                yes(h, twin.getCommand() == t.getCommand() || twin.getRedirect() == t, "Not the same executor: " + t.getName());
                checked++;
            }
        }
        var live = h.getLevel().getServer().getCommands().getDispatcher().getRoot();
        var liveTweaks = live.getChild("tweaks");
        var liveTwins = live.getChild("simpletweaks");
        if (liveTweaks != null) for (var t : liveTweaks.getChildren()) yes(h, liveTwins != null && liveTwins.getChild(t.getName()) != null, "Live dispatcher: missing /simpletweaks " + t.getName());
        yes(h, checked >= 20, "Checked " + checked + " commands");
        yes(h, root.getChild("help") != null, "help exists");
        var weakList = com.simplebuilding.modules.simpletweaks.TweakCommands.allowed(d, weak);
        var strongList = com.simplebuilding.modules.simpletweaks.TweakCommands.allowed(d, strong);
        yes(h, weakList.contains("help") && weakList.contains("pvp") && !weakList.contains("invsee") && !weakList.contains("vanish"), "Weak help: " + weakList);
        yes(h, strongList.containsAll(List.of("help", "pvp", "invsee", "vanish", "inspect", "lag", "claim")), "Strong help: " + strongList);
        var parsed = d.parse("simpletweaks pvp on", weak);
        yes(h, !parsed.getReader().canRead() && parsed.getContext().build("simpletweaks pvp on").getCommand() != null, "/simpletweaks pvp on parses to the command");
        var denied = d.parse("simpletweaks invsee Dev", weak);
        yes(h, denied.getReader().canRead() || denied.getContext().build("simpletweaks invsee Dev").getCommand() == null, "Weak source cannot run /simpletweaks invsee");
        deop(h, owner);
        h.succeed();
    }
    static Set<String> roots(AdminConfig c) {
        var d = new CommandDispatcher<CommandSourceStack>();
        AdminCommands.register(d, c);
        var out = new TreeSet<String>();
        for (var n : d.getRoot().getChildren()) { if (n.getName().equals("simpletweaks")) continue; out.add(n.getName()); if (n.getName().equals("tweaks")) n.getChildren().forEach(x -> out.add("tweaks " + x.getName())); }
        return out;
    }
    static void commands(GameTestHelper h) {
        var on = roots(AdminConfig.DEFAULT);
        yes(h, on.containsAll(List.of("inspect", "invsee", "endersee", "vanish", "freeze", "unfreeze", "backup", "maintenance", "tweaks rollback", "tweaks deaths", "tweaks restart", "lag")), "Default commands: " + on);
        var d = AdminConfig.DEFAULT;
        var off = new AdminConfig(new AdminConfig.BlockLog(false, 2, 3, 14, 1, 5), new AdminConfig.Deaths(false, 3, 5), new AdminConfig.Invsee(false, 2, 2),
                new AdminConfig.Vanish(false, 2, 2), new AdminConfig.Freeze(false, 2), new AdminConfig.Restart(false, 3, List.of(), List.of(), 0),
                new AdminConfig.Backups(false, 3, 6, 5, "backups"), new AdminConfig.Maintenance(false, 3, 2, List.of(), null, null, true), new AdminConfig.Lag(true, 2, 5, 8));
        yes(h, roots(off).equals(Set.of("lag")), "Switched-off features register no command: " + roots(off));
        h.succeed();
    }

    // ------------------------------------------------------------------ T10
    static void blockLog(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1));
        p.setGameMode(GameType.CREATIVE);
        var stonePos = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(stonePos, Blocks.STONE.defaultBlockState(), 3);
        yes(h, p.gameMode.destroyBlock(stonePos), "Player breaks stone");
        var base = h.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(base, Blocks.STONE.defaultBlockState(), 3);
        var stack = new ItemStack(Items.OAK_PLANKS);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.gameMode.useItemOn(p, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(base).add(0, .5, 0), Direction.UP, base, false));
        var placed = base.above();
        yes(h, level.getBlockState(placed).is(Blocks.OAK_PLANKS), "Player placed planks");
        var chestPos = h.absolutePos(new BlockPos(4, 2, 1));
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        p.openMenu(chest);
        chest.removeItem(0, 2);
        chest.setItem(1, new ItemStack(Items.EMERALD, 3));
        p.closeContainer();
        // A tool placement (SimpleBuilding blueprint build) through SimpleLib, without a block item.
        var toolPos = h.absolutePos(new BlockPos(5, 2, 3));
        level.setBlock(toolPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        com.simplelib.api.ToolPlacements.placed(level, p, toolPos, Blocks.OAK_PLANKS.defaultBlockState());
        var tool = get(BlockLog.at(level, toolPos));
        yes(h, tool.size() == 1 && tool.getFirst().action() == 'P' && tool.getFirst().id().equals("minecraft:oak_planks"), "Tool placement logged: " + tool);
        var broken = get(BlockLog.at(level, stonePos));
        var put = get(BlockLog.at(level, placed));
        var box = get(BlockLog.at(level, chestPos));
        yes(h, broken.size() == 1 && broken.getFirst().action() == 'B' && broken.getFirst().id().equals("minecraft:stone") && broken.getFirst().player().equals(p.getUUID()), "Break logged: " + broken);
        yes(h, put.size() == 1 && put.getFirst().action() == 'P' && put.getFirst().id().equals("minecraft:oak_planks"), "Place logged: " + put);
        yes(h, box.stream().anyMatch(e -> e.action() == 'T' && e.id().equals("minecraft:diamond") && e.count() == 2)
                && box.stream().anyMatch(e -> e.action() == 'D' && e.id().equals("minecraft:emerald") && e.count() == 3), "Container take/put logged: " + box);
        yes(h, Files.isDirectory(BlockLog.root().resolve(AdminFiles.folder(level.dimension().identifier().toString()))), "Per-dimension folder");
        h.succeed();
    }
    static void inspect(GameTestHelper h) {
        var level = h.getLevel();
        var admin = op(h, new BlockPos(1, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        var guest = player(h, new BlockPos(1, 2, 2));
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        yes(h, Inspect.toggle(guest) && !Inspect.inspecting(guest), "Without the operator level the inspect mode does nothing");
        admin.setGameMode(GameType.CREATIVE);
        yes(h, Inspect.toggle(admin) && Inspect.inspecting(admin), "Admin inspects");
        admin.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 0);
        yes(h, level.getBlockState(pos).is(Blocks.STONE), "An inspect click does not break");
        h.succeedWhen(() -> {
            h.assertTrue(Inspect.page(admin, 1), "The click stored a history page");
            Inspect.forget(admin.getUUID()); Inspect.forget(guest.getUUID()); deop(h, admin);
        });
    }
    static void rollback(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        p.setGameMode(GameType.CREATIVE);
        var a = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(a, Blocks.BOOKSHELF.defaultBlockState(), 3);
        p.gameMode.destroyBlock(a);
        var base = h.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(base, Blocks.STONE.defaultBlockState(), 3);
        var stack = new ItemStack(Items.DIRT);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.gameMode.useItemOn(p, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(base).add(0, .5, 0), Direction.UP, base, false));
        var chestPos = h.absolutePos(new BlockPos(4, 2, 1));
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 4));
        p.openMenu(chest);
        chest.setItem(0, ItemStack.EMPTY);
        p.closeContainer();
        yes(h, Inspect.parseDuration("1h30m") == 5_400_000L && Inspect.parseDuration("2d") == 172_800_000L && Inspect.parseDuration("5x") < 0 && Inspect.parseDuration("") < 0, "Durations parse");
        var list = get(BlockLog.byPlayer(server, p.getUUID(), System.currentTimeMillis() - 60_000, level, a, 16, 1000));
        yes(h, list.size() == 3, "Three actions found in the radius: " + list);
        var result = Rollback.apply(server, list);
        yes(h, result.undone() == 3, "All undone: " + result);
        yes(h, level.getBlockState(a).is(Blocks.BOOKSHELF) && level.getBlockState(base.above()).isAir(), "Blocks restored");
        yes(h, chest.getItem(0).is(Items.GOLD_INGOT) && chest.getItem(0).getCount() == 4, "Chest items restored");
        var none = get(BlockLog.byPlayer(server, p.getUUID(), System.currentTimeMillis() - 60_000, level, a.offset(5000, 0, 5000), 4, 1000));
        yes(h, none.isEmpty(), "The radius limits the search");
        h.succeed();
    }
    static void retention(GameTestHelper h) {
        var level = h.getLevel();
        String dim = level.dimension().identifier().toString();
        var e = new BlockLog.Entry(1234, 'B', UUID.randomUUID(), "A\tB", dim, 1, 2, 3, "minecraft:chest", 1, "minecraft:chest[facing=north]\u0001{Items:[{id:\"x\",note:\"a\tb\\nc\"}]}");
        var back = blockLogParse(dim, e);
        yes(h, back != null && back.extra().equals(e.extra()) && back.name().equals("A B") && back.x() == 1 && back.time() == 1234, "Entries survive the file format: " + back);
        try {
            Path region = BlockLog.root().resolve(AdminFiles.folder(dim)).resolve("9999_9999");
            Files.createDirectories(region);
            Path old = region.resolve("2000-01-01.tsv"), today = region.resolve(LocalDate.now() + ".tsv");
            Files.writeString(old, "x\n"); Files.writeString(today, "x\n");
            BlockLog.purgeNow();
            yes(h, !Files.exists(old) && Files.exists(today), "Retention deletes old day files only");
            Files.deleteIfExists(today); Files.deleteIfExists(region);
        } catch (java.io.IOException ex) { throw new AssertionError(ex); }
        h.succeed();
    }
    /** Signs (S), explosions (X), fire (F) and hoppers (I/O, only with the switch on) are logged. */
    static void worldEvents(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1));
        // sign: old and new text
        var signPos = h.absolutePos(new BlockPos(1, 2, 4));
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        var sign = (net.minecraft.world.level.block.entity.SignBlockEntity) level.getBlockEntity(signPos);
        sign.setAllowedPlayerEditor(p.getUUID());
        sign.updateSignText(p, net.minecraft.world.level.block.entity.SignTextSlot.FRONT, signLines("Hello", "World"));
        var signLog = get(BlockLog.at(level, signPos));
        yes(h, signLog.size() == 1 && signLog.getFirst().action() == 'S' && signLog.getFirst().player().equals(p.getUUID())
                && signLog.getFirst().extra().endsWith("\u0001Hello\nWorld\n\n"), "Sign edit logged with old and new text: " + signLog);
        // explosion without a source: #explosion
        var dirt = h.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
        BlockLog.resetBudget();
        level.explode(null, dirt.getX() + .5, dirt.getY() + .5, dirt.getZ() + .5, 1.5f, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        var boom = get(BlockLog.at(level, dirt));
        yes(h, level.getBlockState(dirt).isAir() && boom.stream().anyMatch(e -> e.action() == 'X' && e.id().equals("minecraft:dirt") && e.name().equals("#explosion")
                && e.player().equals(BlockLog.WORLD)), "Explosion logged: " + boom);
        // fire: the burn-out step itself (loader-specific signature, called by name)
        var wood = h.absolutePos(new BlockPos(1, 2, 6));
        level.setBlock(wood, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        burnOut(level, wood);
        var burned = get(BlockLog.at(level, wood));
        yes(h, !level.getBlockState(wood).is(Blocks.OAK_PLANKS) && burned.stream().anyMatch(e -> e.action() == 'F' && e.name().equals("#fire") && e.id().equals("minecraft:oak_planks")),
                "Burned block logged: " + burned);
        // hoppers: off by default, then on
        var chestPos = h.absolutePos(new BlockPos(3, 3, 1));
        var hopperPos = chestPos.below();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(hopperPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 4));
        net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
        BlockLog.flushNow();
        yes(h, get(BlockLog.at(level, chestPos)).isEmpty(), "Hoppers are not logged with the switch off");
        var previous = Admin.config();
        var b = previous.blockLog();
        Admin.configure(new AdminConfig(new AdminConfig.BlockLog(b.enabled(), b.permission(), b.rollbackPermission(), b.retentionDays(), b.rollbackMaxChanges(), b.flushSeconds(),
                b.signs(), b.explosions(), b.fire(), true, b.worldEventsPerTick(), b.lookupPermission(), b.restorePermission(), b.purgePermission(), b.lookupMaxResults()),
                previous.deathBackups(), previous.invsee(), previous.vanish(), previous.freeze(), previous.restart(), previous.backups(), previous.maintenance(), previous.lagFinder(), previous.autoOffWith()));
        try {
            BlockLog.resetBudget();
            net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
            net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
            net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(hopper, chest, new ItemStack(Items.EMERALD), Direction.UP);
            BlockLog.flushNow();
        } finally { Admin.configure(previous); }
        var moves = get(BlockLog.at(level, chestPos));
        yes(h, moves.stream().anyMatch(e -> e.action() == 'O' && e.id().equals("minecraft:diamond") && e.count() == 2 && e.name().equals("#hopper"))
                && moves.stream().anyMatch(e -> e.action() == 'I' && e.id().equals("minecraft:emerald") && e.count() == 1), "Hopper moves summed and logged: " + moves);
        yes(h, get(BlockLog.at(level, hopperPos)).isEmpty(), "The hopper itself is not logged");
        // a stuck writer must not grow the queue without limit: over the cap new entries are dropped
        BlockLog.flushNow();
        var capped = new BlockPos[5];
        for (int i = 0; i < capped.length; i++) capped[i] = h.absolutePos(new BlockPos(1 + i, 2, 8));
        int old = BlockLog.maxQueue;
        BlockLog.maxQueue = 2;
        try {
            for (var pos : capped) BlockLog.record(level, "#cap", 'X', pos, "minecraft:dirt", 1, null);
            BlockLog.flushNow();
        } finally { BlockLog.maxQueue = old; }
        int kept = 0;
        for (var pos : capped) if (!get(BlockLog.at(level, pos)).isEmpty()) kept++;
        yes(h, kept == 2, "Queue cap drops entries over the limit: kept " + kept);
        h.succeed();
    }
    private static List<net.minecraft.server.network.FilteredText> signLines(String... text) {
        var out = new ArrayList<net.minecraft.server.network.FilteredText>();
        for (int i = 0; i < 4; i++) out.add(net.minecraft.server.network.FilteredText.passThrough(i < text.length ? text[i] : ""));
        return out;
    }
    /** FireBlock.checkBurnOut with chance 1 (always burns); the parameters differ per loader, so they are filled by type. */
    private static void burnOut(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        try {
            var m = java.util.Arrays.stream(FireBlock.class.getDeclaredMethods()).filter(x -> x.getName().equals("checkBurnOut")).findFirst().orElseThrow();
            m.setAccessible(true);
            var types = m.getParameterTypes();
            var args = new Object[types.length];
            int ints = 0;
            for (int i = 0; i < types.length; i++) {
                if (types[i] == int.class) args[i] = ints++ == 0 ? 1 : 15;
                else if (types[i] == BlockPos.class) args[i] = pos;
                else if (types[i] == Direction.class) args[i] = Direction.UP;
                else if (net.minecraft.util.RandomSource.class.isAssignableFrom(types[i])) args[i] = level.getRandom();
                else args[i] = level;
            }
            m.invoke(Blocks.FIRE, args);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    /** Lookup filters: player (UUID and #actor), action, item, block, dimension, time, radius; parser errors; the old rollback form. */
    static void lookupFilters(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        var here = h.absolutePos(new BlockPos(2, 2, 2));
        String dim = level.dimension().identifier().toString();
        BlockLog.record(level, p, 'B', here, "minecraft:stone", 1, "minecraft:stone");
        BlockLog.record(level, p, 'T', here.east(), "minecraft:diamond", 3, "");
        BlockLog.record(level, "#fire", 'F', here.north(), "minecraft:oak_planks", 1, "minecraft:oak_planks");
        long now = System.currentTimeMillis();
        String uuid = p.getUUID().toString();
        java.util.function.Function<String, List<BlockLog.Entry>> find = text -> {
            var parsed = LogFilter.parse(server, text + " radius:4", here, dim, now);
            h.assertTrue(parsed.error() == null, "Parses: " + text + " -> " + parsed.error());
            return get(BlockLog.query(server, parsed.filter(), 1000));
        };
        yes(h, find.apply("player:" + uuid).size() == 2, "player: finds the two own actions");
        var broke = find.apply("player:" + uuid + " action:break");
        yes(h, broke.size() == 1 && broke.getFirst().action() == 'B', "action:break: " + broke);
        var items = find.apply("item:diamond");
        yes(h, items.size() == 1 && items.getFirst().action() == 'T' && items.getFirst().count() == 3, "item:diamond: " + items);
        var blocks = find.apply("block:minecraft:stone,oak_planks");
        yes(h, blocks.size() == 2 && blocks.stream().noneMatch(e -> e.action() == 'T'), "block: list: " + blocks);
        var fire = find.apply("player:#fire action:burn");
        yes(h, fire.size() == 1 && fire.getFirst().name().equals("#fire"), "#actor filter: " + fire);
        yes(h, find.apply("player:" + uuid + " dim:the_nether").isEmpty(), "dim: limits the dimension");
        yes(h, find.apply("player:" + uuid + " time:1h").size() == 2, "time: keeps recent entries");
        var far = LogFilter.parse(server, "player:" + uuid + " radius:4", here.offset(3000, 0, 3000), dim, now);
        yes(h, get(BlockLog.query(server, far.filter(), 1000)).isEmpty(), "radius: limits the area");
        yes(h, "simpletweaks.admin.blocklog.filter.action".equals(LogFilter.parse(server, "action:fly", here, dim, now).error()), "Unknown action rejected");
        yes(h, "simpletweaks.admin.blocklog.filter.radius".equals(LogFilter.parse(server, "radius:0", here, dim, now).error()), "Bad radius rejected");
        yes(h, "simpletweaks.admin.blocklog.badtime".equals(LogFilter.parse(server, "time:soon", here, dim, now).error()), "Bad time rejected");
        var legacy = LogFilter.parse(server, uuid + " 2h 16 --preview", here, dim, now);
        yes(h, legacy.error() == null && legacy.preview() && legacy.hasTime() && legacy.filter().players().contains(p.getUUID()) && legacy.filter().radius() == 16
                && legacy.filter().since() == now - 7_200_000L, "Old form <player> <time> [radius] still parses");
        var noNarrow = LogFilter.parse(server, "time:1h", here, dim, now);
        yes(h, noNarrow.error() == null && !noNarrow.filter().narrowed(), "time alone is not narrow enough for a rollback");
        h.succeed();
    }

    /** --preview changes nothing; a rollback is undone again by restore. */
    static void previewRestore(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        p.setGameMode(GameType.CREATIVE);
        var shelf = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(shelf, Blocks.BOOKSHELF.defaultBlockState(), 3);
        p.gameMode.destroyBlock(shelf);
        var base = h.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(base, Blocks.STONE.defaultBlockState(), 3);
        var stack = new ItemStack(Items.DIRT);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.gameMode.useItemOn(p, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(base).add(0, .5, 0), Direction.UP, base, false));
        var signPos = h.absolutePos(new BlockPos(1, 2, 4));
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        var sign = (net.minecraft.world.level.block.entity.SignBlockEntity) level.getBlockEntity(signPos);
        sign.setAllowedPlayerEditor(p.getUUID());
        sign.updateSignText(p, net.minecraft.world.level.block.entity.SignTextSlot.FRONT, signLines("Grief"));
        var list = get(BlockLog.byPlayer(server, p.getUUID(), System.currentTimeMillis() - 60_000, level, shelf, 16, 1000));
        yes(h, list.size() == 3, "Three actions: " + list);
        int before = Rollback.restorable();
        var preview = Rollback.apply(server, list, true);
        yes(h, preview.undone() == 3, "Preview counts all three: " + preview);
        yes(h, level.getBlockState(shelf).isAir() && level.getBlockState(base.above()).is(Blocks.DIRT) && BlockRecorder.signText(sign, true).startsWith("Grief")
                && Rollback.restorable() == before, "Preview changed nothing");
        var done = Rollback.apply(server, list, false);
        yes(h, done.undone() == 3, "Rollback undid all three: " + done);
        yes(h, level.getBlockState(shelf).is(Blocks.BOOKSHELF), "Bookshelf back: " + level.getBlockState(shelf));
        yes(h, level.getBlockState(base.above()).isAir(), "Dirt gone: " + level.getBlockState(base.above()));
        yes(h, BlockRecorder.signText(sign, true).equals("\n\n\n"), "Sign text back: " + BlockRecorder.signText(sign, true).replace('\n', '|'));
        yes(h, Rollback.restorable() == 3, "The run can be restored");
        var redo = Rollback.restore(server);
        yes(h, redo.undone() == 3 && level.getBlockState(shelf).isAir() && level.getBlockState(base.above()).is(Blocks.DIRT) && BlockRecorder.signText(sign, true).startsWith("Grief"),
                "Restore brought the changes back: " + redo);
        yes(h, Rollback.restorable() == 0, "Restore empties the buffer");
        h.succeed();
    }

    /** purge removes every line of one player and keeps the others. */
    static void purgePlayer(GameTestHelper h) {
        var level = h.getLevel();
        var a = player(h, new BlockPos(1, 2, 1));
        var b = player(h, new BlockPos(1, 2, 2));
        BlockPos posA = h.absolutePos(new BlockPos(2, 2, 2)), posB = h.absolutePos(new BlockPos(3, 2, 2));
        BlockLog.record(level, a, 'B', posA, "minecraft:stone", 1, "minecraft:stone");
        BlockLog.record(level, a, 'P', posB, "minecraft:dirt", 1, "minecraft:dirt");
        BlockLog.record(level, b, 'B', posB, "minecraft:stone", 1, "minecraft:stone");
        int removed = get(BlockLog.purgePlayer(a.getUUID()));
        yes(h, removed == 2, "Both lines of the player removed: " + removed);
        var leftB = get(BlockLog.at(level, posB));
        yes(h, get(BlockLog.at(level, posA)).isEmpty() && leftB.size() == 1 && leftB.getFirst().player().equals(b.getUUID()), "Other players stay: " + leftB);
        h.succeed();
    }

    /** Each block log command has its own operator level from the config; defaults lookup 2, rollback 3, restore 3, purge 4. */
    static void blockLogPermissions(GameTestHelper h) {
        var b = AdminConfig.DEFAULT.blockLog();
        yes(h, b.retentionDays() == 14 && b.signs() && b.explosions() && b.fire() && !b.hoppers() && b.permission() == 2 && b.lookupPermission() == 2
                && b.rollbackPermission() == 3 && b.restorePermission() == 3 && b.purgePermission() == 4, "Block log defaults: " + b);
        var clamped = new AdminConfig.BlockLog(true, 2, 3, 14, 1, 5, true, true, true, true, -5, 9, -1, 7, 1);
        yes(h, clamped.worldEventsPerTick() == 0 && clamped.lookupPermission() == 4 && clamped.restorePermission() == 0 && clamped.purgePermission() == 4
                && clamped.lookupMaxResults() == 10, "New values are clamped");
        yes(h, !b.withEnabled(false).enabled() && b.withEnabled(false).purgePermission() == 4, "Auto-off keeps the other settings");
        var d = new CommandDispatcher<CommandSourceStack>();
        AdminCommands.register(d, AdminConfig.DEFAULT);
        var tweaks = d.getRoot().getChild("tweaks");
        var root = d.getRoot().getChild("simpletweaks");
        for (var name : List.of("lookup", "rollback", "restore", "purge")) yes(h, tweaks.getChild(name) != null && root.getChild(name) == tweaks.getChild(name), "/tweaks and /simpletweaks " + name);
        var gm = op(h, new BlockPos(1, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        var admin = op(h, new BlockPos(1, 2, 2), LevelBasedPermissionSet.ADMIN);
        var owner = op(h, new BlockPos(1, 2, 3), LevelBasedPermissionSet.OWNER);
        var g = gm.createCommandSourceStack(); var a = admin.createCommandSourceStack(); var o = owner.createCommandSourceStack();
        java.util.function.BiPredicate<String, CommandSourceStack> can = (n, s) -> tweaks.getChild(n).canUse(s);
        yes(h, can.test("lookup", g) && !can.test("rollback", g) && !can.test("restore", g) && !can.test("purge", g), "Level 2: lookup only");
        yes(h, can.test("rollback", a) && can.test("restore", a) && !can.test("purge", a), "Level 3: rollback and restore");
        yes(h, can.test("purge", o), "Level 4: purge");
        var previous = Admin.config();
        var c = previous.blockLog();
        Admin.configure(new AdminConfig(new AdminConfig.BlockLog(c.enabled(), c.permission(), c.rollbackPermission(), c.retentionDays(), c.rollbackMaxChanges(), c.flushSeconds(),
                c.signs(), c.explosions(), c.fire(), c.hoppers(), c.worldEventsPerTick(), 4, c.restorePermission(), 3, c.lookupMaxResults()),
                previous.deathBackups(), previous.invsee(), previous.vanish(), previous.freeze(), previous.restart(), previous.backups(), previous.maintenance(), previous.lagFinder(), previous.autoOffWith()));
        boolean lookupBlocked, purgeAllowed;
        try { lookupBlocked = !can.test("lookup", g); purgeAllowed = can.test("purge", a); } finally { Admin.configure(previous); }
        yes(h, lookupBlocked && purgeAllowed, "The levels follow the config");
        deop(h, gm, admin, owner);
        h.succeed();
    }

    /** Day files of the first T10 version (only B/P/T/D) and lines of newer versions (unknown actions) stay readable. */
    static void legacyLines(GameTestHelper h) {
        var level = h.getLevel();
        String dim = level.dimension().identifier().toString();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var id = UUID.randomUUID();
        long old = System.currentTimeMillis() - 7_200_000L;
        try {
            Path file = BlockLog.root().resolve(AdminFiles.folder(dim)).resolve((pos.getX() >> 9) + "_" + (pos.getZ() >> 9)).resolve(LocalDate.now() + ".tsv");
            Files.createDirectories(file.getParent());
            String legacy = old + "\tB\t" + id + "\tOldTimer\t" + pos.getX() + "\t" + pos.getY() + "\t" + pos.getZ() + "\tminecraft:chest\t1\tminecraft:chest[facing=north]\n"
                    + "garbage line\n"
                    + old + "\tZ\t" + id + "\tOldTimer\t" + pos.getX() + "\t" + pos.getY() + "\t" + pos.getZ() + "\tminecraft:stone\t1\t\tfuture-column\n";
            Files.writeString(file, legacy, java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        var at = get(BlockLog.at(level, pos));
        yes(h, at.stream().anyMatch(e -> e.action() == 'B' && e.player().equals(id) && e.name().equals("OldTimer") && e.id().equals("minecraft:chest") && e.time() == old),
                "Old line read: " + at);
        yes(h, at.stream().anyMatch(e -> e.action() == 'Z'), "Unknown actions and extra columns of newer files do not break reading");
        yes(h, Inspect.action(at.stream().filter(e -> e.action() == 'Z').findFirst().orElseThrow()).getString().contains("Z"), "Unknown actions show their letter");
        var server = level.getServer();
        var recent = LogFilter.parse(server, "player:" + id + " time:1h", pos, level.dimension().identifier().toString(), System.currentTimeMillis());
        var wide = LogFilter.parse(server, "player:" + id + " time:3h action:break", pos, level.dimension().identifier().toString(), System.currentTimeMillis());
        yes(h, get(BlockLog.query(server, recent.filter(), 100)).isEmpty() && get(BlockLog.query(server, wide.filter(), 100)).size() == 1, "time: filters old lines");
        h.succeed();
    }

    private static BlockLog.Entry blockLogParse(String dim, BlockLog.Entry e) {
        try {
            var line = BlockLog.Entry.class.getDeclaredMethod("line"); line.setAccessible(true);
            var parse = BlockLog.Entry.class.getDeclaredMethod("parse", String.class, String.class); parse.setAccessible(true);
            return (BlockLog.Entry) parse.invoke(null, dim, line.invoke(e));
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }

    // ------------------------------------------------------------------ T11
    static void deaths(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        p.getInventory().setItem(39, new ItemStack(Items.IRON_HELMET));
        p.getInventory().setItem(40, new ItemStack(Items.SHIELD));
        p.setExperienceLevels(7);
        p.die(h.getLevel().damageSources().generic());
        var list = DeathBackups.list(server, p.getUUID());
        yes(h, list.size() == 1, "Death saved a backup");
        var b = list.getFirst();
        yes(h, b.slots().get(0).is(Items.DIAMOND_SWORD) && b.slots().get(39).is(Items.IRON_HELMET) && b.slots().get(40).is(Items.SHIELD) && b.xpLevel() == 7 && !b.cause().isEmpty(),
                "Inventory, armor, offhand, XP and cause kept: " + b.cause());
        for (int i = 0; i < 7; i++) DeathBackups.add(server, p.getUUID(), DeathBackups.snapshot(p, "test " + i));
        yes(h, DeathBackups.list(server, p.getUUID()).size() == Admin.config().deathBackups().keep(), "Only the last N deaths are kept");
        // restore the newest real death again: put it in front
        DeathBackups.add(server, p.getUUID(), b);
        p.getInventory().clearContent();
        p.getInventory().setItem(5, new ItemStack(Items.STICK));
        yes(h, DeathBackups.restore(server, p.getUUID(), 0), "Restore online");
        yes(h, p.getInventory().getItem(0).is(Items.DIAMOND_SWORD) && p.getInventory().getItem(39).is(Items.IRON_HELMET) && p.experienceLevel == 7, "Inventory and XP restored");
        var after = DeathBackups.list(server, p.getUUID());
        yes(h, after.getFirst().slots().get(5).is(Items.STICK), "The replaced inventory became a backup first");
        h.succeed();
    }
    static void deathsOffline(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var id = UUID.randomUUID();
        var data = OfflineData.empty(server, id);
        var slots = new ArrayList<ItemStack>(Collections.nCopies(OfflineData.INVENTORY, ItemStack.EMPTY));
        slots.set(3, new ItemStack(Items.APPLE, 2));
        data.setInventory(slots);
        yes(h, data.save(), "Offline data written");
        var backup = new ArrayList<ItemStack>(Collections.nCopies(OfflineData.INVENTORY, ItemStack.EMPTY));
        backup.set(0, new ItemStack(Items.NETHERITE_PICKAXE));
        backup.set(38, new ItemStack(Items.ELYTRA));
        DeathBackups.add(server, id, new DeathBackups.Backup(1, "fell", "minecraft:overworld", 0, 0, 0, 30, .5f, 1000, backup));
        yes(h, DeathBackups.restore(server, id, 0), "Restore offline");
        var reloaded = OfflineData.load(server, id);
        var inv = reloaded.inventory();
        yes(h, inv.get(0).is(Items.NETHERITE_PICKAXE) && inv.get(38).is(Items.ELYTRA) && inv.get(3).isEmpty() && reloaded.xp()[0] == 30, "Offline inventory and XP restored");
        yes(h, DeathBackups.list(server, id).getFirst().slots().get(3).is(Items.APPLE), "Old offline inventory kept as backup");
        try { Files.deleteIfExists(OfflineData.file(server, id)); Files.deleteIfExists(OfflineData.file(server, id).resolveSibling(id + ".dat_old")); Files.deleteIfExists(DeathBackups.file(server, id)); }
        catch (java.io.IOException ignored) {}
        h.succeed();
    }

    // ------------------------------------------------------------------ T13
    static void invseeLive(GameTestHelper h) {
        var viewer = op(h, new BlockPos(1, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        var target = player(h, new BlockPos(2, 2, 1));
        target.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        yes(h, Invsee.openInventory(viewer, target.nameAndId(), false), "Opens");
        yes(h, viewer.containerMenu instanceof InvseeMenu m && m.kind == InvseeMenu.Kind.INVENTORY && m.slots.size() == 41 + 36, "Own menu with 41 + 36 slots");
        var menu = (InvseeMenu) viewer.containerMenu;
        int hotbar0 = 5 + 27;
        yes(h, menu.getSlot(hotbar0).getItem().is(Items.DIAMOND), "The menu shows the target's hotbar");
        menu.clicked(hotbar0, 0, ContainerInput.QUICK_MOVE, viewer);
        yes(h, target.getInventory().getItem(0).isEmpty() && viewer.getInventory().countItem(Items.DIAMOND) == 3, "Taking works live");
        yes(h, !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)) && menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND_HELMET)), "Head slot only takes headgear");
        target.getInventory().setItem(9, new ItemStack(Items.EMERALD));
        menu.broadcastChanges();
        yes(h, menu.getSlot(5).getItem().is(Items.EMERALD), "Target changes show up");
        yes(h, menu.stillValid(viewer), "Valid while both are online");
        deop(h, viewer);
        yes(h, !menu.stillValid(viewer), "Losing the operator level closes the view");
        viewer.closeContainer();
        var readOnly = new InvseeMenu(Invsee.INVSEE, 99, viewer.getInventory(), InvseeMenu.Kind.INVENTORY, target.getInventory(), null, false) {};
        yes(h, !readOnly.getSlot(5).mayPickup(viewer) && !readOnly.getSlot(5).mayPlace(new ItemStack(Items.DIRT)), "Without the edit right the view is read-only");
        var second = op(h, new BlockPos(1, 2, 2), LevelBasedPermissionSet.GAMEMASTER);
        target.getEnderChestInventory().setItem(2, new ItemStack(Items.ENDER_PEARL));
        yes(h, Invsee.openInventory(second, target.nameAndId(), true) && second.containerMenu.getSlot(2).getItem().is(Items.ENDER_PEARL), "Endersee shows the ender chest");
        second.closeContainer();
        deop(h, second);
        h.succeed();
    }
    static void invseeOffline(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var viewer = op(h, new BlockPos(1, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        var id = UUID.randomUUID();
        var data = OfflineData.empty(server, id);
        var slots = new ArrayList<ItemStack>(Collections.nCopies(OfflineData.INVENTORY, ItemStack.EMPTY));
        slots.set(0, new ItemStack(Items.GOLDEN_APPLE, 2));
        slots.set(36, new ItemStack(Items.DIAMOND_BOOTS));
        data.setInventory(slots);
        var ender = new ArrayList<ItemStack>(Collections.nCopies(OfflineData.ENDER, ItemStack.EMPTY));
        ender.set(4, new ItemStack(Items.SHULKER_SHELL));
        data.setEnder(ender);
        data.save();
        var who = new NameAndId(id, "Offline");
        yes(h, Invsee.openInventory(viewer, who, false), "Offline inventory opens");
        var menu = (InvseeMenu) viewer.containerMenu;
        yes(h, menu.getSlot(3).getItem().is(Items.DIAMOND_BOOTS) && menu.getSlot(32).getItem().is(Items.GOLDEN_APPLE), "Offline slots are shown");
        menu.clicked(32, 0, ContainerInput.QUICK_MOVE, viewer);
        menu.broadcastChanges();
        yes(h, OfflineData.load(server, id).inventory().get(0).isEmpty() && viewer.getInventory().countItem(Items.GOLDEN_APPLE) == 2, "Taking is written back at once");
        Invsee.loading(server, id);
        yes(h, !(viewer.containerMenu instanceof InvseeMenu), "A login of the target closes the offline view");
        yes(h, Invsee.openInventory(viewer, who, true) && viewer.containerMenu.getSlot(4).getItem().is(Items.SHULKER_SHELL), "Offline ender chest opens");
        viewer.closeContainer();
        yes(h, !Invsee.openInventory(viewer, new NameAndId(UUID.randomUUID(), "Never"), false), "Unknown players have no data");
        deop(h, viewer);
        try { Files.deleteIfExists(OfflineData.file(server, id)); Files.deleteIfExists(OfflineData.file(server, id).resolveSibling(id + ".dat_old")); } catch (java.io.IOException ignored) {}
        h.succeed();
    }

    // ------------------------------------------------------------------ T14
    static void vanish(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var admin = op(h, new BlockPos(1, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        var guest = player(h, new BlockPos(2, 2, 1));
        var ghost = op(h, new BlockPos(3, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        yes(h, ghost.broadcastToPlayer(guest) && ghost.canBeSeenByAnyone(), "Visible before");
        Vanish.set(ghost, true);
        try {
            yes(h, !ghost.broadcastToPlayer(guest) && ghost.broadcastToPlayer(admin), "Hidden from players, seen by admins");
            yes(h, !ghost.canBeSeenByAnyone(), "Mobs ignore vanished players");
            yes(h, !Vanish.pickup(ghost), "No pickup by default");
            Admin.state().vanishPickup.add(ghost.getUUID());
            yes(h, Vanish.pickup(ghost), "Pickup can be switched on");
            Admin.state().vanishPickup.remove(ghost.getUUID());
            var players = new net.minecraft.network.protocol.status.ServerStatus.Players(20, server.getPlayerList().getPlayerCount(),
                    List.of(ghost.nameAndId(), guest.nameAndId()));
            var shown = Vanish.status(server, players);
            yes(h, shown.online() == players.online() - 1 && shown.sample().equals(List.of(guest.nameAndId())), "Server list leaves vanished players out");
            yes(h, Admin.state().vanished.contains(ghost.getUUID()), "State kept for relogs");
        } finally {
            Vanish.set(ghost, false);
            deop(h, admin, ghost);
        }
        yes(h, ghost.broadcastToPlayer(guest), "Visible again");
        h.succeed();
    }
    static void vibrations(GameTestHelper h) {
        var level = h.getLevel();
        var sensor = new BlockPos(1, 2, 1);
        var ghost = player(h, new BlockPos(4, 2, 1));
        h.setBlock(sensor, Blocks.SCULK_SENSOR);
        var source = Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 2, 1)));
        Vanish.set(ghost, true);
        level.gameEvent(GameEvent.PROJECTILE_LAND, source, GameEvent.Context.of(ghost));
        h.runAfterDelay(30, () -> {
            boolean quiet = SculkSensorBlock.getPhase(h.getBlockState(sensor)) == net.minecraft.world.level.block.state.properties.SculkSensorPhase.INACTIVE;
            Vanish.set(ghost, false);
            h.assertTrue(quiet, "A vanished player causes no vibration");
            level.gameEvent(GameEvent.PROJECTILE_LAND, source, GameEvent.Context.of(ghost));
            h.succeedWhen(() -> h.assertTrue(SculkSensorBlock.getPhase(h.getBlockState(sensor)) != net.minecraft.world.level.block.state.properties.SculkSensorPhase.INACTIVE, "A visible player does"));
        });
    }

    // ------------------------------------------------------------------ T15
    static void freeze(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1));
        var start = p.position();
        var stone = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        Freeze.set(p, true);
        yes(h, Freeze.frozen(p), "Frozen");
        yes(h, !p.gameMode.destroyBlock(stone) && level.getBlockState(stone).is(Blocks.STONE), "Frozen players cannot break");
        p.setPos(start.x + 3, start.y, start.z);
        h.runAfterDelay(3, () -> {
            boolean back = p.position().distanceTo(start) < 0.1;
            Freeze.set(p, false);
            h.assertTrue(back, "Moved back to the freeze position: " + p.position() + " vs " + start);
            h.assertTrue(!Freeze.frozen(p) && !Admin.state().frozen.contains(p.getUUID()), "Unfrozen");
            p.setGameMode(GameType.CREATIVE);
            h.assertTrue(p.gameMode.destroyBlock(stone), "Free again");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ T16
    static void restart(GameTestHelper h) {
        var zone = ZoneId.of("UTC");
        var three = ZonedDateTime.of(2026, 10, 17, 3, 0, 0, 0, zone);
        yes(h, Restart.nextScheduled(List.of("04:00"), three) == three.plusHours(1).toInstant().toEpochMilli(), "Next time today");
        yes(h, Restart.nextScheduled(List.of("04:00", "02:30"), three.plusHours(2)) == three.plusDays(1).withHour(2).withMinute(30).toInstant().toEpochMilli(), "Earliest next time tomorrow");
        yes(h, Restart.nextScheduled(List.of(), three) < 0, "No times, no schedule");
        var marks = new ArrayList<Integer>();
        for (int s = 1000; s > 0; s--) { var m = restartMark(s, List.of(15, 10, 5, 1)); if (m != null) marks.add(m); }
        yes(h, marks.equals(List.of(900, 600, 300, 60, 30, 10, 5, 4, 3, 2, 1)), "Warnings: " + marks);
        boolean[] stopped = {false};
        var previous = restartStop(server -> stopped[0] = true);
        var base = Instant.now();
        Restart.useClock(Clock.fixed(base, ZoneId.systemDefault()));
        Restart.schedule(1500);
        h.runAfterDelay(12, () -> {
            h.assertTrue(Restart.barShown() && !stopped[0], "Boss bar shows the countdown");
            Restart.useClock(Clock.fixed(base.plusSeconds(2), ZoneId.systemDefault()));
        });
        h.succeedWhen(() -> {
            h.assertTrue(stopped[0], "The stop runs at zero");
            restartStop(previous);
            Restart.useClock(Clock.systemDefaultZone());
            h.assertTrue(!Restart.barShown() && Restart.deadline() < 0, "Bar hidden after the stop");
            Restart.schedule(60_000);
            h.assertTrue(Restart.cancel(h.getLevel().getServer()) && Restart.deadline() < 0, "Cancel");
        });
    }
    @SuppressWarnings("unchecked")
    private static java.util.function.Consumer<net.minecraft.server.MinecraftServer> restartStop(java.util.function.Consumer<net.minecraft.server.MinecraftServer> value) {
        try {
            var f = Restart.class.getDeclaredField("stopAction"); f.setAccessible(true);
            var old = (java.util.function.Consumer<net.minecraft.server.MinecraftServer>) f.get(null); f.set(null, value); return old;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Integer restartMark(int s, List<Integer> warn) {
        try { var m = Restart.class.getDeclaredMethod("mark", int.class, List.class); m.setAccessible(true); return (Integer) m.invoke(null, s, warn); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    // ------------------------------------------------------------------ T17
    static void backup(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var previous = Admin.config();
        var d = previous;
        String dir = "gametest-backups-" + UUID.randomUUID().toString().substring(0, 8);
        Admin.configure(new AdminConfig(d.blockLog(), d.deathBackups(), d.invsee(), d.vanish(), d.freeze(), d.restart(),
                new AdminConfig.Backups(true, 3, 6, 2, dir), d.maintenance(), d.lagFinder()));
        Path target = Backups.directory(server);
        Path[] done = {null};
        boolean started = Backups.begin(server, p -> done[0] = p);
        h.succeedWhen(() -> {
            h.assertTrue(started, "Backup started");
            h.assertTrue(done[0] != null && !Backups.running(), "Backup finished");
            boolean levelDat;
            try (var zip = new java.util.zip.ZipFile(done[0].toFile())) { levelDat = zip.stream().anyMatch(e -> e.getName().endsWith("/level.dat")); }
            catch (java.io.IOException e) { throw new AssertionError(e); }
            h.assertTrue(levelDat, "The archive holds level.dat");
            h.assertTrue(server.getAllLevels().iterator().next().noSave == false, "Saving resumed");
            try {
                String name = done[0].getFileName().toString();
                String prefix = name.substring(0, name.length() - "yyyyMMdd-HHmmss.zip".length());
                for (int i = 0; i < 3; i++) Files.writeString(target.resolve(prefix + "19990101-00000" + i + ".zip"), "x");
                var prune = Backups.class.getDeclaredMethod("prune", Path.class, String.class, int.class); prune.setAccessible(true);
                prune.invoke(null, target, prefix, 2);
                try (var list = Files.list(target)) { h.assertTrue(list.filter(p -> p.toString().endsWith(".zip")).count() == 2, "Only the newest archives stay"); }
                h.assertTrue(Files.exists(done[0]), "The newest archive stays");
                try (var walk = Files.walk(target)) { walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
            } catch (Exception e) { throw new AssertionError(e); }
            finally { Admin.configure(previous); }
        });
    }

    // ------------------------------------------------------------------ T18
    static void maintenance(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var admin = op(h, new BlockPos(1, 2, 1), LevelBasedPermissionSet.GAMEMASTER);
        var guest = new NameAndId(UUID.randomUUID(), "Guest");
        var previous = Admin.config();
        var d = previous;
        Admin.configure(new AdminConfig(d.blockLog(), d.deathBackups(), d.invsee(), d.vanish(), d.freeze(), d.restart(), d.backups(),
                new AdminConfig.Maintenance(true, 3, 2, List.of("Helper"), "Closed for work", "Back soon", false), d.lagFinder()));
        try {
            yes(h, Maintenance.refuse(server, guest) == null && Maintenance.motd("Vanilla").equals("Vanilla"), "Off: everybody joins, Vanilla MOTD");
            Admin.state().maintenance = true;
            var refused = Maintenance.refuse(server, guest);
            yes(h, refused != null && refused.getString().equals("Closed for work"), "On: guests are refused with the message");
            yes(h, Maintenance.refuse(server, admin.nameAndId()) == null, "Operators join");
            yes(h, Maintenance.refuse(server, new NameAndId(UUID.randomUUID(), "helper")) == null, "Allowed names join");
            yes(h, Maintenance.motd("Vanilla").equals("Back soon"), "Maintenance MOTD");
        } finally {
            Admin.state().maintenance = false;
            Admin.configure(previous);
            deop(h, admin);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ T34
    static void lag(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(3, 2, 3), Blocks.FURNACE);
        yes(h, LagFinder.begin(server.createCommandSourceStack().withSuppressedOutput(), 1), "Sampling starts");
        yes(h, !LagFinder.begin(server.createCommandSourceStack().withSuppressedOutput(), 1), "Only one sample at a time");
        var wire = Blocks.REDSTONE_WIRE.defaultBlockState().setValue(BlockStateProperties.POWER, 5);
        var at = h.absolutePos(new BlockPos(4, 2, 4));
        for (int i = 0; i < 40; i++) LagFinder.changed(level, at, wire);
        h.succeedWhen(() -> {
            h.assertTrue(!LagFinder.sampling, "The sample ends");
            h.assertTrue(LagFinder.entityTypes().contains("minecraft:pig"), "Entity types are timed: " + LagFinder.entityTypes());
            h.assertTrue(LagFinder.blockEntityTypes().stream().anyMatch(t -> t.contains("furnace")), "Block entity types are timed: " + LagFinder.blockEntityTypes());
            h.assertTrue(LagFinder.clocks(1).contains(level.dimension().identifier() + "|" + (at.getX() >> 4) + "|" + (at.getZ() >> 4)), "Redstone clock hint: " + LagFinder.clocks(1));
            h.assertTrue(LagFinder.report(server, 1).size() >= 4, "Report lines");
        });
    }
    private AdminTests() {}
}
