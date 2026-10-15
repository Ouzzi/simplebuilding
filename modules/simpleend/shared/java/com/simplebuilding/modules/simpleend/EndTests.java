package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.block.EndBudBlock;
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
                        n + "_cauldron", "stripped_" + n + "_cauldron", n + "_crate", "carved_" + n + "_log", n + "_octet", n + "_log_octet",
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
                for (String r : new String[] {"hollow_" + n + "_log", n + "_crate", n + "_cauldron", n + "_octet_from_" + n + "_planks_stonecutting",
                        n + "_log_octet_from_" + n + "_log_stonecutting", "reinforced_chest_boat_" + n}) {
                    var key = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", r));
                    h.assertTrue(recipes.byKey(key).isPresent() == sb, "recipe " + key.identifier() + " only with SimpleBuilding");
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
        add("ashwood_leaves_act_like_cobweb", h -> {
            ServerLevel level = h.getLevel();
            BlockPos at = new BlockPos(20, 3, 20);
            h.setBlock(at, EndWood.ASHWOOD.leaves);
            BlockPos abs = h.absolutePos(at);
            BlockState ash = EndWood.ASHWOOD.leaves.defaultBlockState();
            BlockState dusk = EndWood.DUSKWOOD.leaves.defaultBlockState();
            h.assertTrue(ash.getCollisionShape(level, abs).isEmpty(), "ashwood leaves have no collision");
            h.assertTrue(!dusk.getCollisionShape(level, abs).isEmpty(), "duskwood leaves stay solid");
            // Slowed like a cobweb: a half-block step inside covers a quarter of it; the fall distance is gone.
            var player = h.makeMockServerPlayerInLevel();
            Vec3 start = player.position();
            player.fallDistance = 12.0;
            ash.entityInside(level, abs, player, InsideBlockEffectApplier.NOOP, true);
            h.assertTrue(player.fallDistance == 0.0, "no fall damage inside ashwood leaves, fall distance " + player.fallDistance);
            player.move(MoverType.SELF, new Vec3(0.5, 0.0, 0.0));
            double moved = player.position().x - start.x;
            h.assertTrue(Math.abs(moved - 0.125) < 0.01, "strongly slowed like a cobweb, moved " + moved);
            player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
            player.fallDistance = 12.0;
            dusk.entityInside(level, abs, player, InsideBlockEffectApplier.NOOP, true);
            h.assertTrue(player.fallDistance == 12.0, "duskwood leaves do not catch a fall");
            player.move(MoverType.SELF, new Vec3(0.5, 0.0, 0.0));
            h.assertTrue(player.position().x - start.x > 0.45, "duskwood leaves do not slow, moved " + (player.position().x - start.x));
            // Sword and shears cut them at least as fast as a cobweb (Vanilla leaves tags: sword_efficient, shears_extreme).
            BlockState web = Blocks.COBWEB.defaultBlockState();
            for (net.minecraft.world.item.Item tool : new net.minecraft.world.item.Item[] {Items.IRON_SWORD, Items.SHEARS}) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
                float leaf = ash.getDestroyProgress(player, level, abs);
                float cobweb = web.getDestroyProgress(player, level, abs);
                h.assertTrue(leaf >= cobweb && leaf > 0, "ashwood leaves break at least as fast as a cobweb with " + tool + ": " + leaf + " vs " + cobweb);
            }
            h.succeed();
        });
        add("ashwood_leaves_catch_fall", h -> {
            ServerLevel level = h.getLevel();
            BlockPos ashAt = new BlockPos(20, 4, 20), duskAt = new BlockPos(22, 4, 20);
            for (BlockPos col : new BlockPos[] {ashAt, duskAt}) {
                h.setBlock(col.below(), Blocks.STONE); // the pig lands IN the ashwood leaves, ON the duskwood ones
                for (int y = 0; y < 12; y++) h.setBlock(col.above(y), Blocks.AIR);
            }
            h.setBlock(ashAt, EndWood.ASHWOOD.leaves);
            h.setBlock(duskAt, EndWood.DUSKWOOD.leaves);
            Pig ashPig = h.spawn(EntityTypes.PIG, new BlockPos(20, 12, 20));
            Pig duskPig = h.spawn(EntityTypes.PIG, new BlockPos(22, 12, 20));
            h.succeedWhen(() -> {
                h.assertTrue(ashPig.onGround() && duskPig.onGround(), "both pigs landed: ash " + ashPig.position() + " " + ashPig.onGround() + ", dusk " + duskPig.position() + " " + duskPig.onGround());
                h.assertTrue(ashPig.getHealth() >= ashPig.getMaxHealth(), "ashwood leaves catch the fall, health " + ashPig.getHealth());
                h.assertTrue(duskPig.getHealth() < duskPig.getMaxHealth(), "a fall onto duskwood leaves still hurts (control), health " + duskPig.getHealth());
            });
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
            // 16 cells, each patch 14-24 chunks across with a wavy rim (area ~110..640 chunks)
            h.assertTrue(some >= 16 * 100 && some <= 16 * 640, "patch area per cell, got " + some + " chunks in 16 cells");
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
            h.assertTrue(SimpleEndConfig.KEYS.size() == 7, "config keys");
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
