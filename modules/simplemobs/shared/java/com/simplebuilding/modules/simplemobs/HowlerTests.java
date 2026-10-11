package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/** Hollow Howler GameTests (shared by Fabric wrappers, NeoForge and Forge registration). Standalone-safe. */
final class HowlerTests {
    private static final BlockPos CENTER = new BlockPos(3, 2, 3);

    private HowlerTests() {}

    /** Builds the figure with its body center at {@code rel}; returns the absolute center. */
    static BlockPos figure(GameTestHelper h, BlockPos rel, Direction.Axis axis, Block jaw) {
        ServerLevel l = h.getLevel();
        BlockPos c = h.absolutePos(rel);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) l.setBlock(c.offset(dx, -2, dz), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(c.below(), Blocks.SCULK.defaultBlockState(), 3);
        l.setBlock(c, Blocks.SCULK_CATALYST.defaultBlockState(), 3);
        Direction pos = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        l.setBlock(c.relative(pos), Blocks.SCULK_CATALYST.defaultBlockState(), 3);
        l.setBlock(c.relative(pos.getOpposite()), Blocks.SCULK_CATALYST.defaultBlockState(), 3);
        l.setBlock(c.above(), jaw.defaultBlockState(), 3);
        l.setBlock(c.above(2), Blocks.SKELETON_SKULL.defaultBlockState(), 3);
        return c;
    }

    private static BlockPos sensor(GameTestHelper h, BlockPos center) {
        BlockPos s = center.offset(0, -1, -2);
        h.getLevel().setBlock(s, Blocks.SCULK_SENSOR.defaultBlockState(), 3);
        return s;
    }

    private static boolean partsPresent(ServerLevel l, BlockPos center) { return HowlerPattern.at(l, center).isPresent(); }

    private static int rituals(ServerLevel l, BlockPos center) {
        return l.getEntitiesOfClass(HowlerRitualEntity.class, new AABB(center).inflate(2)).size();
    }

    private static HowlerRitualEntity ritual(ServerLevel l, BlockPos center) {
        return l.getEntitiesOfClass(HowlerRitualEntity.class, new AABB(center).inflate(2)).getFirst();
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("howler_registered", h -> {
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(MobsRegistry.HOWLER_KEY.identifier()), "howler entity type");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(MobsRegistry.RITUAL_KEY.identifier()), "manifestation entity type");
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(MobsRegistry.ECHO_CORE_KEY.identifier()), "echo core");
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(MobsRegistry.HOWLER_EGG_KEY.identifier()), "spawn egg");
            h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(MobsRegistry.SILENCE_KEY.identifier()), "silence effect");
            h.assertTrue(BuiltInRegistries.POTION.containsKey(MobsRegistry.SILENCE_POTION_KEY.identifier())
                    && BuiltInRegistries.POTION.containsKey(MobsRegistry.LONG_SILENCE_POTION_KEY.identifier()), "silence potions");
            var attrs = HowlerEntity.createAttributes().build();
            h.assertTrue(attrs.getBaseValue(Attributes.MAX_HEALTH) == 240.0, "240 health");
            h.assertTrue(attrs.getBaseValue(Attributes.ATTACK_DAMAGE) == 14.0 && attrs.getBaseValue(Attributes.ARMOR) == 6.0, "bite 14, armor 6");
            h.succeed();
        });
        add.accept("howler_rules", h -> {
            h.assertTrue(HowlerLogic.phase(240, 240) == HowlerLogic.Phase.ONE && HowlerLogic.phase(121, 240) == HowlerLogic.Phase.ONE, "phase one above 50 %");
            h.assertTrue(HowlerLogic.phase(120, 240) == HowlerLogic.Phase.HUNGER, "hunger at 50 %");
            h.assertTrue(HowlerLogic.phase(48, 240) == HowlerLogic.Phase.RAGE, "rage at 20 %");
            h.assertTrue(HowlerLogic.shriekCooldown(HowlerLogic.Phase.RAGE) * 2 == HowlerLogic.shriekCooldown(HowlerLogic.Phase.ONE), "rage shrieks twice as often");
            h.assertTrue(HowlerLogic.knockbackResistance(HowlerLogic.Phase.RAGE) == 1.0, "rage ignores knockback");
            h.assertTrue(HowlerLogic.damageTaken(10f, true) == 15f && HowlerLogic.damageTaken(10f, false) == 10f, "open jaws take x1.5");
            h.assertTrue(HowlerLogic.darkEnough(7, 7) && !HowlerLogic.darkEnough(8, 7), "light 7 ok, 8 too bright");
            h.assertTrue(HowlerLogic.sensorInRange(5, -5, 5) && !HowlerLogic.sensorInRange(6, 0, 0), "sensor within 5 blocks");
            h.assertTrue(HowlerLogic.MANIFEST_TICKS == 100, "5 s manifestation");
            h.succeed();
        });
        add.accept("howler_pattern", h -> {
            ServerLevel l = h.getLevel();
            BlockPos c = figure(h, CENTER, Direction.Axis.X, Blocks.SCULK_SHRIEKER);
            h.assertTrue(HowlerPattern.at(l, c).map(m -> m.axis() == Direction.Axis.X).orElse(false), "T along X recognized");
            h.assertTrue(HowlerPattern.nearSensor(l, c.offset(5, -1, -5)).isPresent(), "sensor 5 blocks away hears it");
            h.assertTrue(HowlerPattern.nearSensor(l, c.offset(6, -1, 0)).isEmpty(), "sensor 6 blocks away is too far");
            l.setBlock(c.east(), Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(HowlerPattern.at(l, c).isEmpty(), "missing arm: not recognized");
            figure(h, CENTER, Direction.Axis.Z, Blocks.SCULK_SHRIEKER);
            h.assertTrue(HowlerPattern.at(l, c).isPresent(), "turned figure (T along Z) recognized");
            l.setBlock(c.above(2), Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 3);
            h.assertTrue(HowlerPattern.at(l, c).isEmpty(), "wither skull: not recognized");
            l.setBlock(c.above(2), Blocks.SKELETON_SKULL.defaultBlockState(), 3);
            l.setBlock(c.above(), Blocks.SCULK.defaultBlockState(), 3);
            h.assertTrue(HowlerPattern.at(l, c).isEmpty(), "plain sculk is no jaw");
            l.setBlock(c.above(), Blocks.SCULK_SHRIEKER.defaultBlockState(), 3);
            l.setBlock(c.below(), Blocks.STONE.defaultBlockState(), 3);
            h.assertTrue(HowlerPattern.at(l, c).isEmpty(), "no sculk foot: not recognized");
            h.succeed();
        });
        add.accept("howler_jaw_tag", h -> {
            // Works without Simple Building: the Sculk Shrieker is always a jaw; SB's Sculk Jaw only when it exists.
            h.assertTrue(Blocks.SCULK_SHRIEKER.defaultBlockState().is(HowlerPattern.JAW), "shrieker is a howler jaw");
            var sbJaw = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("simplebuilding", "sculk_jaw"));
            sbJaw.ifPresent(b -> h.assertTrue(b.defaultBlockState().is(HowlerPattern.JAW), "SB sculk jaw is a howler jaw"));
            BlockPos c = figure(h, CENTER, Direction.Axis.X, sbJaw.orElse(Blocks.SCULK_SHRIEKER));
            h.assertTrue(HowlerPattern.at(h.getLevel(), c).isPresent(), "figure with " + (sbJaw.isPresent() ? "sculk jaw" : "shrieker [standalone]"));
            h.succeed();
        });
        add.accept("howler_horn_rule", h -> {
            ServerLevel l = h.getLevel();
            BlockPos c = figure(h, CENTER, Direction.Axis.X, Blocks.SCULK_SHRIEKER);
            BlockPos s = sensor(h, c);
            var player = h.makeMockServerPlayerInLevel();
            player.snapTo(c.getX() + 0.5, c.getY(), c.getZ() - 3.5);
            int horn = VibrationSystem.getGameEventFrequency(GameEvent.INSTRUMENT_PLAY);
            h.assertTrue(HowlerRitual.onSensorActivated(l, s, player, horn) == HowlerRitual.Result.NOT_A_HORN, "no horn blown: nothing");
            player.getCooldowns().addCooldown(new ItemStack(Items.GOAT_HORN), 140);
            h.assertTrue(HowlerRitual.onSensorActivated(l, s, player, horn + 1) == null, "other frequency: nothing");
            var r = HowlerRitual.onSensorActivated(l, s, player, horn);
            h.assertTrue(r == HowlerRitual.Result.STARTED || r == HowlerRitual.Result.TOO_BRIGHT, "horn call reaches the figure: " + r);
            h.getLevel().getEntitiesOfClass(HowlerRitualEntity.class, new AABB(c).inflate(2)).forEach(e -> e.discard());
            h.succeed();
        });
        add.accept("howler_abort_light", h -> {
            ServerLevel l = h.getLevel();
            BlockPos c = figure(h, CENTER, Direction.Axis.X, Blocks.SCULK_SHRIEKER);
            BlockPos s = sensor(h, c);
            l.setBlock(c.above(2).north(), Blocks.GLOWSTONE.defaultBlockState(), 3);
            var m = HowlerPattern.at(l, c).orElseThrow();
            // light updates run between ticks: check after a few
            h.runAfterDelay(5, () -> {
                h.assertTrue(HowlerRitual.start(l, m, s, null, 40, null) == HowlerRitual.Result.TOO_BRIGHT, "glowstone at the skull: too bright to start");
                h.assertTrue(rituals(l, c) == 0, "nothing started");
                h.assertTrue(HowlerRitual.start(l, m, s, null, 40, 0) == HowlerRitual.Result.STARTED, "dark: starts");
            });
            h.runAfterDelay(8, () -> ritual(l, c).forceLight(12));
            h.runAfterDelay(11, () -> {
                h.assertTrue(rituals(l, c) == 0, "a torch during the manifestation aborts it");
                h.assertTrue(partsPresent(l, c), "every part stays after the abort");
                h.assertTrue(l.getEntitiesOfClass(HowlerEntity.class, new AABB(c).inflate(4)).isEmpty(), "no howler");
                h.succeed();
            });
        });
        add.accept("howler_abort_sensor", h -> {
            ServerLevel l = h.getLevel();
            BlockPos c = figure(h, CENTER, Direction.Axis.Z, Blocks.SCULK_SHRIEKER);
            BlockPos s = sensor(h, c);
            var m = HowlerPattern.at(l, c).orElseThrow();
            h.assertTrue(HowlerRitual.start(l, m, s, null, 40, 0) == HowlerRitual.Result.STARTED, "starts");
            h.runAfterDelay(3, () -> l.setBlock(s, Blocks.AIR.defaultBlockState(), 3));
            h.runAfterDelay(6, () -> {
                h.assertTrue(rituals(l, c) == 0, "breaking the sensor aborts");
                h.assertTrue(partsPresent(l, c), "every part stays after the abort");
                h.assertTrue(l.getEntitiesOfClass(HowlerEntity.class, new AABB(c).inflate(4)).isEmpty(), "no howler");
                h.succeed();
            });
        });
        add.accept("howler_summon", h -> {
            ServerLevel l = h.getLevel();
            BlockPos c = figure(h, CENTER, Direction.Axis.X, Blocks.SCULK_SHRIEKER);
            BlockPos s = sensor(h, c);
            var m = HowlerPattern.at(l, c).orElseThrow();
            h.assertTrue(HowlerRitual.start(l, m, s, null, 10, 0) == HowlerRitual.Result.STARTED, "starts");
            h.assertTrue(HowlerRitual.start(l, m, s, null, 10, 0) == HowlerRitual.Result.BUSY, "one manifestation at a time");
            h.runAfterDelay(5, () -> h.assertTrue(partsPresent(l, c), "parts are only used up at the end"));
            h.runAfterDelay(15, () -> {
                h.assertTrue(rituals(l, c) == 0, "manifestation done");
                for (BlockPos p : m.parts()) h.assertTrue(l.getBlockState(p).isAir(), "part consumed at " + p);
                h.assertTrue(l.getBlockState(s).is(Blocks.SCULK_SENSOR), "the sensor stays");
                var howlers = l.getEntitiesOfClass(HowlerEntity.class, new AABB(c).inflate(4));
                h.assertTrue(howlers.size() == 1, "one Howler emerged: " + howlers.size());
                HowlerEntity howler = howlers.getFirst();
                h.assertTrue(howler.emerging() && howler.isPersistenceRequired(), "emerging and persistent");
                h.assertFalse(howler.hurtServer(l, l.damageSources().generic(), 5f), "invulnerable while emerging");
                howler.discard();
                h.succeed();
            });
        });
        add.accept("howler_fight", h -> {
            ServerLevel l = h.getLevel();
            for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) l.setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 3, 1, 3);
            howler.setNoAi(true);
            var player = h.makeMockServerPlayerInLevel();
            var src = l.damageSources().playerAttack(player);
            float before = howler.getHealth();
            howler.hurtServer(l, src, 10f);
            float plain = before - howler.getHealth();
            // a second Howler, so hurt cooldown frames do not mix in
            HowlerEntity biting = h.spawn(MobsRegistry.HOLLOW_HOWLER, 5, 1, 5);
            biting.setNoAi(true);
            biting.startBite();
            before = biting.getHealth();
            biting.hurtServer(l, src, 10f);
            float weak = before - biting.getHealth();
            biting.discard();
            h.assertTrue(Math.abs(weak - plain * 1.5f) < 0.01f, "hits on opening jaws deal x1.5: " + plain + " -> " + weak);
            var p = h.absolutePos(new BlockPos(3, 1, 3));
            player.snapTo(p.getX() + 8.5, p.getY(), p.getZ() + 0.5);
            h.assertTrue(howler.shriek(l, HowlerLogic.Phase.ONE) == 0, "creative players are spared by the shriek");
            h.assertFalse(howler.canHunt(player), "creative players are no prey");
            h.assertTrue(howler.dampensVibrations(), "its own steps are silent");
            var loot = l.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("simplemobs", "entities/hollow_howler")));
            h.assertTrue(loot != LootTable.EMPTY, "loot table loads");
            h.assertTrue(howler.getLootTable().map(k -> k.identifier().getPath().equals("entities/hollow_howler")).orElse(false), "uses its loot table");
            howler.discard();
            h.succeed();
        });
        add.accept("silence_dampens", h -> {
            var player = h.makeMockServerPlayerInLevel();
            h.assertFalse(player.dampensVibrations(), "normal players make vibrations");
            player.addEffect(new MobEffectInstance(MobsRegistry.silence(), 200));
            h.assertTrue(player.dampensVibrations(), "Silence dampens like wool");
            h.succeed();
        });
    }
}
