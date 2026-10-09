package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ColorBrushItem;
import com.simplebuilding.component.PaintBoxContents;
import com.simplebuilding.items.custom.PaintBoxItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side contracts for the color brush. */
public final class ColorBrushTests {
    private ColorBrushTests() {}

    /** Strokes take the dyes like a bow takes arrows: hotbar slot 5 before inventory slot 20; one dye, one durability each. */
    public static void dyesAreUsedInBowOrder(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.getInventory().setItem(5, new ItemStack(item("white_dye")));
        player.getInventory().setItem(20, new ItemStack(item("red_dye")));
        helper.setBlock(rel, block("black_concrete"));
        helper.assertValueEqual(ColorBrushItem.inkKey(player), "white", "the brush tip shows the next dye");
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_concrete"), rel);
        helper.assertTrue(player.getInventory().getItem(5).isEmpty() && player.getInventory().getItem(20).getCount() == 1,
                "the hotbar dye goes first");
        helper.assertValueEqual(brush.getDamageValue(), 1, "one stroke costs one durability");
        helper.setBlock(rel, block("black_concrete"));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("red_concrete"), rel);
        helper.assertTrue(player.getInventory().getItem(20).isEmpty(), "then the inventory dye");
        helper.assertValueEqual(ColorBrushItem.inkKey(player), ColorBrushItem.INK_NONE, "no dye left");
        helper.setBlock(rel, block("black_concrete"));
        var result = stroke(player, pos, net.minecraft.util.RandomSource.create(1));
        helper.assertBlockPresent(block("black_concrete"), rel);
        helper.assertTrue(result == net.minecraft.world.InteractionResult.PASS && brush.getDamageValue() == 2,
                "without a dye the brush paints nothing (it brushes like the vanilla brush instead)");
        helper.succeed();
    }

    /** The other hand comes first, as for a bow; a paint box there beats a loose dye in the inventory. */
    public static void offHandComesFirst(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.getInventory().setItem(1, new ItemStack(item("red_dye"), 4));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("blue_dye"), 2));
        helper.setBlock(rel, block("white_wool"));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("blue_wool"), rel);
        helper.assertTrue(player.getOffhandItem().getCount() == 1 && player.getInventory().getItem(1).getCount() == 4,
                "the off-hand dye is used, the inventory dye stays");
        ItemStack box = box(ModItems.PAINT_BOX, new ItemStack(item("green_dye"), 3));
        player.setItemInHand(InteractionHand.OFF_HAND, box);
        helper.assertValueEqual(ColorBrushItem.inkKey(player), ColorBrushItem.INK_PALETTE, "the tip shows the paint box");
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("green_wool"), rel);
        helper.assertValueEqual(PaintBoxItem.contents(box).total(), 2, "the paint box gives one green dye");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 4, "the loose red dye stays");
        helper.succeed();
    }

    /** Creative strokes need a dye but use none and cost no durability. */
    public static void creativeUsesNothing(GameTestHelper helper) {
        var player = playerInLevel(helper);
        player.getAbilities().instabuild = true;
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.getInventory().setItem(3, new ItemStack(item("lime_dye")));
        helper.setBlock(rel, block("red_terracotta"));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("lime_terracotta"), rel);
        helper.assertTrue(player.getInventory().getItem(3).getCount() == 1 && brush.getDamageValue() == 0,
                "creative strokes are free");
        helper.succeed();
    }

    /** A paint box paints a random colour of its own (seeded: reproducible), never the block's colour, and gives that dye. */
    public static void paintBoxIsRandom(GameTestHelper helper) {
        ItemStack box = box(ModItems.PAINT_BOX, new ItemStack(item("red_dye"), 3), new ItemStack(item("blue_dye"), 3),
                new ItemStack(item("yellow_dye"), 3));
        java.util.Set<DyeColor> seen = new java.util.HashSet<>();
        net.minecraft.util.RandomSource a = net.minecraft.util.RandomSource.create(42), b = net.minecraft.util.RandomSource.create(42);
        for (int i = 0; i < 40; i++) {
            DyeColor pick = PaintBoxItem.pickColor(box, DyeColor.RED, a);
            helper.assertValueEqual(pick, PaintBoxItem.pickColor(box, DyeColor.RED, b), "same seed, same colour");
            seen.add(pick);
        }
        helper.assertTrue(seen.equals(java.util.Set.of(DyeColor.BLUE, DyeColor.YELLOW)),
                "random over the box's colours except the block's: " + seen);

        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.getInventory().setItem(2, box);
        helper.setBlock(rel, block("white_wool"));
        DyeColor expected = PaintBoxItem.pickColor(box, DyeColor.WHITE, net.minecraft.util.RandomSource.create(7));
        stroke(player, pos, net.minecraft.util.RandomSource.create(7));
        helper.assertBlockPresent(block(expected.getName() + "_wool"), rel);
        helper.assertValueEqual(PaintBoxItem.contents(box).total(), 8, "one dye leaves the box");
        helper.assertValueEqual(PaintBoxItem.contents(box).count(expected.getId()), 2, "exactly the painted colour");

        ItemStack only = box(ModItems.PAINT_BOX, new ItemStack(item("white_dye")));
        player.getInventory().setItem(2, only);
        helper.setBlock(rel, block("white_wool"));
        var result = stroke(player, pos, net.minecraft.util.RandomSource.create(7));
        helper.assertTrue(result == net.minecraft.world.InteractionResult.PASS && PaintBoxItem.contents(only).total() == 1
                && brush.getDamageValue() == 1, "a box with only the block's colour paints nothing");
        helper.succeed();
    }

    /** Only dyes go in, at most one stack per colour times the tier factor (64/128/256/512); clicks work like a bundle. */
    public static void paintBoxHoldsOneStackPerColourPerTier(GameTestHelper helper) {
        var player = playerInLevel(helper);
        ItemStack box = new ItemStack(ModItems.PAINT_BOX);
        var container = new net.minecraft.world.SimpleContainer(1);
        container.setItem(0, box);
        var slot = new net.minecraft.world.inventory.Slot(container, 0, 0, 0);
        ItemStack[] carried = {new ItemStack(Items.COBBLESTONE, 5)};
        var access = net.minecraft.world.entity.SlotAccess.of(() -> carried[0], stack -> carried[0] = stack);
        var primary = net.minecraft.world.inventory.ClickAction.PRIMARY;
        box.getItem().overrideOtherStackedOnMe(box, carried[0], slot, primary, player, access);
        helper.assertTrue(PaintBoxItem.contents(box).isEmpty() && carried[0].getCount() == 5, "cobblestone stays out");

        carried[0] = new ItemStack(item("cyan_dye"), 64);
        box.getItem().overrideOtherStackedOnMe(box, carried[0], slot, primary, player, access);
        helper.assertTrue(PaintBoxItem.contents(box).count(DyeColor.CYAN.getId()) == 64 && carried[0].isEmpty(),
                "a whole stack of cyan goes in");
        carried[0] = new ItemStack(item("cyan_dye"), 5);
        box.getItem().overrideOtherStackedOnMe(box, carried[0], slot, primary, player, access);
        helper.assertTrue(PaintBoxItem.contents(box).count(DyeColor.CYAN.getId()) == 64 && carried[0].getCount() == 5,
                "the basic box holds one stack of cyan");
        carried[0] = new ItemStack(item("red_dye"), 20);
        box.getItem().overrideOtherStackedOnMe(box, carried[0], slot, primary, player, access);
        helper.assertValueEqual(PaintBoxItem.contents(box).count(DyeColor.RED.getId()), 20, "another colour has its own stack");

        // Right-click with an empty cursor takes a stack of the colour in front (none chosen: the first held, cyan).
        carried[0] = ItemStack.EMPTY;
        box.getItem().overrideOtherStackedOnMe(box, carried[0], slot, net.minecraft.world.inventory.ClickAction.SECONDARY, player, access);
        helper.assertTrue(carried[0].is(item("cyan_dye")) && carried[0].getCount() == 64, "takes the cyan stack: " + carried[0]);

        int[] expected = {64, 128, 256, 512};
        net.minecraft.world.item.Item[] tiers = {ModItems.PAINT_BOX, ModItems.REINFORCED_PAINT_BOX,
                ModItems.NETHERITE_PAINT_BOX, ModItems.ENDERITE_PAINT_BOX};
        for (int t = 0; t < tiers.length; t++) {
            ItemStack tier = new ItemStack(tiers[t]);
            for (int i = 0; i < 10; i++) PaintBoxItem.insert(tier, new ItemStack(item("lime_dye"), 64));
            helper.assertValueEqual(PaintBoxItem.contents(tier).count(DyeColor.LIME.getId()), expected[t],
                    "lime dyes in " + tiers[t]);
        }
        helper.succeed();
    }

    /** The scroll wheel brings the next held colour to the front; right-click then takes that colour (pure functions). */
    public static void paintBoxScrollSelection(GameTestHelper helper) {
        ItemStack box = box(ModItems.PAINT_BOX, new ItemStack(item("white_dye"), 2), new ItemStack(item("lime_dye"), 3),
                new ItemStack(item("black_dye"), 4));
        PaintBoxContents contents = PaintBoxItem.contents(box);
        helper.assertValueEqual(PaintBoxItem.frontColor(contents), DyeColor.WHITE.getId(), "nothing chosen: the first held");
        int white = DyeColor.WHITE.getId(), lime = DyeColor.LIME.getId(), black = DyeColor.BLACK.getId();
        helper.assertValueEqual(PaintBoxItem.nextSelection(contents, white, 1), lime, "forwards skips colours not held");
        helper.assertValueEqual(PaintBoxItem.nextSelection(contents, lime, 1), black, "then black");
        helper.assertValueEqual(PaintBoxItem.nextSelection(contents, black, 1), white, "and wraps around");
        helper.assertValueEqual(PaintBoxItem.nextSelection(contents, white, -1), black, "backwards wraps too");
        helper.assertValueEqual(PaintBoxItem.nextSelection(PaintBoxContents.EMPTY, PaintBoxContents.NONE, 1),
                PaintBoxContents.NONE, "an empty box has no front colour");
        PaintBoxItem.select(box, lime);
        ItemStack taken = PaintBoxItem.takeFront(box);
        helper.assertTrue(taken.is(item("lime_dye")) && taken.getCount() == 3, "takes the chosen lime: " + taken);
        helper.assertValueEqual(PaintBoxItem.frontColor(PaintBoxItem.contents(box)), white,
                "a chosen colour that runs out falls back to the first held");
        helper.succeed();
    }

    /** Recipes: brush + gold nugget + feather; the reinforced box keeps the contents, netherite and enderite at the smithing table. */
    public static void recipesMakeTheBrushAndUpgradeTheBox(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack brush = craft(helper, net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(Items.FEATHER), new ItemStack(Items.BRUSH), new ItemStack(Items.GOLD_NUGGET))), "simplebuilding:color_brush");
        helper.assertTrue(brush.is(ModItems.COLOR_BRUSH), "brush + gold nugget + feather make " + brush);

        ItemStack basic = box(ModItems.PAINT_BOX, new ItemStack(item("purple_dye"), 10));
        ItemStack d = new ItemStack(ModItems.DIAMOND_PEBBLE), n = new ItemStack(Items.GOLD_NUGGET);
        ItemStack reinforced = craft(helper, net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                java.util.List.of(n, d, n, d, basic, d, n, d, n)), "simplebuilding:reinforced_paint_box");
        helper.assertTrue(reinforced.is(ModItems.REINFORCED_PAINT_BOX)
                && PaintBoxItem.contents(reinforced).count(DyeColor.PURPLE.getId()) == 10, "reinforced box keeps its dyes: " + reinforced);
        ItemStack netherite = smith(helper, new net.minecraft.world.item.crafting.SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), reinforced, new ItemStack(Items.NETHERITE_INGOT)));
        helper.assertTrue(netherite.is(ModItems.NETHERITE_PAINT_BOX)
                && PaintBoxItem.contents(netherite).count(DyeColor.PURPLE.getId()) == 10, "netherite box keeps its dyes");
        ItemStack enderite = smith(helper, new net.minecraft.world.item.crafting.SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE), netherite, new ItemStack(ModItems.ENDERITE_INGOT)));
        helper.assertTrue(enderite.is(ModItems.ENDERITE_PAINT_BOX)
                && PaintBoxItem.contents(enderite).count(DyeColor.PURPLE.getId()) == 10, "enderite box keeps its dyes");
        helper.succeed();
    }

    /** Ticks the brush must keep brushing suspicious sand (10 brushes, 10 ticks apart) plus margin. */
    public static final int BRUSHING_MAX_TICKS = 300;

    /**
     * Without painting, the brush is a vanilla brush, the real way: right-click starts brushing, every use tick runs
     * {@code BrushItem#onUseTick} until the suspicious sand gives its loot and turns into sand. A dye in the inventory
     * changes nothing on a brushable block (conflict rule: brushable blocks are always brushed).
     */
    public static void brushesSuspiciousSandLikeTheVanillaBrush(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.setXRot(90f); // look straight down at the sand; the brush ray reads the old rotation (partial tick 0)
        player.xRotO = 90f;
        player.getInventory().setItem(5, new ItemStack(item("red_dye"), 4));
        helper.setBlock(rel.below(), Blocks.STONE); // suspicious sand falls like sand
        helper.setBlock(rel, Blocks.SUSPICIOUS_SAND);
        var sand = (net.minecraft.world.level.block.entity.BrushableBlockEntity) helper.getLevel().getBlockEntity(pos);
        sand.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, 1L);
        var result = useOn(player, brush, pos);
        helper.assertTrue(result.consumesAction() && player.isUsingItem(), "right-click starts brushing: " + result);
        int[] remaining = {brush.getUseDuration(player)};
        helper.onEachTick(() -> {
            if (remaining[0] > 0) brush.getItem().onUseTick(helper.getLevel(), player, brush, remaining[0]--);
        });
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.SAND, rel);
            helper.assertTrue(player.getInventory().getItem(5).getCount() == 4, "brushing used no dye");
            helper.assertTrue(brush.getDamageValue() == 1, "finished brushing costs one durability: " + brush.getDamageValue());
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(), "the sand gave its find");
        });
    }

    public static void paintsConcreteAndPreservesGlassPaneState(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.getInventory().setItem(1, new ItemStack(item("white_dye"), 3));
        helper.setBlock(rel, block("red_concrete"));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_concrete"), rel);

        helper.setBlock(rel, block("red_stained_glass_pane").defaultBlockState().setValue(BlockStateProperties.NORTH, true));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_stained_glass_pane"), rel);
        helper.assertTrue(helper.getBlockState(rel).getValue(BlockStateProperties.NORTH), "glass pane state survives");

        helper.setBlock(rel, Blocks.GLASS);
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_stained_glass"), rel);
        helper.assertTrue(player.getInventory().getItem(1).isEmpty() && brush.getDamageValue() == 3, "three strokes, three dyes");
        helper.succeed();
    }

    /** Contents and both bed halves stay; wood is no brush family (no wood stain); same colour costs nothing. */
    public static void keepsContentsAndBedHalvesButLeavesWood(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("blue_dye"), 10));

        helper.setBlock(rel, block("red_shulker_box"));
        var box = (net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity) helper.getLevel().getBlockEntity(pos);
        box.setItem(0, new ItemStack(Items.DIAMOND, 5));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("blue_shulker_box"), rel);
        var painted = (net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(painted != null && painted.getItem(0).is(Items.DIAMOND) && painted.getItem(0).getCount() == 5,
                "the shulker box keeps its contents");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3)).isEmpty(), "painting dropped the contents");

        BlockPos bedFoot = new BlockPos(1, 1, 4);
        BlockPos bedHead = bedFoot.east();
        helper.setBlock(bedFoot, block("red_bed").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        helper.setBlock(bedHead, block("red_bed").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        useOn(player, brush, helper.absolutePos(bedFoot));
        helper.assertBlockPresent(block("blue_bed"), bedFoot);
        helper.assertBlockPresent(block("blue_bed"), bedHead);
        int dyes = player.getOffhandItem().getCount();

        helper.setBlock(rel, Blocks.OAK_PLANKS);
        useOn(player, brush, pos);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, rel);
        helper.setBlock(rel, block("blue_wool"));
        useOn(player, brush, pos);
        helper.assertTrue(player.getOffhandItem().getCount() == dyes && brush.getDamageValue() == 2,
                "wood and the same colour cost nothing: " + player.getOffhandItem().getCount() + " / " + brush.getDamageValue());
        helper.succeed();
    }

    private static ItemStack brushInHand(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
        player.getInventory().setSelectedSlot(0);
        ItemStack brush = new ItemStack(ModItems.COLOR_BRUSH);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        return brush;
    }

    private static ItemStack box(net.minecraft.world.item.Item tier, ItemStack... dyes) {
        ItemStack box = new ItemStack(tier);
        for (ItemStack dye : dyes) PaintBoxItem.insert(box, dye);
        return box;
    }

    private static net.minecraft.world.InteractionResult stroke(net.minecraft.world.entity.player.Player player, BlockPos pos,
                                                                  net.minecraft.util.RandomSource random) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return ColorBrushItem.stroke(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit), random);
    }

    private static ItemStack craft(GameTestHelper helper, net.minecraft.world.item.crafting.CraftingInput grid, String recipeId) {
        var level = helper.getLevel();
        var match = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(), recipeId + ": the grid crafts nothing");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "matched recipe");
        return match.get().value().assemble(grid);
    }

    private static ItemStack smith(GameTestHelper helper, net.minecraft.world.item.crafting.SmithingRecipeInput input) {
        var level = helper.getLevel();
        var match = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level);
        helper.assertTrue(match.isPresent(), "no smithing recipe for " + input.base());
        return match.get().value().assemble(input);
    }

    private static net.minecraft.world.item.Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(path)).orElse(Items.AIR);
    }

    private static net.minecraft.world.level.block.Block block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(path)).orElse(Blocks.AIR);
    }

    private static net.minecraft.server.level.ServerPlayer playerInLevel(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        return player;
    }

    private static net.minecraft.world.InteractionResult useOn(net.minecraft.world.entity.player.Player player,
                              ItemStack stack, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
