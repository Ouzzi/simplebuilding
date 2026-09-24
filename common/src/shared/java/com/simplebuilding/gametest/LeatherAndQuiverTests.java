package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.ModTags;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;

/**
 * The leather sheet and the reinforced quiver, and what an upgrade carries across.
 *
 * <p>The leather sheet is a plain material made from nine leather; it is an ingredient of the
 * reinforced bundle, the reinforced quiver and the backpacks. The reinforced quiver sits between
 * the plain and the netherite quiver: it is crafted from the plain quiver with a leather sheet, a
 * diamond pebble, string and a copper nugget, holds 3/2 of a stack of arrows (96), and is the only
 * base the netherite quiver smithing recipe accepts.
 *
 * <p>Both reinforced recipes are of the mod's {@code simplebuilding:reinforced_bundle} type, which
 * carries the whole component patch of the container in the grid over to the result - contents,
 * enchantments and name - exactly like vanilla's smithing transform does for the tiers above. That
 * is what {@link #upgradesKeepContentsEnchantmentsAndName} pins; before 2026-09 the reinforced
 * bundle was a plain {@code crafting_shaped} recipe and emptied the vanilla bundle.
 *
 * <p>{@code BundleWiringTests#bundleRecipesCraftTheBaseAndUpgradeItTierByTier} already drives the
 * reinforced bundle pattern, {@code QuiverTests#capacityDropsTheBundleBonusAndFollowsTierAndEnchantments}
 * the 96 arrows of a fresh reinforced quiver, and
 * {@code BundleWiringTests#containerEnchantmentsAcceptTheBundlesTheyAreMeantFor} the five container
 * enchantments on it; none of that is repeated here.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>Explosions.</b> The reinforced quiver is not explosion-proof, like the plain one; that is
 *       {@code ItemEntityMixin}'s list, whose positive cases {@code BundleWiringTests} drives.</li>
 *   <li><b>The tooltip bar</b>, which reads full at 64 of 96 arrows - a client-side caveat the wiki
 *       documents.</li>
 * </ul>
 */
public final class LeatherAndQuiverTests {

    /** The reinforced quiver: 3/2 of a vanilla stack of arrows. */
    private static final int REINFORCED_QUIVER_ARROWS = 96;

    private LeatherAndQuiverTests() {
    }

    /**
     * A leather sheet takes nine leather, in a full 3x3 grid, and nothing else.
     *
     * <ul>
     *   <li><b>Nine leather</b> craft exactly one leather sheet under the recipe id
     *       {@code simplebuilding:leather_sheet}.</li>
     *   <li><b>Eight leather</b> with the bottom right corner empty craft nothing. The gap sits where
     *       no vanilla leather armour pattern has one, so a match here could only be the sheet.</li>
     *   <li><b>Eight leather and a rabbit hide</b> craft nothing: the key is leather, not "something
     *       leathery".</li>
     * </ul>
     * The sheet itself is an ordinary material that stacks to 64. And the reinforced bundle's pattern
     * from before 2026-09 - string, two copper nuggets and three loose leather - crafts nothing any
     * more: the sheet and the diamond pebble took their place.
     *
     * <p>What breaks this test: a changed pattern, key or count in {@code recipe/leather_sheet.json},
     * a widened ingredient, a sheet registered unstackable, and the old reinforced bundle recipe
     * coming back next to the new one.
     */
    public static void leatherSheetTakesExactlyNineLeather(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item l = Items.LEATHER;

        ItemStack sheet = craft(helper, level, grid3x3(l, l, l, l, l, l, l, l, l),
                "nine leather", "simplebuilding:leather_sheet");
        helper.assertTrue(sheet.is(ModItems.LEATHER_SHEET),
                "nine leather craft " + sheet + " instead of a leather sheet");
        helper.assertValueEqual(sheet.getCount(), 1, "leather sheets per nine leather");
        helper.assertValueEqual(sheet.getMaxStackSize(), 64, "stack size of the leather sheet");

        assertCraftsNothing(helper, level, grid3x3(l, l, l, l, l, l, l, l, null),
                "eight leather with the corner empty still craft something, so the leather sheet no "
                        + "longer needs all nine");
        assertCraftsNothing(helper, level, grid3x3(l, l, l, l, l, l, l, l, Items.RABBIT_HIDE),
                "eight leather and a rabbit hide craft something, so the leather sheet's key accepts more "
                        + "than leather");

        // The sheet replaced loose leather in the reinforced bundle: the pattern before 2026-09 -
        // string, two copper nuggets around the bundle, three leather - must not craft any more.
        assertCraftsNothing(helper, level, grid3x3(null, Items.STRING, null,
                        Items.COPPER_NUGGET, Items.BUNDLE, Items.COPPER_NUGGET, l, l, l),
                "the old reinforced bundle pattern with three leather and two copper nuggets still crafts "
                        + "something; the leather sheet was supposed to replace it");

        helper.succeed();
    }

    /**
     * The reinforced quiver is crafted from the plain quiver: {@code " SD" / "SXN" / "Q  "} - string,
     * diamond pebble, leather sheet, copper nugget and the quiver in the bottom left corner.
     *
     * <ul>
     *   <li>The documented grid crafts exactly one reinforced quiver.</li>
     *   <li>A vanilla bundle in the quiver's place crafts nothing: the plain quiver is the base, so
     *       it is not a dead end once the netherite quiver needs the reinforced one.</li>
     *   <li>Without the copper nugget the grid crafts nothing.</li>
     *   <li>The same items turned upside down craft nothing - the recipe is shaped.</li>
     * </ul>
     *
     * <p>What breaks this test: a changed key or pattern in {@code recipe/reinforced_quiver.json}, a
     * bundle accepted as the base, and a recipe that stops loading.
     */
    public static void reinforcedQuiverCraftsFromThePlainQuiverWithSheetPebbleAndNugget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack crafted = craft(helper, level, quiverGrid(new ItemStack(ModItems.QUIVER), true),
                "string, diamond pebble, leather sheet, copper nugget and a quiver",
                "simplebuilding:reinforced_quiver");
        helper.assertTrue(crafted.is(ModItems.REINFORCED_QUIVER),
                "the reinforced quiver pattern crafts " + crafted + " instead of a reinforced quiver");
        helper.assertValueEqual(crafted.getCount(), 1, "reinforced quivers per craft");

        assertCraftsNothing(helper, level, quiverGrid(new ItemStack(Items.BUNDLE), true),
                "the reinforced quiver pattern with a vanilla bundle in the quiver's place crafts "
                        + "something - the plain quiver is supposed to be the base");
        assertCraftsNothing(helper, level, quiverGrid(new ItemStack(ModItems.QUIVER), false),
                "the reinforced quiver pattern without its copper nugget still crafts something");

        CraftingInput upsideDown = CraftingInput.of(3, 3, List.of(
                new ItemStack(ModItems.QUIVER), ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.STRING), new ItemStack(ModItems.LEATHER_SHEET), new ItemStack(Items.COPPER_NUGGET),
                ItemStack.EMPTY, new ItemStack(Items.STRING), new ItemStack(ModItems.DIAMOND_PEBBLE)));
        assertCraftsNothing(helper, level, upsideDown,
                "the reinforced quiver pattern turned upside down crafts something too, so the recipe "
                        + "is not shaped");

        helper.succeed();
    }

    /**
     * The netherite quiver is smithed from the reinforced quiver and from nothing else, and the chain
     * goes on to the enderite quiver from there.
     *
     * <ul>
     *   <li>Netherite template, reinforced quiver, netherite ingot: one netherite quiver.</li>
     *   <li>Netherite template, <em>plain</em> quiver, netherite ingot: no recipe - the reinforced tier
     *       cannot be skipped.</li>
     *   <li>Enderite template, reinforced quiver, enderite ingot: no recipe either.</li>
     *   <li>Enderite template, netherite quiver, enderite ingot: the enderite quiver, so the tier
     *       above still hangs on the netherite quiver.</li>
     * </ul>
     *
     * <p>What breaks this test: moving the base of {@code netherite_quiver_smithing} back to the plain
     * quiver, adding a second netherite recipe for it, or cutting the chain above.
     */
    public static void netheriteQuiverSmithsOnlyFromTheReinforcedQuiver(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack netherite = smith(helper, level, new SmithingRecipeInput(
                        new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        new ItemStack(ModItems.REINFORCED_QUIVER),
                        new ItemStack(Items.NETHERITE_INGOT)),
                "a reinforced quiver on the netherite template");
        helper.assertTrue(netherite.is(ModItems.NETHERITE_QUIVER),
                "the reinforced quiver on the netherite template smiths " + netherite
                        + " instead of a netherite quiver");

        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,
                        new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                                new ItemStack(ModItems.QUIVER), new ItemStack(Items.NETHERITE_INGOT)), level).isEmpty(),
                "the plain quiver still smiths into a netherite quiver, so the reinforced tier can be "
                        + "skipped");
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,
                        new SmithingRecipeInput(new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                                new ItemStack(ModItems.REINFORCED_QUIVER), new ItemStack(ModItems.ENDERITE_INGOT)), level).isEmpty(),
                "the reinforced quiver smiths straight into an enderite quiver, skipping netherite");

        ItemStack enderite = smith(helper, level, new SmithingRecipeInput(
                        new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                        new ItemStack(ModItems.NETHERITE_QUIVER),
                        new ItemStack(ModItems.ENDERITE_INGOT)),
                "a netherite quiver on the enderite template");
        helper.assertTrue(enderite.is(ModItems.ENDERITE_QUIVER),
                "the netherite quiver on the enderite template smiths " + enderite
                        + " instead of an enderite quiver");

        helper.succeed();
    }

    /**
     * Every upgrade keeps what the container carried: its contents, its enchantments and its name.
     *
     * <ul>
     *   <li><b>Vanilla bundle to reinforced bundle</b> (crafting): five stone, Funnel I and a custom
     *       name all arrive on the reinforced bundle.</li>
     *   <li><b>Quiver to reinforced quiver</b> (crafting): 64 arrows, Funnel I and the name arrive, and
     *       the crafted quiver takes exactly 32 arrows more - it holds the reinforced tier's 96.</li>
     *   <li><b>Reinforced quiver to netherite quiver</b> (smithing): all 96 arrows, Funnel I and the name
     *       arrive.</li>
     * </ul>
     * Funnel is the enchantment of choice because it does not change the capacity; a Drawer or Deep
     * Pockets level would move the 96.
     *
     * <p>What breaks this test: a reinforced recipe that builds a fresh result instead of carrying the
     * container's components over (the old {@code crafting_shaped} behaviour), one that copies only the
     * contents, a recipe type that no longer finds the container in the grid, and a capacity factor
     * other than 3/2 for the reinforced quiver.
     */
    public static void upgradesKeepContentsEnchantmentsAndName(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // --- vanilla bundle -> reinforced bundle ---
        ItemStack vanillaBundle = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable stones = new BundleContents.Mutable(BundleContents.EMPTY);
        stones.tryInsert(new ItemStack(Items.STONE, 5));
        vanillaBundle.set(DataComponents.BUNDLE_CONTENTS, stones.toImmutable());
        vanillaBundle.set(DataComponents.CUSTOM_NAME, Component.literal("Rocks"));
        vanillaBundle.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        helper.assertValueEqual(countIn(vanillaBundle, Items.STONE), 5, "setup: stone in the vanilla bundle");

        CraftingInput bundleGrid = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.STRING), ItemStack.EMPTY,
                new ItemStack(ModItems.DIAMOND_PEBBLE), vanillaBundle, ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(ModItems.LEATHER_SHEET), ItemStack.EMPTY));
        ItemStack reinforcedBundle = craft(helper, level, bundleGrid, "the reinforced bundle pattern",
                "simplebuilding:reinforced_bundle");
        helper.assertTrue(reinforcedBundle.is(ModItems.REINFORCED_BUNDLE),
                "the reinforced bundle pattern crafts " + reinforcedBundle);
        helper.assertValueEqual(countIn(reinforcedBundle, Items.STONE), 5,
                "stone the reinforced bundle kept from the vanilla bundle it was made of");
        assertKeptNameAndFunnel(helper, reinforcedBundle, "Rocks", "the reinforced bundle");

        // --- quiver -> reinforced quiver ---
        ItemStack quiver = new ItemStack(ModItems.QUIVER);
        insertArrows(quiver, 64, player);
        quiver.set(DataComponents.CUSTOM_NAME, Component.literal("Volley"));
        quiver.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        helper.assertValueEqual(countIn(quiver, Items.ARROW), 64, "setup: arrows in the plain quiver");

        ItemStack reinforcedQuiver = craft(helper, level, quiverGrid(quiver, true),
                "the reinforced quiver pattern", "simplebuilding:reinforced_quiver");
        helper.assertValueEqual(countIn(reinforcedQuiver, Items.ARROW), 64,
                "arrows the reinforced quiver kept from the quiver it was made of");
        assertKeptNameAndFunnel(helper, reinforcedQuiver, "Volley", "the reinforced quiver");

        insertArrows(reinforcedQuiver, 64, player);
        helper.assertValueEqual(countIn(reinforcedQuiver, Items.ARROW), REINFORCED_QUIVER_ARROWS,
                "arrows the crafted reinforced quiver holds once it is topped up");

        // --- reinforced quiver -> netherite quiver ---
        ItemStack netheriteQuiver = smith(helper, level, new SmithingRecipeInput(
                        new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), reinforcedQuiver,
                        new ItemStack(Items.NETHERITE_INGOT)),
                "the full reinforced quiver on the netherite template");
        helper.assertTrue(netheriteQuiver.is(ModItems.NETHERITE_QUIVER),
                "the reinforced quiver smiths " + netheriteQuiver + " instead of a netherite quiver");
        helper.assertValueEqual(countIn(netheriteQuiver, Items.ARROW), REINFORCED_QUIVER_ARROWS,
                "arrows the netherite quiver kept from the reinforced quiver it was smithed from");
        assertKeptNameAndFunnel(helper, netheriteQuiver, "Volley", "the netherite quiver");

        helper.succeed();
    }

    /**
     * The reinforced quiver is an ordinary tier: in the three container enchantment tags like every
     * quiver, worn in the chest slot like the plain quiver, and neither fire-proof nor void-protected.
     *
     * <ul>
     *   <li><b>Tags:</b> {@code bundle_enchantable}, {@code extra_inventory_items} and
     *       {@code constructors_touch_enchantable} hold it; {@code void_protected} does not.</li>
     *   <li><b>Fire:</b> it carries no {@code DAMAGE_RESISTANT} component. The netherite quiver does,
     *       which proves the check can say yes.</li>
     *   <li><b>Chest slot:</b> its {@code EQUIPPABLE} component names the chest slot and is not
     *       swappable by right click, the same as the plain quiver's.</li>
     * </ul>
     *
     * <p>What breaks this test: a reinforced quiver dropped from one of the three tags, one that lands
     * in {@code void_protected}, one registered {@code fireResistant()}, and one without the quiver's
     * chest slot.
     */
    public static void reinforcedQuiverIsAnOrdinaryTierInTheContainerTags(GameTestHelper helper) {
        ItemStack reinforced = new ItemStack(ModItems.REINFORCED_QUIVER);

        for (TagKey<Item> tag : List.of(ModTags.Items.BUNDLE_ENCHANTABLE,
                ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE, ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE)) {
            helper.assertTrue(reinforced.is(tag),
                    "the reinforced quiver is missing from " + tag.location());
        }
        helper.assertTrue(!reinforced.is(ModTags.Items.VOID_PROTECTED),
                "the reinforced quiver landed in simplebuilding:void_protected; only the enderite tier is");

        helper.assertTrue(new ItemStack(ModItems.NETHERITE_QUIVER).get(DataComponents.DAMAGE_RESISTANT) != null,
                "the netherite quiver lost its fire resistance, so the check below cannot say yes any more");
        helper.assertTrue(reinforced.get(DataComponents.DAMAGE_RESISTANT) == null,
                "the reinforced quiver resists fire; it is an ordinary tier like the plain quiver");

        Equippable worn = reinforced.get(DataComponents.EQUIPPABLE);
        Equippable plain = new ItemStack(ModItems.QUIVER).get(DataComponents.EQUIPPABLE);
        helper.assertTrue(worn != null && plain != null,
                "the reinforced quiver or the plain quiver has no EQUIPPABLE component any more");
        helper.assertTrue(worn.slot() == EquipmentSlot.CHEST && worn.slot() == plain.slot(),
                "the reinforced quiver is worn in " + worn.slot() + " instead of the chest slot");
        helper.assertTrue(worn.swappable() == plain.swappable(),
                "the reinforced quiver swaps on right click differently from the plain quiver");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** {@code " SD" / "SXN" / "Q  "}, with or without the copper nugget. */
    private static CraftingInput quiverGrid(ItemStack base, boolean withNugget) {
        return CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.STRING), new ItemStack(ModItems.DIAMOND_PEBBLE),
                new ItemStack(Items.STRING), new ItemStack(ModItems.LEATHER_SHEET),
                withNugget ? new ItemStack(Items.COPPER_NUGGET) : ItemStack.EMPTY,
                base, ItemStack.EMPTY, ItemStack.EMPTY));
    }

    /** A 3x3 crafting grid, row by row; {@code null} stands for an empty slot. */
    private static CraftingInput grid3x3(Item... items) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        for (Item item : items) {
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static void insertArrows(ItemStack container, int count, Player player) {
        ((ReinforcedBundleItem) container.getItem()).tryInsertStackFromWorld(container,
                new ItemStack(Items.ARROW, count), player);
    }

    private static int countIn(ItemStack container, Item item) {
        int total = 0;
        for (ItemStack stack : container.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
                .itemCopyStream().toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void assertKeptNameAndFunnel(GameTestHelper helper, ItemStack upgraded, String name, String what) {
        Component customName = upgraded.get(DataComponents.CUSTOM_NAME);
        helper.assertTrue(customName != null && name.equals(customName.getString()),
                what + " lost the name of the container it was made of, it is called " + customName);
        helper.assertValueEqual(upgraded.getEnchantments().getLevel(enchantment(helper, ModEnchantments.FUNNEL)), 1,
                "Funnel level " + what + " kept from the container it was made of");
    }

    private static void assertCraftsNothing(GameTestHelper helper, ServerLevel level, CraftingInput grid, String message) {
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(),
                message);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Crafts {@code grid} through the live recipe manager; the one place a crafting result is assembled. */
    private static ItemStack craft(GameTestHelper helper, ServerLevel level, CraftingInput grid, String what, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(), what + " crafts nothing at all");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched by " + what);
        return match.get().value().assemble(grid);
    }

    /** Smiths {@code input} through the live recipe manager; the one place a smithing result is assembled. */
    private static ItemStack smith(GameTestHelper helper, ServerLevel level, SmithingRecipeInput input, String what) {
        Optional<RecipeHolder<SmithingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(match.isPresent(), what + " matches no smithing recipe at all");
        return match.get().value().assemble(input);
    }
}
