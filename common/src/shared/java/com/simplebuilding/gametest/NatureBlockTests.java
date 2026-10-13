package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CrackedIceBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
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
 * Naturvarianten (QUEUE N24/N25, docs/ai/PLAN-Q-BLOCKS-2026-10-09.md): Rezepte, Beute der Gras-Stufe, fallende Sand-/
 * Kies-Stufen und rissiges Eis, das unter einem Lebewesen nach einigen Sekunden zu Wasser wird. Ohne
 * {@link McVersion#NATURE_VARIANTS} (26.2) bestehen die Tests ohne Pruefung.
 */
public final class NatureBlockTests {
    /** Rissiges Eis: vier Stufen a {@link CrackedIceBlock#TICKS_PER_CRACK} plus Fallen und Reserve. */
    public static final int CRACKED_ICE_MAX_TICKS = 200;
    public static final int FALLING_SLAB_MAX_TICKS = 100;

    private NatureBlockTests() {
    }

    /**
     * Werkbank: 3 Erde/Grasblock/Sand/Kies nebeneinander -> 6 Stufen; 2x2 Nautilusschalen -> Block, Block -> 4 Schalen;
     * Froschlicht (Vanilla oder Mod) + Rot/Cyan/Blau -> Scharlach/Aqua/Azur. Steinmetz: Eis -> rissiges Eis,
     * Packeis/Blaueis -> gemeisselt. Die Froschlichter leuchten wie Vanillas mit 15.
     */
    public static void natureVariantsHaveTheirRecipes(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        List<String> missing = new ArrayList<>();
        Object[][] slabs = {{Items.DIRT, ModItems.DIRT_SLAB}, {Items.GRASS_BLOCK, ModItems.GRASS_SLAB},
                {Items.SAND, ModItems.SAND_SLAB}, {Items.GRAVEL, ModItems.GRAVEL_SLAB}};
        for (Object[] pair : slabs) {
            ItemStack base = new ItemStack((Item) pair[0]);
            craft(helper, CraftingInput.of(3, 1, List.of(base, base, base)), "6 " + path((Item) pair[1]), missing);
        }
        ItemStack shell = new ItemStack(Items.NAUTILUS_SHELL);
        craft(helper, CraftingInput.of(2, 2, List.of(shell, shell, shell, shell)), "1 " + path(ModItems.NAUTILUS_SHELL_BLOCK), missing);
        craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(ModItems.NAUTILUS_SHELL_BLOCK))), "4 nautilus_shell", missing);
        Object[][] dyes = {{Items.DYE.pick(net.minecraft.world.item.DyeColor.RED), ModItems.SCARLET_FROGLIGHT}, {Items.DYE.pick(net.minecraft.world.item.DyeColor.CYAN), ModItems.AQUA_FROGLIGHT},
                {Items.DYE.pick(net.minecraft.world.item.DyeColor.BLUE), ModItems.AZURE_FROGLIGHT}};
        for (Object[] pair : dyes) {
            for (Item froglight : List.of(Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, ModItems.AQUA_FROGLIGHT)) {
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
        for (Block froglight : List.of(ModBlocks.SCARLET_FROGLIGHT, ModBlocks.AQUA_FROGLIGHT, ModBlocks.AZURE_FROGLIGHT)) {
            helper.assertValueEqual(froglight.defaultBlockState().getLightEmission(), 15, "light of " + froglight);
        }
        helper.assertTrue(missing.isEmpty(), "nature variant recipes: " + missing);
        helper.succeed();
    }

    /**
     * Gras-Stufe wie der Grasblock: ohne Behutsamkeit die Erd-Stufe, mit Behutsamkeit sie selbst; als Doppelstufe je zwei.
     */
    public static void grassSlabDropsDirtSlabWithoutSilkTouch(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack silk = new ItemStack(Items.IRON_SHOVEL);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockState single = ModBlocks.GRASS_SLAB.defaultBlockState();
        BlockState doubled = single.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        helper.assertValueEqual(drops(level, pos, single, shovel), "1 dirt_slab", "grass slab mined with a shovel");
        helper.assertValueEqual(drops(level, pos, doubled, shovel), "2 dirt_slab", "double grass slab mined with a shovel");
        helper.assertValueEqual(drops(level, pos, single, silk), "1 grass_slab", "grass slab mined with silk touch");
        helper.assertValueEqual(drops(level, pos, doubled, silk), "2 grass_slab", "double grass slab mined with silk touch");
        helper.assertValueEqual(drops(level, pos, ModBlocks.CRACKED_ICE.defaultBlockState(), new ItemStack(Items.IRON_PICKAXE)), "",
                "cracked ice mined without silk touch");
        helper.succeed();
    }

    /**
     * Sand- und Kies-Stufe fallen wie Sand: eine untere Stufe landet als untere, eine obere faellt als untere; eine untere
     * Stufe, die auf eine untere derselben Art faellt, wird mit ihr zur Doppelstufe (kein Item). Die Erd-Stufe faellt nicht.
     */
    public static void sandAndGravelSlabsFallAndMerge(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        for (int x = 0; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(1, 5, 1), ModBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(3, 5, 1), ModBlocks.GRAVEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(5, 2, 1), ModBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(5, 6, 1), ModBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(6, 5, 1), ModBlocks.DIRT_SLAB.defaultBlockState());
        helper.succeedWhen(() -> {
            expectSlab(helper, new BlockPos(1, 2, 1), ModBlocks.SAND_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(3, 2, 1), ModBlocks.GRAVEL_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 1)).is(Blocks.SAND),
                    "two sand slabs did not land as a full sand block: " + helper.getBlockState(new BlockPos(5, 2, 1)));
            expectSlab(helper, new BlockPos(6, 5, 1), ModBlocks.DIRT_SLAB, SlabType.BOTTOM);
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
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Block[][] pairs = {{ModBlocks.DIRT_SLAB, Blocks.DIRT}, {ModBlocks.GRASS_SLAB, Blocks.GRASS_BLOCK},
                {ModBlocks.SAND_SLAB, Blocks.SAND}, {ModBlocks.GRAVEL_SLAB, Blocks.GRAVEL}};
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
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        BlockPos stoodOn = new BlockPos(1, 1, 1);
        BlockPos alone = new BlockPos(4, 1, 1);
        helper.setBlock(stoodOn, ModBlocks.CRACKED_ICE);
        helper.setBlock(alone, ModBlocks.CRACKED_ICE);
        ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
        helper.assertTrue(stand != null, "no armor stand");
        int[] seenAge = {0};
        helper.runAfterDelay(CrackedIceBlock.TICKS_PER_CRACK / 2, () -> helper.assertTrue(
                helper.getBlockState(stoodOn).is(ModBlocks.CRACKED_ICE), "cracked ice melted at once"));
        helper.onEachTick(() -> {
            BlockState state = helper.getBlockState(stoodOn);
            if (state.is(ModBlocks.CRACKED_ICE)) {
                seenAge[0] = Math.max(seenAge[0], state.getValue(CrackedIceBlock.AGE));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(stoodOn).isAir(), "cracked ice under the stand breaks away without water, is "
                    + helper.getBlockState(stoodOn));
            helper.assertValueEqual(seenAge[0], CrackedIceBlock.MAX_AGE, "highest crack seen before melting");
            helper.assertTrue(helper.getBlockState(alone).is(ModBlocks.CRACKED_ICE)
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
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        for (int x = 0; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);
        }
        BlockState sand = ModBlocks.SAND_SLAB.defaultBlockState();
        BlockState gravel = ModBlocks.GRAVEL_SLAB.defaultBlockState();
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
            expectSlab(helper, new BlockPos(2, 2, 1), ModBlocks.SAND_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 1)).is(Blocks.GRAVEL) && helper.getBlockState(new BlockPos(4, 3, 1)).isAir(),
                    "an upper gravel slab above a lower one did not merge: " + helper.getBlockState(new BlockPos(4, 2, 1)));
            expectSlab(helper, new BlockPos(6, 3, 1), ModBlocks.SAND_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(6, 2, 1), Blocks.STONE_SLAB, SlabType.TOP);
            expectSlab(helper, new BlockPos(0, 3, 4), ModBlocks.SAND_SLAB, SlabType.BOTTOM);
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
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        BlockPos rel = new BlockPos(2, 4, 2);
        helper.setBlock(rel, ModBlocks.SAND_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        BlockPos abs = helper.absolutePos(rel);
        ServerLevel level = helper.getLevel();
        helper.getBlockState(rel).tick(level, abs, level.getRandom());
        List<net.minecraft.world.entity.item.FallingBlockEntity> falling = level.getEntitiesOfClass(
                net.minecraft.world.entity.item.FallingBlockEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(1.0));
        helper.assertTrue(falling.size() == 1, "expected one falling slab, found " + falling.size());
        var entity = falling.get(0);
        helper.assertTrue(Math.abs(entity.getY() - (abs.getY() + 0.5)) < 1.0E-6 && Math.abs(entity.yo - (abs.getY() + 0.5)) < 1.0E-6,
                "the upper slab starts falling at y " + entity.getY() + " (old " + entity.yo + "), not at its visible " + (abs.getY() + 0.5));
        helper.assertTrue(entity.getBlockState().is(ModBlocks.SAND_SLAB) && entity.getBlockState().getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "the falling slab renders as " + entity.getBlockState());
        helper.assertTrue(helper.getBlockState(rel).isAir(), "the slab left its block behind: " + helper.getBlockState(rel));
        helper.succeedWhen(() -> {
            expectSlab(helper, new BlockPos(2, 2, 2), ModBlocks.SAND_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty(), "the slab broke");
        });
    }

    /** Rissiges Eis: abgebaut bleibt kein Wasser zurueck (Eis auf Stein wuerde zu Wasser), der Meissel macht Eis zu rissigem Eis. */
    public static void crackedIceLeavesNoWaterAndChiselMakesIt(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos ice = stone.above();
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(ice, ModBlocks.CRACKED_ICE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.gameMode.destroyBlock(helper.absolutePos(ice));
        helper.assertTrue(helper.getBlockState(ice).isAir(), "mined cracked ice leaves " + helper.getBlockState(ice));
        helper.setBlock(ice, Blocks.ICE);
        player.gameMode.destroyBlock(helper.absolutePos(ice));
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.WATER), "control: mined plain ice on stone leaves water, is " + helper.getBlockState(ice));
        helper.assertTrue(com.simplebuilding.items.custom.ChiselItem.FINAL_STONE_FWD.get(Blocks.ICE) == ModBlocks.CRACKED_ICE,
                "the stone chisel turns ice into cracked ice");
        helper.succeed();
    }

    /**
     * N53: an keine der sechs Seiten eines rissigen Eisblocks laesst sich ein Block setzen (neben normalem Eis schon), und
     * Eis und rissiges Eis blenden die Flaechen zueinander aus wie Eis neben Eis (getarnt, keine Innenraender).
     */
    public static void crackedIceBlocksPlacementOnAllSidesAndBlendsWithIce(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        BlockPos centre = new BlockPos(3, 3, 3);
        helper.setBlock(centre, ModBlocks.CRACKED_ICE);
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
        BlockState cracked = ModBlocks.CRACKED_ICE.defaultBlockState();
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
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.assertValueEqual(drops(level, helper.absolutePos(pos), ModBlocks.CRACKED_ICE.defaultBlockState().setValue(CrackedIceBlock.AGE, 2), silk),
                "1 cracked_ice", "cracked ice mined with silk touch");
        helper.assertValueEqual(drops(level, helper.absolutePos(pos), ModBlocks.CRACKED_ICE.defaultBlockState(), new ItemStack(Items.IRON_PICKAXE)), "",
                "cracked ice mined without silk touch");
        helper.setBlock(pos, ModBlocks.CRACKED_ICE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, silk);
        player.gameMode.destroyBlock(helper.absolutePos(pos));
        helper.assertTrue(helper.getBlockState(pos).isAir(), "mined cracked ice leaves " + helper.getBlockState(pos));
        int dropped = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2))
                .stream().filter(e -> e.getItem().is(ModItems.CRACKED_ICE)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertValueEqual(dropped, 1, "cracked ice items dropped by a silk touch pickaxe");
        helper.succeed();
    }

    /**
     * N53: die Rissiges-Eis-Felder haengen (Schalter an) im Schritt top_layer_modification an Eiszapfen und gefrorenen
     * Ozeanen, nicht an Ebenen; eine kleine Scheibe des registrierten Features macht aus einer Schneeflaeche rissiges Eis
     * ueber einer Grube (CrackedIceFieldChecks).
     */
    public static void crackedIceFieldsGenerateInIceSpikesAndFrozenOceans(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        helper.assertTrue(com.simplebuilding.config.ConfigFlags.test(com.simplebuilding.config.ConfigFlags.CRACKED_ICE_FIELDS),
                "the crackedIceFields switch is on by default");
        int top = net.minecraft.world.level.levelgen.GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
        for (var biome : List.of(net.minecraft.world.level.biome.Biomes.ICE_SPIKES, net.minecraft.world.level.biome.Biomes.FROZEN_OCEAN,
                net.minecraft.world.level.biome.Biomes.DEEP_FROZEN_OCEAN)) {
            helper.assertValueEqual(fieldStep(helper, biome), top, "cracked_ice_field_placed step in " + biome.identifier());
        }
        helper.assertValueEqual(fieldStep(helper, net.minecraft.world.level.biome.Biomes.PLAINS), -1, "cracked_ice_field_placed in plains");
        CrackedIceFieldChecks.placeSmallField(helper);
        helper.succeed();
    }

    private static int fieldStep(GameTestHelper helper, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key) {
        var biome = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getValue(key);
        helper.assertTrue(biome != null, "biome " + key.identifier() + " missing");
        var steps = biome.getGenerationSettings().features();
        for (int step = 0; step < steps.size(); step++) {
            for (var holder : steps.get(step)) {
                if (holder.is(com.simplebuilding.util.ModWorldGen.CRACKED_ICE_FIELD_PLACED_KEY)) {
                    return step;
                }
            }
        }
        return -1;
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
