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

    /** The Vanilla Wither figure at x 1..3, z 2, with {@code foot} under the T. */
    private static void figure(GameTestHelper h, net.minecraft.world.level.block.Block foot) {
        ServerLevel level = h.getLevel();
        level.setBlock(h.absolutePos(new BlockPos(2, 1, 2)), foot.defaultBlockState(), 3);
        level.setBlock(h.absolutePos(new BlockPos(2, 2, 2)), Blocks.SOUL_SAND.defaultBlockState(), 3);
        for (int x = 1; x <= 3; x++) level.setBlock(h.absolutePos(new BlockPos(x, 3, 2)), Blocks.SOUL_SOIL.defaultBlockState(), 3);
        for (int x = 1; x <= 3; x++) level.setBlock(h.absolutePos(new BlockPos(x, 4, 2)), Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 3);
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

    private static BiomeBrush.Result stroke(GameTestHelper h, ServerPlayer p, BlockPos rel) {
        return BiomeBrushItem.stroke(h.getLevel(), p, h.absolutePos(rel), p.getMainHandItem(), p.getOffhandItem());
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
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
            normal(h, () -> {
                BlightedWither w = still(h);
                h.assertTrue(w.getMaxHealth() == 600.0f && w instanceof WitherBoss, "a Wither with 600 health");
                w.setHealth(299);
                h.assertTrue(w.isPowered() && w.phase() == 2, "Vanilla shield (arrows bounce) from half health");
                w.discard();
            });
            h.succeed();
        });
        add.accept("wither_summon", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            figure(h, Blocks.NETHERITE_BLOCK);
            WitherSkullBlock.checkSpawn(level, h.absolutePos(new BlockPos(2, 4, 2)));
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).size() == 1, "Netherite foot: a Blighted Wither");
            h.assertTrue(h.getEntities(EntityTypes.WITHER).isEmpty(), "and no Vanilla Wither");
            h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(2, 1, 2))).isAir(), "the Netherite block is used up");
            h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(2, 4, 2))).isAir(), "the figure is used up");
            BlightedWither w = h.getEntities(WitherMobs.BLIGHTED_WITHER).getFirst();
            h.assertTrue(w.getInvulnerableTicks() == 220, "wakes like the Vanilla Wither");
            h.getEntities(WitherMobs.BLIGHTED_WITHER).forEach(e -> e.discard());
            figure(h, Blocks.STONE);
            WitherSkullBlock.checkSpawn(level, h.absolutePos(new BlockPos(2, 4, 2)));
            h.assertTrue(h.getEntities(WitherMobs.BLIGHTED_WITHER).isEmpty() && h.getEntities(EntityTypes.WITHER).size() == 1, "no Netherite: the Vanilla Wither");
            h.getEntities(EntityTypes.WITHER).forEach(e -> e.discard());
            boolean on = MobsConfig.witherSummonEnabled;
            MobsConfig.witherSummonEnabled = false;
            try {
                figure(h, Blocks.NETHERITE_BLOCK);
                h.assertFalse(BlightedPattern.trySummon(level, h.absolutePos(new BlockPos(2, 4, 2))), "config off: no Blighted Wither");
            } finally {
                MobsConfig.witherSummonEnabled = on;
            }
            h.succeed();
        }));
        add.accept("wither_drop", h -> normal(h, () -> {
            ServerLevel level = h.getLevel();
            floor(h, 0, 0, 6, 6);
            BlightedWither w = still(h);
            w.kill(level);
            var items = h.getEntities(EntityTypes.ITEM);
            int hearts = items.stream().map(ItemEntity::getItem).filter(s -> s.is(WitherMobs.HEART)).mapToInt(ItemStack::getCount).sum();
            boolean star = items.stream().anyMatch(i -> i.getItem().is(Items.NETHER_STAR));
            h.assertTrue(hearts == WitherLogic.HEARTS && hearts == 1, "exactly one Wither Heart: " + hearts);
            h.assertTrue(star, "and the Nether Star");
            items.forEach(e -> e.discard());
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
            // sand -> desert: the clicked cell and its neighbours change, one sand and one durability point are used
            ServerPlayer p = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SAND, 4));
            var first = stroke(h, p, at);
            h.assertTrue(first == BiomeBrush.Result.PAINTED, "stroke painted: " + first);
            h.assertTrue(cellBiome(h, at) == Biomes.DESERT, "clicked cell is desert: " + cellBiome(h, at));
            h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.DESERT, "neighbouring cell too (radius 1 cell)");
            h.assertTrue(p.getOffhandItem().getCount() == 3, "one material per stroke: " + p.getOffhandItem().getCount());
            h.assertTrue(p.getMainHandItem().getDamageValue() == 1, "one durability point per stroke");
            // a Nether material in the Overworld: refused, nothing used
            ServerPlayer q = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.NETHERRACK, 2));
            h.assertTrue(stroke(h, q, at) == BiomeBrush.Result.WRONG_DIMENSION && q.getOffhandItem().getCount() == 2, "Nether biome refused in the Overworld");
            ServerPlayer r = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.DIRT, 2));
            h.assertTrue(stroke(h, r, at) == BiomeBrush.Result.NO_MATERIAL && r.getMainHandItem().getDamageValue() == 0, "no biome material: nothing");
            // creative: no material, no durability
            ServerPlayer c = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SNOW_BLOCK, 1));
            c.getAbilities().instabuild = true;
            h.assertTrue(stroke(h, c, at) == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.SNOWY_PLAINS, "snow paints snowy plains");
            h.assertTrue(c.getOffhandItem().getCount() == 1 && c.getMainHandItem().getDamageValue() == 0, "creative uses nothing");
            // the last durability point breaks the brush
            ItemStack worn = new ItemStack(WitherMobs.BRUSH);
            worn.setDamageValue(worn.getMaxDamage() - 1);
            ServerPlayer b = painter(h, worn, new ItemStack(Items.SAND, 1));
            h.assertTrue(stroke(h, b, at) == BiomeBrush.Result.PAINTED && b.getMainHandItem().isEmpty(), "brush breaks at 0 durability");
            h.succeed();
        });
        add.accept("biome_brush_claims", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(3, 2, 3);
            ServerPlayer p = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SAND, 4));
            var first = stroke(h, p, at);
            h.assertTrue(first == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.DESERT, "unclaimed: painted: " + first + " " + cellBiome(h, at));
            String id = "simplemobs_test_claim";
            Object server = level.getServer();
            try {
                Protection.register(server, id, t -> false);
                ServerPlayer q = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.GRASS_BLOCK, 4));
                h.assertTrue(stroke(h, q, at) == BiomeBrush.Result.DENIED, "claimed: refused");
                h.assertTrue(cellBiome(h, at) == Biomes.DESERT, "claimed: biome unchanged");
                h.assertTrue(q.getOffhandItem().getCount() == 4 && q.getMainHandItem().getDamageValue() == 0, "claimed: nothing used");
                // a claim on part of the stroke: only the free cells change
                BlockPos claimed = h.absolutePos(at.offset(4, 0, 0));
                int cx = QuartPos.fromBlock(claimed.getX());
                Protection.unregister(server, id);
                Protection.register(server, id, t -> QuartPos.fromBlock(t.x()) != cx);
                ServerPlayer s = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.GRASS_BLOCK, 4));
                h.assertTrue(stroke(h, s, at) == BiomeBrush.Result.PAINTED && cellBiome(h, at) == Biomes.PLAINS, "free cells painted");
                h.assertTrue(cellBiome(h, at.offset(4, 0, 0)) == Biomes.DESERT, "claimed cell keeps its biome");
            } finally {
                Protection.unregister(server, id);
            }
            // adventure mode cannot paint
            ServerPlayer adv = painter(h, new ItemStack(WitherMobs.BRUSH), new ItemStack(Items.SNOW_BLOCK, 4));
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
