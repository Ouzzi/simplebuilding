package com.simplestorage.test;

import com.simplestorage.backpack.BackpackBlockEntity;
import com.simplestorage.backpack.BackpackContents;
import com.simplestorage.backpack.BackpackTier;
import com.simplestorage.bundle.PlacedBundleBlock;
import com.simplestorage.bundle.PlacedBundleBlockEntity;
import com.simplestorage.bundle.PlacedBundles;
import com.simplestorage.bundle.PlacedStorage;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageComponents;
import com.simplestorage.registry.StorageItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * B23-M11/M12 (owner 2026-10-23, {@link PlacedStorage}): set-down bundles, quivers and backpacks stand on the floor as a
 * gravity block (they fall like sand and keep everything), hang on a wall like an item frame and never hang under a
 * ceiling; a placed quiver takes arrows only.
 */
public final class PlacedStorageTests {
    /** Ticks the fall tests may take (a few blocks of free fall plus the 2-tick delay). */
    public static final int FALL_MAX_TICKS = 120;

    private PlacedStorageTests() {
    }

    /** A floor bundle without support falls as a falling block and lands with its contents, facing and tier intact. */
    public static void bundleFallsLikeSand(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 5.0, 5.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        ItemStack bundle = filled(StorageItems.REINFORCED_BUNDLE, new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.TORCH, 9));
        bundle.set(DataComponents.CUSTOM_NAME, Component.literal("Falling"));
        use(helper, player, bundle.copy(), new BlockPos(2, 3, 2), Direction.UP);
        BlockState placed = helper.getBlockState(new BlockPos(2, 4, 2));
        helper.assertTrue(placed.is(StorageBlocks.PLACED_BUNDLE) && !placed.getValue(PlacedStorage.WALL), "not placed on the floor: " + placed);
        Direction facing = placed.getValue(PlacedBundleBlock.FACING);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);
        helper.succeedWhen(() -> {
            BlockState landed = helper.getBlockState(new BlockPos(2, 1, 2));
            helper.assertTrue(landed.is(StorageBlocks.PLACED_BUNDLE), "the bundle did not land on the floor below: " + landed);
            helper.assertTrue(landed.getValue(PlacedBundleBlock.FACING) == facing && landed.getValue(PlacedBundleBlock.TIER) == PlacedBundleBlock.Tier.REINFORCED,
                    "facing/tier changed in the fall: " + landed);
            PlacedBundleBlockEntity be = helper.getBlockEntity(new BlockPos(2, 1, 2), PlacedBundleBlockEntity.class);
            helper.assertTrue(ItemStack.isSameItemSameComponents(be.getBundle(), bundle), "the landed bundle holds " + be.getBundle());
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 4, 2)).isAir(), "the old spot is not empty");
        });
    }

    /**
     * A bundle hangs on the wall (clicked side): it faces away from the wall, stays without floor (no fall), and drops
     * with its contents when the wall goes. Under a ceiling nothing is placed.
     */
    public static void bundleHangsOnWallNotOnCeiling(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 1.5));
        player.setShiftKeyDown(true);
        BlockPos wall = new BlockPos(4, 3, 4);
        helper.setBlock(wall, Blocks.STONE);
        ItemStack bundle = filled(StorageItems.NETHERITE_BUNDLE, new ItemStack(Items.APPLE, 5));
        InteractionResult hung = use(helper, player, bundle.copy(), wall, Direction.NORTH);
        BlockPos at = wall.north();
        BlockState state = helper.getBlockState(at);
        helper.assertTrue(hung.consumesAction() && state.is(StorageBlocks.PLACED_BUNDLE), "not hung: " + hung + " " + state);
        helper.assertTrue(state.getValue(PlacedStorage.WALL) && state.getValue(PlacedBundleBlock.FACING) == Direction.NORTH,
                "hangs wrong: " + state);
        helper.assertTrue(state.getShape(helper.getLevel(), helper.absolutePos(at)).bounds().maxZ == 1.0,
                "the hanging bundle does not touch its wall: " + state.getShape(helper.getLevel(), helper.absolutePos(at)).bounds());
        // Ceiling: nothing.
        helper.setBlock(new BlockPos(1, 5, 4), Blocks.STONE);
        InteractionResult ceiling = use(helper, player, new ItemStack(Items.BUNDLE), new BlockPos(1, 5, 4), Direction.DOWN);
        helper.assertTrue(!ceiling.consumesAction() && helper.getBlockState(new BlockPos(1, 4, 4)).isAir(), "hung under a ceiling: " + ceiling);
        // A backpack hangs the same way; a quiver too.
        helper.setBlock(new BlockPos(6, 3, 4), Blocks.STONE);
        use(helper, player, new ItemStack(StorageItems.BACKPACKS.get(BackpackTier.BASIC)), new BlockPos(6, 3, 4), Direction.NORTH);
        BlockState pack = helper.getBlockState(new BlockPos(6, 3, 3));
        helper.assertTrue(pack.is(StorageBlocks.BACKPACKS.get(BackpackTier.BASIC)) && pack.getValue(PlacedStorage.WALL), "backpack not hung: " + pack);
        use(helper, player, new ItemStack(StorageItems.BACKPACKS.get(BackpackTier.BASIC)), new BlockPos(6, 5, 4), Direction.DOWN);
        helper.assertTrue(helper.getBlockState(new BlockPos(6, 4, 4)).isAir(), "backpack hung under a ceiling");
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.STONE);
        use(helper, player, new ItemStack(StorageItems.QUIVER), new BlockPos(2, 3, 4), Direction.NORTH);
        BlockState quiver = helper.getBlockState(new BlockPos(2, 3, 3));
        helper.assertTrue(quiver.is(StorageBlocks.PLACED_BUNDLE) && quiver.getValue(PlacedStorage.WALL)
                && quiver.getValue(PlacedBundleBlock.TIER) == PlacedBundleBlock.Tier.QUIVER, "quiver not hung: " + quiver);
        // No floor below the wall bundle: it must not fall. Then the wall goes: it drops with its contents.
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getBlockState(at).is(StorageBlocks.PLACED_BUNDLE), "the wall bundle fell");
            helper.setBlock(wall, Blocks.AIR);
            helper.assertTrue(helper.getBlockState(at).isAir(), "the bundle stayed without its wall");
            ItemEntity drop = dropped(helper, StorageItems.NETHERITE_BUNDLE);
            helper.assertTrue(drop != null && ItemStack.isSameItemSameComponents(drop.getItem(), bundle),
                    "without its wall the bundle dropped " + (drop == null ? "nothing" : drop.getItem()));
            helper.succeed();
        });
    }

    /**
     * A floor backpack falls like sand; when it cannot land (onto a torch) the falling block breaks and drops the full
     * backpack - contents and name - instead of an empty one.
     */
    public static void backpackFallsAndKeepsContentsWhenItBreaks(GameTestHelper helper) {
        Player player = mockPlayer(helper, new Vec3(5.5, 5.0, 5.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(5, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.TORCH);
        helper.setBlock(new BlockPos(5, 3, 2), Blocks.STONE);
        Item item = StorageItems.BACKPACKS.get(BackpackTier.BASIC);
        ItemStack backpack = new ItemStack(item);
        backpack.set(StorageComponents.BACKPACK_CONTENTS, new BackpackContents(List.of(
                BackpackContents.Entry.of(0, new ItemStack(Items.COBBLESTONE, 32)), BackpackContents.Entry.of(1, new ItemStack(Items.EMERALD, 2)))));
        backpack.set(DataComponents.CUSTOM_NAME, Component.literal("Camp"));
        use(helper, player, backpack.copy(), new BlockPos(5, 3, 2), Direction.UP);
        helper.assertTrue(helper.getBlockEntity(new BlockPos(5, 4, 2), BackpackBlockEntity.class) != null, "the backpack was not placed");
        helper.setBlock(new BlockPos(5, 3, 2), Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, helper.getBounds()).isEmpty(), "still falling");
            ItemEntity drop = dropped(helper, item);
            helper.assertTrue(drop != null, "the broken fall dropped no backpack");
            helper.assertTrue(drop.getItem().getCount() == 1 && "Camp".equals(drop.getItem().getHoverName().getString()),
                    "the dropped backpack lost its name: " + drop.getItem());
            BackpackContents kept = drop.getItem().get(StorageComponents.BACKPACK_CONTENTS);
            helper.assertTrue(kept != null && kept.size() == 2, "the dropped backpack lost its contents: " + kept);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(item)).size() == 1,
                    "more than one backpack dropped (an empty copy?)");
        });
    }

    /** A placed quiver takes arrows (sneak + right-click) but nothing else. */
    public static void placedQuiverTakesArrowsOnly(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        use(helper, player, new ItemStack(StorageItems.REINFORCED_QUIVER), new BlockPos(3, 1, 3), Direction.UP);
        BlockPos at = new BlockPos(3, 2, 3);
        helper.assertTrue(helper.getBlockState(at).getValue(PlacedBundleBlock.TIER) == PlacedBundleBlock.Tier.REINFORCED_QUIVER,
                "not a placed reinforced quiver: " + helper.getBlockState(at));
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 8);
        use(helper, player, cobble, at, Direction.UP);
        helper.assertTrue(player.getMainHandItem().getCount() == 8, "cobblestone went into the quiver");
        ItemStack arrows = new ItemStack(Items.ARROW, 16);
        use(helper, player, arrows, at, Direction.UP);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the arrows did not go in: " + player.getMainHandItem());
        PlacedBundleBlockEntity be = helper.getBlockEntity(at, PlacedBundleBlockEntity.class);
        helper.assertTrue(be.contents().size() == 1 && be.contents().getFirst().is(Items.ARROW), "quiver holds " + be.contents());
        helper.succeed();
    }

    /**
     * W24-2: a placed quiver shows its fill level as 0..5 arrows (full = 5, then fewer, one arrow already shows one), set on
     * placing and on every change of its contents; bundles always stay at 0.
     */
    public static void placedQuiverShowsFillLevel(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 6.5));
        player.setShiftKeyDown(true);
        int[][] cases = {{0, 0}, {1, 1}, {32, 3}, {51, 4}, {64, 5}};
        for (int i = 0; i < cases.length; i++) {
            BlockPos floor = new BlockPos(1 + i, 1, 2);
            helper.setBlock(floor, Blocks.STONE);
            ItemStack quiver = cases[i][0] == 0 ? new ItemStack(StorageItems.QUIVER) : filled(StorageItems.QUIVER, new ItemStack(Items.ARROW, cases[i][0]));
            helper.assertTrue(PlacedBundles.arrowsShown(quiver) == cases[i][1], cases[i][0] + " arrows show " + PlacedBundles.arrowsShown(quiver));
            use(helper, player, quiver, floor, Direction.UP);
            BlockState state = helper.getBlockState(floor.above());
            helper.assertTrue(state.is(StorageBlocks.PLACED_BUNDLE) && state.getValue(PlacedBundleBlock.ARROWS) == cases[i][1],
                    cases[i][0] + " arrows placed as " + state);
        }
        // contents change: 32 arrows (3) + 32 spectral arrows = full (5); taking the spectral ones out gives 3 again
        BlockPos at = new BlockPos(3, 2, 2);
        use(helper, player, new ItemStack(Items.SPECTRAL_ARROW, 32), at, Direction.UP);
        helper.assertTrue(helper.getBlockState(at).getValue(PlacedBundleBlock.ARROWS) == 5, "filled up: " + helper.getBlockState(at));
        PlacedBundleBlockEntity be = helper.getBlockEntity(at, PlacedBundleBlockEntity.class);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PlacedBundles.takeShown(helper.getLevel(), helper.absolutePos(at), be, player);
        helper.assertTrue(player.getMainHandItem().is(Items.SPECTRAL_ARROW), "took " + player.getMainHandItem());
        helper.assertTrue(helper.getBlockState(at).getValue(PlacedBundleBlock.ARROWS) == 3, "after taking: " + helper.getBlockState(at));
        // a bundle never shows arrows
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
        use(helper, player, filled(StorageItems.REINFORCED_BUNDLE, new ItemStack(Items.ARROW, 64)), new BlockPos(1, 1, 4), Direction.UP);
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 4)).getValue(PlacedBundleBlock.ARROWS) == 0, "bundle shows arrows");
        helper.succeed();
    }

    private static ItemStack filled(Item item, ItemStack... contents) {
        ItemStack bundle = new ItemStack(item);
        PlacedBundles.setContents(bundle, List.of(contents));
        return bundle;
    }

    /** Right-click with {@code stack} on side {@code face} of block {@code on} (relative). */
    private static InteractionResult use(GameTestHelper helper, Player player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    private static ItemEntity dropped(GameTestHelper helper, Item item) {
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (entity.getItem().is(item) && entity.isAlive()) {
                return entity;
            }
        }
        return null;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
