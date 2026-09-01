package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.SledgehammerUsageEvent;
import com.simplebuilding.util.VersatilityUsageEvent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * What the mod's enchantments actually <em>do</em>.
 *
 * <p>The data integrity tests already prove every enchantment is registered and that its tags
 * resolve. That is not the same as the enchantment having an effect: an enchantment can be
 * obtainable from loot, applicable in an anvil and completely inert. These tests drive the code
 * that reads each enchantment instead.
 *
 * <p>Two of the mod's enchantments have no effect to test at all - see
 * {@link #coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown()}.
 */
public final class EnchantmentEffectTests {

    private EnchantmentEffectTests() {
    }

    /** Centre of the horizontal block field the sledgehammer tests mine. */
    private static final BlockPos HAMMER_CENTRE = new BlockPos(3, 2, 3);

    // =====================================================================================
    // SLEDGEHAMMER: RADIUS AND BREAK THROUGH
    // =====================================================================================

    /**
     * Radius widens the mined face from 3x3 to 5x5. Sneaking has to suppress it - that is the
     * player's only way to take a single block with an enchanted hammer, so if the sneak check
     * is lost the enchantment becomes impossible to switch off.
     */
    public static void radiusWidensTheSledgehammerFaceAndSneakingSuppressesIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, 90.0F);

        // --- without the enchantment: the plain 3x3 ---
        fillLayer(helper, 2, 0, 6, 0, 6, Blocks.STONE);
        player.setShiftKeyDown(false);
        swing(helper, player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(2, 0, 0));

        // --- with Radius I: the ring at distance 2 goes too ---
        fillLayer(helper, 2, 0, 6, 0, 6, Blocks.STONE);
        swing(helper, player, hammerWith(helper, ModEnchantments.RADIUS, 1));

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue; // the origin is vanilla's job, the hook leaves it standing
                }
                helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(dx, 0, dz));
            }
        }
        // Distance 3 has to survive, otherwise the radius grew without bound.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(3, 0, 0));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(0, 0, 3));

        // --- sneaking cancels the bonus, back to 3x3 ---
        fillLayer(helper, 2, 0, 6, 0, 6, Blocks.STONE);
        player.setShiftKeyDown(true);
        swing(helper, player, hammerWith(helper, ModEnchantments.RADIUS, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(2, 0, 0));
        player.setShiftKeyDown(false);

        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    /**
     * Break Through adds layers behind the mined face. The player looks straight down, so the
     * extra layer is the one below. Sneaking suppresses it for the same reason as Radius.
     */
    public static void breakThroughAddsLayersBehindTheMinedFace(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, 90.0F);

        // --- without it, the layer below survives ---
        fillLayer(helper, 2, 1, 5, 1, 5, Blocks.STONE);
        fillLayer(helper, 1, 1, 5, 1, 5, Blocks.STONE);
        player.setShiftKeyDown(false);
        swing(helper, player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(1, -1, 0));

        // --- with Break Through I, the layer below goes as well ---
        fillLayer(helper, 2, 1, 5, 1, 5, Blocks.STONE);
        fillLayer(helper, 1, 1, 5, 1, 5, Blocks.STONE);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, -1, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(0, -1, 0));

        // --- sneaking cancels it ---
        fillLayer(helper, 2, 1, 5, 1, 5, Blocks.STONE);
        fillLayer(helper, 1, 1, 5, 1, 5, Blocks.STONE);
        player.setShiftKeyDown(true);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(1, -1, 0));
        player.setShiftKeyDown(false);

        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    // =====================================================================================
    // VERSATILITY
    // =====================================================================================

    /**
     * Versatility swaps a better tool into the hand when the player sneak-hits a block. Level I
     * may only look through the hotbar, level II through the whole inventory - that difference
     * is the entire point of the second level, so both are checked.
     */
    public static void versatilitySwapsInTheBetterToolWhileSneaking(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, 0.0F);
        BlockPos stone = new BlockPos(3, 1, 3);
        helper.setBlock(stone, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(stone);
        player.setShiftKeyDown(true);

        // --- no enchantment: the shovel stays in hand even though a pickaxe is right there ---
        arm(player, 0, new ItemStack(Items.DIAMOND_SHOVEL), 3, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SHOVEL),
                "an unenchanted shovel was swapped out anyway");

        // --- Versatility I, pickaxe in the hotbar: the selection moves to it ---
        arm(player, 0, versatilityShovel(helper, 1), 3, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Versatility I did not reach for the pickaxe in the hotbar, hand holds "
                        + player.getMainHandItem());
        // A hotbar hit only moves the selection; the items must stay where they were, otherwise
        // the player's hotbar layout would be rearranged behind their back.
        helper.assertTrue(player.getInventory().getSelectedSlot() == 3,
                "Versatility I did not move the selected slot to the pickaxe");
        helper.assertTrue(player.getInventory().getItem(0).is(Items.DIAMOND_SHOVEL),
                "Versatility I moved items around instead of just changing the selection");

        // --- Versatility I, pickaxe outside the hotbar: must NOT be found ---
        arm(player, 0, versatilityShovel(helper, 1), 20, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SHOVEL),
                "Versatility I reached past the hotbar, which is level II's job; hand holds "
                        + player.getMainHandItem());

        // --- Versatility II: the same pickaxe is found and swapped into the hand ---
        arm(player, 0, versatilityShovel(helper, 2), 20, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Versatility II did not search the full inventory, hand holds "
                        + player.getMainHandItem());
        helper.assertTrue(player.getInventory().getItem(20).is(Items.DIAMOND_SHOVEL),
                "Versatility II did not park the shovel in the slot the pickaxe came from");

        // --- standing upright: no swap at all ---
        player.setShiftKeyDown(false);
        arm(player, 0, versatilityShovel(helper, 2), 3, new ItemStack(Items.DIAMOND_PICKAXE));
        InteractionResult result = attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SHOVEL),
                "the tool was swapped without sneaking, result was " + result);

        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    // =====================================================================================
    // FUNNEL
    // =====================================================================================

    /**
     * Funnel decides what a reinforced bundle vacuums off the floor. Level I is a filter - only
     * what is already inside - level II takes everything. Without the enchantment the bundle
     * must pick up nothing, otherwise every bundle in the game would hoover the ground.
     */
    public static void funnelDecidesWhatTheBundlePicksUp(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, 0.0F);
        ItemStack stone = new ItemStack(Items.STONE, 1);
        ItemStack dirt = new ItemStack(Items.DIRT, 1);

        ItemStack plain = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) plain.getItem();
        helper.assertTrue(!item.canAutoPickup(plain, stone, helper.getLevel()),
                "a bundle without Funnel picked items off the ground");

        // --- Funnel I on an empty bundle: nothing matches, so nothing is taken ---
        ItemStack funnelOne = new ItemStack(ModItems.REINFORCED_BUNDLE);
        funnelOne.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        helper.assertTrue(!item.canAutoPickup(funnelOne, stone, helper.getLevel()),
                "Funnel I picked up an item the empty bundle does not hold");

        // --- Funnel I with stone inside: stone yes, dirt no ---
        item.tryInsertStackFromWorld(funnelOne, new ItemStack(Items.STONE, 8), player);
        helper.assertTrue(item.canAutoPickup(funnelOne, stone, helper.getLevel()),
                "Funnel I refused an item the bundle already holds");
        helper.assertTrue(!item.canAutoPickup(funnelOne, dirt, helper.getLevel()),
                "Funnel I is not filtering, it took an item the bundle does not hold");

        // --- Funnel II: everything, even from an empty bundle ---
        ItemStack funnelTwo = new ItemStack(ModItems.REINFORCED_BUNDLE);
        funnelTwo.enchant(enchantment(helper, ModEnchantments.FUNNEL), 2);
        helper.assertTrue(item.canAutoPickup(funnelTwo, dirt, helper.getLevel()),
                "Funnel II did not take an arbitrary item");

        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    // =====================================================================================
    // DATA DRIVEN EFFECTS
    // =====================================================================================

    /**
     * Two enchantments have no code behind them at all - their whole effect is a component in
     * the generated JSON. A datagen change can drop those silently, and nothing else in the
     * suite would notice, because the enchantment still registers and its tags still resolve.
     */
    public static void dataDrivenEnchantmentEffectsSurviveDatagen(GameTestHelper helper) {
        Enchantment kineticProtection = enchantment(helper, ModEnchantments.KINETIC_PROTECTION).value();
        helper.assertTrue(kineticProtection.effects().has(EnchantmentEffectComponents.DAMAGE_PROTECTION),
                "Kinetic Protection lost its damage_protection effect and now does nothing");

        Enchantment range = enchantment(helper, ModEnchantments.RANGE).value();
        List<?> attributes = range.effects().get(EnchantmentEffectComponents.ATTRIBUTES);
        helper.assertTrue(attributes != null && !attributes.isEmpty(),
                "Range lost its attribute modifier and now does nothing");

        helper.succeed();
    }

    /**
     * Cover and Bridge are registered, obtainable from loot, applicable to building wands - and
     * inert. No code reads them and their generated JSON carries no effect component, so an
     * enchanted wand behaves exactly like an unenchanted one.
     *
     * <p>This test pins that down rather than hiding it: it fails the moment either one grows an
     * effect, which is the point at which a real behaviour test has to be written for it. It is
     * <em>not</em> an assertion that the current state is correct - it is a marker that the two
     * are unfinished.
     */
    public static void coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown(GameTestHelper helper) {
        for (ResourceKey<Enchantment> key : List.of(ModEnchantments.COVER, ModEnchantments.BRIDGE)) {
            Enchantment enchantment = enchantment(helper, key).value();
            helper.assertTrue(enchantment.effects().isEmpty(),
                    key.identifier() + " grew an effect. That is good news, but it now needs a real "
                            + "behaviour test - replace this marker with one.");
        }
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static ServerPlayer mockPlayer(GameTestHelper helper, float xRot) {
        ServerPlayer player = MockPlayers.create(helper);
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 4.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, xRot);
        return player;
    }


    private static void fillLayer(GameTestHelper helper, int y, int minX, int maxX, int minZ, int maxZ, Block block) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, y, z), block);
            }
        }
    }

    /** Runs the mod's block break hook on {@link #HAMMER_CENTRE}, as the tool tests do. */
    private static void swing(GameTestHelper helper, ServerPlayer player, ItemStack hammer) {
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        BlockPos origin = helper.absolutePos(HAMMER_CENTRE);
        SledgehammerUsageEvent.handleBeforeBlockBreak(
                helper.getLevel(), player, origin, helper.getLevel().getBlockState(origin), null);
    }

    private static ItemStack hammerWith(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    /**
     * Clears the inventory, puts {@code hand} in the selected slot and {@code other} in one more
     * slot. Resetting the selected slot each time is load bearing: a hotbar hit moves the
     * selection rather than the items, so without this the next case would start with an empty
     * hand and quietly prove nothing.
     */
    private static void arm(ServerPlayer player, int handSlot, ItemStack hand, int otherSlot, ItemStack other) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(handSlot);
        player.getInventory().setItem(handSlot, hand);
        player.getInventory().setItem(otherSlot, other);
    }

    private static InteractionResult attack(GameTestHelper helper, ServerPlayer player, BlockPos absolutePos) {
        return VersatilityUsageEvent.handleAttackBlock(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, absolutePos, Direction.UP);
    }

    private static ItemStack versatilityShovel(GameTestHelper helper, int level) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SHOVEL);
        stack.enchant(enchantment(helper, ModEnchantments.VERSATILITY), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
