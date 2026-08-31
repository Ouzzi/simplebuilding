package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side behaviour of the mod's remaining tools: the rotator, the quiver, the reinforced
 * bundle's capacity, the ore detector's modes, the octant's corner selection and the building
 * wand's actual placement.
 *
 * <p>These items were covered only by the blanket data checks ("is registered", "recipe is
 * valid") before, so a port could break what any of them <em>does</em> without a single test
 * turning red.
 *
 * <p>The mock player is handed back explicitly at the end of every test rather than through
 * {@code runBeforeTestEnd}: MC 1.21.11 has no such hook, and doing it the same way in both
 * copies of this file keeps the two Minecraft lines from drifting apart any further than the
 * API differences already force them to.
 */
public final class ItemBehaviourTests {

    private ItemBehaviourTests() {
    }

    /** Tick budget for {@link #buildingWandFillsThePlaneItIsPointedAt}. */
    public static final int WAND_MAX_TICKS = 200;

    // =====================================================================================
    // ROTATOR
    // =====================================================================================

    /**
     * On a block with an axis (logs, pillars) the rotator has two distinct behaviours: clicking
     * the middle of a face cycles the axis, clicking within two pixels of a rim snaps the axis
     * parallel to that rim. Both are pure geometry, which is exactly the kind of code that
     * survives a port compiling but stops matching the old feel.
     */
    public static void rotatorTurnsLogsByClickedFaceAndRim(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        BlockPos log = new BlockPos(2, 1, 2);

        // --- centre of the top face: the log already points along Y, so it advances Y -> Z ---
        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "clicking the top of an upright log did not advance the axis to Z, got " + logAxis(helper, log));

        // --- centre of a side face: the axis jumps to the axis of that face ---
        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.NORTH, new Vec3(0.5, 0.5, 0.0));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "clicking the north face did not lay the log down along Z, got " + logAxis(helper, log));

        // --- rim of the top face: the axis snaps parallel to the rim, not to the face ---
        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.UP, new Vec3(0.05, 1.0, 0.5));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.X,
                "clicking the west rim did not align the log along X, got " + logAxis(helper, log));

        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.UP, new Vec3(0.5, 1.0, 0.05));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "clicking the north rim did not align the log along Z, got " + logAxis(helper, log));

        finish(helper, player);
    }

    /**
     * On a block with a facing (furnaces, pistons) the rotator turns it clockwise around the
     * axis of the clicked face, and counter clockwise while sneaking. A block with neither
     * property has to be left alone and the interaction has to report that nothing happened -
     * otherwise the rotator would swallow the click and consume durability for nothing.
     */
    public static void rotatorCyclesFacingBlocksAndLeavesPlainBlocksAlone(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        BlockPos furnace = new BlockPos(4, 1, 2);

        // --- clockwise around Y: north -> east ---
        setFurnaceFacing(helper, furnace, Direction.NORTH);
        player.setShiftKeyDown(false);
        useOn(helper, player, rotator, furnace, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(furnaceFacing(helper, furnace) == Direction.EAST,
                "the furnace did not turn clockwise, it faces " + furnaceFacing(helper, furnace));

        // --- sneaking reverses it: north -> west ---
        setFurnaceFacing(helper, furnace, Direction.NORTH);
        player.setShiftKeyDown(true);
        useOn(helper, player, rotator, furnace, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(furnaceFacing(helper, furnace) == Direction.WEST,
                "sneaking did not reverse the rotation, the furnace faces " + furnaceFacing(helper, furnace));
        player.setShiftKeyDown(false);

        // --- a block without any rotation property must be refused, not silently accepted ---
        BlockPos stone = new BlockPos(6, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        InteractionResult result = useOn(helper, player, rotator, stone, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(result == InteractionResult.PASS,
                "the rotator claimed to have rotated plain stone, result was " + result);
        helper.assertBlockPresent(Blocks.STONE, stone);

        finish(helper, player);
    }

    // =====================================================================================
    // QUIVER AND REINFORCED BUNDLE
    // =====================================================================================

    /**
     * The quiver is a bundle that only takes arrows. Every arrow type has to fit, everything
     * else has to bounce off - that restriction is the only thing separating it from the
     * reinforced bundle it inherits from.
     */
    public static void quiverTakesArrowsAndRefusesEverythingElse(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        ItemStack quiver = new ItemStack(ModItems.QUIVER);
        ReinforcedBundleItem item = (ReinforcedBundleItem) quiver.getItem();

        helper.assertTrue(item.tryInsertStackFromWorld(quiver, new ItemStack(Items.ARROW, 8), player),
                "the quiver refused plain arrows");
        helper.assertTrue(item.tryInsertStackFromWorld(quiver, new ItemStack(Items.SPECTRAL_ARROW, 8), player),
                "the quiver refused spectral arrows");
        helper.assertTrue(item.tryInsertStackFromWorld(quiver, new ItemStack(Items.TIPPED_ARROW, 8), player),
                "the quiver refused tipped arrows");

        helper.assertTrue(!item.tryInsertStackFromWorld(quiver, new ItemStack(Items.STONE, 8), player),
                "the quiver accepted stone, which makes it an ordinary bundle");
        helper.assertTrue(!item.tryInsertStackFromWorld(quiver, new ItemStack(Items.BOW), player),
                "the quiver accepted a bow");

        // The reinforced bundle it inherits from has no such restriction.
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        helper.assertTrue(((ReinforcedBundleItem) bundle.getItem())
                        .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player),
                "the reinforced bundle refused stone");

        finish(helper, player);
    }

    /**
     * Capacity is the reinforced bundle's whole point, and it comes from three independent
     * sources: the material tier, Deep Pockets and Drawer. Rather than pinning down exact
     * numbers - which are balancing, not behaviour - this fills each variant until it refuses
     * and asserts the ordering between them. That still fails loudly if an enchantment stops
     * being read, which is what a port breaks.
     */
    public static void bundleCapacityGrowsWithTierAndEnchantments(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        int plain = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, null, 0);
        int netherite = fillUntilFull(helper, player, ModItems.NETHERITE_BUNDLE, null, 0);
        int enderite = fillUntilFull(helper, player, ModItems.ENDERITE_BUNDLE, null, 0);

        helper.assertTrue(plain > 0, "the plain reinforced bundle took nothing at all");
        helper.assertTrue(netherite > plain,
                "the netherite bundle (" + netherite + ") holds no more than the reinforced one (" + plain + ")");
        helper.assertTrue(enderite > netherite,
                "the enderite bundle (" + enderite + ") holds no more than the netherite one (" + netherite + ")");

        int deepPockets1 = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 1);
        int deepPockets2 = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 2);
        int drawer = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, 1);

        helper.assertTrue(deepPockets1 > plain,
                "Deep Pockets I did not raise the capacity (" + deepPockets1 + " vs " + plain + ")");
        helper.assertTrue(deepPockets2 > deepPockets1,
                "Deep Pockets II is no better than level I (" + deepPockets2 + " vs " + deepPockets1 + ")");
        helper.assertTrue(drawer > plain,
                "Drawer did not raise the capacity (" + drawer + " vs " + plain + ")");

        finish(helper, player);
    }

    // =====================================================================================
    // ORE DETECTOR
    // =====================================================================================

    /**
     * Sneak-using the detector steps through its modes and wraps around; sneak-using it on a
     * block teaches it that block as a custom target. The sonar ping itself is sound and
     * particles, which a headless test cannot observe - so this covers the state the player
     * actually configures, and nothing more. Whether the beam is drawn is a client concern and
     * is left to the renderer tests.
     */
    public static void oreDetectorCyclesModesAndLearnsACustomBlock(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack detector = new ItemStack(ModItems.ORE_DETECTOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, detector);
        player.setShiftKeyDown(true);

        int first = customData(detector).getIntOr("Mode", 0);
        detector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        int second = customData(detector).getIntOr("Mode", 0);
        helper.assertTrue(second != first,
                "sneak-using the ore detector did not change its mode, it stayed at " + first);

        // Walk the whole cycle: it has to come back to where it started, not run off the end.
        int guard = 0;
        while (customData(detector).getIntOr("Mode", 0) != first && guard++ < 32) {
            detector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(guard < 32, "the ore detector mode cycle never wrapped back to the start");

        // Sneak-using it on a block adopts that block as the custom target.
        BlockPos target = new BlockPos(3, 1, 5);
        helper.setBlock(target, Blocks.DIAMOND_ORE);
        useOn(helper, player, detector, target, Direction.UP, new Vec3(0.5, 1.0, 0.5));

        CompoundTag nbt = customData(detector);
        helper.assertTrue(nbt.contains("CustomBlock"),
                "sneak-clicking a block did not teach the detector a custom target");
        helper.assertTrue(nbt.getCompoundOrEmpty("CustomBlock").getString("Name").orElse("")
                        .contains("diamond_ore"),
                "the detector stored the wrong custom block: " + nbt.getCompoundOrEmpty("CustomBlock"));

        player.setShiftKeyDown(false);
        finish(helper, player);
    }

    // =====================================================================================
    // OCTANT
    // =====================================================================================

    /**
     * The octant is a pure selection tool: a click sets the first corner, a sneak-click the
     * second, and once it is locked neither may move. Sneak-using it in the air throws the
     * selection away again.
     */
    public static void octantStoresBothCornersAndRespectsTheLock(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);

        BlockPos firstRelative = new BlockPos(2, 1, 6);
        BlockPos secondRelative = new BlockPos(5, 1, 6);
        helper.setBlock(firstRelative, Blocks.STONE);
        helper.setBlock(secondRelative, Blocks.STONE);
        BlockPos first = helper.absolutePos(firstRelative);
        BlockPos second = helper.absolutePos(secondRelative);

        player.setShiftKeyDown(false);
        useOn(helper, player, octant, firstRelative, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "the plain click did not store the first corner");

        player.setShiftKeyDown(true);
        useOn(helper, player, octant, secondRelative, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(cornerEquals(customData(octant), "Pos2", second),
                "the sneak click did not store the second corner");
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "the sneak click overwrote the first corner");

        // --- locked: neither corner may move any more ---
        CompoundTag locked = customData(octant);
        locked.putBoolean("Locked", true);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(locked));

        player.setShiftKeyDown(false);
        useOn(helper, player, octant, secondRelative, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "a locked octant let the first corner be moved");

        // --- sneak use in the air clears the selection, lock included ---
        CompoundTag unlocked = customData(octant);
        unlocked.putBoolean("Locked", false);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(unlocked));

        player.setShiftKeyDown(true);
        octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(customData(octant).isEmpty(),
                "sneak-using the octant in the air did not clear the selection");

        player.setShiftKeyDown(false);
        finish(helper, player);
    }

    // =====================================================================================
    // BUILDING WAND
    // =====================================================================================

    /**
     * The wand's whole purpose: click a face, get a filled plane in front of it, paid for out
     * of the inventory. It builds one ring per tick batch, so this drives the player tick until
     * the wand reports itself done and then checks the finished plane.
     *
     * <p>The radius is pinned to 1 through the wand's own settings rather than left at the tier
     * maximum, so the expected 3x3 is the same on every tier and the test states a shape instead
     * of restating a balancing number.
     */
    public static void buildingWandFillsThePlaneItIsPointedAt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        CompoundTag settings = customData(wand);
        settings.putInt("SettingsRadius", 1);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));

        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 64));

        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);

        // Clicking the top face builds in the layer above it.
        useOn(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));

        helper.succeedWhen(() -> {
            // The gametest server never pumps a mock player's connection, so the vanilla player
            // tick - and with it the wand's inventoryTick - has to be driven from here.
            player.connection.tick();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertBlockPresent(Blocks.STONE, anchor.offset(dx, 1, dz));
                }
            }
            // One ring further out must stay empty, otherwise the radius setting is ignored.
            helper.assertBlockPresent(Blocks.AIR, anchor.offset(2, 1, 0));
            helper.assertBlockPresent(Blocks.AIR, anchor.offset(0, 1, 2));

            helper.assertTrue(player.getInventory().countItem(Items.STONE) < 64,
                    "the wand built the plane without paying for it out of the inventory");

            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // A mock player does not start in survival, and the wand test checks that the blocks are
        // paid for - a creative player never pays. setGameMode alone leaves isCreative() reporting
        // true, so the game mode is changed directly as well.
        player.setGameMode(GameType.SURVIVAL);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    private static void finish(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    /**
     * Right clicks a block face at a precise spot on that face. The offset is given inside the
     * block (0..1 per axis), which is what the rotator's rim detection reads.
     */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos, Direction face, Vec3 offsetInBlock) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + offsetInBlock.x, pos.getY() + offsetInBlock.y, pos.getZ() + offsetInBlock.z);
        BlockHitResult hitResult = new BlockHitResult(hit, face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    private static void setLogAxis(GameTestHelper helper, BlockPos pos, Direction.Axis axis) {
        helper.setBlock(pos, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, axis));
    }

    private static Direction.Axis logAxis(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BlockStateProperties.AXIS);
    }

    private static void setFurnaceFacing(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
    }

    private static Direction furnaceFacing(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean cornerEquals(CompoundTag nbt, String key, BlockPos expected) {
        int[] stored = nbt.getIntArray(key).orElse(new int[0]);
        return stored.length == 3
                && stored[0] == expected.getX()
                && stored[1] == expected.getY()
                && stored[2] == expected.getZ();
    }

    /**
     * Pushes 64 item stacks into a fresh bundle until it refuses one, and reports how many
     * items went in. Bounded so a bundle that never fills up fails the test instead of hanging
     * the run.
     */
    private static int fillUntilFull(GameTestHelper helper, ServerPlayer player, Item bundleItem,
                                     ResourceKey<Enchantment> enchantmentKey, int level) {
        ItemStack bundle = new ItemStack(bundleItem);
        if (enchantmentKey != null) {
            bundle.enchant(helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey), level);
        }
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();

        int inserted = 0;
        for (int i = 0; i < 512; i++) {
            if (!item.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 64), player)) {
                return inserted;
            }
            inserted += 64;
        }
        helper.fail("the bundle " + bundleItem + " never reported itself full");
        return inserted;
    }
}
