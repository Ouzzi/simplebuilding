package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Smallest possible in-game test: proves the harness runs and that the mod's registrations
 * survived into a live server. Everything else builds on this.
 *
 * <p>"Registered" is asked in the three ways it can fail separately - the item is in the
 * registry, it is there under the id existing saves already contain, and a player can still get
 * hold of it in creative. The first question alone was what this class used to ask, about a
 * single item.
 */
public final class SmokeTests {

    private SmokeTests() {
    }

    /**
     * Item ids that existing worlds already contain, spelled out as literals. An id is not a
     * name, it is the key every save writes into its inventories and shulker boxes: renaming
     * {@code simplebuilding:stone_spatula} silently turns every such stack into nothing on the
     * next load.
     *
     * <p>The spatulas are here because they exist for exactly that reason - they are the
     * pre-rename chisels and are kept registered for old worlds, while the creative tab does not
     * offer them any more. The three bundles are here because they are the items whose contents
     * would be lost with them.
     */
    private static final List<String> PINNED_IDS = List.of(
            "stone_chisel",
            "octant",
            "reinforced_bundle",
            "netherite_bundle",
            "enderite_bundle",
            "stone_spatula",
            "copper_spatula",
            "iron_spatula",
            "gold_spatula",
            "diamond_spatula",
            "netherite_spatula");

    /** The same items, in the same order, as the constants the rest of the mod refers to. */
    private static final List<Item> PINNED_ITEMS = List.of(
            ModItems.STONE_CHISEL,
            ModItems.OCTANT,
            ModItems.REINFORCED_BUNDLE,
            ModItems.NETHERITE_BUNDLE,
            ModItems.ENDERITE_BUNDLE,
            ModItems.STONE_SPATULA,
            ModItems.COPPER_SPATULA,
            ModItems.IRON_SPATULA,
            ModItems.GOLD_SPATULA,
            ModItems.DIAMOND_SPATULA,
            ModItems.NETHERITE_SPATULA);

    /**
     * Items that have to stay reachable in creative. Deliberately not "all of them": the
     * spatulas are legacy items and are meant to be missing from the tab, so freezing the whole
     * tab content here would freeze that decision as well.
     */
    private static final List<Item> TAB_ITEMS = List.of(
            ModItems.STONE_CHISEL,
            ModItems.OCTANT,
            ModItems.REINFORCED_BUNDLE,
            ModItems.NETHERITE_BUNDLE,
            ModItems.ENDERITE_BUNDLE);

    /**
     * Three questions, each of which a registered item can fail on its own: is it in the item
     * registry, is it there under the id old saves use, and can a player still get hold of it.
     *
     * <p>The literal id list is the part {@code DataIntegrityTests} structurally cannot cover.
     * That test derives its expectation from {@code ModItems} in both directions - every declared
     * field has a registry entry, every {@code simplebuilding} registry entry is reachable from a
     * field - so the two sides always agree with each other and a renamed registration stays
     * green there. Only a literal notices it.
     *
     * <p>The last block drives {@code ModItemGroupsContent#populate}, the body every loader hands
     * to its creative tab builder, and asks whether the items are actually offered. Registration
     * and tab membership are independent: a deleted {@code entries.accept} line leaves every
     * registry test in the tree green and makes the item unobtainable without commands.
     *
     * <p>What breaks this test: a renamed or dropped registry id (including a tidied up one), an
     * item removed from {@code ModItems}, a deleted {@code entries.accept} line, and a
     * {@code populate} that throws or returns early.
     */
    public static void modItemsAreRegistered(GameTestHelper helper) {
        // Shape guard: the two lists below are read in lockstep, so a half finished edit has to
        // fail here rather than silently checking fewer items than it looks like.
        helper.assertValueEqual(PINNED_ITEMS.size(), PINNED_IDS.size(),
                "test setup broken: length of the pinned item list against the pinned id list");

        for (int i = 0; i < PINNED_ITEMS.size(); i++) {
            Item item = PINNED_ITEMS.get(i);
            String expectedPath = PINNED_IDS.get(i);
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            helper.assertTrue(id != null && SimpleBuildingGameTests.MOD_ID.equals(id.getNamespace()),
                    "the item behind ModItems." + expectedPath.toUpperCase() + " is not registered in the "
                            + SimpleBuildingGameTests.MOD_ID + " namespace, it came back as " + id);
            helper.assertTrue(expectedPath.equals(id.getPath()),
                    "the item that used to be " + SimpleBuildingGameTests.MOD_ID + ":" + expectedPath
                            + " is registered as " + id + " now; every stack of it in an existing world "
                            + "is dropped on the next load");
        }

        // --- and now the creative tab, which is a second, independent gate ---
        List<ItemStack> offered = new ArrayList<>();
        ModItemGroupsContent.populate(
                (CreativeModeTab.Output) (stack, visibility) -> offered.add(stack),
                helper.getLevel().registryAccess());

        helper.assertTrue(!offered.isEmpty(),
                "ModItemGroupsContent.populate offered no entries at all, so the searches below "
                        + "could not fail");

        for (Item item : TAB_ITEMS) {
            helper.assertTrue(offered.stream().anyMatch(stack -> stack.is(item)),
                    BuiltInRegistries.ITEM.getKey(item) + " is registered but the mod's creative tab "
                            + "does not offer it any more; without a command the player cannot get one");
        }

        helper.succeed();
    }
}
