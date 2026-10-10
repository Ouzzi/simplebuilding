package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.DeceiverLogic.Phase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

/** Test bodies shared by the Fabric @GameTest wrappers and the NeoForge test registration. */
public final class MobsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    private static void add(String name, Consumer<GameTestHelper> body) { ALL.put(name, body); }

    static {
        add("registered", h -> {
            check(BuiltInRegistries.ENTITY_TYPE.containsKey(MobsRegistry.DECEIVER_KEY.identifier()), "deceiver entity type");
            check(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplemobs", "deceiver_cloth")), "cloth item");
            check(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplemobs", "deceiver_spawn_egg")), "spawn egg");
            check(DeceiverEntity.createAttributes().build().getBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) == 100.0, "100 health");
            h.succeed();
        });
        add("phases", h -> {
            check(DeceiverLogic.phase(100, 100) == Phase.ONE, "full health is phase one");
            check(DeceiverLogic.phase(67, 100) == Phase.ONE, "67 percent still phase one");
            check(DeceiverLogic.phase(66, 100) == Phase.TWO, "66 percent is phase two");
            check(DeceiverLogic.phase(34, 100) == Phase.TWO, "34 percent still phase two");
            check(DeceiverLogic.phase(33, 100) == Phase.THREE, "33 percent is phase three");
            check(DeceiverLogic.phase(120, 120) == Phase.ONE, "armored max health");
            check(DeceiverLogic.cycleTicks(Phase.THREE) < DeceiverLogic.cycleTicks(Phase.ONE), "faster cycles when weak");
            check(DeceiverLogic.realChance(Phase.ONE) == 0.15f && DeceiverLogic.realChance(Phase.THREE) == 0.25f, "real chance 15..25");
            h.succeed();
        });
        add("wave_sizes", h -> {
            RandomSource rnd = RandomSource.create(7);
            for (Phase p : Phase.values()) {
                for (int i = 0; i < 200; i++) {
                    int n = DeceiverLogic.waveSize(p, rnd);
                    check(n >= DeceiverLogic.waveMin(p) && n <= DeceiverLogic.waveMax(p), "wave size in range " + p);
                }
            }
            check(DeceiverLogic.waveMin(Phase.ONE) == 5 && DeceiverLogic.waveMax(Phase.THREE) == 16, "owner ranges 5..16");
            check(DeceiverLogic.allowed(12, 20, 24) == 4, "cap leaves room for 4");
            check(DeceiverLogic.allowed(12, 30, 24) == 0, "over cap spawns nothing");
            check(DeceiverLogic.allowed(12, 0, 99) == 12, "config cannot exceed the hard cap");
            check(DeceiverLogic.allowed(30, 0, 99) == DeceiverLogic.HARD_CAP, "hard cap");
            h.succeed();
        });
        add("wave_kinds", h -> {
            RandomSource rnd = RandomSource.create(3);
            for (Phase p : Phase.values()) {
                for (int i = 0; i < 100; i++) {
                    List<String> k = DeceiverLogic.pickKinds(p, false, rnd);
                    check(k.size() >= 2 && k.size() <= 3 && k.stream().distinct().count() == k.size(), "2-3 distinct kinds");
                    for (String s : k) check(DeceiverLogic.fakeSafe(s), "wave kinds are melee " + s);
                    for (String s : DeceiverLogic.pickKinds(p, true, rnd)) check(List.of("pig", "sheep", "cow", "chicken").contains(s), "peaceful wave " + s);
                }
            }
            check(DeceiverLogic.pickSpecial(Phase.ONE, rnd) == null, "no specials in phase one");
            check(DeceiverLogic.pickSpecial(Phase.THREE, rnd) != null, "specials in phase three");
            check(!DeceiverLogic.fakeSafe("skeleton") && !DeceiverLogic.fakeSafe("evoker"), "ranged kinds cannot be fakes");
            h.succeed();
        });
        add("provocation", h -> {
            int s = 0;
            for (int i = 0; i < DeceiverLogic.STARE_TICKS - 1; i++) s = DeceiverLogic.stare(s, true);
            check(!DeceiverLogic.provoked(s, 0, false), "not yet");
            s = DeceiverLogic.stare(s, true);
            check(DeceiverLogic.provoked(s, 0, false), "3 s of staring provokes");
            check(DeceiverLogic.stare(10, false) == 8, "stare decays");
            check(DeceiverLogic.provoked(0, 3, false), "three shoves provoke");
            check(!DeceiverLogic.provoked(0, 2, false), "two shoves do not");
            check(DeceiverLogic.provoked(0, 0, true), "a hit provokes");
            h.succeed();
        });
        add("pulse_and_heal", h -> {
            check(DeceiverLogic.pulseMirage(Phase.ONE, 0) && !DeceiverLogic.pulseReverse(Phase.ONE, 0), "first pulse mirage");
            check(!DeceiverLogic.pulseMirage(Phase.ONE, 1) && DeceiverLogic.pulseReverse(Phase.ONE, 1), "second pulse reverse");
            check(DeceiverLogic.pulseMirage(Phase.THREE, 0) && DeceiverLogic.pulseReverse(Phase.THREE, 0), "phase three both");
            check(!DeceiverLogic.mayHeal(Phase.ONE, 10, 100, 0), "no heal in phase one");
            check(DeceiverLogic.mayHeal(Phase.TWO, 50, 100, 0), "heal when weak");
            check(!DeceiverLogic.mayHeal(Phase.TWO, 50, 100, 5), "heal cooldown");
            h.succeed();
        });
        add("spawn_rules", h -> {
            check(DeceiverLogic.mayNaturallySpawn(true, true, false, false, 3, 0, 0.001f), "dark forest night");
            check(DeceiverLogic.mayNaturallySpawn(true, false, true, false, 3, 0, 0.015f), "outpost is likelier");
            check(!DeceiverLogic.mayNaturallySpawn(true, true, false, false, 3, 0, 0.015f), "dark forest is rarer");
            check(!DeceiverLogic.mayNaturallySpawn(false, true, true, true, 3, 0, 0.0f), "never by day");
            check(!DeceiverLogic.mayNaturallySpawn(true, true, true, true, 12, 0, 0.0f), "never in light");
            check(!DeceiverLogic.mayNaturallySpawn(true, true, true, true, 3, 1, 0.0f), "single Deceiver");
            check(!DeceiverLogic.mayNaturallySpawn(true, false, false, false, 3, 0, 0.0f), "nowhere else");
            check(DeceiverLogic.mayNaturallySpawn(true, false, false, true, 3, 0, 0.004f), "villages too, rarely");
            h.succeed();
        });
        add("fight", h -> {
            var level = h.getLevel();
            for (int x = -10; x < 35; x++) for (int z = -10; z < 35; z++) level.setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
            var deceiver = h.spawn(MobsRegistry.DECEIVER, 12, 1, 12);
            var player = h.makeMockServerPlayerInLevel();
            var at = h.absolutePos(new BlockPos(12, 1, 12));
            player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            Entity probe = deceiver;
            check(!deceiver.isHostileMode(), "peaceful at first");
            deceiver.hurtServer(level, level.damageSources().playerAttack(player), 1.0f);
            check(deceiver.isHostileMode(), "a hit makes it hostile");
            int n = deceiver.summonNow(level, player, false);
            check(n > 0 && n <= DeceiverLogic.HARD_CAP, "wave summoned within the cap: " + n);
            long fakes = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, probe.getBoundingBox().inflate(40)).stream()
                    .filter(m -> m.entityTags().contains(DeceiverEntity.TAG_FAKE)).count();
            check(fakes <= n, "fakes are summons");
            for (var m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, probe.getBoundingBox().inflate(40))) {
                if (m.entityTags().contains(DeceiverEntity.TAG_FAKE)) {
                    check(m.getHealth() <= 1.0f, "a fake dies to the first hit");
                    check(m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == null
                            || m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 0.0, "a fake deals no damage");
                    check(m.isSilent(), "a fake makes no step sounds");
                }
            }
            h.succeed();
        });
    }

    private MobsTests() {}
}
