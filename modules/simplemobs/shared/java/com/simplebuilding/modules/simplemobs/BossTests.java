package com.simplebuilding.modules.simplemobs;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** WP-19 GameTests: Void Runner, Brimguard (hat rule, venting, ducking), both summonings, loot, brood hollow. Standalone-safe. */
final class BossTests {
    private BossTests() {}

    private static void floor(GameTestHelper h, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    private static <T extends LivingEntity> T still(T e) {
        if (e instanceof net.minecraft.world.entity.Mob m) m.setNoAi(true);
        return e;
    }

    private static void drop(GameTestHelper h, BlockPos onTop, ItemStack st) {
        BlockPos p = h.absolutePos(onTop.above());
        ItemEntity e = new ItemEntity(h.getLevel(), p.getX() + 0.5, p.getY() + 0.05, p.getZ() + 0.5, st);
        e.setDeltaMovement(Vec3.ZERO);
        e.setPickUpDelay(200);
        h.getLevel().addFreshEntity(e);
    }

    /** Runs body with Normal difficulty and the End-only rule off (GameTests run in the Overworld). */
    private static void ritualWorld(GameTestHelper h, Runnable body) {
        ServerLevel level = h.getLevel();
        Difficulty old = level.getDifficulty();
        boolean endOnly = MobsConfig.bossRitualsEndOnly;
        if (old == Difficulty.PEACEFUL) level.getServer().setDifficulty(Difficulty.NORMAL, true);
        MobsConfig.bossRitualsEndOnly = false;
        try { body.run(); } finally {
            MobsConfig.bossRitualsEndOnly = endOnly;
            if (old == Difficulty.PEACEFUL) level.getServer().setDifficulty(old, true);
        }
    }

    private static AbstractArrow arrow(GameTestHelper h, Vec3 at, Vec3 motion) {
        var arrow = EntityTypes.ARROW.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        if (arrow == null) throw new AssertionError("arrow");
        arrow.snapTo(at.x, at.y, at.z, 0f, 0f);
        arrow.setDeltaMovement(motion);
        return arrow;
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("boss_registered", h -> {
            for (var key : List.of(BossMobs.RUNNER_KEY, BossMobs.BRIMGUARD_KEY, BossMobs.BROOD_KEY))
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(key.identifier()), "entity type " + key.identifier());
            for (var key : List.of(BossMobs.RUNNER_EGG_KEY, BossMobs.BRIMGUARD_EGG_KEY, BossMobs.ESSENCE_KEY, BossMobs.EGG_BLANK_KEY))
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(key.identifier()), "item " + key.identifier());
            var r = VoidRunnerEntity.createAttributes().build();
            h.assertTrue(r.getBaseValue(Attributes.MAX_HEALTH) == 900.0 && r.getBaseValue(Attributes.ARMOR) == 10.0 && r.getBaseValue(Attributes.ATTACK_DAMAGE) == 14.0, "Void Runner 900/10/14");
            var b = BrimguardEntity.createAttributes().build();
            h.assertTrue(b.getBaseValue(Attributes.MAX_HEALTH) == 700.0 && b.getBaseValue(Attributes.ARMOR) == 14.0, "Brimguard 700/14");
            h.assertTrue(Math.abs(BossMobs.VOID_RUNNER.getWidth() - 0.5f) < 1e-4 && Math.abs(BossMobs.VOID_RUNNER.getHeight() - 1.9f) < 1e-4, "runner 0.5 x 1.9");
            h.assertTrue(Math.abs(BossMobs.BRIMGUARD.getWidth() - 0.6f) < 1e-4 && Math.abs(BossMobs.BRIMGUARD.getHeight() - 1.5f) < 1e-4, "brimguard 0.6 x 1.5");
            h.assertTrue(new ItemStack(BossMobs.ESSENCE).getMaxStackSize() == 16, "essence stacks to 16");
            h.succeed();
        });
        add.accept("boss_hat_rule", h -> {
            double r15 = Math.toRadians(15), r45 = Math.toRadians(45), r30 = Math.toRadians(30);
            // preset M: h = D = 0.8, theta0 = 45 degrees
            h.assertTrue(!BossLogic.hatBlocks(0.79, 0.0, false, 0.0), "level shot reaches just under the brim");
            h.assertTrue(BossLogic.hatBlocks(0.81, 0.0, false, 0.0), "above the brim is hat");
            h.assertTrue(Math.abs(BossLogic.reachableHeight(r15, false, 0.0) - (0.8 - 0.8 * Math.tan(r15))) < 1e-9, "15 degrees: lower part only");
            h.assertTrue(!BossLogic.hatBlocks(0.3, r15, false, 0.0) && BossLogic.hatBlocks(0.7, r15, false, 0.0), "15 degrees: feet yes, chest no");
            h.assertTrue(BossLogic.hatBlocks(0.01, r45 + 0.01, false, 0.0), "45 degrees and steeper: nothing");
            h.assertTrue(BossLogic.hatBlocks(0.3, r30, true, 0.0) && !BossLogic.hatBlocks(0.3, r30, false, 0.0), "ducking covers more (brim 2/3)");
            h.assertTrue(!BossLogic.hatBlocks(0.5, r30, false, BossLogic.VENT_LIFT), "venting lifts the brim: 30 degrees reach the body");
            h.assertTrue(BossLogic.decayLift(0.5f) == 0.0 && Math.abs(BossLogic.decayLift(0.19f) - 0.1) < 1e-9 && Math.abs(BossLogic.decayLift(0.05f) - 0.2) < 1e-9, "decay +0.1 per 10 % below 30 %");
            h.assertTrue(BossLogic.brimPhase(1f) == 1 && BossLogic.brimPhase(0.6f) == 2 && BossLogic.brimPhase(0.2f) == 3, "brim phases");
            h.assertTrue(BossLogic.ventsAfter(2) && !BossLogic.ventsAfter(3) && BossLogic.ventsAfter(4), "every 2nd slam vents");
            h.assertTrue(BossLogic.shouldDuck(0, 30, 2) && BossLogic.shouldDuck(0, 0, 8) && !BossLogic.shouldDuck(0, 10, 3) && !BossLogic.shouldDuck(5, 40, 9), "duck rule");
            h.succeed();
        });
        add.accept("boss_hat_arrow", h -> {
            floor(h, 0, 0, 6, 6);
            var brim = still(h.spawn(BossMobs.BRIMGUARD, 3, 2, 3));
            var player = h.makeMockServerPlayerInLevel();
            ServerLevel l = h.getLevel();
            float full = brim.getHealth();
            // straight down onto the crown: caught
            var down = arrow(h, brim.position().add(0, 2.5, 0), new Vec3(0, -2.0, 0));
            h.assertFalse(brim.hurtServer(l, l.damageSources().arrow(down, player), 10f), "an arrow from above is caught by the hat");
            h.assertTrue(brim.getHealth() == full, "no damage from above");
            // level at the shins from the side: hits
            var side = arrow(h, brim.position().add(-3.0, 0.3, 0), new Vec3(2.0, 0, 0));
            h.assertTrue(brim.hurtServer(l, l.damageSources().arrow(side, player), 10f), "a level arrow at the legs hits");
            h.assertTrue(brim.getHealth() < full, "damage from the side");
            // a falling anvil bounces off
            brim.setInvulnerableTime(0);
            float before = brim.getHealth();
            h.assertFalse(brim.hurtServer(l, l.damageSources().anvil(player), 10f) || brim.getHealth() < before, "anvils bounce off");
            brim.discard();
            h.succeed();
        });
        add.accept("boss_vent_duck", h -> {
            floor(h, 0, 0, 6, 6);
            ServerLevel l = h.getLevel();
            var brim = still(h.spawn(BossMobs.BRIMGUARD, 3, 2, 3));
            brim.setHealth(brim.getMaxHealth() * 0.5f); // phase 2
            h.assertTrue(brim.phase() == 2, "phase 2 at half health");
            brim.slam(l);
            h.assertFalse(brim.venting(), "first slam: no venting");
            brim.slam(l);
            h.assertTrue(brim.venting() && Math.abs(brim.brimLift() - BossLogic.VENT_LIFT) < 1e-9, "second slam vents (brim +0.5)");
            float a = brim.getHealth();
            brim.setInvulnerableTime(0);
            brim.hurtServer(l, l.damageSources().magic(), 10f);
            h.assertTrue(Math.abs((a - brim.getHealth()) - 15f) < 0.01f, "venting: 1.5 x damage, took " + (a - brim.getHealth()));
            h.assertTrue(Math.abs(brim.getBbHeight() - 1.5f) < 1e-3, "standing 1.5");
            brim.duck(40);
            h.assertTrue(brim.ducking() && Math.abs(brim.getBbHeight() - 1.0f) < 1e-3, "ducking 1.0, was " + brim.getBbHeight());
            brim.discard();
            h.succeed();
        });
        add.accept("boss_runner_rules", h -> {
            h.assertTrue(BossLogic.runnerPhase(1f) == 1 && BossLogic.runnerPhase(0.5f) == 2 && BossLogic.runnerPhase(0.2f) == 3, "runner phases");
            h.assertTrue(BossLogic.dodgeCooldown(3) == BossLogic.DODGE_COOLDOWN / 2 && BossLogic.dodgeCooldown(1) == BossLogic.DODGE_COOLDOWN, "rage halves the dodge cooldown");
            h.assertTrue(Math.abs(BossLogic.runnerDamage(14f, 3) - 17.5f) < 1e-4 && BossLogic.runnerDamage(14f, 2) == 14f, "rage x1.25");
            h.assertTrue(BossLogic.lungeReady(2, 0, true, 5) && !BossLogic.lungeReady(1, 0, true, 5) && !BossLogic.lungeReady(2, 0, true, 8) && !BossLogic.lungeReady(2, 3, true, 5), "lunge rule");
            floor(h, 0, 0, 8, 8);
            ServerLevel l = h.getLevel();
            var runner = still(h.spawn(BossMobs.VOID_RUNNER, 4, 2, 4));
            var player = h.makeMockServerPlayerInLevel();
            player.snapTo(h.absoluteVec(new Vec3(1.5, 2, 4.5)));
            runner.resetCooldowns();
            h.assertTrue(runner.hurtServer(l, l.damageSources().playerAttack(player), 5f), "a melee hit lands");
            h.assertTrue(runner.dodgeCooldown() > 0 && runner.getDeltaMovement().horizontalDistanceSqr() > 0.1, "it steps aside after the hit");
            // a shield blocking the lunge: stagger
            runner.markLunging();
            runner.blockedByItem(player, l.damageSources().mobAttack(runner), 18f, true);
            h.assertTrue(runner.action() == VoidRunnerEntity.ACT_STAGGER, "blocked lunge staggers");
            float hp = runner.getHealth();
            runner.setInvulnerableTime(0);
            h.assertFalse(runner.hurtServer(l, l.damageSources().fall(), 20f), "no fall damage");
            h.assertTrue(runner.getHealth() == hp, "fall damage ignored");
            runner.discard();
            h.succeed();
        });
        add.accept("boss_summon_runner", h -> ritualWorld(h, () -> {
            ServerLevel l = h.getLevel();
            floor(h, 0, 0, 6, 4);
            BlockPos base = new BlockPos(2, 1, 2), top = base.above(3);
            l.setBlock(h.absolutePos(base), Blocks.END_STONE.defaultBlockState(), 3);
            for (int i = 1; i <= 3; i++) l.setBlock(h.absolutePos(base.above(i)), Blocks.PURPUR_PILLAR.defaultBlockState(), 3);
            h.assertFalse(BossRituals.runnerMatches(l, h.absolutePos(top)), "no offerings: no summon");
            drop(h, top, new ItemStack(Items.ENDER_EYE));
            drop(h, top, new ItemStack(Items.CHORUS_FRUIT, 4));
            h.assertTrue(BossRituals.runnerMatches(l, h.absolutePos(top)), "column + offerings");
            boolean on = MobsConfig.voidRunnerSummonEnabled;
            MobsConfig.voidRunnerSummonEnabled = false;
            h.assertFalse(BossRituals.runnerMatches(l, h.absolutePos(top)), "config off");
            MobsConfig.voidRunnerSummonEnabled = on;
            MobsConfig.bossRitualsEndOnly = true;
            h.assertFalse(BossRituals.runnerMatches(l, h.absolutePos(top)), "End only: not in the Overworld");
            MobsConfig.bossRitualsEndOnly = false;
            h.assertTrue(BossRituals.trySummonRunner(l, h.absolutePos(top)), "summons");
            var runners = h.getEntities(BossMobs.VOID_RUNNER);
            h.assertTrue(runners.size() == 1 && runners.getFirst().isPersistenceRequired(), "one Void Runner, persistent");
            for (int i = 1; i <= 3; i++) h.assertTrue(l.getBlockState(h.absolutePos(base.above(i))).isAir(), "column used up");
            h.assertTrue(BossRituals.count(BossRituals.offerings(l, h.absolutePos(top)), s -> !s.isEmpty()) == 0, "offerings used up");
            runners.forEach(e -> e.discard());
            h.succeed();
        }));
        add.accept("boss_summon_brimguard", h -> ritualWorld(h, () -> {
            ServerLevel l = h.getLevel();
            BlockPos c = new BlockPos(3, 1, 3);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) l.setBlock(h.absolutePos(c.offset(dx, 0, dz)), Blocks.OBSIDIAN.defaultBlockState(), 3);
            drop(h, c, new ItemStack(EndNetherMobs.SHELL, 2));
            h.assertFalse(BossRituals.brimMatches(l, h.absolutePos(c)), "two shells are not enough");
            drop(h, c, new ItemStack(EndNetherMobs.SHELL, 1));
            l.setBlock(h.absolutePos(c.offset(1, 0, 1)), Blocks.STONE.defaultBlockState(), 3);
            h.assertFalse(BossRituals.brimMatches(l, h.absolutePos(c)), "platform incomplete");
            l.setBlock(h.absolutePos(c.offset(1, 0, 1)), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            h.assertTrue(BossRituals.trySummonBrimguard(l, h.absolutePos(c)), "summons");
            h.assertTrue(h.getEntities(BossMobs.BRIMGUARD).size() == 1, "one Brimguard");
            h.assertTrue(l.getBlockState(h.absolutePos(c)).is(Blocks.OBSIDIAN), "platform stays");
            h.getEntities(BossMobs.BRIMGUARD).forEach(e -> e.discard());
            h.succeed();
        }));
        add.accept("boss_loot", h -> {
            ServerLevel l = h.getLevel();
            floor(h, 0, 0, 4, 4);
            var player = h.makeMockServerPlayerInLevel();
            var tables = l.getServer().reloadableRegistries();
            var runner = still(h.spawn(BossMobs.VOID_RUNNER, 1, 2, 1));
            var brim = still(h.spawn(BossMobs.BRIMGUARD, 3, 2, 3));
            LootTable rl = tables.getLootTable(runner.getLootTable().orElseThrow()), bl = tables.getLootTable(brim.getLootTable().orElseThrow());
            h.assertTrue(rl != LootTable.EMPTY && bl != LootTable.EMPTY, "loot tables load");
            for (int i = 0; i < 30; i++) {
                int hearts = 0, pearls = 0, gems = 0;
                for (ItemStack st : rl.getRandomItems(params(l, runner, player))) {
                    if (st.is(WitherWings.ENDER_HEART)) hearts += st.getCount();
                    if (st.is(Items.ENDER_PEARL)) pearls += st.getCount();
                }
                for (ItemStack st : bl.getRandomItems(params(l, brim, player))) if (st.is(WitherWings.ENDER_GEM)) gems += st.getCount();
                h.assertTrue(hearts == 1, "one Ender Heart: " + hearts);
                h.assertTrue(pearls >= 8 && pearls <= 12, "8-12 pearls: " + pearls);
                h.assertTrue(gems == 1, "one Ender Gem: " + gems);
            }
            runner.discard();
            brim.discard();
            h.succeed();
        });
        add.accept("brood_rules", h -> {
            h.assertTrue(BroodRitual.eggClass(EntityTypes.CHICKEN) == 1, "chicken I");
            h.assertTrue(BroodRitual.eggClass(EntityTypes.VILLAGER) == 2, "villager II");
            h.assertTrue(BroodRitual.eggClass(EntityTypes.ZOMBIE) == 3, "zombie III");
            h.assertTrue(BroodRitual.eggClass(EntityTypes.WITHER_SKELETON) == 4, "wither skeleton IV");
            h.assertTrue(BroodRitual.eggClass(MobsRegistry.DECEIVER) == 5, "deceiver V");
            h.assertTrue(BroodRitual.eggClass(EntityTypes.WITHER) == 0 && BroodRitual.eggClass(EntityTypes.ENDER_DRAGON) == 0, "no Wither/Dragon eggs");
            h.assertTrue(BroodRitual.eggClass(BossMobs.VOID_RUNNER) == 0 && BroodRitual.eggClass(BossMobs.BRIMGUARD) == 0, "no boss eggs");
            h.assertTrue(BroodRitual.catalysts(3).equals(List.of(Items.SOUL_SAND, Items.LAPIS_LAZULI)), "III catalysts");
            h.assertTrue(Math.abs(BroodRitual.essenceChance(true, 0) - MobsConfig.essenceChance) < 1e-6 && BroodRitual.essenceChance(true, 3) > BroodRitual.essenceChance(true, 0), "chance + looting");
            ItemStack ess = MobEssenceItem.of(EntityTypes.ZOMBIE, 2);
            h.assertTrue(MobEssenceItem.species(ess) == EntityTypes.ZOMBIE && ItemStack.isSameItemSameComponents(ess, MobEssenceItem.of(EntityTypes.ZOMBIE, 1))
                    && !ItemStack.isSameItemSameComponents(ess, MobEssenceItem.of(EntityTypes.SKELETON, 1)), "essence keeps its species and stacks per species");
            // spawner / egg mobs are marked and never drop essence
            ServerLevel l = h.getLevel();
            floor(h, 0, 0, 4, 4);
            var fromSpawner = EntityTypes.ZOMBIE.spawn(l, h.absolutePos(new BlockPos(1, 2, 1)), EntitySpawnReason.SPAWNER);
            var natural = EntityTypes.ZOMBIE.spawn(l, h.absolutePos(new BlockPos(3, 2, 3)), EntitySpawnReason.NATURAL);
            h.assertTrue(fromSpawner != null && fromSpawner.entityTags().contains(BroodRitual.NO_ESSENCE), "spawner zombie is marked");
            h.assertTrue(natural != null && !natural.entityTags().contains(BroodRitual.NO_ESSENCE), "natural zombie is not");
            fromSpawner.discard();
            natural.discard();
            h.succeed();
        });
        add.accept("brood_ritual", h -> {
            ServerLevel l = h.getLevel();
            BlockPos c = new BlockPos(3, 1, 3);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                l.setBlock(h.absolutePos(c.offset(dx, 0, dz)), (dx == 0 && dz == 0 ? Blocks.PURPUR_BLOCK : Blocks.SOUL_SAND).defaultBlockState(), 3);
                if (dx != 0 && dz != 0) l.setBlock(h.absolutePos(c.offset(dx, 1, dz)), Blocks.SOUL_TORCH.defaultBlockState(), 3);
            }
            h.assertTrue(BroodRitual.figure(l, h.absolutePos(c)), "hollow complete");
            drop(h, c, new ItemStack(BossMobs.EGG_BLANK));
            drop(h, c, MobEssenceItem.of(EntityTypes.CHICKEN, 3));
            drop(h, c, new ItemStack(Items.WHEAT));
            BroodRitual.tryStart(l, h.absolutePos(c), null);
            h.assertTrue(h.getEntities(BossMobs.BROOD_RITUAL).isEmpty(), "3 of 4 essences: nothing starts");
            drop(h, c, MobEssenceItem.of(EntityTypes.CHICKEN, 1));
            h.assertTrue(BroodRitual.tryStart(l, h.absolutePos(c), null), "starts");
            var broods = h.getEntities(BossMobs.BROOD_RITUAL);
            h.assertTrue(broods.size() == 1 && broods.getFirst().species() == EntityTypes.CHICKEN && broods.getFirst().duration() == BroodRitual.TICKS[1], "one chicken brood, 30 s");
            h.assertTrue(BossRituals.count(BossRituals.offerings(l, h.absolutePos(c)), s -> !s.isEmpty()) == 0, "offerings taken");
            broods.getFirst().skipTo(2);
            h.runAfterDelay(5, () -> {
                h.assertTrue(h.getEntities(BossMobs.BROOD_RITUAL).isEmpty(), "brood done");
                h.assertTrue(BossRituals.count(BossRituals.offerings(l, h.absolutePos(c)), s -> s.is(Items.CHICKEN_SPAWN_EGG)) == 1, "a chicken spawn egg pops out");
                l.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(c)).inflate(3)).forEach(e -> e.discard());
                // second brood, then a torch goes out: everything comes back
                drop(h, c, new ItemStack(BossMobs.EGG_BLANK));
                drop(h, c, MobEssenceItem.of(EntityTypes.CHICKEN, 4));
                drop(h, c, new ItemStack(Items.WHEAT));
                h.assertTrue(BroodRitual.tryStart(l, h.absolutePos(c), null), "second brood starts");
                l.setBlock(h.absolutePos(c.offset(1, 1, 1)), Blocks.AIR.defaultBlockState(), 3);
                h.runAfterDelay(25, () -> {
                    h.assertTrue(h.getEntities(BossMobs.BROOD_RITUAL).isEmpty(), "brood stops without the torch");
                    var back = l.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(c)).inflate(3));
                    int ess = 0, blank = 0;
                    for (ItemEntity e : back) {
                        if (MobEssenceItem.species(e.getItem()) == EntityTypes.CHICKEN) ess += e.getItem().getCount();
                        if (e.getItem().is(BossMobs.EGG_BLANK)) blank += e.getItem().getCount();
                    }
                    h.assertTrue(ess == 4 && blank == 1, "essence and blank come back: " + ess + "/" + blank);
                    back.forEach(e -> e.discard());
                    h.succeed();
                });
            });
        });
    }

    private static LootParams params(ServerLevel l, LivingEntity e, net.minecraft.world.entity.player.Player killer) {
        return new LootParams.Builder(l).withParameter(LootContextParams.THIS_ENTITY, e).withParameter(LootContextParams.ORIGIN, e.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, l.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).create(LootContextParamSets.ENTITY);
    }
}
