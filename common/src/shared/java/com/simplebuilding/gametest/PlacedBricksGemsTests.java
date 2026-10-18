package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.FlatToggles;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.util.TransformTargets;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Queue N55 (owner 2026-10-14): bricks lie as 3D cuboids like ingots, diamonds and emeralds stand as 3D gems, and a hammer
 * strike on the placed pile switches those parts to the flat item plate and back (block state {@code flat}). Only supported
 * items switch. Loader-neutral; without the line's features the tests pass.
 */
public final class PlacedBricksGemsTests {
    private PlacedBricksGemsTests() {
    }

    private static boolean enabled(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES || ModBlocks.PLACED_SMALL_PARTS == null) {
            helper.succeed();
            return false;
        }
        return true;
    }

    private static ServerPlayer player(GameTestHelper helper, boolean sneak) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setShiftKeyDown(sneak);
        return player;
    }

    private static InteractionResult place(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, abs, false)));
    }

    private static PlacedSmallPartsBlockEntity pile(GameTestHelper helper, BlockPos rel) {
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof PlacedSmallPartsBlockEntity, "no pile at " + rel);
        return (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
    }

    /** A right click with {@code stack} on the pile, as the game does it (block first, then the item). */
    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(false);
        BlockPos abs = helper.absolutePos(rel);
        Vec3 eye = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY() + 1.0, rel.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atBottomCenterOf(abs).add(0.0, 0.05, 0.0), Direction.UP, abs, false));
    }

    private static com.google.gson.JsonObject resourceJson(GameTestHelper helper, String path) {
        try (java.io.InputStream in = PlacedBricksGemsTests.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, "missing resource " + path);
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private static float[] boxOf(com.google.gson.JsonObject element) {
        com.google.gson.JsonArray from = element.getAsJsonArray("from");
        com.google.gson.JsonArray to = element.getAsJsonArray("to");
        return new float[]{from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat()};
    }

    /**
     * Placing: bricks (brick, nether brick, resin brick) are {@code BRICK}, diamond and emerald {@code GEM}; bricks stack
     * with ingots; the gems stand; every other small part stays a flat plate. The models and item definitions in the
     * resources have the cuboids the Java shape has.
     */
    public static void bricksAndGemsLieIn3d(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            ServerPlayer player = player(helper, true);
            BlockPos floor = new BlockPos(1, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            ItemStack bricks = new ItemStack(Items.BRICK, 4);
            helper.assertTrue(place(helper, player, bricks, floor).consumesAction(), "the first brick was not laid down");
            for (int n = 2; n <= 4; n++) {
                helper.assertTrue(place(helper, player, bricks, floor.above()).consumesAction(), "brick " + n + " was refused");
            }
            PlacedSmallPartsBlockEntity four = pile(helper, floor.above());
            helper.assertValueEqual(four.parts().size(), 4, "bricks on the pile");
            helper.assertTrue(PlacedSmallParts.kind(new ItemStack(Items.BRICK)) == PlacedSmallParts.Kind.BRICK, "a brick is no 3D brick");
            helper.assertTrue(PlacedSmallParts.ingotStack(four.parts()), "four bricks do not stack");
            VoxelShape stack = four.shape();
            helper.assertTrue(Math.abs(stack.max(Direction.Axis.Y) - 6.0 / 16.0) < 1.0E-4, "the brick stack is " + stack.max(Direction.Axis.Y) * 16 + " px high");

            // bricks and ingots stack together
            BlockPos mixed = new BlockPos(3, 1, 1);
            helper.setBlock(mixed, Blocks.STONE);
            place(helper, player, new ItemStack(Items.NETHER_BRICK), mixed);
            place(helper, player, new ItemStack(Items.IRON_INGOT), mixed.above());
            place(helper, player, new ItemStack(Items.RESIN_BRICK), mixed.above());
            helper.assertTrue(PlacedSmallParts.ingotStack(pile(helper, mixed.above()).parts()), "bricks and ingots do not stack together");
            helper.assertTrue(Math.abs(pile(helper, mixed.above()).shape().max(Direction.Axis.Y) - 6.0 / 16.0) < 1.0E-4, "the mixed stack is not 6 px high");

            // gems stand; every other small part stays a plate
            for (Item gem : List.of(Items.DIAMOND, Items.EMERALD)) {
                BlockPos at = new BlockPos(5 + (gem == Items.EMERALD ? 2 : 0), 1, 1);
                helper.setBlock(at, Blocks.STONE);
                helper.assertTrue(place(helper, player, new ItemStack(gem), at).consumesAction(), gem + " was not laid down");
                PlacedSmallPartsBlockEntity one = pile(helper, at.above());
                helper.assertTrue(PlacedSmallParts.kind(one.parts().get(0)) == PlacedSmallParts.Kind.GEM, gem + " is no 3D gem");
                float rows = PlacedSmallParts.gemBoxes(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(gem).toString())[0][1];
                helper.assertTrue(Math.abs(one.shape().max(Direction.Axis.Y) * 16.0 - rows) < 1.0E-3, gem + " is " + one.shape().max(Direction.Axis.Y) * 16 + " px high");
                helper.assertFalse(PlacedSmallParts.ingotStack(one.parts()), "a gem stacks like an ingot");
            }
            for (Item plate : List.of(Items.FLINT, Items.AMETHYST_SHARD, Items.QUARTZ, Items.GOLD_NUGGET)) {
                helper.assertTrue(PlacedSmallParts.kind(new ItemStack(plate)) == PlacedSmallParts.Kind.PLATE, plate + " is not a plate");
                helper.assertFalse(PlacedSmallParts.isSwitchable(new ItemStack(plate)), plate + " is switchable");
            }
            helper.assertTrue(PlacedSmallParts.kind(new ItemStack(Items.IRON_INGOT)).switchable(), "an ingot is not switchable");

            // the models match the Java cuboids
            for (String id : PlacedSmallParts.MODELLED_BRICKS) {
                String path = id.substring(id.indexOf(':') + 1);
                resourceJson(helper, "/assets/simplebuilding/items/placed_" + path + ".json");
                com.google.gson.JsonArray elements = resourceJson(helper, "/assets/simplebuilding/models/block/placed_" + path + ".json").getAsJsonArray("elements");
                helper.assertValueEqual(elements.size(), PlacedSmallParts.BRICK_BOXES.length, path + " cuboids");
                float[] box = PlacedSmallParts.BRICK_BOXES[0];
                helper.assertTrue(java.util.Arrays.equals(new float[]{8.0F - box[2], box[0], 8.0F - box[3], 8.0F + box[2], box[1], 8.0F + box[3]},
                        boxOf(elements.get(0).getAsJsonObject())), path + " cuboid is " + java.util.Arrays.toString(boxOf(elements.get(0).getAsJsonObject())));
            }
            for (String id : PlacedSmallParts.MODELLED_GEMS) {
                String path = id.substring(id.indexOf(':') + 1);
                resourceJson(helper, "/assets/simplebuilding/items/placed_" + path + ".json");
                com.google.gson.JsonArray elements = resourceJson(helper, "/assets/simplebuilding/models/block/placed_" + path + ".json").getAsJsonArray("elements");
                float[][] boxes = PlacedSmallParts.gemBoxes(id);
                helper.assertValueEqual(elements.size(), boxes.length, path + " cuboids");
                for (int i = 0; i < boxes.length; i++) {
                    float[] b = boxes[i];
                    helper.assertTrue(java.util.Arrays.equals(new float[]{8.0F - b[2], b[0], 8.0F - b[2], 8.0F + b[2], b[1], 8.0F + b[2]},
                            boxOf(elements.get(i).getAsJsonObject())), path + " cuboid " + i + " is " + java.util.Arrays.toString(boxOf(elements.get(i).getAsJsonObject())));
                }
            }
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    /**
     * A hammer right click on the pile: 3D -> flat -> 3D, one durability each, supported parts only; the flat parts have the
     * plate's thin shape, a pile without a supported part (eggs, flint) and a pickaxe do nothing.
     */
    public static void hammerSwitchesPlacedPartsBetween3dAndFlat(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
        PlacedSmallPartsBlockEntity pile = pile(helper, rel);
        pile.setParts(List.of(new ItemStack(Items.BRICK), new ItemStack(Items.DIAMOND), new ItemStack(Items.FLINT)));
        BlockPos abs = helper.absolutePos(rel);
        ServerPlayer player = player(helper, false);
        player.getAbilities().instabuild = false;
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));

        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        click(helper, player, rel, pickaxe);
        helper.assertFalse(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), "a pickaxe switched the pile");

        ItemStack hammer = new ItemStack(com.simplebuilding.util.HammerRefs.item("iron"));
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        helper.assertTrue(FlatToggles.canToggle(level, abs, hammer), "the hammer cannot switch");
        helper.assertTrue(TransformTargets.canTransformTarget(level, new BlockHitResult(Vec3.atBottomCenterOf(abs).add(0.0, 0.05, 0.0), Direction.UP, abs, false),
                player, InteractionHand.MAIN_HAND), "no hint for the hammer");
        double tall = pile.shape().max(Direction.Axis.Y);
        helper.assertTrue(tall > 5.0 / 16.0, "the 3D pile is only " + tall * 16 + " px high");

        click(helper, player, rel, hammer);
        helper.assertTrue(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), "3D -> flat did not switch");
        helper.assertValueEqual(hammer.getDamageValue(), 1, "hammer damage after one switch");
        double flatHeight = pile.shape().max(Direction.Axis.Y);
        helper.assertTrue(flatHeight < tall && flatHeight < 3.0 / 16.0, "the flat pile is " + flatHeight * 16 + " px high (3D " + tall * 16 + ")");
        helper.assertValueEqual(pile.parts().size(), 3, "parts after switching");
        helper.assertTrue(PlacedSmallParts.kind(pile.parts().get(0), true) == PlacedSmallParts.Kind.PLATE, "a flat brick is no plate");

        click(helper, player, rel, hammer);
        helper.assertFalse(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), "flat -> 3D did not switch back");
        helper.assertValueEqual(hammer.getDamageValue(), 2, "hammer damage after two switches");
        helper.assertTrue(Math.abs(pile.shape().max(Direction.Axis.Y) - tall) < 1.0E-6, "the shape did not come back");

        // a pile without a supported part: nothing to switch
        pile.setParts(List.of(new ItemStack(Items.FLINT), new ItemStack(Items.BLUE_EGG)));
        helper.assertFalse(FlatToggles.canToggle(level, abs, hammer), "flint and egg can be switched");
        click(helper, player, rel, hammer);
        helper.assertFalse(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), "the pile without supported parts switched");
        helper.assertValueEqual(hammer.getDamageValue(), 2, "hammer damage without a target");

        // the flat state does not outlive the last supported part
        pile.setParts(List.of(new ItemStack(Items.EMERALD), new ItemStack(Items.FLINT)));
        click(helper, player, rel, hammer);
        helper.assertTrue(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), "emerald pile did not switch");
        pile.setParts(List.of(new ItemStack(Items.FLINT)));
        helper.assertFalse(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), "flat stays without a supported part");
        helper.succeed();
    }

    /** Breaking a flat or 3D pile drops every part as itself, nothing more, nothing less. */
    public static void switchedPartsDropAsThemselves(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        ServerLevel level = helper.getLevel();
        for (boolean flat : new boolean[]{false, true}) {
            BlockPos rel = new BlockPos(flat ? 4 : 1, 1, 2);
            helper.setBlock(rel.below(), Blocks.STONE);
            helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
            PlacedSmallPartsBlockEntity pile = pile(helper, rel);
            pile.setParts(List.of(new ItemStack(Items.BRICK), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.EMERALD)));
            BlockPos abs = helper.absolutePos(rel);
            if (flat) {
                level.setBlock(abs, level.getBlockState(abs).setValue(PlacedSmallPartsBlock.FLAT, true), 3);
            }
            helper.assertValueEqual(level.getBlockState(abs).getValue(PlacedSmallPartsBlock.FLAT), flat, "flat state");
            level.destroyBlock(abs, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.5));
            java.util.Set<Item> dropped = new java.util.HashSet<>();
            drops.forEach(e -> dropped.add(e.getItem().getItem()));
            helper.assertValueEqual(drops.size(), 3, (flat ? "flat" : "3D") + " drops: " + dropped);
            helper.assertTrue(dropped.equals(java.util.Set.of(Items.BRICK, Items.IRON_INGOT, Items.EMERALD)), (flat ? "flat" : "3D") + " drops: " + dropped);
            drops.forEach(e -> e.discard());
        }
        helper.succeed();
    }
}
