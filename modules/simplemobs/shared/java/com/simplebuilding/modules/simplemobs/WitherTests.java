package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.Protection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WitherSkullBlock;

/** Blighted Wither and Biome Brush GameTests (Fabric wrappers, NeoForge, Forge). Standalone-safe. */
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

    private static ResourceKey<Biome> cellBiome(GameTestHelper h, BlockPos rel) {
        BlockPos p = h.absolutePos(rel);
        return h.getLevel().getNoiseBiome(QuartPos.fromBlock(p.getX()), QuartPos.fromBlock(p.getY()), QuartPos.fromBlock(p.getZ()))
                .unwrapKey().orElseThrow();
    }

    /** A fresh survival-like player (mock players are creative; instabuild off means no infinite materials). */
    private static ServerPlayer painter(GameTestHelper h, ItemStack brush, ItemStack material) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getAbilities().instabuild = false;
        p.setItemInHand(InteractionHand.MAIN_HAND, brush);
        p.setItemInHand(InteractionHand.OFF_HAND, material);
        return p;
    }

    /** Test worlds are kept between runs: paint the area back to Plains first (two cells around, no checks). */
    private static void reset(GameTestHelper h, BlockPos rel) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        var plains = BiomeBrush.biome(h.getLevel(), Biomes.PLAINS.identifier());
        BiomeBrush.paint(h.getLevel(), p, h.absolutePos(rel), plains, 2);
        h.assertTrue(cellBiome(h, rel) == Biomes.PLAINS, "reset to plains");
    }

    private static BiomeBrush.Result stroke(GameTestHelper h, ServerPlayer p, BlockPos rel) {
        return BiomeBrushItem.stroke(h.getLevel(), p, h.absolutePos(rel), p.getMainHandItem(), p.getOffhandItem());
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
            for (var key : java.util.List.of(WitherMobs.HEART_KEY, WitherMobs.BRUSH_KEY))
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(key.identifier()), "item " + key.identifier());
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
            floor(h, 0, 0, 6, 6);
            for (WitherLogic.Stage stage : java.util.List.of(WitherLogic.NETHERITE, WitherLogic.ENDERITE)) {
                BlightedWither w = stage == WitherLogic.NETHERITE ? still(h) : h.spawn(WitherMobs.ABYSSAL_WITHER, 3, 2, 3);
                w.setNoAi(true);
                w.kill(level);
                var items = h.getEntities(EntityTypes.ITEM);
                int hearts = count(items, WitherMobs.HEART), stars = count(items, Items.NETHER_STAR);
                h.assertTrue(hearts == WitherLogic.HEARTS && hearts == 1, stage.metal() + ": exactly one Wither Heart: " + hearts);
                h.assertTrue(stars >= 1 && stars <= 2, stage.metal() + ": 1-2 Nether Stars: " + stars);
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
            }
            WitherBoss vanilla = h.spawn(EntityTypes.WITHER, 3, 2, 3);
            vanilla.setNoAi(true);
            vanilla.kill(level);
            h.assertTrue(h.getEntities(EntityTypes.ITEM).stream().noneMatch(i -> i.getItem().is(WitherMobs.HEART)), "the Vanilla Wither drops no heart");
            h.getEntities(EntityTypes.ITEM).forEach(e -> e.discard());
            h.getEntities(EntityTypes.EXPERIENCE_ORB).forEach(e -> e.discard());
            h.succeed();
        }));
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
        add.accept("biome_brush_item", h -> {
            ItemStack brush = new ItemStack(WitherMobs.BRUSH);
            h.assertTrue(brush.getMaxDamage() == 512, "durability 512");
            h.assertTrue(brush.isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT)), "repaired with Netherite Ingots");
            h.assertTrue(brush.get(DataComponents.ENCHANTABLE) != null
                    && brush.is(TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("enchantable/durability"))), "takes Unbreaking and Mending");
            var recipe = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "biome_brush")));
            h.assertTrue(recipe.isPresent(), "recipe heart + netherite ingot + brush");
            var entries = BiomeBrush.entries(h.getLevel().getServer());
            h.assertTrue(entries.size() >= 20, "biome materials loaded from data: " + entries.size());
            h.assertTrue(BiomeBrush.material(h.getLevel().getServer(), new ItemStack(Items.SAND)).map(e -> e.biome().equals(Biomes.DESERT.identifier())).orElse(false), "sand paints desert");
            h.assertTrue(BiomeBrush.material(h.getLevel().getServer(), new ItemStack(Items.DIRT)).isEmpty(), "dirt paints nothing");
            h.succeed();
        });
        add.accept("biome_brush_paint", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(3, 2, 3);
            level.setBlock(h.absolutePos(at), Blocks.STONE.defaultBlockState(), 3);
            reset(h, at);
            // snow -> snowy plains: the clicked cell and its neighbours change, one snow and one durability point are used
            ServerPlayer p = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SNOW_BLOCK, 4));
            var first = stroke(h, p, at);
            h.assertTrue(first == BiomeBrush.Result.PAINTED, "stroke painted: " + first + " (was " + cellBiome(h, at) + ")");
            h.assertTrue(cellBiome(h, at) == Biomes.SNOWY_PLAINS, "clicked cell is snowy plains: " + cellBiome(h, at));
            h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.SNOWY_PLAINS, "neighbouring cell too (radius 1 cell)");
            h.assertTrue(p.getOffhandItem().getCount() == 3, "one material per stroke: " + p.getOffhandItem().getCount());
            h.assertTrue(p.getMainHandItem().getDamageValue() == 1, "one durability point per stroke");
            ServerPlayer again = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SNOW_BLOCK, 4));
            h.assertTrue(stroke(h, again, at) == BiomeBrush.Result.UNCHANGED && again.getOffhandItem().getCount() == 4, "same biome again: nothing used");
            // a Nether material in the Overworld: refused, nothing used
            ServerPlayer q = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.NETHERRACK, 2));
            h.assertTrue(stroke(h, q, at) == BiomeBrush.Result.WRONG_DIMENSION && q.getOffhandItem().getCount() == 2, "Nether biome refused in the Overworld");
            ServerPlayer r = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.DIRT, 2));
            h.assertTrue(stroke(h, r, at) == BiomeBrush.Result.NO_MATERIAL && r.getMainHandItem().getDamageValue() == 0, "no biome material: nothing");
            // creative: no material, no durability
            ServerPlayer c = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SAND, 1));
            c.getAbilities().instabuild = true;
            h.assertTrue(stroke(h, c, at) == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.DESERT, "sand paints desert");
            h.assertTrue(c.getOffhandItem().getCount() == 1 && c.getMainHandItem().getDamageValue() == 0, "creative uses nothing");
            // the last durability point breaks the brush
            ItemStack worn = new ItemStack(WitherMobs.BRUSH);
            worn.setDamageValue(worn.getMaxDamage() - 1);
            ServerPlayer b = painter(h, worn, new ItemStack(Items.MYCELIUM, 1));
            h.assertTrue(stroke(h, b, at) == BiomeBrush.Result.PAINTED && b.getMainHandItem().isEmpty(), "brush breaks at 0 durability");
            h.succeed();
        });
        add.accept("biome_brush_claims", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(3, 2, 3);
            reset(h, at);
            ServerPlayer p = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SNOW_BLOCK, 4));
            var first = stroke(h, p, at);
            h.assertTrue(first == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.SNOWY_PLAINS, "unclaimed: painted: " + first + " " + cellBiome(h, at));
            String id = "simplemobs_test_claim";
            Object server = level.getServer();
            try {
                Protection.register(server, id, t -> false);
                ServerPlayer q = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.MYCELIUM, 4));
                h.assertTrue(stroke(h, q, at) == BiomeBrush.Result.DENIED, "claimed: refused");
                h.assertTrue(cellBiome(h, at) == Biomes.SNOWY_PLAINS, "claimed: biome unchanged");
                h.assertTrue(q.getOffhandItem().getCount() == 4 && q.getMainHandItem().getDamageValue() == 0, "claimed: nothing used");
                // a claim on part of the stroke: only the free cells change
                BlockPos claimed = h.absolutePos(at.offset(4, 0, 0));
                int cx = QuartPos.fromBlock(claimed.getX());
                Protection.unregister(server, id);
                Protection.register(server, id, t -> QuartPos.fromBlock(t.x()) != cx);
                ServerPlayer s = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.MYCELIUM, 4));
                h.assertTrue(stroke(h, s, at) == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.MUSHROOM_FIELDS, "free cells painted");
                h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.SNOWY_PLAINS, "claimed cell keeps its biome");
            } finally {
                Protection.unregister(server, id);
            }
            // adventure mode cannot paint
            ServerPlayer adv = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SAND, 4));
            adv.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            h.assertTrue(stroke(h, adv, at) == BiomeBrush.Result.DENIED, "adventure mode: refused");
            boolean ops = MobsConfig.biomeBrushOpsOnly;
            try {
                MobsConfig.biomeBrushOpsOnly = true;
                ServerPlayer o = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SNOW_BLOCK, 4));
                var res = stroke(h, o, at);
                h.assertTrue(res == BiomeBrush.Result.DISABLED || o.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER), "ops only: refused for players: " + res);
            } finally {
                MobsConfig.biomeBrushOpsOnly = ops;
            }
            h.succeed();
        });
    }
}
