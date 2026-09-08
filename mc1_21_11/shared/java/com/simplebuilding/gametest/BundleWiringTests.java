package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.networking.MasterBuilderPickPayload;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.ReinforcedBundleSelectionPayload;
import com.simplebuilding.trade.ModTradeDefinitions;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTraderPool;
import com.simplebuilding.trade.TradeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Everything that connects the reinforced bundle to the <em>rest</em> of the game, as opposed to
 * what the item does to its own contents.
 *
 * <p>The bundle's inner life - inserting, removing, capacity, the selected entry - is covered by
 * {@code ReinforcedBundleTests} and {@link ItemBehaviourTests#bundleCapacityGrowsWithTierAndEnchantments}.
 * What had no coverage at all was every seam where the bundle meets another system: the
 * {@code ItemEntity} mixin that lets it hoover the floor, the two network payloads that address it
 * by inventory slot, the anvil rule that gates Colour Palette, the building wand pulling its blocks
 * out of a bundle, and the data layer that hands the bundle to the player in the first place
 * (recipe, smithing, trades, chest loot, the enchantability tags). Each of those is a place where
 * the bundle can quietly stop working while every test about the bundle itself stays green.
 *
 * <h2>Deliberately not duplicated here</h2>
 * <ul>
 *   <li><b>What Funnel decides</b> - {@link EnchantmentEffectTests#funnelDecidesWhatTheBundlePicksUp}
 *       already pins level I as a filter and level II as "everything" by calling
 *       {@code canAutoPickup} directly. This file only drives the <em>delivery</em> around that
 *       decision: the sneak lock, hands before inventory, and the pickup delay - all of which live
 *       in {@code ItemEntityMixin} and were never executed by a test.</li>
 *   <li><b>The master builder pick itself</b> -
 *       {@link NetworkHandlerTests#masterBuilderPickTakesBlocksOutOfTheEnchantedBundle} covers the
 *       empty hand, the occupied hand, an unknown item and the missing enchantment. Only the
 *       "no free slot" bail-out was left, and it is picked up below.</li>
 *   <li><b>That the mod adds loot pools at all</b> -
 *       {@link ConfigOptionTests#lootTableChangesStopWhenTheOptionIsSwitchedOff} counts the pools
 *       and pins the config switch. This file looks <em>inside</em> the pools instead.</li>
 * </ul>
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>Creative tab membership</b> ({@code ModItemGroupsContent}). A test could only assert
 *       that three {@code entries.accept(...)} lines still exist; that is a registration check, not
 *       a behaviour, and it would restate the source line it guards.</li>
 *   <li><b>{@code stacksTo(1)}</b> on the three bundles. Vanilla evaluates it, and an assertion on
 *       it restates the registration line in {@code ModItems} - the same call this tree already
 *       makes for the quivers (see {@code QuiverTests}). The fire resistance of the two upper tiers
 *       is different: it is observable on a <em>dropped</em> bundle, so it is asserted in
 *       {@link #netheriteBundleOnTheGroundSurvivesFireAndExplosions} - with the note that the
 *       mechanism behind it is vanilla's, and only the {@code fireResistant()} call is the mod's.</li>
 *   <li><b>The mouse wheel that produces {@code ReinforcedBundleSelectionPayload}</b>, the bundle
 *       tooltip and the fill bar. Client side; the mock player's connection swallows the packets
 *       and never draws anything.</li>
 *   <li><b>The anvil <em>screen</em></b>. Only the menu is driven here; the cost text and the
 *       greyed out output are client side.</li>
 * </ul>
 *
 * <h2>Known defects (pinned nowhere on purpose, so the tests do not cement them)</h2>
 * <ul>
 *   <li><b>No bundle is enchantable at an enchanting table.</b> {@code ModItems} registers
 *       {@code reinforced_bundle}, {@code netherite_bundle} and {@code enderite_bundle} without
 *       {@code .enchantable(...)}, so none of the three carries the {@code ENCHANTABLE} component.
 *       {@code EnchantmentMenu#slotsChanged} zeroes all three offers whenever
 *       {@code ItemStack#isEnchantable()} is false, and that method returns false without the
 *       component - so the table never offers a bundle anything, whatever the item tags say. The
 *       tags decide the <em>anvil</em>, which is a different gate and is open for all three tiers;
 *       {@link #containerEnchantmentsAcceptTheBundlesTheyAreMeantFor} covers that side. Nothing
 *       here is pinned by a test: the fix is a registration change, and an assertion either way
 *       would cement the current state.</li>
 * </ul>
 */
public final class BundleWiringTests {

    private BundleWiringTests() {
    }

    /** Blocks a radius 1 building wand plane covers: the centre plus its eight neighbours. */
    private static final int WAND_PLANE_BLOCKS = 9;

    /** Registry id of the item every claim in this file is about. */
    private static final String BUNDLE_ID = "simplebuilding:reinforced_bundle";

    /**
     * Registry id of the loot function that hands the mineshaft bundle out enchanted, as it is
     * written by {@code LootItemFunctions.TYPED_CODEC}.
     */
    private static final String ENCHANT_RANDOMLY = "minecraft:enchant_randomly";

    /**
     * Radius of the test explosion. Entities up to twice this are affected, so at 2.0 the blast
     * stops well inside the 8x8x8 test room and cannot reach a neighbouring test's entities.
     */
    private static final float BLAST_RADIUS = 2.0F;

    /** More than an {@code ItemEntity}'s five hit points, so a drop that can be hurt is destroyed. */
    private static final float LETHAL_DAMAGE = 6.0F;

    /** Fixed seed for the trade offers, so a definition with a random component stays reproducible. */
    private static final long TRADE_OFFER_SEED = 20260903L;

    // =====================================================================================
    // ITEM ENTITY MIXIN - PICKING UP OFF THE FLOOR
    // =====================================================================================

    /**
     * The delivery half of Funnel, which lives in {@code ItemEntityMixin#playerTouch} and was
     * never executed: what {@code canAutoPickup} allows still has to reach the right bundle, and
     * only under the mixin's own guards.
     *
     * <ol>
     *   <li><b>Sneaking switches the vacuum off.</b> The stone must not end up in the bundle. It
     *       does end up in the inventory, because the mixin then leaves the touch to vanilla -
     *       that is the point of the guard, not a side effect, and asserting it is what separates
     *       "the guard is there" from "nothing happened at all".</li>
     *   <li><b>Hands before inventory.</b> Two identical Funnel II bundles, one in the hand and one
     *       in the backpack: the hand one has to win. Without the ordering the player could never
     *       predict which container swallows a drop.</li>
     *   <li><b>The off hand is a hand, and it is the last thing the inventory loop would reach.</b>
     *       This is the half that can actually catch a reordering. {@code Inventory} maps the
     *       equipment slots onto indices <em>after</em> the backpack ones
     *       ({@code EQUIPMENT_SLOT_MAPPING}), so a bundle in the off hand is the very last thing
     *       {@code getItem(i)} reaches while still coming right after the main hand in
     *       {@code InteractionHand.values()}. With the winner in the off hand, a non-bundle in the
     *       main hand and the loser in backpack slot 2, hands-first and inventory-first pick
     *       different bundles - unlike the main hand case above, where the main hand <em>is</em>
     *       inventory index 0 and both orders happen to agree. The test measures the two indices
     *       rather than assuming them, so a changed inventory layout says so instead of quietly
     *       passing.</li>
     *   <li><b>The inventory branch waits for the pickup delay.</b> A freshly thrown stack must be
     *       left alone until its delay has run out, otherwise a bundle in the backpack would eat
     *       the items the player just dropped on purpose.</li>
     * </ol>
     *
     * <p>What breaks this test: dropping the {@code isShiftKeyDown} return, searching the inventory
     * before the hands (or deleting the hand loop outright - case 3 goes red either way), dropping
     * the {@code pickupDelay == 0} guard around the inventory loop, or failing to {@code discard()}
     * the entity once its stack has been absorbed.
     *
     * <p><em>Not</em> covered here: which items Funnel I and Funnel II accept - that is
     * {@link EnchantmentEffectTests#funnelDecidesWhatTheBundlePicksUp}. Every bundle below carries
     * Funnel II so the filter never has a say in the outcome.
     */
    public static void funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- 1. sneaking: the bundle stays out of it and vanilla's own pickup takes over ---
        ItemStack sneakBundle = funnelBundle(helper);
        armHand(player, sneakBundle);
        ItemEntity sneakDrop = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(2.5, 2.0, 2.5), 0);

        player.setShiftKeyDown(true);
        sneakDrop.playerTouch(player);
        player.setShiftKeyDown(false);

        Assertions.valueEqual(helper, countInBundle(sneakBundle, Items.STONE), 0,
                "a sneaking player still hoovered the stone into the bundle");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), 8,
                "sneaking neither left the stone on the ground nor let vanilla pick it up normally; "
                        + "the mixin swallowed the touch instead of stepping aside");

        // --- 2. a bundle in the hand beats a bundle in the backpack ---
        ItemStack handBundle = funnelBundle(helper);
        ItemStack backpackBundle = funnelBundle(helper);
        armHand(player, handBundle);
        player.getInventory().setItem(9, backpackBundle);

        ItemEntity handDrop = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(3.5, 2.0, 2.5), 0);
        handDrop.playerTouch(player);

        Assertions.valueEqual(helper, countInBundle(handBundle, Items.STONE), 8,
                "the bundle in the hand did not get the stone");
        Assertions.valueEqual(helper, countInBundle(backpackBundle, Items.STONE), 0,
                "the bundle in the backpack got the stone although the hand could have taken it");
        helper.assertTrue(handDrop.isRemoved(),
                "the whole stack went into the bundle but the item entity is still lying there");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), 0,
                "the stone was taken by the bundle and by the vanilla pickup, so it exists twice");

        // --- 3. the off hand beats a backpack slot that comes earlier in the inventory order ---
        // The main hand deliberately holds a non-bundle, so the hand loop only succeeds on its
        // second iteration. The drop has no pickup delay, so the inventory loop is reachable too -
        // that is what makes the two orderings disagree here.
        ItemStack offhandBundle = funnelBundle(helper);
        ItemStack lowSlotBundle = funnelBundle(helper);
        armHand(player, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, offhandBundle);
        player.getInventory().setItem(2, lowSlotBundle);

        // Measured, not assumed: the case only separates the two orderings while the off hand comes
        // after the backpack slot in getItem(i) order. If a future Inventory layout moves it in
        // front, this says so instead of silently going green for the wrong reason.
        int offhandIndex = inventoryIndexOf(helper, player, offhandBundle);
        int lowIndex = inventoryIndexOf(helper, player, lowSlotBundle);
        helper.assertTrue(offhandIndex > lowIndex,
                "the off hand sits at inventory index " + offhandIndex + ", in front of the backpack "
                        + "bundle at " + lowIndex + "; searching the inventory first would pick the "
                        + "off hand too, so this case can no longer tell the two orderings apart");

        ItemEntity offhandDrop = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(5.5, 2.0, 2.5), 0);
        offhandDrop.playerTouch(player);

        Assertions.valueEqual(helper, countInBundle(offhandBundle, Items.STONE), 8,
                "the bundle in the off hand did not get the stone");
        Assertions.valueEqual(helper, countInBundle(lowSlotBundle, Items.STONE), 0,
                "the bundle in backpack slot 2 got the stone although the off hand is a hand and "
                        + "hands are searched first");
        helper.assertTrue(offhandDrop.isRemoved(),
                "the off hand bundle took the whole stack but the item entity is still lying there");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), 0,
                "the stone reached the off hand bundle and the inventory, so it exists twice");

        // --- 4. the backpack branch has to wait for the pickup delay ---
        ItemStack delayedBundle = funnelBundle(helper);
        armHand(player, new ItemStack(Items.STICK));
        player.getInventory().setItem(9, delayedBundle);

        ItemEntity fresh = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(4.5, 2.0, 2.5), 10);
        fresh.playerTouch(player);

        Assertions.valueEqual(helper, countInBundle(delayedBundle, Items.STONE), 0,
                "a bundle in the backpack emptied a drop that is still inside its pickup delay");
        // The entity still lying there is vanilla's own delay talking; the assertion that pins the
        // mixin's guard is the empty bundle above.
        helper.assertTrue(!fresh.isRemoved(),
                "the drop was removed although nothing was allowed to pick it up yet");

        fresh.setNoPickUpDelay();
        fresh.playerTouch(player);

        Assertions.valueEqual(helper, countInBundle(delayedBundle, Items.STONE), 8,
                "once the pickup delay is over the bundle in the backpack has to take the drop");
        helper.assertTrue(fresh.isRemoved(),
                "the backpack bundle took the stack but left the item entity behind");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), 0,
                "the stone reached the backpack bundle and the inventory, so it exists twice");

        TestCleanup.succeed(helper);
    }

    /**
     * A dropped bundle is a container full of someone's belongings, and the two upper tiers are
     * meant to survive what would destroy an ordinary drop.
     *
     * <p>Two hazards, two different owners:
     * <ul>
     *   <li><b>Fire</b> is vanilla machinery - {@code fireResistant()} in {@code ModItems} writes a
     *       {@code DAMAGE_RESISTANT} component and {@code ItemEntity#hurtServer} honours it. The
     *       assertion is still worth having: it fails the moment that one registration line is
     *       dropped, and it is stated here as vanilla-carried rather than as mod coverage.</li>
     *   <li><b>The explosion</b> is the mod's own: {@code ItemEntityMixin#ignoreExplosion}. Nothing
     *       in vanilla protects a bundle from a blast.</li>
     * </ul>
     *
     * <p>Both halves use a plain reinforced bundle as the control, so "the protected ones survived"
     * always comes with proof that the hazard was lethal in the first place - and in the blast half
     * it is also what keeps the item check honest: widen {@code ignoreExplosion} to every drop and
     * this one assertion goes red.
     *
     * <p>The enderite tier is in both halves, and it has to come through both:
     * <ul>
     *   <li><b>Fire.</b> {@code fireResistant()} is on the netherite <em>and</em> the enderite
     *       registration, and the netherite drop cannot speak for the enderite one - the two are
     *       separate lines in {@code ModItems} and either can be lost on its own.</li>
     *   <li><b>The blast.</b> {@code ignoreExplosion} names the netherite bundle, the enderite
     *       bundle and the enderite quiver: blast immunity belongs to the tier, so the upgrade to
     *       enderite may not take it away. The quiver is in this half because it is the item the
     *       mixin lists that is not a bundle - drop it from the check and the bundles alone stay
     *       green.</li>
     * </ul>
     *
     * <p>The netherite quiver is in neither half on purpose: it is not in {@code ignoreExplosion}
     * today, and asserting that in either direction would cement a gap nobody has decided about.
     *
     * <p>What breaks this test: deleting the {@code ignoreExplosion} inject, narrowing <em>or</em>
     * widening its item check, and dropping {@code fireResistant()} from either upper tier.
     */
    public static void netheriteBundleOnTheGroundSurvivesFireAndExplosions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // --- fire: vanilla evaluates the component the mod asks for ---
        // All three drops sit in the far corner, outside the blast radius used further down, so the
        // two halves of this test cannot influence each other.
        ItemEntity plainInFire = drop(helper, new ItemStack(ModItems.REINFORCED_BUNDLE),
                new Vec3(0.5, 2.0, 0.5), 10);
        ItemEntity netheriteInFire = drop(helper, new ItemStack(ModItems.NETHERITE_BUNDLE),
                new Vec3(1.5, 2.0, 0.5), 10);
        ItemEntity enderiteInFire = drop(helper, new ItemStack(ModItems.ENDERITE_BUNDLE),
                new Vec3(0.5, 2.0, 1.5), 10);

        DamageSource fire = level.damageSources().onFire();
        plainInFire.hurtServer(level, fire, LETHAL_DAMAGE);
        netheriteInFire.hurtServer(level, fire, LETHAL_DAMAGE);
        enderiteInFire.hurtServer(level, fire, LETHAL_DAMAGE);

        helper.assertTrue(plainInFire.isRemoved(),
                "the plain reinforced bundle survived being set on fire, so this test cannot tell "
                        + "fire resistance from a harmless flame any more");
        helper.assertTrue(!netheriteInFire.isRemoved(),
                "the netherite bundle burned up; it is registered fireResistant()");
        // The two upper tiers carry fireResistant() on separate registration lines, so the
        // netherite drop above says nothing about this one.
        helper.assertTrue(!enderiteInFire.isRemoved(),
                "the enderite bundle burned up; it is registered fireResistant() in its own right, "
                        + "and it is the tier a player is most likely to be carrying over lava");

        // --- explosion: the mod's own mixin, with the blast kept inside this test room ---
        Vec3 blast = helper.absoluteVec(new Vec3(4.0, 2.0, 4.0));
        ItemEntity plainInBlast = drop(helper, new ItemStack(ModItems.REINFORCED_BUNDLE),
                new Vec3(3.8, 2.0, 4.0), 10);
        ItemEntity netheriteInBlast = drop(helper, new ItemStack(ModItems.NETHERITE_BUNDLE),
                new Vec3(4.2, 2.0, 4.0), 10);
        // Same distance from the centre as the other two, so an outcome can only come from the item
        // check in ignoreExplosion and not from a weaker share of the blast.
        ItemEntity enderiteInBlast = drop(helper, new ItemStack(ModItems.ENDERITE_BUNDLE),
                new Vec3(4.0, 2.0, 4.2), 10);
        ItemEntity enderiteQuiverInBlast = drop(helper, new ItemStack(ModItems.ENDERITE_QUIVER),
                new Vec3(4.0, 2.0, 3.8), 10);

        // NONE keeps the room's blocks intact; entities are still hurt.
        level.explode(null, blast.x, blast.y, blast.z, BLAST_RADIUS, Level.ExplosionInteraction.NONE);

        helper.assertTrue(plainInBlast.isRemoved(),
                "the plain reinforced bundle survived the blast it was standing in, so the blast is "
                        + "too weak to say anything about the protected bundles next to it - and "
                        + "ignoreExplosion may have been widened to every drop in the game");
        helper.assertTrue(!netheriteInBlast.isRemoved(),
                "the netherite bundle was destroyed by the explosion it is supposed to ignore");
        // The tier above netherite: each of the three items is a separate branch in
        // ItemEntityMixin#ignoreExplosion and can be lost on its own.
        helper.assertTrue(!enderiteInBlast.isRemoved(),
                "the enderite bundle was destroyed by the explosion; the top tier keeps what the "
                        + "netherite bundle already had - an upgrade takes nothing away");
        helper.assertTrue(!enderiteQuiverInBlast.isRemoved(),
                "the enderite quiver was destroyed by the explosion; it is the top tier as much as "
                        + "the enderite bundle and ignoreExplosion has to name it too");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // NETWORK PAYLOADS THAT ADDRESS AN INVENTORY SLOT
    // =====================================================================================

    /**
     * Both bundle payloads take something the client picked - a slot id, an item to fetch - and
     * write into the player's inventory with it. Both therefore need a guard, and neither guard
     * was ever executed.
     *
     * <ol>
     *   <li><b>The scroll selection only applies to a slot that really holds one of our bundles.</b>
     *       The control is a <em>vanilla</em> bundle in another slot: it has the same
     *       {@code BUNDLE_CONTENTS} component, so a handler without the
     *       {@code instanceof ReinforcedBundleItem} check would happily rewrite its selection. The
     *       test proves the control is writable first and only then requires the packet to leave
     *       it alone - otherwise the assertion could pass because nothing can ever be written
     *       there.</li>
     *   <li><b>A slot id outside the menu is dropped</b> instead of being handed to the slot list,
     *       where it would throw and kill the connection.</li>
     *   <li><b>The master builder pick does nothing when there is no free slot.</b> Without that
     *       bail-out the block would be pulled out of the bundle and the stack in the hand would
     *       have nowhere to go.</li>
     * </ol>
     *
     * <p>What breaks this test: dropping the item check in
     * {@code handleReinforcedBundleSelection}, dropping its bounds check, or dropping the
     * {@code emptySlot == -1} return in {@code handleMasterBuilderPick} - the last one would either
     * throw or destroy whatever the player was holding.
     */
    public static void bundlePacketsOnlyTouchTheSlotsTheyOwn(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);

        // Two entries, so index 1 is a valid selection - the component rejects out of range ones.
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem bundleItem = (ReinforcedBundleItem) bundle.getItem();
        bundleItem.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player);
        bundleItem.tryInsertStackFromWorld(bundle, new ItemStack(Items.DIRT, 8), player);
        player.getInventory().setItem(1, bundle);

        ItemStack vanillaBundle = new ItemStack(Items.BUNDLE);
        // 1.21.11 has no ItemStackTemplate: BundleContents still stores plain stacks.
        vanillaBundle.set(DataComponents.BUNDLE_CONTENTS,
                new BundleContents(List.of(new ItemStack(Items.STONE, 8))));
        player.getInventory().setItem(2, vanillaBundle);

        int modSlot = menuSlotOf(helper, player, bundle);
        int vanillaSlot = menuSlotOf(helper, player, vanillaBundle);

        // --- the packet reaches the mod's bundle ---
        // On this line the selected index is read with BundleItem#getSelectedItem, which returns
        // the index (26.2 renamed it to getSelectedItemIndex); -1 still means "nothing selected".
        ModMessageHandlers.handleReinforcedBundleSelection(
                new ReinforcedBundleSelectionPayload(modSlot, 1), player);
        Assertions.valueEqual(helper, BundleItem.getSelectedItem(bundle), 1,
                "the scroll selection never reached the reinforced bundle in slot " + modSlot);

        // --- the control can be written to, so requiring it unchanged below means something ---
        BundleItem.toggleSelectedItem(vanillaBundle, 0);
        Assertions.valueEqual(helper, BundleItem.getSelectedItem(vanillaBundle), 0,
                "the vanilla bundle used as a control refuses a selection even directly; it cannot "
                        + "show whether the handler keeps its hands off foreign containers");
        BundleItem.toggleSelectedItem(vanillaBundle, 0);
        Assertions.valueEqual(helper, BundleItem.getSelectedItem(vanillaBundle), -1,
                "the control bundle could not be reset to \"nothing selected\"");

        // --- and the packet must not reach it ---
        ModMessageHandlers.handleReinforcedBundleSelection(
                new ReinforcedBundleSelectionPayload(vanillaSlot, 0), player);
        Assertions.valueEqual(helper, BundleItem.getSelectedItem(vanillaBundle), -1,
                "the mod's selection packet rewrote a vanilla bundle in slot " + vanillaSlot);
        Assertions.valueEqual(helper, BundleItem.getSelectedItem(bundle), 1,
                "a packet aimed at another slot changed the reinforced bundle's selection");

        // --- ids outside the menu are dropped instead of thrown at the slot list ---
        ModMessageHandlers.handleReinforcedBundleSelection(
                new ReinforcedBundleSelectionPayload(-1, 0), player);
        ModMessageHandlers.handleReinforcedBundleSelection(
                new ReinforcedBundleSelectionPayload(player.containerMenu.slots.size(), 0), player);
        Assertions.valueEqual(helper, BundleItem.getSelectedItem(bundle), 1,
                "an out of range slot id changed the bundle's selection anyway");

        // --- the pick has to bail out when there is nowhere to park the stack in the hand ---
        ItemStack pickBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        pickBundle.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        ((ReinforcedBundleItem) pickBundle.getItem())
                .tryInsertStackFromWorld(pickBundle, new ItemStack(Items.STONE, 16), player);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 1));
        }
        player.getInventory().setItem(1, pickBundle);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 5));

        Assertions.valueEqual(helper, player.getInventory().getFreeSlot(), -1,
                "the inventory still has a free slot, so the bail-out below is not the branch under test");

        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.STONE)), player);

        helper.assertTrue(player.getMainHandItem().is(Items.TORCH),
                "the pick overwrote the hand although there was no free slot to park it in, hand holds "
                        + player.getMainHandItem());
        Assertions.valueEqual(helper, countInBundle(pickBundle, Items.STONE), 16,
                "the pick took stone out of the bundle although it had nowhere to put it");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ANVIL
    // =====================================================================================

    /**
     * Colour Palette makes a container hand out a random block instead of the selected one, which
     * is only meant to be reachable together with Master Builder. {@code AnvilScreenHandlerMixin}
     * enforces that by blanking the anvil's output whenever the result would carry the palette
     * without the builder.
     *
     * <p>The menu is driven directly: input slot, a new name, {@code createResult()}. A rename is
     * the shortest vanilla path to a non-empty result, so the test is about the mixin's condition
     * and not about vanilla's enchantment combination rules.
     *
     * <p>Three cases, and the first one is load bearing: a bundle without the palette has to come
     * out of the same anvil with a result and a cost. Without that control, "the output is empty"
     * would also pass if the anvil had stopped producing anything at all.
     *
     * <p>What breaks this test: deleting the restriction (the palette becomes freely combinable),
     * inverting the condition (Master Builder blanks the output instead), or widening it to items
     * that only carry Master Builder - the third case would then lose its result too.
     */
    public static void anvilBlanksTheResultForColourPaletteWithoutMasterBuilder(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- control: the same anvil, the same rename, no palette involved ---
        AnvilMenu plain = renameInAnvil(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), "Toolbox");
        helper.assertTrue(!plain.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "renaming a plain bundle produced no anvil result at all; the restriction below "
                        + "cannot be told apart from a dead anvil");
        helper.assertTrue(plain.getCost() > 0,
                "the control rename cost nothing, so a cost of 0 says nothing further down");

        // --- palette alone: the output is taken away ---
        ItemStack paletteOnly = new ItemStack(ModItems.REINFORCED_BUNDLE);
        paletteOnly.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        AnvilMenu blocked = renameInAnvil(helper, player, paletteOnly, "Palette");

        helper.assertTrue(blocked.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "the anvil handed out a Colour Palette bundle without Master Builder, it holds "
                        + blocked.getSlot(AnvilMenu.RESULT_SLOT).getItem());
        Assertions.valueEqual(helper, blocked.getCost(), 0,
                "the anvil emptied the output but still charged for it");

        // --- palette plus builder: allowed again ---
        ItemStack paletteAndBuilder = new ItemStack(ModItems.REINFORCED_BUNDLE);
        paletteAndBuilder.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        paletteAndBuilder.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        AnvilMenu allowed = renameInAnvil(helper, player, paletteAndBuilder, "Palette");

        helper.assertTrue(allowed.getSlot(AnvilMenu.RESULT_SLOT).getItem().is(ModItems.REINFORCED_BUNDLE),
                "a bundle carrying both Colour Palette and Master Builder was refused by the anvil");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // BUILDING WAND
    // =====================================================================================

    /**
     * A Master Builder bundle is meant to be a material source for the building wand, and the
     * permission is deliberately either-or: the wand may carry the enchantment, or the bundle may.
     *
     * <p>Three runs, all with a bundle as the <em>only</em> material in the inventory:
     * <ul>
     *   <li>enchanted bundle, plain wand - builds, and exactly one piece leaves the bundle per
     *       placed block ({@code removeOneFromBundle}, which no test had ever entered);</li>
     *   <li>plain bundle, enchanted wand - builds as well, which is the other half of the
     *       either-or;</li>
     *   <li>neither enchanted - the click is refused outright, so a bundle's content stays
     *       invisible to an ordinary wand.</li>
     * </ul>
     *
     * <p>The plane is pinned to radius 1 through the wand's own settings, so the expected 3x3 is
     * the same on every wand tier and the count below states a shape, not a balancing number.
     *
     * <p>What breaks this test: dropping the bundle branch out of {@code checkStackIsBlock},
     * requiring the enchantment on both sides instead of either, taking more than one item per
     * placement (or none), or shrinking the bundle stack itself instead of its contents.
     *
     * <p><em>Not</em> covered here: the wand's plane geometry and its durability arithmetic -
     * {@link ConsumptionAndDurabilityTests#buildingWandBillsOneBlockAndOnePointOfWearPerPlacement}
     * owns those, with loose stacks as the material.
     */
    public static void buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.getAbilities().instabuild = false;

        // --- 1. the bundle carries Master Builder, the wand does not ---
        BlockPos anchor = new BlockPos(2, 1, 2);
        helper.setBlock(anchor, Blocks.STONE);

        ItemStack bundle = masterBuilderBundle(helper, player);
        ItemStack wand = radiusOneWand();
        armWithWandAndBundle(player, wand, bundle);

        useOn(helper, player, wand, anchor, Direction.UP);
        runWandToCompletion(helper, player, wand);

        assertPlaneBuilt(helper, anchor);
        Assertions.valueEqual(helper, countInBundle(bundle, Items.STONE), 64 - WAND_PLANE_BLOCKS,
                "stone left in the bundle after a 3x3 plane; exactly one piece per placed block");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), 0,
                "the wand pulled the stone out of the bundle into the inventory instead of "
                        + "placing it straight from the bundle");

        // --- 2. the wand carries it instead, the bundle is plain ---
        BlockPos secondAnchor = new BlockPos(5, 1, 5);
        helper.setBlock(secondAnchor, Blocks.STONE);

        ItemStack plainBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ((ReinforcedBundleItem) plainBundle.getItem())
                .tryInsertStackFromWorld(plainBundle, new ItemStack(Items.STONE, 64), player);
        ItemStack builderWand = radiusOneWand();
        builderWand.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        armWithWandAndBundle(player, builderWand, plainBundle);

        useOn(helper, player, builderWand, secondAnchor, Direction.UP);
        runWandToCompletion(helper, player, builderWand);

        assertPlaneBuilt(helper, secondAnchor);
        Assertions.valueEqual(helper, countInBundle(plainBundle, Items.STONE), 64 - WAND_PLANE_BLOCKS,
                "stone left in the bundle when the wand is the one carrying Master Builder");

        // --- 3. neither side carries it: the bundle is not a material source at all ---
        BlockPos thirdAnchor = new BlockPos(2, 4, 2);
        helper.setBlock(thirdAnchor, Blocks.STONE);

        ItemStack lockedBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ((ReinforcedBundleItem) lockedBundle.getItem())
                .tryInsertStackFromWorld(lockedBundle, new ItemStack(Items.STONE, 64), player);
        ItemStack plainWand = radiusOneWand();
        armWithWandAndBundle(player, plainWand, lockedBundle);

        InteractionResult refused = useOn(helper, player, plainWand, thirdAnchor, Direction.UP);

        helper.assertTrue(refused == InteractionResult.FAIL,
                "a wand without Master Builder accepted the click while its only material was inside "
                        + "a bundle, result was " + refused);
        Assertions.valueEqual(helper, countInBundle(lockedBundle, Items.STONE), 64,
                "the unenchanted pair still spent stone out of the bundle");
        helper.assertBlockPresent(Blocks.AIR, thirdAnchor.above());

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // RECIPES
    // =====================================================================================

    /**
     * How a bundle is made: one shaped recipe for the base and two smithing transforms that walk
     * the tiers upwards. All three are driven through the live {@code RecipeManager} with real
     * inputs, so this asserts what the player can actually craft rather than restating the recipe
     * files.
     *
     * <p>Every positive case comes with the matching negative one: the same nine items in the wrong
     * places must not craft a bundle (otherwise the pattern would be decoration), the enderite
     * transform must refuse a reinforced bundle as its base (no tier skipping), and it must refuse
     * the netherite ingot as its addition (no template/addition mix-up).
     *
     * <p>What breaks this test: a changed pattern or key in {@code recipe/reinforced_bundle.json},
     * a swapped base/addition/template in either smithing recipe, a wrong result id, or a recipe
     * file that stops loading at all.
     */
    public static void bundleRecipesCraftTheBaseAndUpgradeItTierByTier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipes = level.getServer().getRecipeManager();

        ItemStack string = new ItemStack(Items.STRING);
        ItemStack nugget = new ItemStack(Items.COPPER_NUGGET);
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack vanillaBundle = new ItemStack(Items.BUNDLE);

        // " S " / "NBN" / "LLL"
        CraftingInput asShipped = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, string, ItemStack.EMPTY,
                nugget, vanillaBundle, nugget,
                leather, leather, leather));

        Optional<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> crafted =
                recipes.getRecipeFor(RecipeType.CRAFTING, asShipped, level);
        helper.assertTrue(crafted.isPresent(),
                "string / copper nugget - bundle - copper nugget / three leather crafts nothing at all");
        // 1.21.11 still passes the registries into assemble; 26.2 dropped that parameter.
        ItemStack result = crafted.get().value().assemble(asShipped, level.registryAccess());
        helper.assertTrue(result.is(ModItems.REINFORCED_BUNDLE),
                "the bundle pattern crafts " + result + " instead of a reinforced bundle");
        Assertions.valueEqual(helper, result.getCount(), 1, "reinforced bundles produced per craft");

        // Same nine items, string and leather swapped: a pattern that is only a checklist would match.
        CraftingInput upsideDown = CraftingInput.of(3, 3, List.of(
                leather, leather, leather,
                nugget, vanillaBundle, nugget,
                ItemStack.EMPTY, string, ItemStack.EMPTY));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, upsideDown, level).isEmpty(),
                "the same ingredients in the wrong rows still crafted something; the pattern is not "
                        + "being checked");

        // --- reinforced -> netherite ---
        SmithingRecipeInput toNetherite = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(ModItems.REINFORCED_BUNDLE),
                new ItemStack(Items.NETHERITE_INGOT));
        assertSmithingResult(helper, level, recipes, toNetherite, ModItems.NETHERITE_BUNDLE,
                "reinforced bundle + netherite ingot on the netherite template");

        // --- netherite -> enderite ---
        SmithingRecipeInput toEnderite = new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.NETHERITE_BUNDLE),
                new ItemStack(ModItems.ENDERITE_INGOT));
        assertSmithingResult(helper, level, recipes, toEnderite, ModItems.ENDERITE_BUNDLE,
                "netherite bundle + enderite ingot on the enderite template");

        // --- no skipping a tier, no mixing the additions up ---
        SmithingRecipeInput skippedTier = new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.REINFORCED_BUNDLE),
                new ItemStack(ModItems.ENDERITE_INGOT));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, skippedTier, level).isEmpty(),
                "the enderite upgrade accepted a reinforced bundle as its base, so the netherite "
                        + "tier can be skipped");

        SmithingRecipeInput wrongAddition = new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.NETHERITE_BUNDLE),
                new ItemStack(Items.NETHERITE_INGOT));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, wrongAddition, level).isEmpty(),
                "the enderite upgrade accepted a netherite ingot as its addition");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // TRADES
    // =====================================================================================

    /**
     * The wandering trader is the only merchant that deals in reinforced bundles, and he does it in
     * both directions - one entry sells a bundle for emeralds, the other buys one back. The two
     * differ only in which side the cost and the result sit on, which is exactly the kind of thing
     * a copy-paste edit swaps.
     *
     * <p><b>Different from the 26.2 twin.</b> That line reads the two trade JSONs out of the
     * {@code villager_trade} registry, which does not exist on 1.21.11: here the same table is held
     * in code as {@link ModTradeDefinitions#wanderingTraderTrades()}, so the test looks the two
     * definitions up in the pool they are registered for and turns them into offers through
     * {@link TradeDefinition#toListing()} - the very listing both loaders push into the vanilla
     * pools. The offer, and therefore everything asserted below, is produced by exactly the code
     * the player meets.
     *
     * <p>{@link TradeAndMigrationTests#tradeDefinitionsProduceTheExpectedOffers} drives three other
     * definitions through the same path; this adds the two bundle ones, which are the only trades
     * involving a container item. That the pools reach the merchant at all is
     * {@link TradeAndMigrationTests#modTradesAreMergedIntoTheWanderingTraderPools}' job.
     *
     * <p>The prices restate the definition, and vanilla is what turns the listing into an offer.
     * What the assertions really hold is the mod's own table: that both entries still sit in the
     * pool they belong to, that the bundle is on the right side of each offer, and that the
     * one-shot {@code maxUses} of a wandering trader deal survives.
     *
     * <p>What breaks this test: a bundle trade dropped from or moved between the wandering trader
     * pools, a swapped cost/result, a lost count, or a changed {@code maxUses} / {@code xp}.
     */
    public static void wanderingTraderSellsAndBuysTheReinforcedBundle(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 2, 1));

        MerchantOffer selling = offerOf(helper, villager,
                wanderingTradeGiving(helper, WanderingTraderPool.UNCOMMON, ModItems.REINFORCED_BUNDLE));
        assertOffer(helper, selling, "wandering_trader/uncommon emerald -> reinforced_bundle",
                Items.EMERALD, 16, ModItems.REINFORCED_BUNDLE, 1, 1, 15);
        helper.assertTrue(selling.getCostB().isEmpty(),
                "the bundle sale asks for a second item, it should cost emeralds only");

        MerchantOffer buying = offerOf(helper, villager,
                wanderingTradeWanting(helper, WanderingTraderPool.BUY, ModItems.REINFORCED_BUNDLE));
        assertOffer(helper, buying, "wandering_trader/buying reinforced_bundle -> emerald",
                ModItems.REINFORCED_BUNDLE, 1, Items.EMERALD, 12, 1, 10);

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // CHEST LOOT
    // =====================================================================================

    /**
     * Three vanilla chest tables are supposed to hand out a reinforced bundle: the dungeon, the
     * shipwreck treasure and the abandoned mineshaft, the last one enchanted.
     *
     * <p>{@link ConfigOptionTests#lootTableChangesStopWhenTheOptionIsSwitchedOff} only counts the
     * pools the mod offers, which cannot tell a pool full of enchanted books from a pool with a
     * bundle in it. This test serialises the pools the mod hands over through
     * {@code LootPool.CODEC} and looks for the bundle entry inside them - the entry list itself is
     * private, but its serialised form is exact and needs no dice.
     *
     * <p>Deliberately not asserted: the weights. They are balancing numbers, and pinning them here
     * would turn every balance pass into a red test without catching a single wiring bug.
     *
     * <p>A vanilla table the mod never touches is recorded with the same helper and has to come
     * back empty, so the three positive answers cannot come from a recorder that reports a bundle
     * for everything.
     *
     * <p>What breaks this test: dropping the bundle entry from one of the three tables, moving a
     * table's block behind another key, or losing the random enchantment on the mineshaft entry.
     */
    public static void reinforcedBundleSitsInDungeonShipwreckAndMineshaftLoot(GameTestHelper helper) {
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges,
                "worldGen.enableLootTableChanges is switched off for this run, so the mod adds no "
                        + "pools at all and nothing below could be found either way");

        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        JsonObject dungeon = bundleEntry(helper, registries, BuiltInLootTables.SIMPLE_DUNGEON);
        helper.assertTrue(dungeon != null,
                "the reinforced bundle is no longer in the dungeon chest loot");
        helper.assertTrue(bundleEntry(helper, registries, BuiltInLootTables.SHIPWRECK_TREASURE) != null,
                "the reinforced bundle is no longer in the shipwreck treasure loot");

        JsonObject mineshaft = bundleEntry(helper, registries, BuiltInLootTables.ABANDONED_MINESHAFT);
        helper.assertTrue(mineshaft != null,
                "the reinforced bundle is no longer in the abandoned mineshaft loot");
        helper.assertTrue(mineshaft.has("functions"),
                "the mineshaft bundle lost its loot functions; it is the one entry that is handed "
                        + "out enchanted, entry is " + mineshaft);
        // Naming the function, not just counting the key: every other loot function - a set_count,
        // a set_components - keeps "functions" present while the bundle drops out of the chest
        // unenchanted, which is the whole difference between this entry and the two above.
        helper.assertTrue(hasLootFunction(mineshaft, ENCHANT_RANDOMLY),
                "the mineshaft bundle no longer runs " + ENCHANT_RANDOMLY + ", so it comes out of "
                        + "the chest unenchanted like the dungeon and shipwreck ones, entry is "
                        + mineshaft);
        // Control: the dungeon entry is the same item without the function, so a helper that
        // answered "yes" for any entry would have to say so here.
        helper.assertTrue(!hasLootFunction(dungeon, ENCHANT_RANDOMLY),
                "the dungeon bundle reports " + ENCHANT_RANDOMLY + " as well, so the check above "
                        + "recognises no function in particular, entry is " + dungeon);

        // Control: a table the mod does not touch must come back without a bundle entry.
        helper.assertTrue(bundleEntry(helper, registries, BuiltInLootTables.SPAWN_BONUS_CHEST) == null,
                "the recorder finds a reinforced bundle even in a loot table the mod never edits, "
                        + "so the three answers above prove nothing");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ENCHANTABILITY
    // =====================================================================================

    /**
     * Every bundle feature that matters - Drawer, Deep Pockets, Funnel, Master Builder, Colour
     * Palette - is only reachable if the anvil considers the bundle a legal target for that
     * enchantment, and that is decided by two item tags the mod ships:
     * {@code simplebuilding:bundle_enchantable} carries Drawer, Deep Pockets and Funnel,
     * {@code simplebuilding:extra_inventory_items} carries Master Builder and Colour Palette. A
     * bundle that falls out of them keeps every line of its behaviour and still becomes useless,
     * because the player can no longer put the enchantment on it.
     *
     * <p>The check goes through {@code Enchantment#canEnchant}, which is what the anvil itself asks
     * before it lets an enchanted book through. Vanilla evaluates it; what is under test is the
     * mod's tag content. The enchanting table is a second, independent gate and is closed for every
     * bundle regardless of these tags - see the known defect in the class javadoc.
     *
     * <p>Two controls, and the second one is what makes this a test of the tag <em>wiring</em>
     * rather than of "some tag happens to list both bundles":
     * <ul>
     *   <li>a diamond pickaxe is in neither tag and must be rejected by all five, otherwise the
     *       tags would be wildcards and the positive answers would mean nothing;</li>
     *   <li>the diamond building wand is in {@code extra_inventory_items} only - it enters through
     *       {@code #simplebuilding:building_wand_enchantable} and is nowhere in
     *       {@code bundle_enchantable}. So it has to be accepted by Master Builder and Colour
     *       Palette and refused by Drawer, Deep Pockets and Funnel. Moving one of the five
     *       enchantments onto the other tag leaves every bundle assertion green and turns this
     *       one red.</li>
     * </ul>
     *
     * <p>All three tiers are asserted, the enderite one included: the tiers are separate lines in
     * {@code ModItemTagProvider} and the top tier is the one an upgrade must not silently
     * downgrade, so the netherite bundle passing says nothing about it.
     *
     * <p>What breaks this test: removing a bundle from either tag file, pointing one of the five
     * enchantments at a different {@code supported_items} tag, or merging the two tags into one.
     */
    public static void containerEnchantmentsAcceptTheBundlesTheyAreMeantFor(GameTestHelper helper) {
        // Split exactly the way ModEnchantments splits them, so the wand control below can tell the
        // two tags apart instead of only proving that both bundles are in "some" tag.
        List<ResourceKey<Enchantment>> bundleTagEnchantments = List.of(
                ModEnchantments.DRAWER,
                ModEnchantments.DEEP_POCKETS,
                ModEnchantments.FUNNEL);
        List<ResourceKey<Enchantment>> extraInventoryTagEnchantments = List.of(
                ModEnchantments.MASTER_BUILDER,
                ModEnchantments.COLOR_PALETTE);

        List<ResourceKey<Enchantment>> containerEnchantments = new ArrayList<>(bundleTagEnchantments);
        containerEnchantments.addAll(extraInventoryTagEnchantments);

        for (Item bundle : List.of(ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE,
                ModItems.ENDERITE_BUNDLE)) {
            ItemStack stack = new ItemStack(bundle);
            for (ResourceKey<Enchantment> key : containerEnchantments) {
                helper.assertTrue(enchantment(helper, key).value().canEnchant(stack),
                        key.identifier() + " cannot be put on " + bundle
                                + "; the bundle fell out of its supported_items tag");
            }
        }

        ItemStack control = new ItemStack(Items.DIAMOND_PICKAXE);
        for (ResourceKey<Enchantment> key : containerEnchantments) {
            helper.assertTrue(!enchantment(helper, key).value().canEnchant(control),
                    key.identifier() + " can be put on a diamond pickaxe, so its item tag accepts "
                            + "everything and says nothing about the bundles");
        }

        // The wand is the item that separates the two tags: extra_inventory_items pulls in the
        // building wands, bundle_enchantable does not.
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        for (ResourceKey<Enchantment> key : extraInventoryTagEnchantments) {
            helper.assertTrue(enchantment(helper, key).value().canEnchant(wand),
                    key.identifier() + " cannot be put on the diamond building wand; it is supposed "
                            + "to hang on simplebuilding:extra_inventory_items, which contains "
                            + "#simplebuilding:building_wand_enchantable");
        }
        for (ResourceKey<Enchantment> key : bundleTagEnchantments) {
            helper.assertTrue(!enchantment(helper, key).value().canEnchant(wand),
                    key.identifier() + " can be put on the diamond building wand, so it no longer "
                            + "hangs on simplebuilding:bundle_enchantable - the two tags have been "
                            + "confused with each other");
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * A connected mock player inside the room, handed back to the server when the test ends - a
     * leaked mock player keeps the player list non-empty and stalls the gametest server on
     * shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(0.5, 2.0, 0.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** A reinforced bundle that takes anything off the floor, so the Funnel filter never decides. */
    private static ItemStack funnelBundle(GameTestHelper helper) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        bundle.enchant(enchantment(helper, ModEnchantments.FUNNEL), 2);
        return bundle;
    }

    /** A Master Builder bundle holding a full stack of stone, the wand's only material source. */
    private static ItemStack masterBuilderBundle(GameTestHelper helper, ServerPlayer player) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        bundle.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        ((ReinforcedBundleItem) bundle.getItem())
                .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 64), player);
        return bundle;
    }

    /**
     * Empties the inventory and puts one stack into the selected hotbar slot.
     * {@code Inventory#clearContent} also clears the equipment, so a previous case's off hand item
     * is gone - which is why the off hand has to be filled <em>after</em> this call, not before.
     */
    private static void armHand(ServerPlayer player, ItemStack stack) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static void armWithWandAndBundle(ServerPlayer player, ItemStack wand, ItemStack bundle) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.getInventory().setItem(1, bundle);
    }

    /**
     * Drops one stack at a room relative position with an explicit pickup delay. The random launch
     * velocity the {@code ItemEntity} constructor hands out is cleared so the drop stays where the
     * test put it, and the entity is removed again when the test ends.
     */
    private static ItemEntity drop(GameTestHelper helper, ItemStack stack, Vec3 relativePos, int pickupDelay) {
        Vec3 pos = helper.absoluteVec(relativePos);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(pickupDelay);
        helper.getLevel().addFreshEntity(entity);
        TestCleanup.before(helper, entity::discard);
        return entity;
    }

    /** How many of {@code item} sit inside the bundle's contents. */
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

    /** How many of {@code item} lie loose in the inventory - not counting anything inside a bundle. */
    private static int looseCount(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * The index {@code Inventory#getItem(int)} hands this exact stack back under - the same order
     * {@code ItemEntityMixin}'s inventory loop walks. Equipment slots are part of that range, so
     * this also resolves a stack that is only held in the off hand.
     */
    private static int inventoryIndexOf(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) {
                return i;
            }
        }
        throw helper.assertionException("the stack " + stack + " is at no inventory index at all");
    }

    /** The menu slot id the given stack currently sits in - the id the client would send. */
    private static int menuSlotOf(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        for (int i = 0; i < player.containerMenu.slots.size(); i++) {
            Slot slot = player.containerMenu.getSlot(i);
            if (slot.getItem() == stack) {
                return i;
            }
        }
        throw helper.assertionException("the stack " + stack + " is in no slot of the open menu");
    }

    /**
     * Puts one item into a fresh anvil and renames it, which is the shortest vanilla path to a
     * non-empty result. {@code setItemName} runs {@code createResult} itself; the return value is
     * checked because a refused name would leave the output empty for a reason that has nothing to
     * do with the mixin under test.
     */
    private static AnvilMenu renameInAnvil(GameTestHelper helper, ServerPlayer player,
                                           ItemStack input, String name) {
        AnvilMenu menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(input);
        helper.assertTrue(menu.setItemName(name),
                "the anvil refused the new name \"" + name + "\", so no result was ever computed");
        return menu;
    }

    /** A wand pinned to radius 1, so the plane is a 3x3 on every tier. */
    private static ItemStack radiusOneWand() {
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        CompoundTag settings = customData(wand);
        settings.putInt("SettingsRadius", 1);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean wandIsActive(ItemStack wand) {
        return customData(wand).getBooleanOr("Active", false);
    }

    /**
     * Drives the wand's own {@code inventoryTick} until it switches itself off. The gametest server
     * never pumps a mock player's connection, so calling the item hook directly is both closer to
     * what is under test and deterministic - the whole build finishes inside one test tick.
     */
    private static void runWandToCompletion(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        helper.assertTrue(wandIsActive(wand),
                "the wand did not arm itself on the click, so nothing was ever taken out of the bundle");

        for (int tick = 0; tick < 200; tick++) {
            wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            if (!wandIsActive(wand)) {
                return;
            }
        }
        throw helper.assertionException("the building wand was still active after 200 inventory ticks");
    }

    /** The 3x3 the wand builds in the layer above the block that was clicked. */
    private static void assertPlaneBuilt(GameTestHelper helper, BlockPos anchor) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertBlockPresent(Blocks.STONE, anchor.offset(dx, 1, dz));
            }
        }
    }

    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hit, face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    /** Asks the recipe manager for a smithing recipe and checks what it would forge. */
    private static void assertSmithingResult(GameTestHelper helper, ServerLevel level, RecipeManager recipes,
                                             SmithingRecipeInput input, Item expected, String what) {
        Optional<RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>> holder =
                recipes.getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(holder.isPresent(), what + " matches no smithing recipe at all");

        ItemStack forged = holder.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(forged.is(expected),
                what + " forges " + forged + " instead of " + expected);
        Assertions.valueEqual(helper, forged.getCount(), 1, what + ": items produced per upgrade");
    }

    /**
     * The one wandering trader definition of that pool whose <em>result</em> is the given item -
     * a sale. Missing definitions fail the test instead of skipping the assertions below.
     */
    private static TradeDefinition wanderingTradeGiving(GameTestHelper helper, WanderingTraderPool pool,
                                                        Item result) {
        for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
            if (group.pool() != pool) {
                continue;
            }
            for (TradeDefinition trade : group.trades()) {
                if (trade.result().is(result)) {
                    return trade;
                }
            }
        }
        helper.fail("no wandering trader " + pool + " trade gives " + result);
        throw new IllegalStateException("unreachable");
    }

    /**
     * The one wandering trader definition of that pool whose <em>cost</em> is the given item - a
     * buy-back. Looking the two directions up by opposite sides is what makes a swapped
     * cost/result show up here rather than pass unnoticed.
     */
    private static TradeDefinition wanderingTradeWanting(GameTestHelper helper, WanderingTraderPool pool,
                                                         Item cost) {
        for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
            if (group.pool() != pool) {
                continue;
            }
            for (TradeDefinition trade : group.trades()) {
                if (trade.cost().item().value() == cost) {
                    return trade;
                }
            }
        }
        helper.fail("no wandering trader " + pool + " trade asks for " + cost);
        throw new IllegalStateException("unreachable");
    }

    /**
     * Turns a definition into the offer the merchant would hand out, through the same
     * {@code ItemListing} both loaders register. The seed is fixed so a trade with a random
     * component stays reproducible.
     */
    private static MerchantOffer offerOf(GameTestHelper helper, Villager merchant, TradeDefinition trade) {
        MerchantOffer offer = trade.toListing()
                .getOffer(helper.getLevel(), merchant, RandomSource.create(TRADE_OFFER_SEED));
        helper.assertTrue(offer != null, "trade definition " + trade + " produced no offer");
        return offer;
    }

    private static void assertOffer(GameTestHelper helper, MerchantOffer offer, String id,
                                    Item wantedItem, int wantedCount,
                                    Item givenItem, int givenCount,
                                    int maxUses, int xp) {
        ItemStack costA = offer.getBaseCostA();
        helper.assertTrue(costA.is(wantedItem),
                id + ": expected cost item " + wantedItem + ", was " + costA);
        Assertions.valueEqual(helper, costA.getCount(), wantedCount, id + ": cost count");

        ItemStack result = offer.getResult();
        helper.assertTrue(result.is(givenItem),
                id + ": expected result item " + givenItem + ", was " + result);
        Assertions.valueEqual(helper, result.getCount(), givenCount, id + ": result count");

        Assertions.valueEqual(helper, offer.getMaxUses(), maxUses, id + ": max uses");
        Assertions.valueEqual(helper, offer.getXp(), xp, id + ": trade xp");
    }

    /**
     * Runs the mod's loot hook for one table, serialises every pool it hands over and returns the
     * reinforced bundle entry from the first pool that holds one, or {@code null}. The entry list
     * of a built {@code LootPool} is private, but its codec output is exact - and unlike rolling
     * the pool it needs no dice and no statistics.
     *
     * <p>A loot item entry carries its item id under {@code "name"} and its loot functions under
     * {@code "functions"}; both spellings come from vanilla's own entry codecs.
     */
    private static JsonObject bundleEntry(GameTestHelper helper, HolderLookup.Provider registries,
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
                if (name != null && name.isJsonPrimitive() && BUNDLE_ID.equals(name.getAsString())) {
                    return object;
                }
            }
        }
        return null;
    }

    /**
     * Whether a serialised loot entry runs the named loot function. Vanilla's function codec is a
     * registry dispatch on the key {@code "function"}, so the id of each element of the entry's
     * {@code "functions"} array sits under that key - which is what separates "this entry still has
     * some function" from "this entry still enchants what it hands out".
     */
    private static boolean hasLootFunction(JsonObject entry, String functionId) {
        JsonElement functions = entry.get("functions");
        if (functions == null || !functions.isJsonArray()) {
            return false;
        }
        for (JsonElement function : functions.getAsJsonArray()) {
            if (!function.isJsonObject()) {
                continue;
            }
            JsonElement id = function.getAsJsonObject().get("function");
            if (id != null && id.isJsonPrimitive() && functionId.equals(id.getAsString())) {
                return true;
            }
        }
        return false;
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

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
