package com.simplebuilding.modules.simpleenddragonfight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * Test bodies shared by the Fabric @GameTest wrappers and the NeoForge/Forge registration. The fight is driven with its
 * own {@link FightSession}, a test dragon (no AI, no real fight) and mock players in the test world.
 */
public final class DragonFightTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private DragonFightTests() {}

    private static void add(String name, Consumer<GameTestHelper> body) { ALL.put(name, body); }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 0.01; }

    private static EnderDragon dragon(GameTestHelper h) {
        EnderDragon d = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(d != null, "test dragon");
        BlockPos at = h.absolutePos(new BlockPos(1, 12, 1));
        d.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        d.setNoAi(true);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    /**
     * Runs the mites' dissolve check by hand. The test area sits in freshly loaded chunks that can take longer than the
     * test's tick budget to start ticking entities, so waiting for the natural once-a-second check is load-dependent.
     */
    private static void checkMites(ServerLevel level, BlockPos center) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center).inflate(DragonFightConfig.islandRadius, 128, DragonFightConfig.islandRadius);
        for (Dragonmite m : level.getEntities(DragonFightRegistry.DRAGONMITE, box, x -> true)) m.checkBound(level);
    }

    private static Player player(GameTestHelper h, GameType mode, BlockPos rel) {
        Player p = h.makeMockPlayer(mode);
        BlockPos at = h.absolutePos(rel);
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        return p;
    }

    private static List<Player> players(GameTestHelper h, int n) {
        List<Player> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(player(h, GameType.SURVIVAL, new BlockPos(1 + i % 3, 1, 1 + i / 3)));
        return out;
    }

    static {
        add("registered", h -> {
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(DragonFightRegistry.DRAGONMITE_KEY.identifier()), "dragonmite entity type");
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(DragonFightRegistry.CHITIN_KEY.identifier()), "dragon chitin item");
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(DragonFightRegistry.EGG_KEY.identifier()), "spawn egg item");
            h.assertTrue(DragonFightRegistry.DRAGONMITE.getCategory() == MobCategory.MONSTER, "monster category");
            var attrs = Dragonmite.createAttributes().build();
            h.assertTrue(attrs.getBaseValue(Attributes.MAX_HEALTH) == 14.0 && attrs.getBaseValue(Attributes.ATTACK_DAMAGE) == 3.0
                    && near(attrs.getBaseValue(Attributes.MOVEMENT_SPEED), 0.32) && attrs.getBaseValue(Attributes.ARMOR) == 2.0
                    && near(attrs.getBaseValue(Attributes.KNOCKBACK_RESISTANCE), 0.3), "dragonmite values 14 HP / 3 damage / 0.32 / armor 2 / kb 0.3");
            h.succeed();
        });
        add("scaling_math", h -> {
            h.assertTrue(DragonScaling.targetHealth(true, 200, 1) == 300 && DragonScaling.targetHealth(true, 200, 3) == 900, "300 x players");
            h.assertTrue(DragonScaling.targetHealth(false, 200, 1) == 200 && DragonScaling.targetHealth(false, 200, 4) == 800, "200 x players with the switch off");
            h.assertTrue(DragonScaling.players(0, 0) == 1 && DragonScaling.players(9, 0) == 9 && DragonScaling.players(9, 4) == 4, "at least one, optional cap");
            h.assertTrue(DragonScaling.appliedMax(1200) == 1024 && near(DragonScaling.damageFactor(1200), 1024.0 / 1200) && DragonScaling.damageFactor(900) == 1f, "attribute cap and damage factor");
            h.assertTrue(near(DragonScaling.rescale(150, 300, 900), 450) && near(DragonScaling.rescale(450, 900, 300), 150) && DragonScaling.rescale(0.2f, 900, 300) == 1f, "share kept, never below 1");
            int[] waves = {2, 2, 3, 5}, alive = {9, 10, 12, 16}, n = {1, 2, 4, 8};
            for (int i = 0; i < n.length; i++) {
                h.assertTrue(DragonScaling.mitesPerWave(2, 1, n[i]) == waves[i], "mites per wave for " + n[i]);
                h.assertTrue(DragonScaling.maxAlive(16, n[i]) == alive[i], "alive cap for " + n[i]);
            }
            h.assertTrue(near(DragonScaling.speed(1.2, 0.015, 1.35, 1), 1.2) && near(DragonScaling.speed(1.2, 0.015, 1.35, 8), 1.305) && near(DragonScaling.speed(1.2, 0.015, 1.35, 30), 1.35), "speed table");
            h.assertTrue(DragonScaling.half(100, 200) && !DragonScaling.half(101, 200), "half threshold");
            h.assertTrue(near(DragonScaling.growlPitch(0), 0.7) && near(DragonScaling.growlPitch(2), 0.8) && DragonScaling.growlPitch(20) == 1f, "growl pitch");
            h.succeed();
        });
        add("scale_keeps_share", h -> {
            ServerLevel level = h.getLevel();
            EnderDragon d = dragon(h);
            BlockPos center = h.absolutePos(new BlockPos(1, 1, 1));
            FightSession s = new FightSession();
            List<Player> three = players(h, 3);
            Player creative = player(h, GameType.CREATIVE, new BlockPos(2, 1, 2));
            Player far = h.makeMockPlayer(GameType.SURVIVAL);
            far.snapTo(center.getX() + DragonFightConfig.islandRadius + 5, center.getY(), center.getZ(), 0f, 0f);
            long t = 0;
            s.scan(level, d, center, List.of(three.get(0), creative, far), t);
            h.assertTrue(s.counted() == 1 && near(d.getMaxHealth(), 300) && near(d.getHealth(), 300), "solo: 300, full (creative and far away do not count)");
            d.setHealth(150);
            s.scan(level, d, center, three, t += 20);
            h.assertTrue(s.counted() == 3 && near(d.getMaxHealth(), 900) && near(d.getHealth(), 450), "three players: 900, share kept (got " + d.getHealth() + "/" + d.getMaxHealth() + ")");
            h.assertTrue(s.queued() == 3, "one entry animation per newcomer queued");
            s.scan(level, d, center, three.subList(0, 1), t += 20);
            h.assertTrue(s.counted() == 3 && near(d.getMaxHealth(), 900), "leavers still count during the grace period");
            t += DragonFightConfig.leaveGraceSeconds * 20L + 20;
            s.scan(level, d, center, three.subList(0, 1), t);
            h.assertTrue(s.counted() == 1 && near(d.getMaxHealth(), 300) && near(d.getHealth(), 150), "after the grace period back to 300, share kept");
            List<Player> four = new ArrayList<>(three);
            four.add(player(h, GameType.SURVIVAL, new BlockPos(3, 1, 3)));
            s.scan(level, d, center, four, t += 20);
            h.assertTrue(near(d.getMaxHealth(), 1024) && near(s.targetHealth(), 1200) && near(DragonFightServer.damageFactor(d), 1024.0 / 1200), "1200 effective: 1024 health and a damage factor");
            float before = d.getHealth();
            d.hurtServer(level, level.damageSources().playerAttack(four.get(0)), 10f);
            float lost = before - d.getHealth();
            h.assertTrue(near(lost, 3.5f * 1024f / 1200f), "body hit 3.5 scaled by the factor (lost " + lost + ")");
            FightSession.unscale(d);
            h.assertTrue(near(d.getMaxHealth(), 200) && DragonFightServer.damageFactor(d) == 1f, "unscaled: Vanilla 200, no factor");
            d.discard();
            h.succeed();
        });
        add("raise_base_off", h -> {
            boolean old = DragonFightConfig.raiseBaseHealth;
            EnderDragon d = dragon(h);
            try {
                DragonFightConfig.raiseBaseHealth = false;
                FightSession s = new FightSession();
                s.scale(d, 2);
                h.assertTrue(near(d.getMaxHealth(), 400) && near(d.getHealth(), 400), "switch off: 200 x 2");
            } finally {
                DragonFightConfig.raiseBaseHealth = old;
                d.discard();
            }
            h.succeed();
        });
        add("speed_factor", h -> {
            EnderDragon d = dragon(h);
            d.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
            h.assertTrue(DragonFightServer.speedFactor(d) == 1f, "unmanaged dragon keeps Vanilla speed");
            FightSession s = new FightSession();
            s.scale(d, 1);
            h.assertTrue(near(DragonFightServer.speedFactor(d), 1.2), "managed: x1.2");
            s.scale(d, 8);
            h.assertTrue(near(DragonFightServer.speedFactor(d), 1.305), "eight players: x1.305");
            FightSession.unscale(d);
            d.discard();
            h.succeed();
        });
        add("half_and_waves", h -> {
            ServerLevel level = h.getLevel();
            EnderDragon d = dragon(h);
            BlockPos center = h.absolutePos(new BlockPos(1, 1, 1));
            FightSession s = new FightSession();
            List<Player> one = players(h, 1);
            long t = 1000;
            s.tick(level, d, center, one, t);
            h.assertTrue(!s.halfReached() && FightSession.aliveMites(level, d.getUUID(), center) == 0, "no mites above half");
            d.setHealth(d.getMaxHealth() * 0.5f);
            s.tick(level, d, center, one, t += 20);
            int first = FightSession.aliveMites(level, d.getUUID(), center);
            h.assertTrue(s.halfReached() && first == 2, "first wave at once: 2 mites (got " + first + ")");
            d.setHealth(d.getMaxHealth());
            s.tick(level, d, center, one, t += 20);
            h.assertTrue(s.halfReached() && FightSession.aliveMites(level, d.getUUID(), center) == 2, "half state latches, no early wave");
            s.tick(level, d, center, one, t + DragonFightConfig.waveIntervalSeconds * 20L);
            h.assertTrue(FightSession.aliveMites(level, d.getUUID(), center) == 4, "second wave after the interval");
            d.discard();
            checkMites(level, center);
            h.assertTrue(FightSession.aliveMites(level, d.getUUID(), center) == 0, "mites dissolve when the dragon is gone");
            h.succeed();
        });
        add("mite_cap", h -> {
            ServerLevel level = h.getLevel();
            EnderDragon d = dragon(h);
            BlockPos center = h.absolutePos(new BlockPos(1, 1, 1));
            FightSession s = new FightSession();
            s.scan(level, d, center, players(h, 1), 0);
            for (int i = 0; i < 8; i++) s.spawnWave(level, d, center);
            int alive = FightSession.aliveMites(level, d.getUUID(), center);
            h.assertTrue(alive <= DragonScaling.maxAlive(DragonFightConfig.maxAliveMites, 1), "living cap holds (" + alive + ")");
            d.discard();
            checkMites(level, center);
            h.assertTrue(FightSession.aliveMites(level, d.getUUID(), center) == 0, "cleared");
            h.succeed();
        });
        add("unbound_mite_stays", h -> {
            Dragonmite bound = h.spawn(DragonFightRegistry.DRAGONMITE, new BlockPos(1, 1, 1));
            bound.bindTo(UUID.randomUUID());
            Dragonmite free = h.spawn(DragonFightRegistry.DRAGONMITE, new BlockPos(3, 1, 3));
            free.setNoAi(true);
            bound.checkBound(h.getLevel());
            free.checkBound(h.getLevel());
            h.assertTrue(bound.isRemoved(), "a mite without its dragon dissolves");
            h.assertTrue(!free.isRemoved(), "a spawn-egg mite stays");
            free.discard();
            h.succeed();
        });
        add("entry_queue", h -> {
            int oldMax = DragonFightConfig.maxQueued;
            ServerLevel level = h.getLevel();
            EnderDragon d = dragon(h);
            BlockPos center = h.absolutePos(new BlockPos(1, 1, 1));
            try {
                DragonFightConfig.maxQueued = 2;
                FightSession s = new FightSession();
                s.tick(level, d, center, List.of(), 0);
                List<Player> ps = players(h, 3);
                for (Player p : ps) s.enqueue(new FightSession.Entry(p, p.getName().getString()));
                h.assertTrue(s.queued() == 2, "overflow drops the oldest");
                long t = 1;
                s.tick(level, d, center, List.of(), t++);
                h.assertTrue(s.current() != null && s.current().player() == ps.get(1), "FIFO: the second one runs first after the drop");
                for (int i = 1; i < DragonFightConfig.animationTicks; i++) s.tick(level, d, center, List.of(), t++);
                h.assertTrue(s.current() == null && s.queued() == 1, "one animation lasts animationTicks");
                for (int i = 0; i < FightSession.PAUSE_TICKS; i++) s.tick(level, d, center, List.of(), t++);
                h.assertTrue(s.current() == null, "pause between animations");
                s.tick(level, d, center, List.of(), t);
                h.assertTrue(s.current() != null && s.current().player() == ps.get(2), "next one after the pause");
            } finally {
                DragonFightConfig.maxQueued = oldMax;
                d.discard();
            }
            h.succeed();
        });
        add("switch_off_is_vanilla", h -> {
            ServerLevel level = h.getLevel();
            EnderDragon d = dragon(h);
            BlockPos center = h.absolutePos(new BlockPos(1, 1, 1));
            FightSession s = new FightSession();
            List<Player> two = players(h, 2);
            s.tick(level, d, center, two, 0);
            h.assertTrue(near(d.getMaxHealth(), 600), "on: 600 for two");
            boolean old = com.simplebuilding.framework.api.SubMods.enabled(DragonFightRegistry.ID);
            try {
                com.simplebuilding.framework.api.SubMods.set(DragonFightRegistry.ID, false);
                h.assertTrue(!DragonFightRegistry.on(), "super-mod switch reaches the module");
                d.setHealth(d.getMaxHealth() * 0.5f);
                s.tick(level, d, center, two, 20);
                h.assertTrue(near(d.getMaxHealth(), 200) && near(d.getHealth(), 100) && DragonFightServer.speedFactor(d) == 1f, "off: Vanilla 200, share kept, Vanilla speed");
                h.assertTrue(FightSession.aliveMites(level, d.getUUID(), center) == 0 && !s.halfReached(), "off: no Dragonmites");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(DragonFightRegistry.ID, old);
                d.discard();
            }
            h.succeed();
        });
        add("summon_counter", h -> {
            DragonSummons data = new DragonSummons();
            UUID first = UUID.randomUUID(), second = UUID.randomUUID(), third = UUID.randomUUID();
            h.assertTrue(data.record(first, false) == 0, "the first dragon is no further summoning");
            h.assertTrue(data.record(first, false) == 0, "the same dragon again (restart, reload) counts once");
            h.assertTrue(data.record(second, true) == 1 && data.record(third, true) == 2, "every respawned dragon adds one");
            h.assertTrue(new DragonSummons().record(first, true) == 1, "a world that killed a dragon before the module counts the next one");
            var round = DragonSummons.CODEC.encodeStart(com.mojang.serialization.JavaOps.INSTANCE, data).getOrThrow();
            h.assertTrue(DragonSummons.CODEC.parse(com.mojang.serialization.JavaOps.INSTANCE, round).getOrThrow().record(third, true) == 2, "count and last dragon are saved");
            h.assertTrue(near(DragonScaling.summonFactor(0.15, 3.0, 0), 1.0) && near(DragonScaling.summonFactor(0.15, 3.0, 2), 1.3)
                    && near(DragonScaling.summonFactor(0.15, 3.0, 40), 3.0) && near(DragonScaling.summonFactor(0.0, 3.0, 5), 1.0), "+15 % per summoning, capped");
            h.succeed();
        });
        add("summon_scaling", h -> {
            ServerLevel level = h.getLevel();
            EnderDragon d = dragon(h);
            BlockPos center = h.absolutePos(new BlockPos(1, 1, 1));
            FightSession s = new FightSession();
            s.setSummons(2);
            d.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
            List<Player> two = players(h, 2);
            s.scan(level, d, center, two, 0);
            h.assertTrue(near(d.getMaxHealth(), 600 * 1.3) && near(d.getHealth(), 780), "2 players x 2 summonings: 600 x 1.3 = 780 (got " + d.getMaxHealth() + ")");
            h.assertTrue(near(DragonFightServer.speedFactor(d), 1.215 * 1.3) && near(DragonFightServer.attackFactor(d), 1.3), "speed and attack x1.3");
            Player strong = two.get(0), plain = two.get(1);
            float a = strong.getHealth();
            strong.hurtServer(level, level.damageSources().mobAttack(d), 4f);
            float lostStrong = a - strong.getHealth();
            s.setSummons(0);
            s.scan(level, d, center, two, 20);
            h.assertTrue(near(d.getMaxHealth(), 600) && near(DragonFightServer.attackFactor(d), 1.0), "no summonings: plain player scaling");
            float b = plain.getHealth();
            plain.hurtServer(level, level.damageSources().mobAttack(d), 4f);
            float lostPlain = b - plain.getHealth();
            h.assertTrue(lostPlain > 0 && near(lostStrong, lostPlain * 1.3f), "dragon hits 30 % harder (" + lostStrong + " vs " + lostPlain + ")");
            FightSession.unscale(d);
            d.discard();
            h.succeed();
        });
        add("no_simplebuilding_needed", h -> {
            // Principle 8: the module runs alone; nothing here may depend on SimpleBuilding being loaded.
            h.assertTrue(DragonFightRegistry.on(), "on by default without a super-mod");
            h.assertTrue(BuiltInRegistries.ITEM.getKey(DragonFightRegistry.DRAGON_CHITIN).getNamespace().equals(DragonFightRegistry.ID), "own namespace");
            h.succeed();
        });
    }
}
