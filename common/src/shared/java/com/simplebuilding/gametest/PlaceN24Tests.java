package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.GoatHornHolderBlock;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.custom.StandingRodBlock;
import com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.HammockItem;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.util.SpearDispensing;
import com.simplebuilding.version.McVersion;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placing, queue N24/N16 (docs/ai/PLAN-PLATZIEREN-N24-2026-10-09.md): smithing templates pile up to four, ingots lie as
 * 3D bars and stack, the goat horn holds a torch or a rod, a spear in a dispenser thrusts, stacked standing rods join
 * without a gap, a tied hammock comes loose like a lead. Loader-neutral; without the line's features the tests pass.
 */
public final class PlaceN24Tests {
    private PlaceN24Tests() {
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setShiftKeyDown(true);
        return player;
    }

    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    private static List<Item> parts(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof PlacedSmallPartsBlockEntity be
                ? be.parts().stream().map(ItemStack::getItem).toList() : List.of();
    }

    private static com.google.gson.JsonObject resourceJson(GameTestHelper helper, String path) {
        try (java.io.InputStream in = PlaceN24Tests.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, "missing resource " + path);
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    // =====================================================================================
    // Trims up to four ("Plex")
    // =====================================================================================

    /**
     * The first template lies alone (hammer upgrade stays), each further one makes and fills a pile up to four; a fifth
     * stays in the hand. A pebble does not go onto a lone template, but a template goes onto a pebble pile.
     */
    public static void smithingTemplatesPileUpToFour(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        ItemStack trims = new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 5);
        helper.assertTrue(use(helper, player, trims, floor, Direction.UP).consumesAction(), "the first trim was not laid down");
        helper.assertTrue(helper.getBlockState(floor.above()).is(ModBlocks.PLACED_SMITHING_TEMPLATE), "the first trim is no lone template: "
                + helper.getBlockState(floor.above()));
        for (int n = 2; n <= 4; n++) {
            helper.assertTrue(use(helper, player, trims, floor.above(), Direction.UP).consumesAction(), "trim " + n + " was refused");
            helper.assertTrue(helper.getBlockState(floor.above()).getBlock() instanceof PlacedSmallPartsBlock, "no pile after trim " + n);
            helper.assertValueEqual(parts(helper, floor.above()).size(), n, "trims on the pile");
        }
        helper.assertFalse(use(helper, player, trims, floor.above(), Direction.UP).consumesAction(), "a fifth trim was added");
        helper.assertValueEqual(trims.getCount(), 1, "trims left in the hand");
        // a pebble pile takes a template; a lone template does not take a pebble
        BlockPos other = new BlockPos(4, 1, 1);
        helper.setBlock(other, Blocks.STONE);
        helper.assertTrue(use(helper, player, new ItemStack(ModItems.STONE_PEBBLE), other, Direction.UP).consumesAction(), "no pebble pile");
        helper.assertTrue(use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), other, Direction.UP).consumesAction(),
                "the pebble pile refused a template");
        helper.assertValueEqual(parts(helper, other.above()), List.of(ModItems.STONE_PEBBLE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), "mixed pile");
        BlockPos lone = new BlockPos(1, 1, 4);
        helper.setBlock(lone, Blocks.STONE);
        use(helper, player, new ItemStack(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE), lone, Direction.UP);
        helper.assertFalse(use(helper, player, new ItemStack(ModItems.STONE_PEBBLE), lone, Direction.UP).consumesAction(),
                "a pebble went onto a lone template");
        // functional items stay single
        helper.assertFalse(PlacedSmallParts.isPileTemplate(new ItemStack(ModItems.MAGNET)), "the attractor piles");
        helper.succeed();
    }

    // =====================================================================================
    // Ingots as 3D bars
    // =====================================================================================

    /**
     * Ingots lie as 3D bars; only ingots on a spot stack (two below, the third and fourth across on top, 6 px high);
     * mixed with another part each bar stands on the floor. The bar model matches the hitbox boxes.
     */
    public static void ingotsLieAsBarsAndStack(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            ServerPlayer player = player(helper);
            BlockPos floor = new BlockPos(1, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            ItemStack ingots = new ItemStack(Items.IRON_INGOT, 4);
            helper.assertTrue(use(helper, player, ingots, floor, Direction.UP).consumesAction(), "the first ingot was not laid down");
            for (int n = 2; n <= 4; n++) {
                helper.assertTrue(use(helper, player, ingots, floor.above(), Direction.UP).consumesAction(), "ingot " + n + " was refused");
            }
            helper.assertValueEqual(parts(helper, floor.above()).size(), 4, "ingots on the pile");
            PlacedSmallPartsBlockEntity be = (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(floor.above()));
            helper.assertTrue(PlacedSmallParts.kind(new ItemStack(Items.IRON_INGOT)) == PlacedSmallParts.Kind.INGOT, "iron is no 3D ingot");
            helper.assertTrue(PlacedSmallParts.ingotStack(be.parts()), "four ingots do not stack");
            VoxelShape stack = be.shape();
            helper.assertTrue(Math.abs(stack.max(Direction.Axis.Y) - 6.0 / 16.0) < 1.0E-4, "the stack is " + stack.max(Direction.Axis.Y) * 16 + " px high");
            // mixed: the ingot stands on the floor, 3 px high
            BlockPos mixed = new BlockPos(4, 1, 1);
            helper.setBlock(mixed, Blocks.STONE);
            use(helper, player, new ItemStack(Items.GOLD_INGOT), mixed, Direction.UP);
            use(helper, player, new ItemStack(Items.FLINT), mixed.above(), Direction.UP);
            PlacedSmallPartsBlockEntity mix = (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(mixed.above()));
            helper.assertFalse(PlacedSmallParts.ingotStack(mix.parts()), "a mixed pile stacks");
            helper.assertTrue(Math.abs(mix.shape().max(Direction.Axis.Y) - 3.0 / 16.0) < 1.0E-4, "the mixed pile is "
                    + mix.shape().max(Direction.Axis.Y) * 16 + " px high");
            for (Item ingot : java.util.Arrays.asList(Items.COPPER_INGOT, Items.GOLD_INGOT, Items.NETHERITE_INGOT, ModItems.ENDERITE_INGOT)) {
                helper.assertTrue(PlacedSmallParts.isModelledIngot(new ItemStack(ingot)), ingot + " has no bar model");
            }
            helper.assertFalse(PlacedSmallParts.isModelledIngot(new ItemStack(Items.BRICK)), "a brick is a 3D ingot");
            for (String name : PlacedSmallParts.MODELLED_INGOTS) {
                String path = name.substring(name.indexOf(':') + 1);
                com.google.gson.JsonArray elements = resourceJson(helper, "/assets/simplebuilding/models/block/placed_" + path + ".json")
                        .getAsJsonArray("elements");
                helper.assertValueEqual(elements.size(), PlacedSmallParts.INGOT_BOXES.length, path + " cuboids");
                for (int i = 0; i < elements.size(); i++) {
                    float[] box = PlacedSmallParts.INGOT_BOXES[i];
                    com.google.gson.JsonArray from = elements.get(i).getAsJsonObject().getAsJsonArray("from");
                    com.google.gson.JsonArray to = elements.get(i).getAsJsonObject().getAsJsonArray("to");
                    float[] expected = {8.0F - box[2], box[0], 8.0F - box[3], 8.0F + box[2], box[1], 8.0F + box[3]};
                    float[] actual = {from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                            to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat()};
                    helper.assertTrue(java.util.Arrays.equals(expected, actual), path + " cuboid " + i + " is "
                            + java.util.Arrays.toString(actual) + ", the hitbox " + java.util.Arrays.toString(expected));
                }
                helper.assertValueEqual(resourceJson(helper, "/assets/simplebuilding/items/placed_" + path + ".json").getAsJsonObject("model")
                        .get("model").getAsString(), "simplebuilding:block/placed_" + path, path + " item definition");
            }
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    // =====================================================================================
    // Goat horn holder
    // =====================================================================================

    /**
     * Sneaking + right-click puts a goat horn down on a floor or a wall (not without sneaking, not under a ceiling); a
     * torch goes in and lights it, a second one does not fit, an empty hand takes it out; rods fit too (the blaze rod
     * glows a little); breaking drops the horn with its instrument and the held item.
     */
    public static void goatHornHoldsATorchOrARod(GameTestHelper helper) {
        if (ModBlocks.GOAT_HORN_HOLDER == null) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        ItemStack horn = new ItemStack(Items.GOAT_HORN, 3);
        horn.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Herald"));
        player.setShiftKeyDown(false);
        use(helper, player, horn, floor, Direction.UP);
        helper.assertFalse(helper.getBlockState(floor.above()).is(ModBlocks.GOAT_HORN_HOLDER), "the horn went down without sneaking");
        player.setShiftKeyDown(true);
        helper.assertTrue(use(helper, player, horn, floor, Direction.UP).consumesAction(), "sneak + right-click did not put the horn down");
        BlockPos pos = floor.above();
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(ModBlocks.GOAT_HORN_HOLDER) && state.getValue(GoatHornHolderBlock.FACE) == AttachFace.FLOOR,
                "no standing horn: " + state);
        helper.assertValueEqual(horn.getCount(), 2, "horns left");
        GoatHornHolderBlockEntity be = (GoatHornHolderBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertValueEqual(be.horn().getHoverName().getString(), "Herald", "the horn's name");
        // a torch goes in and lights the holder
        player.setShiftKeyDown(false);
        ItemStack torches = new ItemStack(Items.TORCH, 2);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), false);
        player.setItemInHand(InteractionHand.MAIN_HAND, torches);
        helper.assertTrue(state.useItemOn(torches, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(), "the torch did not go in");
        helper.assertTrue(be.held().is(Items.TORCH), "holds " + be.held());
        helper.assertValueEqual(torches.getCount(), 1, "torches left");
        helper.assertValueEqual(helper.getBlockState(pos).getValue(GoatHornHolderBlock.LIGHT), 14, "light with a torch");
        helper.assertFalse(helper.getBlockState(pos).useItemOn(torches, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "a second torch went in");
        // an empty hand takes it out
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit).consumesAction(), "the torch did not come out");
        helper.assertTrue(be.held().isEmpty(), "still holds " + be.held());
        helper.assertValueEqual(helper.getBlockState(pos).getValue(GoatHornHolderBlock.LIGHT), 0, "light without a torch");
        helper.assertTrue(player.getInventory().countItem(Items.TORCH) >= 1, "the torch is not in the inventory");
        // rods fit, the blaze rod glows a little; dirt does not fit
        helper.assertTrue(GoatHornHolderBlock.fits(new ItemStack(Items.STICK)) && GoatHornHolderBlock.fits(new ItemStack(Items.SOUL_TORCH))
                && GoatHornHolderBlock.fits(new ItemStack(Items.REDSTONE_TORCH)), "stick/soul torch/redstone torch do not fit");
        helper.assertFalse(GoatHornHolderBlock.fits(new ItemStack(Items.DIRT)), "dirt fits");
        ItemStack blaze = new ItemStack(Items.BLAZE_ROD);
        player.setItemInHand(InteractionHand.MAIN_HAND, blaze);
        helper.getBlockState(pos).useItemOn(blaze, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertValueEqual(helper.getBlockState(pos).getValue(GoatHornHolderBlock.LIGHT), StandingRodBlock.Rod.BLAZE_ROD.light(), "blaze rod light");
        // on a wall; never under a ceiling
        BlockPos wall = new BlockPos(4, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        player.setShiftKeyDown(true);
        helper.assertTrue(use(helper, player, horn, wall, Direction.NORTH).consumesAction(), "the horn did not hang on the wall");
        BlockState onWall = helper.getBlockState(wall.north());
        helper.assertTrue(onWall.is(ModBlocks.GOAT_HORN_HOLDER) && onWall.getValue(GoatHornHolderBlock.FACE) == AttachFace.WALL
                && onWall.getValue(GoatHornHolderBlock.FACING) == Direction.NORTH, "no wall horn: " + onWall);
        BlockPos ceiling = new BlockPos(1, 4, 5);
        helper.setBlock(ceiling, Blocks.STONE);
        use(helper, player, horn, ceiling, Direction.DOWN);
        helper.assertFalse(helper.getBlockState(ceiling.below()).is(ModBlocks.GOAT_HORN_HOLDER), "a horn hangs under the ceiling");
        // breaking drops the horn (with its name) and the blaze rod
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true, player);
        AABB box = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(8.0);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box);
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.GOAT_HORN) && "Herald".equals(e.getItem().getHoverName().getString())),
                "the named horn did not drop");
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.BLAZE_ROD)), "the blaze rod did not drop");
        helper.succeed();
    }

    // =====================================================================================
    // Spear in a dispenser
    // =====================================================================================

    /** A powered dispenser with a spear thrusts: the pig in front is hurt, the spear stays in and loses one point. */
    public static void spearInADispenserThrustsLikeASpikeTrap(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(1, 2, 3);
        helper.setBlock(dispenser.below(), Blocks.STONE);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity be = (DispenserBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(dispenser));
        be.setItem(0, new ItemStack(Items.IRON_SPEAR));
        BlockPos front = dispenser.east(2);
        helper.setBlock(front.below(), Blocks.STONE);
        Pig pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, front);
        float full = pig.getHealth();
        helper.assertTrue(SpearDispensing.thrustDamage(new ItemStack(Items.IRON_SPEAR)) > 1.0F, "the spear has no attack damage");
        helper.setBlock(dispenser.west(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < full, "the pig was not hurt");
            ItemStack spear = be.getItem(0);
            helper.assertTrue(spear.is(Items.IRON_SPEAR), "the spear left the dispenser: " + spear);
            helper.assertValueEqual(spear.getDamageValue(), 1, "spear damage");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(dispenser)).inflate(4.0)).isEmpty(),
                    "something was thrown out");
        });
    }

    // =====================================================================================
    // Standing rods join (N16)
    // =====================================================================================

    /** A rod with another one on it reaches the top of its block (state up, full-height shape); taken off, it shrinks back. */
    public static void stackedStandingRodsJoinWithoutAGap(GameTestHelper helper) {
        if (!McVersion.STANDING_RODS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        use(helper, player, new ItemStack(Items.STICK), floor, Direction.UP);
        helper.assertFalse(helper.getBlockState(floor.above()).getValue(StandingRodBlock.UP), "a lone stick is joined");
        helper.assertTrue(use(helper, player, new ItemStack(Items.BONE), floor.above(), Direction.UP).consumesAction(), "the bone did not stack");
        BlockState lower = helper.getBlockState(floor.above());
        helper.assertTrue(lower.getValue(StandingRodBlock.UP), "the stick under the bone is not joined: " + lower);
        helper.assertTrue(lower.getShape(helper.getLevel(), helper.absolutePos(floor.above())).max(Direction.Axis.Y) == 1.0, "the joined stick is short");
        helper.assertFalse(helper.getBlockState(floor.above(2)).getValue(StandingRodBlock.UP), "the top bone is joined");
        helper.setBlock(floor.above(2), Blocks.AIR);
        helper.assertFalse(helper.getBlockState(floor.above()).getValue(StandingRodBlock.UP), "the stick stayed joined");
        helper.succeed();
    }

    // =====================================================================================
    // Hammock like a lead (N16)
    // =====================================================================================

    /** The tied hammock has no time limit but comes loose when its holder goes more than 10 blocks away. */
    public static void tiedHammockComesLooseWhenTooFar(GameTestHelper helper) {
        if (!McVersion.HAMMOCK || ModItems.WHITE_HAMMOCK == null) {
            helper.succeed();
            return;
        }
        BlockPos anchor = new BlockPos(1, 2, 1);
        helper.setBlock(anchor, Blocks.STONE);
        ServerPlayer player = player(helper);
        ItemStack hammock = new ItemStack(ModItems.WHITE_HAMMOCK);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammock);
        BlockPos abs = helper.absolutePos(anchor);
        HammockItem.rememberAnchor(hammock, helper.getLevel(), abs);
        // an old tie (more than the former 30 s) still holds
        CustomData.update(DataComponents.CUSTOM_DATA, hammock, tag -> tag.getCompound(HammockItem.ANCHOR_KEY)
                .ifPresent(a -> a.putLong("time", helper.getLevel().getGameTime() - 10_000L)));
        helper.assertValueEqual(HammockItem.storedAnchor(hammock, helper.getLevel()), Optional.of(abs), "an old tie came loose");
        Item item = hammock.getItem();
        player.setPos(Vec3.atCenterOf(abs).add(6.0, 0.0, 0.0));
        item.inventoryTick(hammock, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        helper.assertValueEqual(HammockItem.storedAnchor(hammock, helper.getLevel()), Optional.of(abs), "came loose 6 blocks away");
        player.setPos(Vec3.atCenterOf(abs).add(11.0, 0.0, 0.0));
        item.inventoryTick(hammock, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        helper.assertTrue(HammockItem.storedAnchor(hammock, helper.getLevel()).isEmpty(), "still tied 11 blocks away");
        helper.assertTrue(HammockItem.tooFar(abs, Vec3.atCenterOf(abs).add(10.5, 0.0, 0.0)), "10.5 blocks is not too far");
        helper.assertFalse(HammockItem.tooFar(abs, Vec3.atCenterOf(abs).add(9.5, 0.0, 0.0)), "9.5 blocks is too far");
        helper.succeed();
    }
}
