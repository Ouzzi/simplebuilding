package com.simplestorage.test;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.bundle.ReinforcedBundleItem;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
 *       it restates the registration line in {@code StorageItems} - the same call this tree already
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
 *   <li><b>No bundle is enchantable at an enchanting table.</b> {@code StorageItems} registers
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
final class BundleWiringTests {

    private BundleWiringTests() {
    }

    /**
     * Radius of the test explosion. Entities up to twice this are affected, so at 2.0 the blast
     * stops well inside the 8x8x8 test room and cannot reach a neighbouring test's entities.
     */
    private static final float BLAST_RADIUS = 2.0F;

    /** More than an {@code ItemEntity}'s five hit points, so a drop that can be hurt is destroyed. */
    private static final float LETHAL_DAMAGE = 6.0F;

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

        helper.assertValueEqual(countInBundle(sneakBundle, Items.STONE), 0,
                "a sneaking player still hoovered the stone into the bundle");
        helper.assertValueEqual(looseCount(player, Items.STONE), 8,
                "sneaking neither left the stone on the ground nor let vanilla pick it up normally; "
                        + "the mixin swallowed the touch instead of stepping aside");

        // --- 2. a bundle in the hand beats a bundle in the backpack ---
        ItemStack handBundle = funnelBundle(helper);
        ItemStack backpackBundle = funnelBundle(helper);
        armHand(player, handBundle);
        player.getInventory().setItem(9, backpackBundle);

        ItemEntity handDrop = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(3.5, 2.0, 2.5), 0);
        handDrop.playerTouch(player);

        helper.assertValueEqual(countInBundle(handBundle, Items.STONE), 8,
                "the bundle in the hand did not get the stone");
        helper.assertValueEqual(countInBundle(backpackBundle, Items.STONE), 0,
                "the bundle in the backpack got the stone although the hand could have taken it");
        helper.assertTrue(handDrop.isRemoved(),
                "the whole stack went into the bundle but the item entity is still lying there");
        helper.assertValueEqual(looseCount(player, Items.STONE), 0,
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

        helper.assertValueEqual(countInBundle(offhandBundle, Items.STONE), 8,
                "the bundle in the off hand did not get the stone");
        helper.assertValueEqual(countInBundle(lowSlotBundle, Items.STONE), 0,
                "the bundle in backpack slot 2 got the stone although the off hand is a hand and "
                        + "hands are searched first");
        helper.assertTrue(offhandDrop.isRemoved(),
                "the off hand bundle took the whole stack but the item entity is still lying there");
        helper.assertValueEqual(looseCount(player, Items.STONE), 0,
                "the stone reached the off hand bundle and the inventory, so it exists twice");

        // --- 4. the backpack branch has to wait for the pickup delay ---
        ItemStack delayedBundle = funnelBundle(helper);
        armHand(player, new ItemStack(Items.STICK));
        player.getInventory().setItem(9, delayedBundle);

        ItemEntity fresh = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(4.5, 2.0, 2.5), 10);
        fresh.playerTouch(player);

        helper.assertValueEqual(countInBundle(delayedBundle, Items.STONE), 0,
                "a bundle in the backpack emptied a drop that is still inside its pickup delay");
        // The entity still lying there is vanilla's own delay talking; the assertion that pins the
        // mixin's guard is the empty bundle above.
        helper.assertTrue(!fresh.isRemoved(),
                "the drop was removed although nothing was allowed to pick it up yet");

        fresh.setNoPickUpDelay();
        fresh.playerTouch(player);

        helper.assertValueEqual(countInBundle(delayedBundle, Items.STONE), 8,
                "once the pickup delay is over the bundle in the backpack has to take the drop");
        helper.assertTrue(fresh.isRemoved(),
                "the backpack bundle took the stack but left the item entity behind");
        helper.assertValueEqual(looseCount(player, Items.STONE), 0,
                "the stone reached the backpack bundle and the inventory, so it exists twice");

        helper.succeed();
    }

    /**
     * REV2-C: a claim that refuses the containers right keeps the drop out of the Funnel bundle (framework Protection,
     * asked by the Funnel itself, because the claim's own pickup check runs after it). Without the refusal the same drop is
     * swept up.
     */
    public static void funnelRespectsProtection(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack bundle = funnelBundle(helper);
        armHand(player, bundle);
        Object server = helper.getLevel().getServer();
        BlockPos denied = helper.absolutePos(new BlockPos(2, 2, 2));
        com.simplebuilding.framework.api.Protection.register(server, "rev2c-test", new com.simplebuilding.framework.api.Protection.Check() {
            @Override public boolean allows(com.simplebuilding.framework.api.Protection.Target target) {
                return !(target.x() == denied.getX() && target.z() == denied.getZ());
            }
        });
        try {
            ItemEntity refused = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(2.5, 2.0, 2.5), 0);
            refused.playerTouch(player);
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0, "the Funnel bundle took a drop the claim refuses");
            ItemEntity allowed = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(4.5, 2.0, 2.5), 0);
            allowed.playerTouch(player);
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8, "the Funnel bundle did not take an allowed drop");
        } finally {
            com.simplebuilding.framework.api.Protection.unregister(server, "rev2c-test");
        }
        helper.succeed();
    }

    /**
     * The funnel obeys the same two gates as vanilla's own pickup, on the hand path too: the
     * pickup delay must be over and the drop must not be reserved for someone else
     * ({@code ItemEntity#target}). The hand loop used to run before both checks, so a Funnel
     * bundle in the hand swallowed drops other mods park with an endless delay (display items)
     * and the reserved copies a {@code /give} with a full inventory leaves behind (audit
     * 2026-09-26 #14).
     */
    public static void funnelHonoursPickupDelayAndTargetInTheHand(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack bundle = funnelBundle(helper);
        armHand(player, bundle);

        // --- still inside its pickup delay ---
        ItemEntity delayed = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(2.5, 2.0, 2.5), 10);
        delayed.playerTouch(player);
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                "the bundle in the hand took a drop that is still inside its pickup delay");
        helper.assertTrue(!delayed.isRemoved(), "the delayed drop vanished");

        // --- reserved for another player ---
        ItemEntity reserved = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(3.5, 2.0, 2.5), 0);
        reserved.setTarget(java.util.UUID.randomUUID());
        reserved.playerTouch(player);
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                "the bundle in the hand took a drop reserved for another player");
        helper.assertTrue(!reserved.isRemoved(), "the reserved drop vanished");

        // --- reserved for this player: allowed ---
        ItemEntity mine = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(4.5, 2.0, 2.5), 0);
        mine.setTarget(player.getUUID());
        mine.playerTouch(player);
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                "a drop reserved for the touching player itself did not reach the bundle");
        helper.assertTrue(mine.isRemoved(), "the absorbed drop is still lying there");
        helper.succeed();
    }

    /**
     * A dropped bundle is a container full of someone's belongings, and the two upper tiers are
     * meant to survive what would destroy an ordinary drop.
     *
     * <p>Two hazards, two different owners:
     * <ul>
     *   <li><b>Fire</b> is vanilla machinery - {@code fireResistant()} in {@code StorageItems} writes a
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
     *       separate lines in {@code StorageItems} and either can be lost on its own.</li>
     *   <li><b>The blast.</b> {@code ignoreExplosion} names the netherite bundle, the enderite
     *       bundle and the enderite quiver: blast immunity belongs to the tier, so the upgrade to
     *       enderite may not take it away. The quiver is in this half because it is the item the
     *       mixin lists that is not a bundle - drop it from the check and the bundles alone stay
     *       green.</li>
     * </ul>
     *
     * <p>The netherite quiver joined {@code ignoreExplosion} on 2026-09-28 (owner decision); its
     * blast immunity is pinned by {@code QuiverTests#netheriteQuiverSurvivesAnExplosionLikeTheNetheriteBundle}.
     *
     * <p>What breaks this test: deleting the {@code ignoreExplosion} inject, narrowing <em>or</em>
     * widening its item check, and dropping {@code fireResistant()} from either upper tier.
     */
    public static void netheriteBundleOnTheGroundSurvivesFireAndExplosions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // --- fire: vanilla evaluates the component the mod asks for ---
        // All three drops sit in the far corner, outside the blast radius used further down, so the
        // two halves of this test cannot influence each other.
        ItemEntity plainInFire = drop(helper, new ItemStack(StorageItems.REINFORCED_BUNDLE),
                new Vec3(0.5, 2.0, 0.5), 10);
        ItemEntity netheriteInFire = drop(helper, new ItemStack(StorageItems.NETHERITE_BUNDLE),
                new Vec3(1.5, 2.0, 0.5), 10);
        ItemEntity enderiteInFire = drop(helper, new ItemStack(StorageItems.ENDERITE_BUNDLE),
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
        ItemEntity plainInBlast = drop(helper, new ItemStack(StorageItems.REINFORCED_BUNDLE),
                new Vec3(3.8, 2.0, 4.0), 10);
        ItemEntity netheriteInBlast = drop(helper, new ItemStack(StorageItems.NETHERITE_BUNDLE),
                new Vec3(4.2, 2.0, 4.0), 10);
        // Same distance from the centre as the other two, so an outcome can only come from the item
        // check in ignoreExplosion and not from a weaker share of the blast.
        ItemEntity enderiteInBlast = drop(helper, new ItemStack(StorageItems.ENDERITE_BUNDLE),
                new Vec3(4.0, 2.0, 4.2), 10);
        ItemEntity enderiteQuiverInBlast = drop(helper, new ItemStack(StorageItems.ENDERITE_QUIVER),
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

        helper.succeed();
    }

    // =====================================================================================
    // NETWORK PAYLOADS THAT ADDRESS AN INVENTORY SLOT
    // =====================================================================================

    /**
     * The scroll selection packet is not tied to the twelve entries Vanilla's tooltip shows:
     * Simple Interfaces' bundle view (docs/ai/KONZEPT-BUNDLES-BEYOND-2026-10-19.md) lets the mouse
     * wheel reach every entry, so the handler must accept index 13+ of a fuller reinforced bundle
     * and still drop indices at or past the entry count and negative ones (the component's own
     * bounds check, {@code BundleContents.Mutable.toggleSelectedItem}).
     *
     * <p>What breaks it: a handler or bundle clamping the index to Vanilla's shown count, or one
     * that writes an out-of-range index into the component.
     */
    public static void selectionPacketReachesEntriesPastTheTwelfth(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.getInventory().clearContent();
        List<ItemStackTemplate> entries = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ItemStack stack = new ItemStack(Items.STONE, 1);
            stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("entry " + i));
            entries.add(ItemStackTemplate.fromNonEmptyStack(stack));
        }
        ItemStack bundle = new ItemStack(StorageItems.REINFORCED_BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(entries));
        player.getInventory().setItem(1, bundle);
        int slot = menuSlotOf(helper, player, bundle);

        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, 15), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(bundle), 15, "selection 15 of 20 entries");
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, 19), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(bundle), 19, "the last of 20 entries");
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, 20), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(bundle), -1, "index = entry count selects nothing");
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, -3), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(bundle), -1, "a negative index selects nothing");
        player.getInventory().clearContent();
        helper.succeed();
    }

    /**
     * A funnel pickup plays the pickup animation and counts the statistic for what it really took
     * (audit N7): the mixin used to read the stack <em>after</em> shrinking it, so a full pickup
     * reported 0 items (of air - no animation, no statistic) and a partial one the rest that stayed
     * on the ground instead of the part that was taken.
     *
     * <ul>
     *   <li><b>Whole:</b> 8 stone into an empty funnel bundle - the statistic rises by 8.</li>
     *   <li><b>Partial:</b> a bundle with room for 3 more takes 3, vanilla's own pickup behind the
     *       mixin puts the other 5 into the inventory and counts them itself - 8 in total. With the
     *       old count the mixin reported the 5 left over, and the sum came to 10.</li>
     * </ul>
     */
    public static void funnelPickupCountsWhatItTook(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack bundle = funnelBundle(helper);
        armHand(player, bundle);
        net.minecraft.stats.Stat<Item> stonePickedUp = net.minecraft.stats.Stats.ITEM_PICKED_UP.get(Items.STONE);
        int before = player.getStats().getValue(stonePickedUp);

        ItemEntity whole = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(2.5, 2.0, 2.5), 0);
        whole.playerTouch(player);
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8, "stone the funnel bundle took off the floor");
        helper.assertTrue(whole.isRemoved(), "the absorbed drop is still lying there");
        helper.assertValueEqual(player.getStats().getValue(stonePickedUp) - before, 8,
                "stone counted as picked up after a funnel bundle took a whole drop of 8");

        // --- a bundle with room for exactly 3 more ---
        ItemStack probe = funnelBundle(helper);
        ReinforcedBundleItem item = (ReinforcedBundleItem) probe.getItem();
        int capacity = 0;
        while (capacity < 10_000 && item.tryInsertStackFromWorld(probe, new ItemStack(Items.STONE), player)) {
            capacity++;
        }
        ItemStack almostFull = funnelBundle(helper);
        for (int i = 0; i < capacity - 3; i++) {
            item.tryInsertStackFromWorld(almostFull, new ItemStack(Items.STONE), player);
        }
        armHand(player, almostFull);
        int beforePartial = player.getStats().getValue(stonePickedUp);
        ItemEntity partial = drop(helper, new ItemStack(Items.STONE, 8), new Vec3(3.5, 2.0, 2.5), 0);
        partial.playerTouch(player);
        helper.assertValueEqual(countInBundle(almostFull, Items.STONE), capacity,
                "stone in the bundle that had room for 3 more");
        helper.assertValueEqual(player.getStats().getValue(stonePickedUp) - beforePartial, 8,
                "stone counted as picked up when the bundle took 3 and the inventory the other 5");
        helper.succeed();
    }

    /**
     * The mod's bundles close the way vanilla's do. "Open" is nothing but a selected entry in
     * {@code BundleContents} (the item model switches on {@code bundle/has_selected_item}), and
     * that index is in neither {@code BundleContents.equals} nor its stream codec: no container
     * sync ever carries it back to the client. Every place that opens or closes a bundle therefore
     * has to act on its own; the owner saw bundles that stayed open after the mouse had left them
     * and even after the inventory had been closed and reopened (2026-09-28).
     *
     * <ol>
     *   <li><b>The selection packet opens and closes.</b> A wheel notch sends an index, leaving the
     *       slot or closing the screen sends -1 ({@code ReinforcedBundleTooltipSubmenuHandler},
     *       which now also writes the client stack - that half is client-only).</li>
     *   <li><b>Picking an open bundle up closes it</b>, like vanilla's
     *       {@code BundleItem.overrideOtherStackedOnMe}: a left click with an empty cursor falls
     *       through to the ordinary pickup and clears the selection first. Before the fix the
     *       bundle rode the cursor open and landed open in the next slot.</li>
     *   <li><b>A quiver swapped for a non-arrow closes too</b> - its arrow filter returns before the
     *       bundle code, so it needs the same reset of its own.</li>
     *   <li><b>The wheel steps like vanilla's {@code ScrollWheelHandler}</b>: from "nothing
     *       selected" a notch down selects the first entry and a notch up the last one, and both
     *       ends wrap. The old handler turned "nothing" into 0 before adding the step, so the first
     *       notch down skipped the first entry.</li>
     * </ol>
     *
     * <p>What breaks this test: dropping {@code clearBundleSelection} from the fall-through of
     * {@code ReinforcedBundleItem.overrideOtherStackedOnMe} or from the quiver's arrow filter, the
     * payload handler losing -1, or {@code nextScrollSelection} clamping or skipping.
     */
    public static void bundlesCloseLikeVanillaWhenPickedUpOrLeft(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.getInventory().clearContent();

        ItemStack bundle = new ItemStack(StorageItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem bundleItem = (ReinforcedBundleItem) bundle.getItem();
        bundleItem.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player);
        bundleItem.tryInsertStackFromWorld(bundle, new ItemStack(Items.DIRT, 8), player);
        bundleItem.tryInsertStackFromWorld(bundle, new ItemStack(Items.OAK_PLANKS, 8), player);
        player.getInventory().setItem(9, bundle);
        int slot = menuSlotOf(helper, player, bundle);

        // --- 1. the packet opens and closes ---
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, 2), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(player.containerMenu.getSlot(slot).getItem()), 2,
                "a wheel notch did not open the bundle on entry 2");
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, BundleContents.NO_SELECTED_ITEM_INDEX), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(player.containerMenu.getSlot(slot).getItem()),
                BundleContents.NO_SELECTED_ITEM_INDEX, "leaving the slot did not close the bundle");

        // --- 2. picking the open bundle up closes it ---
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(slot, 1), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(player.containerMenu.getSlot(slot).getItem()), 1,
                "the bundle could not be opened again, so the pickup below would prove nothing");
        player.containerMenu.clicked(slot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        ItemStack carried = player.containerMenu.getCarried();
        helper.assertTrue(carried.is(StorageItems.REINFORCED_BUNDLE),
                "a left click with an empty cursor did not pick the bundle up but left " + carried + " on the cursor");
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(carried), BundleContents.NO_SELECTED_ITEM_INDEX,
                "the bundle is still open on the cursor after being picked up");
        helper.assertValueEqual(carried.get(DataComponents.BUNDLE_CONTENTS).size(), 3,
                "picking the bundle up changed its contents");
        player.containerMenu.setCarried(ItemStack.EMPTY);

        // --- 3. a quiver swapped for a non-arrow closes as well ---
        ItemStack quiver = new ItemStack(StorageItems.QUIVER);
        ReinforcedBundleItem quiverItem = (ReinforcedBundleItem) quiver.getItem();
        quiverItem.tryInsertStackFromWorld(quiver, new ItemStack(Items.ARROW, 8), player);
        quiverItem.tryInsertStackFromWorld(quiver, new ItemStack(Items.SPECTRAL_ARROW, 8), player);
        player.getInventory().setItem(10, quiver);
        int quiverSlot = menuSlotOf(helper, player, quiver);
        com.simplestorage.bundle.BundleNet.handle(new com.simplestorage.bundle.BundleNet.SelectPayload(quiverSlot, 1), player);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(player.containerMenu.getSlot(quiverSlot).getItem()), 1,
                "the quiver could not be opened, so the swap below would prove nothing");
        player.containerMenu.setCarried(new ItemStack(Items.DIRT, 4));
        player.containerMenu.clicked(quiverSlot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        ItemStack swapped = player.containerMenu.getCarried();
        helper.assertTrue(swapped.is(StorageItems.QUIVER),
                "clicking the quiver with dirt did not swap them; the cursor holds " + swapped);
        helper.assertValueEqual(BundleItem.getSelectedItemIndex(swapped), BundleContents.NO_SELECTED_ITEM_INDEX,
                "the quiver is still open on the cursor after being swapped for dirt");
        player.containerMenu.setCarried(ItemStack.EMPTY);

        // --- 4. the wheel steps exactly like vanilla's ScrollWheelHandler ---
        int none = BundleContents.NO_SELECTED_ITEM_INDEX;
        String steps = ReinforcedBundleItem.nextScrollSelection(-1, none, 3) + ","
                + ReinforcedBundleItem.nextScrollSelection(-1, 0, 3) + ","
                + ReinforcedBundleItem.nextScrollSelection(-1, 2, 3) + ","
                + ReinforcedBundleItem.nextScrollSelection(1, none, 3) + ","
                + ReinforcedBundleItem.nextScrollSelection(1, 0, 3) + ","
                + ReinforcedBundleItem.nextScrollSelection(1, 2, 3) + ","
                + ReinforcedBundleItem.nextScrollSelection(-1, none, 0);
        helper.assertTrue(steps.equals("0,1,0,2,2,1,-1"),
                "wheel steps (down from none, down from 0, down from last, up from none, up from 0, up from last, "
                        + "empty bundle): expected 0,1,0,2,2,1,-1 as in vanilla but were " + steps);

        helper.succeed();
    }

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
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** A reinforced bundle that takes anything off the floor, so the Funnel filter never decides. */
    private static ItemStack funnelBundle(GameTestHelper helper) {
        ItemStack bundle = new ItemStack(StorageItems.REINFORCED_BUNDLE);
        bundle.enchant(enchantment(helper, com.simplestorage.bundle.BundleEnchantments.FUNNEL), 2);
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
        helper.runBeforeTestEnd(entity::discard);
        return entity;
    }

    /** How many of {@code item} sit inside the bundle's contents. */
    private static int countInBundle(ItemStack bundle, Item item) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : (contents).itemCopies().toList()) {
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

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
