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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 */
public final class NetworkHandlerTests {

    private NetworkHandlerTests() {
    }

    // =====================================================================================
    // DOUBLE JUMP
    // =====================================================================================

    /**
     * The air jump is granted by the boots, not by the packet: an unenchanted pair has to be
     * ignored completely, otherwise anyone could fly by spamming the payload. With the
     * enchantment the fall distance is cleared, which is what makes the second jump survivable.
     *
     * <p><strong>Not covered here:</strong> that the boots take a point of wear in survival.
     * The handler guards that branch with {@code !player.isCreative()}, and a gametest mock
     * player is permanently creative - {@code GameTestHelper}'s anonymous player subclass
     * overrides {@code gameMode()} to return {@code CREATIVE} unconditionally, so no amount of
     * {@code setGameMode} changes it. What can be checked is the other side of that guard: in
     * creative the boots must stay pristine, and that is asserted below. The wear itself needs
     * a real survival player and stays a manual check.
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

        finish(helper, player);
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

        finish(helper, player);
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

        finish(helper, player);
    }

    /** The octant screen sends its whole state in one payload; all of it has to land in the item. */
    public static void octantConfigureStoresTheWholeSelectionState(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);

        String shape = OctantItem.SelectionShape.values()[1].name();
        String fillOrder = OctantItem.FillOrder.values()[0].name();
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

        finish(helper, player);
    }

    /**
     * Scrolling on the octant either cycles the shape (with alt) or nudges one of the two
     * corners along the direction the player looks at. Steep pitch switches to up/down - the
     * part most likely to rot when the look direction API changes between versions.
     */
    public static void octantScrollCyclesShapesAndNudgesCornersByFacing(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);
        OctantItem.SelectionShape[] shapes = OctantItem.SelectionShape.values();

        // --- alt scroll cycles the shape, forwards and with a wrap around backwards ---
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
        helper.assertTrue(customData(octant).getString("Shape").orElse("").equals(shapes[1].name()),
                "alt scrolling did not advance the selection shape");

        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(-1, false, false, true), player);
        helper.assertTrue(customData(octant).getString("Shape").orElse("").equals(shapes[0].name()),
                "scrolling back did not return to the first shape");

        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(-1, false, false, true), player);
        helper.assertTrue(customData(octant).getString("Shape").orElse("").equals(shapes[shapes.length - 1].name()),
                "scrolling below the first shape did not wrap around to the last one");

        // --- ctrl scroll moves corner 1 along the horizontal facing (yaw 0 = south = +Z) ---
        setCorners(octant, new int[]{10, 20, 30}, new int[]{40, 50, 60});
        look(player, 0.0F, 0.0F);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(2, false, true, false), player);

        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos1"), new int[]{10, 20, 32}),
                "ctrl scrolling did not move the first corner two blocks south");
        helper.assertTrue(Arrays.equals(intArray(customData(octant), "Pos2"), new int[]{40, 50, 60}),
                "ctrl scrolling moved the second corner as well");

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

        finish(helper, player);
    }

    // =====================================================================================
    // MASTER BUILDER PICK
    // =====================================================================================

    /**
     * With Master Builder on a reinforced bundle the player pulls a block straight out of the
     * bundle into the hand. An occupied hand has to be parked in a free slot rather than
     * destroyed, an unknown item has to change nothing, and without the enchantment the bundle
     * must not hand anything out at all.
     */
    public static void masterBuilderPickTakesBlocksOutOfTheEnchantedBundle(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.getInventory().clearContent();

        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        bundle.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        // Filled through the item's own public insert path, so this test stays free of the
        // BundleContents constructor differences between the two Minecraft lines.
        boolean inserted = ((ReinforcedBundleItem) bundle.getItem())
                .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 16), player);
        helper.assertTrue(inserted, "the reinforced bundle refused to take the stone");

        player.getInventory().setItem(1, bundle);
        player.getInventory().setSelectedSlot(0);

        // --- empty hand: the block lands in the selected slot ---
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE),
                "the pick did not put the stone into the empty hand, hand holds " + player.getMainHandItem());

        // --- occupied hand: the previous stack is parked, not lost ---
        // The first pick took the bundle's only entry with it, so refill before asking again.
        ((ReinforcedBundleItem) bundle.getItem())
                .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 16), player);
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 5));
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE),
                "the second pick did not reach the hand");
        helper.assertTrue(player.getInventory().contains(stack -> stack.is(Items.TORCH)),
                "the torches that were in the hand got destroyed by the pick");

        // --- an item the bundle does not hold must leave everything untouched ---
        ItemStack before = player.getMainHandItem().copy();
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.OBSIDIAN)), player);
        helper.assertTrue(ItemStack.matches(before, player.getMainHandItem()),
                "picking an item the bundle does not contain changed the hand anyway");

        // --- without the enchantment nothing may be pulled out at all ---
        player.getInventory().clearContent();
        ItemStack plainBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ((ReinforcedBundleItem) plainBundle.getItem())
                .tryInsertStackFromWorld(plainBundle, new ItemStack(Items.STONE, 16), player);
        player.getInventory().setItem(1, plainBundle);
        player.getInventory().setSelectedSlot(0);
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "an unenchanted bundle handed out its content anyway");

        finish(helper, player);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.create(helper);
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    /** Hands the mock player back and reports success; see {@link #mockPlayer}. */
    private static void finish(GameTestHelper helper, ServerPlayer player) {
        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    private static void look(ServerPlayer player, float yRot, float xRot) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yRot, xRot);
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
