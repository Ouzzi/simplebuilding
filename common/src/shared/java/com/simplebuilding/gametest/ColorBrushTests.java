package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ColorBrushItem;
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

    public static void loadsAndConsumesOneDye(GameTestHelper helper) {
        var player = playerInLevel(helper);
        ItemStack brush = new ItemStack(ModItems.COLOR_BRUSH);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        player.getInventory().setItem(1, new ItemStack(item("red_dye"), 2));
        player.setShiftKeyDown(true);
        ModItems.COLOR_BRUSH.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 1, "one dye is consumed");
        helper.assertTrue(charges(brush) == ColorBrushItem.STROKES_PER_DYE, "one dye loads eight strokes");
        helper.succeed();
    }

    public static void paintsConcreteAndPreservesGlassPaneState(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
        ItemStack brush = new ItemStack(ModItems.COLOR_BRUSH);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        player.getInventory().setItem(1, new ItemStack(item("white_dye")));
        player.setShiftKeyDown(true);
        ModItems.COLOR_BRUSH.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(charges(brush) == ColorBrushItem.STROKES_PER_DYE, "painting brush is loaded");
        helper.assertValueEqual(ColorBrushItem.color(brush), DyeColor.WHITE, "painting brush has white color");
        player.setShiftKeyDown(false);
        helper.setBlock(rel, block("red_concrete"));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_concrete"), rel);
        helper.assertTrue(charges(brush) == ColorBrushItem.STROKES_PER_DYE - 1 && brush.getDamageValue() == 1,
                "one stroke costs one charge and one durability: " + charges(brush) + " / " + brush.getDamageValue());

        helper.setBlock(rel, block("red_stained_glass_pane").defaultBlockState().setValue(BlockStateProperties.NORTH, true));
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_stained_glass_pane"), rel);
        helper.assertTrue(helper.getBlockState(rel).getValue(BlockStateProperties.NORTH), "glass pane state survives");

        helper.setBlock(rel, Blocks.GLASS);
        useOn(player, brush, pos);
        helper.assertBlockPresent(block("white_stained_glass"), rel);
        helper.succeed();
    }

    /** Pipette: an empty brush takes the block's colour, paid with one matching dye in survival; a sponge washes it. */
    public static void pipetteAndWash(GameTestHelper helper) {
        var player = playerInLevel(helper);
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
        helper.setBlock(rel, block("red_concrete"));
        ItemStack brush = new ItemStack(ModItems.COLOR_BRUSH);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        player.setShiftKeyDown(true);
        useOn(player, brush, pos);
        helper.assertTrue(ColorBrushItem.color(brush) == null, "pipette without a red dye loads nothing");
        player.getInventory().setItem(1, new ItemStack(item("red_dye"), 2));
        useOn(player, brush, pos);
        helper.assertTrue(ColorBrushItem.color(brush) == DyeColor.RED && charges(brush) == ColorBrushItem.STROKES_PER_DYE,
                "pipette loads red: " + ColorBrushItem.color(brush) + " " + charges(brush));
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 1, "pipette uses one red dye");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPONGE));
        useOn(player, brush, pos);
        helper.assertTrue(charges(brush) == 0 && ColorBrushItem.color(brush) == null, "sponge washes the brush");
        helper.succeed();
    }

    /** Contents and both bed halves stay; wood is no brush family (no wood stain); creative strokes are free. */
    public static void keepsContentsAndBedHalvesButLeavesWood(GameTestHelper helper) {
        var player = playerInLevel(helper);
        player.getAbilities().instabuild = true;
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
        ItemStack brush = new ItemStack(ModItems.COLOR_BRUSH);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item("blue_dye")));
        player.setShiftKeyDown(true);
        useOn(player, brush, pos);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        helper.assertTrue(ColorBrushItem.color(brush) == DyeColor.BLUE, "brush loads blue from the off hand");
        int loaded = charges(brush);

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

        helper.setBlock(rel, Blocks.OAK_PLANKS);
        useOn(player, brush, pos);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, rel);
        helper.assertTrue(charges(brush) == loaded && brush.getDamageValue() == 0, "creative strokes are free");
        helper.succeed();
    }

    private static int charges(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("Charges", 0);
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
