package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.networking.BuildingWandConfigurePayload;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.networking.MasterBuilderPickPayload;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.OctantConfigurePayload;
import com.simplebuilding.networking.OctantScrollPayload;
import com.simplebuilding.networking.SpaceKeyPayload;
import com.simplebuilding.networking.TrimBenefitPayload;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

/**
 * Server side behaviour of the mod's own network payloads.
 *
 * <p>This is the layer that broke silently during the multiloader port: the NeoForge
 * {@code canSend} check used to look only at {@code player.connection != null} and therefore
 * claimed every channel was open. Driving the handlers directly cannot catch that particular
 * bug - it lives one layer further out, in the loader adapter - but it pins down what each
 * handler is supposed to do, so the next port has a reference to fail against instead of
 * "it looked fine in game".
 *
 * <p>Every handler takes {@code (payload, ServerPlayer)} and nothing else, so all of this runs
 * against a mock player without a client, a screen or a real connection.
 *
 * <h2>Known defects (asserted around, so that no test here cements them)</h2>
 *
 * <ul>
 *   <li><b>Double Jump: client and server disagree about which slot counts.</b>
 *       {@code handleDoubleJump} reads {@code EquipmentSlot.FEET} only, which is what the
 *       enchantment declares ({@code slots: ["feet"]}, {@code supported_items:
 *       #minecraft:foot_armor}). The client side {@code DoubleJumpController#getDoubleJumpLevel}
 *       loops over every {@code EquipmentSlot} instead. Through the anvil the two can never
 *       disagree, but with {@code /enchant} or a foreign item the client performs the air jump,
 *       clears {@code fallDistance} locally and sends the payload, while the server refuses -
 *       so the player jumps and then takes the full fall damage anyway. The test below pins the
 *       server side, which is the side the data backs, and touches nothing on the client.</li>
 *   <li><b>Hollow, LayerMode and FillOrder are stored and never used.</b>
 *       {@code handleOctantConfigure} writes all three into the item; the only code that reads
 *       them back is {@code client/gui/OctantScreen}. Nothing on the server, and nothing in any
 *       placement path, ever looks at them. {@link #octantConfigureStoresTheWholeSelectionState}
 *       therefore proves that they survive the trip and nothing beyond that - it cannot tell
 *       whether they have started to mean something, in either direction.</li>
 * </ul>
 */
public final class NetworkHandlerTests {

    private NetworkHandlerTests() {
    }

    /**
     * The selection shapes in the order the octant cycles through them, as literals.
     *
     * <p>Deliberately not {@code OctantItem.SelectionShape.values()}: the constant's <em>name</em>
     * is the item's persistence format. Both octant handlers write it into the {@code Shape} NBT
     * string and read it back with {@code valueOf} inside a {@code try/catch}, so a renamed,
     * deleted or reordered constant throws nowhere - it resets every octant in an existing world
     * to the first shape, and the shape suffix {@code OctantItem#getName} appends to the item
     * name disappears with it. An expectation taken from the same enum the handler reads would
     * move along with any such edit and could never fail.
     */
    private static final List<String> OCTANT_SHAPE_NAMES = List.of(
            "CUBOID", "CYLINDER", "TRIANGLE", "PYRAMID", "SPHERE", "RECTANGLE", "ELLIPSE");

    /** The fill orders, spelled out for the same reason as {@link #OCTANT_SHAPE_NAMES}. */
    private static final List<String> OCTANT_FILL_ORDER_NAMES = List.of(
            "DEFAULT", "BOTTOM_UP", "TOP_DOWN");

    /** Starting position of the octant's first corner in the scroll cases. */
    private static final int[] SCROLL_CORNER_1 = {10, 20, 30};

    /** Starting position of the second corner; far enough away to tell the two apart. */
    private static final int[] SCROLL_CORNER_2 = {40, 50, 60};

    /** How many stone the reinforced bundle is filled with before a Master Builder pick. */
    private static final int BUNDLE_STONE = 16;

    // =====================================================================================
    // DOUBLE JUMP
    // =====================================================================================

    /**
     * The air jump is granted by the boots, not by the packet: an unenchanted pair has to be
     * ignored completely, otherwise anyone could fly by spamming the payload. With the
     * enchantment the fall distance is cleared, which is what makes the second jump survivable.
     *
     * <p><strong>Not covered here:</strong> that the boots take a point of wear in survival.
     * The handler guards that branch with {@code !player.isCreative()}, and the player this
     * class uses is permanently creative - {@code GameTestHelper#makeMockServerPlayerInLevel}
     * builds an anonymous subclass that overrides {@code gameMode()} to return {@code CREATIVE}
     * unconditionally, so no amount of {@code setGameMode} changes it. What can be checked here
     * is the other side of that guard: in creative the boots must stay pristine, and that is
     * asserted below. The wear itself is covered - it is not a manual check any more:
     * {@code ConsumptionAndDurabilityTests#doubleJumpBootsWearDownForTheSurvivalPlayer} drives
     * the same handler with a player from {@code GameTestHelper#makeMockServerPlayer(GameType)},
     * a <em>second</em> mock class whose {@code gameMode()} answers what it was asked for.
     *
     * <p>What breaks this test: an enchantment check that stops looking at the enchantment (any
     * pair of boots would jump), one that stops clearing {@code fallDistance}, a wear branch that
     * loses its creative guard, and a slot lookup that stops being the feet - the last one is the
     * case below with the enchantment on the head and in the hand. That slot is not a taste
     * question: the enchantment definition declares {@code slots: ["feet"]} and
     * {@code supported_items: #minecraft:foot_armor}, so the feet are the only place vanilla
     * would ever evaluate it. The client side {@code DoubleJumpController#getDoubleJumpLevel}
     * does loop over every {@code EquipmentSlot}, which makes "unify the two" a plausible edit -
     * and it would hand out air jumps for a helmet.
     */
    public static void doubleJumpNeedsEnchantedBootsAndWearsThem(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- plain boots: the handler must not touch anything ---
        ItemStack plain = new ItemStack(Items.DIAMOND_BOOTS);
        player.setItemSlot(EquipmentSlot.FEET, plain);
        player.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);

        helper.assertTrue(player.fallDistance == 7.5F,
                "unenchanted boots cleared the fall distance, so anyone could air jump");
        helper.assertTrue(plain.getDamageValue() == 0, "unenchanted boots took wear");

        // --- enchanted boots: the fall distance is cleared ---
        ItemStack enchanted = new ItemStack(Items.DIAMOND_BOOTS);
        enchanted.enchant(enchantment(helper, ModEnchantments.DOUBLE_JUMP), 1);
        player.setItemSlot(EquipmentSlot.FEET, enchanted);
        player.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);

        helper.assertTrue(player.fallDistance == 0.0F,
                "the fall distance survived the air jump, so the landing would still hurt");

        // --- the mock player is creative by construction, so the boots must stay pristine ---
        helper.assertTrue(player.isCreative(),
                "the gametest mock player is no longer creative; the wear branch is reachable now "
                        + "and this test should assert the wear instead of the creative case");
        helper.assertTrue(enchanted.getDamageValue() == 0,
                "the boots were worn down even though the player is in creative, they took "
                        + enchanted.getDamageValue());

        // --- no boots at all: must not throw ---
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        player.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);
        helper.assertTrue(player.fallDistance == 7.5F, "a barefoot player got an air jump");

        // --- the enchantment counts on the feet and nowhere else ---
        // Both stacks below are illegal in game (#minecraft:foot_armor is the supported item tag),
        // ItemStack#enchant simply does not ask - which is exactly what makes them usable as a
        // probe for the slot lookup. With the feet empty the handler has to stay out of it.
        ItemStack enchantedHelmet = new ItemStack(Items.DIAMOND_HELMET);
        enchantedHelmet.enchant(enchantment(helper, ModEnchantments.DOUBLE_JUMP), 1);
        player.setItemSlot(EquipmentSlot.HEAD, enchantedHelmet);
        ItemStack carriedBoots = new ItemStack(Items.DIAMOND_BOOTS);
        carriedBoots.enchant(enchantment(helper, ModEnchantments.DOUBLE_JUMP), 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, carriedBoots);
        player.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);

        helper.assertTrue(player.fallDistance == 7.5F,
                "the air jump was granted while the feet were bare - the enchantment was found on "
                        + "the head or in the hand, so the handler no longer reads EquipmentSlot.FEET "
                        + "but searches every slot");
        helper.assertTrue(enchantedHelmet.getDamageValue() == 0 && carriedBoots.getDamageValue() == 0,
                "a piece outside the feet slot was charged for an air jump");

        helper.succeed();
    }

    // =====================================================================================
    // PLAYER STATE FLAGS
    // =====================================================================================

    /**
     * Both flags live on the player through a mixin. If a mixin ever stops applying on one
     * loader, the cast fails and this test says so, instead of the feature going quietly dead
     * on that loader only.
     */
    public static void spaceKeyAndTrimBenefitFlagsReachThePlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        helper.assertTrue(player instanceof ISpaceKeyTracker,
                "the space key mixin is not applied to ServerPlayer on this loader");
        helper.assertTrue(player instanceof TrimBenefitUser,
                "the trim benefit mixin is not applied to ServerPlayer on this loader");

        ISpaceKeyTracker spaceTracker = (ISpaceKeyTracker) player;
        ModMessageHandlers.handleSpaceKey(new SpaceKeyPayload(true), player);
        helper.assertTrue(spaceTracker.simplebuilding$isSpacePressed(),
                "the jump key press never reached the server side tracker");
        ModMessageHandlers.handleSpaceKey(new SpaceKeyPayload(false), player);
        helper.assertTrue(!spaceTracker.simplebuilding$isSpacePressed(),
                "the jump key release never reached the server side tracker");

        TrimBenefitUser trimUser = (TrimBenefitUser) player;
        ModMessageHandlers.handleTrimBenefit(new TrimBenefitPayload(true), player);
        helper.assertTrue(trimUser.simplebuilding$areTrimBenefitsEnabled(),
                "enabling the armour trim benefits never reached the server");
        ModMessageHandlers.handleTrimBenefit(new TrimBenefitPayload(false), player);
        helper.assertTrue(!trimUser.simplebuilding$areTrimBenefitsEnabled(),
                "disabling the armour trim benefits never reached the server");

        helper.succeed();
    }

    // =====================================================================================
    // ITEM CONFIGURATION PAYLOADS
    // =====================================================================================

    /** The wand settings screen writes radius and axis mode into the wand the player holds. */
    public static void buildingWandConfigureStoresRadiusAndAxis(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        ModMessageHandlers.handleBuildingWandConfigure(new BuildingWandConfigurePayload(2, 3), player);

        CompoundTag nbt = customData(wand);
        helper.assertTrue(nbt.getIntOr("SettingsRadius", -1) == 2,
                "the wand radius was not stored, got " + nbt.getIntOr("SettingsRadius", -1));
        helper.assertTrue(nbt.getIntOr("SettingsAxis", -1) == 3,
                "the wand axis mode was not stored, got " + nbt.getIntOr("SettingsAxis", -1));

        // A payload aimed at something that is not a wand must be dropped, not applied blindly.
        ItemStack notAWand = new ItemStack(Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, notAWand);
        ModMessageHandlers.handleBuildingWandConfigure(new BuildingWandConfigurePayload(1, 1), player);
        helper.assertTrue(customData(notAWand).isEmpty(),
                "the wand settings were written onto an unrelated item");

        helper.succeed();
    }

    /**
     * The octant screen sends its whole state in one payload; all of it has to land in the item.
     *
     * <p>Every field is sent twice, the second time with the opposite value. One payload on its
     * own cannot tell a stored value from a constant: a handler that wrote
     * {@code nbt.putBoolean("Hollow", true)} without ever looking at the payload would pass a
     * single {@code hollow=true} round. Each assertion below therefore has a partner that fails
     * if the value stops coming out of the payload.
     *
     * <p>Shape and fill order are compared against the literal names in
     * {@link #OCTANT_SHAPE_NAMES} and {@link #OCTANT_FILL_ORDER_NAMES} rather than against
     * {@code values()[i]}: what is stored is the constant's <em>name</em>, and reading the
     * expectation out of the same enum the handler reads would make a rename invisible.
     *
     * <p><strong>What this cannot say:</strong> that the stored state is used. Locked, the two
     * corners and the shape do have server side readers ({@code OctantItem#useOn},
     * {@code OctantItem#getName}, {@code handleOctantScroll}); Hollow, LayerMode and FillOrder
     * are read by the octant screen and by nothing else, so no gametest can see whether they are
     * evaluated - neither that they still are, nor that they have started to be.
     *
     * <p>What breaks this test: a dropped or renamed NBT key, a value that stops being taken from
     * the payload, an empty shape name that overwrites the stored one, an absent corner that
     * clears the stored one, and a handler that stops checking that the player is holding an
     * octant at all.
     */
    public static void octantConfigureStoresTheWholeSelectionState(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);

        String shape = OCTANT_SHAPE_NAMES.get(1);
        String fillOrder = OCTANT_FILL_ORDER_NAMES.get(0);
        ModMessageHandlers.handleOctantConfigure(new OctantConfigurePayload(
                Optional.of(new BlockPos(1, 2, 3)),
                Optional.of(new BlockPos(4, 5, 6)),
                shape, true, 2, true, true, fillOrder), player);

        CompoundTag nbt = customData(octant);
        helper.assertTrue(Arrays.equals(intArray(nbt, "Pos1"), new int[]{1, 2, 3}),
                "the first corner was not stored");
        helper.assertTrue(Arrays.equals(intArray(nbt, "Pos2"), new int[]{4, 5, 6}),
                "the second corner was not stored");
        helper.assertTrue(nbt.getString("Shape").orElse("").equals(shape),
                "the selection shape was not stored");
        helper.assertTrue(nbt.getBooleanOr("Locked", false), "the lock flag was not stored");
        helper.assertTrue(nbt.getIntOr("Orientation", -1) == 2, "the orientation was not stored");
        helper.assertTrue(nbt.getBooleanOr("Hollow", false), "the hollow flag was not stored");
        helper.assertTrue(nbt.getBooleanOr("LayerMode", false), "the layer mode flag was not stored");
        helper.assertTrue(nbt.getString("FillOrder").orElse("").equals(fillOrder),
                "the fill order was not stored");

        // --- the same eight fields again, every one of them flipped ---
        String otherShape = OCTANT_SHAPE_NAMES.get(4);
        String otherFillOrder = OCTANT_FILL_ORDER_NAMES.get(2);
        ModMessageHandlers.handleOctantConfigure(new OctantConfigurePayload(
                Optional.of(new BlockPos(-7, 8, -9)),
                Optional.of(new BlockPos(10, -11, 12)),
                otherShape, false, 0, false, false, otherFillOrder), player);

        CompoundTag second = customData(octant);
        helper.assertTrue(Arrays.equals(intArray(second, "Pos1"), new int[]{-7, 8, -9}),
                "the first corner kept its old value, so it is not read from the payload");
        helper.assertTrue(Arrays.equals(intArray(second, "Pos2"), new int[]{10, -11, 12}),
                "the second corner kept its old value, so it is not read from the payload");
        helper.assertTrue(second.getString("Shape").orElse("").equals(otherShape),
                "the selection shape kept its old value, so it is not read from the payload");
        helper.assertTrue(!second.getBooleanOr("Locked", true),
                "the lock flag cannot be switched off again - it is stored as a constant true");
        helper.assertTrue(second.getIntOr("Orientation", -1) == 0,
                "the orientation kept its old value, so it is not read from the payload");
        helper.assertTrue(!second.getBooleanOr("Hollow", true),
                "the hollow flag cannot be switched off again - it is stored as a constant true");
        helper.assertTrue(!second.getBooleanOr("LayerMode", true),
                "the layer mode flag cannot be switched off again - it is stored as a constant true");
        helper.assertTrue(second.getString("FillOrder").orElse("").equals(otherFillOrder),
                "the fill order kept its old value, so it is not read from the payload");

        // --- the screen sends empty corners and empty names while nothing is selected yet;
        //     those must leave the stored selection alone instead of wiping it ---
        ModMessageHandlers.handleOctantConfigure(new OctantConfigurePayload(
                Optional.empty(), Optional.empty(),
                "", false, 0, false, false, ""), player);

        CompoundTag third = customData(octant);
        helper.assertTrue(Arrays.equals(intArray(third, "Pos1"), new int[]{-7, 8, -9}),
                "an absent first corner in the payload deleted the stored one");
        helper.assertTrue(Arrays.equals(intArray(third, "Pos2"), new int[]{10, -11, 12}),
                "an absent second corner in the payload deleted the stored one");
        helper.assertTrue(third.getString("Shape").orElse("").equals(otherShape),
                "an empty shape name in the payload overwrote the stored shape");
        helper.assertTrue(third.getString("FillOrder").orElse("").equals(otherFillOrder),
                "an empty fill order in the payload overwrote the stored one");

        // --- a payload aimed at something that is not an octant has to be dropped ---
        ItemStack notAnOctant = new ItemStack(Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, notAnOctant);
        ModMessageHandlers.handleOctantConfigure(new OctantConfigurePayload(
                Optional.of(new BlockPos(1, 2, 3)),
                Optional.of(new BlockPos(4, 5, 6)),
                shape, true, 2, true, true, fillOrder), player);
        helper.assertTrue(customData(notAnOctant).isEmpty(),
                "the octant settings were written onto an unrelated item");

        helper.succeed();
    }

    /**
     * Scrolling on the octant either cycles the shape (with alt) or nudges one of the two
     * corners along the direction the player looks at. Steep pitch switches to up/down - the
     * part most likely to rot when the look direction API changes between versions.
     *
     * <p>Three things this test has to do that the obvious version does not:
     *
     * <ul>
     *   <li><b>The shape ring is spelled out.</b> The expectation comes from
     *       {@link #OCTANT_SHAPE_NAMES}, not from {@code SelectionShape.values()}. Reading it out
     *       of the enum the handler reads makes the test agree with any enum: a deleted or
     *       swapped constant would move the assertion with it. On top of the ring, one round
     *       starts from a name that is already stored in the item, which is what an octant out of
     *       an existing world looks like - {@code valueOf} sits in a {@code try/catch}, so an
     *       unknown name does not throw, it silently resets the selection to the first shape.</li>
     *   <li><b>Four horizontal facings, not one.</b> Yaw 0 is south, and south is also what a
     *       hard coded {@code Direction.SOUTH} would produce, so a single facing cannot tell the
     *       two apart. West, north and east are asked as well, which pins both axes and both
     *       signs of {@code player.getDirection()}.</li>
     *   <li><b>The pitch threshold is bracketed.</b> {@code -60} and {@code +60} have to stay
     *       horizontal while {@code -61} and {@code +61} switch to up and down. Probing only
     *       {@code +-70} leaves everything between 0 and 70 degrees open: a threshold moved to
     *       30 degrees would keep such a test green and would flip the axis in normal play.</li>
     * </ul>
     *
     * <p>What breaks this test: a shape constant that is renamed, deleted or reordered, a facing
     * that stops being read from the player, a pitch threshold that moves, ctrl and shift
     * addressing the wrong corner, and a scroll amount that stops being honoured.
     */
    public static void octantScrollCyclesShapesAndNudgesCornersByFacing(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);
        int shapeCount = OCTANT_SHAPE_NAMES.size();

        // Setup guard: the ring below walks the literal list and expects to arrive back at the
        // first shape, so an added eighth shape has to be reported here instead of quietly
        // turning the ring into a partial walk.
        helper.assertValueEqual(OctantItem.SelectionShape.values().length, shapeCount,
                "number of selection shapes");

        // --- alt scroll walks the whole ring, in the documented order, and comes back ---
        for (int step = 1; step <= shapeCount; step++) {
            ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
            String expected = OCTANT_SHAPE_NAMES.get(step % shapeCount);
            helper.assertTrue(shapeOf(octant).equals(expected),
                    "alt scrolling " + step + " step(s) up from " + OCTANT_SHAPE_NAMES.get(0)
                            + " has to stand on " + expected + ", the octant stores " + shapeOf(octant));
        }

        // --- backwards below the first shape wraps around to the last one, and back ---
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(-1, false, false, true), player);
        helper.assertTrue(shapeOf(octant).equals(OCTANT_SHAPE_NAMES.get(shapeCount - 1)),
                "scrolling below the first shape did not wrap around to the last one, it stands on "
                        + shapeOf(octant));

        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
        helper.assertTrue(shapeOf(octant).equals(OCTANT_SHAPE_NAMES.get(0)),
                "scrolling forward from the last shape did not return to the first one");

        // --- the amount is a step count, not a direction: three at once skips two shapes ---
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(3, false, false, true), player);
        helper.assertTrue(shapeOf(octant).equals(OCTANT_SHAPE_NAMES.get(3)),
                "alt scrolling by three from " + OCTANT_SHAPE_NAMES.get(0) + " has to stand on "
                        + OCTANT_SHAPE_NAMES.get(3) + ", the octant stores " + shapeOf(octant));

        // --- a name that is already in the item has to be recognised, not silently reset ---
        String storedShape = OCTANT_SHAPE_NAMES.get(4);
        setShape(octant, storedShape);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
        helper.assertTrue(shapeOf(octant).equals(OCTANT_SHAPE_NAMES.get(5)),
                "an octant that carried " + storedShape + " scrolled to " + shapeOf(octant)
                        + " instead of " + OCTANT_SHAPE_NAMES.get(5) + "; the stored name was not "
                        + "understood, so every octant in an existing world loses its shape");

        // --- ctrl scroll moves corner 1 along the horizontal facing, in all four directions ---
        // Yaw 0 is south. It is asked first because it is the one a hard coded Direction.SOUTH
        // would also produce; the other three are what tells the two apart.
        assertCornerNudge(helper, player, octant, 0.0F, 0.0F, 2, new int[]{10, 20, 32},
                "facing south (yaw 0)");
        assertCornerNudge(helper, player, octant, 90.0F, 0.0F, 2, new int[]{8, 20, 30},
                "facing west (yaw 90)");
        assertCornerNudge(helper, player, octant, 180.0F, 0.0F, 2, new int[]{10, 20, 28},
                "facing north (yaw 180)");
        assertCornerNudge(helper, player, octant, -90.0F, 0.0F, 2, new int[]{12, 20, 30},
                "facing east (yaw -90)");

        // --- the pitch threshold, from both sides: 60 degrees is still horizontal, 61 is not ---
        assertCornerNudge(helper, player, octant, 0.0F, -60.0F, 2, new int[]{10, 20, 32},
                "looking up at exactly 60 degrees, which is still the horizontal branch");
        assertCornerNudge(helper, player, octant, 0.0F, -61.0F, 2, new int[]{10, 22, 30},
                "looking up at 61 degrees, one degree past the threshold");
        assertCornerNudge(helper, player, octant, 0.0F, 60.0F, 2, new int[]{10, 20, 32},
                "looking down at exactly 60 degrees, which is still the horizontal branch");
        assertCornerNudge(helper, player, octant, 0.0F, 61.0F, 2, new int[]{10, 18, 30},
                "looking down at 61 degrees, one degree past the threshold");

        // --- looking steeply up switches the axis to +Y, and shift addresses corner 2 ---
        setCorners(octant, new int[]{10, 20, 30}, new int[]{40, 50, 60});
        look(player, 0.0F, -70.0F);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(3, true, false, false), player);

        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos2"), new int[]{40, 53, 60}),
                "looking up did not move the second corner upwards");
        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos1"), new int[]{10, 20, 30}),
                "shift scrolling moved the first corner as well");

        // --- looking steeply down mirrors it ---
        setCorners(octant, new int[]{10, 20, 30}, new int[]{40, 50, 60});
        look(player, 0.0F, 70.0F);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(3, false, true, false), player);

        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos1"), new int[]{10, 17, 30}),
                "looking down did not move the first corner downwards");

        helper.succeed();
    }

    // =====================================================================================
    // MASTER BUILDER PICK
    // =====================================================================================

    /**
     * With Master Builder on a reinforced bundle the player pulls a block straight out of the
     * bundle into the hand. An occupied hand has to be parked in a free slot rather than
     * destroyed, an unknown item has to change nothing, and without the enchantment the bundle
     * must not hand anything out at all.
     *
     * <p>Every case looks at <em>both</em> sides of the move, the hand and the bundle, because a
     * pick is a transfer and a transfer can go wrong in two directions. Asking only where the
     * stone arrived cannot see that it also stayed where it was: dropping the two lines that
     * rewrite {@code BUNDLE_CONTENTS} turns the bundle into an infinite source of whatever it
     * holds, and the hand looks exactly the same afterwards. The counts are asserted for the same
     * reason - a parked stack that arrives as a single item instead of the five torches that were
     * in the hand still satisfies "the torches are somewhere in the inventory".
     *
     * <p>The last case is the branch that is otherwise never entered: a full inventory. There is
     * nowhere to park the hand, so the handler has to refuse the whole pick - keeping the hand
     * <em>and</em> leaving the bundle full. Turning that {@code return} into "park it in slot 0"
     * would destroy the stack in the hand, and every other case in this test would stay green.
     *
     * <p>What breaks this test: a pick that copies instead of moves, a parked stack that loses
     * its count, a miss that empties the bundle, a full inventory that costs the player the stack
     * in the hand, and an enchantment check that stops being asked.
     */
    public static void masterBuilderPickTakesBlocksOutOfTheEnchantedBundle(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.getInventory().clearContent();

        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        bundle.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        fill(helper, bundle, player, Items.STONE);

        player.getInventory().setItem(1, bundle);
        player.getInventory().setSelectedSlot(0);

        // --- empty hand: the block lands in the selected slot and leaves the bundle ---
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE),
                "the pick did not put the stone into the empty hand, hand holds " + player.getMainHandItem());
        helper.assertValueEqual(player.getMainHandItem().getCount(), BUNDLE_STONE,
                "stone in the hand after the pick - the whole bundle entry has to move");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                "stone left in the bundle after the pick; anything above zero means the pick "
                        + "copies instead of moving and the bundle is an infinite source");

        // --- occupied hand: the previous stack is parked, not lost, and not shrunk ---
        // The first pick took the bundle's only entry with it, so refill before asking again.
        // Overwriting the hand also gets rid of the stone from the round above, which keeps the
        // stone count below unambiguous.
        fill(helper, bundle, player, Items.STONE);
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 5));
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE),
                "the second pick did not reach the hand");
        helper.assertTrue(player.getInventory().contains(stack -> stack.is(Items.TORCH)),
                "the torches that were in the hand got destroyed by the pick");
        helper.assertValueEqual(countLoose(player, Items.TORCH), 5,
                "torches in the inventory after the hand stack was parked; the whole stack has "
                        + "to survive, not one item of it");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                "stone left in the bundle after the pick into an occupied hand");

        // --- an item the bundle does not hold must leave everything untouched ---
        fill(helper, bundle, player, Items.STONE);
        ItemStack before = player.getMainHandItem().copy();
        int stoneBefore = countInBundle(bundle, Items.STONE);
        int torchesBefore = countLoose(player, Items.TORCH);
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.OBSIDIAN)), player);
        helper.assertTrue(ItemStack.matches(before, player.getMainHandItem()),
                "picking an item the bundle does not contain changed the hand anyway");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), stoneBefore,
                "stone in the bundle after a pick for an item it does not hold");
        helper.assertValueEqual(countLoose(player, Items.TORCH), torchesBefore,
                "torches in the inventory after a pick for an item the bundle does not hold");

        // --- a full inventory: nowhere to park the hand, so nothing may happen at all ---
        player.getInventory().clearContent();
        ItemStack fullBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        fullBundle.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        fill(helper, fullBundle, player, Items.STONE);
        player.getInventory().setItem(1, fullBundle);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 5));
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 1));
            }
        }
        helper.assertValueEqual(player.getInventory().getFreeSlot(), -1,
                "test setup broken: first free inventory slot - the inventory is meant to be "
                        + "full, so that the pick has nowhere to park the stack in the hand");

        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().is(Items.TORCH)
                        && player.getMainHandItem().getCount() == 5,
                "a pick into a full inventory overwrote the stack in the hand, which now holds "
                        + player.getMainHandItem() + "; with no free slot the whole pick has to be "
                        + "refused instead");
        helper.assertValueEqual(countInBundle(fullBundle, Items.STONE), BUNDLE_STONE,
                "stone in the bundle after a pick that had nowhere to park the hand");

        // --- without the enchantment nothing may be pulled out at all ---
        player.getInventory().clearContent();
        ItemStack plainBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        fill(helper, plainBundle, player, Items.STONE);
        player.getInventory().setItem(1, plainBundle);
        player.getInventory().setSelectedSlot(0);
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "an unenchanted bundle handed out its content anyway");
        helper.assertValueEqual(countInBundle(plainBundle, Items.STONE), BUNDLE_STONE,
                "stone in an unenchanted bundle after a pick - it must not lose anything");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the
        // player list non-empty and the gametest server then stalls on shutdown - a failing
        // test would cost minutes of wall clock instead of seconds.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }


    private static void look(ServerPlayer player, float yRot, float xRot) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yRot, xRot);
    }

    /**
     * Puts both corners back where they started, points the player and ctrl scrolls once, then
     * checks where the first corner ended up - and that the second one did not move with it.
     * Absolute expectations on purpose: recomputing the target with the same
     * {@code getDirection()} the handler uses would agree with any direction it produced.
     */
    private static void assertCornerNudge(GameTestHelper helper, ServerPlayer player, ItemStack octant,
                                          float yRot, float xRot, int amount, int[] expectedCorner1,
                                          String what) {
        setCorners(octant, SCROLL_CORNER_1.clone(), SCROLL_CORNER_2.clone());
        look(player, yRot, xRot);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(amount, false, true, false), player);

        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos1"), expectedCorner1),
                what + ": ctrl scrolling by " + amount + " from " + Arrays.toString(SCROLL_CORNER_1)
                        + " has to end at " + Arrays.toString(expectedCorner1) + ", the corner is at "
                        + Arrays.toString(intArray(customData(octant), "Pos1")));
        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos2"), SCROLL_CORNER_2),
                what + ": ctrl scrolling moved the second corner as well, it is at "
                        + Arrays.toString(intArray(customData(octant), "Pos2")));
    }

    /** The selection shape the octant currently carries, or {@code ""} if it has none. */
    private static String shapeOf(ItemStack stack) {
        return customData(stack).getString("Shape").orElse("");
    }

    /** Writes a shape name into the item the way a previous session would have left it. */
    private static void setShape(ItemStack stack, String name) {
        CompoundTag nbt = customData(stack);
        nbt.putString("Shape", name);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    /**
     * Fills a reinforced bundle through the item's own public insert path, so this test stays
     * free of the {@code BundleContents} constructor differences between the two Minecraft lines.
     */
    private static void fill(GameTestHelper helper, ItemStack bundle, ServerPlayer player, Item item) {
        boolean inserted = ((ReinforcedBundleItem) bundle.getItem())
                .tryInsertStackFromWorld(bundle, new ItemStack(item, BUNDLE_STONE), player);
        helper.assertTrue(inserted, "test setup broken: the reinforced bundle refused the " + item);
        helper.assertValueEqual(countInBundle(bundle, item), BUNDLE_STONE,
                "test setup broken: what the bundle holds after being filled");
    }

    /** How many of {@code item} sit inside the bundle. */
    private static int countInBundle(ItemStack bundle, Item item) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : contents.itemCopyStream().toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** How many of {@code item} lie loose in the inventory - not counting anything in a bundle. */
    private static int countLoose(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static int[] intArray(CompoundTag nbt, String key) {
        return nbt.getIntArray(key).orElse(new int[0]);
    }

    private static void setCorners(ItemStack stack, int[] pos1, int[] pos2) {
        CompoundTag nbt = customData(stack);
        nbt.putIntArray("Pos1", pos1);
        nbt.putIntArray("Pos2", pos2);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
