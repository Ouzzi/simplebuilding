package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ColorBrushItem;
import com.simplebuilding.items.custom.PaintPaletteItem;
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
        var result = useOn(player, brush, pos);
        helper.assertBlockPresent(block("black_concrete"), rel);
        helper.assertTrue(!result.consumesAction() && brush.getDamageValue() == 2, "an empty brush does nothing");
        helper.succeed();
    }

    /** The other hand comes first, as for a bow; a palette there beats a loose dye in the inventory. */
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
        ItemStack palette = palette(new ItemStack(item("green_dye"), 3));
        player.setItemInHand(InteractionHand.OFF_HAND, palette);
        helper.assertValueEqual(ColorBrushItem.inkKey(player), ColorBrushItem.INK_PALETTE, "the tip shows the palette");
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("green_wool"), rel);
        helper.assertValueEqual(paletteCount(palette), 2, "the palette gives one green dye");
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

    /** A palette paints a random colour of its own (seeded: reproducible), never the block's colour, and gives that dye. */
    public static void paletteIsRandom(GameTestHelper helper) {
        ItemStack palette = palette(new ItemStack(item("red_dye"), 3), new ItemStack(item("blue_dye"), 3),
                new ItemStack(item("yellow_dye"), 3));
        java.util.Set<DyeColor> seen = new java.util.HashSet<>();
        net.minecraft.util.RandomSource a = net.minecraft.util.RandomSource.create(42), b = net.minecraft.util.RandomSource.create(42);
        for (int i = 0; i < 40; i++) {
            int pick = PaintPaletteItem.pickIndex(palette, DyeColor.RED, a);
            helper.assertValueEqual(pick, PaintPaletteItem.pickIndex(palette, DyeColor.RED, b), "same seed, same colour");
            seen.add(PaintPaletteItem.colorAt(palette, pick));
        }
        helper.assertTrue(seen.equals(java.util.Set.of(DyeColor.BLUE, DyeColor.YELLOW)),
                "random over the palette's colours except the block's: " + seen);

        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ItemStack brush = brushInHand(player, pos);
        player.getInventory().setItem(2, palette);
        helper.setBlock(rel, block("white_wool"));
        DyeColor expected = PaintPaletteItem.colorAt(palette,
                PaintPaletteItem.pickIndex(palette, DyeColor.WHITE, net.minecraft.util.RandomSource.create(7)));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ColorBrushItem.stroke(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit),
                net.minecraft.util.RandomSource.create(7));
        helper.assertBlockPresent(block(expected.getName() + "_wool"), rel);
        helper.assertValueEqual(paletteCount(palette), 8, "one dye leaves the palette");

        ItemStack only = palette(new ItemStack(item("white_dye")));
        player.getInventory().setItem(2, only);
        helper.setBlock(rel, block("white_wool"));
        useOn(player, brush, pos);
        helper.assertTrue(paletteCount(only) == 1 && brush.getDamageValue() == 1, "a palette with only the block's colour does nothing");
        helper.succeed();
    }

    /** The palette takes dyes only. */
    public static void paletteTakesOnlyDyes(GameTestHelper helper) {
        var player = playerInLevel(helper);
        ItemStack palette = new ItemStack(ModItems.PAINT_PALETTE);
        var container = new net.minecraft.world.SimpleContainer(1);
        container.setItem(0, palette);
        var slot = new net.minecraft.world.inventory.Slot(container, 0, 0, 0);
        ItemStack[] carried = {new ItemStack(Items.COBBLESTONE, 5)};
        var access = net.minecraft.world.entity.SlotAccess.of(() -> carried[0], stack -> carried[0] = stack);
        boolean took = palette.getItem().overrideOtherStackedOnMe(palette, carried[0], slot,
                net.minecraft.world.inventory.ClickAction.PRIMARY, player, access);
        helper.assertTrue(!took && paletteCount(palette) == 0 && carried[0].getCount() == 5, "cobblestone stays out");
        carried[0] = new ItemStack(item("cyan_dye"), 5);
        took = palette.getItem().overrideOtherStackedOnMe(palette, carried[0], slot,
                net.minecraft.world.inventory.ClickAction.PRIMARY, player, access);
        helper.assertTrue(took && paletteCount(palette) == 5, "dyes go in: " + paletteCount(palette));
        helper.assertTrue(PaintPaletteItem.hasDyes(palette), "a filled palette is ink");
        helper.succeed();
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

    private static ItemStack palette(ItemStack... dyes) {
        ItemStack palette = new ItemStack(ModItems.PAINT_PALETTE);
        java.util.List<net.minecraft.world.item.ItemStackTemplate> items = new java.util.ArrayList<>();
        for (ItemStack dye : dyes) items.add(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(dye));
        palette.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,
                new net.minecraft.world.item.component.BundleContents(items));
        return palette;
    }

    private static int paletteCount(ItemStack palette) {
        int count = 0;
        var contents = palette.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
        if (contents != null) for (var entry : contents.items()) count += entry.count();
        return count;
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
