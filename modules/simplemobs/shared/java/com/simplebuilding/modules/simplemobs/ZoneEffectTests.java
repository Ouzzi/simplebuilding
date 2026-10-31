package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.ResonanceContracts;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * WP-22 danger zones in Simple Mobs: the pure effect rules (chances, bell-shaped levels, caps), the spawn hook on real monsters with a
 * fake Resonance service (so the tests also run without Simple Resonance), the switches, and the boss points.
 */
final class ZoneEffectTests {
    private ZoneEffectTests() {}

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, double eps, String what) {
        check(Math.abs(actual - expected) <= eps, what + ": expected " + expected + " got " + actual);
    }

    /** A Resonance service that gives every spawn the fixed level and records the awards. */
    private static final class Fixed implements ResonanceContracts.Service {
        final double level;
        final java.util.List<String> awards = new java.util.ArrayList<>();
        boolean on = true;

        Fixed(double level) {
            this.level = level;
        }

        @Override public boolean enabled() { return on; }
        @Override public double playerValue(UUID player) { return 0; }
        @Override public double playerLevel(UUID player) { return 0; }
        @Override public double fraction(UUID player) { return 0; }
        @Override public double trimMultiplier(UUID player) { return 0.2; }
        @Override public double zoneLevel(String dimension, double x, double z) { return level; }
        @Override public Optional<UUID> pickOwner(String dimension, double x, double y, double z, double roll) { return Optional.empty(); }
        @Override public ResonanceContracts.Spawn sample(String dimension, double x, double y, double z, double ownerRoll, double noiseRoll) {
            return new ResonanceContracts.Spawn(Optional.empty(), level, level);
        }
        @Override public void award(UUID player, String source, double points) { awards.add(source + ":" + points); }
        @Override public boolean isNewcomer(UUID player) { return false; }
    }

    /** Runs {@code body} with the service, the switch and the rolls set, and puts everything back. */
    private static void with(ResonanceContracts.Service service, boolean zones, java.util.function.DoubleSupplier rng, Runnable body) {
        ResonanceContracts.Service oldService = ResonanceContracts.get();
        var snapshot = MobsConfig.toJson();
        try {
            ResonanceContracts.set(service);
            var json = MobsConfig.toJson();
            json.addProperty("zoneMobsEnabled", zones);
            MobsConfig.apply(json);
            ZoneSpawns.testRng = rng;
            body.run();
        } finally {
            ZoneSpawns.testRng = null;
            MobsConfig.apply(snapshot);
            ResonanceContracts.set(oldService);
        }
    }

    private static Mob zombie(GameTestHelper h) {
        var z = h.spawn(EntityTypes.ZOMBIE, 2, 2, 2);
        z.setNoAi(true);
        // Fresh effects only: Vanilla's spawn code of the test helper may already have run the hook.
        z.removeAllEffects();
        return z;
    }

    private static void finalize(GameTestHelper h, Mob mob, EntitySpawnReason reason) {
        mob.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(mob.blockPosition()), reason, null);
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("zone_effect_rules", h -> {
            near(ZoneEffects.chance(0.35, 0.0), 0.0, 1e-12, "no chance at level 0");
            near(ZoneEffects.chance(0.35, 5.0), 0.35, 1e-12, "full chance at level 5");
            near(ZoneEffects.chance(0.35, 2.5), 0.35 * Math.pow(0.5, 1.3), 1e-12, "the curve (S/5)^1.3");
            near(ZoneEffects.chance(0.35, 9.0), 0.35, 1e-12, "level clamped to 5");
            near(ZoneEffects.chance(2.0, 5.0), 1.0, 1e-12, "a chance is at most 1");
            // concept table 1.5: expected number of effects over all pools at S = 1, 2, 3, 4, 5 -> 0.22, 0.55, 0.93, 1.35, 1.80
            double[] expected = {0.22, 0.55, 0.93, 1.35, 1.80};
            for (int s = 1; s <= 5; s++) {
                double sum = 0;
                for (ZoneEffects.Pool p : ZoneEffects.Pool.values()) sum += ZoneEffects.chance(p.defaultChance, s);
                near(sum, expected[s - 1], 0.02, "expected effects at level " + s);
            }
            // level distribution: S = 1 -> I 61 % / II 33 % / III 5 %; S = 5 -> V 61 % / IV 33 % / III 5 %
            int[] low = new int[6], high = new int[6];
            int n = 10000;
            for (int i = 0; i < n; i++) {
                low[ZoneEffects.pickLevel(1.0, 5, (i + 0.5) / n)]++;
                high[ZoneEffects.pickLevel(5.0, 5, (i + 0.5) / n)]++;
            }
            near(low[1] / (double) n, 0.61, 0.02, "level I at S=1");
            near(low[2] / (double) n, 0.33, 0.02, "level II at S=1");
            near(low[3] / (double) n, 0.05, 0.02, "level III at S=1");
            near(high[5] / (double) n, 0.61, 0.02, "level V at S=5");
            near(high[4] / (double) n, 0.33, 0.02, "level IV at S=5");
            // caps
            for (int i = 0; i < 200; i++) check(ZoneEffects.pickLevel(5.0, 2, i / 200.0) <= 2, "cap 2");
            check(ZoneEffects.pickLevel(5.0, 1, 0.99) == 1 && ZoneEffects.pickLevel(0.0, 5, 0.0) == 1, "level I is the floor");
            check(ZoneEffects.pickLevel(3.0, 99, 0.999999) <= 5, "never above V");
            h.succeed();
        });
        add.accept("zone_effect_roll", h -> {
            java.util.function.ToDoubleFunction<ZoneEffects.Pool> chance = p -> p.defaultChance;
            java.util.function.ToIntFunction<ZoneEffects.Pool> cap = p -> p.defaultCap;
            check(ZoneEffects.roll(0.0, 3, chance, cap, () -> 0.0).isEmpty(), "level 0 draws nothing");
            check(ZoneEffects.roll(5.0, 0, chance, cap, () -> 0.0).isEmpty(), "maxEffects 0 draws nothing");
            check(ZoneEffects.roll(5.0, 3, chance, cap, () -> 0.999999).isEmpty(), "bad luck draws nothing");
            check(ZoneEffects.roll(5.0, 3, chance, cap, () -> 0.0).size() == 3, "at most maxEffects");
            check(ZoneEffects.roll(5.0, 8, chance, cap, () -> 0.0).size() == 8, "all pools with a lucky roll");
            check(ZoneEffects.roll(5.0, 8, chance, p -> 0, () -> 0.0).isEmpty(), "level cap 0 switches a pool off");
            var one = ZoneEffects.roll(5.0, 8, chance, p -> p == ZoneEffects.Pool.OOZING ? 1 : 0, () -> 0.0);
            check(one.size() == 1 && one.get(0).pool() == ZoneEffects.Pool.OOZING && one.get(0).level() == 1, "only the pool with a cap");
            // seeded: the mean number of effects and P(at least one) at S = 3 follow the table
            Random random = new Random(20261030L);
            double meanEffects = 0, atLeastOne = 0;
            int n = 20000;
            for (int i = 0; i < n; i++) {
                int count = ZoneEffects.roll(3.0, 8, chance, cap, random::nextDouble).size();
                meanEffects += count;
                if (count > 0) atLeastOne++;
            }
            double none = 1;
            for (ZoneEffects.Pool p : ZoneEffects.Pool.values()) none *= 1.0 - ZoneEffects.chance(p.defaultChance, 3.0);
            near(meanEffects / n, 0.93, 0.04, "mean effects at S=3");
            near(atLeastOne / n, 1.0 - none, 0.015, "P(at least one) at S=3");
            // never a pool twice, levels within caps
            for (int i = 0; i < 500; i++) {
                var rolled = ZoneEffects.roll(5.0, 8, chance, cap, random::nextDouble);
                check(rolled.stream().map(ZoneEffects.Rolled::pool).distinct().count() == rolled.size(), "no pool twice");
                for (var r : rolled) check(r.level() >= 1 && r.level() <= r.pool().defaultCap, "level within the cap of " + r.pool());
            }
            h.succeed();
        });
        add.accept("zone_spawn_hook", h -> {
            with(new Fixed(5.0), true, () -> 0.0, () -> {
                Mob mob = zombie(h);
                float health = mob.getMaxHealth();
                finalize(h, mob, EntitySpawnReason.NATURAL);
                check(mob.getActiveEffects().size() == MobsConfig.zoneMaxEffects, "a natural spawn at level 5 gets " + MobsConfig.zoneMaxEffects + " effects, has " + mob.getActiveEffects());
                for (var e : mob.getActiveEffects()) check(e.isInfiniteDuration(), "infinite: " + e);
                near(mob.getMaxHealth(), health, 1e-6, "no extra health from the zone itself");
                // reasons that are not natural stay untouched
                for (EntitySpawnReason reason : new EntitySpawnReason[] {EntitySpawnReason.SPAWNER, EntitySpawnReason.SPAWN_ITEM_USE, EntitySpawnReason.COMMAND,
                        EntitySpawnReason.MOB_SUMMONED, EntitySpawnReason.REINFORCEMENT, EntitySpawnReason.BREEDING, EntitySpawnReason.EVENT}) {
                    Mob other = zombie(h);
                    finalize(h, other, reason);
                    check(other.getActiveEffects().isEmpty(), reason + " must not get zone effects");
                }
                // patrols and chunk generation do
                Mob patrol = zombie(h);
                finalize(h, patrol, EntitySpawnReason.PATROL);
                check(!patrol.getActiveEffects().isEmpty(), "patrol spawns count");
                // named monsters, animals and bosses do not
                Mob named = zombie(h);
                named.setCustomName(net.minecraft.network.chat.Component.literal("Bob"));
                finalize(h, named, EntitySpawnReason.NATURAL);
                check(named.getActiveEffects().isEmpty(), "a named monster keeps to itself");
                var cow = h.spawn(EntityTypes.COW, 4, 2, 4);
                cow.setNoAi(true);
                finalize(h, cow, EntitySpawnReason.NATURAL);
                check(cow.getActiveEffects().isEmpty(), "animals get no zone effects");
                var guardian = h.spawn(EntityTypes.ELDER_GUARDIAN, 6, 2, 6);
                guardian.setNoAi(true);
                guardian.removeAllEffects();
                finalize(h, guardian, EntitySpawnReason.NATURAL);
                check(guardian.getActiveEffects().isEmpty(), "bosses keep their own values");
                guardian.discard();
                // a zero-level spawn stays a Vanilla spawn
                with(new Fixed(0.0), true, () -> 0.0, () -> {
                    Mob calm = zombie(h);
                    finalize(h, calm, EntitySpawnReason.NATURAL);
                    check(calm.getActiveEffects().isEmpty(), "level 0: no effects");
                });
            });
            h.succeed();
        });
        add.accept("zone_switches", h -> {
            // default off until the client check (owner question Q1)
            var defaults = new com.google.gson.JsonObject();
            var snapshot = MobsConfig.toJson();
            try {
                MobsConfig.apply(defaults);
                check(!MobsConfig.zoneMobsEnabled, "zone effects are off by default");
                check(MobsConfig.zoneMaxEffects == 3 && MobsConfig.zoneSpawnReasons.contains("natural"), "defaults");
                for (ZoneEffects.Pool p : ZoneEffects.Pool.values()) {
                    near(MobsConfig.zoneChance[p.ordinal()], p.defaultChance, 1e-12, "default chance " + p.id);
                    check(MobsConfig.zoneLevelCap[p.ordinal()] == p.defaultCap, "default cap " + p.id);
                }
                var wild = new com.google.gson.JsonObject();
                wild.addProperty("zoneMaxEffects", 99);
                var pools = new com.google.gson.JsonObject();
                var strength = new com.google.gson.JsonObject();
                strength.addProperty("chance", 7.0);
                strength.addProperty("levelCap", 99);
                pools.add("strength", strength);
                wild.add("zoneEffects", pools);
                MobsConfig.apply(wild);
                check(MobsConfig.zoneMaxEffects == 8, "maxEffects clamped to 8");
                near(MobsConfig.zoneChance[ZoneEffects.Pool.STRENGTH.ordinal()], 1.0, 1e-12, "chance clamped to 1");
                check(MobsConfig.zoneLevelCap[ZoneEffects.Pool.STRENGTH.ordinal()] == 5, "cap clamped to 5");
                // round trip through the JSON the config command uses
                var json = MobsConfig.toJson();
                check(json.has("zoneEffects") && json.getAsJsonObject("zoneEffects").has("mirror") && json.has("zoneSpawnReasons"), "written complete");
                MobsConfig.apply(json);
                check(MobsConfig.zoneMaxEffects == 8, "round trip");
            } finally {
                MobsConfig.apply(snapshot);
            }
            // switched off, no service, or a service that is off: no effects
            with(new Fixed(5.0), false, () -> 0.0, () -> {
                Mob mob = zombie(h);
                finalize(h, mob, EntitySpawnReason.NATURAL);
                check(mob.getActiveEffects().isEmpty(), "zoneMobsEnabled off");
            });
            with(null, true, () -> 0.0, () -> {
                Mob mob = zombie(h);
                finalize(h, mob, EntitySpawnReason.NATURAL);
                check(mob.getActiveEffects().isEmpty(), "without Simple Resonance nothing happens");
            });
            Fixed off = new Fixed(5.0);
            off.on = false;
            with(off, true, () -> 0.0, () -> {
                Mob mob = zombie(h);
                finalize(h, mob, EntitySpawnReason.NATURAL);
                check(mob.getActiveEffects().isEmpty(), "Resonance switched off");
            });
            boolean old = com.simplebuilding.framework.api.SubMods.enabled("simplemobs");
            try {
                com.simplebuilding.framework.api.SubMods.set("simplemobs", false);
                with(new Fixed(5.0), true, () -> 0.0, () -> {
                    Mob mob = zombie(h);
                    finalize(h, mob, EntitySpawnReason.NATURAL);
                    check(mob.getActiveEffects().isEmpty(), "Simple Mobs switched off");
                });
            } finally {
                com.simplebuilding.framework.api.SubMods.set("simplemobs", old);
            }
            h.succeed();
        });
        add.accept("zone_effect_kinds", h -> {
            // every pool puts a real effect with its amplifier on the mob
            for (ZoneEffects.Pool pool : ZoneEffects.Pool.values()) {
                Mob mob = zombie(h);
                ZoneEffects.apply(mob, java.util.List.of(new ZoneEffects.Rolled(pool, Math.min(2, pool.defaultCap))));
                var effect = mob.getEffect(pool.effect());
                check(effect != null && effect.getAmplifier() == Math.min(2, pool.defaultCap) - 1, "pool " + pool.id + " applies its effect");
                mob.discard();
            }
            check(ZoneEffects.Pool.STRENGTH.effect() == MobEffects.STRENGTH && ZoneEffects.Pool.EXTRA_HEARTS.effect() == MobEffects.ABSORPTION, "Vanilla pools use Vanilla effects");
            check(ZoneEffects.Pool.DEFLECTION.effect().equals(CombatEffects.deflection()), "Simple Mobs pools use the combat effects");
            h.succeed();
        });
        add.accept("zone_boss_award", h -> {
            Fixed service = new Fixed(1.0);
            with(service, false, () -> 0.0, () -> {
                var player = h.makeMockServerPlayerInLevel();
                h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
                var howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 3, 2, 3);
                howler.setNoAi(true);
                var source = h.getLevel().damageSources().playerAttack(player);
                ZoneSpawns.onDeath(howler, source);
                check(service.awards.size() == 1 && service.awards.get(0).startsWith("simplemobs:hollow_howler:5.0"), "a Howler kill gives 5 points: " + service.awards);
                var zombie = zombie(h);
                ZoneSpawns.onDeath(zombie, source);
                check(service.awards.size() == 1, "an ordinary monster gives none");
                ZoneSpawns.onDeath(howler, h.getLevel().damageSources().fall());
                check(service.awards.size() == 1, "no award without a player killer");
                howler.discard();
            });
            h.succeed();
        });
    }
}
