package com.simpleblocks.test;

import com.simpleblocks.SimpleBlocks;
import com.simpleblocks.nature.CrackedIceBlock;
import com.simpleblocks.nature.NatureBlocks;
import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The building blocks that moved here from SimpleBuilding (b-blocksmove; tests moved with them from SimpleBuilding's
 * {@code NatureBlockTests}): recipes, the grass slab's loot, falling sand/gravel slabs, cracked ice (cracks under a living
 * entity, leaves no water, blocks placement, hides its faces in ice), the gravity blocks, the id moves out of SimpleBuilding,
 * the Vanilla recipe ways without SimpleBuilding and the creative tab. All on the module's empty 8 x 8 x 8 structure.
 * SimpleBuilding keeps the tests of its own uses (chisel, hammer, cracked ice fields, pistons and gravity).
 */
public final class NatureTests {
    /** Rissiges Eis: vier Stufen a {@link CrackedIceBlock#TICKS_PER_CRACK} plus Fallen und Reserve. */
    public static final int CRACKED_ICE_MAX_TICKS = 200;
    public static final int FALLING_SLAB_MAX_TICKS = 100;
    public static final int GRAVITY_MAX_TICKS = 100;

    private NatureTests() {
    }

    /** Test name -> body for the module catalogue ({@link BlocksTests#ALL}) and their tick limits. */
    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("nature_recipes", NatureTests::natureVariantsHaveTheirRecipes);
        all.put("grass_slab_drops_dirt_slab_without_silk_touch", NatureTests::grassSlabDropsDirtSlabWithoutSilkTouch);
        all.put("grass_spreads_onto_and_from_slabs", NatureTests::grassSpreadsOntoAndFromSlabs);
        all.put("sand_and_gravel_slabs_fall_and_merge", NatureTests::sandAndGravelSlabsFallAndMerge);
        maxTicks.put("sand_and_gravel_slabs_fall_and_merge", FALLING_SLAB_MAX_TICKS);
        all.put("gravity_slabs_settle_on_slabs_and_half_steps", NatureTests::gravitySlabsSettleOnSlabsAndHalfSteps);
        maxTicks.put("gravity_slabs_settle_on_slabs_and_half_steps", FALLING_SLAB_MAX_TICKS);
        all.put("upper_gravity_slab_starts_falling_from_its_own_height", NatureTests::upperGravitySlabStartsFallingFromItsOwnHeight);
        maxTicks.put("upper_gravity_slab_starts_falling_from_its_own_height", FALLING_SLAB_MAX_TICKS);
        all.put("natural_slabs_place_into_the_full_block", NatureTests::naturalSlabsPlaceIntoTheFullBlock);
        all.put("cracked_ice_leaves_no_water", NatureTests::crackedIceLeavesNoWater);
        all.put("cracked_ice_melts_under_an_entity_after_some_seconds", NatureTests::crackedIceMeltsUnderAnEntityAfterSomeSeconds);
        maxTicks.put("cracked_ice_melts_under_an_entity_after_some_seconds", CRACKED_ICE_MAX_TICKS);
        all.put("cracked_ice_blocks_placement_on_all_sides_and_blends_with_ice", NatureTests::crackedIceBlocksPlacementOnAllSidesAndBlendsWithIce);
        all.put("cracked_ice_drops_itself_with_silk_touch", NatureTests::crackedIceDropsItselfWithSilkTouch);
        all.put("moved_blocks_registered", NatureTests::movedBlocksRegistered);
        all.put("moved_ids_from_simplebuilding", NatureTests::movedIdsFromSimplebuilding);
        all.put("gravity_recipes_with_and_without_simplebuilding", NatureTests::gravityRecipesWithAndWithoutSimplebuilding);
        all.put("suspended_blocks_stay_levitating_blocks_rise", NatureTests::suspendedBlocksStayLevitatingBlocksRise);
        maxTicks.put("suspended_blocks_stay_levitating_blocks_rise", GRAVITY_MAX_TICKS);
        all.put("moved_blocks_in_the_tab", NatureTests::movedBlocksInTheTab);
    }

    /**
     * Werkbank: 3 Erde/Grasblock/Sand/Kies nebeneinander -> 6 Stufen; 2x2 Nautilusschalen -> Block, Block -> 4 Schalen;
     * Froschlicht (Vanilla oder Mod) + Rot/Cyan/Blau -> Scharlach/Aqua/Azur. Steinmetz: Eis -> rissiges Eis,
     * Packeis/Blaueis -> gemeisselt. Die Froschlichter leuchten wie Vanillas mit 15.
     */
    public static void natureVariantsHaveTheirRecipes(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        Object[][] slabs = {{Items.DIRT, NatureBlocks.DIRT_SLAB.asItem()}, {Items.GRASS_BLOCK, NatureBlocks.GRASS_SLAB.asItem()},
                {Items.SAND, NatureBlocks.SAND_SLAB.asItem()}, {Items.GRAVEL, NatureBlocks.GRAVEL_SLAB.asItem()}};
        for (Object[] pair : slabs) {
            ItemStack base = new ItemStack((Item) pair[0]);
            craft(helper, CraftingInput.of(3, 1, List.of(base, base, base)), "6 " + path((Item) pair[1]), missing);
        }
        ItemStack shell = new ItemStack(Items.NAUTILUS_SHELL);
        craft(helper, CraftingInput.of(2, 2, List.of(shell, shell, shell, shell)), "1 " + path(NatureBlocks.NAUTILUS_SHELL_BLOCK.asItem()), missing);
        craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(NatureBlocks.NAUTILUS_SHELL_BLOCK.asItem()))), "4 nautilus_shell", missing);
        Object[][] dyes = {{Items.DYE.pick(net.minecraft.world.item.DyeColor.RED), NatureBlocks.SCARLET_FROGLIGHT.asItem()}, {Items.DYE.pick(net.minecraft.world.item.DyeColor.CYAN), NatureBlocks.AQUA_FROGLIGHT.asItem()},
                {Items.DYE.pick(net.minecraft.world.item.DyeColor.BLUE), NatureBlocks.AZURE_FROGLIGHT.asItem()}};
        for (Object[] pair : dyes) {
            for (Item froglight : List.of(Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, NatureBlocks.AQUA_FROGLIGHT.asItem())) {
                craft(helper, CraftingInput.of(2, 1, List.of(new ItemStack(froglight), new ItemStack((Item) pair[0]))),
                        "1 " + path((Item) pair[1]), missing);
            }
        }
        Set<String> cuts = stonecutting(helper.getLevel());
        for (String cut : List.of("ice -> 1 cracked_ice", "packed_ice -> 1 chiseled_packed_ice", "blue_ice -> 1 chiseled_blue_ice")) {
            if (!cuts.contains(cut)) {
                missing.add("stonecutter " + cut);
            }
        }
        for (Block froglight : List.of(NatureBlocks.SCARLET_FROGLIGHT, NatureBlocks.AQUA_FROGLIGHT, NatureBlocks.AZURE_FROGLIGHT)) {
            helper.assertValueEqual(froglight.defaultBlockState().getLightEmission(), 15, "light of " + froglight);
        }
        helper.assertTrue(missing.isEmpty(), "nature variant recipes: " + missing);
        helper.succeed();
    }

    /**
     * Gras-Stufe wie der Grasblock: ohne Behutsamkeit die Erd-Stufe, mit Behutsamkeit sie selbst; als Doppelstufe je zwei.
     */
    public static void grassSlabDropsDirtSlabWithoutSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack silk = new ItemStack(Items.IRON_SHOVEL);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockState single = NatureBlocks.GRASS_SLAB.defaultBlockState();
        BlockState doubled = single.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        helper.assertValueEqual(drops(level, pos, single, shovel), "1 dirt_slab", "grass slab mined with a shovel");
        helper.assertValueEqual(drops(level, pos, doubled, shovel), "2 dirt_slab", "double grass slab mined with a shovel");
        helper.assertValueEqual(drops(level, pos, single, silk), "1 grass_slab", "grass slab mined with silk touch");
        helper.assertValueEqual(drops(level, pos, doubled, silk), "2 grass_slab", "double grass slab mined with silk touch");
        helper.assertValueEqual(drops(level, pos, NatureBlocks.CRACKED_ICE.defaultBlockState(), new ItemStack(Items.IRON_PICKAXE)), "",
                "cracked ice mined without silk touch");
        helper.succeed();
    }

    /**
     * Sand- und Kies-Stufe fallen wie Sand: eine untere Stufe landet als untere, eine obere faellt als untere; eine untere
     * Stufe, die auf eine untere derselben Art faellt, wird mit ihr zur Doppelstufe (kein Item). Die Erd-Stufe faellt nicht.
     */
    public static void sandAndGravelSlabsFallAndMerge(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(1, 5, 1), NatureBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(3, 5, 1), NatureBlocks.GRAVEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(5, 2, 1), NatureBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(5, 6, 1), NatureBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(6, 5, 1), NatureBlocks.DIRT_SLAB.defaultBlockState());
        helper.succeedWhen(() -> {
            expectSlab(helper, new BlockPos(1, 2, 1), NatureBlocks.SAND_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(3, 2, 1), NatureBlocks.GRAVEL_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 1)).is(Blocks.SAND),
                    "two sand slabs did not land as a full sand block: " + helper.getBlockState(new BlockPos(5, 2, 1)));
            expectSlab(helper, new BlockPos(6, 5, 1), NatureBlocks.DIRT_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 5, 1)).isAir() && helper.getBlockState(new BlockPos(5, 6, 1)).isAir(),
                    "a falling slab left its old place filled");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty(),
                    "a landing slab broke into an item");
        });
    }

    /**
     * Besitzer N31: wer eine zweite Stufe derselben Art auf eine untere Erd-, Gras-, Sand- oder Kies-Stufe setzt, bekommt
     * den vollen Vanilla-Block (Erde, Grasblock, Sand, Kies) statt einer Doppelstufe; das Item wird verbraucht.
     */
    public static void naturalSlabsPlaceIntoTheFullBlock(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Block[][] pairs = {{NatureBlocks.DIRT_SLAB, Blocks.DIRT}, {NatureBlocks.GRASS_SLAB, Blocks.GRASS_BLOCK},
                {NatureBlocks.SAND_SLAB, Blocks.SAND}, {NatureBlocks.GRAVEL_SLAB, Blocks.GRAVEL}};
        for (int i = 0; i < pairs.length; i++) {
            BlockPos pos = new BlockPos(1 + 2 * i, 2, 1);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, pairs[i][0].defaultBlockState());
            ItemStack stack = new ItemStack(pairs[i][0].asItem(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos abs = helper.absolutePos(pos);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            helper.assertTrue(helper.getBlockState(pos).is(pairs[i][1]), "two " + pairs[i][0] + " gave "
                    + helper.getBlockState(pos) + " instead of " + pairs[i][1]);
            helper.assertValueEqual(stack.getCount(), 1, "slabs left after placing the second " + pairs[i][0]);
        }
        helper.succeed();
    }

    /**
     * Rissiges Eis wird unter einem Lebewesen (Ruestungsstaender) erst nach einigen Sekunden zu Wasser, reisst dabei sichtbar
     * (AGE steigt); ohne jemanden darauf bleibt es unversehrt.
     */
    public static void crackedIceMeltsUnderAnEntityAfterSomeSeconds(GameTestHelper helper) {
        BlockPos stoodOn = new BlockPos(1, 1, 1);
        BlockPos alone = new BlockPos(4, 1, 1);
        helper.setBlock(stoodOn, NatureBlocks.CRACKED_ICE);
        helper.setBlock(alone, NatureBlocks.CRACKED_ICE);
        ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
        helper.assertTrue(stand != null, "no armor stand");
        int[] seenAge = {0};
        helper.runAfterDelay(CrackedIceBlock.TICKS_PER_CRACK / 2, () -> helper.assertTrue(
                helper.getBlockState(stoodOn).is(NatureBlocks.CRACKED_ICE), "cracked ice melted at once"));
        helper.onEachTick(() -> {
            BlockState state = helper.getBlockState(stoodOn);
            if (state.is(NatureBlocks.CRACKED_ICE)) {
                seenAge[0] = Math.max(seenAge[0], state.getValue(CrackedIceBlock.AGE));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(stoodOn).isAir(), "cracked ice under the stand breaks away without water, is "
                    + helper.getBlockState(stoodOn));
            helper.assertValueEqual(seenAge[0], CrackedIceBlock.MAX_AGE, "highest crack seen before melting");
            helper.assertTrue(helper.getBlockState(alone).is(NatureBlocks.CRACKED_ICE)
                    && helper.getBlockState(alone).getValue(CrackedIceBlock.AGE) == 0, "cracked ice nobody stood on changed: "
                    + helper.getBlockState(alone));
        });
    }

    /**
     * Besitzer N34: Sand-/Kies-Stufen landen auf Stufen. (a) Eine untere Stufe direkt ueber einer unteren derselben Art faellt
     * den halben Block und beide werden voll; (b) eine obere Stufe ueber einem vollen Block oder einer oberen Stufe faellt
     * in ihrer Zelle nach unten (schwebte vorher); eine obere ueber einer unteren derselben Art wird mit ihr voll; ueber
     * einer fremden unteren Stufe rutscht sie in der eigenen Zelle nach unten. Kein Item.
     */
    public static void gravitySlabsSettleOnSlabsAndHalfSteps(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);
        }
        BlockState sand = NatureBlocks.SAND_SLAB.defaultBlockState();
        BlockState gravel = NatureBlocks.GRAVEL_SLAB.defaultBlockState();
        helper.setBlock(new BlockPos(0, 2, 1), sand);
        helper.setBlock(new BlockPos(0, 3, 1), sand);
        helper.setBlock(new BlockPos(2, 2, 1), sand.setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(4, 2, 1), gravel);
        helper.setBlock(new BlockPos(4, 3, 1), gravel.setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(6, 2, 1), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(6, 3, 1), sand.setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(0, 2, 4), Blocks.STONE_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(0, 3, 4), sand.setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(3, 2, 4), sand);
        helper.setBlock(new BlockPos(3, 5, 4), sand.setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(0, 2, 1)).is(Blocks.SAND) && helper.getBlockState(new BlockPos(0, 3, 1)).isAir(),
                    "(a) a sand slab right above a lower sand slab did not drop onto it: " + helper.getBlockState(new BlockPos(0, 2, 1))
                            + " / " + helper.getBlockState(new BlockPos(0, 3, 1)));
            expectSlab(helper, new BlockPos(2, 2, 1), NatureBlocks.SAND_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 1)).is(Blocks.GRAVEL) && helper.getBlockState(new BlockPos(4, 3, 1)).isAir(),
                    "an upper gravel slab above a lower one did not merge: " + helper.getBlockState(new BlockPos(4, 2, 1)));
            expectSlab(helper, new BlockPos(6, 3, 1), NatureBlocks.SAND_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(6, 2, 1), Blocks.STONE_SLAB, SlabType.TOP);
            expectSlab(helper, new BlockPos(0, 3, 4), NatureBlocks.SAND_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(0, 2, 4), Blocks.STONE_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 4)).is(Blocks.SAND) && helper.getBlockState(new BlockPos(3, 5, 4)).isAir(),
                    "an upper sand slab falling from higher up did not merge with the lower one: " + helper.getBlockState(new BlockPos(3, 2, 4)));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, helper.getBounds()).isEmpty(),
                    "a slab is still falling");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty(),
                    "a landing slab broke into an item");
        });
    }

    /**
     * Besitzer N34 (c): eine obere Sand-Stufe startet ihren Fall auf ihrer sichtbaren Hoehe (halber Block ueber dem Zellboden)
     * statt im ersten Tick einen halben Block tiefer aufzutauchen, und landet unten als untere Stufe.
     */
    public static void upperGravitySlabStartsFallingFromItsOwnHeight(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        BlockPos rel = new BlockPos(2, 4, 2);
        helper.setBlock(rel, NatureBlocks.SAND_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        BlockPos abs = helper.absolutePos(rel);
        ServerLevel level = helper.getLevel();
        helper.getBlockState(rel).tick(level, abs, level.getRandom());
        List<net.minecraft.world.entity.item.FallingBlockEntity> falling = level.getEntitiesOfClass(
                net.minecraft.world.entity.item.FallingBlockEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(1.0));
        helper.assertTrue(falling.size() == 1, "expected one falling slab, found " + falling.size());
        var entity = falling.get(0);
        helper.assertTrue(Math.abs(entity.getY() - (abs.getY() + 0.5)) < 1.0E-6 && Math.abs(entity.yo - (abs.getY() + 0.5)) < 1.0E-6,
                "the upper slab starts falling at y " + entity.getY() + " (old " + entity.yo + "), not at its visible " + (abs.getY() + 0.5));
        helper.assertTrue(entity.getBlockState().is(NatureBlocks.SAND_SLAB) && entity.getBlockState().getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "the falling slab renders as " + entity.getBlockState());
        helper.assertTrue(helper.getBlockState(rel).isAir(), "the slab left its block behind: " + helper.getBlockState(rel));
        helper.succeedWhen(() -> {
            expectSlab(helper, new BlockPos(2, 2, 2), NatureBlocks.SAND_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty(), "the slab broke");
        });
    }

    /**
     * B23-M3: grass lives on slabs like Vanilla grass - a grass block spreads onto a dirt slab (bottom stays bottom), a grass
     * slab spreads onto dirt, a covered grass slab dies to a dirt slab. Random ticks are called directly; glowstone gives the
     * light (9 or more) above them.
     */
    public static void grassSpreadsOntoAndFromSlabs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos grass = new BlockPos(1, 1, 3);
        BlockPos slab = new BlockPos(2, 1, 3);
        BlockPos dirt = new BlockPos(3, 1, 3);
        BlockPos covered = new BlockPos(5, 1, 3);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        helper.setBlock(slab, NatureBlocks.DIRT_SLAB);
        helper.setBlock(dirt, Blocks.DIRT);
        helper.setBlock(covered, NatureBlocks.GRASS_SLAB);
        helper.setBlock(covered.above(), Blocks.STONE);
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 3, 4), Blocks.GLOWSTONE);
        }
        net.minecraft.util.RandomSource random = level.getRandom();
        helper.succeedWhen(() -> {
            for (int i = 0; i < 20; i++) {
                for (BlockPos at : List.of(slab, covered)) {
                    helper.getBlockState(at).randomTick(level, helper.absolutePos(at), random);
                }
            }
            BlockState grown = helper.getBlockState(slab);
            helper.assertTrue(grown.is(NatureBlocks.GRASS_SLAB) && grown.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                    "the dirt slab next to grass became " + grown);
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, dirt);
            helper.assertTrue(helper.getBlockState(covered).is(NatureBlocks.DIRT_SLAB), "the covered grass slab is " + helper.getBlockState(covered));
        });
    }

    /** Rissiges Eis: abgebaut bleibt kein Wasser zurueck (Eis auf Stein wuerde zu Wasser). Den Meissel-Weg prueft SimpleBuilding. */
    public static void crackedIceLeavesNoWater(GameTestHelper helper) {
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos ice = stone.above();
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(ice, NatureBlocks.CRACKED_ICE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.gameMode.destroyBlock(helper.absolutePos(ice));
        helper.assertTrue(helper.getBlockState(ice).isAir(), "mined cracked ice leaves " + helper.getBlockState(ice));
        helper.setBlock(ice, Blocks.ICE);
        player.gameMode.destroyBlock(helper.absolutePos(ice));
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.WATER), "control: mined plain ice on stone leaves water, is " + helper.getBlockState(ice));
        helper.succeed();
    }

    /**
     * N53: an keine der sechs Seiten eines rissigen Eisblocks laesst sich ein Block setzen (neben normalem Eis schon; B23-M2: Eis und
     * rissiges Eis selbst schon), und
     * Eis und rissiges Eis blenden die Flaechen zueinander aus wie Eis neben Eis (getarnt, keine Innenraender).
     */
    public static void crackedIceBlocksPlacementOnAllSidesAndBlendsWithIce(GameTestHelper helper) {
        BlockPos centre = new BlockPos(3, 3, 3);
        helper.setBlock(centre, NatureBlocks.CRACKED_ICE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        for (Direction direction : Direction.values()) {
            BlockPos target = centre.relative(direction);
            helper.setBlock(target, Blocks.AIR);
            ItemStack stone = new ItemStack(Items.STONE, 4);
            var result = placeAt(helper, player, stone, target);
            helper.assertTrue(!result.consumesAction() && helper.getBlockState(target).isAir() && stone.getCount() == 4,
                    "placing stone " + direction + " of cracked ice gave " + result + ", block " + helper.getBlockState(target));
        }
        BlockPos ice = new BlockPos(6, 3, 6);
        helper.setBlock(ice, Blocks.ICE);
        ItemStack stone = new ItemStack(Items.STONE, 4);
        var control = placeAt(helper, player, stone, ice.above());
        helper.assertTrue(control.consumesAction() && helper.getBlockState(ice.above()).is(Blocks.STONE),
                "control: stone on normal ice must still be placed, gave " + control);
        // B23-M2: cracked ice and ice go next to cracked ice (camouflaged fields)
        for (Direction direction : List.of(Direction.UP, Direction.EAST, Direction.DOWN)) {
            BlockPos target = centre.relative(direction);
            helper.setBlock(target, Blocks.AIR);
            ItemStack more = new ItemStack(NatureBlocks.CRACKED_ICE.asItem(), 2);
            var placed = placeAt(helper, player, more, target);
            helper.assertTrue(placed.consumesAction() && helper.getBlockState(target).is(NatureBlocks.CRACKED_ICE),
                    "cracked ice " + direction + " of cracked ice gave " + placed + ", block " + helper.getBlockState(target));
            helper.setBlock(target, Blocks.AIR);
        }
        BlockPos west = centre.west();
        helper.setBlock(west, Blocks.AIR);
        var icePlaced = placeAt(helper, player, new ItemStack(Items.ICE, 2), west);
        helper.assertTrue(icePlaced.consumesAction() && helper.getBlockState(west).is(Blocks.ICE),
                "ice next to cracked ice gave " + icePlaced + ", block " + helper.getBlockState(west));
        BlockState cracked = NatureBlocks.CRACKED_ICE.defaultBlockState();
        BlockState plain = Blocks.ICE.defaultBlockState();
        for (Direction direction : Direction.values()) {
            helper.assertTrue(plain.skipRendering(cracked, direction), "ice draws its face towards cracked ice (" + direction + ")");
            helper.assertTrue(cracked.skipRendering(plain, direction), "cracked ice draws its face towards ice (" + direction + ")");
            helper.assertTrue(cracked.skipRendering(cracked.setValue(CrackedIceBlock.AGE, 3), direction), "cracked ice towards cracked ice");
        }
        helper.assertTrue(!plain.skipRendering(Blocks.GLASS.defaultBlockState(), Direction.UP), "control: ice still draws its face towards glass");
        helper.succeed();
    }

    private static net.minecraft.world.InteractionResult placeAt(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos target) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(target);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return ((net.minecraft.world.item.BlockItem) stack.getItem()).place(
                new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
    }

    /** N53: mit Behutsamkeit abgebaut droppt rissiges Eis sich selbst (auch auf dem echten Abbauweg), ohne nichts. */
    public static void crackedIceDropsItselfWithSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.assertValueEqual(drops(level, helper.absolutePos(pos), NatureBlocks.CRACKED_ICE.defaultBlockState().setValue(CrackedIceBlock.AGE, 2), silk),
                "1 cracked_ice", "cracked ice mined with silk touch");
        helper.assertValueEqual(drops(level, helper.absolutePos(pos), NatureBlocks.CRACKED_ICE.defaultBlockState(), new ItemStack(Items.IRON_PICKAXE)), "",
                "cracked ice mined without silk touch");
        helper.setBlock(pos, NatureBlocks.CRACKED_ICE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, silk);
        player.gameMode.destroyBlock(helper.absolutePos(pos));
        helper.assertTrue(helper.getBlockState(pos).isAir(), "mined cracked ice leaves " + helper.getBlockState(pos));
        int dropped = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2))
                .stream().filter(e -> e.getItem().is(NatureBlocks.CRACKED_ICE.asItem())).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertValueEqual(dropped, 1, "cracked ice items dropped by a silk touch pickaxe");
        helper.succeed();
    }

    // ===================================================================================== moved from SimpleBuilding

    /** The 15 blocks with their items, the rising block entity and their tags (shovel/pickaxe, slabs, ice, froglights). */
    public static void movedBlocksRegistered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        if (!NatureBlocks.NAMES.equals(com.simpleblocks.nature.NatureIds.NAMES)) problems.add("NatureIds.NAMES differs from the registered blocks: " + NatureBlocks.NAMES);
        for (String name : NatureBlocks.NAMES) {
            if (BuiltInRegistries.BLOCK.getValue(SimpleBlocks.id(name)) != NatureBlocks.BLOCKS.get(name)) problems.add("block " + name);
            if (BuiltInRegistries.ITEM.getValue(SimpleBlocks.id(name)) != NatureBlocks.ITEMS.get(name)) problems.add("item " + name);
        }
        if (NatureBlocks.LEVITATING_BLOCK == null || BuiltInRegistries.ENTITY_TYPE.getValue(SimpleBlocks.id(NatureBlocks.LEVITATING_ENTITY)) != NatureBlocks.LEVITATING_BLOCK) {
            problems.add("entity type levitating_block");
        }
        for (Block slab : List.of(NatureBlocks.DIRT_SLAB, NatureBlocks.GRASS_SLAB, NatureBlocks.SAND_SLAB, NatureBlocks.GRAVEL_SLAB)) {
            BlockState state = slab.defaultBlockState();
            if (!state.is(net.minecraft.tags.BlockTags.SLABS) || !state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)
                    || !new ItemStack(slab).is(net.minecraft.tags.TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.withDefaultNamespace("slabs")))) problems.add("slab tags of " + slab);
        }
        for (Block ice : List.of(NatureBlocks.CRACKED_ICE, NatureBlocks.CHISELED_PACKED_ICE, NatureBlocks.CHISELED_BLUE_ICE)) {
            if (!ice.defaultBlockState().is(net.minecraft.tags.BlockTags.ICE) || !ice.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) problems.add("ice tags of " + ice);
        }
        if (!NatureBlocks.NAUTILUS_SHELL_BLOCK.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) problems.add("nautilus shell block pickaxe");
        // Sand/gravel copies dig like Vanilla sand: fast with a shovel (AUDM24).
        for (Block gravity : List.of(NatureBlocks.SUSPENDED_SAND, NatureBlocks.SUSPENDED_GRAVEL, NatureBlocks.LEVITATING_SAND, NatureBlocks.LEVITATING_GRAVEL)) {
            if (!gravity.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) problems.add("shovel tag of " + gravity);
        }
        var froglights = net.minecraft.tags.TagKey.create(Registries.ITEM, SimpleBlocks.id("froglights"));
        for (Item froglight : List.of(Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, NatureBlocks.SCARLET_FROGLIGHT.asItem(),
                NatureBlocks.AQUA_FROGLIGHT.asItem(), NatureBlocks.AZURE_FROGLIGHT.asItem())) {
            if (!new ItemStack(froglight).is(froglights)) problems.add("#simpleblocks:froglights lacks " + froglight);
        }
        helper.assertTrue(problems.isEmpty(), "moved blocks: " + problems);
        helper.succeed();
    }

    /**
     * Old worlds of SimpleBuilding: every {@code simplebuilding:} id of the moved blocks (blocks, items, the rising block entity,
     * recipe book, recipe advancements) resolves to the id here (SimpleLib IdMoves; chunk palettes and item stacks look ids up the same way;
     * SimpleBuilding's MigrationWorldTests load a whole old chunk).
     */
    public static void movedIdsFromSimplebuilding(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (String name : NatureBlocks.NAMES) {
            if (BuiltInRegistries.BLOCK.getValue(SimpleBlocks.old(name)) != NatureBlocks.BLOCKS.get(name)) problems.add("block " + name);
            if (BuiltInRegistries.ITEM.getValue(SimpleBlocks.old(name)) != NatureBlocks.ITEMS.get(name)) problems.add("item " + name);
            // Chunk palette entry of an old world: 26.3 names the block under "id" and decodes it through BlockState.CODEC (REV2-A).
            var paletteTag = net.minecraft.nbt.NbtUtils.writeBlockState(NatureBlocks.BLOCKS.get(name).defaultBlockState());
            if (!paletteTag.contains("id")) problems.add("palette entry without id: " + paletteTag);
            paletteTag.remove("Name");
            paletteTag.putString("id", SimpleBlocks.old(name).toString());
            var decoded = net.minecraft.world.level.block.state.BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, paletteTag).result().orElse(null);
            if (decoded == null || !decoded.is(NatureBlocks.BLOCKS.get(name))) problems.add("palette " + name + " loads as " + decoded);
        }
        if (BuiltInRegistries.ENTITY_TYPE.getValue(SimpleBlocks.old(NatureBlocks.LEVITATING_ENTITY)) != NatureBlocks.LEVITATING_BLOCK) problems.add("entity levitating_block");
        NatureBlocks.MOVED_RECIPES.forEach((path, folder) -> {
            if (!SimpleBlocks.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleBlocks.old(path)))) problems.add("recipe " + path);
            String advancement = "recipes/" + folder + "/" + path;
            if (!SimpleBlocks.id(advancement).equals(IdMoves.advancement(SimpleBlocks.old(advancement)))) problems.add("advancement " + advancement);
        });
        helper.assertTrue(problems.isEmpty(), "moved ids: " + problems);
        helper.succeed();
    }

    /**
     * Gravity blocks (principles 1 and 5): with SimpleBuilding 8 sand/gravel + a Nihilith shard (suspended) or Astralit dust
     * (levitating), as before the move; alone the Vanilla way 8 + phantom membrane / breeze rod. Only one way at a time.
     */
    public static void gravityRecipesWithAndWithoutSimplebuilding(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        boolean sb = BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "nihilith_shard"));
        Item suspend = sb ? BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "nihilith_shard")) : Items.PHANTOM_MEMBRANE;
        Item lift = sb ? BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "astralit_dust")) : Items.BREEZE_ROD;
        for (Item base : List.of(Items.SAND, Items.GRAVEL)) {
            String kind = path(base);
            craft(helper, ring(base, suspend), "8 suspended_" + kind, missing);
            craft(helper, ring(base, lift), "8 levitating_" + kind, missing);
            if (sb) {
                // The Vanilla way is off while SimpleBuilding's way is there.
                craft(helper, ring(base, Items.PHANTOM_MEMBRANE), "nothing", missing);
                craft(helper, ring(base, Items.BREEZE_ROD), "nothing", missing);
            }
        }
        helper.assertTrue(missing.isEmpty(), "gravity block recipes (simplebuilding=" + sb + "): " + missing);
        helper.succeed();
    }

    private static CraftingInput ring(Item base, Item centre) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(new ItemStack(i == 4 ? centre : base));
        return CraftingInput.of(3, 3, grid);
    }

    /** Suspended sand/gravel stay in the air while Vanilla sand falls; levitating sand rises and lands under a ceiling. */
    public static void suspendedBlocksStayLevitatingBlocksRise(GameTestHelper helper) {
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        BlockPos sand = new BlockPos(1, 4, 1);
        BlockPos suspended = new BlockPos(3, 4, 1);
        BlockPos suspendedGravel = new BlockPos(5, 4, 1);
        helper.setBlock(sand, Blocks.SAND);
        helper.setBlock(suspended, NatureBlocks.SUSPENDED_SAND);
        helper.setBlock(suspendedGravel, NatureBlocks.SUSPENDED_GRAVEL);
        BlockPos start = new BlockPos(3, 1, 5);
        BlockPos ceiling = new BlockPos(3, 6, 5);
        helper.setBlock(start.below(), Blocks.STONE);
        helper.setBlock(ceiling, Blocks.STONE);
        helper.setBlock(start, NatureBlocks.LEVITATING_SAND);
        BlockPos gravelStart = new BlockPos(5, 1, 5);
        helper.setBlock(gravelStart.below(), Blocks.STONE);
        helper.setBlock(gravelStart.above(5), Blocks.STONE);
        helper.setBlock(gravelStart, NatureBlocks.LEVITATING_GRAVEL);
        boolean[] flew = {false};
        helper.onEachTick(() -> {
            if (!helper.getLevel().getEntitiesOfClass(com.simpleblocks.nature.LevitatingBlockEntity.class, helper.getBounds()).isEmpty()) flew[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(floor.above()).is(Blocks.SAND), "control: Vanilla sand fell onto the floor");
            helper.assertTrue(helper.getBlockState(suspended).is(NatureBlocks.SUSPENDED_SAND) && helper.getBlockState(suspendedGravel).is(NatureBlocks.SUSPENDED_GRAVEL),
                    "suspended sand and gravel stay where they were put");
            helper.assertTrue(helper.getBlockState(ceiling.below()).is(NatureBlocks.LEVITATING_SAND) && helper.getBlockState(start).isAir(),
                    "levitating sand rose and landed under the ceiling: " + helper.getBlockState(ceiling.below()));
            helper.assertTrue(helper.getBlockState(gravelStart.above(4)).is(NatureBlocks.LEVITATING_GRAVEL), "levitating gravel rose to its ceiling");
            helper.assertTrue(flew[0], "the levitating blocks rose as the rising block entity");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty(), "a landing block broke into an item");
        });
    }

    /**
     * Tab stage T2: the moved blocks stand in this module's tab, in their order behind the own blocks and before the guide
     * book; they go into the Vanilla tabs by this module only without SimpleBuilding (else its search-tab chains place them).
     */
    public static void movedBlocksInTheTab(GameTestHelper helper) {
        List<ItemStack> tab = com.simpleblocks.BlocksRegistry.tabStacks();
        List<Item> items = tab.stream().map(ItemStack::getItem).toList();
        int first = items.indexOf(NatureBlocks.SUSPENDED_SAND.asItem());
        helper.assertTrue(first > 0, "suspended sand in the Simple Blocks tab");
        for (int i = 0; i < NatureBlocks.NAMES.size(); i++) {
            Item expected = NatureBlocks.ITEMS.get(NatureBlocks.NAMES.get(i));
            helper.assertTrue(first + i < items.size() && items.get(first + i) == expected, "tab slot " + (first + i) + " should be " + expected + ": " + items);
        }
        helper.assertTrue(items.indexOf(com.simpleblocks.guide.BlocksGuide.stack().getItem()) > first, "the guide book stays last");
        boolean placed = com.simpleblocks.BlocksRegistry.placements().stream().anyMatch(p -> p.names().contains("cracked_ice"));
        helper.assertTrue(placed == !SimpleBlocks.simplebuildingLoaded, "Vanilla tab placement of the moved blocks only without SimpleBuilding (placed="
                + placed + ", simplebuilding=" + SimpleBlocks.simplebuildingLoaded + ")");
        helper.succeed();
    }

    private static void expectSlab(GameTestHelper helper, BlockPos pos, Block slab, SlabType type) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(slab) && state.getValue(SlabBlock.TYPE) == type,
                "expected " + type + " " + slab + " at " + pos + ", found " + state);
    }

    private static String drops(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, tool);
        StringBuilder out = new StringBuilder();
        for (ItemStack stack : drops) {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append(stack.getCount()).append(' ').append(path(stack.getItem()));
        }
        return out.toString();
    }

    private static Set<String> stonecutting(ServerLevel level) {
        ContextMap context = SlotDisplayContext.fromLevel(level);
        Set<String> cuts = new TreeSet<>();
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe)) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                if (display instanceof StonecutterRecipeDisplay cut) {
                    for (ItemStack in : cut.input().resolveForStacks(context)) {
                        for (ItemStack out : cut.result().resolveForStacks(context)) {
                            cuts.add(path(in.getItem()) + " -> " + out.getCount() + " " + path(out.getItem()));
                        }
                    }
                }
            }
        }
        return cuts;
    }

    private static void craft(GameTestHelper helper, CraftingInput grid, String expected, List<String> missing) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        String got = match.map(m -> {
            ItemStack result = m.value().assemble(grid);
            return result.getCount() + " " + path(result.getItem());
        }).orElse("nothing");
        if (!got.equals(expected)) {
            missing.add("crafting " + expected + " (got " + got + ")");
        }
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
