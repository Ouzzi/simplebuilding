package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Blighted/Abyssal Wither and their loot GameTests (Fabric wrappers, NeoForge, Forge). Standalone-safe. */
final class WitherTests {
    private WitherTests() {}

    /** Runs a body on Normal difficulty (the Wither and its figure do nothing on Peaceful). */
    private static void normal(GameTestHelper h, Runnable body) {
        ServerLevel level = h.getLevel();
        Difficulty old = level.getDifficulty();
        if (old == Difficulty.PEACEFUL) level.getServer().setDifficulty(Difficulty.NORMAL, true);
        try {
            body.run();
        } finally {
            if (old == Difficulty.PEACEFUL) level.getServer().setDifficulty(old, true);
        }
    }

    private static void floor(GameTestHelper h, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    private static BlightedWither still(GameTestHelper h) {
        BlightedWither w = h.spawn(WitherMobs.BLIGHTED_WITHER, 3, 2, 3);
        w.setNoAi(true);
        return w;
    }

    private static LivingEntity pig(GameTestHelper h, int x, int z) {
        LivingEntity pig = h.spawn(EntityTypes.PIG, x, 2, z);
        if (pig instanceof net.minecraft.world.entity.Mob m) m.setNoAi(true);
        return pig;
    }

    private static final BlockPos LEFT = new BlockPos(1, 4, 2), MIDDLE = new BlockPos(2, 4, 2), RIGHT = new BlockPos(3, 4, 2);

    /** The Vanilla Wither T at x 1..3, z 2 (stone below), with wither skulls on the chosen places (no summon check). */
    private static void figure(GameTestHelper h, boolean left, boolean middle, boolean right) {
        ServerLevel level = h.getLevel();
        level.setBlock(h.absolutePos(new BlockPos(2, 1, 2)), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(h.absolutePos(new BlockPos(2, 2, 2)), Blocks.SOUL_SAND.defaultBlockState(), 3);
        for (int x = 1; x <= 3; x++) level.setBlock(h.absolutePos(new BlockPos(x, 3, 2)), Blocks.SOUL_SOIL.defaultBlockState(), 3);
        boolean[] skulls = {left, middle, right};
        for (int x = 1; x <= 3; x++)
            level.setBlock(h.absolutePos(new BlockPos(x, 4, 2)), (skulls[x - 1] ? Blocks.WITHER_SKELETON_SKULL : Blocks.AIR).defaultBlockState(), 3);
    }

    /** Places a wither skull like a player does: the block, then the Vanilla summon check. */
    private static void placeSkull(GameTestHelper h, BlockPos rel) {
        h.getLevel().setBlock(h.absolutePos(rel), Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 3);
        WitherSkullBlock.checkSpawn(h.getLevel(), h.absolutePos(rel));
    }

    /** A survival player right-clicks the skull with the helmet (the real item use, mixin included). Returns the hand. */
    private static ItemStack putHelmet(GameTestHelper h, BlockPos rel, ItemStack helmet) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getAbilities().instabuild = false;
        p.setItemInHand(InteractionHand.MAIN_HAND, helmet);
        BlockPos at = h.absolutePos(rel);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
        p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p, InteractionHand.MAIN_HAND, hit));
        return p.getMainHandItem();
    }

    /** The first Enderite Helmet of {@code #simplemobs:abyssal_wither_crowns}, or empty when no mod adds one. */
    private static ItemStack enderiteHelmet() {
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(WitherMobs.ABYSSAL_CROWNS)) return new ItemStack(holder);
        return ItemStack.EMPTY;
    }

    private static void clearBosses(GameTestHelper h) {
        h.getEntities(WitherMobs.BLIGHTED_WITHER).forEach(e -> e.discard());
        h.getEntities(WitherMobs.ABYSSAL_WITHER).forEach(e -> e.discard());
        h.getEntities(EntityTypes.WITHER).forEach(e -> e.discard());
        h.getEntities(EntityTypes.ITEM_DISPLAY).forEach(e -> e.discard());
        h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
    }

    private static int count(java.util.List<ItemEntity> items, Item item) {
        return items.stream().map(ItemEntity::getItem).filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int count(java.util.Collection<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** Air above the floor (y 2..6) of the test area: removes placed loot of earlier tests. */
    private static void clearAbove(GameTestHelper h) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(6, 6, 6))) h.getLevel().setBlock(h.absolutePos(p), Blocks.AIR.defaultBlockState(), 3);
    }

    /** How many of {@code block} stand in the 3 x 3 around {@code rel} (same height). */
    private static int around(GameTestHelper h, BlockPos rel, net.minecraft.world.level.block.Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(rel.offset(-1, 0, -1), rel.offset(1, 0, 1))) if (h.getLevel().getBlockState(h.absolutePos(p)).is(block)) n++;
        return n;
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("wither_bossbar_phases", h -> normal(h, () -> {
            BlightedWither w = h.spawn(WitherMobs.BLIGHTED_WITHER, 3, 2, 3);
            w.setInvulnerableTicks(0);
            var bar = ((com.simplebuilding.modules.simplemobs.mixin.WitherBossAccessor) (Object) w).simplemobs$bossEvent();
            h.runAfterDelay(5, () -> {
                h.assertTrue(bar.getOverlay() == net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_20, "notches on 50 % and 15 %");
                h.assertTrue(bar.getColor() == net.minecraft.world.BossEvent.BossBarColor.PURPLE && bar.getName().toString().contains("simplemobs.bossbar.wither.1"), "phase 1: purple, named");
                w.setHealth(w.getMaxHealth() * 0.1f);
            });
            h.runAfterDelay(15, () -> {
                h.assertTrue(w.phase() == 3, "phase 3 reached");
                h.assertTrue(bar.getColor() == net.minecraft.world.BossEvent.BossBarColor.RED && bar.getName().toString().contains("simplemobs.bossbar.wither.3"), "phase 3: red, named");
                w.discard();
                h.succeed();
            });
        }));
        add.accept("wither_values", h -> {
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(WitherMobs.BLIGHTED_WITHER_KEY.identifier()), "entity type");
            for (var key : java.util.List.of(WitherMobs.HEART_KEY, WitherMobs.GEM_KEY))
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(key.identifier()), "item " + key.identifier());
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(WitherMobs.HEART_BLOCK_KEY.identifier()) && BuiltInRegistries.BLOCK.containsKey(WitherMobs.GEM_BLOCK_KEY.identifier()), "placed forms");
            h.assertTrue(BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(MobsRegistry.ID, "biome_brush"))
                    .map(i -> !BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(MobsRegistry.ID)).orElse(true), "the Biome Brush moved to Simple Tools");
            var a = BlightedWither.createAttributes().build();
            h.assertTrue(a.getBaseValue(Attributes.MAX_HEALTH) == 600.0 && a.getBaseValue(Attributes.ARMOR) == 8.0, "600 health, armor 8 (Vanilla 300/4)");
            h.assertTrue(WitherLogic.phase(600, 600) == 1 && WitherLogic.phase(301, 600) == 1, "phase 1 above half");
            h.assertTrue(WitherLogic.phase(300, 600) == 2 && WitherLogic.phase(91, 600) == 2, "phase 2 from half");
            h.assertTrue(WitherLogic.phase(89, 600) == 3 && WitherLogic.phase(1, 600) == 3, "phase 3 below 15 %");
            h.assertTrue(WitherLogic.minionsToCall(0) == 3 && WitherLogic.minionsToCall(5) == 1 && WitherLogic.minionsToCall(9) == 0, "three minions, cap six");
            for (int warn : new int[] {WitherLogic.VOLLEY_WARN, WitherLogic.MINION_WARN, WitherLogic.NOVA_WARN, WitherLogic.WILT_WARN})
                h.assertTrue(warn >= 20, "every special attack is announced at least 1 s ahead: " + warn);
            var ab = BlightedWither.createAttributes(WitherLogic.ENDERITE).build();
            h.assertTrue(ab.getBaseValue(Attributes.MAX_HEALTH) == 1000.0 && ab.getBaseValue(Attributes.ARMOR) == 14.0, "Abyssal: 1000 health, armor 14");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(WitherMobs.ABYSSAL_WITHER_KEY.identifier()), "Abyssal entity type");
            h.assertTrue(WitherLogic.ENDERITE.interval(WitherLogic.NOVA_INTERVAL) < WitherLogic.NOVA_INTERVAL
                    && WitherLogic.ENDERITE.volleySkulls() == 3 && WitherLogic.ENDERITE.minionsToCall(0) == 4 && WitherLogic.ENDERITE.minionsToCall(7) == 1,
                    "Abyssal: shorter pauses, three-skull volley, four minions (cap eight)");
            h.assertTrue(WitherLogic.ENDERITE.xp() > WitherLogic.NETHERITE.xp(), "Abyssal: more experience");
            normal(h, () -> {
                BlightedWither w = still(h);
                h.assertTrue(w.getMaxHealth() == 600.0f && w instanceof WitherBoss, "a Wither with 600 health");
                w.setHealth(299);
                h.assertTrue(w.isPowered() && w.phase() == 2, "Vanilla shield (arrows bounce) from half health");
                w.discard();
                AbyssalWither abyssal = h.spawn(WitherMobs.ABYSSAL_WITHER, 3, 2, 3);
                abyssal.setNoAi(true);
                h.assertTrue(abyssal.getMaxHealth() == 1000.0f && abyssal.stage() == WitherLogic.ENDERITE, "an Abyssal Wither with 1000 health");
                abyssal.discard();
            });
            h.succeed();
        });
        add.accept("wither_summon", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            clearBosses(h);
            // Netherite: helmet on the middle skull, then the third skull
            figure(h, true, true, false);
            ItemStack hand = putHelmet(h, MIDDLE, new ItemStack(Items.NETHERITE_HELMET));
            h.assertTrue(hand.isEmpty(), "the helmet is put on (used up)");
            h.assertTrue(BlightedPattern.crownAt(level, h.absolutePos(MIDDLE)) != null, "the helmet sits on the middle skull");
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).isEmpty(), "nothing yet with two skulls");
            placeSkull(h, RIGHT);
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).size() == 1, "Netherite Helmet: a Blighted Wither");
            h.assertTrue(h.getEntities(EntityTypes.WITHER).isEmpty() && h.getEntities(WitherMobs.ABYSSAL_WITHER).isEmpty(), "and no other Wither");
            h.assertTrue(BlightedPattern.crownAt(level, h.absolutePos(MIDDLE)) == null, "the helmet is used up");
            h.assertTrue(level.getBlockState(h.absolutePos(MIDDLE)).isAir(), "the figure is used up");
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).getFirst().getInvulnerableTicks() == 220, "wakes like the Vanilla Wither");
            clearBosses(h);
            // Enderite (only with a mod that adds an Enderite Helmet): the Abyssal Wither
            ItemStack enderite = enderiteHelmet();
            if (!enderite.isEmpty()) {
                figure(h, false, true, true);
                h.assertTrue(putHelmet(h, MIDDLE, enderite).isEmpty(), "Enderite Helmet put on");
                placeSkull(h, LEFT);
                h.assertTrue(h.getEntities(WitherMobs.ABYSSAL_WITHER).size() == 1 && h.getEntities(WitherMobs.BLIGHTED_WITHER).isEmpty(), "Enderite Helmet: the Abyssal Wither");
                h.assertTrue(h.getEntities(WitherMobs.ABYSSAL_WITHER).getFirst().getMaxHealth() == 1000.0f, "with 1000 health");
                clearBosses(h);
            } else {
                h.assertTrue(BlightedPattern.stageOf(new ItemStack(Items.DIAMOND_HELMET)) == null, "no Enderite mod: only Netherite crowns");
            }
            // a side skull refuses the helmet: smoke, the helmet stays in hand
            figure(h, true, true, false);
            h.assertTrue(putHelmet(h, LEFT, new ItemStack(Items.NETHERITE_HELMET)).is(Items.NETHERITE_HELMET), "side skull: the helmet stays in hand");
            h.assertTrue(BlightedPattern.crownAt(level, h.absolutePos(LEFT)) == null, "side skull: no crown");
            // no helmet: the Vanilla Wither
            placeSkull(h, RIGHT);
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).isEmpty() && h.getEntities(EntityTypes.WITHER).size() == 1, "no helmet: the Vanilla Wither");
            clearBosses(h);
            // the skull breaks: the helmet falls out again
            figure(h, false, true, false);
            h.assertTrue(BlightedPattern.crown(level, h.absolutePos(MIDDLE), new ItemStack(Items.NETHERITE_HELMET)), "crowned");
            var crown = BlightedPattern.crownAt(level, h.absolutePos(MIDDLE));
            level.setBlock(h.absolutePos(MIDDLE), Blocks.AIR.defaultBlockState(), 3);
            BlightedPattern.tickCrown(level, crown);
            h.assertTrue(crown.isRemoved() && count(h.getEntities(EntityTypes.ITEM), Items.NETHERITE_HELMET) == 1, "broken skull: the helmet drops");
            clearBosses(h);
            boolean on = MobsConfig.witherSummonEnabled;
            MobsConfig.witherSummonEnabled = false;
            try {
                figure(h, true, true, false);
                h.assertFalse(BlightedPattern.canCrown(level, h.absolutePos(MIDDLE)), "config off: no helmet on the skull");
            } finally {
                MobsConfig.witherSummonEnabled = on;
            }
            clearBosses(h);
            h.succeed();
        }));
        add.accept("wither_order", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            clearBosses(h);
            // the helmet must sit before the third skull: on a finished figure it is refused
            figure(h, true, true, true);
            h.assertFalse(BlightedPattern.canCrown(level, h.absolutePos(MIDDLE)), "complete figure: too late for the helmet");
            h.assertTrue(putHelmet(h, MIDDLE, new ItemStack(Items.NETHERITE_HELMET)).is(Items.NETHERITE_HELMET), "complete figure: the helmet stays in hand");
            // middle skull alone: the helmet goes on, the second skull does nothing, the third summons
            figure(h, false, true, false);
            h.assertTrue(putHelmet(h, MIDDLE, new ItemStack(Items.NETHERITE_HELMET)).isEmpty(), "middle skull alone takes the helmet");
            h.assertFalse(BlightedPattern.canCrown(level, h.absolutePos(MIDDLE)), "only one helmet per skull");
            placeSkull(h, LEFT);
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).isEmpty() && h.getEntities(EntityTypes.WITHER).isEmpty(), "second skull: nothing yet");
            h.assertTrue(BlightedPattern.crownAt(level, h.absolutePos(MIDDLE)) != null, "the helmet stays on");
            placeSkull(h, RIGHT);
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).size() == 1, "third skull: the Blighted Wither");
            clearBosses(h);
            h.succeed();
        }));
        add.accept("wither_minion_armor", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            floor(h, 0, 0, 7, 7);
            var spot = net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6, 2, 6)));
            BlightedWither w = still(h);
            w.setHealth(w.getMaxHealth() * 0.5f);
            var plain = (net.minecraft.world.entity.Mob) w.summonMinion(level, spot);
            // above 30 %: Vanilla equipment (random, so only the rule is checked)
            h.assertTrue(plain != null && !WitherLogic.armoredMinions(w.getHealth(), w.getMaxHealth())
                    && WitherLogic.armoredMinions(w.getMaxHealth() * 0.3f, w.getMaxHealth()), "armored from 30 % health");
            w.setHealth(w.getMaxHealth() * 0.2f);
            var armored = (net.minecraft.world.entity.Mob) w.summonMinion(level, spot);
            h.assertTrue(armored.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                    && armored.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                    && armored.getMainHandItem().is(Items.IRON_SWORD), "low health: iron armor and sword");
            w.discard();
            AbyssalWither a = h.spawn(WitherMobs.ABYSSAL_WITHER, 3, 2, 3);
            a.setNoAi(true);
            a.setHealth(a.getMaxHealth() * 0.2f);
            var strong = (net.minecraft.world.entity.Mob) a.summonMinion(level, spot);
            h.assertTrue(strong.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)
                    && strong.getMainHandItem().is(Items.DIAMOND_SWORD), "Abyssal at low health: diamond armor and sword");
            for (var minion : java.util.List.of(plain, armored, strong)) {
                minion.kill(level);
            }
            h.assertTrue(h.getEntities(EntityTypes.ITEM).stream().noneMatch(i -> i.getItem().is(Items.IRON_CHESTPLATE) || i.getItem().is(Items.DIAMOND_CHESTPLATE)
                    || i.getItem().is(Items.WITHER_SKELETON_SKULL)), "minions drop nothing (no armor farming)");
            a.discard();
            h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
            h.getEntities(EntityTypes.EXPERIENCE_ORB).forEach(e -> e.discard());
            h.succeed();
        }));
        add.accept("wither_nova_push", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            floor(h, 0, 0, 7, 7);
            BlightedWither w = still(h);
            LivingEntity pig = pig(h, 6, 3);
            w.setHealth(w.getMaxHealth() * 0.1f);
            h.assertTrue(w.phase() == 3, "below 15 %: phase 3 (nova)");
            pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            h.assertTrue(w.nova(level) >= 1, "the pig is hit");
            double push = pig.getDeltaMovement().horizontalDistance();
            h.assertTrue(push >= 2.5 && pig.getDeltaMovement().x > 0, "strong knockback away from the Wither: " + push);
            w.discard();
            AbyssalWither a = h.spawn(WitherMobs.ABYSSAL_WITHER, 3, 2, 3);
            a.setNoAi(true);
            a.setHealth(a.getMaxHealth() * 0.1f);
            pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            pig.teleportTo(h.absolutePos(new BlockPos(6, 2, 3)).getX() + 0.5, pig.getY(), h.absolutePos(new BlockPos(6, 2, 3)).getZ() + 0.5);
            a.nova(level);
            h.assertTrue(pig.getDeltaMovement().horizontalDistance() > push, "Abyssal: stronger still: " + pig.getDeltaMovement().horizontalDistance());
            a.discard();
            pig.discard();
            h.succeed();
        }));
        add.accept("wither_drop", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            clearBosses(h);
            floor(h, 0, 0, 6, 6);
            clearAbove(h);
            // stage 2 (Blighted): Nether Heart (placed later), 1-2 Nether Stars, nuggets; stage 3 (Abyssal): heart + gem, no star
            for (WitherLogic.Stage stage : java.util.List.of(WitherLogic.NETHERITE, WitherLogic.ENDERITE)) {
                BlightedWither w = stage == WitherLogic.NETHERITE ? still(h) : h.spawn(WitherMobs.ABYSSAL_WITHER, 3, 2, 3);
                w.setNoAi(true);
                w.kill(level);
                var items = h.getEntities(EntityTypes.ITEM);
                int stars = count(items, Items.NETHER_STAR);
                if (stage == WitherLogic.NETHERITE) h.assertTrue(stars >= 1 && stars <= 2, "Blighted: 1-2 Nether Stars: " + stars);
                else h.assertTrue(stars == 0, "Abyssal: no Nether Star: " + stars);
                h.assertTrue(count(items, WitherMobs.HEART) == 0 && count(items, WitherMobs.GEM) == 0, stage.metal() + ": heart and gem wait for the end of the death");
                int hearts = count(w.placeLater(), WitherMobs.HEART), gems = count(w.placeLater(), WitherMobs.GEM);
                h.assertTrue(hearts == WitherLogic.HEARTS && hearts == 1, stage.metal() + ": exactly one Nether Heart: " + hearts);
                h.assertTrue(gems == stage.gems() && gems == (stage == WitherLogic.ENDERITE ? 1 : 0), stage.metal() + ": Nether Gem only at stage 3: " + gems);
                h.assertTrue(count(items, Items.WITHER_SKELETON_SKULL) == 0, "no skull");
                ItemStack nugget = BlightedWither.nuggets(stage, 1);
                if (nugget.isEmpty()) {
                    h.assertTrue(stage == WitherLogic.ENDERITE, "only Enderite nuggets may be missing");
                } else if (nugget.is(Items.NETHERITE_SCRAP)) {
                    h.assertTrue(count(items, Items.NETHERITE_SCRAP) == 1, "no Netherite nugget mod: one Netherite Scrap");
                } else {
                    int n = count(items, nugget.getItem());
                    h.assertTrue(n >= WitherLogic.NUGGETS_MIN && n <= WitherLogic.NUGGETS_MAX, stage.metal() + ": 3-5 nuggets: " + n);
                }
                items.forEach(e -> e.discard());
                h.getEntities(EntityTypes.EXPERIENCE_ORB).forEach(e -> e.discard());
                w.discard(); // ends the death at once: the loot is placed now
                BlockState heart = level.getBlockState(h.absolutePos(new BlockPos(3, 2, 3)));
                h.assertTrue(heart.is(WitherMobs.HEART_BLOCK), stage.metal() + ": the Nether Heart is placed under the Wither: " + heart);
                if (stage == WitherLogic.ENDERITE) {
                    h.assertTrue(around(h, new BlockPos(3, 2, 3), WitherMobs.GEM_BLOCK) == 1, "Abyssal: the Nether Gem is placed next to it");
                } else {
                    h.assertTrue(around(h, new BlockPos(3, 2, 3), WitherMobs.GEM_BLOCK) == 0, "Blighted: no gem");
                }
                h.assertTrue(count(h.getEntities(EntityTypes.ITEM), WitherMobs.HEART) == 0, "placed, not dropped");
                clearAbove(h);
            }
            WitherBoss vanilla = h.spawn(EntityTypes.WITHER, 3, 2, 3);
            vanilla.setNoAi(true);
            vanilla.kill(level);
            h.assertTrue(h.getEntities(EntityTypes.ITEM).stream().noneMatch(i -> i.getItem().is(WitherMobs.HEART) || i.getItem().is(WitherMobs.GEM)), "the Vanilla Wither drops no heart");
            h.assertTrue(count(h.getEntities(EntityTypes.ITEM), Items.NETHER_STAR) == 1, "the Vanilla Wither keeps its Nether Star");
            vanilla.discard();
            h.assertTrue(around(h, new BlockPos(3, 2, 3), WitherMobs.HEART_BLOCK) == 0, "and places nothing");
            h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
            h.getEntities(EntityTypes.EXPERIENCE_ORB).forEach(e -> e.discard());
            h.succeed();
        }));
        add.accept("wither_death_places_loot", h -> normal(h, () -> {
            // the real death: the heart appears only once the death animation has ended (20 ticks)
            ServerLevel level = h.getLevel();
            clearBosses(h);
            floor(h, 0, 0, 6, 6);
            clearAbove(h);
            BlightedWither w = still(h);
            w.kill(level);
            h.runAfterDelay(5, () -> {
                if (!w.isRemoved()) h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(3, 2, 3))).isAir(), "not yet during the death");
            });
            h.runAfterDelay(30, () -> {
                h.assertTrue(w.isRemoved(), "the death has ended");
                h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(3, 2, 3))).is(WitherMobs.HEART_BLOCK), "then the Nether Heart is placed");
                clearAbove(h);
                h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
                h.getEntities(EntityTypes.EXPERIENCE_ORB).forEach(e -> e.discard());
                h.succeed();
            });
        }));
        add.accept("wither_loot_place", h -> {
            ServerLevel level = h.getLevel();
            floor(h, 0, 0, 6, 6);
            clearAbove(h);
            Vec3 above = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 6, 3)));
            // ground below: both blocks are set, side by side, on the floor
            var placed = WitherLoot.place(level, above, java.util.List.of(new ItemStack(WitherMobs.HEART), new ItemStack(WitherMobs.GEM)));
            h.assertTrue(placed.size() == 2 && !placed.get(0).equals(placed.get(1)), "two spots: " + placed);
            h.assertTrue(level.getBlockState(placed.get(0)).is(WitherMobs.HEART_BLOCK) && level.getBlockState(placed.get(1)).is(WitherMobs.GEM_BLOCK), "heart and gem placed");
            h.assertTrue(placed.get(0).equals(h.absolutePos(new BlockPos(3, 2, 3))), "straight under the Wither, on the floor: " + placed.get(0));
            // blast resistant blocks: an explosion leaves them, broken they give the item back (obsidian floor keeps their support)
            for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) level.setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.OBSIDIAN.defaultBlockState(), 3);
            h.assertTrue(WitherMobs.HEART_BLOCK.getExplosionResistance() >= 1200.0F && WitherMobs.GEM_BLOCK.getExplosionResistance() >= 1200.0F, "resistance 1200");
            Vec3 boom = Vec3.atCenterOf(placed.get(0).above());
            level.explode(null, boom.x, boom.y, boom.z, 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            h.assertTrue(level.getBlockState(placed.get(0)).is(WitherMobs.HEART_BLOCK) && level.getBlockState(placed.get(1)).is(WitherMobs.GEM_BLOCK), "explosion: both blocks stay");
            h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
            level.destroyBlock(placed.get(0), true);
            h.assertTrue(count(h.getEntities(EntityTypes.ITEM), WitherMobs.HEART) == 1, "broken: the Nether Heart item");
            clearAbove(h);
            h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
            floor(h, 0, 0, 6, 6);
            // no ground below (high in the air): items instead, explosion and fire proof, long-lived
            Vec3 sky = new Vec3(above.x, level.getMaxY() - 2, above.z);
            h.assertTrue(WitherLoot.ground(level, BlockPos.containing(sky)) == null, "no ground within " + WitherLoot.SEARCH_DEPTH + " blocks");
            h.assertTrue(WitherLoot.place(level, sky, java.util.List.of(new ItemStack(WitherMobs.HEART), new ItemStack(WitherMobs.GEM))).isEmpty(), "nothing placed");
            var drops = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(sky, sky).inflate(2.0));
            h.assertTrue(count(drops, WitherMobs.HEART) == 1 && count(drops, WitherMobs.GEM) == 1, "heart and gem dropped as items: " + drops.size());
            drops.forEach(e -> e.discard());
            // the items survive an explosion that destroys an ordinary item
            Vec3 at = Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 3)));
            ItemEntity heart = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(WitherMobs.HEART));
            ItemEntity gem = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(WitherMobs.GEM));
            ItemEntity dirt = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.DIRT));
            for (ItemEntity e : java.util.List.of(heart, gem, dirt)) level.addFreshEntity(e);
            level.explode(null, at.x, at.y, at.z, 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            h.assertTrue(dirt.isRemoved(), "the explosion destroys an ordinary item");
            h.assertTrue(!heart.isRemoved() && !gem.isRemoved(), "Nether Heart and Nether Gem are explosion proof");
            h.assertTrue(heart.getItem().has(DataComponents.DAMAGE_RESISTANT) && gem.getItem().has(DataComponents.DAMAGE_RESISTANT), "damage resistant component");
            heart.discard();
            gem.discard();
            h.succeed();
        });
        add.accept("wither_heart_moved", h -> {
            // worlds from before 21.10.: the Wither Heart id finds the Nether Heart (SimpleLib IdMoves)
            var old = BuiltInRegistries.ITEM.getValue(WitherLoot.OLD_HEART);
            h.assertTrue(old == WitherMobs.HEART, "simplemobs:wither_heart -> simplemobs:nether_heart: " + old);
            h.assertTrue(BuiltInRegistries.ITEM.getKey(WitherMobs.HEART).equals(WitherLoot.HEART), "saved under the new id");
            h.succeed();
        });
        add.accept("nether_heart_shape", h -> {
            // owner 21.10. (b-rarity21b): the placed heart is heart-shaped and turns its front to the placer / nearest player
            BlockState north = WitherMobs.HEART_BLOCK.defaultBlockState();
            h.assertTrue(WitherMobs.HEART_BLOCK instanceof NetherHeartBlock && north.getValue(NetherHeartBlock.FACING) == net.minecraft.core.Direction.NORTH,
                    "facing, north by default (old worlds)");
            var box = north.getShape(h.getLevel(), BlockPos.ZERO).bounds();
            h.assertTrue(box.getXsize() > 0.74 && box.getYsize() > 0.68 && box.getZsize() < 0.51 && box.minY == 0.0,
                    "heart outline 12 wide, 11 high, 8 deep, on the ground: " + box);
            var east = north.setValue(NetherHeartBlock.FACING, net.minecraft.core.Direction.EAST).getShape(h.getLevel(), BlockPos.ZERO).bounds();
            h.assertTrue(Math.abs(east.getXsize() - box.getZsize()) < 1.0E-6 && Math.abs(east.getZsize() - box.getXsize()) < 1.0E-6,
                    "the outline turns with the heart: " + east);
            h.assertTrue(north.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90).getValue(NetherHeartBlock.FACING)
                    == net.minecraft.core.Direction.EAST, "structure rotation turns it");
            // placing: the front looks back at the player (yaw 0 looks south -> front north, yaw 90 looks west -> front east)
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at.below(), false);
            for (float yaw : new float[] {0f, 90f, 180f, 270f}) {
                player.setYRot(yaw);
                BlockState placed = WitherMobs.HEART_BLOCK.getStateForPlacement(
                        new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(WitherMobs.HEART), hit));
                h.assertTrue(placed != null && placed.getValue(NetherHeartBlock.FACING) == player.getDirection().getOpposite(),
                        "yaw " + yaw + ": front to the player, got " + placed);
            }
            // after the fight: towards the nearest player, north without one
            BlockPos c = new BlockPos(0, 64, 0);
            h.assertTrue(NetherHeartBlock.facing(c, new Vec3(9.5, 64, 1.0)) == net.minecraft.core.Direction.EAST
                    && NetherHeartBlock.facing(c, new Vec3(0.2, 70, -6)) == net.minecraft.core.Direction.NORTH
                    && NetherHeartBlock.facing(c, new Vec3(-4, 64, 0.5)) == net.minecraft.core.Direction.WEST
                    && NetherHeartBlock.facing(c, new Vec3(1, 64, 12)) == net.minecraft.core.Direction.SOUTH
                    && NetherHeartBlock.facing(c, null) == net.minecraft.core.Direction.NORTH, "loot faces the nearest player");
            h.succeed();
        });
        add.accept("nether_loot_rarity", h -> {
            // owner 21.10.: Nether Heart and Red Nether Gem are cursed (SimpleLib tier above Epic, Vanilla Epic as fallback)
            for (Item item : java.util.List.of(WitherMobs.HEART, WitherMobs.GEM)) {
                ItemStack stack = new ItemStack(item);
                h.assertTrue(com.simplelib.api.SimpleRarity.of(stack) == com.simplelib.api.SimpleRarity.CURSED, "cursed: " + item);
                h.assertTrue(stack.getRarity() == net.minecraft.world.item.Rarity.EPIC, "Epic fallback: " + item);
                h.assertTrue(stack.getHoverName().getStyle().getColor().getValue() == com.simplelib.api.SimpleRarity.CURSED.color(), "red name: " + item);
            }
            h.assertTrue(new ItemStack(WitherMobs.GEM).getHoverName().getString().equals("Red Nether Gem")
                    || !net.minecraft.locale.Language.getInstance().has("block.simplemobs.nether_gem"), "renamed to Red Nether Gem");
            h.succeed();
        });
        add.accept("wither_attacks", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            floor(h, 0, 0, 7, 7);
            BlightedWither w = still(h);
            LivingEntity pig = pig(h, 6, 6);
            // nova (phase 3): the ring warns 2 s, nothing hits before, the pig inside is hit after
            w.setHealth(60);
            w.announce(level, BlightedWither.Attack.NOVA);
            h.assertTrue(w.pending() == BlightedWither.Attack.NOVA && w.pendingTicks() == WitherLogic.NOVA_WARN, "nova announced");
            for (int i = 1; i < WitherLogic.NOVA_WARN; i++) w.tickAbilities(level);
            h.assertFalse(pig.hasEffect(MobEffects.WITHER), "nothing hits during the warning");
            w.tickAbilities(level);
            h.assertTrue(w.pending() == null && pig.hasEffect(MobEffects.WITHER) && pig.getEffect(MobEffects.WITHER).getAmplifier() == 1, "after the warning: Wither II");
            // volley: two skulls at once after the warning
            w.setTarget(pig);
            int skulls = h.getEntities(EntityTypes.WITHER_SKULL).size();
            w.announce(level, BlightedWither.Attack.VOLLEY);
            for (int i = 1; i < WitherLogic.VOLLEY_WARN; i++) w.tickAbilities(level);
            h.assertTrue(h.getEntities(EntityTypes.WITHER_SKULL).size() == skulls, "no skull during the warning");
            w.tickAbilities(level);
            h.assertTrue(h.getEntities(EntityTypes.WITHER_SKULL).size() == skulls + WitherLogic.VOLLEY_SKULLS, "twin volley: two skulls at once");
            h.getEntities(EntityTypes.WITHER_SKULL).forEach(e -> e.discard());
            // minions: marked spots first, then up to three Wither Skeletons
            w.announce(level, BlightedWither.Attack.MINIONS);
            for (int i = 1; i < WitherLogic.MINION_WARN; i++) w.tickAbilities(level);
            h.assertTrue(h.getEntities(EntityTypes.WITHER_SKELETON).isEmpty(), "no minion during the warning");
            w.tickAbilities(level);
            int minions = h.getEntities(EntityTypes.WITHER_SKELETON).size();
            h.assertTrue(minions >= 1 && minions <= WitherLogic.MINIONS, "minions rise: " + minions);
            h.getEntities(EntityTypes.WITHER_SKELETON).forEach(e -> e.discard());
            pig.discard();
            w.discard();
            h.succeed();
        }));
        add.accept("wither_wilt", h -> {
            var natural = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
            var placed = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            h.assertTrue(BlightedWither.wilted(natural, false).isAir(), "natural leaves fall");
            h.assertTrue(BlightedWither.wilted(placed, false) == null, "placed leaves stay (builds)");
            h.assertTrue(BlightedWither.wilted(Blocks.SHORT_GRASS.defaultBlockState(), false).is(Blocks.DEAD_BUSH), "grass wilts to a dead bush");
            h.assertTrue(BlightedWither.wilted(Blocks.POPPY.defaultBlockState(), false).is(Blocks.DEAD_BUSH), "flowers wilt");
            h.assertTrue(BlightedWither.wilted(Blocks.GRASS_BLOCK.defaultBlockState(), true).is(Blocks.COARSE_DIRT)
                    && BlightedWither.wilted(Blocks.GRASS_BLOCK.defaultBlockState(), false) == null, "some grass blocks die");
            for (var b : java.util.List.of(Blocks.STONE, Blocks.OAK_PLANKS, Blocks.WHEAT, Blocks.OAK_SAPLING, Blocks.WITHER_ROSE))
                h.assertTrue(BlightedWither.wilted(b.defaultBlockState(), true) == null, "stays: " + b);
            normal(h, () -> {
                ServerLevel level = h.getLevel();
                level.setBlock(h.absolutePos(new BlockPos(5, 1, 5)), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(h.absolutePos(new BlockPos(5, 2, 5)), Blocks.SHORT_GRASS.defaultBlockState(), 3);
                level.setBlock(h.absolutePos(new BlockPos(6, 2, 5)), placed, 3);
                BlightedWither w = still(h);
                boolean griefing = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
                boolean on = MobsConfig.witherWilt;
                try {
                    MobsConfig.witherWilt = false;
                    h.assertTrue(w.wilt(level) == 0, "config off: nothing wilts");
                    MobsConfig.witherWilt = true;
                    int n = w.wilt(level);
                    if (griefing) {
                        h.assertTrue(n >= 1 && level.getBlockState(h.absolutePos(new BlockPos(5, 2, 5))).is(Blocks.DEAD_BUSH), "grass wilted: " + n);
                    } else {
                        h.assertTrue(n == 0, "mobGriefing off: nothing wilts");
                    }
                    h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(6, 2, 5))).is(Blocks.OAK_LEAVES), "placed leaves stay");
                } finally {
                    MobsConfig.witherWilt = on;
                    w.discard();
                }
            });
            h.succeed();
        });
    }
}
