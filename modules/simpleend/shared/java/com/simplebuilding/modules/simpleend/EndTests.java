package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.block.EndBudBlock;
import com.simplebuilding.modules.simpleend.block.EndLeavesBlock;
import com.simplebuilding.modules.simpleend.block.EndSticksBlock;
import com.simplebuilding.modules.simpleend.mixin.FireBlockInvoker;
import com.simplebuilding.modules.simpleend.world.EndBiomes;
import com.simplebuilding.modules.simpleend.world.EndFlora;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Test bodies shared by the Fabric @GameTest wrappers and the NeoForge/Forge registration (structure simpleend:grove). */
public final class EndTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private EndTests() {}

    private static void add(String name, Consumer<GameTestHelper> body) { ALL.put(name, body); }

    private static final BlockPos BUD = new BlockPos(12, 2, 12);

    private static void ground(GameTestHelper h, BlockPos at, int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) h.setBlock(at.offset(dx, -1, dz), Blocks.END_STONE);
    }

    private static int count(GameTestHelper h, Block block, BlockPos from, BlockPos to) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(from, to)) if (h.getBlockState(p).is(block)) n++;
        return n;
    }

    /** A partner mod is there when its namespace has registered items (no loader call in shared code). */
    private static boolean loaded(String namespace) {
        return BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals(namespace));
    }

    /** Removes a grown tree again (tests in the sky must not leave it for later batches). */
    private static void clear(GameTestHelper h, com.simplebuilding.modules.simpleend.world.EndTrees.Grown grown, BlockPos groundRel, int radius) {
        ServerLevel level = h.getLevel();
        for (BlockPos p : grown.leaves()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
        for (BlockPos p : grown.wood()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) level.setBlock(h.absolutePos(groundRel.offset(dx, -1, dz)), Blocks.AIR.defaultBlockState(), 18);
    }

    /** True when the block's shape reaches the face towards {@code d}. */
    private static boolean reaches(BlockState s, BlockPos p, ServerLevel level, Direction d) {
        var box = s.getShape(level, p).bounds();
        return d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.max(d.getAxis()) >= 1.0 : box.min(d.getAxis()) <= 0.0;
    }

    private static int countAbs(ServerLevel level, java.util.List<BlockPos> list, Block block) {
        int n = 0;
        for (BlockPos p : list) if (level.getBlockState(p).is(block)) n++;
        return n;
    }

    private static void waitForLight(GameTestHelper h, BlockPos rel, boolean bright, Runnable then) {
        h.succeedWhen(() -> {
            h.assertTrue(EndBudBlock.bright(h.getLevel(), h.absolutePos(rel)) == bright, "light around the bud not settled yet");
            then.run();
        });
    }

    static {
        add("guide_book", com.simplebuilding.modules.simpleend.guide.EndGuide::gameTest);
        add("registered", h -> {
            for (EndWood w : EndWood.ALL) {
                h.assertTrue(w.blockIds.size() == 22, w.name + ": 22 blocks, got " + w.blockIds.size());
                h.assertTrue(w.itemIds.size() == 21, w.name + ": 21 items, got " + w.itemIds.size());
                h.assertTrue(BlockEntityTypes.SHELF.isValid(w.shelf.defaultBlockState()), w.name + " shelf block entity");
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleEnd.id(w.id("W_boat"))), w.name + " boat entity");
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleEnd.id(w.id("W_chest_boat"))), w.name + " chest boat entity");
                h.assertTrue(BlockEntityTypes.SIGN.isValid(w.sign.defaultBlockState()) && BlockEntityTypes.SIGN.isValid(w.wallSign.defaultBlockState()), w.name + " sign block entity");
                h.assertTrue(BlockEntityTypes.HANGING_SIGN.isValid(w.hangingSign.defaultBlockState()), w.name + " hanging sign block entity");
            }
            h.assertTrue(BuiltInRegistries.FEATURE_TYPE.containsKey(SimpleEnd.id("end_flora")), "feature type end_flora");
            var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
            h.assertTrue(biomes.get(EndBiomes.ASH_VALLEY).isPresent() && biomes.get(EndBiomes.DAWNLANDS).isPresent(), "Ash Valley and Dawnlands biomes");
            h.succeed();
        });
        add("wood_tags_and_recipes", h -> {
            var recipes = h.getLevel().recipeAccess();
            for (EndWood w : EndWood.ALL) {
                h.assertTrue(w.planks.defaultBlockState().is(BlockTags.PLANKS), "planks tag");
                h.assertTrue(w.log.defaultBlockState().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("logs_that_burn"))) && w.log.defaultBlockState().is(BlockTags.LOGS), "logs tags");
                h.assertTrue(w.leaves.defaultBlockState().is(BlockTags.LEAVES), "leaves tag");
                h.assertTrue(new ItemStack(w.boat).is(ItemTags.BOATS) && new ItemStack(w.chestBoat).is(ItemTags.CHEST_BOATS), "boat tags");
                h.assertTrue(new ItemStack(w.itemIds.get(w.id("W_sign"))).is(ItemTags.SIGNS), "sign tag");
                h.assertTrue(new ItemStack(w.log).is(EndRegistry.VOID_SAFE), "void-safe tag");
                h.assertTrue(w.shelf.defaultBlockState().is(net.minecraft.tags.TagKey.create(Registries.BLOCK,
                        net.minecraft.resources.Identifier.withDefaultNamespace("wooden_shelves"))), "wooden shelves tag");
                for (String r : new String[] {"W_planks", "W_wood", "stripped_W_wood", "W_stairs", "W_slab", "W_fence", "W_fence_gate", "W_door",
                        "W_trapdoor", "W_button", "W_pressure_plate", "W_sign", "W_hanging_sign", "W_shelf", "W_boat", "W_chest_boat"}) {
                    ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = ResourceKey.create(Registries.RECIPE, SimpleEnd.id(w.id(r)));
                    h.assertTrue(recipes.byKey(key).isPresent(), "recipe " + key.identifier());
                }
            }
            h.succeed();
        });
        add("axe_strips_and_wood_burns", h -> {
            FireBlockInvoker fire = (FireBlockInvoker) Blocks.FIRE;
            for (EndWood w : EndWood.ALL) {
                BlockPos at = new BlockPos(2 + EndWood.ALL.indexOf(w) * 2, 2, 2);
                h.setBlock(at, w.log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
                Player p = h.makeMockPlayer(GameType.SURVIVAL);
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                BlockPos abs = h.absolutePos(at);
                Items.IRON_AXE.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
                BlockState now = h.getBlockState(at);
                h.assertTrue(now.is(w.strippedLog) && now.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, w.name + ": axe strips the log, got " + now);
                for (Block b : new Block[] {w.log, w.planks, w.shelf, w.leaves, w.sticks}) h.assertTrue(fire.simpleend$canBurn(b.defaultBlockState()), BuiltInRegistries.BLOCK.getKey(b) + " burns");
            }
            h.assertTrue(fire.simpleend$canBurn(EndRegistry.END_GRASS.defaultBlockState()), "End grass burns");
            h.succeed();
        });
        // docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md: with SimpleBuilding / Simple Sandwiches loaded, the End woods have
        // every per-wood block of those mods (registered, recipe, burns); without them none of those ids exists.
        // Only public registry ids, no class of the partners (module independence, rule 3).
        add("partner_wood_features", h -> {
            var recipes = h.getLevel().recipeAccess();
            boolean sb = loaded("simplebuilding");
            boolean sandwiches = loaded("simplesandwiches");
            FireBlockInvoker fire = (FireBlockInvoker) Blocks.FIRE;
            for (EndWood w : EndWood.ALL) {
                String n = w.name;
                for (String id : new String[] {"hollow_" + n + "_log", "hollow_stripped_" + n + "_log", n + "_sheet", "stripped_" + n + "_sheet",
                        n + "_cauldron", "stripped_" + n + "_cauldron", "carved_" + n + "_log", n + "_octet", n + "_log_octet",
                        "stripped_" + n + "_log_octet"}) {
                    var key = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", id);
                    h.assertTrue(BuiltInRegistries.BLOCK.containsKey(key) == sb, key + " registered only with SimpleBuilding");
                    h.assertTrue(BuiltInRegistries.ITEM.containsKey(key) == sb, key + " item only with SimpleBuilding");
                    if (sb && !id.endsWith("_octet")) {
                        BlockState state = BuiltInRegistries.BLOCK.getValue(key).defaultBlockState();
                        h.assertTrue(fire.simpleend$canBurn(state), key + " burns like wood");
                        h.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), key + " is mined with the axe");
                    }
                }
                // The crates moved to Simple Storage (split stage 8a, simplestorage:<wood>_crate): there with Simple Storage.
                boolean storage = loaded("simplestorage");
                var crate = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplestorage", n + "_crate");
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(crate) == storage, crate + " registered only with Simple Storage");
                h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, crate)).isPresent() == storage, "recipe " + crate + " only with Simple Storage");
                if (storage) {
                    BlockState state = BuiltInRegistries.BLOCK.getValue(crate).defaultBlockState();
                    h.assertTrue(fire.simpleend$canBurn(state) && state.is(BlockTags.MINEABLE_WITH_AXE), crate + " burns and is mined with the axe");
                }
                // The tiered chest boats moved too (split stage 8b-2, simplestorage:<tier>_chest_boat_<wood>).
                var boatRecipe = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplestorage", "reinforced_chest_boat_" + n));
                h.assertTrue(recipes.byKey(boatRecipe).isPresent() == storage, "recipe " + boatRecipe.identifier() + " only with Simple Storage");
                for (String r : new String[] {"hollow_" + n + "_log", n + "_cauldron"}) {
                    var key = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", r));
                    h.assertTrue(recipes.byKey(key).isPresent() == sb, "recipe " + key.identifier() + " only with SimpleBuilding");
                }
                // The octet cuts live in Simple Octets since stage 6 (simpleoctets:<wood>_octet_from_..., IdMoves): in the
                // Shapecutter (_sawing) with Simple Blocks, in the stonecutter (_stonecutting) only without it.
                boolean octets = loaded("simpleoctets"), saw = loaded("simpleblocks");
                for (String r : new String[] {n + "_octet_from_" + n + "_planks", n + "_log_octet_from_" + n + "_log"}) {
                    var sawing = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleoctets", r + "_sawing"));
                    var cutting = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleoctets", r + "_stonecutting"));
                    h.assertTrue(recipes.byKey(sawing).isPresent() == (octets && saw), "recipe " + sawing.identifier() + " only with Simple Octets and Simple Blocks");
                    h.assertTrue(recipes.byKey(cutting).isPresent() == (octets && !saw), "recipe " + cutting.identifier() + " only with Simple Octets without Simple Blocks");
                }
                var board = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplesandwiches", n + "_cutting_board");
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(board) == sandwiches, board + " only with Simple Sandwiches");
                var boardRecipe = ResourceKey.create(Registries.RECIPE, board);
                h.assertTrue(!sandwiches || recipes.byKey(boardRecipe).isPresent(), "recipe " + board);
            }
            h.succeed();
        });
        // Umbau 13.10.: buds outside the End grow gigantic wood trees. Tree tests grow in the sky, each in its own height
        // band (50, 85, 120, 155, 200, 265) above the barrier roof of the test structure, so trees of neighbouring
        // tests never meet.
        add("bud_light_grows_tree", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = BUD.above(85);
            ground(h, at, 3);
            h.setBlock(at, w.bud);
            waitForLight(h, at, true, () -> {
                BlockPos abs = h.absolutePos(at);
                var grown = com.simplebuilding.modules.simpleend.world.EndTrees.grow(h.getLevel(), abs, w,
                        com.simplebuilding.modules.simpleend.world.EndTrees.Size.BIG, false, h.getLevel().getRandom(), abs);
                h.assertTrue(grown != null, "a lit bud grows");
                h.assertTrue(h.getBlockState(at).is(w.log) && h.getBlockState(at.above(14)).is(w.log), "gigantic trunk of at least 15 logs");
                h.assertTrue(grown.leaves().size() > 150, "big leaf crown, got " + grown.leaves().size());
                h.assertTrue(countAbs(h.getLevel(), grown.wood(), w.sticks) == 0, "a wood tree outside the End has no sticks");
                h.assertTrue(grown.buds() <= 1 && grown.fruits() <= 1, "0.8 buds and 1 fruit on average: 0-1 each, got " + grown.buds() + "/" + grown.fruits());
                int marked = 0;
                for (BlockPos p : grown.leaves()) if (h.getLevel().getBlockState(p).getValue(com.simplebuilding.modules.simpleend.block.EndLeavesBlock.DROP) != 0) marked++;
                h.assertTrue(marked == grown.buds() + grown.fruits(), "marked leaves = buds + fruits");
                clear(h, grown, at, 3);
            });
        });
        add("bud_dark_grows_scrub", h -> {
            EndWood w = EndWood.DUSKWOOD;
            ground(h, BUD, 3);
            for (BlockPos p : BlockPos.betweenClosed(BUD.offset(-2, -1, -2), BUD.offset(2, 15, 2))) {
                BlockPos r = p.immutable();
                boolean shell = Math.abs(r.getX() - BUD.getX()) == 2 || Math.abs(r.getZ() - BUD.getZ()) == 2 || r.getY() == BUD.getY() + 15;
                if (shell) h.setBlock(r, Blocks.STONE);
            }
            h.setBlock(BUD, w.bud);
            waitForLight(h, BUD, false, () -> {
                BlockState before = h.getBlockState(BUD);
                for (int i = 0; i < 60; i++) before.randomTick(h.getLevel(), h.absolutePos(BUD), h.getLevel().getRandom());
                h.assertTrue(h.getBlockState(BUD).is(w.bud), "a dark bud never grows by itself");
                boolean grown = ((EndBudBlock) w.bud).grow(h.getLevel(), h.absolutePos(BUD), h.getLevel().getRandom());
                h.assertTrue(grown && h.getBlockState(BUD).is(w.sticks), "bone meal in the dark gives End sticks");
                h.assertTrue(h.getBlockState(BUD).getValue(EndSticksBlock.THICKNESS) == 3, "thick base");
                h.assertTrue(count(h, w.log, BUD.offset(-1, 0, -1), BUD.offset(1, 14, 1)) == 0, "no tree in the dark");
            });
        });
        add("bud_square_grows_huge_tree", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = BUD.above(155);
            ground(h, at, 5);
            for (BlockPos p : new BlockPos[] {at, at.east(), at.south(), at.south().east()}) h.setBlock(p, w.bud);
            waitForLight(h, at, true, () -> {
                boolean grown = ((EndBudBlock) w.bud).grow(h.getLevel(), h.absolutePos(at.south().east()), h.getLevel().getRandom(), false);
                h.assertTrue(grown, "2x2 buds grow");
                for (BlockPos p : new BlockPos[] {at, at.east(), at.south(), at.south().east()})
                    h.assertTrue(h.getBlockState(p.above(22)).is(w.log), "2x2 trunk at least 23 high at " + p);
                for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(at.offset(-16, 0, -16)), h.absolutePos(at.offset(17, 45, 17))))
                    if (!h.getLevel().getBlockState(p).isAir()) h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 18);
            });
        });
        add("colossal_tree_from_sixteen_buds", h -> {
            EndWood w = EndWood.DUSKWOOD;
            BlockPos at = BUD.above(200);
            ground(h, at, 6);
            for (int i = 0; i < 16; i++) h.setBlock(at.offset(i % 4, 0, i / 4), w.bud);
            waitForLight(h, at, true, () -> {
                BlockPos abs = h.absolutePos(at.offset(2, 0, 1));
                var plan = com.simplebuilding.modules.simpleend.world.EndTrees.plan(com.simplebuilding.modules.simpleend.world.EndTrees.Size.COLOSSAL, w, false,
                        h.getLevel().getRandom(), abs);
                h.assertTrue(plan.height >= 38 && plan.reach(abs) >= 6, "colossal plan: trunk " + plan.height + ", reach " + plan.reach(abs));
                boolean grown = ((EndBudBlock) w.bud).grow(h.getLevel(), abs, h.getLevel().getRandom(), false);
                h.assertTrue(grown, "4x4 buds grow");
                for (int i = 0; i < 16; i++) h.assertTrue(h.getBlockState(at.offset(i % 4, 20, i / 4)).is(w.log), "4x4 trunk 20 up");
                h.assertTrue(h.getBlockState(at.offset(1, 37, 1)).is(w.log), "trunk at least 38 high");
                int leaves = 0;
                for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(at.offset(-20, 0, -20)), h.absolutePos(at.offset(23, 64, 23)))) {
                    BlockState s2 = h.getLevel().getBlockState(p);
                    if (s2.is(w.leaves)) leaves++;
                    if (!s2.isAir()) h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
                h.assertTrue(leaves > 1500, "biggest crown in the game, got " + leaves + " leaves");
            });
        });
        add("bud_in_end_grows_stick_tree", h -> {
            EndWood w = EndWood.DUSKWOOD;
            BlockPos at = BUD.above(265);
            ground(h, at, 3);
            h.setBlock(at, w.bud);
            BlockPos abs = h.absolutePos(at);
            var grown = com.simplebuilding.modules.simpleend.world.EndTrees.grow(h.getLevel(), abs, w,
                    com.simplebuilding.modules.simpleend.world.EndTrees.Size.BIG, true, h.getLevel().getRandom(), abs);
            h.assertTrue(grown != null, "stick tree grows");
            h.assertTrue(countAbs(h.getLevel(), grown.wood(), w.log) == 0, "no wood in the End");
            h.assertTrue(h.getBlockState(at.above(12)).is(w.sticks) && h.getBlockState(at).getValue(EndSticksBlock.NATURAL), "natural stick trunk");
            h.assertTrue(grown.wood().stream().anyMatch(p -> h.getLevel().getBlockState(p).getValue(EndSticksBlock.AXIS) != Direction.Axis.Y), "lying branches");
            h.assertTrue(grown.leaves().size() > 100, "crown of End leaves");
            h.assertTrue(grown.buds() >= 1 && grown.buds() <= 2, "1.5 buds on average: 1-2, got " + grown.buds());
            clear(h, grown, at, 3);
            h.succeed();
        });
        add("stick_tree_wood_connected", h -> {
            // N68 bug: bars only reach neighbours along their own axis, so bends, roots and trunks floated apart.
            // Every stick of a grown tree must meet a neighbouring stick that it reaches and that reaches back.
            EndWood w = EndWood.DUSKWOOD;
            BlockPos at = BUD.above(200);
            ServerLevel level = h.getLevel();
            BlockPos abs = h.absolutePos(at);
            var size = com.simplebuilding.modules.simpleend.world.EndTrees.Size.values();
            for (int t = 0; t < 6; t++) {
                var s = size[t % 2];
                ground(h, at, 4);
                var grown = com.simplebuilding.modules.simpleend.world.EndTrees.grow(level, abs, w, s, true, net.minecraft.util.RandomSource.create(100 + t));
                h.assertTrue(grown != null, "stick tree grows");
                int loose = 0;
                for (BlockPos p : grown.wood()) {
                    BlockState a = level.getBlockState(p);
                    if (!a.is(w.sticks)) continue;
                    boolean joined = false;
                    for (Direction d : Direction.values()) {
                        BlockState b = level.getBlockState(p.relative(d));
                        if (b.is(w.sticks) && reaches(a, p, level, d) && reaches(b, p.relative(d), level, d.getOpposite())) joined = true;
                    }
                    if (!joined && grown.wood().size() > 1) loose++;
                }
                h.assertTrue(loose == 0, "every stick of the tree touches its neighbour, " + loose + " float loose (tree " + t + ")");
                clear(h, grown, at, 4);
            }
            h.succeed();
        });
        add("sticks_taper_like_dripstone", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = new BlockPos(3, 1, 3);
            h.setBlock(at.below(), Blocks.END_STONE);
            for (int i = 0; i < 4; i++) {
                BlockPos p = h.absolutePos(at.above(i));
                ServerLevel level = h.getLevel();
                level.setBlockAndUpdate(p, ((EndSticksBlock) w.sticks).shaped(level.getBlockState(p.above())));
            }
            h.assertTrue(h.getBlockState(at.above(3)).getValue(EndSticksBlock.THICKNESS) == 1, "tip on top");
            h.assertTrue(h.getBlockState(at.above(2)).getValue(EndSticksBlock.THICKNESS) == 2, "middle under the tip");
            h.assertTrue(h.getBlockState(at).getValue(EndSticksBlock.THICKNESS) == 3, "thick base");
            h.succeed();
        });
        add("stick_tree_feature", h -> {
            BlockPos at = BUD.above(120);
            ground(h, at, 3);
            BlockPos abs = h.absolutePos(at);
            boolean placed = new EndFlora("stick_tree", "duskwood").place(h.getLevel(), null, h.getLevel().getRandom(), abs);
            h.assertTrue(placed && h.getBlockState(at).is(EndWood.DUSKWOOD.sticks) && h.getBlockState(at.above(12)).is(EndWood.DUSKWOOD.sticks), "stick tree in the biome");
            int logs = 0;
            for (BlockPos p : BlockPos.betweenClosed(abs.offset(-16, 0, -16), abs.offset(16, 30, 16))) {
                BlockState s2 = h.getLevel().getBlockState(p);
                if (s2.is(EndWood.DUSKWOOD.log)) logs++;
                if (!s2.isAir()) h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 18);
            }
            h.assertTrue(logs == 0, "the End grows no wood trees");
            h.succeed();
        });
        add("stick_tree_falls_like_chorus", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = BUD.above(50);
            ground(h, at, 3);
            BlockPos abs = h.absolutePos(at);
            ServerLevel level = h.getLevel();
            var grown = com.simplebuilding.modules.simpleend.world.EndTrees.grow(level, abs, w,
                    com.simplebuilding.modules.simpleend.world.EndTrees.Size.BIG, true, level.getRandom());
            h.assertTrue(grown != null, "stick tree grows");
            int before = countAbs(level, grown.wood(), w.sticks);
            level.destroyBlock(abs, true);
            int high = 0;
            for (BlockPos p : grown.wood()) if (p.getY() >= abs.getY() + 2 && level.getBlockState(p).is(w.sticks)) high++;
            h.assertTrue(high == 0, "the whole stick tree fell, " + high + " of " + before + " left");
            var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(24));
            int sticks = items.stream().filter(e -> e.getItem().is(w.sticks.asItem())).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(sticks >= before - 12, "the sticks dropped (" + sticks + " of " + before + ")");
            h.assertTrue(items.size() <= 6, "drops merged into a few stacks, got " + items.size());
            items.forEach(ItemEntity::discard);
            clear(h, grown, at, 3);
            h.succeed();
        });
        add("placed_sticks_stay", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = new BlockPos(2, 3, 9);
            for (int i = 0; i < 3; i++) h.setBlock(at.above(i), w.sticks.defaultBlockState());
            h.getLevel().destroyBlock(h.absolutePos(at), false);
            h.assertTrue(h.getBlockState(at.above(1)).is(w.sticks) && h.getBlockState(at.above(2)).is(w.sticks), "placed sticks never fall");
            h.assertTrue(w.sticks.defaultBlockState().is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY), "sticks keep leaves alive");
            h.succeed();
        });
        add("bud_return_expectation", h -> {
            var r = net.minecraft.util.RandomSource.create(42);
            int n = 20000;
            double wood = 0, sticks = 0;
            for (int i = 0; i < n; i++) {
                wood += com.simplebuilding.modules.simpleend.world.EndTrees.marks(r, com.simplebuilding.modules.simpleend.world.EndTrees.BUDS_WOOD);
                sticks += com.simplebuilding.modules.simpleend.world.EndTrees.marks(r, com.simplebuilding.modules.simpleend.world.EndTrees.BUDS_STICKS);
            }
            h.assertTrue(Math.abs(wood / n - 0.8) < 0.02, "about 0.8 buds per wood tree, got " + wood / n);
            h.assertTrue(Math.abs(sticks / n - 1.5) < 0.02, "about 1.5 buds per stick tree, got " + sticks / n);
            h.succeed();
        });
        add("leaves_drop_bud_sticks_fruit", h -> {
            EndWood w = EndWood.ASHWOOD;
            ServerLevel level = h.getLevel();
            BlockPos abs = h.absolutePos(new BlockPos(5, 2, 5));
            BlockState plain = w.leaves.defaultBlockState();
            boolean stick = false, budFromPlain = false;
            for (int i = 0; i < 400; i++)
                for (ItemStack s2 : Block.getDrops(plain, level, abs, null)) {
                    stick |= s2.is(Items.STICK);
                    budFromPlain |= s2.is(w.bud.asItem()) || s2.is(EndRegistry.DRIFT_FRUIT) || s2.is(EndRegistry.SPOILED_DRIFT_FRUIT);
                }
            h.assertTrue(stick && !budFromPlain, "unmarked leaves: sticks only (stick " + stick + ", bud/fruit " + budFromPlain + ")");
            BlockState bud = plain.setValue(com.simplebuilding.modules.simpleend.block.EndLeavesBlock.DROP, 1);
            for (int i = 0; i < 20; i++)
                h.assertTrue(Block.getDrops(bud, level, abs, null).stream().filter(s2 -> s2.is(w.bud.asItem())).mapToInt(ItemStack::getCount).sum() == 1,
                        "a bud-marked leaf always drops exactly one bud");
            h.succeed();
        });
        add("fruit_ripens_only_in_the_end", h -> {
            BlockState fruit = EndWood.DUSKWOOD.leaves.defaultBlockState().setValue(com.simplebuilding.modules.simpleend.block.EndLeavesBlock.DROP, 2);
            ServerLevel over = h.getLevel();
            ServerLevel end = over.getServer().getLevel(net.minecraft.world.level.Level.END);
            h.assertTrue(end != null, "test server has the End");
            var overDrops = Block.getDrops(fruit, over, h.absolutePos(BlockPos.ZERO), null);
            var endDrops = Block.getDrops(fruit, end, new BlockPos(100, 64, 0), null);
            h.assertTrue(overDrops.stream().anyMatch(s2 -> s2.is(EndRegistry.SPOILED_DRIFT_FRUIT)) && overDrops.stream().noneMatch(s2 -> s2.is(EndRegistry.DRIFT_FRUIT)),
                    "outside the End the fruit is spoiled: " + overDrops);
            h.assertTrue(endDrops.stream().anyMatch(s2 -> s2.is(EndRegistry.DRIFT_FRUIT)) && endDrops.stream().noneMatch(s2 -> s2.is(EndRegistry.SPOILED_DRIFT_FRUIT)),
                    "in the End it ripens: " + endDrops);
            h.succeed();
        });
        add("spoiled_fruit_drifting", h -> {
            Player p = h.makeMockPlayer(GameType.SURVIVAL);
            BlockPos abs = h.absolutePos(new BlockPos(4, 2, 6));
            p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
            float health = p.getHealth();
            new ItemStack(EndRegistry.SPOILED_DRIFT_FRUIT).finishUsingItem(h.getLevel(), p);
            var drift = p.getEffect(EndRegistry.drifting());
            h.assertTrue(drift != null && drift.getDuration() <= 120, "short Drifting effect");
            h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING) && p.getEffect(MobEffects.SLOW_FALLING).getDuration() > drift.getDuration(), "Slow Falling outlasts the drift");
            h.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY) < 0.02, "nearly weightless");
            h.assertTrue(p.getHealth() >= health, "not harmful");
            h.succeed();
        });
        add("end_leaves_not_full_block", h -> {
            for (EndWood w : EndWood.ALL) {
                BlockState s2 = w.leaves.defaultBlockState();
                h.assertTrue(!s2.canOcclude(), w.name + " leaves never occlude");
                h.assertTrue(s2.getValue(com.simplebuilding.modules.simpleend.block.EndLeavesBlock.DROP) == 0, "placed leaves carry nothing");
            }
            h.succeed();
        });
        add("end_leaves_act_like_cobweb", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(20, 3, 20);
            BlockPos abs = h.absolutePos(at);
            var player = h.makeMockServerPlayerInLevel();
            // Both End woods (N65: Duskwood too) behave like a cobweb; a plain solid block is the control.
            for (EndWood w : EndWood.ALL) {
                h.setBlock(at, w.leaves);
                BlockState leaf = w.leaves.defaultBlockState();
                h.assertTrue(leaf.getCollisionShape(level, abs).isEmpty(), w.name + " leaves have no collision");
                // Slowed like a cobweb: a half-block step inside covers a quarter of it; the fall distance is gone.
                Vec3 start = player.position();
                player.fallDistance = 12.0;
                leaf.entityInside(level, abs, player, InsideBlockEffectApplier.NOOP, true);
                h.assertTrue(player.fallDistance == 0.0, "no fall damage inside " + w.name + " leaves, fall distance " + player.fallDistance);
                player.move(MoverType.SELF, new Vec3(0.5, 0.0, 0.0));
                double moved = player.position().x - start.x;
                h.assertTrue(Math.abs(moved - 0.125) < 0.01, w.name + " leaves slow strongly like a cobweb, moved " + moved);
                player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
                // Sword and shears cut them at least as fast as a cobweb (Vanilla leaves tags: sword_efficient, shears_extreme).
                BlockState web = Blocks.COBWEB.defaultBlockState();
                for (net.minecraft.world.item.Item tool : new net.minecraft.world.item.Item[] {Items.IRON_SWORD, Items.SHEARS}) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
                    float prog = leaf.getDestroyProgress(player, level, abs);
                    float cobweb = web.getDestroyProgress(player, level, abs);
                    h.assertTrue(prog >= cobweb && prog > 0, w.name + " leaves break at least as fast as a cobweb with " + tool + ": " + prog + " vs " + cobweb);
                }
            }
            h.succeed();
        });
        add("end_leaves_catch_fall", h -> {
            ServerLevel level = h.getLevel();
            BlockPos ashAt = new BlockPos(20, 4, 20), duskAt = new BlockPos(22, 4, 20), stoneAt = new BlockPos(24, 4, 20);
            for (BlockPos col : new BlockPos[] {ashAt, duskAt, stoneAt}) {
                h.setBlock(col.below(), Blocks.STONE); // the pigs land IN the leaves, ON the control block
                for (int y = 0; y < 12; y++) h.setBlock(col.above(y), Blocks.AIR);
            }
            // Persistent: placed leaves are far from any log, so a random tick may decay them. A leaf that vanished before the pig
            // arrived (the pig needs ~15 ticks, then creeps ~100 ticks through the cobweb-like block) gave the full 8-block fall damage.
            h.setBlock(ashAt, EndWood.ASHWOOD.leaves.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
            h.setBlock(duskAt, EndWood.DUSKWOOD.leaves.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
            h.setBlock(stoneAt, Blocks.STONE);
            Pig ashPig = h.spawn(EntityTypes.PIG, new BlockPos(20, 12, 20));
            Pig duskPig = h.spawn(EntityTypes.PIG, new BlockPos(22, 12, 20));
            Pig stonePig = h.spawn(EntityTypes.PIG, new BlockPos(24, 12, 20));
            h.succeedWhen(() -> {
                h.assertTrue(ashPig.onGround() && duskPig.onGround() && stonePig.onGround(), "all pigs landed");
                h.assertTrue(ashPig.getHealth() >= ashPig.getMaxHealth(), "ashwood leaves catch the fall, health " + ashPig.getHealth());
                h.assertTrue(duskPig.getHealth() >= duskPig.getMaxHealth(), "duskwood leaves catch the fall, health " + duskPig.getHealth());
                h.assertTrue(stonePig.getHealth() < stonePig.getMaxHealth(), "a fall onto stone still hurts (control), health " + stonePig.getHealth());
            });
        });
        add("end_leaves_density_state", h -> {
            // TX22-4: density from the neighbours that are not air, whatever they are (0-3 loose, 4-5 medium, 6 dense), kept up to date.
            ServerLevel level = h.getLevel();
            BlockPos c = new BlockPos(20, 3, 20);
            BlockPos abs = h.absolutePos(c);
            Direction[] sides = Direction.values();
            var player = h.makeMockServerPlayerInLevel();
            int[] want = {EndLeavesBlock.LOOSE, EndLeavesBlock.LOOSE, EndLeavesBlock.LOOSE, EndLeavesBlock.LOOSE,
                    EndLeavesBlock.MEDIUM, EndLeavesBlock.MEDIUM, EndLeavesBlock.DENSE};
            for (EndWood w : EndWood.ALL) {
                h.setBlock(c, Blocks.AIR);
                for (Direction d : sides) h.setBlock(c.relative(d), Blocks.AIR);
                BlockState placed = w.leaves.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player,
                        InteractionHand.MAIN_HAND, new ItemStack(w.leaves), new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
                h.assertTrue(placed.getValue(EndLeavesBlock.DENSITY) == EndLeavesBlock.LOOSE, w.name + " leaf placed in open air is loose");
                h.setBlock(c, placed);
                // Fill the sides one by one with mixed blocks (stone, dirt, logs, leaves): every count gives its stage.
                Block[] fill = {Blocks.STONE, Blocks.DIRT, w.log, w.leaves, Blocks.GLASS, Blocks.STONE};
                for (int i = 0; i < 6; i++) {
                    h.setBlock(c.relative(sides[i]), fill[i]);
                    int got = level.getBlockState(abs).getValue(EndLeavesBlock.DENSITY);
                    h.assertTrue(got == want[i + 1], w.name + " with " + (i + 1) + " neighbours has density " + want[i + 1] + ", got " + got);
                }
                // Removing neighbours steps back down (shape update).
                h.setBlock(c.relative(sides[0]), Blocks.AIR);
                h.assertTrue(level.getBlockState(abs).getValue(EndLeavesBlock.DENSITY) == EndLeavesBlock.MEDIUM, w.name + " back to medium with 5");
                h.setBlock(c.relative(sides[1]), Blocks.AIR);
                h.setBlock(c.relative(sides[2]), Blocks.AIR);
                h.assertTrue(level.getBlockState(abs).getValue(EndLeavesBlock.DENSITY) == EndLeavesBlock.LOOSE, w.name + " back to loose with 3");
                h.setBlock(c, Blocks.AIR);
                for (Direction d : sides) h.setBlock(c.relative(d), Blocks.AIR);
            }
            h.succeed();
        });
        add("end_leaves_density_brake", h -> {
            // TX22-4: the denser the leaf, the stronger the cobweb brake; medium is exactly the Vanilla cobweb.
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(20, 3, 20);
            BlockPos abs = h.absolutePos(at);
            var player = h.makeMockServerPlayerInLevel();
            for (EndWood w : EndWood.ALL) {
                double last = Double.MAX_VALUE;
                for (int d = EndLeavesBlock.LOOSE; d <= EndLeavesBlock.DENSE; d++) {
                    BlockState leaf = w.leaves.defaultBlockState().setValue(EndLeavesBlock.DENSITY, d);
                    h.setBlock(at, leaf);
                    Vec3 start = player.position();
                    leaf.entityInside(level, abs, player, InsideBlockEffectApplier.NOOP, true);
                    player.move(MoverType.SELF, new Vec3(0.5, 0.0, 0.0));
                    double moved = player.position().x - start.x;
                    player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
                    double expect = 0.5 * EndLeavesBlock.stuckSpeed(d, false).x;
                    h.assertTrue(Math.abs(moved - expect) < 0.01, w.name + " density " + d + " moves " + expect + ", got " + moved);
                    h.assertTrue(moved < last, w.name + " density " + d + " brakes harder than the stage before (" + moved + " vs " + last + ")");
                    last = moved;
                }
                h.assertTrue(EndLeavesBlock.stuckSpeed(EndLeavesBlock.MEDIUM, false).equals(new Vec3(0.25, 0.05F, 0.25)), "medium = Vanilla cobweb");
                h.assertTrue(EndLeavesBlock.stuckSpeed(EndLeavesBlock.MEDIUM, true).equals(new Vec3(0.5, 0.25, 0.5)), "medium with Weaving = Vanilla cobweb with Weaving");
                h.assertTrue(EndLeavesBlock.stuckSpeed(EndLeavesBlock.LOOSE, false).y > EndLeavesBlock.stuckSpeed(EndLeavesBlock.MEDIUM, false).y
                        && EndLeavesBlock.stuckSpeed(EndLeavesBlock.MEDIUM, false).y > EndLeavesBlock.stuckSpeed(EndLeavesBlock.DENSE, false).y,
                        "falling is braked by density too");
            }
            h.succeed();
        });
        add("end_leaves_hoe_tool", h -> {
            // TX22-4: the hoe is the tool; without one the leaves take as long as a cobweb (bare hand: 20 s, like Vanilla).
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(20, 3, 20);
            BlockPos abs = h.absolutePos(at);
            var player = h.makeMockServerPlayerInLevel();
            BlockState web = Blocks.COBWEB.defaultBlockState();
            for (EndWood w : EndWood.ALL) {
                h.setBlock(at, w.leaves);
                BlockState leaf = level.getBlockState(abs);
                h.assertTrue(leaf.is(BlockTags.MINEABLE_WITH_HOE), w.name + " leaves are mineable with a hoe");
                for (net.minecraft.world.item.Item tool : new net.minecraft.world.item.Item[] {Items.AIR, Items.STICK, Items.IRON_PICKAXE, Items.IRON_AXE}) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
                    float prog = leaf.getDestroyProgress(player, level, abs), cobweb = web.getDestroyProgress(player, level, abs);
                    h.assertTrue(Math.abs(prog - cobweb) < 1e-6, w.name + " leaves with " + tool + " break like a cobweb: " + prog + " vs " + cobweb);
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                float hand = leaf.getDestroyProgress(player, level, abs);
                float ticks = 400.0F * (player.onGround() ? 1 : 5); // Vanilla: mining in the air is five times slower
                h.assertTrue(Math.abs(1.0F / hand - ticks) < 1.0F, w.name + " leaves by hand take " + ticks + " ticks (cobweb), got " + (1.0F / hand));
                for (net.minecraft.world.item.Item hoe : new net.minecraft.world.item.Item[] {Items.WOODEN_HOE, Items.IRON_HOE}) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(hoe));
                    float prog = leaf.getDestroyProgress(player, level, abs);
                    h.assertTrue(prog >= hand * 10, w.name + " leaves break far faster with " + hoe + ": " + prog + " vs hand " + hand);
                }
            }
            h.succeed();
        });
        add("drift_fruit_glide", h -> {
            Player p = h.makeMockPlayer(GameType.SURVIVAL);
            BlockPos abs = h.absolutePos(new BlockPos(4, 2, 4));
            p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
            p.setDeltaMovement(Vec3.ZERO);
            ItemStack fruit = new ItemStack(EndRegistry.DRIFT_FRUIT);
            fruit.finishUsingItem(h.getLevel(), p);
            h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling after eating");
            int t = p.getEffect(MobEffects.SLOW_FALLING).getDuration();
            h.assertTrue(t >= 60 && t <= 100, "3-5 s of Slow Falling, got " + t + " ticks");
            h.assertTrue(p.getDeltaMovement().y > 0.4, "small hop up");
            h.succeed();
        });
        add("void_safe_items", h -> {
            ServerLevel level = h.getLevel();
            BlockPos abs = h.absolutePos(new BlockPos(6, 2, 6));
            ItemEntity item = new ItemEntity(level, abs.getX() + 0.5, level.getMinY() - 6, abs.getZ() + 0.5, new ItemStack(EndWood.ASHWOOD.log));
            item.setDeltaMovement(0, -0.5, 0);
            level.addFreshEntity(item);
            h.succeedWhen(() -> {
                h.assertTrue(item.isAlive(), "item survives the void");
                h.assertTrue(item.getAge() < 0, "despawn timer doubled (age starts at -6000), age " + item.getAge());
                // Rising from 6 below the bottom; in the test world bedrock at minY may stop it from below.
                h.assertTrue(item.isNoGravity() && item.getY() > level.getMinY() - 2, "floats back up to the world bottom, y " + item.getY());
                item.discard();
            });
        });
        add("fringe_hedge_feature", h -> {
            // N1 A: the fringe hedge is a wall of persistent leaves 4-7 high, and the fringe biomes list the denser flora.
            BlockPos at = BUD.above(120);
            BlockPos abs = h.absolutePos(at);
            ServerLevel level = h.getLevel();
            for (BlockPos p : BlockPos.betweenClosed(abs.offset(-12, -1, -12), abs.offset(12, -1, 12))) level.setBlock(p, Blocks.END_STONE.defaultBlockState(), 18);
            boolean placed = new EndFlora("hedge", "ashwood").place(level, null, level.getRandom(), abs);
            int leaves = 0, tallest = 0;
            for (BlockPos p : BlockPos.betweenClosed(abs.offset(-12, 0, -12), abs.offset(12, 12, 12))) {
                BlockState s2 = level.getBlockState(p);
                if (!s2.is(EndWood.ASHWOOD.leaves)) continue;
                leaves++;
                tallest = Math.max(tallest, p.getY() - abs.getY() + 1);
                h.assertTrue(s2.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT), "hedge leaves are persistent");
            }
            h.assertTrue(placed && leaves >= 20, "a hedge of leaves, got " + leaves);
            h.assertTrue(tallest >= 4 && tallest <= 7, "hedge 4-7 high, got " + tallest);
            for (BlockPos p : BlockPos.betweenClosed(abs.offset(-12, 0, -12), abs.offset(12, 12, 12))) if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            for (var entry : java.util.Map.of(EndBiomes.ASH_VALLEY_FRINGE, "ash_valley_fringe_hedges", EndBiomes.DAWNLANDS_FRINGE, "dawnlands_fringe_hedges").entrySet()) {
                boolean listed = biomes.getOrThrow(entry.getKey()).value().getGenerationSettings().features().stream()
                        .flatMap(set -> set.stream()).anyMatch(f -> f.unwrapKey().map(k -> k.identifier().getPath().equals(entry.getValue())).orElse(false));
                h.assertTrue(listed, entry.getKey().identifier() + " lists " + entry.getValue());
            }
            h.assertTrue(!biomes.getOrThrow(EndBiomes.ASH_VALLEY).value().getGenerationSettings().features().stream()
                    .flatMap(set -> set.stream()).anyMatch(f -> f.unwrapKey().map(k -> k.identifier().getPath().contains("hedges")).orElse(false)), "no hedges in the core");
            h.succeed();
        });
        add("forest_fog_config", h -> {
            // N1 B: client fog wall config keys, defaults and clamping.
            var json = new com.google.gson.JsonObject();
            boolean wall = SimpleEndConfig.forestFogWall;
            double strength = SimpleEndConfig.forestFogStrength;
            try {
                SimpleEndConfig.apply(json);
                h.assertTrue(SimpleEndConfig.forestFogWall && SimpleEndConfig.forestFogStrength == 0.85, "defaults on and 0.85");
                json.addProperty("forestFogStrength", 7);
                json.addProperty("forestFogWall", false);
                SimpleEndConfig.apply(json);
                h.assertTrue(!SimpleEndConfig.forestFogWall && SimpleEndConfig.forestFogStrength == 1.0, "switch off, strength clamped to 1");
                json.addProperty("forestFogStrength", "junk");
                SimpleEndConfig.apply(json);
                h.assertTrue(SimpleEndConfig.forestFogStrength == 0.85, "bad value falls back");
                h.assertTrue(SimpleEndConfig.KEYS.contains("forestFogWall") && SimpleEndConfig.KEYS.contains("forestFogStrength"), "keys listed");
            } finally {
                SimpleEndConfig.forestFogWall = wall;
                SimpleEndConfig.forestFogStrength = strength;
            }
            h.succeed();
        });
        add("biome_patches", h -> {
            EndBiomes.Noise flat = (x, z) -> 0.1;
            int some = 0, core = 0, fringe = 0;
            for (int x = 0; x < EndBiomes.CELL * 4; x++)
                for (int z = 0; z < EndBiomes.CELL * 4; z++) {
                    h.assertTrue(EndBiomes.pick(x, z, flat, 0.0) == EndBiomes.NONE, "chance 0 means no patch");
                    int pick = EndBiomes.pick(x, z, flat, 1.0);
                    h.assertTrue(pick == EndBiomes.pick(x, z, flat, 1.0), "deterministic");
                    if (pick != EndBiomes.NONE) some++;
                    if (pick == EndBiomes.ASH || pick == EndBiomes.DAWN) core++;
                    if (pick == EndBiomes.ASH_FRINGE || pick == EndBiomes.DAWN_FRINGE) fringe++;
                }
            // 16 cells, each patch 28-44 chunks across with a wavy rim (area ~600..1550 chunks, N66: several islands wide)
            h.assertTrue(some >= 16 * 600 && some <= 16 * 1550, "patch area per cell, got " + some + " chunks in 16 cells");
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            for (int x = 0; x < EndBiomes.CELL; x++)
                for (int z = 0; z < EndBiomes.CELL; z++)
                    if (EndBiomes.pick(x, z, flat, 1.0) != EndBiomes.NONE) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); }
            h.assertTrue(maxX - minX + 1 >= 20, "a patch is at least 20 chunks (320 blocks) wide, got " + (maxX - minX + 1));
            h.assertTrue(core > some / 5 && fringe > some / 5, "core " + core + " and fringe " + fringe);
            var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
            TheEndBiomeSource source = TheEndBiomeSource.create(biomes);
            for (var key : java.util.List.of(EndBiomes.ASH_VALLEY, EndBiomes.DAWNLANDS, EndBiomes.ASH_VALLEY_FRINGE, EndBiomes.DAWNLANDS_FRINGE))
                h.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(key)), "End biome source lists " + key.identifier());
            h.assertTrue(biomes.getOrThrow(EndBiomes.ASH_VALLEY).is(EndBiomes.WATCHER_CORE) && biomes.getOrThrow(EndBiomes.DAWNLANDS).is(EndBiomes.WATCHER_CORE),
                    "cores in simpleend:watcher_core");
            h.assertTrue(!biomes.getOrThrow(EndBiomes.ASH_VALLEY_FRINGE).is(EndBiomes.WATCHER_CORE), "fringe not in watcher_core");
            // a real End biome source with real End noise (the test server's own End may use a fixed biome):
            // cores and fringes appear on the outer islands
            if (SimpleEndConfig.biomes && SimpleEnd.on()) {
                var access = h.getLevel().registryAccess();
                var settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.END).value();
                var state = net.minecraft.world.level.levelgen.RandomState.create(access.lookupOrThrow(Registries.NOISE), 12345L, settings);
                var resolver = source.createUncachedResolver(state);
                java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
                for (int x = -12000; x <= 12000; x += 128)
                    for (int z = -12000; z <= 12000; z += 128)
                        seen.merge(resolver.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(64),
                                net.minecraft.core.QuartPos.fromBlock(z)).unwrapKey().map(k -> k.identifier().toString()).orElse("?"), 1, Integer::sum);
                for (var key : java.util.List.of(EndBiomes.ASH_VALLEY, EndBiomes.DAWNLANDS, EndBiomes.ASH_VALLEY_FRINGE, EndBiomes.DAWNLANDS_FRINGE))
                    h.assertTrue(seen.containsKey(key.identifier().toString()), key.identifier() + " in a real End (seen " + seen + ")");
            }
            h.succeed();
        });
        add("submod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleEnd.ID);
            try {
                com.simplebuilding.framework.api.SubMods.set(SimpleEnd.ID, false);
                SimpleEnd.applySubSwitches();
                h.assertTrue(!SimpleEnd.on(), "Simple End off");
                h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled(SimpleEnd.DRAGON_FIGHT), "nested Dragon Fight follows Simple End");
                com.simplebuilding.framework.api.SubMods.set(SimpleEnd.ID, true);
                SimpleEnd.applySubSwitches();
                h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled(SimpleEnd.DRAGON_FIGHT) == SimpleEndConfig.enableSimpleEndDragonFight, "Dragon Fight back on");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleEnd.ID, before);
                SimpleEnd.applySubSwitches();
            }
            h.assertTrue(SimpleEndConfig.KEYS.size() == 9, "config keys");
            h.succeed();
        });
        add("nibblemite_nibbles_end_stone", h -> {
            BlockPos at = new BlockPos(6, 2, 6);
            h.setBlock(at.below(), Blocks.END_STONE);
            var mite = EndRegistry.NIBBLEMITE.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            h.assertTrue(mite != null, "nibblemite entity");
            BlockPos abs = h.absolutePos(at);
            mite.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
            mite.setNoAi(true);
            h.getLevel().addFreshEntity(mite);
            h.assertTrue(mite.nibble(), "nibbles the End stone below");
            h.assertTrue(h.getBlockState(at.below()).is(EndRegistry.BURROWED_END_STONE), "End stone became Burrowed End Stone");
            h.assertTrue(!mite.nibble(), "burrowed stone is not nibbled again");
            h.assertTrue(com.simplebuilding.modules.simpleend.entity.Nibblemite.isEndGround(h.getBlockState(at.below())), "burrowed stone still counts as End ground");
            mite.discard();
            h.succeed();
        });
        add("burrowed_end_stone_recovers", h -> {
            BlockPos at = new BlockPos(7, 2, 7);
            h.setBlock(at, EndRegistry.BURROWED_END_STONE);
            for (int i = 0; i < 200 && h.getBlockState(at).is(EndRegistry.BURROWED_END_STONE); i++)
                h.getBlockState(at).randomTick(h.getLevel(), h.absolutePos(at), h.getLevel().getRandom());
            h.assertTrue(h.getBlockState(at).is(Blocks.END_STONE), "Burrowed End Stone recovers to End stone");
            h.assertTrue(EndRegistry.BURROWED_END_STONE.defaultBlockState().isRandomlyTicking(), "recovers on random ticks");
            h.succeed();
        });
        add("nibblemite_is_passive", h -> {
            var mite = EndRegistry.NIBBLEMITE.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            h.assertTrue(mite != null, "nibblemite entity");
            h.assertTrue(!((Object) mite instanceof net.minecraft.world.entity.monster.Enemy), "not an enemy");
            h.assertTrue(!((Object) mite instanceof net.minecraft.world.entity.monster.Endermite), "not an Endermite (Endermen ignore it)");
            h.assertTrue(EndRegistry.NIBBLEMITE.getCategory() == net.minecraft.world.entity.MobCategory.CREATURE, "creature category");
            h.assertTrue(mite.isFood(new ItemStack(Items.CHORUS_FRUIT)) && !mite.isFood(new ItemStack(Items.WHEAT)), "fed with chorus fruit");
            h.assertTrue(!mite.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "no attack damage");
            h.succeed();
        });
        add("end_grass_on_end_stone", h -> {
            BlockPos at = new BlockPos(8, 2, 8);
            h.setBlock(at.below(), Blocks.END_STONE);
            h.assertTrue(EndRegistry.END_GRASS.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(at)), "End grass stays on End stone");
            h.assertTrue(EndWood.ASHWOOD.bud.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(at)), "bud stays on End stone");
            h.setBlock(at.below(), Blocks.STONE);
            h.assertTrue(!EndRegistry.END_GRASS.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(at)), "not on stone");
            h.succeed();
        });
    }

    /** Creative-tab switch: every Vanilla-tab entry is an item of the own tab, and every wood/plant/fruit/egg item is sorted into one. */
    private static void vanillaTabStacks(GameTestHelper h) {
        var map = EndRegistry.vanillaTabStacks();
        var own = EndRegistry.tabStacks();
        int sorted = 0;
        h.assertTrue(EndRegistry.VANILLA_TABS.containsAll(map.keySet()), "every target tab is registered by the Fabric listener list");
        for (var e : map.entrySet()) {
            for (var stack : e.getValue()) {
                sorted++;
                h.assertTrue(own.stream().anyMatch(o -> o.is(stack.getItem())), "not in the own tab: " + stack.getItem());
            }
        }
        int ownItems = own.size() - (com.simplebuilding.modules.simpleend.guide.EndGuide.book() != null ? 1 : 0);
        h.assertTrue(sorted == ownItems, "Vanilla tabs get all " + ownItems + " items except the guide, got " + sorted);
        h.assertTrue(map.containsKey(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS) && map.containsKey(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS)
                && map.containsKey(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS), "wood, plants and the egg land in matching tabs");
        h.succeed();
    }

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("vanilla_tab_stacks", EndTests::vanillaTabStacks);
        ALL.put("discover_recipe_unlocks", com.simplebuilding.modules.simpleend.discover.EndDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplebuilding.modules.simpleend.discover.EndDiscover::behaviourHints);
    }
}
