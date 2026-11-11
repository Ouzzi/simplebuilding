package com.simplegadgets.test;

import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.config.GadgetsConfig;
import com.simplegadgets.detector.DetectorItem;
import com.simplegadgets.detector.PlacedDetector;
import com.simplelib.api.GadgetActions;
import com.simplelib.api.PlacedGadgets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Detector in Simple Gadgets (OW24-2 G3, b-gadg14): the search per mode, stone damping, calibrating with a sneak click
 * (no block entities), a ping's cost and its event, the placed form through SimpleLib {@link PlacedGadgets} (a test plate),
 * the recipes and the config factors. Long sight lines run on a block source of their own ({@link LineWorld}), as in
 * SimpleBuilding's {@code OreDetectorTests}, which keep the full behaviour for 26.2.
 */
public final class DetectorTests {
    private static final Identifier IRON_POLE = Identifier.fromNamespaceAndPath(SimpleGadgets.OLD_NAMESPACE, "iron_pole");
    /** Modes by their stored index: iron 0, gold 1, diamond 2, netherite 3, all 4, custom 5. */
    private static final int MODE_ALL = 4, MODE_CUSTOM = 5;
    private static final int LINE_Y = 64;
    private static final Vec3 LINE_EYE = new Vec3(0.5, LINE_Y + 0.5, 0.5);
    /**
     * Targets in the real level: blocks no other test places and no world generation makes, one per test, so a block
     * left on a neighbouring test plate is never found (Gate 180 failed on a froglight from another test).
     */
    private static final Block PING_TARGET = Blocks.HONEYCOMB_BLOCK;
    private static final Block PLATE_TARGET = Blocks.DRIED_KELP_BLOCK;

    private DetectorTests() {
    }

    private static boolean withSimpleBuilding() {
        return BuiltInRegistries.ITEM.containsKey(IRON_POLE);
    }

    private static DetectorItem detector() {
        return (DetectorItem) GadgetsRegistry.DETECTOR;
    }

    /** The default iron mode finds iron and skips nearer gold; "all ores" takes the nearest of any class. */
    public static void eachModeFindsItsOresAndAllOresTheNearest(GameTestHelper helper) {
        LineWorld line = new LineWorld();
        line.put(3, Blocks.GOLD_ORE);
        line.put(6, Blocks.IRON_ORE);
        ItemStack iron = new ItemStack(GadgetsRegistry.DETECTOR);
        same(helper, line.scan(helper, iron), line.pos(6), "iron mode target");
        ItemStack all = inMode(MODE_ALL);
        same(helper, line.scan(helper, all), line.pos(3), "all-ores mode target");
        line.put(3, Blocks.STONE);
        line.put(2, Blocks.STONE);
        same(helper, line.scan(helper, all), line.pos(6), "iron behind two stone (loss 8 of 18)");
        helper.assertTrue(line.scan(helper, inMode(2)) == null, "the diamond mode found something on a line without diamond ore");
        helper.succeed();
    }

    /** Dense rock damps more: iron through four stone is found, through five it is not; deepslate costs more than stone. */
    public static void rockDampsTheSignalAndDenseRockMore(GameTestHelper helper) {
        LineWorld line = new LineWorld();
        for (int x = 1; x <= 4; x++) line.put(x, Blocks.STONE);
        line.put(5, Blocks.IRON_ORE);
        ItemStack iron = new ItemStack(GadgetsRegistry.DETECTOR);
        same(helper, line.scan(helper, iron), line.pos(5), "iron behind four stone (loss 16 of 18)");
        line.put(5, Blocks.STONE);
        line.put(6, Blocks.IRON_ORE);
        helper.assertTrue(line.scan(helper, iron) == null, "iron behind five stone (loss 20 of 18) was found");
        helper.assertTrue(DetectorItem.Material.DENSE.loss > DetectorItem.Material.STONE.loss
                && DetectorItem.Material.STONE.loss > DetectorItem.Material.SOFT.loss, "the material losses no longer rise with density");
        helper.succeed();
    }

    /** A sneak click on a block calibrates (custom mode); block entities refuse; a plain click changes nothing. */
    public static void sneakClickingCalibratesAndBlockEntitiesRefuse(GameTestHelper helper) {
        ServerPlayer player = AttractorTests.mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        BlockPos sample = new BlockPos(3, 1, 3);
        helper.setBlock(sample, Blocks.OAK_LOG);
        ItemStack stack = new ItemStack(GadgetsRegistry.DETECTOR);
        InteractionResult plain = useOn(helper, player, stack, sample);
        helper.assertTrue(plain == InteractionResult.PASS && !customData(stack).contains("CustomBlock"), "a plain click did something: " + plain);
        player.setShiftKeyDown(true);
        InteractionResult calibrate = useOn(helper, player, stack, sample);
        helper.assertTrue(calibrate == InteractionResult.SUCCESS && DetectorItem.isArmed(stack), "the sneak click did not calibrate: " + calibrate);
        helper.assertTrue(DetectorItem.calibratedTarget(stack, helper.getLevel().registryAccess()).is(Blocks.OAK_LOG), "calibrated on the wrong block");
        ItemStack chest = new ItemStack(GadgetsRegistry.DETECTOR);
        helper.setBlock(sample, Blocks.CHEST);
        InteractionResult refused = useOn(helper, player, chest, sample);
        player.setShiftKeyDown(false);
        helper.assertTrue(refused == InteractionResult.FAIL && !customData(chest).contains("CustomBlock"), "a chest was calibrated: " + refused);
        helper.assertTrue(!PlacedGadgets.placeable(new ItemStack(GadgetsRegistry.DETECTOR)) && PlacedGadgets.placeable(stack),
                "only a calibrated Detector may be laid down");
        helper.succeed();
    }

    /**
     * A ping that finds something costs 1 durability and tells partners ("ore_detected"); an empty ping costs nothing.
     * The ping searches the real level, so the target is a block no other test places and no world generation makes
     * (Gate 180: a froglight left on a neighbouring test plate made the "empty" ping find something), and the reach is
     * cut to its minimum factor (6 blocks) so neighbouring plates stay out of the sphere.
     */
    public static void pingsCostDurabilityOnlyWhenTheyFindSomething(GameTestHelper helper) {
        double range = GadgetsConfig.detectorRangeMultiplier;
        try {
            GadgetsConfig.detectorRangeMultiplier = 0.25;
            ServerPlayer player = AttractorTests.mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
            player.getAbilities().instabuild = false;
            ItemStack stack = calibratedOn(PING_TARGET);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            AtomicInteger told = new AtomicInteger();
            GadgetActions.listen(GadgetActions.ORE_DETECTED, p -> {
                if (p == player) told.incrementAndGet();
            });
            BlockPos target = new BlockPos(3, 2, 3);
            helper.setBlock(target, PING_TARGET);
            BlockPos found = detector().ping(helper.getLevel(), stack, player, EquipmentSlot.MAINHAND);
            same(helper, found, helper.absolutePos(target), "the ping target");
            helper.assertValueEqual(stack.getDamageValue(), DetectorItem.DURABILITY_PER_HIT, "durability after a finding ping");
            helper.assertValueEqual(told.get(), 1, "ore_detected events for the finding ping");
            helper.assertTrue(stack.has(DataComponents.LODESTONE_TRACKER), "the needle does not point at the find");
            helper.setBlock(target, Blocks.AIR);
            BlockPos empty = detector().ping(helper.getLevel(), stack, player, EquipmentSlot.MAINHAND);
            helper.assertTrue(empty == null, "the empty ping found something at " + empty);
            helper.assertValueEqual(stack.getDamageValue(), DetectorItem.DURABILITY_PER_HIT, "an empty ping cost durability");
            helper.assertTrue(!stack.has(DataComponents.LODESTONE_TRACKER), "the needle keeps pointing after an empty ping");
        } finally {
            GadgetsConfig.detectorRangeMultiplier = range;
        }
        helper.succeed();
    }

    /** Laid down (test plate): it finds its target from the plate, notices a holder of the target block and wears down. */
    public static void thePlacedDetectorSearchesFromThePlate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        AttractorTests.TestPlate plate = new AttractorTests.TestPlate(calibratedOn(PLATE_TARGET), Vec3.atCenterOf(pos));
        helper.setBlock(new BlockPos(4, 1, 1), PLATE_TARGET);
        PlacedDetector.Result result = PlacedDetector.scan(level, pos, plate, false);
        same(helper, result.found(), helper.absolutePos(new BlockPos(4, 1, 1)), "the placed Detector's find");
        ServerPlayer holder = AttractorTests.mockPlayer(helper, new Vec3(3.5, 1.0, 3.5));
        holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PLATE_TARGET.asItem()));
        helper.assertTrue(PlacedDetector.nearestHolder(level, pos, PLATE_TARGET.defaultBlockState()) == holder,
                "the placed Detector does not notice the holder of its target");
        int paid = 0;
        for (int i = 0; i < 40; i++) paid += PlacedDetector.wear(level, pos, plate, new PlacedDetector.Result(helper.absolutePos(new BlockPos(4, 1, 1)), null));
        helper.assertTrue(paid > 5 && paid < 35, "about half the tones should cost durability, " + paid + " of 40 did");
        helper.assertTrue(plate.changes > 0 && plate.stack().getDamageValue() > 0, "the worn plate stack was not reported changed");
        helper.assertTrue(PlacedDetector.tickPlate(level, pos, new AttractorTests.TestPlate(new ItemStack(GadgetsRegistry.DETECTOR), Vec3.atCenterOf(pos)))
                == PlacedDetector.Result.NONE, "an uncalibrated plate searched");
        helper.succeed();
    }

    /** " ES" / "ECE" / "GE ": recovery compass, four echo shards, a calibrated sculk sensor and a gold core (alone a gold ingot). */
    public static void theDetectorRecipeKeepsItsShape(GameTestHelper helper) {
        boolean sb = withSimpleBuilding();
        Item g = sb ? BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplelib", "gold_core")) : Items.GOLD_INGOT;
        helper.assertTrue(g != Items.AIR, "the gold core is not registered");
        Item e = Items.ECHO_SHARD;
        CraftingInput grid = AttractorTests.grid(null, e, Items.CALIBRATED_SCULK_SENSOR, e, Items.RECOVERY_COMPASS, e, g, e, null);
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the Detector pattern matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), sb ? "simplegadgets:detector" : "simplegadgets:detector_without_simplebuilding", "Detector recipe");
        helper.assertTrue(match.get().value().assemble(grid).is(GadgetsRegistry.DETECTOR), "the recipe makes something else");
        helper.succeed();
    }

    /** detectorRangeMultiplier scales every class's reach (at least 1 block); the scan interval doubles in the off hand. */
    public static void theConfigScalesReachAndInterval(GameTestHelper helper) {
        double range = GadgetsConfig.detectorRangeMultiplier;
        int interval = GadgetsConfig.detectorScanIntervalTicks;
        try {
            GadgetsConfig.detectorRangeMultiplier = 0.5;
            helper.assertValueEqual(DetectorItem.OreClass.COMMON.range(false), 12, "iron reach at factor 0.5");
            GadgetsConfig.detectorRangeMultiplier = 1.0;
            helper.assertValueEqual(DetectorItem.OreClass.COMMON.range(false), 24, "iron reach at factor 1");
            GadgetsConfig.detectorScanIntervalTicks = 30;
            helper.assertValueEqual(DetectorItem.scanInterval(EquipmentSlot.MAINHAND), 30, "main hand interval");
            helper.assertValueEqual(DetectorItem.scanInterval(EquipmentSlot.OFFHAND), 60, "off hand interval");
            helper.assertValueEqual(PlacedDetector.interval(), 60, "placed interval");
        } finally {
            GadgetsConfig.detectorRangeMultiplier = range;
            GadgetsConfig.detectorScanIntervalTicks = interval;
        }
        helper.succeed();
    }

    // ---- helpers

    private static void same(GameTestHelper helper, BlockPos actual, BlockPos expected, String what) {
        helper.assertTrue(java.util.Objects.equals(actual, expected), what + ": expected " + expected + ", got " + actual);
    }

    private static ItemStack inMode(int mode) {
        ItemStack stack = new ItemStack(GadgetsRegistry.DETECTOR);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putInt("Mode", mode));
        return stack;
    }

    private static ItemStack calibratedOn(Block block) {
        ItemStack stack = inMode(MODE_CUSTOM);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.put("CustomBlock", NbtUtils.writeBlockState(block.defaultBlockState())));
        return stack;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /** Air everywhere except the blocks on the line y = 64, z = 0 along +X (the test rooms are too small for long lines). */
    private static final class LineWorld implements BlockGetter {
        private final Map<Integer, BlockState> line = new HashMap<>();

        void put(int x, Block block) {
            line.put(x, block.defaultBlockState());
        }

        BlockPos pos(int x) {
            return new BlockPos(x, LINE_Y, 0);
        }

        BlockPos scan(GameTestHelper helper, ItemStack stack) {
            return detector().findTarget(this, helper.getLevel(), stack, LINE_EYE);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (pos.getY() != LINE_Y || pos.getZ() != 0) return Blocks.AIR.defaultBlockState();
            return line.getOrDefault(pos.getX(), Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}
