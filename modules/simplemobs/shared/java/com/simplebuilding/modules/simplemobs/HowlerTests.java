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
import net.minecraft.world.effect.MobEffects;
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

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    /**
     * Stand-in prey (mock server players are always creative, see NetworkHandlerTests): a still pig tagged as prey,
     * standing (or, with {@code ground} false, in the air) at relative x/z on the floor.
     */
    private static net.minecraft.world.entity.animal.pig.Pig prey(GameTestHelper h, double x, double z, boolean ground) {
        var p = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, BlockPos.ZERO.above());
        p.setNoAi(true);
        p.addTag(HowlerEntity.TEST_PREY_TAG);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        p.snapTo(o.getX() + x, o.getY() + 1 + (ground ? 0 : 0.8), o.getZ() + z);
        p.setOnGround(ground);
        return p;
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
            h.assertTrue(attrs.getBaseValue(Attributes.MAX_HEALTH) == 600.0, "600 health");
            h.assertTrue(attrs.getBaseValue(Attributes.ATTACK_DAMAGE) == 34.0 && attrs.getBaseValue(Attributes.ARMOR) == 8.0, "bite 34, armor 8");
            // Nachtrag 46: a little stronger than the Warden
            var warden = net.minecraft.world.entity.monster.warden.Warden.createAttributes().build();
            h.assertTrue(attrs.getBaseValue(Attributes.MAX_HEALTH) > warden.getBaseValue(Attributes.MAX_HEALTH), "more health than the Warden");
            h.assertTrue(attrs.getBaseValue(Attributes.ARMOR) > warden.getBaseValue(Attributes.ARMOR), "more armor than the Warden");
            h.assertTrue(attrs.getBaseValue(Attributes.KNOCKBACK_RESISTANCE) >= warden.getBaseValue(Attributes.KNOCKBACK_RESISTANCE), "knockback resistance like the Warden");
            h.assertTrue(attrs.getBaseValue(Attributes.MOVEMENT_SPEED) >= warden.getBaseValue(Attributes.MOVEMENT_SPEED) - 1.0e-6, "as fast as the Warden");
            h.succeed();
        });
        add.accept("howler_rules", h -> {
            h.assertTrue(HowlerLogic.phase(240, 240) == HowlerLogic.Phase.ONE && HowlerLogic.phase(121, 240) == HowlerLogic.Phase.ONE, "phase one above 50 %");
            h.assertTrue(HowlerLogic.phase(120, 240) == HowlerLogic.Phase.HUNGER, "hunger at 50 %");
            h.assertTrue(HowlerLogic.phase(48, 240) == HowlerLogic.Phase.RAGE, "rage at 20 %");
            h.assertTrue(HowlerLogic.shriekCooldown(HowlerLogic.Phase.RAGE) * 2 == HowlerLogic.shriekCooldown(HowlerLogic.Phase.ONE), "rage shrieks twice as often");
            h.assertTrue(HowlerLogic.cooldown(180, HowlerLogic.Phase.RAGE) == 120 && HowlerLogic.cooldown(180, HowlerLogic.Phase.HUNGER) == 180, "rage: cooldowns a third shorter");
            h.assertTrue(HowlerLogic.speedFactor(HowlerLogic.Phase.RAGE) > HowlerLogic.speedFactor(HowlerLogic.Phase.HUNGER), "rage is faster");
            h.succeed();
        });
        add.accept("howler_attack_choice", h -> {
            // choose(dist, reach, sight, gap, bite, shockwave, shriek, charge, tendrils)
            h.assertTrue(HowlerLogic.choose(2, 3, true, 0, 0, 0, 0, 0, 0) == HowlerLogic.Attack.BITE, "in reach: bite");
            h.assertTrue(HowlerLogic.choose(2, 3, true, 20, 0, 0, 0, 0, 0) == HowlerLogic.Attack.BITE, "the bite ignores the pause");
            h.assertTrue(HowlerLogic.choose(3.5, 3, true, 0, 10, 0, 0, 0, 0) == HowlerLogic.Attack.SHOCKWAVE, "close, bite cooling: shockwave");
            h.assertTrue(HowlerLogic.choose(3.5, 3, true, 5, 10, 0, 0, 0, 0) == HowlerLogic.Attack.NONE, "pause after a special attack: nothing");
            h.assertTrue(HowlerLogic.choose(8, 3, true, 0, 0, 0, 0, 0, 0) == HowlerLogic.Attack.SHRIEK, "8 blocks in sight: shriek");
            h.assertTrue(HowlerLogic.choose(8, 3, true, 0, 0, 0, 50, 0, 0) == HowlerLogic.Attack.CHARGE, "shriek cooling: charge");
            h.assertTrue(HowlerLogic.choose(14, 3, true, 0, 0, 0, 0, 0, 0) == HowlerLogic.Attack.CHARGE, "14 blocks: charge");
            h.assertTrue(HowlerLogic.choose(8, 3, false, 0, 0, 0, 0, 0, 0) == HowlerLogic.Attack.TENDRILS, "behind cover: tendrils, no shriek or charge");
            h.assertTrue(HowlerLogic.choose(20, 3, true, 0, 0, 0, 0, 0, 0) == HowlerLogic.Attack.NONE, "too far: nothing");
            h.assertTrue(HowlerLogic.shockwaveHits(4.9, 0, true) && !HowlerLogic.shockwaveHits(4.9, 0, false) && !HowlerLogic.shockwaveHits(5.1, 0, true),
                    "shockwave: on the ground within 5 blocks; jumping or stepping out dodges");
            // fair: every special attack is announced at least as long as the bite wind-up, then a pause follows
            for (int w : new int[] {HowlerLogic.SHOCKWAVE_WINDUP_TICKS, HowlerLogic.CHARGE_WINDUP_TICKS, HowlerLogic.SHRIEK_WINDUP_TICKS, HowlerLogic.TENDRIL_WINDUP_TICKS})
                h.assertTrue(w >= HowlerLogic.BITE_WINDUP_TICKS, "telegraph of " + w + " ticks");
            h.assertTrue(HowlerLogic.PATTERN_GAP_TICKS > 0, "pause between special attacks");
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
            howler.hurtServer(l, src, 20f);
            float plain = before - howler.getHealth();
            // a second Howler, so hurt cooldown frames do not mix in
            HowlerEntity biting = h.spawn(MobsRegistry.HOLLOW_HOWLER, 5, 1, 5);
            biting.setNoAi(true);
            biting.startBite();
            before = biting.getHealth();
            biting.hurtServer(l, src, 20f);
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
        add.accept("howler_shockwave", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 1, 1, 4);
            howler.setNoAi(true);
            var near = prey(h, 4.5, 4.5, true);
            var far = prey(h, 6.5, 0.5, true);
            var jumping = prey(h, 1.5, 7.5, false);
            howler.startShockwave(l);
            h.assertTrue(howler.attack() == HowlerLogic.Attack.SHOCKWAVE && near.getHealth() == near.getMaxHealth(), "wind-up first, no damage yet");
            h.assertTrue(howler.shockwave(l) == 1, "only the player on the ground inside the ring is hit");
            h.assertTrue(near.getHealth() < near.getMaxHealth(), "3 blocks away on the ground: hit");
            h.assertTrue(far.getHealth() == far.getMaxHealth(), "6.4 blocks away: outside the ring");
            h.assertTrue(jumping.getHealth() == jumping.getMaxHealth(), "in the air: jumped over it");
            howler.discard();
            h.succeed();
        });
        add.accept("howler_shriek_cover", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 1, 1, 1);
            howler.setNoAi(true);
            var open = prey(h, 1.5, 7.5, true);
            var covered = prey(h, 7.5, 1.5, true);
            for (int y = 1; y <= 5; y++) for (int z = 0; z <= 3; z++) l.setBlock(h.absolutePos(new BlockPos(5, y, z)), Blocks.STONE.defaultBlockState(), 3);
            h.assertTrue(howler.shriek(l, HowlerLogic.Phase.ONE) == 1, "one player in sight hit");
            h.assertTrue(open.getHealth() < open.getMaxHealth(), "in sight: hit");
            h.assertTrue(covered.getHealth() == covered.getMaxHealth(), "behind a wall: cover blocks the shriek");
            howler.discard();
            h.succeed();
        });
        add.accept("howler_tendrils", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 0);
            howler.setNoAi(true);
            var target = prey(h, 4.5, 4.5, true);
            var aside = prey(h, 7.5, 4.5, true);
            h.assertTrue(howler.canHunt(target), "tagged stand-in is prey");
            var plainPig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(6, 1, 1));
            h.assertFalse(howler.canHunt(plainPig), "other mobs are no prey");
            int marks = howler.spitTendrils(l, target);
            h.assertTrue(marks >= 1 && howler.pendingTendrils() == marks, "marks placed, waiting to erupt: " + marks);
            h.assertTrue(target.getHealth() == target.getMaxHealth(), "no damage while the marks pulse");
            h.assertTrue(howler.eruptTendril(l, target.position()) == 1, "standing on the mark: caught");
            h.assertTrue(target.hasEffect(MobEffects.SLOWNESS) && target.getHealth() < target.getMaxHealth(), "caught: damage and Slowness");
            h.assertFalse(aside.hasEffect(MobEffects.SLOWNESS), "3 blocks aside: stepped off");
            howler.discard();
            h.succeed();
        });
        add.accept("howler_charge_hit", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 3);
            double startX = howler.getX();
            var target = prey(h, 7.0, 3.5, true);
            var aside = prey(h, 4.5, 6.8, true);
            howler.setTarget(target);
            howler.startCharge(l, target.position().subtract(howler.position()));
            h.assertTrue(howler.attack() == HowlerLogic.Attack.CHARGE && howler.chargeDirection().x > 0.99, "locked straight ahead");
            h.runAfterDelay(HowlerLogic.CHARGE_WINDUP_TICKS - 2, () -> h.assertTrue(howler.getX() < startX + 0.5, "stays put during the wind-up"));
            h.succeedWhen(() -> {
                h.assertTrue(target.getHealth() < target.getMaxHealth(), "the charge hits the player in its line (moved " + (howler.getX() - startX) + ", " + howler.attack() + ")");
                h.assertTrue(aside.getHealth() == aside.getMaxHealth(), "a player beside the line is spared");
                howler.discard();
            });
        });
        add.accept("howler_charge_wall", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            for (int y = 1; y <= 4; y++) for (int z = 0; z <= 7; z++) l.setBlock(h.absolutePos(new BlockPos(4, y, z)), Blocks.STONE.defaultBlockState(), 3);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 3);
            double startX = howler.getX();
            var target = prey(h, 7.0, 3.5, true);
            howler.setTarget(target);
            howler.startCharge(l, target.position().subtract(howler.position()));
            h.succeedWhen(() -> {
                h.assertTrue(howler.staggered(), "running into the wall staggers it (moved " + (howler.getX() - startX) + ", " + howler.attack() + ", target " + howler.getTarget() + ")");
                h.assertTrue(target.getHealth() == target.getMaxHealth(), "the player behind the wall is safe");
                howler.discard();
            });
        });
        add.accept("howler_heartbeat", h -> {
            HowlerLogic.Phase one = HowlerLogic.Phase.ONE, hunger = HowlerLogic.Phase.HUNGER, rage = HowlerLogic.Phase.RAGE;
            h.assertTrue(HowlerLogic.heartbeatDelay(one) == 40 && HowlerLogic.heartbeatDelay(hunger) == 26 && HowlerLogic.heartbeatDelay(rage) == 14, "heartbeat tempo 40/26/14 ticks");
            int beats = 0, seconds = 0;
            for (int t = 0; t < 280; t++) {
                if (HowlerLogic.heartbeatCycle(t, 3, rage) == 0) beats++;
                if (HowlerLogic.heartbeatCycle(t, 3, rage) == HowlerLogic.HEART_SECOND_BEAT_TICKS) seconds++;
            }
            h.assertTrue(beats == 20 && seconds == 20, "rage: 20 lub-dub pairs in 280 ticks (" + beats + "/" + seconds + ")");
            h.assertTrue(HowlerLogic.heartbeatCycle(35, 7, one) == 2 && HowlerLogic.heartbeatCycle(0, -5, one) == 35, "cycle is positive for any id");
            float min = 1.0f;
            for (int t = 0; t < 14; t++) min = Math.min(min, HowlerLogic.heartGlow(t));
            h.assertTrue(min >= 0.45f && HowlerLogic.heartGlow(0) == 1.0f && HowlerLogic.heartGlow(HowlerLogic.HEART_SECOND_BEAT_TICKS) > 0.8f, "glow never below 0.45, peaks on both beats");
            h.assertTrue(!HowlerLogic.quiet(false, HowlerLogic.Attack.NONE, 0, 0) && HowlerLogic.quiet(true, HowlerLogic.Attack.NONE, 0, 0)
                    && HowlerLogic.quiet(false, HowlerLogic.Attack.SHRIEK, 0, 0) && HowlerLogic.quiet(false, HowlerLogic.Attack.NONE, 5, 0) && HowlerLogic.quiet(false, HowlerLogic.Attack.NONE, 0, 9),
                    "quiet while emerging, attacking, holding its breath or staggered");
            h.assertTrue(HowlerLogic.biteJaw(0) == 0.0f && HowlerLogic.biteJaw(1.5f) < 0.0f && Math.abs(HowlerLogic.biteJaw(HowlerLogic.BITE_WINDUP_TICKS) - 1.0f) < 1.0e-4f
                    && HowlerLogic.biteRecoil(4) < HowlerLogic.biteRecoil(1) && HowlerLogic.biteRecoil(0) == 0.0f, "bite: anticipation, cosine open, overshoot decays");
            float last = -1.0f;
            boolean rising = true;
            for (int i = 0; i <= 40; i++) { float e = HowlerLogic.emergeHeight(i / 40.0f); if (i <= 12 && e < last - 1.0e-6f) rising = false; last = e; }
            h.assertTrue(rising && HowlerLogic.emergeHeight(0) == 0.0f && HowlerLogic.emergeHeight(1) == 1.0f && HowlerLogic.emergeHeight(0.4f) == 0.25f && Math.abs(HowlerLogic.emergeHeight(0.3f) - 0.25f) < 1.0e-5f,
                    "emerge: slow start, hold at a quarter, out at the end");
            h.assertTrue(HowlerLogic.shakeAmplitude(HowlerLogic.Attack.SHOCKWAVE, 0) > HowlerLogic.shakeAmplitude(HowlerLogic.Attack.SHRIEK, 8) && HowlerLogic.shakeAmplitude(HowlerLogic.Attack.CHARGE, 1) == 0.0f
                    && HowlerLogic.shakeAmplitude(HowlerLogic.Attack.SHOCKWAVE, 16) == 0.0f, "shake only for shockwave and shriek, fading with distance");
            h.succeed();
        });
        add.accept("howler_rage_pulse_rules", h -> {
            int configured = HowlerLogic.PULSE_INTERVAL_TICKS;
            int interval = HowlerLogic.pulseInterval(configured, HowlerLogic.Phase.RAGE);
            h.assertTrue(interval % HowlerLogic.heartbeatDelay(HowlerLogic.Phase.RAGE) == 0 && Math.abs(interval - configured) < 14, "pulse period is a whole number of rage heartbeats (" + interval + ")");
            int rage = 0;
            for (int t = 0; t < 1200; t++) {
                for (HowlerLogic.Phase p : new HowlerLogic.Phase[] {HowlerLogic.Phase.ONE, HowlerLogic.Phase.HUNGER}) {
                    h.assertFalse(HowlerLogic.pulseDue(t, 5, p, configured), "no pulse before rage");
                }
                if (HowlerLogic.pulseDue(t, 5, HowlerLogic.Phase.RAGE, configured)) {
                    rage++;
                    h.assertTrue(HowlerLogic.heartbeatCycle(t, 5, HowlerLogic.Phase.RAGE) == 0, "the pulse lands on a heartbeat");
                }
            }
            h.assertTrue(rage == 1200 / interval, "rage pulses " + rage + " times in 1200 ticks");
            h.succeed();
        });
        add.accept("howler_phase_sounds", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 3);
            h.assertTrue(!howler.quiet() && howler.getAmbientSound() == net.minecraft.sounds.SoundEvents.WARDEN_AMBIENT, "phase one: Warden ambient, not quiet");
            howler.setHealth(howler.getMaxHealth() * 0.4f);
            h.runAfterDelay(6, () -> {
                h.assertTrue(howler.phase() == HowlerLogic.Phase.ONE && howler.hushTicks() > 0 && howler.quiet() && howler.getAmbientSound() == null, "holds its breath before the phase change (silence)");
            });
            h.succeedWhen(() -> {
                h.assertTrue(howler.phase() == HowlerLogic.Phase.HUNGER && !howler.quiet(), "hunger after the hush");
                h.assertTrue(howler.getAmbientSound() == net.minecraft.sounds.SoundEvents.WARDEN_AGITATED, "hunger: agitated ambient");
                howler.discard();
            });
        });
        add.accept("howler_rage_pulse", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 0);
            var prey = prey(h, 6.0, 6.0, true);
            int saved = MobsConfig.howlerPulseInterval;
            MobsConfig.howlerPulseInterval = 14;   // one rage heartbeat period, so the test needs no long wait
            howler.setHealth(howler.getMaxHealth() * 0.1f);
            boolean direct = howler.pulseDarkness(l) == 1 && prey.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            prey.removeAllEffects();
            if (!direct) MobsConfig.howlerPulseInterval = saved;
            h.assertTrue(direct, "the pulse puts Darkness on hunted players in range");
            h.succeedWhen(() -> {
                h.assertTrue(howler.phase() == HowlerLogic.Phase.RAGE, "rage reached");
                h.assertTrue(prey.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS), "in rage the Darkness pulse comes by itself");
                MobsConfig.howlerPulseInterval = saved;
                howler.discard();
            });
        });
        add.accept("howler_no_pulse_before_rage", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 0);
            var prey = prey(h, 6.0, 6.0, true);
            int saved = MobsConfig.howlerPulseInterval;
            MobsConfig.howlerPulseInterval = 14;
            howler.setHealth(howler.getMaxHealth() * 0.4f);
            h.runAfterDelay(60, () -> {
                MobsConfig.howlerPulseInterval = saved;
                h.assertTrue(howler.phase() == HowlerLogic.Phase.HUNGER, "hunger phase");
                // The bite also gives Darkness (4 s) now that the bigger Howler reaches the stand-in; the pulse is the long, particle-less one.
                var dark = prey.getEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
                h.assertTrue(dark == null || dark.getDuration() <= HowlerLogic.DARKNESS_TICKS, "no pulse in hunger");
                howler.discard();
                h.succeed();
            });
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
