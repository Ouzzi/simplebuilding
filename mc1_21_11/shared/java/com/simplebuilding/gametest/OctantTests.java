package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.OctantItem.FillOrder;
import com.simplebuilding.items.custom.OctantItem.SelectionShape;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.OctantConfigurePayload;
import com.simplebuilding.networking.OctantScrollPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The octant - the mod's measuring tool - end to end, on the parts that no other suite touches.
 *
 * <p>Six other suites already own big pieces of the octant, and none of them is repeated here:
 * {@code ItemBehaviourTests#octantStoresBothCornersAndRespectsTheLock} covers the two corner
 * clicks and the lock on {@code useOn} <em>and</em> on {@code use},
 * {@code ConsumptionAndDurabilityTests} covers the one durability point per accepted click,
 * {@code NetworkHandlerTests} covers the two payload handlers on a well formed main hand octant,
 * {@code ProtectionAndRangeTests#rangeAddsBlockInteractionReachInTheMainHandOnly} covers which
 * items may carry Range - the plain octant, all sixteen dyed ones and a negative control - and
 * {@code ConfigOptionTests} rolls the three chest pools until the octant item falls out of each.
 * The wandering trader's two octant trades are pinned field by field twice over, in
 * {@code TradeAndMigrationTests} and {@code TradeOfferTests}.
 *
 * <p>What was left over - and is what this class does - is everything around that: the shipped
 * numbers all seventeen octants share plus the paint each of the sixteen dyed ones reports, the
 * {@code use()} branch that quietly does nothing because the player is not sneaking, the display
 * name and the tooltip, the three guards
 * on the scroll handler, the sixteen dyeing recipes, the water cauldron that washes a coloured one
 * clean again - all sixteen colours of it - and the one thing about the chest entries that rolling
 * them cannot show: which of them hand the octant over randomly enchanted.
 *
 * <h2>Known defects</h2>
 * <ul>
 *   <li><b>The lock does not protect against scrolling on the server.</b>
 *       {@code OctantItem#useOn} and {@code OctantItem#use} both refuse to touch a locked octant,
 *       but {@code ModMessageHandlers#handleOctantScroll} never reads the {@code Locked} tag - the
 *       only thing that suppresses scrolling is the client side mouse mixin. A client that sends
 *       the payload anyway moves the corners of a locked octant. Nothing here asserts that, in
 *       either direction: pinning it down would write the defect into the suite as if it were the
 *       intended behaviour. {@link #airClicksOnlyResetAnUnlockedOctantWhileSneaking} asserts the
 *       half of the lock the server really does enforce.</li>
 *   <li><b>{@code Hollow}, {@code LayerMode} and {@code FillOrder} are stored and no server path
 *       reads them.</b> The octant screen sends them, {@code handleOctantConfigure} writes them
 *       into the item, and the only code that reads them back is {@code client/gui/OctantScreen},
 *       which restores its own controls from them ({@code OctantScreen#loadDataFromStack}).
 *       Nothing on the server and no placement path ever looks at them - the octant builds nothing
 *       at all. {@link #theOctantOnlyMeasuresAndPlacesNothing} is the marker for that, in the same
 *       spirit as
 *       {@code EnchantmentEffectTests#coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown}.</li>
 * </ul>
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li>Everything the colour of a coloured octant is actually <em>for</em>: the selection
 *       outline, the corner markers and the heads up display are renderer code and need a
 *       client.</li>
 *   <li>What {@code Orientation} <em>means</em>. Unlike the three switches above it is read back,
 *       but only on the client: {@code BlockHighlightRenderer#drawOctantHighlights} turns the
 *       stored ordinal into a {@link Direction} and rotates the cylinder, ellipse, pyramid and
 *       triangle preview with it. That is renderer code on both loaders, so the only thing said
 *       about the tag here is that a corner click does not drop it.</li>
 *   <li>The overlay message a locked octant sends on a refused click
 *       ({@code player.sendOverlayMessage}) and the click sounds - both are packets to a real
 *       client.</li>
 *   <li>The octant screen itself ({@code OctantScreen}), the mouse mixin that suppresses
 *       scrolling while the item is locked, and {@code tools.invertOctantSneak}, which is read on
 *       the client only.</li>
 *   <li>Whether a generated chest in a real ancient city, nether fortress or pillager outpost ends
 *       up holding an octant: gametests run in an already generated room, so only the pools the
 *       mod hands to the loot loader can be inspected, not the rolls a world does. Rolling them
 *       for the item is {@code ConfigOptionTests}' job.</li>
 * </ul>
 */
public final class OctantTests {

    private OctantTests() {
    }

    /** Shipped durability of every octant; {@code OctantItem.DURABILITY_OCTANT}. */
    private static final int OCTANT_DURABILITY = 128;

    /** Item id of the plain octant, as the loot pool codec writes it. */
    private static final String OCTANT_ID = "simplebuilding:octant";

    /**
     * {@code SelectionShape}, in order, as {@code <constant>=<translation key>}. The order is not
     * cosmetic: {@code handleOctantScroll} cycles by ordinal and falls back to the first constant,
     * and a saved octant stores the constant's {@code name()}, so inserting a shape in the middle
     * silently re-labels every octant already in a world.
     */
    private static final List<String> EXPECTED_SHAPES = List.of(
            "CUBOID=simplebuilding.shape.cuboid",
            "CYLINDER=simplebuilding.shape.cylinder",
            "TRIANGLE=simplebuilding.shape.triangle",
            "PYRAMID=simplebuilding.shape.pyramid",
            "SPHERE=simplebuilding.shape.sphere",
            "RECTANGLE=simplebuilding.shape.rectangle",
            "ELLIPSE=simplebuilding.shape.ellipse");

    /** The same for {@code FillOrder}, which is stored by name as well. */
    private static final List<String> EXPECTED_FILL_ORDERS = List.of(
            "DEFAULT=simplebuilding.order.default",
            "BOTTOM_UP=simplebuilding.order.bottom_up",
            "TOP_DOWN=simplebuilding.order.top_down");

    /**
     * The loot function id the two "dangerous chest" octant entries carry, as
     * {@code EnchantRandomlyFunction}'s codec writes it.
     */
    private static final String ENCHANT_RANDOMLY = "minecraft:enchant_randomly";

    /** The two corners every NBT-only test uses; far outside the test room on purpose. */
    private static final int[] FIRST_CORNER = {10, 20, 30};
    private static final int[] SECOND_CORNER = {-40, 50, -60};

    // =====================================================================================
    // (a) ITEM PROPERTIES
    // =====================================================================================

    /**
     * The plain octant and the sixteen dyed ones are the same tool wearing different paint: same
     * durability, same stack size, and each dyed one reporting exactly its own colour.
     *
     * <p>The dyed items are built inside a {@code DyeColor} loop in {@code ModItems}, which is
     * exactly the kind of thing that survives a port with one colour missing, so all sixteen are
     * walked rather than a sample. {@code getColor()} is what the cauldron interaction and the
     * highlight renderer tell the dyed ones apart by, and {@code null} is what marks the plain one.
     *
     * <p><b>Which items may carry Range is deliberately not asked here.</b>
     * {@code ProtectionAndRangeTests#rangeAddsBlockInteractionReachInTheMainHandOnly} already walks
     * {@code canEnchant} over the plain octant, over all sixteen dyed ones (behind a size guard)
     * and over a diamond helmet as the negative control, and {@code Enchantment#canEnchant} and
     * {@code Enchantment#isSupportedItem} are the same question spelled twice -
     * {@code supportedItems().contains(stack.typeHolder())} against {@code stack.is(supportedItems)}.
     * Asking it again with an emerald instead of a helmet would add nothing.
     *
     * <p>What is left of Range here is the one half that no {@code isPrimaryItem} call can show.
     * {@code isPrimaryItem} is {@code isSupportedItem(stack) && (primaryItems.isEmpty() ||
     * stack.is(primaryItems))}, so deleting the {@code primary_items} field from
     * {@code range.json} outright makes it answer <em>true</em> for everything supported - the
     * enchanting table would silently stop offering Range while every {@code isPrimaryItem}
     * assertion stayed green. So the definition is read directly instead: both fields have to name
     * a tag, and it has to be the same one.
     *
     * <p>What breaks this test: a colour dropping out of {@code COLORED_OCTANT_ITEMS}, a dyed item
     * built with the wrong {@code DyeColor}, one octant being registered with a different
     * {@code durability(...)} or a stackable one, or {@code range.json} losing its
     * {@code primary_items} field or pointing the two fields at different tags.
     */
    public static void allOctantColoursShareDurabilityAndTheirPaint(GameTestHelper helper) {
        ItemStack plain = new ItemStack(ModItems.OCTANT);
        helper.assertValueEqual(plain.getMaxDamage(), OCTANT_DURABILITY,
                "durability of simplebuilding:octant");
        helper.assertValueEqual(plain.getMaxStackSize(), 1,
                "stack size of simplebuilding:octant; a damageable tool has to be unstackable, "
                        + "otherwise a stack of them shares one damage bar");
        helper.assertTrue(ModItems.OCTANT.getColor() == null,
                "the plain octant reports the dye colour " + ModItems.OCTANT.getColor()
                        + "; the renderer and the cauldron both tell it apart from the dyed ones by "
                        + "exactly that null");

        // The loop walks DyeColor.values(), not the map, so a colour that never made it into
        // COLORED_OCTANT_ITEMS shows up as a missing entry rather than as a shorter loop.
        List<String> problems = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            String name = "octant_" + color.getName();
            OctantItem dyed = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (dyed == null) {
                problems.add(name + " is missing from ModItems.COLORED_OCTANT_ITEMS");
                continue;
            }
            if (dyed.getColor() != color) {
                problems.add(name + " reports the colour " + dyed.getColor());
            }
            ItemStack stack = new ItemStack(dyed);
            if (stack.getMaxDamage() != OCTANT_DURABILITY) {
                problems.add(name + " has " + stack.getMaxDamage() + " durability instead of "
                        + OCTANT_DURABILITY);
            }
            if (stack.getMaxStackSize() != 1) {
                problems.add(name + " stacks to " + stack.getMaxStackSize());
            }
        }
        helper.assertTrue(problems.isEmpty(), "coloured octant problems: " + problems);

        // --- and the one Range question isPrimaryItem cannot answer ---
        Enchantment range = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getValueOrThrow(ModEnchantments.RANGE);
        Optional<TagKey<Item>> supported = range.definition().supportedItems().unwrapKey();
        Optional<TagKey<Item>> primary = range.definition().primaryItems()
                .flatMap(HolderSet::unwrapKey);
        helper.assertTrue(supported.isPresent(),
                "Range's supported_items is an inline item list (" + range.definition().supportedItems()
                        + ") instead of a tag, so the octants can no longer join it through "
                        + "#simplebuilding:octants_enchantable");
        helper.assertValueEqual(primary, supported,
                "Range's primary_items and supported_items no longer name the same tag; an empty "
                        + "primary_items makes every supported item primary, which is why this is "
                        + "read off the definition and not through isPrimaryItem");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (b) THE USE() BRANCH THAT QUIETLY DOES NOTHING
    // =====================================================================================

    /**
     * Right clicking the air throws the selection away - but only while sneaking, and only when
     * the octant is not locked. <b>The sneaking branch of that is not new here.</b>
     * {@code ItemBehaviourTests#octantStoresBothCornersAndRespectsTheLock} already drives both
     * sneaking cases - locked, where {@code use} has to answer {@code PASS} and leave both corners
     * alone, and unlocked, where the selection has to end up empty - and names the lock check in
     * {@code use} in its own "what breaks this test" list. Steps one and three below repeat that
     * deliberately, as the setup for and the control around the branch that is genuinely
     * uncovered: <b>unlocked, but not sneaking</b>. Nothing else in the suite right clicks the air
     * with an octant while standing upright, and that is the branch a player hits by accident.
     *
     * <p>The result value cannot separate the two refusals: the locked branch returns
     * {@code PASS} explicitly and vanilla's {@code Item#use} returns {@code PASS} for an item with
     * no consumable, equippable, blocks_attacks or kinetic_weapon component either. So the
     * assertions are on the stored data, and the last step - the sneak click that <em>is</em>
     * allowed - is there as the control: without it, an octant whose {@code use} never did
     * anything at all would pass the first two steps.
     *
     * <p>What breaks this test: the {@code user.isShiftKeyDown()} condition disappearing or being
     * inverted, the reset being moved somewhere that runs before that guard, or - which
     * {@code ItemBehaviourTests} would catch as well - the {@code Locked} check at the top of
     * {@code use} disappearing.
     */
    public static void airClicksOnlyResetAnUnlockedOctantWhileSneaking(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);

        // --- locked and sneaking: the reset is refused, lock included ---
        setSelection(octant, FIRST_CORNER, SECOND_CORNER, true);
        player.setShiftKeyDown(true);
        InteractionResult locked = octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(locked == InteractionResult.PASS,
                "a locked octant answered a sneaking air click with " + locked
                        + " instead of passing the click on");
        assertSelection(helper, octant, FIRST_CORNER, SECOND_CORNER,
                "a locked octant threw its selection away on a sneaking air click");
        helper.assertTrue(customData(octant).getBooleanOr("Locked", false),
                "the refused air click cleared the lock itself, so the next click would go through");

        // --- unlocked but not sneaking: nothing happens either ---
        setSelection(octant, FIRST_CORNER, SECOND_CORNER, false);
        player.setShiftKeyDown(false);
        octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertSelection(helper, octant, FIRST_CORNER, SECOND_CORNER,
                "a plain right click in the air cleared the selection; only the sneak click may");

        // --- control: unlocked and sneaking really does clear it ---
        player.setShiftKeyDown(true);
        octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(customData(octant).isEmpty(),
                "control: an unlocked, sneaking air click did not clear the selection, so the two "
                        + "refusals above prove nothing");

        player.setShiftKeyDown(false);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (c) NAME AND TOOLTIP
    // =====================================================================================

    /**
     * The seven shapes, in their shipped order, and the one place the chosen shape becomes visible
     * without a client: the item's display name.
     *
     * <p>The catalogue is pinned as {@code <constant>=<translation key>} pairs because both halves
     * are load bearing. The constant name is what {@code handleOctantConfigure} writes into the
     * item and {@code getName} reads back, so a renamed constant orphans every saved octant; the
     * ordinal is what {@code handleOctantScroll} cycles through; and the translation key is what a
     * player actually reads. Comparing the whole list in one go also pins the count.
     *
     * <p>The names are compared through {@code Component#getString()} on both sides, so it makes
     * no difference whether the server has a language file for the mod's keys loaded.
     *
     * <p>The unreadable {@code Shape} tag at the end is the {@code catch} in {@code getName}: an
     * octant written by an older version, or by a shape that has since been renamed, has to keep
     * its plain name instead of throwing out of a display name lookup.
     *
     * <p>What breaks this test: a shape added, removed, renamed or reordered, a changed
     * translation key, the {@code shape != CUBOID} exception going away (which would append
     * "(Cuboid)" to every default octant), or the try/catch around {@code valueOf} disappearing.
     */
    public static void shapeCatalogueIsFixedAndTheChosenShapeShowsInTheName(GameTestHelper helper) {
        List<String> shapes = new ArrayList<>();
        for (SelectionShape shape : SelectionShape.values()) {
            shapes.add(shape.name() + "=" + translationKey(shape.getText()));
        }
        helper.assertValueEqual(shapes, EXPECTED_SHAPES, "the SelectionShape catalogue");
        for (SelectionShape shape : SelectionShape.values()) {
            helper.assertValueEqual(shape.getTranslationKey(), translationKey(shape.getText()),
                    "getTranslationKey() and getText() disagree for " + shape.name());
        }

        List<String> orders = new ArrayList<>();
        for (FillOrder order : FillOrder.values()) {
            orders.add(order.name() + "=" + translationKey(order.getText()));
        }
        helper.assertValueEqual(orders, EXPECTED_FILL_ORDERS, "the FillOrder catalogue");

        // --- and now the name ---
        String baseName = new ItemStack(ModItems.OCTANT).getHoverName().getString();

        helper.assertValueEqual(withShape(SelectionShape.CUBOID.name()).getHoverName().getString(),
                baseName,
                "an octant set to CUBOID has to keep its plain name; the default shape is not "
                        + "worth a suffix");

        for (SelectionShape shape : SelectionShape.values()) {
            if (shape == SelectionShape.CUBOID) {
                continue;
            }
            helper.assertValueEqual(withShape(shape.name()).getHoverName().getString(),
                    baseName + " (" + shape.getText().getString() + ")",
                    "the display name of an octant set to " + shape.name());
        }

        helper.assertValueEqual(withShape("NOT_A_SHAPE").getHoverName().getString(), baseName,
                "an octant whose Shape tag names a constant that no longer exists lost its name "
                        + "instead of falling back to the plain one");

        TestCleanup.succeed(helper);
    }

    /**
     * The tooltip is the only place the stored selection is readable in game, and it is built by
     * hand in {@code OctantItem#appendHoverText}: the lock first, then the first corner, then -
     * only if the first one is there - the second.
     *
     * <p>The two corner lines are asserted as literal text. Their format is vanilla's
     * ({@code BlockPos#toShortString()} renders {@code "x, y, z"}), so a vanilla change would turn
     * this red as well; that is the informative failure, because the string is what a player sees.
     * The lock line is a translatable component, so it is checked by its key rather than its text -
     * that survives a server without the mod's language file.
     *
     * <p>{@code Item#appendHoverText} is empty in vanilla, so the collected list is exactly the
     * lines the mod adds and a fresh octant has to produce none at all.
     *
     * <p>What breaks this test: a dropped or reordered line, the second corner being printed
     * without the first one being set, the {@code p1.length == 3} guard going away (a
     * half-written tag would then throw out of the tooltip), or a fresh octant starting to print
     * something.
     */
    public static void octantTooltipListsTheLockAndBothCorners(GameTestHelper helper) {
        helper.assertTrue(tooltip(helper, new ItemStack(ModItems.OCTANT)).isEmpty(),
                "a fresh octant already prints tooltip lines: "
                        + tooltip(helper, new ItemStack(ModItems.OCTANT)));

        ItemStack firstOnly = new ItemStack(ModItems.OCTANT);
        setSelection(firstOnly, FIRST_CORNER, null, false);
        helper.assertValueEqual(tooltip(helper, firstOnly), List.of("10, 20, 30"),
                "the tooltip of an octant with only the first corner set");

        ItemStack secondOnly = new ItemStack(ModItems.OCTANT);
        setSelection(secondOnly, null, SECOND_CORNER, false);
        helper.assertValueEqual(tooltip(helper, secondOnly), List.of(),
                "an octant with a second corner but no first one printed a line anyway; the whole "
                        + "corner block hangs off the Pos1 check");

        ItemStack both = new ItemStack(ModItems.OCTANT);
        setSelection(both, FIRST_CORNER, SECOND_CORNER, false);
        helper.assertValueEqual(tooltip(helper, both), List.of("10, 20, 30", "-40, 50, -60"),
                "the tooltip of an octant with both corners set");

        ItemStack locked = new ItemStack(ModItems.OCTANT);
        setSelection(locked, FIRST_CORNER, SECOND_CORNER, true);
        List<Component> lines = tooltipComponents(helper, locked);
        helper.assertValueEqual(lines.size(), 3,
                "a locked octant with both corners has to print three lines, it printed "
                        + lines.stream().map(Component::getString).toList());
        helper.assertValueEqual(translationKey(lines.get(0)), "simplebuilding.gui.locked",
                "the first tooltip line of a locked octant");
        helper.assertValueEqual(List.of(lines.get(1).getString(), lines.get(2).getString()),
                List.of("10, 20, 30", "-40, 50, -60"),
                "the corner lines of a locked octant");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (d) SCROLL HANDLER GUARDS
    // =====================================================================================

    /**
     * {@code handleOctantScroll} reads {@code player.getMainHandItem()} and writes the item back
     * only when it actually changed something. Three guards come out of that, and
     * {@code NetworkHandlerTests} exercises none of them because it always hands the handler a
     * well formed octant in the main hand:
     *
     * <ol>
     *   <li>an octant in the <em>off</em> hand is not the one being configured - the screen and
     *       the mouse mixin are main hand only, so a scroll must not reach it;</li>
     *   <li>a payload that arrives while the player holds something else must not write octant
     *       data onto that item;</li>
     *   <li>a corner nudge on an octant that has no corners yet must leave the item completely
     *       alone - that is what the {@code changed} flag is for, and without it every stray
     *       scroll would attach an empty {@code custom_data} component to a fresh octant.</li>
     * </ol>
     *
     * <p>The last step is the control: the same payload, with the octant in the main hand, has to
     * land. Without it a handler that had become a no-op would pass the three guards.
     *
     * <p>This test says nothing about a locked octant - see "Known defects" in the class javadoc.
     *
     * <p>What breaks this test: {@code getMainHandItem()} turning into a search over both hands or
     * the whole inventory, the {@code instanceof OctantItem} guard going away, or the
     * {@code changed} flag being dropped so the handler always writes the tag back.
     */
    public static void octantScrollPacketsOnlyEverTouchTheMainHand(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // (1) octant in the off hand only
        ItemStack offHand = new ItemStack(ModItems.OCTANT);
        setSelection(offHand, FIRST_CORNER, SECOND_CORNER, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, true, true, false), player);
        helper.assertFalse(customData(offHand).contains("Shape"),
                "an alt scroll changed the shape of an octant that is only in the off hand");
        assertSelection(helper, offHand, FIRST_CORNER, SECOND_CORNER,
                "a corner nudge moved an octant that is only in the off hand");

        // (2) something else in the main hand
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
        helper.assertFalse(pickaxe.has(DataComponents.CUSTOM_DATA),
                "the scroll handler wrote octant data onto a diamond pickaxe");

        // (3) a corner nudge with nothing to nudge
        ItemStack fresh = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, fresh);
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(2, true, true, false), player);
        helper.assertFalse(fresh.has(DataComponents.CUSTOM_DATA),
                "a corner nudge on an octant without a selection attached an empty custom_data "
                        + "component to it");

        // (4) control: in the main hand, the very same payload has to land
        ModMessageHandlers.handleOctantScroll(new OctantScrollPayload(1, false, false, true), player);
        helper.assertValueEqual(customData(fresh).getString("Shape").orElse(""),
                SelectionShape.values()[1].name(),
                "control: an alt scroll on the main hand octant did nothing, so the three guards "
                        + "above prove nothing");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (e) WHERE COLOURED OCTANTS COME FROM AND GO
    // =====================================================================================

    /**
     * Every one of the sixteen dyeing recipes, resolved the way a crafting table resolves it: put
     * a plain octant and the matching dye into a grid and ask the live {@code RecipeManager} what
     * comes out.
     *
     * <p>Going through the lookup instead of reading the recipe's own fields is deliberate - it
     * fails for a missing file, a changed ingredient, a changed result and a recipe that has been
     * shadowed by another one alike. All sixteen are walked because they are generated in a
     * {@code DyeColor} loop, so they fail as a group or one at a time, never in a way a sample
     * would catch.
     *
     * <p>The last step is the negative: a <em>coloured</em> octant plus a dye must craft nothing.
     * The recipes name {@code simplebuilding:octant} as their ingredient, so re-dyeing is not a
     * thing, and without that check a recipe that had grown a tag ingredient would look correct.
     *
     * <p>What breaks this test: a dropped or renamed {@code octant_<colour>_from_dye} recipe, a
     * changed result, a changed dye, a datagen run that was never re-executed after a colour was
     * added, or an ingredient widened to a tag.
     */
    public static void dyeingRecipesProduceEveryColouredOctant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipes = level.getServer().getRecipeManager();
        List<String> problems = new ArrayList<>();

        for (DyeColor color : DyeColor.values()) {
            Identifier id = Identifier.fromNamespaceAndPath(
                    SimpleBuildingGameTests.MOD_ID, "octant_" + color.getName() + "_from_dye");
            Item expected = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (expected == null) {
                problems.add(id + ": there is no item for " + color.getName() + " to craft");
                continue;
            }

            CraftingInput input = CraftingInput.of(2, 1, List.of(
                    new ItemStack(ModItems.OCTANT), new ItemStack(DyeItem.byColor(color))));
            Optional<RecipeHolder<CraftingRecipe>> matched =
                    recipes.getRecipeFor(RecipeType.CRAFTING, input, level);
            if (matched.isEmpty()) {
                problems.add(id + ": an octant plus " + color.getName() + " dye crafts nothing");
                continue;
            }
            Identifier matchedId = matched.get().id().identifier();
            if (!id.equals(matchedId)) {
                problems.add(id + ": that pair crafts " + matchedId + " instead");
                continue;
            }
            // MC 1.21.11: Recipe#assemble still takes the registry access alongside the input.
            ItemStack crafted = matched.get().value().assemble(input, level.registryAccess());
            if (!crafted.is(expected) || crafted.getCount() != 1) {
                problems.add(id + ": crafts " + crafted.getCount() + "x " + crafted.getItem()
                        + " instead of one " + expected);
            }
        }
        helper.assertTrue(problems.isEmpty(), "dyeing recipe problems: " + problems);

        CraftingInput reDye = CraftingInput.of(2, 1, List.of(
                new ItemStack(ModItems.COLORED_OCTANT_ITEMS.get(DyeColor.RED)),
                new ItemStack(DyeItem.byColor(DyeColor.BLUE))));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, reDye, level).isEmpty(),
                "a red octant plus blue dye crafts something; the recipes are supposed to take the "
                        + "plain octant only, and re-dyeing goes through the cauldron");

        TestCleanup.succeed(helper);
    }

    /**
     * The way back: dipping a coloured octant into a water cauldron hands the player a plain one
     * that keeps the whole selection, counts as a wash in the statistics and costs one water
     * level. <b>All sixteen colours are dipped</b>, because the interaction is registered inside a
     * {@code DyeColor} loop ({@code SimplebuildingNeoForge#registerCauldronBehavior} and its Forge
     * and Fabric twins) - a colour falling out of that loop leaves the feature dead for one paint
     * job and alive for the other fifteen, which a single sample would never see. Nothing else in
     * the suite touches a cauldron at all.
     *
     * <p>The dips run through {@code BlockState#useItemOn}, i.e. through
     * {@code AbstractCauldronBlock#useItemOn} and the interaction map the block was built with -
     * so they only pass if the loader's entry point really did put the interaction into
     * {@code CauldronInteraction.WATER.map()}. On MC 1.21.11 that map is public and mutable, so
     * both entry points write into it directly and the 26.2 dispatcher accessor mixin has no twin
     * here - an entry point that stopped running fails in this test instead of going quietly dead
     * in game. The fill level is only worth checking once; the wash statistic is counted across
     * the whole loop, so a dropped {@code awardStat} turns sixteen into zero.
     *
     * <p>The second half is about the plain octant, and it is deliberately <em>not</em> a dip.
     * Going through the block would prove nothing: the loop registers the sixteen dyed items and
     * only those, so the interaction map holds no entry for a plain octant and falls back to its
     * own {@code defaultReturnValue}, which answers {@code TRY_WITH_EMPTY_HAND} whatever the mod
     * does - the {@code item == ModItems.OCTANT} guard inside the lambda is unreachable from there
     * and could be deleted without a dip ever noticing. So both halves of that are driven where
     * they can actually fail: the map is asked whether it holds the plain octant at all (it must
     * not, i.e. the plain item was never registered as a key), and the interaction registered for
     * a <em>dyed</em> octant is then invoked directly with a plain one, where the guard is the
     * only thing between the call and a wash.
     *
     * <p>Only the block half of a click is driven, not the item half that
     * {@code ServerPlayerGameMode} would run afterwards. In game a plain octant dipped into a
     * cauldron therefore also stores the cauldron as its first corner; that is the corner
     * selection doing its normal job and belongs to
     * {@code ItemBehaviourTests#octantStoresBothCornersAndRespectsTheLock}, not here.
     *
     * <p>What breaks this test: the interaction not being registered for one of the sixteen
     * colours, the {@code custom_data} copy going away (the player loses the measured corners),
     * the {@code item == ModItems.OCTANT} guard going away, the plain octant being registered as
     * an interaction map key of its own, the fill level no longer being lowered, or the statistic
     * being dropped.
     */
    public static void waterCauldronWashesTheColourOffAnOctant(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos cauldron = new BlockPos(2, 1, 2);

        // --- every colour goes in, each with a locked selection on it ---
        int washesBefore = washCount(player);
        boolean levelChecked = false;
        List<String> problems = new ArrayList<>();

        for (DyeColor color : DyeColor.values()) {
            String name = "octant_" + color.getName();
            OctantItem dyed = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (dyed == null) {
                problems.add(name + " is not registered at all, so it was never dipped");
                continue;
            }

            fillCauldron(helper, cauldron, 3);
            ItemStack stack = new ItemStack(dyed);
            setSelection(stack, FIRST_CORNER, SECOND_CORNER, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            InteractionResult washed = useBlockWithHeldItem(helper, player, cauldron);
            if (washed != InteractionResult.SUCCESS) {
                // TRY_WITH_EMPTY_HAND is exactly what an item the dispatcher does not know gets.
                problems.add(name + " answered " + washed + " instead of SUCCESS, so no cauldron "
                        + "interaction is registered for it");
                continue;
            }

            ItemStack afterwards = player.getMainHandItem();
            if (!afterwards.is(ModItems.OCTANT)) {
                problems.add(name + " washed into " + afterwards.getItem()
                        + " instead of the plain octant");
                continue;
            }
            CompoundTag carried = customData(afterwards);
            if (!Arrays.equals(corner(carried, "Pos1"), FIRST_CORNER)
                    || !Arrays.equals(corner(carried, "Pos2"), SECOND_CORNER)
                    || !carried.getBooleanOr("Locked", false)) {
                problems.add(name + " lost data in the wash; the whole custom_data component is "
                        + "supposed to carry over untouched, it now holds " + carried);
            }

            if (!levelChecked) {
                levelChecked = true;
                helper.assertValueEqual(waterLevel(helper, cauldron), 2,
                        "the water level after washing one " + name);
            }
        }
        helper.assertTrue(problems.isEmpty(), "cauldron wash problems: " + problems);
        helper.assertValueEqual(washCount(player) - washesBefore, DyeColor.values().length,
                "the number of washes the statistics counted for " + DyeColor.values().length
                        + " dipped octants");

        // --- the plain octant is not an interaction key, and the guard says so as well ---
        // MC 1.21.11 has neither a dispatcher nor a CauldronInteraction.DEFAULT constant: the
        // entries live in CauldronInteraction.WATER.map(), a fastutil map whose defaultReturnValue
        // is the TRY_WITH_EMPTY_HAND fallback. get() therefore never returns null, so "was never
        // registered" has to be asked as containsKey - the 26.2 comparison against DEFAULT is the
        // same question in that line's words.
        Map<Item, CauldronInteraction> waterInteractions = CauldronInteraction.WATER.map();
        helper.assertFalse(waterInteractions.containsKey(ModItems.OCTANT),
                "the plain octant has a water cauldron interaction of its own; the registration "
                        + "loop is supposed to put the sixteen dyed items in and nothing else");

        Item redOctant = ModItems.COLORED_OCTANT_ITEMS.get(DyeColor.RED);
        helper.assertTrue(waterInteractions.containsKey(redOctant),
                "nothing is registered for a red octant, so the guard below would be driven "
                        + "through vanilla's fallback and could not fail");
        CauldronInteraction wash = waterInteractions.get(redOctant);

        fillCauldron(helper, cauldron, 3);
        ItemStack plain = new ItemStack(ModItems.OCTANT);
        setSelection(plain, FIRST_CORNER, null, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        int washesBeforePlain = washCount(player);

        BlockPos absolute = helper.absolutePos(cauldron);
        InteractionResult refused = wash.interact(helper.getLevel().getBlockState(absolute),
                helper.getLevel(), absolute, player, InteractionHand.MAIN_HAND, plain);
        helper.assertTrue(refused == InteractionResult.PASS,
                "the wash interaction claimed a plain octant (" + refused + "); the "
                        + "item == ModItems.OCTANT guard is the only thing that stops an already "
                        + "plain octant from being swapped for a fresh one");
        helper.assertTrue(player.getMainHandItem().is(ModItems.OCTANT),
                "the refused wash swapped the plain octant for "
                        + player.getMainHandItem().getItem());
        assertSelection(helper, player.getMainHandItem(), FIRST_CORNER, null,
                "the refused wash changed the stored selection");
        helper.assertValueEqual(waterLevel(helper, cauldron), 3,
                "the water level after the refused wash; that call is supposed to be a no-op");
        helper.assertValueEqual(washCount(player) - washesBeforePlain, 0,
                "the refused wash counted as a wash in the statistics");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (f) THE CHEST ENTRIES - THE HALF ROLLING THEM CANNOT SEE
    // =====================================================================================

    /**
     * <b>Which of the three chest octants come out enchanted</b> - the one thing about those
     * entries that no amount of rolling reveals.
     *
     * <p>That the octant is in the ancient city, the nether fortress and the pillager outpost pool
     * at all is {@code ConfigOptionTests}' job: its {@code EXPECTED_LOOT} lists
     * {@code simplebuilding:octant} under all three keys and rolls each pool 2048 times, with the
     * bonus chest in {@code UNTOUCHED_TABLES} as the control. That test compares item ids, so an
     * octant that fell out of a chest stripped of its {@code enchant_randomly} function would look
     * exactly like one that kept it. Hence this test, and hence the shape of it: the pools the mod
     * hands to the loot loader are serialised through {@code LootPool.CODEC} - the entry list of a
     * built pool is private, but its codec output is exact and needs no dice - and the octant
     * entry's {@code functions} array is read out by name.
     *
     * <p>Naming the function is what gives this teeth. {@code entry.has("functions")} would stay
     * green if {@code EnchantRandomlyFunction} were swapped for a {@code set_count} or a
     * {@code set_name}, which is the opposite of what the ancient city and nether fortress entries
     * are for: those two are the dangerous chests, and their octant is the reward that arrives
     * ready to use. The outpost is the counterweight - its octant is handed out plain - and it
     * doubles as the control that the function lookup is not simply answering yes. Weights are
     * deliberately not asserted: they are balancing numbers.
     *
     * <p>Two more controls stop the entry lookup from being a recorder that says yes to any pool:
     * the woodland mansion, which the mod <em>does</em> add a pool to but fills with wands and
     * cores, and the bonus chest, which the mod never touches at all.
     *
     * <p>What breaks this test: the random enchantment falling off the ancient city or nether
     * fortress octant, being swapped for another loot function, growing onto the outpost octant,
     * an octant entry dropped from one of the three pools, a pool moved behind a different loot
     * table key, or {@code worldGen.enableLootTableChanges} defaulting to off.
     */
    public static void chestOctantsAreEnchantedInTheTwoDangerousChestsOnly(GameTestHelper helper) {
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges,
                "worldGen.enableLootTableChanges is switched off for this run, so the mod adds no "
                        + "pools at all and nothing below could find anything either way");

        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        JsonObject ancientCity = octantEntry(helper, registries, BuiltInLootTables.ANCIENT_CITY);
        helper.assertTrue(ancientCity != null, "the octant is no longer in the ancient city loot");
        helper.assertTrue(functionNamed(ancientCity, ENCHANT_RANDOMLY) != null,
                "the ancient city octant carries no " + ENCHANT_RANDOMLY + " function; it is "
                        + "supposed to come out randomly enchanted, entry is " + ancientCity);

        JsonObject netherBridge = octantEntry(helper, registries, BuiltInLootTables.NETHER_BRIDGE);
        helper.assertTrue(netherBridge != null, "the octant is no longer in the nether fortress loot");
        helper.assertTrue(functionNamed(netherBridge, ENCHANT_RANDOMLY) != null,
                "the nether fortress octant carries no " + ENCHANT_RANDOMLY + " function; it is "
                        + "supposed to come out randomly enchanted, entry is " + netherBridge);

        JsonObject outpost = octantEntry(helper, registries, BuiltInLootTables.PILLAGER_OUTPOST);
        helper.assertTrue(outpost != null, "the octant is no longer in the pillager outpost loot");
        helper.assertTrue(functionNamed(outpost, ENCHANT_RANDOMLY) == null,
                "the pillager outpost octant became enchanted too; it is the one that is handed "
                        + "out plain, and it is what makes the two answers above mean anything, "
                        + "entry is " + outpost);

        // Controls: a table the mod edits but puts no octant in, and one it never touches.
        helper.assertTrue(octantEntry(helper, registries, BuiltInLootTables.WOODLAND_MANSION) == null,
                "the recorder finds an octant in the woodland mansion pool, which the mod fills "
                        + "with wands and cores; it is answering yes to any pool it is given");
        helper.assertTrue(octantEntry(helper, registries, BuiltInLootTables.SPAWN_BONUS_CHEST) == null,
                "the recorder finds an octant even in a loot table the mod never edits");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (g) MARKER: THE OCTANT BUILDS NOTHING
    // =====================================================================================

    /**
     * The octant measures; it never places or removes a block. That is easy to state and nowhere
     * written down, and three of the settings the octant screen sends - {@code Hollow},
     * {@code LayerMode} and {@code FillOrder} - are stored in the item and read back by nothing
     * but the screen that sent them ({@code OctantScreen#loadDataFromStack}, which restores its
     * own controls). No server path and no placement path looks at them. This is the marker for
     * that, in the same spirit as
     * {@code EnchantmentEffectTests#coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown}: it is
     * <em>not</em> a claim that the current state is finished, it is a tripwire that goes off the
     * moment the octant grows a build step, at which point a real behaviour test has to replace
     * it.
     *
     * <p>{@code Orientation} is asserted alongside them but is <em>not</em> part of that claim: it
     * is read back, by {@code BlockHighlightRenderer#drawOctantHighlights}, which turns it into a
     * {@link Direction} and rotates the cylinder, ellipse, pyramid and triangle preview with it.
     * That is client code on both loaders, so what it does with the value is out of reach here -
     * see "Not covered" in the class javadoc. All this test says about it is the same thing it
     * says about the other three: a corner click must not drop it.
     *
     * <p>The full sequence a player would use is driven: configure the switches, click both
     * corners, then the sneaking air click - the one gesture a build trigger would most plausibly
     * be hung on. Every block in the selected box is compared afterwards, in absolute coordinates
     * (never through {@code relativePos}, which is not the inverse of {@code absolutePos}).
     *
     * <p>The second assertion is the other half of "written by the screen, kept by the item": all
     * four settings have to survive the corner clicks untouched. {@code useOn} copies the whole
     * tag, edits one corner and writes it back, so a rewrite that rebuilt the tag from scratch
     * would silently drop settings the player had made - and the reader would be a screen or a
     * renderer, neither of which can complain.
     *
     * <p>What breaks this test: the octant placing or clearing anything, or a corner click no
     * longer preserving the rest of the item's data.
     */
    public static void theOctantOnlyMeasuresAndPlacesNothing(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);

        // A hollow 3x2x3 box: air everywhere, one block at each of the two corners to click on.
        BlockPos firstCorner = new BlockPos(1, 1, 1);
        BlockPos secondCorner = new BlockPos(3, 2, 3);
        forEachInBox(firstCorner, secondCorner, pos -> helper.setBlock(pos, Blocks.AIR));
        helper.setBlock(firstCorner, Blocks.STONE);
        helper.setBlock(secondCorner, Blocks.STONE);

        // Everything the screen can send, including the three settings nothing reads.
        ModMessageHandlers.handleOctantConfigure(new OctantConfigurePayload(
                Optional.empty(), Optional.empty(), SelectionShape.SPHERE.name(),
                false, 1, true, true, FillOrder.TOP_DOWN.name()), player);

        player.setShiftKeyDown(false);
        useItemOnBlock(helper, player, firstCorner);
        player.setShiftKeyDown(true);
        useItemOnBlock(helper, player, secondCorner);

        CompoundTag afterCorners = customData(octant);
        helper.assertTrue(afterCorners.contains("Pos1") && afterCorners.contains("Pos2"),
                "the two corner clicks stored no selection at all, so the box assertions below "
                        + "would pass for an octant that was never even used: " + afterCorners);
        helper.assertTrue(afterCorners.getBooleanOr("Hollow", false)
                        && afterCorners.getBooleanOr("LayerMode", false)
                        && FillOrder.TOP_DOWN.name().equals(afterCorners.getString("FillOrder").orElse(""))
                        && afterCorners.getIntOr("Orientation", -1) == 1,
                "the corner clicks dropped settings the screen had made; the item now holds "
                        + afterCorners);

        assertNothingBuilt(helper, firstCorner, secondCorner, "after both corner clicks");

        // The sneaking air click: the reset, and the only other gesture the item reacts to.
        octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertTrue(customData(octant).isEmpty(),
                "the sneaking air click did not clear the selection, so the sequence above is not "
                        + "the one this test thinks it drove");

        assertNothingBuilt(helper, firstCorner, secondCorner, "after the sneaking air click");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * A creative mock player, handed back to the server no matter how the test ends. None of the
     * branches here look at the game mode - the octant only checks {@code instabuild} for its
     * durability, which {@code ConsumptionAndDurabilityTests} owns.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** Writes a selection straight into the item, the way the screen's payload would. */
    private static void setSelection(ItemStack stack, int[] pos1, int[] pos2, boolean locked) {
        CompoundTag nbt = new CompoundTag();
        if (pos1 != null) {
            nbt.putIntArray("Pos1", pos1.clone());
        }
        if (pos2 != null) {
            nbt.putIntArray("Pos2", pos2.clone());
        }
        nbt.putBoolean("Locked", locked);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    private static int[] corner(CompoundTag nbt, String key) {
        return nbt.getIntArray(key).orElse(null);
    }

    /** Both corners at once; {@code null} means "this corner must not be stored at all". */
    private static void assertSelection(GameTestHelper helper, ItemStack stack,
                                        int[] pos1, int[] pos2, String message) {
        CompoundTag nbt = customData(stack);
        helper.assertTrue(Arrays.equals(corner(nbt, "Pos1"), pos1),
                message + " - Pos1 is " + Arrays.toString(corner(nbt, "Pos1"))
                        + ", expected " + Arrays.toString(pos1));
        helper.assertTrue(Arrays.equals(corner(nbt, "Pos2"), pos2),
                message + " - Pos2 is " + Arrays.toString(corner(nbt, "Pos2"))
                        + ", expected " + Arrays.toString(pos2));
    }

    /** A plain octant carrying nothing but the given {@code Shape} string. */
    private static ItemStack withShape(String shapeName) {
        ItemStack stack = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Shape", shapeName);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return stack;
    }

    /** The translation key behind a component, or a readable complaint if it carries none. */
    private static String translationKey(Component component) {
        return component.getContents() instanceof TranslatableContents translatable
                ? translatable.getKey()
                : "<not a translatable component: " + component.getContents() + ">";
    }

    /** The tooltip lines the item itself appends, as components. */
    @SuppressWarnings("deprecation")
    private static List<Component> tooltipComponents(GameTestHelper helper, ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()),
                TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
        return lines;
    }

    /** The same lines as plain text, which drops the colours a client would render. */
    private static List<String> tooltip(GameTestHelper helper, ItemStack stack) {
        return tooltipComponents(helper, stack).stream().map(Component::getString).toList();
    }

    /** The hit a player produces by aiming at the middle of a block's top face. */
    private static BlockHitResult topFaceHit(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        return new BlockHitResult(hit, Direction.UP, pos, false);
    }

    /**
     * Right clicks the top face of a block on the <em>item</em> side, which is where the octant's
     * corner selection lives. This is the second half of what
     * {@code ServerPlayerGameMode#useItemOn} does; the block gets asked first there, and answers
     * {@code TRY_WITH_EMPTY_HAND} for anything without an interaction of its own.
     */
    private static InteractionResult useItemOnBlock(GameTestHelper helper, ServerPlayer player,
                                                    BlockPos relativePos) {
        return player.getMainHandItem().getItem().useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, topFaceHit(helper, relativePos)));
    }

    /**
     * Right clicks the top face of a block on the <em>block</em> side. That is the path a cauldron
     * interaction sits on: {@code AbstractCauldronBlock#useItemOn} asks the dispatcher the block
     * was built with, which is the map the mod writes into.
     */
    private static InteractionResult useBlockWithHeldItem(GameTestHelper helper, ServerPlayer player,
                                                          BlockPos relativePos) {
        ServerLevel level = helper.getLevel();
        return level.getBlockState(helper.absolutePos(relativePos)).useItemOn(
                player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                topFaceHit(helper, relativePos));
    }

    private static void fillCauldron(GameTestHelper helper, BlockPos relativePos, int level) {
        helper.setBlock(relativePos, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, level));
    }

    private static int waterLevel(GameTestHelper helper, BlockPos relativePos) {
        BlockState state = helper.getBlockState(relativePos);
        helper.assertTrue(state.is(Blocks.WATER_CAULDRON),
                "the cauldron is " + state + " instead of a water cauldron");
        return state.getValue(LayeredCauldronBlock.LEVEL);
    }

    /** How often this player has washed something; the octant awards the vanilla armour wash. */
    private static int washCount(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.CLEAN_ARMOR));
    }

    private static void forEachInBox(BlockPos from, BlockPos to, Consumer<BlockPos> action) {
        for (int x = Math.min(from.getX(), to.getX()); x <= Math.max(from.getX(), to.getX()); x++) {
            for (int y = Math.min(from.getY(), to.getY()); y <= Math.max(from.getY(), to.getY()); y++) {
                for (int z = Math.min(from.getZ(), to.getZ()); z <= Math.max(from.getZ(), to.getZ()); z++) {
                    action.accept(new BlockPos(x, y, z));
                }
            }
        }
    }

    /**
     * Every block of the selected box is still what the test put there: stone at the two clicked
     * corners, air everywhere else. Compared in absolute coordinates, because
     * {@code GameTestHelper#relativePos} is not the inverse of {@code absolutePos}.
     */
    private static void assertNothingBuilt(GameTestHelper helper, BlockPos firstCorner,
                                           BlockPos secondCorner, String when) {
        List<String> changed = new ArrayList<>();
        forEachInBox(firstCorner, secondCorner, pos -> {
            boolean isCorner = pos.equals(firstCorner) || pos.equals(secondCorner);
            BlockState state = helper.getLevel().getBlockState(helper.absolutePos(pos));
            boolean matches = isCorner ? state.is(Blocks.STONE) : state.isAir();
            if (!matches) {
                changed.add(helper.absolutePos(pos).toShortString() + " is " + state);
            }
        });
        helper.assertTrue(changed.isEmpty(),
                "the octant changed blocks inside its own selection " + when
                        + "; it is a measuring tool and places nothing: " + changed);
    }

    // --- loot ---------------------------------------------------------------------------

    /**
     * Runs the mod's loot hook for one table, serialises every pool it hands over and returns the
     * plain octant's entry from the first pool that holds one, or {@code null}. The entry list of a
     * built pool is private, but its codec output is exact - and unlike rolling the pool it needs
     * no dice and no statistics. A loot item entry carries its item id under {@code "name"} and
     * its loot functions under {@code "functions"}; both spellings are vanilla's own.
     */
    private static JsonObject octantEntry(GameTestHelper helper, HolderLookup.Provider registries,
                                          ResourceKey<LootTable> table) {
        PoolCollector collector = new PoolCollector();
        ModLootTableModifications.apply(table, collector, registries);

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        for (LootPool pool : collector.pools) {
            JsonElement encoded = LootPool.CODEC.encodeStart(ops, pool)
                    .getOrThrow(message -> helper.assertionException(
                            "a loot pool the mod adds to " + table.identifier()
                                    + " cannot be serialised: " + message));
            if (!encoded.isJsonObject()) {
                continue;
            }
            JsonElement entries = encoded.getAsJsonObject().get("entries");
            if (entries == null || !entries.isJsonArray()) {
                continue;
            }
            for (JsonElement entry : entries.getAsJsonArray()) {
                if (!entry.isJsonObject()) {
                    continue;
                }
                JsonObject object = entry.getAsJsonObject();
                JsonElement name = object.get("name");
                if (name != null && name.isJsonPrimitive() && OCTANT_ID.equals(name.getAsString())) {
                    return object;
                }
            }
        }
        return null;
    }

    /**
     * The loot function of the given id hanging off one serialised entry, or {@code null}. The
     * spelling - a {@code "functions"} array of objects each naming its type under
     * {@code "function"} - is vanilla's own, from {@code LootItemFunctions.CODEC}.
     */
    private static JsonObject functionNamed(JsonObject entry, String id) {
        JsonElement functions = entry.get("functions");
        if (functions == null || !functions.isJsonArray()) {
            return null;
        }
        for (JsonElement element : functions.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject function = element.getAsJsonObject();
            JsonElement type = function.get("function");
            if (type != null && type.isJsonPrimitive() && id.equals(type.getAsString())) {
                return function;
            }
        }
        return null;
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
}
