package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.SledgehammerUsageEvent;
import com.simplebuilding.util.VersatilityUsageEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
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

    /** Upper bound for the building wand tick loop, so a wand that never finishes fails instead of hanging. */
    private static final int WAND_TICK_CAP = 60;

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

        helper.succeed();
    }

    /**
     * Break Through adds layers <em>behind the mined face</em>, and which way "behind" points is
     * decided by the side the player hit: {@code SledgehammerItem#getBlocksToBeDestroyed} maps the
     * hit side onto one of three axes and takes the sign from it. Each of those mappings is its
     * own line in that method, so each one is driven here:
     *
     * <ul>
     *   <li>looking down - hit side {@code UP}, the extra layers go downwards,</li>
     *   <li>looking up - hit side {@code DOWN}, they go upwards,</li>
     *   <li>facing south - hit side {@code NORTH}, they go south (+z),</li>
     *   <li>facing west - hit side {@code EAST}, they go west (-x).</li>
     * </ul>
     *
     * <p>The two horizontal cases carry different sign expressions in the source and are checked
     * from opposite sides on purpose: a single horizontal case would pass just as happily if both
     * branches dug towards the player instead of away from them.
     *
     * <p>Depth is pinned at both registered levels. Break Through is registered with a max level
     * of 2 in {@code ModEnchantments} (the "Max Level 1" comment above that registration is
     * wrong), so level I has to stop after exactly one extra layer and level II after exactly two.
     *
     * <p>Sneaking suppresses it for the same reason as Radius: it is the player's only way to take
     * a plain face with an enchanted hammer.
     *
     * <p>What breaks it: losing the sneak gate, reading the level from anywhere but the held
     * stack, clamping the depth (the level II case comes up a layer short), letting the depth grow
     * past the level (the "one layer only" and "two layers only" survivors go), flipping the sign
     * in either horizontal branch or deleting the branch outright (the extra layer lands on the
     * wrong side of the origin, or nowhere), and collapsing {@code (sideHit == UP) ? -z : z} to a
     * constant (the look-up case then digs into the floor instead of the ceiling).
     */
    public static void breakThroughAddsLayersBehindTheMinedFace(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, 90.0F);

        // --- without it, the layer below survives ---
        fillCube(helper, 1, 5, 1, 2, 1, 5, Blocks.STONE);
        player.setShiftKeyDown(false);
        swing(helper, player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(1, -1, 0));

        // --- with Break Through I, the layer below goes as well ---
        fillCube(helper, 1, 5, 0, 2, 1, 5, Blocks.STONE);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, -1, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(0, -1, 0));
        // Exactly one extra layer: the second one below is level II's, and without this the depth
        // could be wired to the maximum level instead of the level on the stack.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(0, -2, 0));

        // --- sneaking cancels it ---
        fillCube(helper, 1, 5, 0, 2, 1, 5, Blocks.STONE);
        player.setShiftKeyDown(true);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(1, -1, 0));
        player.setShiftKeyDown(false);

        // --- looking up: hit side DOWN, so the layers go up - and level II reaches two of them ---
        aim(player, 0.0F, -90.0F);
        fillCube(helper, 1, 5, 1, 5, 1, 5, Blocks.STONE);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 2));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(0, 1, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(0, 2, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 2, 0));
        // Two layers and no more ...
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(0, 3, 0));
        // ... and they were taken above the face, not below it.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(0, -1, 0));

        // --- facing south: hit side NORTH, so the extra layer is the one further south ---
        aim(player, 0.0F, 0.0F);
        helper.assertTrue(player.getDirection() == Direction.SOUTH,
                "the mock player is not facing south, so this case would no longer pin the "
                        + "north/south branch of the hit side");
        fillCube(helper, 1, 5, 1, 3, 1, 5, Blocks.STONE);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(0, 0, 1));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(1, 1, 1));
        // The block on the player's side of the face is not "behind" anything and must stand.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(0, 0, -1));

        // --- facing west: hit side EAST, the branch with the other sign ---
        aim(player, 90.0F, 0.0F);
        helper.assertTrue(player.getDirection() == Direction.WEST,
                "the mock player is not facing west, so this case would no longer pin the "
                        + "east/west branch of the hit side");
        fillCube(helper, 1, 5, 1, 3, 1, 5, Blocks.STONE);
        swing(helper, player, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(-1, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(-1, 1, 1));
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(1, 0, 0));

        helper.succeed();
    }

    // =====================================================================================
    // VERSATILITY
    // =====================================================================================

    /**
     * Versatility swaps a better tool into the hand when the player sneak-hits a block. Level I
     * may only look through the hotbar, level II through the whole inventory - that difference is
     * the entire point of the second level, so both are checked, and both are checked at their
     * <em>boundaries</em>: slot 8 (last hotbar slot) and slot 9 (first slot past it) for level I,
     * slot 35 (last inventory slot) for level II. Cases well inside each range prove only that the
     * search runs at all; narrowing {@code searchRange} to a handful of slots would leave them
     * green while most of the player's inventory silently dropped out of reach.
     *
     * <p><b>What this test cannot reach.</b> The handler opens with
     * {@code if (world.isClientSide() || !player.isShiftKeyDown())}. Only the sneak half of that
     * line is testable here: {@code helper.getLevel()} is always a {@code ServerLevel}, so
     * {@code isClientSide()} is constantly false and an assertion about it could not fail no
     * matter what the mod did. The guard is not decoration - Fabric hangs this handler on
     * {@code AttackBlockCallback}, which also fires client side - it is simply out of reach of a
     * gametest, and no assertion below should be read as covering it.
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

        // --- Versatility I, pickaxe in the LAST hotbar slot: the whole hotbar is in range ---
        arm(player, 0, versatilityShovel(helper, 1), 8, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Versatility I stopped short of the last hotbar slot, hand holds "
                        + player.getMainHandItem());
        helper.assertTrue(player.getInventory().getSelectedSlot() == 8,
                "Versatility I did not move the selected slot to the pickaxe in slot 8");

        // --- Versatility I, pickaxe outside the hotbar: must NOT be found ---
        arm(player, 0, versatilityShovel(helper, 1), 20, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SHOVEL),
                "Versatility I reached past the hotbar, which is level II's job; hand holds "
                        + player.getMainHandItem());

        // --- Versatility I, pickaxe in the FIRST slot past the hotbar: the exact boundary ---
        arm(player, 0, versatilityShovel(helper, 1), 9, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SHOVEL),
                "Versatility I reached one slot past the hotbar, hand holds "
                        + player.getMainHandItem());

        // --- Versatility II: the same pickaxe is found and swapped into the hand ---
        arm(player, 0, versatilityShovel(helper, 2), 20, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Versatility II did not search the full inventory, hand holds "
                        + player.getMainHandItem());
        helper.assertTrue(player.getInventory().getItem(20).is(Items.DIAMOND_SHOVEL),
                "Versatility II did not park the shovel in the slot the pickaxe came from");

        // --- Versatility II, pickaxe in the LAST inventory slot: all 36 slots are in range ---
        arm(player, 0, versatilityShovel(helper, 2), 35, new ItemStack(Items.DIAMOND_PICKAXE));
        attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Versatility II stopped short of the last inventory slot, hand holds "
                        + player.getMainHandItem());
        helper.assertTrue(player.getInventory().getItem(35).is(Items.DIAMOND_SHOVEL),
                "Versatility II did not park the shovel in slot 35");

        // --- standing upright: no swap at all ---
        player.setShiftKeyDown(false);
        arm(player, 0, versatilityShovel(helper, 2), 3, new ItemStack(Items.DIAMOND_PICKAXE));
        InteractionResult result = attack(helper, player, absolute);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SHOVEL),
                "the tool was swapped without sneaking, result was " + result);

        helper.succeed();
    }

    // =====================================================================================
    // FUNNEL
    // =====================================================================================

    /**
     * Funnel decides what a reinforced bundle vacuums off the floor. Level I is a filter - only
     * what is already inside - level II takes everything. Without the enchantment the bundle
     * must pick up nothing, otherwise every bundle in the game would hoover the ground.
     *
     * <p>"Already inside" means item <em>and</em> components. Telling the two readings apart needs
     * a ground stack whose item is in the bundle and whose components are not, so the last block
     * offers a stone that differs from the one inside by its custom name only - by item it is a
     * match, by kind it is not. Both directions are asserted, because the filter has to look at
     * the components of the stored stack and of the ground stack alike, and the named bundle
     * pairs its refusal with the matching name being taken: a filter that refuses everything is
     * not the same as one that compares by kind.
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

        // --- Funnel I sorts by kind, not by item: a named stone is not the stone inside ---
        // Comparing by item alone would let a Funnel I quiver holding water breathing arrows
        // vacuum up poison ones, and this bundle swallow a renamed stone.
        helper.assertTrue(!item.canAutoPickup(funnelOne, namedStone("funnel stone"), helper.getLevel()),
                "Funnel I took a stone that differs from the one inside by its custom name - the "
                        + "filter compares by item alone, so every component variant looks like a match");

        // --- the same, with the components on the stored side ---
        ItemStack funnelNamed = new ItemStack(ModItems.REINFORCED_BUNDLE);
        funnelNamed.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        helper.assertTrue(item.tryInsertStackFromWorld(funnelNamed, namedStone("funnel stone"), player),
                "test setup broken: the Funnel bundle refused the named stone");
        helper.assertTrue(item.canAutoPickup(funnelNamed, namedStone("funnel stone"), helper.getLevel()),
                "Funnel I refused a stone carrying exactly the name of the one inside, so the "
                        + "components of the stored stack do not survive the bundle");
        helper.assertTrue(!item.canAutoPickup(funnelNamed, stone, helper.getLevel()),
                "Funnel I took a plain stone although the bundle only holds a named one");

        // --- Funnel II: everything, even from an empty bundle ---
        ItemStack funnelTwo = new ItemStack(ModItems.REINFORCED_BUNDLE);
        funnelTwo.enchant(enchantment(helper, ModEnchantments.FUNNEL), 2);
        helper.assertTrue(item.canAutoPickup(funnelTwo, dirt, helper.getLevel()),
                "Funnel II did not take an arbitrary item");

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
     *
     * <p>"Inert" is asserted from both sides, because the two sides can be finished separately.
     * The <em>data</em> side is the empty effect list below. The <em>code</em> side is a real
     * building wand run: the same plane is built three times, once with a plain wand and once with
     * each enchantment, and all three runs have to agree on every position they filled, on how
     * many ticks they took and on how many blocks they spent. An effect written into
     * {@code BuildingWandItem} rather than into the enchantment JSON - a wider radius, an extra
     * layer, a different pace - leaves the effect list empty and would pass the data half without
     * a word; this is the half that can see it. What it cannot see is an effect on some item other
     * than the wand, which is the only one the two are applicable to today.
     */
    public static void coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown(GameTestHelper helper) {
        for (ResourceKey<Enchantment> key : List.of(ModEnchantments.COVER, ModEnchantments.BRIDGE)) {
            Enchantment enchantment = enchantment(helper, key).value();
            helper.assertTrue(enchantment.effects().isEmpty(),
                    key.identifier() + " grew an effect. That is good news, but it now needs a real "
                            + "behaviour test - replace this marker with one.");
        }

        // --- the code side: the wand has to behave identically with and without them ---
        ServerPlayer player = mockPlayer(helper, 0.0F);
        // Every payment the wand makes sits behind instabuild, so the block count below only
        // measures anything with it cleared.
        player.getAbilities().instabuild = false;
        BlockPos anchor = new BlockPos(3, 1, 3);

        WandRun plain = runWandOnce(helper, player, wandWithRadiusOne(helper, null), anchor);
        helper.assertValueEqual(plain.placed().size(), 9,
                "the unenchanted wand did not build the 3x3 its radius setting asks for, it placed "
                        + plain.placed().size() + " blocks - the comparisons below would prove nothing");

        for (ResourceKey<Enchantment> key : List.of(ModEnchantments.COVER, ModEnchantments.BRIDGE)) {
            WandRun enchantedRun = runWandOnce(helper, player, wandWithRadiusOne(helper, key), anchor);
            String name = String.valueOf(key.identifier());
            helper.assertValueEqual(enchantedRun.placed(), plain.placed(),
                    name + " changed which blocks the wand places. That is a real feature now, so "
                            + "this marker has to be replaced by a test that states what it does.");
            helper.assertValueEqual(enchantedRun.ticks(), plain.ticks(),
                    name + " changed how long the wand takes (" + enchantedRun.ticks() + " ticks "
                            + "against " + plain.ticks() + "), so it is no longer inert.");
            helper.assertValueEqual(enchantedRun.spent(), plain.spent(),
                    name + " changed how many blocks the wand spends, so it is no longer inert.");
        }

        helper.succeed();
    }

    /** What one building wand run did: how long it took, what it filled and what it cost. */
    private record WandRun(int ticks, Set<BlockPos> placed, int spent) {
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, float xRot) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 4.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, xRot);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the
        // player list non-empty and the gametest server then stalls on shutdown - a failing
        // test would cost minutes of wall clock instead of seconds.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }


    /**
     * Points the mock player without moving it. Both the hit side the sledgehammer digs behind
     * and the mining direction are read off pitch and yaw, so aiming is what selects the branch
     * under test.
     */
    private static void aim(ServerPlayer player, float yRot, float xRot) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yRot, xRot);
    }

    private static void fillLayer(GameTestHelper helper, int y, int minX, int maxX, int minZ, int maxZ, Block block) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, y, z), block);
            }
        }
    }

    /**
     * Rebuilds a solid block of stone around the hammer centre. The horizontal Break Through
     * cases dig along x or z, so a two layer stack is not enough any more - every case needs
     * whole material on all three axes and a fresh one, because the case before it left holes.
     */
    private static void fillCube(GameTestHelper helper, int minX, int maxX, int minY, int maxY,
                                 int minZ, int maxZ, Block block) {
        for (int y = minY; y <= maxY; y++) {
            fillLayer(helper, y, minX, maxX, minZ, maxZ, block);
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

    /**
     * One stone carrying {@code name}. Two of these are the same item and a different kind, which
     * is what separates a comparison by item from one by item and components.
     */
    private static ItemStack namedStone(String name) {
        ItemStack stack = new ItemStack(Items.STONE, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /**
     * A diamond building wand pinned to radius 1, optionally carrying one enchantment. The radius
     * is written into the wand's own settings so the expected plane is a 3x3 rather than whatever
     * the tier happens to allow, and the enchantment is verified to have landed on the stack -
     * comparing an "enchanted" run against a plain one proves nothing if the two stacks are the
     * same.
     */
    private static ItemStack wandWithRadiusOne(GameTestHelper helper, ResourceKey<Enchantment> key) {
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        CompoundTag settings = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        settings.putInt("SettingsRadius", 1);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));

        if (key != null) {
            Holder<Enchantment> holder = enchantment(helper, key);
            wand.enchant(holder, 1);
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(holder, wand) > 0,
                    key.identifier() + " could not be put on a building wand at all, so the run it "
                            + "is compared with would be a second plain run");
        }
        return wand;
    }

    /**
     * Arms the wand on the top face of {@code anchor} and drives its own {@code inventoryTick}
     * until it switches itself off, then reads back what it built.
     *
     * <p>The build window is cleared first and the supplies are handed out fresh, so several runs
     * can share one anchor and be compared position for position. The wand's item hook is called
     * directly rather than through a player tick: a gametest server never pumps a mock player's
     * connection, and this way the whole build happens inside one test tick.
     */
    private static WandRun runWandOnce(GameTestHelper helper, ServerPlayer player, ItemStack wand,
                                       BlockPos anchor) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    helper.setBlock(anchor.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }
        helper.setBlock(anchor, Blocks.STONE);

        ItemStack supplies = new ItemStack(Items.OAK_PLANKS, 64);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.getInventory().setItem(1, supplies);

        BlockPos absolute = helper.absolutePos(anchor);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5),
                Direction.UP, absolute, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        InteractionResult armed = wand.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand did not arm itself on the clicked face, it returned " + armed);

        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_TICK_CAP) {
            wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < WAND_TICK_CAP,
                "the wand was still building after " + WAND_TICK_CAP + " inventory ticks");

        Set<BlockPos> placed = new HashSet<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos offset = new BlockPos(dx, dy, dz);
                    if (!helper.getBlockState(anchor.offset(dx, dy, dz)).isAir()) {
                        placed.add(offset);
                    }
                }
            }
        }
        return new WandRun(ticks, placed, 64 - supplies.getCount());
    }

    private static boolean wandIsActive(ItemStack wand) {
        return wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr("Active", false);
    }
}
