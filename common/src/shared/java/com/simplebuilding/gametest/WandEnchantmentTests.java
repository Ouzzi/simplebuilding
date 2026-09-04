package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.util.ModTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What is left of the building enchantments once the behaviour tests around them are counted:
 * Master Builder, the one place Fast Chiseling can hit a floor, the item tags that decide which
 * tool may carry any of them at all, and the chests and merchants the books come out of.
 *
 * <p>Master Builder is the reason this file exists. It is the most expensive of the building
 * enchantments (anvil cost 8, weight 1) and it is the only one that changes <em>where</em> the
 * building wand is allowed to look for material: without it the wand sees the off hand and the
 * nine hotbar slots, with it the whole backpack and every reinforced bundle inside it. Two
 * separate methods implement that rule - {@code findFirstBuildingBlock}/{@code findSpecificMaterial}
 * for what the server places, {@code findFirstBlockStateClient}/{@code findAllBuildingBlocks} for
 * what the highlight draws - and they can drift apart without anything failing, because until now
 * nothing put Master Builder on a wand at all.
 *
 * <p>The other three methods are about reachability rather than behaviour. An enchantment that
 * works perfectly is still dead if its {@code supported_items} tag does not name the tool that
 * reads it, or if no chest and no merchant ever hands out the book.
 *
 * <p><strong>What this file deliberately leaves to its neighbours</strong> - the four building
 * enchantments already have behaviour tests and this file does not repeat them:
 * {@code BuildingEnchantmentTests} owns Constructor's Touch on the chisel and on the stick, Fast
 * Chiseling's per tier cooldown staircase and mining bonus, both halves of Color Palette, and the
 * preview's off hand-before-hotbar order; {@code BuildingWandTests} owns the wand itself - its tier
 * diameters, its axis modes, the upward radius cap in {@code inventoryTick}, its behaviour when it
 * leaves the hand, and Master Builder opening the backpack for a <em>loose stack</em>;
 * {@code BundleWiringTests} owns the <em>placement</em> half of the either-or rule that lets a
 * bundle be a material source, and the anvil's refusal to combine Color Palette without Master
 * Builder - the preview half of that same rule is a second implementation and is owned here, by
 * {@link #masterBuilderMovesThePreviewSourcesTheSameWayItMovesThePlacement};
 * {@code ReinforcedBundleTests} owns Master Builder placing straight out of a bundle;
 * {@code MagnetTests}, {@code OreDetectorTests} and {@code QuiverTests} each own Constructor's
 * Touch on their own tool; {@code MiningEnchantmentTests} owns the {@code in_enchanting_table}
 * tag and the mining books' loot pools; {@code ConfigOptionTests} owns the config switch that
 * turns the loot pools off.
 *
 * <p><strong>Not covered</strong>, because a gametest server has no client:
 * <ul>
 *   <li>the wand's preview <em>rendering</em> ({@code BuildingWandPreviewRenderer}) - only the
 *       server side {@code getPreviewStates} it draws from is exercised here;</li>
 *   <li>{@code BlockHighlightRenderer}'s Constructor's Touch flip of the highlight fill, and
 *       {@code EnchantmentModelProperty}'s per enchantment model swap;</li>
 *   <li>the anvil, chest and merchant <em>screens</em>; the placement sounds the wand plays and
 *       the overlay messages the magnet filter writes.</li>
 * </ul>
 *
 * <p><strong>Known defects</strong> found while writing this file. None of them is asserted -
 * pinning them down would freeze them - and each one is named here so the next reader does not
 * have to find it again:
 * <ul>
 *   <li><strong>The enderite chisel and all six spatulas cannot be enchanted.</strong>
 *       {@code simplebuilding:chisel_tools} ({@code ModItemTagProvider}) lists the stone, copper,
 *       iron, gold, diamond and netherite chisel and nothing else, while
 *       {@code ModItems.ENDERITE_CHISEL} and {@code STONE_SPATULA} through {@code NETHERITE_SPATULA}
 *       are {@code ChiselItem}s too. Both Fast Chiseling and Constructor's Touch hang on that tag
 *       (the second through {@code constructors_touch_enchantable}, which nests it), and both are
 *       read by {@code ChiselItem} for every instance - so the spatula's whole reverse
 *       transformation table and the enderite chisel's cooldown discount are live code that no
 *       anvil can ever switch on.</li>
 *   <li><strong>Bridge has no source at all.</strong> {@code ModLootTableModifications} adds books
 *       for every other building enchantment - Cover out of two chests, Color Palette out of two,
 *       Linear out of two, Master Builder out of four (three code blocks, the last of which matches
 *       both the rare and the ominous trial chamber vault) - and mentions
 *       {@code simplebuilding:bridge} nowhere, and no
 *       {@code weighted_enchant} pool under {@code data/simplebuilding/villager_trade/} names it
 *       either. {@code EnchantmentEffectTests#coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown}
 *       already records that Bridge does nothing; it is also unobtainable.</li>
 *   <li><strong>The three readers of {@code SettingsRadius} have drifted.</strong>
 *       {@code getConfiguredRadius} (the preview) clamps a negative radius to 0,
 *       {@code inventoryTick} (the placement) only caps upwards and ends up building the centre
 *       block anyway, and {@code getBuildingPositions} - the list the highlight is drawn from -
 *       returns nothing at all. {@code BuildingWandTests} records the visible half of that.
 *       {@link #masterBuilderMovesThePreviewSourcesTheSameWayItMovesThePlacement} asserts only the
 *       reader that behaves, {@code getConfiguredRadius}, so nothing here freezes the other two.</li>
 * </ul>
 */
public final class WandEnchantmentTests {

    private WandEnchantmentTests() {
    }

    /** Inventory slot well inside the backpack range (9-35) that only Master Builder opens up. */
    private static final int BACKPACK_SLOT = 20;

    /** The block whose top face every wand run in this file is armed on. */
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    /** The block every chisel case in this file is run on, far away from {@link #ANCHOR}. */
    private static final BlockPos CHISEL_TARGET = new BlockPos(6, 1, 6);

    /** How many blocks a wand pinned to radius 1 fills: the 3x3 plane above the clicked face. */
    private static final int WAND_PLANE_BLOCKS = 9;

    /**
     * The building enchantment books this file answers for. The set is closed on purpose: every
     * table below is held to exactly the books out of this set that it declares, and everything
     * outside it - the mining books, Range, Deep Pockets, Funnel, Double Jump - shares the same
     * tables and belongs to {@code MiningEnchantmentTests}. Bridge is listed although nothing hands
     * it out, so that giving it a chest one day fails here instead of passing unnoticed.
     */
    private static final Set<Identifier> BUILDING_BOOKS = Set.of(
            ModEnchantments.CONSTRUCTORS_TOUCH.identifier(),
            ModEnchantments.FAST_CHISELING.identifier(),
            ModEnchantments.COLOR_PALETTE.identifier(),
            ModEnchantments.COVER.identifier(),
            ModEnchantments.LINEAR.identifier(),
            ModEnchantments.BRIDGE.identifier(),
            ModEnchantments.MASTER_BUILDER.identifier());

    /** The component key the enchanted book loot entries are written under. */
    private static final String STORED_ENCHANTMENTS_KEY = "minecraft:stored_enchantments";

    /** Upper bound for the wand tick loop, so a wand that never finishes fails instead of hanging. */
    private static final int WAND_TICK_CAP = 60;

    /** The same for the cooldown drain loop. */
    private static final int COOLDOWN_TICK_CAP = 400;

    /**
     * Trade rolls per merchant book trade. The draw is deterministic per seed, so this is not a
     * sample size in the statistical sense - it only has to be large enough that every declared
     * pool entry is reached. The thinnest pool share below is 25 of 85; missing it 120 times in a
     * row has probability 0.71^120, about 1e-18.
     */
    private static final int TRADE_ROLLS = 120;

    // =====================================================================================
    // MASTER BUILDER - THE PLACEMENT
    // =====================================================================================

    /**
     * Master Builder is the wand's "reach into the backpack" enchantment, and it is the only
     * reason {@code BuildingWandItem#findFirstBuildingBlock} has a third search step at all. The
     * first two steps - off hand, then hotbar slots 0 to 8 - run for every wand; the loop over
     * slots 9 to 35 sits behind {@code if (hasMasterBuilder)}, and so does the whole nested
     * bundle branch when the bundle itself is unenchanted.
     *
     * <p>That the enchantment opens the backpack <em>for a loose stack</em> is
     * {@code BuildingWandTests#materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack}'s
     * subject and is not repeated here. What is left, and what nothing covers, is where the new
     * step sits in the order and what happens when the thing standing in the backpack is a bundle:
     * <ul>
     *   <li><strong>The order is not reordered.</strong> Off hand stone against backpack glass,
     *       Master Builder on the wand: the plane comes out stone and the backpack is untouched.
     *       The enchantment appends a step to the end of the search, it does not promote the
     *       backpack over the hands.</li>
     *   <li><strong>The hotbar keeps its place too.</strong> Hotbar planks against backpack glass,
     *       off hand empty: planks, and again nothing is taken out of the backpack. Both of these
     *       are runs the plain wand tests cannot make - without the enchantment the backpack
     *       cannot lose the race, because it never enters it.</li>
     *   <li><strong>A bundle in the backpack is locked.</strong> An <em>unenchanted</em> reinforced
     *       bundle in slot {@value #BACKPACK_SLOT} is the only material in the game and a plain
     *       wand refuses the click outright with {@code FAIL} - the
     *       {@code preview == null && !instabuild} early return - without touching its contents.</li>
     *   <li><strong>And opens for a Master Builder wand.</strong> The same bundle, the same slot:
     *       the full 3x3 appears, exactly nine stone leave the bundle, and none of it is pulled
     *       out into the inventory on the way.</li>
     * </ul>
     *
     * <p>The last pair does not separate the two gates it passes - reaching the bundle needs the
     * enchantment for the slot range <em>and</em> again for the bundle branch, and one wand carries
     * both. That is deliberate: {@code BundleWiringTests#buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock}
     * already separates the bundle gate on its own, with the bundle in the hotbar. What this pair
     * adds is the combination, which is where a guard that was tightened from "or" to "and" would
     * first show up.
     *
     * <p>The player is stripped of {@code instabuild} rather than of creative mode: the gametest
     * mock overrides {@code gameMode()} unconditionally, and {@code instabuild} is the flag every
     * branch here actually reads - see {@code ConsumptionAndDurabilityTests}.
     *
     * <p><strong>What breaks this test:</strong> moving the slots 9 to 35 loop ahead of the off
     * hand or ahead of the hotbar (runs one and two flip), dropping the {@code hasMasterBuilder}
     * guard in front of that loop (run three stops failing), losing the bundle branch or turning
     * its wand-or-bundle condition into wand-and-bundle (run four refuses the click), taking more
     * or fewer than one item out of the bundle per block, and pulling the bundle's contents into
     * the inventory instead of placing straight out of it.
     */
    public static void masterBuilderOpensTheBackpackAndTheBundlesInsideItToTheWand(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- 1. the off hand still comes first ---
        ItemStack offHandWand = masterBuilderWand(helper);
        armWand(player, offHandWand);
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.GLASS, 32));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 32));

        runWand(helper, player, offHandWand);
        assertPlaneOf(helper, Blocks.STONE,
                "Master Builder promoted the backpack ahead of the off hand");
        helper.assertValueEqual(player.getInventory().getItem(BACKPACK_SLOT).getCount(), 32,
                "the backpack paid for a plane the off hand had material for");

        // --- 2. and so does the hotbar ---
        ItemStack hotbarWand = masterBuilderWand(helper);
        armWand(player, hotbarWand);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.GLASS, 32));

        runWand(helper, player, hotbarWand);
        assertPlaneOf(helper, Blocks.OAK_PLANKS,
                "Master Builder promoted the backpack ahead of the hotbar");
        helper.assertValueEqual(player.getInventory().getItem(BACKPACK_SLOT).getCount(), 32,
                "the backpack paid for a plane the hotbar had material for");

        // --- 3. a plain bundle in the backpack is invisible to a plain wand ---
        ItemStack plainWand = radiusOneWand();
        armWand(player, plainWand);
        ItemStack lockedBundle = bundleOfStone(player, 64);
        player.getInventory().setItem(BACKPACK_SLOT, lockedBundle);

        InteractionResult refused = armOnAnchor(helper, player, plainWand);
        helper.assertTrue(refused == InteractionResult.FAIL,
                "a wand without Master Builder accepted the click while its only material sat in a "
                        + "bundle in inventory slot " + BACKPACK_SLOT + "; it returned " + refused);
        helper.assertFalse(wandIsActive(plainWand),
                "the refused click still switched the wand on, so it will build on the next tick");
        helper.assertValueEqual(placedBlocks(helper).size(), 0,
                "a wand without Master Builder built out of the backpack anyway");
        helper.assertValueEqual(countInBundle(lockedBundle, Items.STONE), 64,
                "the refused click still spent stone out of the bundle");

        // --- 4. and the same bundle feeds a Master Builder wand, one piece per block ---
        ItemStack bundleWand = masterBuilderWand(helper);
        armWand(player, bundleWand);
        ItemStack bundle = bundleOfStone(player, 64);
        player.getInventory().setItem(BACKPACK_SLOT, bundle);

        runWand(helper, player, bundleWand);
        assertPlaneOf(helper, Blocks.STONE,
                "Master Builder did not reach a plain bundle sitting in the backpack");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 64 - WAND_PLANE_BLOCKS,
                "the run did not take exactly one stone out of the bundle per placed block");
        helper.assertValueEqual(looseCount(player, Items.STONE), 0,
                "the wand emptied the bundle into the inventory instead of placing straight out of it");

        helper.succeed();
    }

    // =====================================================================================
    // MASTER BUILDER - THE PREVIEW
    // =====================================================================================

    /**
     * {@code BuildingWandItem#getPreviewStates} is the second, independent implementation of the
     * same material search: {@code findFirstBlockStateClient} repeats the off hand / hotbar /
     * backpack walk, and {@code findAllBuildingBlocks} repeats it once more for the Color Palette
     * branch, this time as {@code limit = hasMasterBuilder ? inventory size : 9}. Neither had ever
     * been called with Master Builder on the wand, so the enchantment could have been dropped from
     * either of them and the only symptom would have been a highlight that promises nothing while
     * the wand builds, or promises a plane the wand then refuses.
     *
     * <p>Only the third step is the subject here. That the off hand comes before the <em>hotbar</em>
     * in both preview branches is
     * {@code BuildingEnchantmentTests#colorPaletteSpreadsTheCarriedBlocksOverTheWandPreview}'s last
     * block; what this test adds is the off hand against the <em>backpack</em>, which no wand can
     * even be asked without the enchantment.
     *
     * <p>Both search copies end in the same either-or gate: {@code checkStackIsBlockState} and
     * {@code collectBlocksFromStack} each open a reinforced bundle when <em>the wand or the
     * bundle</em> carries Master Builder. The placement's copy of that gate is
     * {@code BundleWiringTests#buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock}'s subject;
     * the preview's two copies had no caller anywhere in the tree with a bundle in the inventory,
     * so either branch could be deleted with every test still green and the highlight going blank
     * over a wand that keeps building. Both halves of the or are therefore driven separately, once
     * per search copy: a plain wand with a plain bundle previews nothing, a plain wand with an
     * <em>enchanted</em> bundle previews the full plane, and a Master Builder wand reaches a plain
     * bundle. The first two put the bundle in the hotbar on purpose - inside the hotbar range the
     * slot gate is out of the way, so the bundle gate alone decides - while the third puts it in
     * the backpack, where the wand's enchantment has to pass both gates in a row.
     *
     * <p>The preview is a read: after every one of those runs the bundle still holds its 64 stone.
     * A preview that reached its material through the placement path would empty it.
     *
     * <p>The last two blocks of the test are the part that ties the two implementations together:
     * the same wand, the same inventory and the same clicked face are run through the preview and
     * then through a real build, and the placed blocks are compared against the previewed states
     * position by position - once with a loose stack in the backpack and once with the stack inside
     * a bundle there, which is the arrangement in which the two searches reach their material
     * through different branches of their own copies of the gate. The bundle run also counts what
     * left the bundle afterwards, so a preview that happened to name the right block while the wand
     * paid out of something else still fails. Because {@code getPreviewStates} takes and returns
     * <em>absolute</em> positions, the comparison is done in absolute coordinates against
     * {@code helper.getLevel().getBlockState}; converting back through {@code relativePos} would
     * be wrong, since that method mirrors instead of inverting {@code absolutePos}.
     *
     * <p>The radius cases pass {@code maxDiameter} straight into the method (3, i.e. a tier radius
     * of 1) instead of reading a wand tier, so they state what {@code getConfiguredRadius} does
     * with the stored setting and stay independent of how the tiers are balanced: a stored radius
     * of 99 has to come back as the tier's 3x3, a stored -1 as the single centre block, and no
     * stored value at all as the tier maximum.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code hasMasterBuilder} argument or
     * the third loop in {@code findFirstBlockStateClient}, replacing the
     * {@code hasMasterBuilder ? size : 9} limit in {@code findAllBuildingBlocks} with a constant
     * (either direction is caught: one by the plain Color Palette run, the other by the enchanted
     * one), losing either clamp in {@code getConfiguredRadius}, deleting the reinforced bundle
     * branch out of {@code checkStackIsBlockState} or out of {@code collectBlocksFromStack} or
     * narrowing either of them from wand-or-bundle to wand-and-bundle (the bundle runs and the
     * second cross check go red), a preview that spends the bundle's contents while it draws, and
     * any change that makes the preview and the placement disagree about what goes where.
     */
    public static void masterBuilderMovesThePreviewSourcesTheSameWayItMovesThePlacement(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(ANCHOR);

        // --- the plain preview stops at the hotbar ---
        ItemStack plainWand = radiusOneWand();
        armWand(player, plainWand);
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.GLASS, 32));

        Map<BlockPos, BlockState> lockedOut = BuildingWandItem.getPreviewStates(
                level, player, plainWand, origin, Direction.UP, 3);
        helper.assertTrue(lockedOut.isEmpty(),
                "a wand without Master Builder previewed " + lockedOut.size() + " blocks out of "
                        + "inventory slot " + BACKPACK_SLOT + ", which it is not allowed to build from");

        // --- with the enchantment it reaches the same block the placement would ---
        ItemStack builderWand = masterBuilderWand(helper);
        armWand(player, builderWand);
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.GLASS, 32));

        Map<BlockPos, BlockState> opened = BuildingWandItem.getPreviewStates(
                level, player, builderWand, origin, Direction.UP, 3);
        helper.assertValueEqual(opened.size(), WAND_PLANE_BLOCKS,
                "Master Builder did not open the backpack to the preview");
        helper.assertValueEqual(distinctBlocks(opened), Set.of(Blocks.GLASS),
                "the Master Builder preview drew something other than the backpack's glass");

        // --- the new step goes to the end of the search, not to the front of it ---
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 32));
        Map<BlockPos, BlockState> offHandFirst = BuildingWandItem.getPreviewStates(
                level, player, builderWand, origin, Direction.UP, 3);
        helper.assertValueEqual(distinctBlocks(offHandFirst), Set.of(Blocks.STONE),
                "the preview drew the backpack's glass although the off hand held stone; Master "
                        + "Builder promoted the backpack over the hands in findFirstBlockStateClient");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        // --- neither side carries it: a bundle is not a preview source either ---
        // Hotbar slot 1, not the backpack: there the slot range gate is already open, so what is
        // being asserted is the bundle gate itself and not the loop it sits in.
        ItemStack plainBundleWand = radiusOneWand();
        armWand(player, plainBundleWand);
        ItemStack lockedBundle = bundleOfStone(player, 64);
        player.getInventory().setItem(1, lockedBundle);

        Map<BlockPos, BlockState> lockedPreview = BuildingWandItem.getPreviewStates(
                level, player, plainBundleWand, origin, Direction.UP, 3);
        helper.assertTrue(lockedPreview.isEmpty(),
                "a plain wand previewed " + lockedPreview.size() + " blocks out of a plain bundle in "
                        + "hotbar slot 1; with the enchantment on neither side the highlight has to "
                        + "stay empty, the way the click is refused");

        // --- the bundle carries it instead of the wand: the other half of the or ---
        ItemStack ownBundle = masterBuilderBundle(helper, player, 64);
        armWand(player, plainBundleWand);
        player.getInventory().setItem(1, ownBundle);

        Map<BlockPos, BlockState> fromOwnBundle = BuildingWandItem.getPreviewStates(
                level, player, plainBundleWand, origin, Direction.UP, 3);
        helper.assertValueEqual(fromOwnBundle.size(), WAND_PLANE_BLOCKS,
                "a Master Builder bundle did not open itself to a plain wand's preview, although the "
                        + "same pair builds; findFirstBlockStateClient lost the bundle's half of the or");
        helper.assertValueEqual(distinctBlocks(fromOwnBundle), Set.of(Blocks.STONE),
                "the preview drew something other than the stone inside the bundle");
        helper.assertValueEqual(countInBundle(ownBundle, Items.STONE), 64,
                "drawing the preview took stone out of the bundle; the highlight is a read");

        // --- and the wand's own enchantment reaches a plain bundle, past the slot gate as well ---
        ItemStack bundleReachWand = masterBuilderWand(helper);
        armWand(player, bundleReachWand);
        ItemStack backpackBundle = bundleOfStone(player, 64);
        player.getInventory().setItem(BACKPACK_SLOT, backpackBundle);

        Map<BlockPos, BlockState> fromBackpackBundle = BuildingWandItem.getPreviewStates(
                level, player, bundleReachWand, origin, Direction.UP, 3);
        helper.assertValueEqual(fromBackpackBundle.size(), WAND_PLANE_BLOCKS,
                "Master Builder did not reach a plain bundle in inventory slot " + BACKPACK_SLOT
                        + " through the preview, although the same wand builds out of it");
        helper.assertValueEqual(distinctBlocks(fromBackpackBundle), Set.of(Blocks.STONE),
                "the preview drew something other than the stone inside the backpack's bundle");
        helper.assertValueEqual(countInBundle(backpackBundle, Items.STONE), 64,
                "drawing the preview took stone out of the backpack's bundle");

        // --- Color Palette builds its palette from the same range ---
        ItemStack paletteWand = radiusOneWand();
        paletteWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        armWand(player, paletteWand);
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.OAK_PLANKS, 32));

        Map<BlockPos, BlockState> emptyPalette = BuildingWandItem.getPreviewStates(
                level, player, paletteWand, origin, Direction.UP, 3);
        helper.assertTrue(emptyPalette.isEmpty(),
                "Color Palette alone collected " + emptyPalette.size() + " blocks out of the backpack; "
                        + "its palette is supposed to stop at the hotbar without Master Builder");

        ItemStack paletteBuilderWand = radiusOneWand();
        paletteBuilderWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        paletteBuilderWand.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        armWand(player, paletteBuilderWand);
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.OAK_PLANKS, 32));

        Map<BlockPos, BlockState> fullPalette = BuildingWandItem.getPreviewStates(
                level, player, paletteBuilderWand, origin, Direction.UP, 3);
        helper.assertValueEqual(fullPalette.size(), WAND_PLANE_BLOCKS,
                "Master Builder did not widen the Color Palette palette to the backpack");
        helper.assertValueEqual(distinctBlocks(fullPalette), Set.of(Blocks.OAK_PLANKS),
                "the widened palette drew something the player is not carrying");

        // --- the palette reads bundles through its own copy of the same gate ---
        ItemStack palettePlainBundleWand = radiusOneWand();
        palettePlainBundleWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        armWand(player, palettePlainBundleWand);
        player.getInventory().setItem(1, bundleOfStone(player, 64));

        Map<BlockPos, BlockState> lockedBundlePalette = BuildingWandItem.getPreviewStates(
                level, player, palettePlainBundleWand, origin, Direction.UP, 3);
        helper.assertTrue(lockedBundlePalette.isEmpty(),
                "Color Palette collected " + lockedBundlePalette.size() + " blocks out of a bundle "
                        + "neither it nor the wand may open");

        ItemStack paletteOwnBundleWand = radiusOneWand();
        paletteOwnBundleWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        armWand(player, paletteOwnBundleWand);
        player.getInventory().setItem(1, masterBuilderBundle(helper, player, 64));

        Map<BlockPos, BlockState> ownBundlePalette = BuildingWandItem.getPreviewStates(
                level, player, paletteOwnBundleWand, origin, Direction.UP, 3);
        helper.assertValueEqual(ownBundlePalette.size(), WAND_PLANE_BLOCKS,
                "a Master Builder bundle in the hotbar contributed nothing to the palette, so "
                        + "collectBlocksFromStack lost the bundle's half of the or");
        helper.assertValueEqual(distinctBlocks(ownBundlePalette), Set.of(Blocks.STONE),
                "the palette drew something other than the stone inside the bundle");

        ItemStack paletteBuilderBundleWand = radiusOneWand();
        paletteBuilderBundleWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        paletteBuilderBundleWand.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        armWand(player, paletteBuilderBundleWand);
        player.getInventory().setItem(BACKPACK_SLOT, bundleOfStone(player, 64));

        Map<BlockPos, BlockState> backpackBundlePalette = BuildingWandItem.getPreviewStates(
                level, player, paletteBuilderBundleWand, origin, Direction.UP, 3);
        helper.assertValueEqual(backpackBundlePalette.size(), WAND_PLANE_BLOCKS,
                "the widened palette did not open a plain bundle in inventory slot " + BACKPACK_SLOT
                        + ", although the wand carries the enchantment the bundle lacks");
        helper.assertValueEqual(distinctBlocks(backpackBundlePalette), Set.of(Blocks.STONE),
                "the widened palette drew something other than the bundle's stone");

        // --- the stored radius, clamped at both ends ---
        armWand(player, builderWand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 32));

        helper.assertValueEqual(previewSize(helper, player, withRadius(builderWand, 99), origin), WAND_PLANE_BLOCKS,
                "a stored radius of 99 was not capped at the tier maximum of 1");
        helper.assertValueEqual(previewSize(helper, player, withRadius(builderWand, -1), origin), 1,
                "a stored radius of -1 did not fall back to the single centre block");
        helper.assertValueEqual(previewSize(helper, player, withoutRadius(builderWand), origin), WAND_PLANE_BLOCKS,
                "a wand with no stored radius did not fall back to the tier maximum");

        // --- and finally: what the preview promised is what the wand places ---
        ItemStack crossCheckWand = masterBuilderWand(helper);
        armWand(player, crossCheckWand);
        player.getInventory().setItem(BACKPACK_SLOT, new ItemStack(Items.GLASS, 32));

        int diameter = ((BuildingWandItem) crossCheckWand.getItem()).getWandSquareDiameter();
        Map<BlockPos, BlockState> promised = BuildingWandItem.getPreviewStates(
                level, player, crossCheckWand, origin, Direction.UP, diameter);
        helper.assertValueEqual(promised.size(), WAND_PLANE_BLOCKS,
                "the cross checked preview is not the 3x3 the wand's own radius setting asks for");

        runWand(helper, player, crossCheckWand);
        assertPreviewWasKept(helper, level, promised);

        // --- and once more with the stack inside a bundle, where the two searches split up ---
        ItemStack bundleCrossCheckWand = masterBuilderWand(helper);
        armWand(player, bundleCrossCheckWand);
        ItemStack crossCheckBundle = bundleOfStone(player, 64);
        player.getInventory().setItem(BACKPACK_SLOT, crossCheckBundle);

        Map<BlockPos, BlockState> promisedFromBundle = BuildingWandItem.getPreviewStates(
                level, player, bundleCrossCheckWand, origin, Direction.UP, diameter);
        helper.assertValueEqual(promisedFromBundle.size(), WAND_PLANE_BLOCKS,
                "the preview drew " + promisedFromBundle.size() + " blocks for a wand that builds the "
                        + "full plane out of the bundle in the backpack; the two searches have drifted "
                        + "apart over the bundle branch");
        helper.assertValueEqual(distinctBlocks(promisedFromBundle), Set.of(Blocks.STONE),
                "the preview promised something other than the stone the wand is about to place");

        runWand(helper, player, bundleCrossCheckWand);
        assertPreviewWasKept(helper, level, promisedFromBundle);
        helper.assertValueEqual(countInBundle(crossCheckBundle, Items.STONE), 64 - WAND_PLANE_BLOCKS,
                "the wand did not pay for the promised plane out of the bundle the preview read it "
                        + "from, so the two found their material in different places");

        helper.succeed();
    }

    // =====================================================================================
    // FAST CHISELING - THE FLOOR
    // =====================================================================================

    /**
     * Fast Chiseling multiplies the tier's cooldown by {@code 1 - 0.3 * level} and truncates the
     * result to an {@code int}, with a {@code Math.max(1, ...)} underneath. On the cheap tiers
     * that product gets small enough that the truncation, not the percentage, decides the answer,
     * and this is the one case in the whole formula where a plausible looking edit produces a
     * chisel with <em>no</em> cooldown at all.
     *
     * <p>The netherite chisel is the tool that gets there: its base is the shortest in the game,
     * and Fast Chiseling II leaves it at exactly one tick. That one is not the {@code Math.max}
     * floor, it is arithmetic - {@code 1.0f - 2 * 0.3f} is 0.39999998, not 0.4, so
     * {@code (int)(5 * 0.39999998f)} truncates to 1 rather than to 2. Which is worth writing down:
     * a "cleanup" that rounds instead of truncating, or that computes the factor in
     * {@code double}, moves this number.
     *
     * <p>The floor itself is only reachable above the enchantment's own cap, so the test says so
     * out loud: it first asserts that the cap is 2 - if a later balance pass raises it, level 3
     * stops being an out of range value and this whole method has to be re-read - and then writes
     * level 3 onto the stack directly, the way a command or a datapack can. The factor is 0.099999
     * there, so every tier from diamond down would truncate to 0 without the guard, and a chisel
     * whose cooldown is 0 is a chisel that can be spammed at 20 transformations a second.
     *
     * <p>All numbers are drained off {@code ItemCooldowns} one tick at a time rather than read
     * back out of the formula, so a tier that stops calling {@code addCooldown} at all fails here
     * instead of passing on its getter.
     *
     * <p><strong>What breaks this test:</strong> deleting the {@code Math.max(1, ...)} guard (the
     * level 3 case measures 0 ticks, or finds no cooldown at all), retuning the {@code 0.3f} step,
     * moving the factor to {@code double} or rounding it, retuning the netherite chisel's base
     * cooldown, and raising Fast Chiseling's level cap without revisiting any of this.
     */
    public static void fastChiselingNeverPushesTheChiselCooldownBelowOneTick(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        ChiselItem chisel = ModItems.NETHERITE_CHISEL;
        helper.assertValueEqual(chisel.getCooldownTicks(), 5,
                "the netherite chisel's base cooldown was retuned; it is the shortest in the game and "
                        + "the truncation numbers below are derived from it");

        helper.assertValueEqual(measureCooldown(helper, player, new ItemStack(chisel)), 5,
                "an unenchanted netherite chisel did not wait out its full cooldown");
        helper.assertValueEqual(
                measureCooldown(helper, player, enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 1)), 3,
                "Fast Chiseling I on the netherite chisel: (int)(5 * 0.7f) is 3");
        helper.assertValueEqual(
                measureCooldown(helper, player, enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2)), 1,
                "Fast Chiseling II on the netherite chisel: (int)(5 * 0.39999998f) truncates to 1, "
                        + "not to 2 - if this is 2 now, the factor stopped being computed in float");

        // --- the guard below the formula, reachable only past the level cap ---
        int cap = enchantment(helper, ModEnchantments.FAST_CHISELING).value().getMaxLevel();
        helper.assertValueEqual(cap, 2,
                "Fast Chiseling's level cap moved. Level 3 below was chosen because no anvil and no "
                        + "enchanting table can produce it; re-read this test before trusting it again.");

        for (ChiselItem tier : List.of(ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL)) {
            int measured = measureCooldown(helper, player,
                    enchanted(helper, tier, ModEnchantments.FAST_CHISELING, cap + 1));
            helper.assertValueEqual(measured, 1,
                    "a level " + (cap + 1) + " Fast Chiseling " + tier.getCooldownTicks()
                            + " tick chisel came out at " + measured + " ticks; the factor is 0.099999 "
                            + "at that level, so without Math.max(1, ...) the tool has no cooldown at all");
        }

        helper.succeed();
    }

    // =====================================================================================
    // THE TAGS THAT DECIDE WHERE THE ENCHANTMENTS MAY GO
    // =====================================================================================

    /**
     * Every building enchantment is only worth as much as its {@code supported_items} tag lets it
     * be. Fast Chiseling hangs on {@code simplebuilding:chisel_tools}, Constructor's Touch on
     * {@code simplebuilding:constructors_touch_enchantable} - and the second of those is a nest of
     * five item tags plus seven loose items that has never been asserted anywhere.
     *
     * <p>The list below is not "some items that happen to be in the tag". It is exactly the set of
     * items whose <em>code</em> reads Constructor's Touch: {@code ChiselItem} (the second
     * transformation table), {@code SledgehammerItem} (the reverse swing and the extra layer),
     * {@code MagnetItem} (the doubled reach), {@code OreDetectorItem} (the halved step cost),
     * {@code QuiverItem} (arrows out of the backpack) and {@code ConstructorsTouchInteraction}
     * (the plain stick that cycles block states). If one of those falls out of the tag, the
     * feature behind it becomes dead code that its own behaviour test - which enchants the stack
     * directly and never asks the anvil - keeps reporting as working.
     *
     * <p>Both directions are asserted. A diamond pickaxe is in neither tag and has to be refused
     * by both, otherwise a tag that had grown into a wildcard would make every positive answer
     * meaningless. The building wand is the second control: it is in
     * {@code constructors_touch_enchantable} through the nested wand tag but is not a chisel, so
     * it separates the two tags from each other - if Fast Chiseling ever accepted it, the two
     * would have been merged.
     *
     * <p>{@code chisel_tools} is checked twice, once through {@code ItemStack#is} and once through
     * {@code Enchantment#canEnchant}, so a tag that lost its content is separable from an
     * enchantment definition that was pointed at a different tag.
     *
     * <p>The enderite chisel and the six spatulas are deliberately absent from both directions -
     * they are {@code ChiselItem}s that cannot be enchanted at all, which is the known defect in
     * the class javadoc. Asserting either state would freeze it.
     *
     * <p><strong>What breaks this test:</strong> an item dropped from {@code chisel_tools} or from
     * {@code constructors_touch_enchantable}, a nested tag reference lost from the second (the
     * chisels, the sledgehammers or the octants would all disappear at once), an enchantment
     * repointed at another {@code supported_items} tag, and any edit that widens either tag far
     * enough to swallow a plain vanilla pickaxe.
     */
    public static void theBuildingEnchantmentsReachEveryToolWhoseCodeReadsThem(GameTestHelper helper) {
        Enchantment fastChiseling = enchantment(helper, ModEnchantments.FAST_CHISELING).value();
        Enchantment constructorsTouch = enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH).value();

        List<Item> chisels = List.of(
                ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL,
                ModItems.GOLD_CHISEL, ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL);

        List<String> problems = new ArrayList<>();

        // --- Fast Chiseling: the chisels, through the tag and through the definition ---
        for (Item chisel : chisels) {
            if (!new ItemStack(chisel).is(ModTags.Items.CHISEL_TOOLS)) {
                problems.add("simplebuilding:chisel_tools no longer contains " + chisel);
            }
            if (!fastChiseling.canEnchant(new ItemStack(chisel))) {
                problems.add("Fast Chiseling cannot be put on " + chisel
                        + ", so that tier's cooldown discount is unreachable");
            }
        }

        // --- Constructor's Touch: every item that has a branch reading it ---
        List<Item> touchReaders = new ArrayList<>(chisels);
        touchReaders.addAll(List.of(
                ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER,
                ModItems.MAGNET, ModItems.ORE_DETECTOR,
                ModItems.QUIVER, ModItems.NETHERITE_QUIVER,
                Items.STICK));
        for (Item reader : touchReaders) {
            if (!constructorsTouch.canEnchant(new ItemStack(reader))) {
                problems.add("Constructor's Touch cannot be put on " + reader
                        + ", although that item's code reads the enchantment - the branch is dead");
            }
        }

        // --- the controls, without which every line above could be a wildcard ---
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        if (fastChiseling.canEnchant(pickaxe)) {
            problems.add("Fast Chiseling can be put on a plain diamond pickaxe, so chisel_tools "
                    + "accepts everything and the assertions above say nothing");
        }
        if (constructorsTouch.canEnchant(pickaxe)) {
            problems.add("Constructor's Touch can be put on a plain diamond pickaxe, so "
                    + "constructors_touch_enchantable accepts everything");
        }
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        if (fastChiseling.canEnchant(wand)) {
            problems.add("Fast Chiseling can be put on the diamond building wand, so chisel_tools "
                    + "and constructors_touch_enchantable have been confused with each other");
        }

        helper.assertTrue(problems.isEmpty(), "building enchantment tag problems: " + problems);
        helper.succeed();
    }

    // =====================================================================================
    // WHERE THE BOOKS COME FROM - LOOT
    // =====================================================================================

    /**
     * Which structure chest hands out which building enchantment book.
     * {@code ConfigOptionTests} proves the mod adds pools at all and that
     * {@code worldGen.enableLootTableChanges} switches them off; it counts pools and never looks
     * inside one, and {@code MiningEnchantmentTests} only ever looks for the six mining books. So
     * the whole progression that the building side of the mod is built around - the chisel books
     * out of the warm, early chests, the wand modifiers out of the raid structures, Master Builder
     * only out of the four late ones (the stronghold library, the end city, and both the rare and
     * the ominous trial chamber vault) - could be shuffled or dropped with every test still green.
     *
     * <p>The pools are serialised through {@code LootPool.CODEC} rather than rolled: rolling needs
     * dice and statistics, the codec output is exact. The first assertion is a shape guard, so a
     * serialisation change is reported as "this test reads the wrong shape" instead of quietly
     * passing on an empty search.
     *
     * <p>Every table is held to the <em>exact</em> set of building books it declares, levels
     * included, rather than to a list of books it has to contain among others. Both halves of that
     * matter. A level is the part of a loot entry that changes silently - a Fast Chiseling II
     * edited down to I still looks like a working chest, which buried treasure and the igloo make
     * visible with the same two enchantments at different levels. And a book that appears in a
     * chest it does not belong in is exactly the progression break this test claims to guard, so a
     * Master Builder book pushed into the pillager outpost has to fail here, which under a
     * "contains" check it never would.
     *
     * <p>What counts as a building book is nailed down in {@code BUILDING_BOOKS}, and nothing else
     * is judged: the mining books, Range, Deep Pockets, Funnel and Double Jump share these same
     * tables and belong to {@code MiningEnchantmentTests}. Bridge is in the set although the mod
     * hands it out nowhere (the known defect in the class javadoc) - whoever gives it a chest has
     * to name that chest here, and is told so by a red test instead of by nothing at all.
     *
     * <p>All three trial chamber tables are read, because
     * {@code ModLootTableModifications} matches them with two overlapping conditions:
     * {@code COMMON || RARE} carries the chisel books, {@code OMINOUS || RARE} carries Master
     * Builder, and the rare vault is the one table that has to answer both. Reading only the rare
     * vault leaves the first half of either condition unguarded - dropping {@code OMINOUS ||} takes
     * Master Builder out of the ominous vault while the rare half keeps matching, and nothing
     * moves.
     *
     * <p><strong>What breaks this test:</strong> a book moved to another chest or added to one, a
     * level that changes, a table that loses its pool entirely, and any of the four halves of the
     * two trial chamber conditions being dropped.
     */
    public static void buildingEnchantmentBooksSitInTheStructureChestsTheyBelongTo(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        List<String> problems = new ArrayList<>();
        try {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = true;

            Map<Identifier, Set<Integer>> outpost = storedEnchantments(helper, registries, BuiltInLootTables.PILLAGER_OUTPOST);
            helper.assertTrue(!outpost.isEmpty(),
                    "no stored enchantments could be read out of the pillager outpost pools; the loot "
                            + "pool serialisation changed shape and this test has to be rewritten - it is "
                            + "not a mod regression");

            // --- the wand modifiers: the two raid structures, the same three books each ---
            Map<ResourceKey<Enchantment>, Set<Integer>> wandModifiers = Map.of(
                    ModEnchantments.COLOR_PALETTE, Set.of(1),
                    ModEnchantments.COVER, Set.of(1),
                    ModEnchantments.LINEAR, Set.of(1));
            expectBuildingBooks(outpost, "pillager_outpost", wandModifiers, problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.WOODLAND_MANSION),
                    "woodland_mansion", wandModifiers, problems);

            // --- the chisel books: the same pair of enchantments, different levels per chest ---
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.BURIED_TREASURE),
                    "buried_treasure", Map.of(
                            ModEnchantments.CONSTRUCTORS_TOUCH, Set.of(1),
                            ModEnchantments.FAST_CHISELING, Set.of(2)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.IGLOO_CHEST),
                    "igloo_chest", Map.of(
                            ModEnchantments.CONSTRUCTORS_TOUCH, Set.of(1),
                            ModEnchantments.FAST_CHISELING, Set.of(1)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.SHIPWRECK_TREASURE),
                    "shipwreck_treasure", Map.of(ModEnchantments.FAST_CHISELING, Set.of(1)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.SIMPLE_DUNGEON),
                    "simple_dungeon", Map.of(ModEnchantments.FAST_CHISELING, Set.of(1)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.ABANDONED_MINESHAFT),
                    "abandoned_mineshaft", Map.of(ModEnchantments.FAST_CHISELING, Set.of(1)), problems);

            // --- Master Builder, the expensive one, only out of the late structures ---
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.STRONGHOLD_LIBRARY),
                    "stronghold_library", Map.of(ModEnchantments.MASTER_BUILDER, Set.of(1)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.END_CITY_TREASURE),
                    "end_city_treasure", Map.of(ModEnchantments.MASTER_BUILDER, Set.of(1)), problems);

            // --- the trial chambers: two overlapping conditions, so all three tables are read ---
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON),
                    "trial_chambers_reward_common", Map.of(
                            ModEnchantments.CONSTRUCTORS_TOUCH, Set.of(1),
                            ModEnchantments.FAST_CHISELING, Set.of(2)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS),
                    "trial_chambers_reward_ominous", Map.of(ModEnchantments.MASTER_BUILDER, Set.of(1)), problems);
            expectBuildingBooks(storedEnchantments(helper, registries, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE),
                    "trial_chambers_reward_rare", Map.of(
                            ModEnchantments.CONSTRUCTORS_TOUCH, Set.of(1),
                            ModEnchantments.FAST_CHISELING, Set.of(2),
                            ModEnchantments.MASTER_BUILDER, Set.of(1)), problems);
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.assertTrue(problems.isEmpty(), "building enchantment loot problems: " + problems);
        helper.succeed();
    }

    // =====================================================================================
    // WHERE THE BOOKS COME FROM - THE LIBRARIAN
    // =====================================================================================

    /**
     * The librarian's two building book trades are the only way to buy a building enchantment
     * instead of finding it, and nothing anywhere asserts what comes out of them.
     * {@code TradeAndMigrationTests} builds offers for the mason, the wandering trader and the
     * toolsmith and checks that the librarian's master book sits in the right trade set - it never
     * builds a librarian offer, so both of these files could hand out an unenchanted book, or the
     * wrong enchantment entirely, without a single test moving.
     *
     * <p>Each trade is held to its declared pool from both sides:
     * <ul>
     *   <li>per roll: at least one enchantment, and every enchantment on the result is a pool
     *       entry <em>at the level that entry declares</em> - a level typo produces a book no
     *       anvil can do anything sensible with and would otherwise look like a working trade;</li>
     *   <li>over all rolls: the pairs that actually came out are exactly the pairs the file
     *       declares. Without that half, deleting {@code linear} from the level 3 pool or zeroing
     *       its weight leaves every other assertion green while the mod stops selling a third of
     *       what it advertises.</li>
     * </ul>
     *
     * <p>The enchantments are read through {@code EnchantmentHelper#getEnchantmentsForCrafting},
     * which is what routes an enchanted book to its {@code stored_enchantments} instead of to the
     * {@code enchantments} a tool carries. That routing is vanilla's, not the mod's - the mod's
     * {@code WeightedEnchantFunction} only calls {@code updateEnchantments} - so the test reads
     * whichever component vanilla picked rather than pinning the choice.
     *
     * <p>The price, the use count and the experience are asserted too, because they are the rest
     * of what the JSON declares and nothing else reads these two files at all.
     *
     * <p><strong>What breaks this test:</strong> a pool entry that changes enchantment, level or
     * weight-to-zero, an entry deleted from the file, a {@code weighted_enchant} modifier dropped
     * in a datagen run (the book comes out plain), a repriced trade, and a trade whose result item
     * stops being an enchanted book.
     */
    public static void librarianBookTradesHandOutOnlyTheEnchantmentsTheyDeclare(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        List<String> problems = new ArrayList<>();

        // data/simplebuilding/villager_trade/librarian/3/emerald_building_book.json
        MerchantOffer buildingBook = assertBookTrade(helper, villager,
                "simplebuilding:librarian/3/emerald_building_book", 25, 3, 15,
                Set.of(pair(ModEnchantments.COLOR_PALETTE, 1),
                        pair(ModEnchantments.FAST_CHISELING, 1),
                        pair(ModEnchantments.LINEAR, 1)),
                problems);
        helper.assertTrue(buildingBook.getCostB().isEmpty(),
                "librarian/3/emerald_building_book grew a second cost item: " + buildingBook.getCostB());

        // data/simplebuilding/villager_trade/librarian/4/emerald_advanced_book.json
        assertBookTrade(helper, villager,
                "simplebuilding:librarian/4/emerald_advanced_book", 25, 2, 25,
                Set.of(pair(ModEnchantments.LINEAR, 1),
                        pair(ModEnchantments.OVERRIDE, 1)),
                problems);

        helper.assertTrue(problems.isEmpty(), "librarian book trade problems: " + problems);
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS - THE PLAYER
    // =====================================================================================

    /**
     * A mock player in the room with creative building taken away from it.
     *
     * <p>{@code GameTestHelper}'s in level mock hard-overrides {@code gameMode()} to
     * {@code CREATIVE}, so {@code isCreative()} cannot be moved and {@code setGameMode} is not even
     * attempted. What every payment in this file is gated on is {@code Abilities.instabuild}, a
     * plain public field: the wand's block consumption, its abort-when-out-of-material branch and
     * the chisel's {@code addCooldown} all read it. Clearing it is the premise of the whole file,
     * so the assertion below fails loudly if a later edit drops the line, instead of letting half
     * the file pass while measuring nothing.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 6.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        helper.assertFalse(player.getAbilities().instabuild,
                "the mock player is still building for free, so no block would ever be consumed and "
                        + "no cooldown ever imposed - this file would measure nothing");
        // Hand the player back no matter how the test ends; a leaked mock player keeps the player
        // list non-empty and the gametest server then stalls on shutdown.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    // =====================================================================================
    // HELPERS - THE WAND
    // =====================================================================================

    /**
     * A diamond wand pinned to radius 1, so the expected plane is a 3x3 regardless of how the
     * tiers are balanced - the test states a shape, not a tuning number.
     */
    private static ItemStack radiusOneWand() {
        return withRadius(new ItemStack(ModItems.DIAMOND_BUILDING_WAND), 1);
    }

    private static ItemStack masterBuilderWand(GameTestHelper helper) {
        ItemStack wand = radiusOneWand();
        wand.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        return wand;
    }

    /** Writes the wand's own radius/axis settings, the way the settings screen's packet does. */
    private static ItemStack withRadius(ItemStack wand, int radius) {
        ItemStack copy = wand.copy();
        CompoundTag settings = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        settings.putInt("SettingsRadius", radius);
        settings.putInt("SettingsAxis", 0);
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return copy;
    }

    /** The same wand with no stored radius at all, so the tier maximum has to take over. */
    private static ItemStack withoutRadius(ItemStack wand) {
        ItemStack copy = wand.copy();
        CompoundTag settings = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        settings.remove("SettingsRadius");
        settings.putInt("SettingsAxis", 0);
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return copy;
    }

    /**
     * Empties the inventory, clears the plane above {@link #ANCHOR} and puts the wand in the
     * selected hotbar slot. {@code Inventory#clearContent} clears the equipment too, so the off
     * hand has to be filled by the caller <em>after</em> this call, never before.
     */
    private static void armWand(ServerPlayer player, ItemStack wand) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    /** Clears the read back window, rebuilds the anchor and right clicks its top face. */
    private static InteractionResult armOnAnchor(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        clearPlane(helper);
        helper.setBlock(ANCHOR, Blocks.STONE);
        return useOnTopFace(helper, player, wand, ANCHOR);
    }

    /**
     * Arms the wand on {@link #ANCHOR} and drives its item tick until it switches itself off -
     * either because the plane is finished or because it ran out of material. Callers have to
     * assert the resulting shape themselves; a run that quietly built nothing would otherwise
     * look exactly like a completed one.
     */
    private static void runWand(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        InteractionResult armed = armOnAnchor(helper, player, wand);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand did not arm itself on the clicked face, it returned " + armed);
        helper.assertTrue(wandIsActive(wand), "the wand did not switch itself on when it was armed");

        BuildingWandItem item = (BuildingWandItem) wand.getItem();
        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_TICK_CAP) {
            item.inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < WAND_TICK_CAP,
                "the wand never finished within " + WAND_TICK_CAP + " ticks");
    }

    /** Whether the wand's own NBT still says it has building left to do. */
    private static boolean wandIsActive(ItemStack wand) {
        return wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr("Active", false);
    }

    /** Empties the 5x5 window the runs are read back through, so no run inherits the last one. */
    private static void clearPlane(GameTestHelper helper) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(ANCHOR.offset(dx, 1, dz), Blocks.AIR);
            }
        }
    }

    /**
     * Every non air block in that same 5x5 window, as offset to block. The window is two blocks
     * wider than the expected 3x3 on every side on purpose: a wand that built one ring too many
     * has to show up as a size mismatch instead of being cropped away.
     */
    private static Map<BlockPos, Block> placedBlocks(GameTestHelper helper) {
        Map<BlockPos, Block> placed = new HashMap<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos offset = new BlockPos(dx, 1, dz);
                BlockState state = helper.getBlockState(ANCHOR.offset(dx, 1, dz));
                if (!state.isAir()) {
                    placed.put(offset, state.getBlock());
                }
            }
        }
        return placed;
    }

    /** The plane is exactly {@value #WAND_PLANE_BLOCKS} blocks and every one of them is {@code block}. */
    private static void assertPlaneOf(GameTestHelper helper, Block block, String what) {
        Map<BlockPos, Block> placed = placedBlocks(helper);
        helper.assertValueEqual(placed.size(), WAND_PLANE_BLOCKS,
                what + ": the wand placed " + placed.size() + " blocks instead of the expected 3x3");
        for (Map.Entry<BlockPos, Block> entry : placed.entrySet()) {
            helper.assertValueEqual(entry.getValue(), block,
                    what + ": " + entry.getValue() + " at offset " + entry.getKey());
        }
    }

    private static int previewSize(GameTestHelper helper, ServerPlayer player, ItemStack wand, BlockPos origin) {
        return BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, wand, origin, Direction.UP, 3).size();
    }

    /**
     * Compares what the highlight promised against what the wand then placed. Both sides are read
     * in absolute coordinates - {@code getPreviewStates} hands out absolute positions and
     * {@code relativePos} mirrors instead of inverting {@code absolutePos}, so converting back
     * would compare the wrong blocks. The size check underneath catches the other direction: a
     * preview that promised a subset of what was built.
     */
    private static void assertPreviewWasKept(GameTestHelper helper, ServerLevel level,
                                             Map<BlockPos, BlockState> promised) {
        for (Map.Entry<BlockPos, BlockState> entry : promised.entrySet()) {
            Block placed = level.getBlockState(entry.getKey()).getBlock();
            helper.assertValueEqual(placed, entry.getValue().getBlock(),
                    "the highlight promised " + entry.getValue().getBlock() + " at " + entry.getKey()
                            + " and the wand placed " + placed);
        }
        helper.assertValueEqual(placedBlocks(helper).size(), promised.size(),
                "the wand placed a different number of blocks than the preview drew");
    }

    private static Set<Block> distinctBlocks(Map<BlockPos, BlockState> preview) {
        Set<Block> blocks = new HashSet<>();
        for (BlockState state : preview.values()) {
            blocks.add(state.getBlock());
        }
        return blocks;
    }

    /** Right clicks the centre of a block's top face, server side. */
    private static InteractionResult useOnTopFace(GameTestHelper helper, ServerPlayer player,
                                                  ItemStack stack, BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    // =====================================================================================
    // HELPERS - BUNDLES AND INVENTORY
    // =====================================================================================

    private static ItemStack bundleOfStone(ServerPlayer player, int count) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ((ReinforcedBundleItem) bundle.getItem())
                .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, count), player);
        return bundle;
    }

    /**
     * The same bundle carrying Master Builder itself - the half of the either-or that does not need
     * an enchanted wand. The enchantment goes on before the stone because
     * {@code ReinforcedBundleItem#tryInsertStackFromWorld} reads the bundle's own enchantments for
     * its capacity while inserting.
     */
    private static ItemStack masterBuilderBundle(GameTestHelper helper, ServerPlayer player, int count) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        bundle.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        ((ReinforcedBundleItem) bundle.getItem())
                .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, count), player);
        return bundle;
    }

    private static int countInBundle(ItemStack bundle, Item item) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int total = 0;
        for (ItemStackTemplate template : contents.items()) {
            ItemStack stack = template.create();
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** How many of {@code item} lie loose in the inventory, i.e. outside any container item. */
    private static int looseCount(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // =====================================================================================
    // HELPERS - THE CHISEL COOLDOWN
    // =====================================================================================

    /**
     * Chisels stone into chiseled stone bricks and drains the resulting cooldown one tick at a
     * time, returning how many ticks that took. Bounded, so a cooldown that never ends fails the
     * test instead of hanging the run.
     */
    private static int measureCooldown(GameTestHelper helper, ServerPlayer player, ItemStack chisel) {
        player.setShiftKeyDown(false);
        ItemCooldowns cooldowns = player.getCooldowns();
        cooldowns.removeCooldown(cooldowns.getCooldownGroup(chisel));
        helper.setBlock(CHISEL_TARGET, Blocks.STONE);

        InteractionResult result = useOnTopFace(helper, player, chisel, CHISEL_TARGET);
        helper.assertTrue(result != InteractionResult.PASS,
                "the chisel refused stone, so there is no cooldown to measure; it returned " + result);
        helper.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, CHISEL_TARGET);
        helper.assertTrue(cooldowns.isOnCooldown(chisel),
                "the chisel put itself on no cooldown at all after a real transformation, so its "
                        + "cooldown is effectively zero");

        int ticks = 0;
        while (cooldowns.isOnCooldown(chisel) && ticks < COOLDOWN_TICK_CAP) {
            cooldowns.tick();
            ticks++;
        }
        helper.assertTrue(ticks < COOLDOWN_TICK_CAP,
                "the chisel cooldown never ran out within " + COOLDOWN_TICK_CAP + " ticks");
        return ticks;
    }

    // =====================================================================================
    // HELPERS - LOOT POOLS
    // =====================================================================================

    /**
     * Holds one table to exactly the building books it is meant to carry: every declared
     * enchantment at exactly the declared levels, and no further book out of
     * {@code BUILDING_BOOKS} on top of them. Books outside that set are ignored rather than
     * reported, so this says nothing about the mining pools that share the same tables.
     */
    private static void expectBuildingBooks(Map<Identifier, Set<Integer>> found, String table,
                                            Map<ResourceKey<Enchantment>, Set<Integer>> declared,
                                            List<String> problems) {
        Set<String> actual = new TreeSet<>();
        for (Map.Entry<Identifier, Set<Integer>> entry : found.entrySet()) {
            if (BUILDING_BOOKS.contains(entry.getKey())) {
                actual.add(entry.getKey() + " at " + new TreeSet<>(entry.getValue()));
            }
        }
        Set<String> expected = new TreeSet<>();
        for (Map.Entry<ResourceKey<Enchantment>, Set<Integer>> entry : declared.entrySet()) {
            expected.add(entry.getKey().identifier() + " at " + new TreeSet<>(entry.getValue()));
        }
        if (!actual.equals(expected)) {
            problems.add(table + " hands out the building books " + actual
                    + " instead of exactly " + expected);
        }
    }

    /**
     * Every {@code stored_enchantments} the mod's pools for one table carry, as
     * {@code enchantment id -> levels}. The pools are serialised through {@code LootPool.CODEC}
     * because a built pool keeps its entry list private; unlike rolling the pool this needs no
     * dice and no statistics.
     */
    private static Map<Identifier, Set<Integer>> storedEnchantments(GameTestHelper helper,
                                                                    HolderLookup.Provider registries,
                                                                    ResourceKey<LootTable> table) {
        PoolCollector collector = new PoolCollector();
        ModLootTableModifications.apply(table, collector, registries);

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Map<Identifier, Set<Integer>> found = new LinkedHashMap<>();
        for (LootPool pool : collector.pools) {
            JsonElement encoded = LootPool.CODEC.encodeStart(ops, pool)
                    .getOrThrow(message -> helper.assertionException(
                            "a loot pool the mod adds to " + table.identifier()
                                    + " cannot be serialised: " + message));
            collectStoredEnchantments(encoded, found);
        }
        return found;
    }

    /**
     * Walks the serialised pool for objects stored under {@code minecraft:stored_enchantments} and
     * collects the {@code id -> level} pairs below them. The search below that key is recursive on
     * purpose: whether the component encodes as a bare map or wraps its levels in another object is
     * vanilla's business, and either shape has to keep working.
     */
    private static void collectStoredEnchantments(JsonElement element, Map<Identifier, Set<Integer>> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectStoredEnchantments(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (STORED_ENCHANTMENTS_KEY.equals(entry.getKey())) {
                collectLevels(entry.getValue(), out);
            } else {
                collectStoredEnchantments(entry.getValue(), out);
            }
        }
    }

    private static void collectLevels(JsonElement element, Map<Identifier, Set<Integer>> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectLevels(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            Optional<Identifier> id = Identifier.read(entry.getKey()).result();
            if (id.isPresent() && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                out.computeIfAbsent(id.get(), unused -> new TreeSet<>()).add(value.getAsInt());
            } else {
                collectLevels(value, out);
            }
        }
    }

    /** Collects the pools the mod offers for one table, from both editor paths. */
    private static final class PoolCollector implements ModLootTableModifications.Editor {
        private final List<LootPool> pools = new ArrayList<>();

        @Override
        public void addPool(LootPool.Builder pool) {
            this.pools.add(pool.build());
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.pools.add(pool);
        }
    }

    // =====================================================================================
    // HELPERS - TRADES
    // =====================================================================================

    /**
     * Rolls one book trade {@value #TRADE_ROLLS} times and holds the result to its declared pool
     * from both sides. Returns the last offer so the caller can assert anything else about it.
     */
    private static MerchantOffer assertBookTrade(GameTestHelper helper, Villager villager, String id,
                                                 int emeralds, int maxUses, int xp,
                                                 Set<String> declared, List<String> problems) {
        VillagerTrade trade = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .getValue(Identifier.parse(id));
        helper.assertTrue(trade != null, "the trade " + id + " is not registered");

        // Built once up front, so the numbers below are asserted even when a later roll fails, and
        // so a problem carried in from another trade cannot leave this one without an offer.
        MerchantOffer sample = trade.getOffer(tradeContext(helper, villager, 7919L));
        helper.assertTrue(sample != null, id + " produced no offer at all");

        ItemStack cost = sample.getBaseCostA();
        helper.assertTrue(cost.is(Items.EMERALD), id + ": the price is paid in " + cost + ", not in emeralds");
        helper.assertValueEqual(cost.getCount(), emeralds, id + ": emeralds per book");
        helper.assertValueEqual(sample.getResult().getCount(), 1, id + ": books per trade");
        helper.assertValueEqual(sample.getMaxUses(), maxUses, id + ": max uses");
        helper.assertValueEqual(sample.getXp(), xp, id + ": trade xp");

        List<String> own = new ArrayList<>();
        Set<String> handedOut = new TreeSet<>();

        for (int roll = 1; roll <= TRADE_ROLLS && own.isEmpty(); roll++) {
            MerchantOffer offer = trade.getOffer(tradeContext(helper, villager, roll * 7919L));
            helper.assertTrue(offer != null, id + " produced no offer on roll " + roll);

            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) {
                own.add(id + " roll " + roll + ": the merchant handed out " + result
                        + " instead of an enchanted book");
                continue;
            }

            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
            if (enchantments.isEmpty()) {
                own.add(id + " roll " + roll + ": the book came out unenchanted, so its "
                        + "weighted_enchant modifier did nothing");
                continue;
            }
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                String drawn = holder.getRegisteredName() + "@" + enchantments.getLevel(holder);
                handedOut.add(drawn);
                if (!declared.contains(drawn)) {
                    own.add(id + " roll " + roll + ": the merchant put " + drawn + " on the book, "
                            + "which is not one of the pairs its json offers (" + declared + ")");
                }
            }
        }

        // The other direction, and only worth asking once every roll was clean - after an early
        // exit the collected set is half filled and the difference would be noise.
        if (own.isEmpty() && !handedOut.equals(declared)) {
            own.add(id + ": over " + TRADE_ROLLS + " rolls the trade handed out " + handedOut
                    + " instead of the pairs the file declares, " + declared);
        }

        problems.addAll(own);
        return sample;
    }

    /** A trade roll with a fixed seed, so a failure names a case that can be reproduced. */
    private static LootContext tradeContext(GameTestHelper helper, Villager villager, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
    }

    private static String pair(ResourceKey<Enchantment> key, int level) {
        return key.identifier() + "@" + level;
    }

    // =====================================================================================
    // HELPERS - ENCHANTMENTS
    // =====================================================================================

    private static ItemStack enchanted(GameTestHelper helper, Item item,
                                       ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
