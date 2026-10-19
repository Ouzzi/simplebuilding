package com.simplebuilding.modules.simpletweaks.play;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.afk.Afk;
import com.simplebuilding.modules.simpletweaks.play.border.Borders;
import com.simplebuilding.modules.simpletweaks.play.chat.Mentions;
import com.simplebuilding.modules.simpletweaks.play.cleanup.GroundCleanup;
import com.simplebuilding.modules.simpletweaks.play.coords.Coords;
import com.simplebuilding.modules.simpletweaks.play.crowd.EntityLimit;
import com.simplebuilding.modules.simpletweaks.play.death.DeathHint;
import com.simplebuilding.modules.simpletweaks.play.dragon.DragonEgg;
import com.simplebuilding.modules.simpletweaks.play.events.ServerEvents;
import com.simplebuilding.modules.simpletweaks.play.grief.MobGriefing;
import com.simplebuilding.modules.simpletweaks.play.keep.KeepInventory;
import com.simplebuilding.modules.simpletweaks.play.phantoms.Phantoms;
import com.simplebuilding.modules.simpletweaks.play.pregen.Pregen;
import com.simplebuilding.modules.simpletweaks.play.pvp.Pvp;
import com.simplebuilding.modules.simpletweaks.play.reward.LoginReward;
import com.simplebuilding.modules.simpletweaks.play.roof.NetherRoof;
import com.simplebuilding.modules.simpletweaks.play.top.Leaderboard;
import com.simplebuilding.modules.simpletweaks.play.vote.Votes;
import com.simplebuilding.modules.simpletweaks.play.welcome.Welcome;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.stats.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;

/**
 * GameTests of the player/world/event features (plan docs/ai/PLAN-TWEAKS-PLAY-2026-10-17.md), registered per loader as
 * "play_game_test_…". Tests that change the settings do so within one tick and restore them before returning (tests of
 * one batch share the server).
 */
public final class PlayTests {
    public static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("config_defaults_and_bounds", PlayTests::config);
        TESTS.put("commands_follow_switches", PlayTests::commands);
        TESTS.put("coords_line_and_ping", PlayTests::coords);
        TESTS.put("death_hint_after_death", PlayTests::deathHint);
        TESTS.put("mentions_highlight_and_sound_targets", PlayTests::mentions);
        TESTS.put("playtime_and_seen", PlayTests::playtime);
        TESTS.put("mob_griefing_per_mob", PlayTests::griefing);
        TESTS.put("pvp_both_must_agree", PlayTests::pvp);
        TESTS.put("welcome_lines_and_tips", PlayTests::welcome);
        TESTS.put("login_reward_streak", PlayTests::reward);
        TESTS.put("events_xp_loot_schedule", PlayTests::events);
        TESTS.put("leaderboard_sorts_and_sidebar", PlayTests::leaderboard);
        TESTS.put("vote_majority_and_options", PlayTests::vote);
        TESTS.put("afk_tab_name_and_sleep", PlayTests::afk);
        TESTS.put("border_per_dimension", PlayTests::border);
        TESTS.put("nether_roof_moves_down", PlayTests::roof);
        TESTS.put("keep_inventory_modes", PlayTests::keep);
        TESTS.put("dragon_egg_every_fight", PlayTests::dragon);
        TESTS.put("phantoms_opt_out", PlayTests::phantoms);
        TESTS.put("entity_limit_reports_and_blocks", PlayTests::crowd);
        TESTS.put("cleanup_spares_named_and_rare", PlayTests::cleanup);
        TESTS.put("pregen_spiral_and_generates", PlayTests::pregen);
        TESTS.put("vanished_admins_stay_hidden", PlayTests::vanished);
    }

    public static int maxTicks(String name) { return name.equals("pregen_spiral_and_generates") ? 12000 : 100; /* test servers tick unthrottled, chunk generation takes real seconds */ }

    static void yes(GameTestHelper h, boolean ok, String why) { h.assertTrue(ok, why); }
    static ServerPlayer player(GameTestHelper h, BlockPos rel) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        var pos = h.absolutePos(rel);
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return p;
    }
    /** Runs {@code body} with other settings and restores the previous ones (same tick). */
    static void with(PlayConfig c, Runnable body) {
        var old = Play.config();
        Play.configure(c);
        try { body.run(); } finally { Play.configure(old); }
    }
    static boolean hasClick(Component c, Class<?> type) {
        if (c.getStyle().getClickEvent() != null && type.isInstance(c.getStyle().getClickEvent())) return true;
        for (var s : c.getSiblings()) if (hasClick(s, type)) return true;
        if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) for (var a : t.getArgs()) if (a instanceof Component ac && hasClick(ac, type)) return true;
        return false;
    }
    static final String ALL_OFF;
    static {
        var b = new StringBuilder("{");
        for (var k : List.of("coords", "deathHint", "mentions", "playtime", "mobGriefing", "pvp", "welcome", "loginReward", "events", "leaderboard",
                "vote", "afk", "border", "netherRoof", "keepInventory", "dragonEgg", "phantoms", "entityLimit", "cleanup", "pregen"))
            b.append(b.length() > 1 ? "," : "").append('"').append(k).append("\":{\"enabled\":false}");
        ALL_OFF = b.append('}').toString();
    }

    // ------------------------------------------------------------------ config
    static void config(GameTestHelper h) {
        var d = PlayConfig.DEFAULT;
        yes(h, d.coords().enabled() && d.deathHint().enabled() && d.mentions().enabled() && d.playtime().enabled() && d.pvp().enabled()
                && d.vote().enabled() && d.afk().enabled() && d.border().enabled() && d.keepInventory().enabled() && d.dragonEgg().enabled(),
                "Owner defaults on: T4 T5 T6 T7 T9 T23 T24 T25 T27 T28");
        yes(h, d.events().enabled() && d.leaderboard().enabled() && d.phantoms().enabled() && d.pregen().enabled(),
                "Owner rule 17.10.: command-only features on (T21 command, T22 command, T30, T33)");
        yes(h, !d.mobGriefing().enabled() && !d.welcome().enabled() && !d.loginReward().enabled() && !d.netherRoof().enabled()
                && !d.entityLimit().enabled() && !d.cleanup().enabled(), "Automatic features off: T8 T19 T20 T26 T31 T32");
        yes(h, !d.events().scheduleEnabled() && !d.leaderboard().sidebar() && !d.vote().dayNight() && !d.afk().kick(),
                "Automatic parts off: event schedule, sidebar, day/night vote, AFK kick");
        var coords = new PlayConfig.Coords(true, true, 999, -5);
        yes(h, coords.pingSeconds() == 60 && coords.cooldownSeconds() == 0, "Coords values are clamped");
        var vote = new PlayConfig.Vote(true, 1, 500, -1, true, true, false);
        yes(h, vote.seconds() == 10 && vote.percent() == 100 && vote.cooldownSeconds() == 0, "Vote values are clamped");
        var ev = new PlayConfig.Events(true, 9, true, List.of("SAT 20:00 double_xp 60", "XYZ 25:00 foo 0", "daily 7:30 blood_moon 30"), 0, 99f, 0, 1, 99);
        yes(h, ev.schedule().equals(List.of("SAT 20:00 double_xp 60", "daily 7:30 blood_moon 30")) && ev.permission() == 4 && ev.xpMultiplier() == 10f
                && ev.extraLootRolls() == 1 && ev.bloodMoonWaveSeconds() == 5 && ev.bloodMoonMobs() == 8, "Event schedule and values validated: " + ev.schedule());
        var keep = new PlayConfig.KeepInventory(true, "weird", true, true, true, false, 300);
        yes(h, keep.mode().equals("hotbar_armor") && keep.keepXpPercent() == 100, "Unknown keep mode falls back, percent clamped");
        try {
            Path file = Files.createTempDirectory("st-play-config").resolve("p.json");
            yes(h, PlayConfig.load(file).equals(PlayConfig.DEFAULT) && Files.isRegularFile(file), "A missing file is written with the defaults");
            Files.writeString(file, "{\"cleanup\":{\"enabled\":true},\"afk\":{\"minutes\":9},\"border\":{\"dimensions\":{\"minecraft:the_nether\":{\"size\":500}}},\"unknown\":1}");
            var loaded = PlayConfig.load(file);
            yes(h, loaded.cleanup().enabled() && loaded.cleanup().intervalMinutes() == 15 && loaded.afk().minutes() == 9 && loaded.afk().enabled()
                    && loaded.border().dimensions().get("minecraft:the_nether").size() == 500, "Partial files merge onto the defaults");
            Files.writeString(file, "{broken");
            yes(h, PlayConfig.load(file).equals(PlayConfig.DEFAULT) && Files.readString(file).equals("{broken"), "A broken file is kept, defaults are used");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }
    static Set<String> roots(PlayConfig c) {
        var d = new CommandDispatcher<CommandSourceStack>();
        PlayCommands.register(d, c);
        var out = new TreeSet<String>();
        for (var n : d.getRoot().getChildren()) {
            if (n.getName().equals("simpletweaks")) continue;
            out.add(n.getName());
            if (n.getName().equals("tweaks") || n.getName().equals("vote")) n.getChildren().forEach(x -> out.add(n.getName() + " " + x.getName()));
        }
        return out;
    }
    static void commands(GameTestHelper h) {
        var on = roots(PlayConfig.DEFAULT);
        yes(h, on.containsAll(List.of("coords", "playtime", "seen", "pvp", "vote", "afk", "leaderboard", "phantoms", "pregen", "tweaks event", "tweaks border",
                "vote weather", "vote event", "vote yes", "vote no")), "Default commands: " + on);
        yes(h, !on.contains("tweaks cleanup") && !on.contains("vote day") && !on.contains("vote night") && !on.contains("simpletweaks"), "Off by default: cleanup, day/night vote; no /simpletweaks: " + on);
        var withDay = roots(PlayConfig.of("{\"vote\":{\"dayNight\":true},\"cleanup\":{\"enabled\":true}}"));
        yes(h, withDay.contains("vote day") && withDay.contains("vote night") && withDay.contains("tweaks cleanup"), "Switches add their commands: " + withDay);
        yes(h, roots(PlayConfig.of(ALL_OFF)).isEmpty(), "Switched-off features register no command: " + roots(PlayConfig.of(ALL_OFF)));
        h.succeed();
    }

    // ------------------------------------------------------------------ T4
    static void coords(GameTestHelper h) {
        var p = player(h, new BlockPos(1, 2, 1));
        var pos = p.blockPosition();
        var line = Coords.line(p, "base");
        yes(h, line.getString().contains(pos.getX() + " " + pos.getY() + " " + pos.getZ()) && line.getString().contains("base"), "Line shows the coordinates: " + line.getString());
        yes(h, hasClick(line, ClickEvent.CopyToClipboard.class), "Coordinates copy on click");
        Coords.reset();
        var server = h.getLevel().getServer();
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "coords hello");
        yes(h, Coords.pings() == 1, "/coords shares and pings");
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "coords again");
        yes(h, Coords.pings() == 1, "The cooldown stops a second share at once");
        Coords.reset();
        h.succeed();
    }

    // ------------------------------------------------------------------ T5
    static void deathHint(GameTestHelper h) {
        var p = player(h, new BlockPos(1, 2, 1));
        DeathHint.last = null;
        p.setHealth(0);
        p.die(h.getLevel().damageSources().generic());
        var pos = p.blockPosition();
        yes(h, DeathHint.last != null && DeathHint.last.getString().contains(pos.getX() + " " + pos.getY() + " " + pos.getZ()), "The dead player gets the place: " + DeathHint.last);
        yes(h, hasClick(DeathHint.last, ClickEvent.CopyToClipboard.class), "Death place copies on click");
        var alive = player(h, new BlockPos(2, 2, 2));
        DeathHint.last = null;
        DeathHint.died(alive);
        yes(h, DeathHint.last == null, "No hint while alive (e.g. downed with Simple Respawn)");
        with(PlayConfig.of("{\"deathHint\":{\"enabled\":false}}"), () -> { DeathHint.died(p); yes(h, DeathHint.last == null, "Switched off: no hint"); });
        h.succeed();
    }

    // ------------------------------------------------------------------ T6
    static void mentions(GameTestHelper h) {
        var a = player(h, new BlockPos(1, 2, 1));
        var b = player(h, new BlockPos(2, 2, 1));
        var c = player(h, new BlockPos(3, 2, 1));
        Map<ServerPlayer, String> names = Map.of(a, "Alice", b, "Bob", c, "Carl");
        var online = List.of(a, b, c);
        var r = Mentions.decorate(online, names::get, a, false, "hi @bob and @nobody");
        yes(h, r != null && r.mentioned().equals(Set.of(b)) && r.text().getString().equals("hi @bob and @nobody"), "Bob is mentioned (case-insensitive), text unchanged");
        yes(h, Mentions.decorate(online, names::get, a, false, "no mention here") == null, "No mention = no change");
        yes(h, Mentions.decorate(online, names::get, a, false, "@Alice") == null || Mentions.decorate(online, names::get, a, false, "@Alice").mentioned().isEmpty(), "No sound for the sender");
        var all = Mentions.decorate(online, names::get, a, true, "@everyone look");
        yes(h, all != null && all.mentioned().equals(Set.of(b, c)), "@everyone from an operator pings all others");
        yes(h, Mentions.decorate(online, names::get, a, false, "@everyone look") == null, "@everyone needs the permission");
        h.succeed();
    }

    // ------------------------------------------------------------------ T7
    static void playtime(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        p.getStats().setValue(p, Stats.CUSTOM.get(Stats.PLAY_TIME), 72000);
        yes(h, StatBoard.value(StatBoard.counter(server, p.getUUID()), "playtime") == 72000, "Online play time from the live statistic");
        var offline = UUID.randomUUID();
        var stats = new ServerStatsCounter(server, server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_STATS_DIR).resolve(offline + ".json"));
        stats.setValue(p, Stats.CUSTOM.get(Stats.PLAY_TIME), 36000);
        stats.save();
        yes(h, StatBoard.value(StatBoard.counter(server, offline), "playtime") == 36000, "Offline play time from the statistics file");
        yes(h, PlayText.duration(3_600_000L + 120_000L).getString().length() > 0, "Duration text");
        com.simplebuilding.modules.simpletweaks.play.seen.Playtime.left(p);
        var seen = Play.state().lastSeen.get(p.getUUID().toString());
        yes(h, seen != null && System.currentTimeMillis() - seen < 60_000 && "test-mock-player".equals(Play.state().names.get(p.getUUID().toString())), "Leaving stores time and name");
        Play.state().lastSeen.remove(p.getUUID().toString());
        Play.state().names.remove(p.getUUID().toString());
        try { Files.deleteIfExists(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_STATS_DIR).resolve(offline + ".json")); } catch (java.io.IOException ignored) {}
        h.succeed();
    }

    // ------------------------------------------------------------------ T8
    static void griefing(GameTestHelper h) {
        var level = h.getLevel();
        var rules = level.getGameRules();
        boolean vanilla = rules.get(GameRules.MOB_GRIEFING);
        var creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        var sheep = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
        var ghast = EntityTypes.GHAST.create(level, EntitySpawnReason.COMMAND);
        var fireball = EntityTypes.FIREBALL.create(level, EntitySpawnReason.COMMAND);
        fireball.setOwner(ghast);
        with(PlayConfig.of("{\"mobGriefing\":{\"enabled\":true}}"), () -> {
            try {
                MobGriefing.enter(creeper);
                yes(h, !rules.get(GameRules.MOB_GRIEFING), "A listed creeper may not grief");
                MobGriefing.enter(sheep);
                yes(h, rules.get(GameRules.MOB_GRIEFING) == vanilla, "A sheep follows the Vanilla rule");
                MobGriefing.enter(fireball);
                yes(h, !rules.get(GameRules.MOB_GRIEFING), "A ghast fireball counts as its ghast");
            } finally { MobGriefing.exit(); }
            yes(h, rules.get(GameRules.MOB_GRIEFING) == vanilla, "Outside entity ticks the Vanilla rule is unchanged");
        });
        MobGriefing.enter(creeper);
        try { yes(h, rules.get(GameRules.MOB_GRIEFING) == vanilla, "Switched off (default): Vanilla"); } finally { MobGriefing.exit(); }
        h.succeed();
    }

    // ------------------------------------------------------------------ T9
    static void pvp(GameTestHelper h) {
        var a = player(h, new BlockPos(1, 2, 1));
        var b = player(h, new BlockPos(2, 2, 1));
        Pvp.resetCooldowns();
        boolean vanilla = h.getLevel().isPvpAllowed();
        yes(h, Pvp.on(a) && Pvp.on(b) && a.canHarmPlayer(b) == vanilla, "Default: PvP on as in Vanilla");
        Pvp.set(a, false);
        yes(h, !a.canHarmPlayer(b) && !b.canHarmPlayer(a), "One side off: nobody hurts the other");
        yes(h, Pvp.waitSeconds(a) > 0, "Switching again waits for the cooldown");
        Pvp.set(a, true);
        yes(h, a.canHarmPlayer(b) == vanilla, "Both on again: Vanilla rules");
        var s = Play.state();
        s.pvpOn.remove(a.getUUID().toString()); s.pvpOff.remove(a.getUUID().toString());
        Pvp.resetCooldowns();
        h.succeed();
    }

    // ------------------------------------------------------------------ T19
    static void welcome(GameTestHelper h) {
        var p = player(h, new BlockPos(1, 2, 1));
        yes(h, Welcome.vanillaLine(p, true), "Off by default: Vanilla join line");
        with(PlayConfig.of("{\"welcome\":{\"enabled\":true,\"tips\":[\"a\",\"b\"],\"joinMessage\":\"J {player}\"}}"), () -> {
            yes(h, Welcome.fill("Hi {player}", p).equals("Hi test-mock-player"), "Placeholder filled");
            var first = Welcome.nextTip();
            var second = Welcome.nextTip();
            yes(h, first != null && second != null && !first.equals(second) && Welcome.nextTip().equals(first), "Tips rotate: " + first + ", " + second);
            yes(h, !Welcome.vanillaLine(p, true), "Own join line replaces the Vanilla one");
            yes(h, Welcome.vanillaLine(p, false), "Empty leave line keeps Vanilla");
        });
        h.succeed();
    }

    // ------------------------------------------------------------------ T20
    static void reward(GameTestHelper h) {
        var p = player(h, new BlockPos(1, 2, 1));
        var zone = ZoneId.systemDefault();
        var day = LocalDate.of(2026, 10, 17).atTime(12, 0).atZone(zone).toInstant();
        with(PlayConfig.of("{\"loginReward\":{\"enabled\":true,\"items\":[\"minecraft:bread 2\"],\"bonusEvery\":2,\"bonusItems\":[\"minecraft:apple 1\"]}}"), () -> {
            try {
                LoginReward.useClock(Clock.fixed(day, zone));
                p.getInventory().clearContent();
                yes(h, LoginReward.claim(p) == 1, "First day");
                yes(h, p.getInventory().countItem(Items.BREAD) == 2, "Reward items given");
                yes(h, LoginReward.claim(p) == 0, "Only once per day");
                LoginReward.useClock(Clock.fixed(day.plus(Duration.ofDays(1)), zone));
                yes(h, LoginReward.claim(p) == 2 && p.getInventory().countItem(Items.APPLE) == 1, "Next day: streak 2 and the bonus");
                LoginReward.useClock(Clock.fixed(day.plus(Duration.ofDays(4)), zone));
                yes(h, LoginReward.claim(p) == 1, "A gap starts again");
                var bill = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(LoginReward.BILL);
                boolean hasBills = false;
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) hasBills |= net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getInventory().getItem(i).getItem()).equals(LoginReward.BILL);
                yes(h, hasBills == bill, "Money bills only with Simple Money (loaded=" + bill + ")");
            } finally {
                LoginReward.useClock(Clock.systemDefaultZone());
                Play.state().streaks.remove(p.getUUID().toString());
            }
        });
        h.succeed();
    }

    // ------------------------------------------------------------------ T21
    static void events(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var s = ServerEventsSchedule.parse("SAT 20:00 double_xp 60");
        yes(h, s != null && s.startsAt(LocalDateTime.of(2026, 10, 17, 20, 0)) && !s.startsAt(LocalDateTime.of(2026, 10, 18, 20, 0)), "Schedule line (17.10.2026 is a Saturday)");
        yes(h, ServerEventsSchedule.parse("DAILY 07:30 more_loot 10").startsAt(LocalDateTime.of(2026, 10, 19, 7, 30)) && ServerEventsSchedule.parse("SAT 20:00 party 60") == null, "Daily line, unknown event refused");
        var cow = EntityTypes.COW.create(h.getLevel(), EntitySpawnReason.COMMAND);
        try {
            yes(h, ServerEvents.start(server, "double_xp", 5) && ServerEvents.is("double_xp"), "Double XP started");
            yes(h, ServerEvents.xp(10) == 20 && ServerEvents.xp(0) == 0, "Experience doubled");
            var at = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 3, 2)));
            // 2 -> 4 = orbs 3+1: distinct sizes never merge (7 -> 14 = two 7-orbs merge into one count-2 orb, getValue() then reads 7)
            ExperienceOrb.award(h.getLevel(), at, 2);
            int total = 0;
            for (var orb : h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new net.minecraft.world.phys.AABB(at, at).inflate(2))) { total += orb.getValue(); orb.discard(); }
            yes(h, total == 4, "Awarded orbs carry double experience: " + total);
            yes(h, ServerEvents.extraLootRolls(cow, true) == 0, "No extra loot during double XP");
            ServerEvents.start(server, "more_loot", 5);
            yes(h, ServerEvents.extraLootRolls(cow, true) == 1 && ServerEvents.extraLootRolls(cow, false) == 0, "More loot only for player kills");
            int[] inner = {-1};
            ServerEvents.extra(1, () -> inner[0] = ServerEvents.extraLootRolls(cow, true));
            yes(h, inner[0] == 0, "The extra roll does not roll again");
            yes(h, ServerEvents.stop(server) && ServerEvents.current() == null, "Stopped");
        } finally { ServerEvents.shutdown(); }
        h.succeed();
    }

    // ------------------------------------------------------------------ T22
    static void leaderboard(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        p.getStats().setValue(p, Stats.CUSTOM.get(Stats.DEATHS), 1_000_000);
        var top = StatBoard.top(server, "deaths", 3);
        yes(h, !top.isEmpty() && top.get(0).id().equals(p.getUUID()) && top.get(0).value() == 1_000_000, "Highest first: " + top);
        p.getStats().setValue(p, Stats.BLOCK_MINED.get(Blocks.STONE), 5);
        p.getStats().setValue(p, Stats.BLOCK_MINED.get(Blocks.DIRT), 6);
        yes(h, StatBoard.value(p.getStats(), "mined") == 11, "Blocks mined are summed over all blocks");
        var board = server.getScoreboard();
        yes(h, board.getObjective(Leaderboard.OBJECTIVE) == null, "Sidebar off by default: no objective");
        with(PlayConfig.of("{\"leaderboard\":{\"sidebar\":true,\"sidebarStat\":\"deaths\"}}"), () -> {
            Leaderboard.refresh(server);
            var obj = board.getObjective(Leaderboard.OBJECTIVE);
            yes(h, obj != null && board.getDisplayObjective(DisplaySlot.SIDEBAR) == obj, "Sidebar shows the objective");
            board.removeObjective(obj);
        });
        p.getStats().setValue(p, Stats.CUSTOM.get(Stats.DEATHS), 0);
        h.succeed();
    }

    // ------------------------------------------------------------------ T23
    static void vote(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var p = player(h, new BlockPos(1, 2, 1));
        yes(h, Votes.passes(2, 4, 50) && !Votes.passes(1, 4, 50) && Votes.passes(1, 1, 100), "Majority rule");
        var done = new ArrayList<String>();
        Votes.reset();
        Votes.action = (s, topic) -> done.add(topic);
        Votes.eligibleCount = s -> 2;
        try {
            with(PlayConfig.of("{\"vote\":{\"percent\":50}}"), () -> {
                yes(h, Votes.start(server, p, "weather_clear") == null, "Vote started");
                yes(h, done.equals(List.of("weather_clear")) && Votes.running() == null, "One yes of two players is 50 %: " + done);
                yes(h, Votes.start(server, p, "weather_clear") != null, "The topic waits for its cooldown");
            });
            with(PlayConfig.of("{\"vote\":{\"percent\":100,\"cooldownSeconds\":0}}"), () -> {
                Votes.start(server, p, "event_double_xp");
                yes(h, Votes.running() != null && !done.contains("event_double_xp"), "100 % of two players needs a second yes");
                Votes.reset();
            });
            yes(h, Votes.topic("event_double_xp").getString().length() > 0, "Topic text");
        } finally { Votes.reset(); }
        h.succeed();
    }

    // ------------------------------------------------------------------ T24
    static void afk(GameTestHelper h) {
        var a = player(h, new BlockPos(1, 2, 1));
        var b = player(h, new BlockPos(2, 2, 1));
        var status = new SleepStatus();
        status.update(List.of(a, b));
        yes(h, status.sleepersNeeded(100) == 2, "Both count for sleeping");
        Afk.set(a, true, true);
        try {
            yes(h, Afk.afk(a) && !Afk.afk(b), "Marked AFK");
            var name = Afk.tabName(a, null);
            yes(h, name != null && name.getStyle().getColor() != null && name.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GRAY)), "Grey in the tab list");
            yes(h, a.getTabListDisplayName() != null, "The tab list name is replaced");
            status.update(List.of(a, b));
            yes(h, status.sleepersNeeded(100) == 1, "An AFK player does not count for sleeping");
            yes(h, Votes.eligible(h.getLevel().getServer()) >= 1, "Votes count active players");
        } finally { Afk.set(a, false, false); }
        yes(h, !Afk.afk(a), "Back from AFK");
        h.succeed();
    }

    // ------------------------------------------------------------------ T14 x T19/T24/T7/T22/T6
    static void vanished(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var ghost = player(h, new BlockPos(1, 2, 1));
        var guest = player(h, new BlockPos(2, 2, 1));
        var admin = player(h, new BlockPos(3, 2, 1));
        server.getPlayerList().op(admin.nameAndId(), Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), Optional.empty());
        ghost.getStats().setValue(ghost, Stats.CUSTOM.get(Stats.DEATHS), 2_000_000);
        com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.set(ghost, true);
        try {
            with(PlayConfig.of("{\"welcome\":{\"enabled\":true,\"joinMessage\":\"J {player}\",\"leaveMessage\":\"L {player}\"}}"), () -> {
                yes(h, !Welcome.vanillaLine(ghost, true) && !Welcome.vanillaLine(ghost, false), "T19: no join/leave line for a vanished player");
                yes(h, !Welcome.vanillaLine(guest, true), "T19: a visible player still gets the own line");
            });
            Afk.set(ghost, true, true);
            try {
                var viewers = Afk.tabViewers(ghost);
                yes(h, !viewers.contains(guest) && viewers.contains(admin), "T24: tab list name update only to players who may see the vanished one");
            } finally { Afk.set(ghost, false, false); }
            var guestSource = guest.createCommandSourceStack();
            var adminSource = admin.createCommandSourceStack();
            yes(h, !com.simplebuilding.modules.simpletweaks.play.seen.Playtime.shownOnline(guestSource, ghost.getUUID()), "T7: /seen shows a vanished player as not online");
            yes(h, com.simplebuilding.modules.simpletweaks.play.seen.Playtime.shownOnline(adminSource, ghost.getUUID()), "T7: admins that may see it still see it online");
            yes(h, com.simplebuilding.modules.simpletweaks.play.seen.Playtime.shownOnline(server.createCommandSourceStack(), ghost.getUUID()), "T7: the console sees everybody");
            yes(h, com.simplebuilding.modules.simpletweaks.play.seen.Playtime.shownOnline(guestSource, admin.getUUID()), "T7: visible players are online");
            yes(h, Leaderboard.top(server, "deaths", guestSource).stream().noneMatch(e -> e.id().equals(ghost.getUUID())), "T22: leaderboard leaves the vanished player out");
            yes(h, Leaderboard.top(server, "deaths", null).stream().noneMatch(e -> e.id().equals(ghost.getUUID())), "T22: sidebar leaves the vanished player out");
            yes(h, Leaderboard.top(server, "deaths", adminSource).stream().anyMatch(e -> e.id().equals(ghost.getUUID())), "T22: admins that may see it get the full list");
            var mention = com.simplebuilding.modules.simpletweaks.play.chat.Mentions.decorate(List.of(ghost, guest, admin), p -> "x", guest, false, "@x");
            yes(h, mention != null && !mention.mentioned().contains(ghost) && mention.mentioned().contains(admin) && !mention.mentioned().contains(guest), "T6: a mention does not highlight or ping a vanished player");
        } finally {
            com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.set(ghost, false);
            server.getPlayerList().deop(admin.nameAndId());
            ghost.getStats().setValue(ghost, Stats.CUSTOM.get(Stats.DEATHS), 0);
        }
        yes(h, Welcome.vanillaLine(ghost, true), "Visible again: Vanilla join line");
        h.succeed();
    }

    // ------------------------------------------------------------------ T25
    static void border(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
        var over = server.overworld();
        if (nether == null) { h.succeed(); return; }
        double oldNether = nether.getWorldBorder().getSize(), oldOver = over.getWorldBorder().getSize();
        double nx = nether.getWorldBorder().getCenterX(), nz = nether.getWorldBorder().getCenterZ();
        yes(h, Borders.apply(server) == 0, "Without entries nothing changes");
        with(PlayConfig.of("{\"border\":{\"dimensions\":{\"minecraft:the_nether\":{\"size\":1234,\"centerX\":16,\"centerZ\":-16}}}}"), () -> {
            yes(h, Borders.apply(server) == 1, "One dimension configured");
            yes(h, nether.getWorldBorder().getSize() == 1234 && nether.getWorldBorder().getCenterX() == 16 && over.getWorldBorder().getSize() == oldOver,
                    "Only the Nether border changed (Vanilla per-dimension border)");
        });
        nether.getWorldBorder().setCenter(nx, nz);
        nether.getWorldBorder().setSize(oldNether);
        h.succeed();
    }

    // ------------------------------------------------------------------ T26
    static void roof(GameTestHelper h) {
        var level = h.getLevel();
        var p = player(h, new BlockPos(1, 2, 1));
        yes(h, NetherRoof.exempt(p), "Creative players may stay (mock players are creative)");
        p.setPos(p.getX(), 140, p.getZ());
        NetherRoof.moveDown(level, p);
        yes(h, p.getY() < NetherRoof.ROOF, "Moved below the roof: " + p.getY());
        h.succeed();
    }

    // ------------------------------------------------------------------ T27
    static void keep(GameTestHelper h) {
        var level = h.getLevel();
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) { h.succeed(); return; }
        var p = player(h, new BlockPos(1, 2, 1));
        var inv = p.getInventory();
        inv.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        inv.setItem(12, new ItemStack(Items.COBBLESTONE, 32));
        inv.setItem(39, new ItemStack(Items.IRON_HELMET));
        inv.setItem(40, new ItemStack(Items.SHIELD));
        p.setExperienceLevels(10);
        yes(h, KeepInventory.droppedXp(p, 70) == 70, "hotbar_armor mode drops the experience");
        p.setHealth(0);
        p.die(level.damageSources().generic());
        yes(h, inv.getItem(0).is(Items.DIAMOND_SWORD) && inv.getItem(39).is(Items.IRON_HELMET) && inv.getItem(40).is(Items.SHIELD) && inv.getItem(12).isEmpty(),
                "Hotbar, armor and offhand kept, main inventory dropped");
        var next = player(h, new BlockPos(2, 2, 2));
        next.getInventory().clearContent();
        next.restoreFrom(p, false);
        yes(h, next.getInventory().getItem(0).is(Items.DIAMOND_SWORD) && next.getInventory().getItem(39).is(Items.IRON_HELMET) && next.getInventory().getItem(12).isEmpty(), "Kept stacks move to the respawned player");
        yes(h, inv.getItem(0).isEmpty() && inv.getItem(39).isEmpty() && p.entityTags().stream().noneMatch(t -> t.startsWith("simpletweaks_kept:")),
                "The dead player keeps no copy (moved, not copied)");
        var twice = player(h, new BlockPos(3, 2, 1));
        twice.getInventory().clearContent();
        twice.restoreFrom(p, false);
        yes(h, twice.getInventory().isEmpty(), "A second respawn from the same death gives nothing (no dupe)");
        var plain = player(h, new BlockPos(1, 2, 3));
        plain.getInventory().setItem(0, new ItemStack(Items.STICK));
        plain.getInventory().dropAll();
        yes(h, plain.getInventory().getItem(0).isEmpty(), "dropAll outside a death is untouched by T27");
        with(PlayConfig.of("{\"keepInventory\":{\"mode\":\"xp\"}}"), () -> {
            yes(h, !KeepInventory.keepsSlot(0) && KeepInventory.droppedXp(p, 70) == 0, "xp mode: items drop, experience stays");
            var again = player(h, new BlockPos(3, 2, 3));
            again.restoreFrom(p, false);
            yes(h, again.experienceLevel == 10, "Experience restored: " + again.experienceLevel);
        });
        with(PlayConfig.of("{\"keepInventory\":{\"mode\":\"custom\",\"keepHotbar\":false,\"keepMain\":true,\"keepXpPercent\":50}}"), () -> {
            yes(h, !KeepInventory.keepsSlot(0) && KeepInventory.keepsSlot(12) && KeepInventory.keepsSlot(39) && KeepInventory.droppedXp(p, 70) == 35, "custom mode per part");
        });
        yes(h, KeepInventory.points(1, 0f) == 7 && KeepInventory.points(2, 0f) == 16 && KeepInventory.points(16, 0f) == 352, "Experience points formula");
        h.succeed();
    }

    // ------------------------------------------------------------------ T28
    static void dragon(GameTestHelper h) {
        var level = h.getLevel();
        var podium = h.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(podium, Blocks.BEDROCK.defaultBlockState(), 3);
        yes(h, DragonEgg.give(level, podium), "Egg placed");
        var top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, podium);
        yes(h, level.getBlockState(top.below()).is(Blocks.DRAGON_EGG), "Egg on top of the podium: " + level.getBlockState(top.below()));
        yes(h, !DragonEgg.give(level, podium), "A second egg is not stacked");
        var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(top).inflate(3), e -> e.getItem().is(Items.DRAGON_EGG));
        yes(h, items.size() == 1, "A second egg on a taken spot drops as an item");
        items.forEach(Entity::discard);
        level.setBlock(top.below(), Blocks.AIR.defaultBlockState(), 3);
        yes(h, DragonEgg.active(), "On by default");
        h.succeed();
    }

    // ------------------------------------------------------------------ T30
    static void phantoms(GameTestHelper h) {
        var p = player(h, new BlockPos(1, 2, 1));
        yes(h, !Phantoms.off(p), "Phantoms on by default");
        Phantoms.set(p, false);
        yes(h, Phantoms.off(p) && Play.state().phantomsOff.contains(p.getUUID().toString()), "Opted out (saved)");
        Phantoms.set(p, true);
        yes(h, !Phantoms.off(p), "Back on");
        h.succeed();
    }

    // ------------------------------------------------------------------ T31
    static void crowd(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        var at = h.absolutePos(new BlockPos(2, 2, 2));
        var mobs = new ArrayList<Entity>();
        with(PlayConfig.of("{\"entityLimit\":{\"enabled\":true,\"perChunk\":4}}"), () -> {
            try {
                EntityLimit.reset();
                for (int i = 0; i < 6; i++) {
                    var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
                    cow.setPos(Vec3.atBottomCenterOf(at));
                    cow.setNoAi(true);
                    level.addFreshEntity(cow);
                    mobs.add(cow);
                }
                EntityLimit.scan(server);
                yes(h, EntityLimit.over(level, at), "Chunk over the limit is reported");
                var extra = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
                extra.setPos(Vec3.atBottomCenterOf(at));
                yes(h, !level.addFreshEntity(extra), "No new mob in a crowded chunk");
                yes(h, mobs.stream().allMatch(Entity::isAlive), "Nothing deleted");
                mobs.forEach(Entity::discard);
                EntityLimit.scan(server);
                yes(h, !EntityLimit.over(level, at), "Under the limit again");
            } finally { mobs.forEach(Entity::discard); EntityLimit.reset(); }
        });
        h.succeed();
    }

    // ------------------------------------------------------------------ T32
    static void cleanup(GameTestHelper h) {
        var level = h.getLevel();
        var pos = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2)));
        var plain = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.COBBLESTONE));
        var named = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.COBBLESTONE));
        named.getItem().set(DataComponents.CUSTOM_NAME, Component.literal("Keep"));
        var star = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.NETHER_STAR));
        var rare = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        var young = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.DIRT));
        for (var e : List.of(plain, named, star, rare, young)) { e.setNoPickUpDelay(); level.addFreshEntity(e); }
        for (var e : List.of(plain, named, star, rare)) e.tickCount = 20 * 600;
        young.tickCount = 0;
        with(PlayConfig.of("{\"cleanup\":{\"enabled\":true}}"), () -> {
            yes(h, !GroundCleanup.exempt(plain) && GroundCleanup.exempt(named) && GroundCleanup.exempt(star) && GroundCleanup.exempt(rare), "Named, listed and rare items are exempt");
            GroundCleanup.clear(level.getServer());
            yes(h, plain.isRemoved() && !named.isRemoved() && !star.isRemoved() && !rare.isRemoved() && !young.isRemoved(), "Only old common items are cleared");
            GroundCleanup.reset();
        });
        for (var e : List.of(named, star, rare, young)) e.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ T33
    static void pregen(GameTestHelper h) {
        var seen = new HashSet<Long>();
        for (int i = 0; i < 25; i++) { var o = Pregen.spiral(i); seen.add(((long) o[0] << 32) ^ (o[1] & 0xffffffffL)); yes(h, Math.max(Math.abs(o[0]), Math.abs(o[1])) <= 2, "Spiral stays in the square"); }
        yes(h, seen.size() == 25 && Pregen.total(32) == 25 && Pregen.total(16) == 9, "Spiral covers each chunk once");
        if (Pregen.job() != null) { h.succeed(); return; }
        var level = h.getLevel();
        var job = Pregen.start(level, 16);
        // far away from spawn so the chunks are new
        job.centerX = 40_000 >> 4; job.centerZ = 40_000 >> 4;
        h.succeedWhen(() -> {
            var j = Pregen.job();
            if (j != null) h.assertTrue(false, "pregen still running: " + j.done + "/" + j.total);
            h.assertTrue(job.done == job.total && job.total == 9, "All 9 chunks generated: " + job.done + "/" + job.total);
        });
    }
    private PlayTests() {}
}
