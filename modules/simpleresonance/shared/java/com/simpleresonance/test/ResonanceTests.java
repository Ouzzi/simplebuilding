package com.simpleresonance.test;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.framework.api.ResonanceContracts;
import com.simpleresonance.LegacyImport;
import com.simpleresonance.PlayerRecord;
import com.simpleresonance.ResonanceMath;
import com.simpleresonance.ResonanceService;
import com.simpleresonance.SimpleResonance;
import com.simpleresonance.config.ResonanceConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * Test bodies shared by the Fabric @GameTest wrappers (ModuleGameTest) and the NeoForge registration (WP-22: player value, zones,
 * owner choice, newcomer protection, migration, contract). Every test that touches the config restores it.
 */
public final class ResonanceTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    /** The tests that need no running Resonance service (pure rules, registrations): what a loader without the service wiring can run. */
    public static final List<String> PURE = List.of("registered", "config_caps", "zone_curve", "player_value",
            "owner_probabilities", "owner_distribution_seeded", "newcomer_caps", "migration_legacy_block");
    private static final String OVERWORLD = "minecraft:overworld";

    private ResonanceTests() {}

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, double eps, String what) {
        check(Math.abs(actual - expected) <= eps, what + ": expected " + expected + " got " + actual);
    }

    /** Runs {@code body} and puts the config, the test players and the sub-mod switch back afterwards. */
    private static void guarded(Runnable body) {
        var snapshot = ResonanceConfig.toJson();
        boolean switchOn = com.simplebuilding.framework.api.SubMods.enabled(SimpleResonance.MOD_ID);
        try {
            body.run();
        } finally {
            ResonanceConfig.apply(snapshot);
            ResonanceService.testPlayers = null;
            com.simplebuilding.framework.api.SubMods.set(SimpleResonance.MOD_ID, switchOn);
        }
    }

    /** A mock player standing at the test-relative column x/z, in the server's player list for the length of the test. */
    private static ServerPlayer mock(GameTestHelper h, int x, int z) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> {
            h.getLevel().getServer().getPlayerList().remove(p);
            ResonanceService.INSTANCE.forget(p.getUUID());
        });
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        p.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void playHours(ServerPlayer p, double hours) {
        p.getStats().setValue(p, Stats.CUSTOM.get(Stats.PLAY_TIME), (int) Math.round(hours * 72000.0));
    }

    private static int run(CommandDispatcher<CommandSourceStack> d, String command, CommandSourceStack source) {
        try {
            return d.execute(command, source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return -1;
        }
    }

    private static void add(String name, Consumer<GameTestHelper> body) {
        ALL.put(name, body);
    }

    static {
        add("registered", h -> {
            check(ResonanceContracts.get() == ResonanceService.INSTANCE, "the contract carries the Resonance service");
            check(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(SimpleResonance.MOD_ID, "guide_book")), "guide book registered");
            h.succeed();
        });
        add("config_caps", h -> {
            guarded(() -> {
                var wild = new com.google.gson.JsonObject();
                wild.addProperty("z0", 99);
                wild.addProperty("zmax", -4);
                wild.addProperty("d1", 90000);
                wild.addProperty("d2", 5);
                wild.addProperty("lifeWeight", 7);
                wild.addProperty("deathKeep", -1);
                wild.addProperty("center", "nowhere");
                wild.addProperty("ownerRadius", 1);
                wild.addProperty("snapRatio", "x");
                ResonanceConfig.apply(wild);
                check(ResonanceConfig.z0 <= 5 && ResonanceConfig.zmax >= ResonanceConfig.z0 && ResonanceConfig.zmax <= 5, "zone levels clamped to 0..5");
                check(ResonanceConfig.d2 > ResonanceConfig.d1, "d2 stays above d1");
                near(ResonanceConfig.lifeWeight, 1.0, 1e-9, "lifeWeight capped at 1");
                near(ResonanceConfig.deathKeep, 0.0, 1e-9, "deathKeep floored at 0");
                check(ResonanceConfig.center.equals(ResonanceConfig.CENTER_SPAWN), "a broken centre falls back to the world spawn");
                near(ResonanceConfig.ownerRadius, 16, 1e-9, "owner radius floored");
                near(ResonanceConfig.snapRatio, 3.0, 1e-9, "a wrong type keeps the default");
                ResonanceConfig.reset();
                check(ResonanceConfig.parseCenter("100, -250")[1] == -250.0 && ResonanceConfig.parseCenter("1,2,3") == null, "centre parsing");
                check(ResonanceConfig.KEYS.size() == ResonanceConfig.toJson().size(), "KEYS lists every key of the file");
                for (String key : ResonanceConfig.KEYS) check(ResonanceConfig.toJson().has(key), "key " + key);
            });
            h.succeed();
        });
        add("sub_mod_switch", h -> {
            guarded(() -> {
                var p = mock(h, 2, 2);
                ResonanceService.testPlayers = List.of(p);
                BlockPos at = p.blockPosition();
                check(ResonanceService.INSTANCE.enabled(), "on by default");
                check(ResonanceService.INSTANCE.sample(OVERWORLD, at.getX(), at.getY(), at.getZ(), 0.0, 0.5) != ResonanceContracts.Spawn.NONE, "a sample while on");
                com.simplebuilding.framework.api.SubMods.set(SimpleResonance.MOD_ID, false);
                check(!ResonanceService.INSTANCE.enabled() && !SimpleResonance.on(), "the super-mod switch reaches the service");
                check(ResonanceService.INSTANCE.sample(OVERWORLD, at.getX(), at.getY(), at.getZ(), 0.0, 0.5) == ResonanceContracts.Spawn.NONE, "nothing while off");
                com.simplebuilding.framework.api.SubMods.set(SimpleResonance.MOD_ID, true);
                ResonanceConfig.enabled = false;
                check(!ResonanceService.INSTANCE.enabled(), "the own switch too");
            });
            h.succeed();
        });
        add("zone_curve", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                // KONZEPT-A-WP22 1.1: 1000 -> 0.6, 2000 -> 1.3, 3000 -> 2.4, 4000 -> 3.6, 6000 -> 5
                near(ResonanceMath.curve(0), 0.5, 1e-9, "close to the centre is light but not peaceful");
                near(ResonanceMath.curve(512), 0.5, 1e-9, "flat up to 512");
                near(ResonanceMath.curve(1000), 0.6, 0.05, "1000 blocks");
                near(ResonanceMath.curve(2000), 1.3, 0.1, "2000 blocks");
                near(ResonanceMath.curve(3000), 2.4, 0.1, "3000 blocks");
                near(ResonanceMath.curve(4000), 3.6, 0.1, "4000 blocks");
                near(ResonanceMath.curve(6000), 5.0, 1e-9, "full at 6000");
                near(ResonanceMath.curve(60000), 5.0, 1e-9, "and beyond");
                double last = -1;
                for (int d = 0; d <= 7000; d += 50) {
                    double z = ResonanceMath.curve(d);
                    check(z >= last - 1e-12, "monotonic at " + d);
                    last = z;
                }
                // soft: no step bigger than a quarter level per 50 blocks
                for (int d = 0; d < 7000; d += 50) check(ResonanceMath.curve(d + 50) - ResonanceMath.curve(d) < 0.25, "no hard edge at " + d);
            });
            h.succeed();
        });
        add("zone_dimensions", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                near(ResonanceMath.zone(OVERWORLD, 3000, 0, 0, 0), ResonanceMath.curve(3000), 1e-9, "Overworld: distance to the centre");
                near(ResonanceMath.zone(OVERWORLD, 3500, 500, 500, 500), ResonanceMath.curve(3000), 1e-9, "centre moves the zones");
                near(ResonanceMath.zone("minecraft:the_nether", 0, 0, 0, 0), 2.0, 1e-9, "Nether never milder than netherZ0");
                near(ResonanceMath.zone("minecraft:the_nether", 500, 0, 0, 0), ResonanceMath.curve(4000), 1e-9, "Nether counts eightfold");
                near(ResonanceMath.zone("minecraft:the_end", 0, 0, 0, 0), 3.0, 1e-9, "End never milder than endZ0");
                near(ResonanceMath.zone("minecraft:the_end", 2000, 0, 0, 0), 4.0, 1e-9, "outer End islands add one");
                near(ResonanceMath.zone("somemod:dim", 3000, 0, 0, 0), ResonanceMath.curve(3000), 1e-9, "other dimensions use the Overworld curve");
                ResonanceConfig.otherDimensions = false;
                near(ResonanceMath.zone("somemod:dim", 3000, 0, 0, 0), 0.0, 1e-9, "other dimensions off");
                ResonanceConfig.overworld = false;
                ResonanceConfig.nether = false;
                ResonanceConfig.end = false;
                near(ResonanceMath.zone(OVERWORLD, 5000, 0, 0, 0) + ResonanceMath.zone("minecraft:the_nether", 5000, 0, 0, 0)
                        + ResonanceMath.zone("minecraft:the_end", 5000, 0, 0, 0), 0.0, 1e-9, "each vanilla dimension switchable");
                ResonanceConfig.reset();
                ResonanceConfig.center = "-3000,0";
                var svc = ResonanceService.INSTANCE;
                near(svc.centre()[0], -3000, 1e-9, "fixed centre");
                near(svc.zoneLevel(OVERWORLD, 0, 0), ResonanceMath.curve(3000), 1e-9, "service zone level from the configured centre");
                ResonanceConfig.center = "world_spawn";
                var spawn = h.getLevel().getServer().getRespawnData().pos();
                near(svc.centre()[0], spawn.getX() + 0.5, 1e-9, "world spawn centre");
            });
            h.succeed();
        });
        add("player_value", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                near(ResonanceMath.longTerm(0), 0.0, 1e-9, "no points, no long-term value");
                // KONZEPT 1.2 examples: 5 h = 10 points -> 0.28, 15 h -> 0.63, 30 h -> 0.86 (one point per 30 active minutes)
                near(ResonanceMath.longTerm(5 * 2), 0.28, 0.01, "5 active hours");
                near(ResonanceMath.longTerm(15 * 2), 0.63, 0.01, "15 active hours");
                near(ResonanceMath.longTerm(30 * 2), 0.86, 0.01, "30 active hours");
                near(ResonanceMath.TICKS_PER_POINT, 36000.0, 1e-9, "30 active minutes per point");
                near(ResonanceMath.value(0, 0), 0.0, 1e-9, "a fresh player");
                near(ResonanceMath.value(1e9, 1.0), 1.0, 1e-6, "the best player");
                near(ResonanceMath.value(1e9, 0.0), 0.7, 1e-6, "long-term alone is worth 70 %");
                near(ResonanceMath.value(0, 1.0), 0.3, 1e-9, "the share since the last death alone 30 %");
                near(ResonanceMath.level(0.5), 2.5, 1e-9, "P = 5 x W");
                // the three factors are the core's: fresh = 0.1 each, full at 1395 xp points / 4000 m + 1 h / 100 kills
                near(ResonanceMath.xpFactor(0), 0.1, 1e-9, "xp factor fresh");
                near(ResonanceMath.xpFactor(ResonanceMath.XP_POINTS_FOR_FULL_FACTOR), 1.0, 1e-9, "xp factor full");
                near(ResonanceMath.survivalFactor(0, 0), 0.1, 1e-9, "survival fresh");
                near(ResonanceMath.combatFactor(0, 0, 0), 0.1, 1e-9, "combat fresh");
                near(ResonanceMath.fraction(ResonanceMath.meanFactor(0.1, 0.1, 0.1)), 0.0, 1e-9, "F of a fresh player");
                near(ResonanceMath.fraction(ResonanceMath.meanFactor(1.0, 1.0, 1.0)), 1.0, 1e-9, "F of a full player");
            });
            h.succeed();
        });
        add("death_and_respawn", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceService.INSTANCE;
                var p = mock(h, 2, 2);
                ResonanceService.testPlayers = List.of(p);
                PlayerRecord r = svc.data().record(p.getUUID());
                r.points = 40;
                r.hostile = 12;
                p.totalExperience = 600;
                double before = svc.valueOf(p);
                svc.onDeath(p);
                near(r.points, 30.0, 1e-9, "a death keeps 75 % of the points");
                svc.onRespawn(p, true);
                check(r.baseXp == 0 && r.baseHostile == 0, "no reset when the player did not die");
                svc.onRespawn(p, false);
                check(r.baseXp == 600 && r.baseHostile == 12, "F counts from the death on");
                double after = svc.valueOf(p);
                check(after < before && after > 0.0, "the value falls by part, not to zero: " + before + " -> " + after);
                double[] f = svc.factors(p);
                near(f[0], 0.1, 1e-9, "xp factor starts again");
                ResonanceConfig.deathKeep = 0.5;
                svc.onDeath(p);
                near(r.points, 15.0, 1e-9, "configurable");
            });
            h.succeed();
        });
        add("kill_points_farm_guard", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceService.INSTANCE;
                var p = mock(h, 2, 2);
                PlayerRecord r = svc.data().record(p.getUUID());
                double start = r.points;
                Zombie z = h.spawn(EntityTypes.ZOMBIE, 5, 2, 5);
                z.setNoAi(true);
                svc.onKill(p, z);
                near(r.points - start, ResonanceConfig.killPoints, 1e-9, "one hostile kill");
                check(r.hostile == 1, "counted");
                for (int i = 0; i < 100; i++) svc.onKill(p, z);
                near(r.points - start, ResonanceConfig.killPointsPerHour, 1e-9, "kill points stop at the hourly cap");
                check(r.hostile == 101, "kills keep counting for F");
                playHours(p, 2);
                svc.onKill(p, z);
                check(r.points - start > ResonanceConfig.killPointsPerHour, "the next played hour opens a new window");
                var cow = h.spawn(EntityTypes.COW, 6, 2, 6);
                cow.setNoAi(true);
                svc.onKill(p, cow);
                check(r.passive == 1, "passive kills count for F only");
            });
            h.succeed();
        });
        add("boss_award", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceService.INSTANCE;
                var p = mock(h, 2, 2);
                PlayerRecord r = svc.data().record(p.getUUID());
                double start = r.points;
                svc.award(p.getUUID(), "minecraft:wither", 5.0);
                near(r.points - start, 5.0, 1e-9, "wither");
                svc.award(p.getUUID(), "minecraft:wither", 5.0);
                near(r.points - start, 5.0, 1e-9, "the same boss within 24 played hours gives nothing");
                svc.award(p.getUUID(), "minecraft:ender_dragon", 10.0);
                near(r.points - start, 15.0, 1e-9, "another boss counts");
                playHours(p, 25);
                svc.award(p.getUUID(), "minecraft:wither", 5.0);
                near(r.points - start, 20.0, 1e-9, "after 24 played hours again");
                svc.award(p.getUUID(), "x", -3.0);
                near(r.points - start, 20.0, 1e-9, "no negative awards");
                near(ResonanceService.BOSS_POINTS.get("minecraft:warden"), 8.0, 1e-9, "warden points");
            });
            h.succeed();
        });
        add("owner_probabilities", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                // owner example: an attempt 75 % of the way to the newcomer A -> d_A = 0.25 D, d_B = 0.75 D -> 75 % / 25 %
                double[] p = ResonanceMath.ownerProbabilities(new double[] {25, 75}, 3.0, false);
                near(p[0], 0.75, 1e-9, "75 % for the near one");
                near(p[1], 0.25, 1e-9, "25 % for the far one");
                p = ResonanceMath.ownerProbabilities(new double[] {20, 80}, 3.0, false);
                near(p[0], 1.0, 1e-9, "inside the nearest quarter he takes all");
                near(p[1], 0.0, 1e-9, "and the other none");
                p = ResonanceMath.ownerProbabilities(new double[] {80, 20}, 3.0, false);
                near(p[1], 1.0, 1e-9, "order does not matter");
                p = ResonanceMath.ownerProbabilities(new double[] {50, 50}, 3.0, false);
                near(p[0], 0.5, 1e-9, "same distance, same chance");
                p = ResonanceMath.ownerProbabilities(new double[] {20, 80}, 0.0, false);
                near(p[0], 0.8, 1e-9, "snap off: plain 1/d weights");
                p = ResonanceMath.ownerProbabilities(new double[] {0, 0.5, 40}, 3.0, false);
                near(p[0] + p[1] + p[2], 1.0, 1e-9, "distances below one block count as one");
                p = ResonanceMath.ownerProbabilities(new double[] {7}, 3.0, false);
                near(p[0], 1.0, 1e-9, "a lone player");
                check(ResonanceMath.ownerProbabilities(new double[0], 3.0, false).length == 0, "nobody");
                // soft: 75 % stays 75 %, 82.5 % climbs, 90 % and more is 100 %
                p = ResonanceMath.ownerProbabilities(new double[] {25, 75}, 3.0, true);
                near(p[0], 0.75, 1e-9, "soft: at the line nothing changes");
                p = ResonanceMath.ownerProbabilities(new double[] {17.5, 82.5}, 3.0, true);
                check(p[0] > 0.825 && p[0] < 1.0 && Math.abs(p[0] + p[1] - 1.0) < 1e-9, "soft: between the line and 90 % it climbs smoothly: " + p[0]);
                p = ResonanceMath.ownerProbabilities(new double[] {10, 90}, 3.0, true);
                near(p[0], 1.0, 1e-9, "soft: from 90 % it is 100 %");
                check(ResonanceMath.pick(new double[] {0.2, 0.8}, 0.1) == 0 && ResonanceMath.pick(new double[] {0.2, 0.8}, 0.2) == 1
                        && ResonanceMath.pick(new double[] {0.2, 0.8}, 0.999999) == 1 && ResonanceMath.pick(new double[0], 0.5) == -1, "pick by roll");
                check(ResonanceMath.pick(new double[] {1.0, 0.0}, 0.9999999999) == 0, "a zero chance is never picked at the end");
            });
            h.succeed();
        });
        add("owner_distribution_seeded", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                Random random = new Random(20261030L);
                double[] distances = {30, 60, 90, 120};
                double[] prob = ResonanceMath.ownerProbabilities(distances, 3.0, false);
                int[] hits = new int[4];
                int n = 20000;
                for (int i = 0; i < n; i++) hits[ResonanceMath.pick(prob, random.nextDouble())]++;
                double sum = 0;
                for (int i = 0; i < 4; i++) {
                    near(hits[i] / (double) n, prob[i], 0.015, "share of candidate " + i);
                    sum += prob[i];
                }
                near(sum, 1.0, 1e-9, "probabilities sum to one");
                // snapped: the same seed always gives the near one
                double[] snapped = ResonanceMath.ownerProbabilities(new double[] {10, 60, 90}, 3.0, false);
                for (int i = 0; i < 1000; i++) check(ResonanceMath.pick(snapped, random.nextDouble()) == 0, "snap owner");
                // the owner example as a distribution: 75 % / 25 %
                double[] ex = ResonanceMath.ownerProbabilities(new double[] {25, 75}, 3.0, false);
                int a = 0;
                for (int i = 0; i < n; i++) if (ResonanceMath.pick(ex, random.nextDouble()) == 0) a++;
                near(a / (double) n, 0.75, 0.015, "owner example 75 %");
            });
            h.succeed();
        });
        add("newcomer_caps", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                near(ResonanceMath.graceCap(0), 1.0, 1e-9, "grace: level 1 from the start");
                near(ResonanceMath.graceCap(5.9), 1.0, 1e-9, "until six hours");
                near(ResonanceMath.graceCap(9), 3.0, 1e-9, "halfway through the second period");
                near(ResonanceMath.graceCap(12), 5.0, 1e-9, "open after twelve hours");
                ResonanceConfig.graceHours = 0;
                near(ResonanceMath.graceCap(0), 5.0, 1e-9, "grace period off");
                ResonanceConfig.graceHours = 6;
                // a newcomer (P 0.3, 1 h) far out (zone 5): at most his own level + 1 = 1.3
                near(ResonanceMath.playerCap(0.3, 1), 1.0, 1e-9, "grace cap beats level + 1");
                near(ResonanceMath.playerCap(0.3, 100), 1.3, 1e-9, "a veteran newcomer: level + 1");
                near(ResonanceMath.spawnLevel(5.0, 0.06, 1.3, new double[0], 0.0), 1.3, 1e-9, "the owner's cap limits a far spawn");
                near(ResonanceMath.spawnLevel(5.0, 0.9, 4.8, new double[] {1.3}, 0.0), 1.3, 1e-9, "a newcomer next to a pro limits the pro's spawn");
                ResonanceConfig.nearbyCap = false;
                near(ResonanceMath.spawnLevel(5.0, 0.9, 4.8, new double[] {1.3}, 0.0), 4.8, 1e-9, "unless the nearby cap is off");
                ResonanceConfig.nearbyCap = true;
                near(ResonanceMath.spawnLevel(0.5, 0.0, 5.0, new double[0], 0.0), 0.5, 1e-9, "close to the centre");
                near(ResonanceMath.spawnLevel(5.0, 1.0, 5.0, new double[0], 1.0), 5.0, 1e-9, "never above 5");
                near(ResonanceMath.spawnLevel(0.0, 0.0, 5.0, new double[0], -1.0), 0.0, 1e-9, "never below 0");
                near(ResonanceMath.spawnLevel(2.0, 0.5, 5.0, new double[0], 0.0), 3.0, 1e-9, "zone + bmax x W");
                near(ResonanceMath.ownerlessLevel(5.0, 0.0), ResonanceConfig.ownerlessCap, 1e-9, "no owner: the ownerless cap");
                near(ResonanceMath.ownerlessLevel(0.5, 0.0), 0.5, 1e-9, "no owner in a mild zone");
            });
            h.succeed();
        });
        add("spawn_service_levels", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceService.INSTANCE;
                ResonanceConfig.center = "-3000,0";
                ServerPlayer newcomer = mock(h, 2, 2);
                ServerPlayer pro = mock(h, 12, 2);
                svc.data().record(pro.getUUID()).points = 120;
                playHours(pro, 20);
                playHours(newcomer, 0.5);
                BlockPos at = newcomer.blockPosition();
                double zone = svc.zoneLevel(OVERWORLD, at.getX(), at.getZ());
                // the newcomer alone: far out the zone is hard, but he is capped at level 1
                ResonanceService.testPlayers = List.of(newcomer);
                var alone = svc.sample(OVERWORLD, at.getX(), at.getY(), at.getZ(), 0.0, 0.5);
                check(alone.owner().isPresent() && alone.owner().get().equals(newcomer.getUUID()), "the newcomer owns it");
                near(alone.zone(), zone, 1e-9, "zone in the result");
                near(alone.level(), 1.0, 1e-6, "newcomer alone: grace cap 1");
                check(svc.isNewcomer(newcomer.getUUID()) && !svc.isNewcomer(pro.getUUID()), "newcomer flag");
                // the pro alone (at his own place): zone + 2 x W, below his cap
                ResonanceService.testPlayers = List.of(pro);
                BlockPos pat = pro.blockPosition();
                double proZone = svc.zoneLevel(OVERWORLD, pat.getX(), pat.getZ());
                var proAlone = svc.sample(OVERWORLD, pat.getX(), pat.getY(), pat.getZ(), 0.0, 0.5);
                double w = svc.valueOf(pro);
                near(proAlone.level(), Math.min(proZone + ResonanceConfig.bmax * w, ResonanceMath.playerCap(ResonanceMath.level(w), 20)), 1e-6, "pro alone");
                check(proAlone.level() > 2.0, "a pro in a hard zone meets real monsters: " + proAlone.level());
                // both stand within 40 blocks: the newcomer next to the pro limits the pro's spawn
                ResonanceService.testPlayers = List.of(newcomer, pro);
                var both = svc.sample(OVERWORLD, pat.getX(), pat.getY(), pat.getZ(), 0.999, 0.5);
                check(both.owner().isPresent() && both.owner().get().equals(pro.getUUID()), "the pro stands at the place and owns it: " + both.owner());
                near(both.level(), 1.0, 1e-6, "pro's spawn next to a newcomer is capped to his level");
                ResonanceConfig.nearbyCap = false;
                var free = svc.sample(OVERWORLD, pat.getX(), pat.getY(), pat.getZ(), 0.999, 0.5);
                near(free.level(), proAlone.level(), 1e-6, "nearby cap off: the pro's own level");
                ResonanceConfig.nearbyCap = true;
                // nobody near: the ownerless cap
                ResonanceService.testPlayers = List.of();
                var nobody = svc.sample(OVERWORLD, at.getX(), at.getY(), at.getZ(), 0.0, 0.5);
                check(nobody.owner().isEmpty(), "no owner");
                near(nobody.level(), Math.min(zone, ResonanceConfig.ownerlessCap), 1e-6, "ownerless cap");
                // another dimension: players there do not own an Overworld attempt
                ResonanceService.testPlayers = List.of(newcomer);
                check(svc.pickOwner("minecraft:the_nether", at.getX(), at.getY(), at.getZ(), 0.0).isEmpty(), "wrong dimension, no owner");
                check(svc.pickOwner(OVERWORLD, at.getX() + 500, at.getY(), at.getZ(), 0.0).isEmpty(), "out of the owner radius, no owner");
                // noise only moves within +-config noise
                ResonanceService.testPlayers = List.of(pro);
                var low = svc.sample(OVERWORLD, pat.getX(), pat.getY(), pat.getZ(), 0.0, 0.0);
                var high = svc.sample(OVERWORLD, pat.getX(), pat.getY(), pat.getZ(), 0.0, 1.0);
                near(high.level() - low.level(), 2 * ResonanceConfig.noise, 1e-6, "noise span");
            });
            h.succeed();
        });
        add("migration_legacy_block", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                CompoundTag block = new CompoundTag();
                block.putInt("BaseDist", 1500);
                block.putInt("BaseTime", 7200);
                block.putInt("BaseHostile", 10);
                block.putInt("BasePassive", 4);
                block.putInt("BaseDamage", 33);
                block.putInt("BaseXp", 250);
                block.putInt("ActiveTicks", 72000);
                block.putInt("TotalHostile", 30);
                block.putInt("TotalPassive", 9);
                PlayerRecord r = new PlayerRecord();
                LegacyImport.apply(r, block);
                check(r.baseDistance == 1500 && r.baseActive == 7200 && r.baseHostile == 10 && r.basePassive == 4 && r.baseDamage == 33 && r.baseXp == 250,
                        "F baselines taken over");
                check(r.active == 72000 && r.hostile == 30 && r.passive == 9, "totals taken over");
                near(r.points, 2.0, 1e-9, "one hour of active time = 2 starting points");
                check(r.legacyImported, "marked as imported");
                // an old block from before the active counter starts at 0 like the core did
                CompoundTag old = new CompoundTag();
                old.putInt("BaseTime", 99999);
                old.putInt("BaseXp", 5);
                PlayerRecord o = new PlayerRecord();
                LegacyImport.apply(o, old);
                check(o.active == 0 && o.baseActive == 0 && o.baseXp == 5 && o.points == 0.0, "blocks without ActiveTicks");
                // parking by the mixin and taking by the first tick
                UUID id = UUID.randomUUID();
                LegacyImport.park(id, block);
                check(LegacyImport.take(id) == block && LegacyImport.take(id) == null, "parked once, taken once");
                // the F share of the imported baselines equals the core's formula for the same numbers (recomputed here by hand)
                double mean = ResonanceMath.meanFactor(ResonanceMath.xpFactor(1395 + 250 - r.baseXp), ResonanceMath.survivalFactor(4000, 36000),
                        ResonanceMath.combatFactor(100, 0, 0));
                double hand = (1.0 + (0.1 + 0.9 * (1 - Math.exp(-1.0))) / 2 + (0.1 + 0.9 * (1 - Math.exp(-0.5))) / 2 + (0.1 + 0.9 * (1 - Math.exp(-1.0)))) / 3.0;
                near(mean, hand, 1e-9, "core formula reproduced");
                // config takeover: the base multiplier of the SimpleBuilding file
                try {
                    java.nio.file.Path file = java.nio.file.Files.createTempFile("simplebuilding", ".json");
                    java.nio.file.Files.writeString(file, "{\"trimBenefitBaseMultiplier\": 1.5, \"other\": 1}");
                    var legacy = ResonanceConfig.readLegacy(file);
                    check(legacy != null && legacy.get("trimBenefitBaseMultiplier").getAsDouble() == 1.5, "legacy config read");
                    var json = new com.google.gson.JsonObject();
                    json.add("trimBenefitBaseMultiplier", legacy.get("trimBenefitBaseMultiplier"));
                    ResonanceConfig.apply(json);
                    near(ResonanceConfig.trimBenefitBaseMultiplier, 1.5, 1e-9, "base multiplier taken over");
                    java.nio.file.Files.writeString(file, "not json");
                    check(ResonanceConfig.readLegacy(file) == null, "a broken foreign file is ignored, not quarantined");
                    check(java.nio.file.Files.exists(file), "and stays where it is");
                    java.nio.file.Files.delete(file);
                } catch (java.io.IOException e) {
                    throw new AssertionError(e);
                }
            });
            h.succeed();
        });
        add("migration_tick_consumes", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceService.INSTANCE;
                var p = mock(h, 2, 2);
                CompoundTag block = new CompoundTag();
                block.putInt("ActiveTicks", 36000);
                block.putInt("BaseXp", 77);
                block.putInt("TotalHostile", 5);
                LegacyImport.park(p.getUUID(), block);
                check(svc.update(p, 1000), "the first second imports and counts");
                PlayerRecord r = svc.data().record(p.getUUID());
                check(r.legacyImported && r.baseXp == 77 && r.hostile == 5 && r.active >= 36000, "block imported on the first tick");
                double points = r.points;
                near(points, 1.0 + 20.0 / ResonanceMath.TICKS_PER_POINT, 1e-6, "half an active hour = 1 point, plus the second");
                LegacyImport.park(p.getUUID(), block);
                svc.update(p, 1020);
                check(r.baseXp == 77 && LegacyImport.take(p.getUUID()) == block, "imported only once");
                // standing still for longer than the active window: no more active time
                int active = r.active;
                svc.update(p, 1020 + ResonanceMath.ACTIVE_GRACE_TICKS + 40);
                check(r.active == active, "AFK time does not count");
                // moving again
                p.getStats().setValue(p, Stats.CUSTOM.get(Stats.WALK_ONE_CM), 5000);
                svc.update(p, 1020 + ResonanceMath.ACTIVE_GRACE_TICKS + 60);
                check(r.active == active + 20, "movement counts again");
            });
            h.succeed();
        });
        add("contract_values", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceContracts.get();
                var p = mock(h, 2, 2);
                ResonanceService.testPlayers = List.of(p);
                check(svc != null && svc.enabled(), "contract live");
                near(svc.fraction(p.getUUID()), 0.0, 1e-9, "fresh F");
                near(svc.trimMultiplier(p.getUUID()), 0.2, 1e-9, "fresh trim multiplier = 2.0 x 0.1 like the core");
                near(svc.playerValue(p.getUUID()), 0.0, 1e-9, "fresh W");
                near(svc.playerLevel(p.getUUID()), 0.0, 1e-9, "fresh P");
                near(svc.playerValue(UUID.randomUUID()), 0.0, 1e-9, "unknown player");
                ResonanceService.INSTANCE.data().record(p.getUUID()).points = 1e6;
                p.totalExperience = 1395;
                ResonanceService.INSTANCE.forget(p.getUUID());
                check(svc.playerValue(p.getUUID()) > 0.7, "long-term value counts");
                check(svc.trimMultiplier(p.getUUID()) > 0.2 && svc.trimMultiplier(p.getUUID()) <= 2.0, "trim multiplier stays 0.2..2.0");
                check(svc.zoneLevel(OVERWORLD, 0, 0) >= 0.5, "zone level from the contract");
            });
            h.succeed();
        });
        add("command", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var server = h.getLevel().getServer();
                var p = mock(h, 2, 2);
                ResonanceService.testPlayers = List.of(p);
                var source = server.createCommandSourceStack().withEntity(p).withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
                var d = server.getCommands().getDispatcher();
                check(run(d, "resonance", source) == 1, "/resonance");
                check(run(d, "resonance debug", source) >= 1, "/resonance debug lists the owner");
                check(run(d, "resonance debug", source.withPermission(LevelBasedPermissionSet.ALL)) == -1, "debug needs game master");
                ResonanceConfig.enabled = false;
                check(run(d, "resonance", source) == 0, "off: the command says so");
            });
            h.succeed();
        });
        add("performance", h -> {
            guarded(() -> {
                ResonanceConfig.reset();
                var svc = ResonanceService.INSTANCE;
                var a = mock(h, 2, 2);
                var b = mock(h, 10, 4);
                var c = mock(h, 6, 12);
                ResonanceService.testPlayers = List.of(a, b, c);
                BlockPos at = a.blockPosition();
                Random random = new Random(7);
                svc.sample(OVERWORLD, at.getX(), at.getY(), at.getZ(), 0.1, 0.5);
                long start = System.nanoTime();
                for (int i = 0; i < 1000; i++) svc.sample(OVERWORLD, at.getX(), at.getY(), at.getZ(), random.nextDouble(), random.nextDouble());
                double ms = (System.nanoTime() - start) / 1.0e6;
                check(ms < 250.0, "1000 spawn samples took " + ms + " ms");
                long pure = System.nanoTime();
                for (int i = 0; i < 1000; i++) {
                    double[] p = ResonanceMath.ownerProbabilities(new double[] {20 + i % 50, 60, 90}, 3.0, false);
                    ResonanceMath.spawnLevel(ResonanceMath.curve(i * 7), 0.4, 3.0, new double[] {2.0}, ResonanceMath.pick(p, 0.5));
                }
                double pureMs = (System.nanoTime() - pure) / 1.0e6;
                check(pureMs < 50.0, "1000 pure rule runs took " + pureMs + " ms");
            });
            h.succeed();
        });
        add("guide_book", h -> com.simpleresonance.guide.ResonanceGuide.gameTest(h));
    }
}
