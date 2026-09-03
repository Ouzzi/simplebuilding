package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OreDetectorItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The ore detector's sonar: which block it locks onto, how far its "power" carries through
 * different rock, and the two clicks that configure it.
 *
 * <h2>How the search is driven</h2>
 *
 * <p>{@code OreDetectorItem#inventoryTick} turns its search result into three sounds and a
 * particle beam and into nothing else - no block, no entity, no component changes. A gametest
 * mock player swallows both sounds and particles, so the tick itself proves nothing. The item
 * therefore exposes the decision on its own as {@code OreDetectorItem#findTarget(ServerLevel,
 * ItemStack, Vec3)}, which is what the tick calls; every test below asks that method which
 * block it would ping and compares the answer against an exact position.
 *
 * <h2>The sight line and its arithmetic</h2>
 *
 * <p>All search tests look along +X from the centre of the block at relative {@code (1,3,3)},
 * so the ray runs exactly through the centres of the blocks in that row and the cost the item
 * accumulates can be stated in closed form. {@code canReach} walks in 0.5 steps, charges
 * {@code 0.5 * density * multiplier} per step and stops when it enters the target block, so a
 * target {@code n} blocks out is charged
 *
 * <pre>cost = 0.5 * multiplier * (density(eye block) + 2 * sum of the densities of the n-1 blocks between)</pre>
 *
 * <p>With the unenchanted multiplier of 2 and an air eye block that is {@code 1 + 2 * (sum)}.
 * The numbers quoted in each test come from that formula, which is why every "is found" case
 * sits a couple of points under the budget and every "is not found" case a couple over: the
 * tests pin the cutoff, not a coincidence.
 *
 * <p>Positions are asserted, never mere "something was found". The gametest rooms of the other
 * suites stand a few blocks away and the detector's radius reaches well past our own 8x8x8
 * room, so a bare "found nothing" could be broken by a neighbour's ore. An "is not found" case
 * therefore asserts that <em>this</em> position is not the answer, which stays true whatever a
 * neighbour contributes, and every "is found" case names a position closer than any neighbour
 * could be.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>That the tick still runs its search through {@code findTarget}.</b> Every search test
 *       below calls that method itself; nothing here forces {@code inventoryTick} to keep going
 *       through it. Whoever inlines the search back into the tick, or adds a second search path
 *       beside it, leaves this whole file green while the item in a player's hand pings something
 *       else. The seam is untestable from the tick side for the same reason it exists: all the
 *       tick does with the result is sounds and particles, and those are discarded.</li>
 *   <li><b>The two guards in front of the search</b> - that it only runs for a stack in the main
 *       or off hand, and only on every 20th game tick. Both guards decide whether sounds and
 *       particles are emitted, and nothing else; there is no server side state that records
 *       whether a tick scanned. Driving {@code inventoryTick} from a test therefore cannot tell
 *       the two outcomes apart.</li>
 *   <li><b>The ping</b> itself: the amethyst chime, the sculk click, the target block's break
 *       sound, the distance-to-pitch curve in {@code getPingPitch} and the particle beam. These
 *       are packets to nearby players, and the mock player's connection discards them.</li>
 *   <li><b>The overlay messages</b> "Detector Mode: ..." and "Calibrated to: ..." - same reason.
 *       What can be checked is that the state behind them changed, and that is asserted.</li>
 *   <li><b>The durability a mode switch costs in survival.</b> {@code cycleMode} pays it behind
 *       {@code !player.isCreative()}, and no mock player can be on that side of the guard and
 *       reachable at the same time: the in-level mock ({@code GameTestHelper$3}) hard-overrides
 *       {@code gameMode()} to {@code CREATIVE}, and the detached mock from
 *       {@code makeMockServerPlayer(GameType)} - the one {@link ConsumptionAndDurabilityTests}
 *       uses for exactly this - has no {@code connection}, while {@code cycleMode} sends an
 *       overlay message before it ever reaches the {@code hurtAndBreak}. The creative half is
 *       asserted instead, and {@link #modeSwitchIsFreeInCreativeAndTheToolStaysUnstackable}
 *       shows that vanilla was not the one keeping the tool pristine.</li>
 *   <li><b>The colours</b> the tooltip and the messages use ({@code ChatFormatting}) -
 *       {@code Component#getString()} drops the style, and colour is a client concern.</li>
 *   <li><b>The crafting recipe's pattern and the loot tables</b> that carry Constructor's Touch
 *       books. Both are checkable server side but belong to the data suites next door
 *       ({@link DataIntegrityTests}) rather than to the item's behaviour, and the loot side is
 *       one shared list for every mod loot modification, not an ore detector matter.</li>
 * </ul>
 *
 * <p>The tooltip assertions below spell out English strings on purpose: the item builds its
 * tooltip from {@code Component.literal}, so those words are hardcoded and not translatable.
 * The assertions are the record of that; they go red if someone moves the tooltip to a
 * translation key, which is precisely when this file should be looked at again.
 */
public final class OreDetectorTests {

    private OreDetectorTests() {
    }

    /** Mode indices as they are written into the item's {@code Mode} NBT value. */
    private static final int MODE_IRON = 0;
    private static final int MODE_GOLD = 1;
    private static final int MODE_DIAMOND = 2;
    private static final int MODE_NETHERITE = 3;
    private static final int MODE_ALL = 4;
    private static final int MODE_CUSTOM = 5;

    /** Height and depth of the sight line every search test looks along. */
    private static final int ROW_Y = 3;
    private static final int ROW_Z = 3;

    /** Block the eye sits in; the search always starts at its centre. */
    private static final BlockPos EYE_BLOCK = new BlockPos(1, ROW_Y, ROW_Z);
    private static final Vec3 EYE = new Vec3(EYE_BLOCK.getX() + 0.5, EYE_BLOCK.getY() + 0.5, EYE_BLOCK.getZ() + 0.5);

    /** Upright oak log used as the calibration sample; outside the sight line's corridor. */
    private static final BlockPos SAMPLE_LOG = new BlockPos(4, 1, ROW_Z);

    private static final String MODE_PREFIX = "Mode: ";

    // =====================================================================================
    // SEARCH
    // =====================================================================================

    /**
     * Of everything a mode matches, the detector reports the closest block it can still pay for -
     * and the budget that decides "can pay for" is the mode's own, not one shared number.
     *
     * <p>Six blocks of open air cost 11 points. Iron carries 24 and reaches; Netherite carries
     * only 10 and stops one block short, which is asserted at the same distance the iron mode
     * clears, so the refusal cannot be blamed on the room or the scan radius.
     *
     * <p>Gold (18) and diamond (16) are measured the same way, because otherwise the tooltip in
     * {@link #tooltipNamesEveryModeWithItsPowerAndTarget} would be the only thing saying what
     * those two budgets are - a mode could advertise 18 and search with 24. They are only two
     * points apart, so the sight line that separates them is built to cost exactly 17: two blocks
     * of stone and two of air in front of a target 5 blocks out.
     *
     * <p>What breaks this: returning the first hit of the {@code x/y/z} loop instead of the
     * nearest one (the loop runs from -radius, so a farther block would win); dropping the
     * {@code distanceSq >= bestDistanceSq} guard; giving every mode the same budget, or reading
     * the budget as the scan radius only and not as the cost cap - the far target stays inside
     * the radius in every step here, so only the cost cap can reject it.
     */
    public static void detectorReportsTheNearestTargetInsideItsBudget(GameTestHelper helper) {
        ItemStack iron = detectorInMode(MODE_IRON);

        // --- two iron ores on the same line, 2 and 5 blocks out ---
        clearCorridor(helper);
        helper.setBlock(rowPos(3), Blocks.IRON_ORE);
        helper.setBlock(rowPos(6), Blocks.IRON_ORE);
        assertFinds(helper, iron, rowPos(3), "the nearer of two iron ores");

        // Taking the near one away must reveal the far one: it was reachable all along, it just
        // lost on distance. Without this the assertion above would also pass for a detector that
        // simply cannot see 5 blocks.
        helper.setBlock(rowPos(3), Blocks.AIR);
        assertFinds(helper, iron, rowPos(6), "the remaining iron ore 5 blocks out");

        // --- the budget is per mode: 6 blocks of air cost 11, iron pays 11 of 24, netherite cannot ---
        clearCorridor(helper);
        ItemStack netherite = detectorInMode(MODE_NETHERITE);
        helper.setBlock(rowPos(7), Blocks.ANCIENT_DEBRIS);
        assertDoesNotFind(helper, netherite, rowPos(7),
                "ancient debris 6 blocks out through open air, which costs 11 of the netherite budget of 10");

        helper.setBlock(rowPos(7), Blocks.AIR);
        helper.setBlock(rowPos(6), Blocks.ANCIENT_DEBRIS);
        assertFinds(helper, netherite, rowPos(6), "ancient debris 5 blocks out, which costs 9 of 10");

        helper.setBlock(rowPos(6), Blocks.AIR);
        helper.setBlock(rowPos(7), Blocks.IRON_ORE);
        assertFinds(helper, iron, rowPos(7),
                "iron ore at the very distance the netherite mode had to refuse");

        // --- gold pays 17 of 18 for the same sight line that diamond cannot pay 17 of 16 for ---
        clearCorridor(helper);
        fillCorridor(helper, 2, 3, Blocks.STONE);
        helper.setBlock(rowPos(6), Blocks.GOLD_ORE);
        assertFinds(helper, detectorInMode(MODE_GOLD), rowPos(6),
                "gold ore 5 blocks out behind two stone, which costs 17 of the gold budget of 18");

        helper.setBlock(rowPos(6), Blocks.DIAMOND_ORE);
        assertDoesNotFind(helper, detectorInMode(MODE_DIAMOND), rowPos(6),
                "diamond ore on that same 17 point sight line, one point past the diamond budget of 16");

        // The control for the refusal: take one of the two stone blocks away and the cost drops to
        // 13, which the diamond mode does pay for the very same block. Without this line the
        // assertion above would also hold for a diamond mode that searches nothing at all.
        fillCorridor(helper, 2, 2, Blocks.AIR);
        assertFinds(helper, detectorInMode(MODE_DIAMOND), rowPos(6),
                "diamond ore 5 blocks out behind one stone, which costs 13 of 16");

        helper.succeed();
    }

    /**
     * Each mode hunts the blocks its own tag names - and only those. Every case puts a block
     * that must be ignored one step in front of the block that must be found, so a mode that
     * matched too much would report the decoy instead and fail on the position.
     *
     * <p>The deepslate variants are the point of the iron, gold and diamond cases: they share
     * no block with their stone cousins and are only reachable through the tag. This is the
     * block the 26.2 port moved from {@code BlockTags} to {@code BlockItemTags.X.block()}, so
     * these are the assertions that would have caught a wrong tag key.
     *
     * <p>The "all ores" mode is a chain of ten disjuncts, and each one is asked for separately -
     * a single sample is not enough there, because any nine of the ten could be deleted and a
     * test that only ever shows the mode one ore would stay green while the mode stopped finding
     * most of what its name promises.
     *
     * <p>What breaks this: a mode reading the wrong tag; the {@code ALL} chain losing any one of
     * its ten disjuncts; {@code NETHERITE} matching the netherite <em>block</em> or a tag instead
     * of ancient debris.
     */
    public static void detectorModesMatchTheirOreTags(GameTestHelper helper) {
        // A gold ore in the way must not answer for iron, and the deepslate variant must.
        placeDecoyAndTarget(helper, Blocks.GOLD_ORE, Blocks.DEEPSLATE_IRON_ORE);
        assertFinds(helper, detectorInMode(MODE_IRON), rowPos(3),
                "deepslate iron ore in iron mode, past a gold ore");

        placeDecoyAndTarget(helper, Blocks.IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        assertFinds(helper, detectorInMode(MODE_GOLD), rowPos(3),
                "deepslate gold ore in gold mode, past an iron ore");

        placeDecoyAndTarget(helper, Blocks.IRON_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        assertFinds(helper, detectorInMode(MODE_DIAMOND), rowPos(3),
                "deepslate diamond ore in diamond mode, past an iron ore");

        placeDecoyAndTarget(helper, Blocks.DIAMOND_ORE, Blocks.ANCIENT_DEBRIS);
        assertFinds(helper, detectorInMode(MODE_NETHERITE), rowPos(3),
                "ancient debris in netherite mode, past a diamond ore");

        // Nether quartz ore belongs to the "all ores" list and to no other mode.
        placeDecoyAndTarget(helper, Blocks.STONE, Blocks.NETHER_QUARTZ_ORE);
        assertFinds(helper, detectorInMode(MODE_ALL), rowPos(3), "nether quartz ore in all-ores mode");
        assertDoesNotFind(helper, detectorInMode(MODE_IRON), rowPos(3),
                "nether quartz ore while it was set to iron");

        // ...and so does every other member of the list, one at a time. Nether quartz ore above is
        // the only one no other mode can reach; for the nine below the ALL branch is the sole
        // reason they answer a detector in this mode, so dropping any single disjunct from
        // OreDetectorItem#isTarget makes exactly one of these lines fail.
        Block[] allOresMembers = {
                Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE,
                Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
                Blocks.ANCIENT_DEBRIS,
        };
        for (Block ore : allOresMembers) {
            placeDecoyAndTarget(helper, Blocks.STONE, ore);
            assertFinds(helper, detectorInMode(MODE_ALL), rowPos(3),
                    ore.getName().getString() + " in all-ores mode");
        }

        helper.succeed();
    }

    /**
     * How far the sonar carries depends on what stands in the way, in the four steps the density
     * table has. The same iron ore, at the same distance, is found through netherrack and lost
     * behind stone; one block closer it is found through stone and lost behind deepslate.
     *
     * <p>The numbers, for an iron budget of 24 and the unenchanted multiplier of 2:
     * <ul>
     *   <li>5 blocks out, four blocks of material between - air {@code 1+2*4*1.0 = 9},
     *       netherrack {@code 1+2*4*1.5 = 13}, stone {@code 1+2*4*3.0 = 25}.</li>
     *   <li>4 blocks out, three blocks between - stone {@code 1+2*3*3.0 = 19},
     *       deepslate {@code 1+2*3*6.0 = 37}.</li>
     * </ul>
     *
     * <p>What breaks this: flattening the density table (every solid block costing the same
     * would let stone through at 5 blocks or stop netherrack); dropping the deepslate branch,
     * which is what makes digging in the deep slate layer the expensive case the item is
     * balanced around; or losing the {@code accumulatedCost > budget} bail-out, which would
     * make the beam infinitely long.
     *
     * <p>Two more branches of the table are pinned below. Blackstone and basalt belong to no stone
     * tag and are named one by one in {@code getBlockDensity}; they are checked at the distance
     * that separates 6.0 from the 3.0 fallback, so deleting their branch makes them transparent
     * enough for the ore behind them to be found. And a block that is solid but does not occlude
     * costs as little as air - that case runs on the netherite budget of 10 rather than on iron's
     * 24, because a budget of 24 swallows four blocks of anything up to 2.875 per block, while 10
     * leaves room for four blocks of air (9) and for nothing denser (netherrack would be 13).
     *
     * <p>Not distinguished here: stone reached through {@code BlockTags.BASE_STONE_OVERWORLD} from
     * the untagged fallback of {@code getBlockDensity}. Both return 3.0, so no search can tell the
     * two apart - and deleting the tag branch would not change a thing in game either.
     */
    public static void denseBlocksShortenTheBeamMoreThanSoftOnes(GameTestHelper helper) {
        ItemStack iron = detectorInMode(MODE_IRON);

        placeWallAndOre(helper, 2, 5, Blocks.AIR, 6);
        assertFinds(helper, iron, rowPos(6), "iron ore 5 blocks out through open air (cost 9 of 24)");

        placeWallAndOre(helper, 2, 5, Blocks.NETHERRACK, 6);
        assertFinds(helper, iron, rowPos(6), "iron ore behind four netherrack (cost 13 of 24)");

        placeWallAndOre(helper, 2, 5, Blocks.STONE, 6);
        assertDoesNotFind(helper, iron, rowPos(6), "iron ore behind four stone (cost 25 of 24)");

        placeWallAndOre(helper, 2, 4, Blocks.STONE, 5);
        assertFinds(helper, iron, rowPos(5), "iron ore behind three stone (cost 19 of 24)");

        placeWallAndOre(helper, 2, 4, Blocks.DEEPSLATE, 5);
        assertDoesNotFind(helper, iron, rowPos(5), "iron ore behind three deepslate (cost 37 of 24)");

        // Blackstone and basalt have to cost 6.0 like deepslate, and they get there through their
        // own branch in getBlockDensity - no stone tag holds them. Lose that branch and they fall
        // to the 3.0 fallback, where the ore behind them costs 19 of 24 and is found: the stone
        // case above, at this very distance, is what shows that 19 is a hit.
        placeWallAndOre(helper, 2, 4, Blocks.BLACKSTONE, 5);
        assertDoesNotFind(helper, iron, rowPos(5), "iron ore behind three blackstone (cost 37 of 24)");

        placeWallAndOre(helper, 2, 4, Blocks.BASALT, 5);
        assertDoesNotFind(helper, iron, rowPos(5), "iron ore behind three basalt (cost 37 of 24)");

        // The cheapest branch, on the tightest budget: glass is solid but does not occlude, so four
        // of them cost the 9 of 10 that open air would. Anything above 1.125 per block is out of
        // reach here, which is what makes this the assertion that the !canOcclude() branch exists -
        // without it glass takes the 3.0 fallback and lands at 25.
        clearCorridor(helper);
        fillCorridor(helper, 2, 5, Blocks.GLASS);
        helper.setBlock(rowPos(6), Blocks.ANCIENT_DEBRIS);
        assertFinds(helper, detectorInMode(MODE_NETHERITE), rowPos(6),
                "ancient debris 5 blocks out behind four glass blocks (cost 9 of 10)");

        helper.succeed();
    }

    /**
     * Constructor's Touch halves what every step through a block costs, which is the difference
     * between stopping in front of four stone blocks and seeing through them: the same ore, at
     * the same distance, costs 25 points unenchanted and 12.5 enchanted against the same budget
     * of 24.
     *
     * <p>The enchantment's item list is asserted first. Nothing else in the mod puts
     * Constructor's Touch on the detector, so if the item leaves that list the cheaper
     * multiplier becomes unreachable in a real game while this test would happily keep passing
     * on a hand-enchanted stack.
     *
     * <p>What breaks this: the two multipliers collapsing into one; reading the enchantment off
     * the wrong stack or with a null level (the helper falls back to the raw component then, so
     * this would still pass - the item list assertion is the part that catches the item being
     * dropped from the enchantment); or inverting the branch so that the enchantment makes the
     * search more expensive.
     */
    public static void constructorsTouchDoublesTheReachThroughSolidRock(GameTestHelper helper) {
        Holder<Enchantment> touch = enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH);
        helper.assertTrue(supports(touch, ModItems.ORE_DETECTOR),
                "Constructor's Touch no longer lists the ore detector among its supported items, so no "
                        + "player could put it on one and the halved search cost is dead code");

        placeWallAndOre(helper, 2, 5, Blocks.STONE, 6);

        ItemStack plain = detectorInMode(MODE_IRON);
        assertDoesNotFind(helper, plain, rowPos(6),
                "iron ore behind four stone without the enchantment (cost 25 of 24)");

        ItemStack enchanted = detectorInMode(MODE_IRON);
        enchanted.enchant(touch, 1);
        assertFinds(helper, enchanted, rowPos(6),
                "iron ore behind four stone with Constructor's Touch (cost 12.5 of 24)");

        helper.succeed();
    }

    // =====================================================================================
    // THE TWO CLICKS
    // =====================================================================================

    /**
     * Sneak-clicking a block teaches the detector that block and switches it to the custom mode
     * in the same step; the search then hunts the block, not the blockstate it was taught from.
     * Clicking without sneaking must leave the item exactly as it was.
     *
     * <p>The custom search is proved on an oak log lying on its side after calibrating on an
     * upright one, with a block that is not a log standing closer to the eye. A comparison on
     * the full blockstate would find nothing; a comparison that ignored the stored block
     * altogether would report the closer stone.
     *
     * <p>What breaks this: {@code useOn} losing its sneak guard (a plain build click would start
     * recalibrating the detector); the calibration writing the target but leaving the mode
     * alone, which would store a block nobody ever searches for; {@code isTarget} comparing
     * {@code state} instead of {@code state.getBlock()}; and {@code use}/{@code useOn} returning
     * anything but a pass-through for a plain click, which would swallow the interaction and
     * stop the block underneath from being placed or used.
     */
    public static void sneakClickingCalibratesTheDetectorAndPlainClicksDoNot(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack detector = new ItemStack(ModItems.ORE_DETECTOR);
        setLogAxis(helper, SAMPLE_LOG, Direction.Axis.Y);

        // --- a plain click configures nothing and does not consume the interaction ---
        player.setShiftKeyDown(false);
        InteractionResult plainUseOn = useOn(helper, player, detector, SAMPLE_LOG);
        helper.assertTrue(plainUseOn == InteractionResult.PASS,
                "a plain right click on a block did not fall through to the base item, it returned " + plainUseOn);
        helper.assertTrue(customData(detector).isEmpty(),
                "a plain right click on a block wrote " + customData(detector) + " to the detector");

        InteractionResult plainUse = detector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(plainUse == InteractionResult.PASS,
                "a plain right click in the air did not pass, it returned " + plainUse);
        helper.assertTrue(customData(detector).isEmpty(),
                "a plain right click in the air wrote " + customData(detector) + " to the detector");
        helper.assertValueEqual(modeName(helper, detector), "Iron", "mode after two plain clicks");

        // --- the sneak click calibrates and switches to the custom mode ---
        player.setShiftKeyDown(true);
        InteractionResult calibrate = useOn(helper, player, detector, SAMPLE_LOG);
        helper.assertTrue(calibrate == InteractionResult.SUCCESS,
                "the calibration click did not consume the interaction, it returned " + calibrate);
        helper.assertTrue(customData(detector).contains("CustomBlock"),
                "the calibration click stored no target block, the item data is " + customData(detector));
        helper.assertValueEqual(modeName(helper, detector), "Custom",
                "the calibration click has to switch to the custom mode as well, or the stored block "
                        + "is never searched for");

        // --- the search now follows the block, whatever state it stands in ---
        clearCorridor(helper);
        helper.setBlock(rowPos(2), Blocks.STONE);
        setLogAxis(helper, rowPos(3), Direction.Axis.X);
        assertFinds(helper, detector, rowPos(3),
                "an oak log lying on its side after calibrating on an upright one");

        player.setShiftKeyDown(false);
        helper.succeed();
    }

    /**
     * Sneak-using walks the six modes in order and back to the first, it costs a creative player
     * nothing, and the detector stays the single, damageable item the mode data and the durability
     * both depend on.
     *
     * <p>This is the only test that goes through {@code cycleMode} at all - everywhere else the
     * {@code Mode} value is written into the item data directly - so it is the only place that can
     * say the six modes are reachable by clicking. It therefore names each of the six.
     *
     * <p>The creative half only says something because of the control at the end: the mock
     * player has {@code instabuild} cleared, so vanilla's own "creative tools take no damage"
     * rule ({@code ItemStack#processDurabilityChange} bails out on
     * {@code Player#hasInfiniteMaterials}) does <em>not</em> apply to it, as the direct
     * {@code hurtAndBreak} at the end demonstrates. The zero damage after six switches can
     * therefore only come from the item's own {@code !player.isCreative()} guard.
     *
     * <p>What breaks this: deleting that guard (every creative mode switch would then eat a
     * point of durability); making the detector stackable, which would put one damage bar and
     * one mode on a whole stack; dropping the durability, which turns the survival cost of a
     * mode switch into a no-op; and any cycle that skips a mode, wraps before the last one or
     * stops writing the mode altogether. The survival side of the guard is out of reach here -
     * see the "Not covered" note on the class.
     */
    public static void modeSwitchIsFreeInCreativeAndTheToolStaysUnstackable(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack detector = new ItemStack(ModItems.ORE_DETECTOR);

        helper.assertValueEqual(detector.getMaxStackSize(), 1,
                "the ore detector became stackable, so a whole stack would share one mode and one damage bar");
        helper.assertTrue(detector.isDamageableItem(),
                "the ore detector lost its durability, so a mode switch cannot cost anything any more");

        player.setItemInHand(InteractionHand.MAIN_HAND, detector);
        player.setShiftKeyDown(true);
        // Every click is checked against the mode it has to produce, not just the sixth against the
        // one it started from: "Iron again after six clicks" is equally true of a cycleMode that
        // stopped writing the mode at all, and of one that wraps after two or three modes - and in
        // those cases the names in between are unreachable in game. use() answers SUCCESS whatever
        // cycleMode did (OreDetectorItem#use), so its return value cannot stand in for the state.
        String[] cycle = {"Gold", "Diamond", "Netherite", "All Ores", "Custom", "Iron"};
        for (int click = 0; click < cycle.length; click++) {
            InteractionResult result = detector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.SUCCESS,
                    "sneak-using the detector did not consume the interaction on click " + click
                            + ", it returned " + result);
            helper.assertValueEqual(modeName(helper, detector), cycle[click],
                    "the mode after sneak-use number " + (click + 1));
        }
        helper.assertValueEqual(detector.getDamageValue(), 0,
                "a creative player was billed for switching modes");

        // The control: this player is creative but not instabuild, so vanilla would have let the
        // damage through. If this line ever fails, the assertion above proves nothing.
        helper.assertTrue(player.isCreative(), "the in-level mock player stopped reporting creative");
        helper.assertFalse(player.getAbilities().instabuild,
                "the mock player builds for free again, so vanilla - not the mod - would be refusing the damage");
        detector.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        helper.assertValueEqual(detector.getDamageValue(), 1,
                "vanilla refused to damage this player's tool, so the zero above says nothing about "
                        + "the mod's own creative guard");

        player.setShiftKeyDown(false);
        helper.succeed();
    }

    /**
     * The tooltip is the only place a player can read which mode is active, what it hunts and
     * how much search power it has, so it is asserted line by line for all six modes.
     *
     * <p>The power line is the number the search itself runs on - the same {@code mode.budget}.
     * Four of the six are measured against real distances in
     * {@link #detectorReportsTheNearestTargetInsideItsBudget} (iron 24, gold 18, diamond 16,
     * netherite 10), so for those this line is a consistency check between what the item does and
     * what it tells the player. For "all ores" and "custom" it is the only assertion there is:
     * both share iron's 24, and no test drives their search to the edge of it.
     *
     * <p>The last two lines cover an index that is not a mode at all: a stack that carries
     * {@code Mode=99} (a hand-edited item, or an old save after a mode was removed) has to clamp
     * to the last mode and a negative one to the first, instead of throwing an
     * {@code ArrayIndexOutOfBoundsException} out of the tooltip and the search.
     *
     * <p>What breaks this: reordering or renaming the modes without moving the indices; changing
     * a budget; the custom mode no longer telling a player how to set a target, or naming the
     * wrong block; and losing the clamp in {@code getMode}, which turns a strange item into a
     * crash every time it is hovered.
     */
    public static void tooltipNamesEveryModeWithItsPowerAndTarget(GameTestHelper helper) {
        String[] names = {"Iron", "Gold", "Diamond", "Netherite", "All Ores", "Custom"};
        int[] powers = {24, 18, 16, 10, 24, 24};

        for (int mode = 0; mode < names.length; mode++) {
            List<String> lines = tooltip(helper, detectorInMode(mode));
            helper.assertValueEqual(lines.getFirst(), MODE_PREFIX + names[mode],
                    "the first tooltip line of mode index " + mode);
            helper.assertTrue(lines.contains("Power: " + powers[mode]),
                    "mode " + names[mode] + " does not advertise a search power of " + powers[mode]
                            + ", its tooltip is " + lines);
        }

        // Every mode but the custom one points at the mode cycle and has no target line at all.
        List<String> ironLines = tooltip(helper, detectorInMode(MODE_IRON));
        helper.assertTrue(ironLines.contains("Sneak + Use to cycle modes"),
                "the iron mode tooltip no longer says how to change modes: " + ironLines);
        helper.assertTrue(ironLines.stream().noneMatch(line -> line.startsWith("Target: ")),
                "a non-custom mode claims to have a target block: " + ironLines);

        // Custom without a calibration has to say so rather than show an empty target.
        List<String> uncalibrated = tooltip(helper, detectorInMode(MODE_CUSTOM));
        helper.assertTrue(uncalibrated.contains("Target: None (Sneak-Use on block)"),
                "an uncalibrated custom detector does not tell the player how to set a target: " + uncalibrated);

        // ...and with one, it names the block that was clicked.
        ServerPlayer player = mockPlayer(helper);
        setLogAxis(helper, SAMPLE_LOG, Direction.Axis.Y);
        ItemStack calibrated = new ItemStack(ModItems.ORE_DETECTOR);
        player.setShiftKeyDown(true);
        useOn(helper, player, calibrated, SAMPLE_LOG);
        player.setShiftKeyDown(false);
        String expectedTarget = "Target: " + Blocks.OAK_LOG.getName().getString();
        helper.assertTrue(tooltip(helper, calibrated).contains(expectedTarget),
                "a detector calibrated on an oak log does not name it, its tooltip is "
                        + tooltip(helper, calibrated));

        // An index outside the enum is clamped into it instead of throwing.
        helper.assertValueEqual(tooltip(helper, detectorInMode(99)).getFirst(), MODE_PREFIX + "Custom",
                "a Mode of 99 has to clamp to the last mode");
        helper.assertValueEqual(tooltip(helper, detectorInMode(-4)).getFirst(), MODE_PREFIX + "Iron",
                "a negative Mode has to clamp to the first mode");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** A position on the sight line, {@code x - 1} blocks out from the eye. */
    private static BlockPos rowPos(int x) {
        return new BlockPos(x, ROW_Y, ROW_Z);
    }

    /** The block the detector would ping for this stack, in absolute coordinates, or null. */
    private static BlockPos scan(GameTestHelper helper, ItemStack stack) {
        OreDetectorItem item = (OreDetectorItem) ModItems.ORE_DETECTOR;
        return item.findTarget(helper.getLevel(), stack, helper.absoluteVec(EYE));
    }

    private static void assertFinds(GameTestHelper helper, ItemStack stack, BlockPos relative, String what) {
        BlockPos expected = helper.absolutePos(relative);
        BlockPos found = scan(helper, stack);
        helper.assertTrue(expected.equals(found),
                "the detector was supposed to report " + what + " at " + relative
                        + " but reported " + describe(helper, found));
    }

    /**
     * The detector must not report this exact position. Stated as "not this block" rather than
     * "nothing at all" on purpose: the scan radius reaches out of our own room, so a matching
     * block in a neighbouring test structure could legitimately be the answer without saying
     * anything about the case under test.
     */
    private static void assertDoesNotFind(GameTestHelper helper, ItemStack stack, BlockPos relative, String what) {
        BlockPos forbidden = helper.absolutePos(relative);
        BlockPos found = scan(helper, stack);
        helper.assertTrue(!forbidden.equals(found),
                "the detector reported " + what + ", which is beyond what it can pay for");
    }

    private static String describe(GameTestHelper helper, BlockPos absolute) {
        return absolute == null ? "nothing at all" : (helper.relativePos(absolute) + " (relative)");
    }

    /** Clears the 3x3 corridor around the sight line so nothing survives from a previous step. */
    private static void clearCorridor(GameTestHelper helper) {
        fillCorridor(helper, 2, 7, Blocks.AIR);
    }

    /** Fills the 3x3 cross section around the sight line, for every column from fromX to toX. */
    private static void fillCorridor(GameTestHelper helper, int fromX, int toX, Block block) {
        for (int x = fromX; x <= toX; x++) {
            for (int y = ROW_Y - 1; y <= ROW_Y + 1; y++) {
                for (int z = ROW_Z - 1; z <= ROW_Z + 1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    /** An empty corridor with a block to be ignored 1 block out and the real target 2 blocks out. */
    private static void placeDecoyAndTarget(GameTestHelper helper, Block decoy, Block target) {
        clearCorridor(helper);
        helper.setBlock(rowPos(2), decoy);
        helper.setBlock(rowPos(3), target);
    }

    /** An empty corridor walled with {@code material} from {@code fromX} to {@code toX}, iron ore behind it. */
    private static void placeWallAndOre(GameTestHelper helper, int fromX, int toX, Block material, int oreX) {
        clearCorridor(helper);
        fillCorridor(helper, fromX, toX, material);
        helper.setBlock(rowPos(oreX), Blocks.IRON_ORE);
    }

    private static ItemStack detectorInMode(int modeIndex) {
        ItemStack stack = new ItemStack(ModItems.ORE_DETECTOR);
        CompoundTag nbt = customData(stack);
        nbt.putInt("Mode", modeIndex);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return stack;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** The mode the item itself believes it is in, read back off its tooltip. */
    private static String modeName(GameTestHelper helper, ItemStack stack) {
        String line = tooltip(helper, stack).getFirst();
        return line.startsWith(MODE_PREFIX) ? line.substring(MODE_PREFIX.length()) : line;
    }

    /** The tooltip the item appends for this stack, as plain text without the colours. */
    @SuppressWarnings("deprecation")
    private static List<String> tooltip(GameTestHelper helper, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()),
                TooltipDisplay.DEFAULT, component -> lines.add(component.getString()), TooltipFlag.NORMAL);
        return lines;
    }

    private static void setLogAxis(GameTestHelper helper, BlockPos pos, Direction.Axis axis) {
        helper.setBlock(pos, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, axis));
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static boolean supports(Holder<Enchantment> enchantment, Item item) {
        for (Holder<Item> supported : enchantment.value().getSupportedItems()) {
            if (supported.value() == item) {
                return true;
            }
        }
        return false;
    }

    /**
     * A mock player that is in the level - the calibration click and the mode switch both send
     * the player an overlay message, which needs a connection. Its {@code instabuild} flag is
     * cleared so that vanilla stops shielding its tools from damage; see
     * {@link #modeSwitchIsFreeInCreativeAndTheToolStaysUnstackable}.
     */
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 6.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        // A leaked mock player keeps the player list non-empty and stalls the server shutdown.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Right clicks the top face of a block, server side. */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hit, Direction.UP, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }
}
