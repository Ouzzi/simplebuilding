package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/** Nachtrag 68: Howler stats against the Warden, miniboss bars, phase notches, and the phase-two sculk jaw rules. */
final class MinibossTests {
    private MinibossTests() {}

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    private static net.minecraft.world.entity.animal.pig.Pig prey(GameTestHelper h, double x, double z) {
        var p = h.spawn(EntityTypes.PIG, BlockPos.ZERO.above());
        p.setNoAi(true);
        p.addTag(HowlerEntity.TEST_PREY_TAG);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        p.snapTo(o.getX() + x, o.getY() + 1, o.getZ() + z);
        p.setOnGround(true);
        return p;
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("howler_vs_warden", h -> {
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 3, 1, 3);
            howler.setNoAi(true);
            var warden = h.spawn(EntityTypes.WARDEN, 6, 1, 6);
            warden.setNoAi(true);
            double wh = warden.getAttributeBaseValue(Attributes.MAX_HEALTH), wa = warden.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            h.assertTrue(howler.getMaxHealth() > wh && howler.getMaxHealth() <= wh * 1.5, "a little more health than the Warden: " + howler.getMaxHealth() + " vs " + wh);
            h.assertTrue(howler.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) > wa && howler.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) <= wa * 1.5, "a little more melee damage than the Warden");
            h.assertTrue(howler.getAttributeValue(Attributes.ARMOR) > warden.getAttributeValue(Attributes.ARMOR), "armor over the Warden's 0");
            h.assertTrue(wh == HowlerLogic.WARDEN_HEALTH && wa == HowlerLogic.WARDEN_ATTACK, "the logic's Warden numbers are the real ones");
            h.assertTrue(howler.getBbHeight() > 2.5f * 1.25f && howler.getBbWidth() > 1.1f * 1.25f, "larger: hitbox " + howler.getBbWidth() + " x " + howler.getBbHeight());
            warden.discard();
            howler.discard();
            h.succeed();
        });
        add.accept("boss_phase_notches", h -> {
            h.assertTrue(BossBars.overlayFor(HowlerLogic.HUNGER_SHARE, HowlerLogic.RAGE_SHARE) == BossEvent.BossBarOverlay.NOTCHED_10, "Howler 50/20: 10 segments");
            h.assertTrue(BossBars.overlayFor(WitherLogic.PHASE_2, WitherLogic.PHASE_3) == BossEvent.BossBarOverlay.NOTCHED_20, "Wither 50/15: 20 segments");
            h.assertTrue(BossBars.overlayFor(0.5f) == BossEvent.BossBarOverlay.NOTCHED_6, "one border at half: 6 segments");
            h.assertTrue(BossBars.overlayFor(0.25f) == BossEvent.BossBarOverlay.NOTCHED_12, "a quarter: 12 segments");
            h.assertTrue(BossBars.overlayFor() == BossEvent.BossBarOverlay.PROGRESS, "no phases: plain bar");
            h.assertTrue(BossBars.overlayFor(0.123f) == BossEvent.BossBarOverlay.PROGRESS, "borders no notch hits: plain bar");
            h.succeed();
        });
        add.accept("howler_bossbar", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 3, 1, 3);
            h.assertTrue(howler.bossEvent().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_10, "notches on the phase borders");
            h.assertTrue(howler.bossEvent().getName().toString().contains("simplemobs.bossbar.miniboss"), "named as a miniboss");
            howler.setHealth(howler.getMaxHealth() * 0.4f);
            h.succeedWhen(() -> {
                h.assertTrue(howler.bossEvent().getColor() == BossEvent.BossBarColor.YELLOW && howler.bossEvent().getName().toString().contains("simplemobs.bossbar.howler.2"),
                        "hunger: yellow, phase 2 in the name");
                howler.discard();
            });
        });
        add.accept("howler_bossbar_range", h -> {
            h.assertTrue(BossBars.inRange(47.0 * 47.0, 48.0) && !BossBars.inRange(49.0 * 49.0, 48.0), "the bar shows within the range only");
            h.succeed();
        });
        add.accept("dread_bossbar", h -> {
            floor(h);
            var dread = h.spawn(FearMobs.DREAD, 3, 1, 3);
            h.assertTrue(dread.bossEvent().getName().toString().contains("simplemobs.bossbar.miniboss"), "Dread is named as a miniboss");
            h.assertTrue(dread.bossEvent().getOverlay() == BossEvent.BossBarOverlay.PROGRESS, "no life phases: plain bar");
            h.runAfterDelay(5, () -> {
                h.assertTrue(dread.bossEvent().getProgress() == 1.0f, "full bar");
                dread.discard();
                h.succeed();
            });
        });
        add.accept("howler_jaw_rules", h -> {
            h.assertTrue(HowlerLogic.jawTarget(new boolean[0], 0) == -1, "nobody: no target");
            h.assertTrue(HowlerLogic.jawTarget(new boolean[] {true, false, true}, 0) == 1, "the one without a jaw is picked");
            h.assertTrue(HowlerLogic.jawTarget(new boolean[] {false, false}, 1) == 1, "free players rotate from the start");
            boolean[] all = {true, true};
            int t = HowlerLogic.jawTarget(all, 3);
            h.assertTrue(t == 1 && !HowlerLogic.jawUnderPlayer(all, t), "all covered: someone is picked, the jaw goes next to them");
            h.assertTrue(HowlerLogic.jawUnderPlayer(new boolean[] {false}, 0), "a free player gets it underneath");
            h.succeed();
        });
        add.accept("howler_jaw_spawn", h -> {
            ServerLevel l = h.getLevel();
            floor(h);
            HowlerEntity howler = h.spawn(MobsRegistry.HOLLOW_HOWLER, 0, 1, 0);
            howler.setNoAi(true);
            var prey = prey(h, 4.5, 4.5);
            howler.addTag(HowlerEntity.TEST_SOLO_PREFIX + "jaw_spawn");   // neighbouring tests' stand-ins are within the 24-block range
            prey.addTag(HowlerEntity.TEST_SOLO_PREFIX + "jaw_spawn");
            prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);   // a real Sculk Jaw under it must not kill the stand-in
            prey.setHealth(2000.0f);
            int savedMax = MobsConfig.howlerJawMax;
            MobsConfig.howlerJawMax = 2;
            try {
                BlockPos under = prey.blockPosition();
                h.assertTrue(under.equals(howler.planJaw(l)), "no jaw yet: it goes directly under the player");
                h.assertTrue(howler.spitTraps(l) == 1, "the first jaw is placed");
                BlockPos near = howler.planJaw(l);
                h.assertTrue(near != null && !near.equals(under), "a jaw lies at the first spot: the next one goes elsewhere (planned " + near + ", first " + under + ", pig now " + prey.blockPosition() + ")");
                double d = Math.hypot(near.getX() + 0.5 - prey.getX(), near.getZ() + 0.5 - prey.getZ());
                h.assertTrue(d <= HowlerLogic.JAW_NEAR_MAX + 1.5 && l.getBlockState(near).isAir() && !l.getBlockState(near.below()).isAir(), "near the player, on valid ground, not in a block: " + near + " d=" + d + " state=" + l.getBlockState(near) + " below=" + l.getBlockState(near.below()));
                h.assertTrue(howler.spitTraps(l) == 1, "the second jaw is placed");
                h.assertTrue(howler.spitTraps(l) == 0, "the cap holds");
            } finally {
                MobsConfig.howlerJawMax = savedMax;
            }
            howler.discard();
            prey.discard();
            h.succeed();
        });
    }
}
