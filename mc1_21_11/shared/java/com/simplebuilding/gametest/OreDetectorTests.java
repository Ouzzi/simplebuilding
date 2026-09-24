package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OreDetectorItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
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
 * <h2>How the signal is charged</h2>
 *
 * <p>An ore is found when it lies within the range of its class and its signal survives the way
 * there: every block the sight line crosses between the eye block and the ore takes off the loss
 * of its material step ({@code OreDetectorItem.Material}: air 0.125, soft 2, stone 4, dense 6,
 * very dense 16), the eye block and the ore themselves are free. The short cases run along +X
 * from the centre of the block at relative {@code (1,3,3)}, so the ray runs exactly through the
 * centres of the blocks in that row and the loss is simply the sum over the blocks between; the
 * numbers quoted in each test come from that sum. Sight lines longer than the 8-block room run
 * in a {@link LineWorld} of their own.
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
 *   <li><b>That the search returns a copy of its cursor and not the cursor itself.</b>
 *       {@code findTarget} walks its cube with one {@code MutableBlockPos} and writes
 *       {@code bestTarget = checkPos.immutable()}. Dropping that {@code immutable()} is
 *       unobservable from here, and not because the tests are weak: the cursor is a local
 *       of that method, so nothing outside it ever moves the object again, and inside the
 *       loop only a <em>strictly closer</em> candidate writes the cursor - after the last
 *       winner nothing does. An aliased answer would therefore read correctly on every
 *       path a test can reach. The copy stays because it is what makes the method safe to
 *       change (hoist the cursor out of the method, keep scanning after the winner, and
 *       the alias turns into a wrong answer), not because a test holds it.</li>
 *   <li><b>That the tick still runs its search through {@code findTarget}.</b> Every search test
 *       below calls that method itself; nothing here forces {@code inventoryTick} to keep going
 *       through it. Whoever inlines the search back into the tick, or adds a second search path
 *       beside it, leaves this whole file green while the item in a player's hand pings something
 *       else. The seam is untestable from the tick side for the same reason it exists: all the
 *       tick does with the result is sounds and particles, and those are discarded.</li>
 *   <li><b>The two guards in front of the search</b> - that it only runs for a stack in the main
 *       or off hand, and only on every 20th game tick counted from a phase of the player's own
 *       entity id, so that the scans of several players do not all land in one tick. Both guards
 *       decide whether sounds and particles are emitted, and nothing else; there is no server
 *       side state that records whether a tick scanned. Driving {@code inventoryTick} from a test
 *       therefore cannot tell the two outcomes apart, and it cannot tell the phase either: what a
 *       wrong phase would cost is one ping up to 19 ticks early or late, which is a sound.</li>
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
     * Of everything a mode matches, the detector reports the closest block its signal still
     * reaches - and the signal that decides "still reaches" is the one of the ore that is found,
     * not one shared number. This is the one search test that runs on the real level, through the
     * same {@code findTarget(ServerLevel, ...)} the tick calls; the reach tables further down use
     * a block source of their own (see {@link LineWorld}).
     *
     * <p>Every class is cut on the same kind of wall from both sides: debris (signal 5) is found
     * behind two netherrack (4.125) and lost behind three (6), where iron (18) is found; gold (13)
     * behind three stone (12) and not four (16), where iron again is found; diamond (9) behind two
     * stone and two air (8.25) and not three stone (12.125), where gold is found.
     *
     * <p>What breaks this: returning the first hit of the {@code x/y/z} loop instead of the
     * nearest one (the loop runs from -radius, so a farther block would win); dropping the
     * {@code distanceSq >= bestDistanceSq} guard; giving every class the same signal; and moving
     * the {@code bestDistanceSq} update in front of the reach check, which would let the nearest
     * match the signal cannot reach hide every reachable one behind it.
     */
    public static void detectorReportsTheNearestTargetInsideItsBudget(GameTestHelper helper) {
        ItemStack iron = detectorInMode(MODE_IRON);

        // --- two iron ores on the same line, 2 and 5 blocks out ---
        clearCorridor(helper);
        helper.setBlock(rowPos(3), Blocks.IRON_ORE);
        helper.setBlock(rowPos(6), Blocks.IRON_ORE);
        assertFinds(helper, iron, rowPos(3), "the nearer of two iron ores");

        // Taking the near one away must reveal the far one: it was reachable all along, it just
        // lost on distance.
        helper.setBlock(rowPos(3), Blocks.AIR);
        assertFinds(helper, iron, rowPos(6), "the remaining iron ore 5 blocks out");

        // A nearer match the signal cannot reach must not hide a farther one it can. The ore three
        // blocks straight up sits behind two obsidian (loss 32 of 18), the one five blocks out along
        // the row behind four air (0.5) - and the walled-in one is the nearer, so it is visited
        // first.
        BlockPos walledInOre = EYE_BLOCK.above(3);
        helper.setBlock(EYE_BLOCK.above(), Blocks.OBSIDIAN);
        helper.setBlock(EYE_BLOCK.above(2), Blocks.OBSIDIAN);
        helper.setBlock(walledInOre, Blocks.IRON_ORE);
        assertFinds(helper, iron, rowPos(6),
                "the iron ore it can reach, past a nearer one behind obsidian");

        helper.setBlock(walledInOre, Blocks.AIR);
        helper.setBlock(EYE_BLOCK.above(2), Blocks.AIR);
        helper.setBlock(EYE_BLOCK.above(), Blocks.AIR);

        // --- debris: two netherrack and one air cost 4.125 of 5, three netherrack 6 ---
        ItemStack netherite = detectorInMode(MODE_NETHERITE);
        clearCorridor(helper);
        fillCorridor(helper, 2, 3, Blocks.NETHERRACK);
        helper.setBlock(rowPos(5), Blocks.ANCIENT_DEBRIS);
        assertFinds(helper, netherite, rowPos(5),
                "ancient debris behind two netherrack and one air (loss 4.125 of 5)");

        fillCorridor(helper, 4, 4, Blocks.NETHERRACK);
        assertDoesNotFind(helper, netherite, rowPos(5),
                "ancient debris behind three netherrack (loss 6 of 5)");

        helper.setBlock(rowPos(5), Blocks.IRON_ORE);
        assertFinds(helper, iron, rowPos(5),
                "iron ore behind the three netherrack the debris could not get through (loss 6 of 18)");

        // --- gold: three stone cost 12 of 13, four stone 16 ---
        clearCorridor(helper);
        fillCorridor(helper, 2, 4, Blocks.STONE);
        helper.setBlock(rowPos(5), Blocks.GOLD_ORE);
        assertFinds(helper, detectorInMode(MODE_GOLD), rowPos(5),
                "gold ore behind three stone (loss 12 of 13)");

        clearCorridor(helper);
        fillCorridor(helper, 2, 5, Blocks.STONE);
        helper.setBlock(rowPos(6), Blocks.GOLD_ORE);
        assertDoesNotFind(helper, detectorInMode(MODE_GOLD), rowPos(6),
                "gold ore behind four stone (loss 16 of 13)");

        helper.setBlock(rowPos(6), Blocks.IRON_ORE);
        assertFinds(helper, iron, rowPos(6), "iron ore behind four stone (loss 16 of 18)");

        // --- diamond: two stone and two air cost 8.25 of 9, three stone and one air 12.125 ---
        clearCorridor(helper);
        fillCorridor(helper, 2, 3, Blocks.STONE);
        helper.setBlock(rowPos(6), Blocks.DIAMOND_ORE);
        assertFinds(helper, detectorInMode(MODE_DIAMOND), rowPos(6),
                "diamond ore behind two stone and two air (loss 8.25 of 9)");

        fillCorridor(helper, 4, 4, Blocks.STONE);
        assertDoesNotFind(helper, detectorInMode(MODE_DIAMOND), rowPos(6),
                "diamond ore behind three stone and one air (loss 12.125 of 9)");

        helper.setBlock(rowPos(6), Blocks.GOLD_ORE);
        assertFinds(helper, detectorInMode(MODE_GOLD), rowPos(6),
                "gold ore behind the wall the diamond could not get through (loss 12.125 of 13)");

        TestCleanup.succeed(helper);
    }

    /**
     * Each mode hunts the blocks its own tag names - and only those. Every case puts a block
     * that must be ignored nearer than the block that must be found, so a mode that matched too
     * much would report the decoy instead and fail on the position.
     *
     * <p>The deepslate variants are the point of the iron, gold and diamond cases: they share
     * no block with their stone cousins and are only reachable through the tag. This is the
     * block the 26.2 port moved from {@code BlockTags} to {@code BlockItemTags.X.block()}, so
     * these are the assertions that would have caught a wrong tag key.
     *
     * <p>The decoys stand one step in front of the target on the sight line, so the target is seen
     * through them. The netherite case is the exception: a diamond ore is dense (loss 6) and would
     * swallow the whole debris signal of 5, so its decoy stands one block above the eye instead -
     * still nearer than the debris two blocks out.
     *
     * <p>The "all ores" mode is a chain of twelve disjuncts, and each one is asked for separately -
     * a single sample is not enough there, because any eleven of the twelve could be deleted and a
     * test that only ever shows the mode one ore would stay green while the mode stopped finding
     * most of what its name promises. The last two are the mod's own End ores.
     *
     * <p>What breaks this: a mode reading the wrong tag; the {@code ALL} chain losing any one of
     * its twelve disjuncts; {@code NETHERITE} matching the netherite <em>block</em> or a tag instead
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

        placeDecoyAndTarget(helper, Blocks.AIR, Blocks.ANCIENT_DEBRIS);
        helper.setBlock(EYE_BLOCK.above(), Blocks.DIAMOND_ORE);
        assertFinds(helper, detectorInMode(MODE_NETHERITE), rowPos(3),
                "ancient debris in netherite mode, past a nearer diamond ore");
        helper.setBlock(EYE_BLOCK.above(), Blocks.AIR);

        // Nether quartz ore belongs to the "all ores" list and to no other mode.
        placeDecoyAndTarget(helper, Blocks.STONE, Blocks.NETHER_QUARTZ_ORE);
        assertFinds(helper, detectorInMode(MODE_ALL), rowPos(3), "nether quartz ore in all-ores mode");
        assertDoesNotFind(helper, detectorInMode(MODE_IRON), rowPos(3),
                "nether quartz ore while it was set to iron");

        // ...and so does every other member of the list, one at a time. Nether quartz ore above is
        // the only one no other mode can reach; for the eleven below the ALL branch is the sole
        // reason they answer a detector in this mode, so dropping any single disjunct from
        // OreDetectorItem#isTarget makes exactly one of these lines fail. The stone in front costs
        // 4, which even the debris signal of 5 pays.
        Block[] allOresMembers = {
                Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE,
                Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
                Blocks.ANCIENT_DEBRIS, ModBlocks.ASTRALIT_ORE, ModBlocks.NIHILITH_ORE,
        };
        for (Block ore : allOresMembers) {
            placeDecoyAndTarget(helper, Blocks.STONE, ore);
            assertFinds(helper, detectorInMode(MODE_ALL), rowPos(3),
                    ore.getName().getString() + " in all-ores mode");
        }

        TestCleanup.succeed(helper);
    }

    /**
     * What a block costs the signal depends on how hard it is to mine, in the five steps of
     * {@code OreDetectorItem.Material}: air and everything that does not occlude 0.125, soft
     * blocks under hardness 1.0 (netherrack, dirt, sand, gravel) and end stone 2, hardness 1.0 to
     * under 3.0 (stone, cobblestone, tuff, basalt, blackstone, wood) 4, hardness 3.0 to under 10
     * (deepslate, the ores) 6, hardness 10 and up or unbreakable (obsidian, ancient debris,
     * bedrock) 16.
     *
     * <p>Each step is asserted on the block that marks it, with the boundary blocks on both sides:
     * calcite (0.75) and nether wart block (1.0) around the first threshold, cobblestone (2.0) and
     * deepslate (3.0) around the second, the iron block (5.0) and obsidian (50) around the third.
     * Leaves, glass and water stand for the blocks that do not occlude. End stone is the
     * one block that is named and not measured: at hardness 3.0 it would be dense, and it is soft
     * because it is the host rock of the End ores as netherrack is of ancient debris.
     *
     * <p>Then {@code pathLoss} on the real level: a sight line through one block of every solid
     * step and one air costs exactly {@code 2 + 4 + 6 + 16 + 0.125 = 28.125}; the eye block and the
     * target are not charged (a stone eye block and an obsidian target leave the sum alone), and
     * Constructor's Touch halves it to 14.0625.
     *
     * <p>What breaks this: a changed step value or threshold, the {@code !canOcclude()} branch
     * gone (glass and water would cost as much as their hardness says), end stone losing its
     * exception, unbreakable blocks (hardness -1) falling into the cheapest step, and the ray
     * charging the blocks at its ends.
     */
    public static void denseBlocksShortenTheBeamMoreThanSoftOnes(GameTestHelper helper) {
        Assertions.valueEqual(helper, lossTable(), "AIR 0.125, SOFT 2.0, STONE 4.0, DENSE 6.0, VERY_DENSE 16.0",
                "the signal loss of each material step");

        Block[] samples = {
                Blocks.AIR, Blocks.GLASS, Blocks.WATER, Blocks.OAK_LEAVES,
                Blocks.NETHERRACK, Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.CALCITE, Blocks.END_STONE,
                Blocks.NETHER_WART_BLOCK, Blocks.STONE, Blocks.COBBLESTONE, Blocks.TUFF, Blocks.BASALT,
                Blocks.BLACKSTONE, Blocks.OAK_LOG,
                Blocks.DEEPSLATE, Blocks.IRON_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.IRON_BLOCK,
                Blocks.OBSIDIAN, Blocks.ANCIENT_DEBRIS, Blocks.BEDROCK,
        };
        BlockPos probe = new BlockPos(6, 1, 6);
        StringBuilder actual = new StringBuilder();
        for (Block block : samples) {
            helper.setBlock(probe, block);
            BlockPos absolute = helper.absolutePos(probe);
            actual.append(block.getName().getString()).append('=')
                    .append(OreDetectorItem.material(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute)))
                    .append(' ');
        }
        helper.setBlock(probe, Blocks.AIR);
        String expected = names(samples, 0, 4, "AIR") + names(samples, 4, 10, "SOFT")
                + names(samples, 10, 17, "STONE") + names(samples, 17, 21, "DENSE")
                + names(samples, 21, 24, "VERY_DENSE");
        Assertions.valueEqual(helper, actual.toString(), expected, "the material step of each sample block");

        // One block of every solid step and one air, eye block and target not charged.
        clearCorridor(helper);
        helper.setBlock(EYE_BLOCK, Blocks.STONE);
        helper.setBlock(rowPos(2), Blocks.NETHERRACK);
        helper.setBlock(rowPos(3), Blocks.STONE);
        helper.setBlock(rowPos(4), Blocks.DEEPSLATE);
        helper.setBlock(rowPos(5), Blocks.OBSIDIAN);
        helper.setBlock(rowPos(7), Blocks.OBSIDIAN);
        Vec3 eye = helper.absoluteVec(EYE);
        BlockPos target = helper.absolutePos(rowPos(7));
        String losses = OreDetectorItem.pathLoss(helper.getLevel(), eye, target, 1.0, 1000)
                + " / " + OreDetectorItem.pathLoss(helper.getLevel(), eye, target, OreDetectorItem.TOUCH_LOSS_FACTOR, 1000);
        helper.setBlock(EYE_BLOCK, Blocks.AIR);
        clearCorridor(helper);
        Assertions.valueEqual(helper, losses, "28.125 / 14.0625",
                "the loss through netherrack, stone, deepslate, obsidian and air, plain / with Constructor's Touch");

        TestCleanup.succeed(helper);
    }

    /**
     * Constructor's Touch halves the loss of every block, so the signal carries twice as far
     * through rock; it does not stretch the range. Measured as the wall an iron ore can still be
     * seen through ({@link #penetration}): 4 stone and 2 deepslate plain, 8 stone and 5 deepslate
     * enchanted. Together with the eight air blocks behind each wall that pins the enchanted factor
     * between 0.486 and 0.531 - a "halving" that were really a 10 % discount would stop at 4 or 5
     * stone.
     *
     * <p>The enchantment's item list is asserted first. Nothing else in the mod puts
     * Constructor's Touch on the detector, so if the item leaves that list the cheaper factor
     * becomes unreachable in a real game while this test would happily keep passing on a
     * hand-enchanted stack.
     *
     * <p>What breaks this: the factor gone or drifting towards 1; reading the enchantment off the
     * wrong stack; or inverting the branch so that the enchantment makes the search more
     * expensive.
     */
    public static void constructorsTouchDoublesTheReachThroughSolidRock(GameTestHelper helper) {
        Holder<Enchantment> touch = enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH);
        helper.assertTrue(supports(touch, ModItems.ORE_DETECTOR),
                "Constructor's Touch no longer lists the ore detector among its supported items, so no "
                        + "player could put it on one and the halved loss is dead code");

        ItemStack plain = detectorInMode(MODE_IRON);
        ItemStack enchanted = detectorInMode(MODE_IRON);
        enchanted.enchant(touch, 1);

        String actual = "plain: stone " + penetration(helper, plain, Blocks.IRON_ORE, Blocks.STONE)
                + ", deepslate " + penetration(helper, plain, Blocks.IRON_ORE, Blocks.DEEPSLATE)
                + " | Constructor's Touch: stone " + penetration(helper, enchanted, Blocks.IRON_ORE, Blocks.STONE)
                + ", deepslate " + penetration(helper, enchanted, Blocks.IRON_ORE, Blocks.DEEPSLATE);
        Assertions.valueEqual(helper, actual,
                "plain: stone 4, deepslate 2 | Constructor's Touch: stone 8, deepslate 5",
                "how many blocks of rock an iron ore is seen through");

        TestCleanup.succeed(helper);
    }

    /**
     * How much rock the signal of an ore gets through depends on how rare that ore is, and the
     * Radius enchantment roughly doubles it for the rare classes. The documented table
     * ({@code OreDetectorItem.OreClass}): signal 18 for the common ores (coal, copper, iron,
     * redstone, lapis, quartz), 13 for gold, 9 for diamond and emerald, 5 for ancient debris and
     * the two End ores; with Radius 22, 17, 17 and 9.
     *
     * <p>Measured in the "all ores" mode, where one detector sees every class at once, as the
     * thickest wall of stone, of netherrack (end stone for the End ores, whose host rock it is)
     * and of deepslate an ore is still found behind, with eight blocks of air between wall and
     * ore ({@link #penetration}). The wall of {@code k} blocks costs {@code k * loss + 1}, so
     * with Radius debris goes from 2 to 4 netherrack and diamond from 2 to 4 stone, while the
     * common ores only go from 4 to 5 stone.
     *
     * <p>A calibrated detector takes the class of its target: set to diamond ore it gets through
     * exactly what the all-ores mode does for diamond. Radius is also asserted to accept the ore
     * detector at all - otherwise the stretched signals would be dead code in a real game.
     *
     * <p>What breaks this: one signal for every class, a changed class signal or Radius bonus, an
     * ore sorted into the wrong class, Radius read off the wrong enchantment or leaving the
     * detector's item list.
     */
    public static void allOresReachFollowsTheOreRarityAndRadiusStretchesTheRareOnes(GameTestHelper helper) {
        Holder<Enchantment> radius = enchantment(helper, ModEnchantments.RADIUS);
        helper.assertTrue(supports(radius, ModItems.ORE_DETECTOR),
                "Radius no longer lists the ore detector among its supported items, so the stretched "
                        + "signals cannot be reached in a real game");

        ItemStack plain = detectorInMode(MODE_ALL);
        ItemStack stretched = detectorInMode(MODE_ALL);
        stretched.enchant(radius, 1);

        Block[] ores = {
                Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.ANCIENT_DEBRIS, ModBlocks.ASTRALIT_ORE, ModBlocks.NIHILITH_ORE,
        };
        StringBuilder actual = new StringBuilder();
        for (Block ore : ores) {
            Block soft = ore == ModBlocks.ASTRALIT_ORE || ore == ModBlocks.NIHILITH_ORE ? Blocks.END_STONE : Blocks.NETHERRACK;
            actual.append(ore.getName().getString()).append(": ")
                    .append(walls(helper, plain, ore, soft)).append(" | with Radius: ")
                    .append(walls(helper, stretched, ore, soft)).append("; ");
        }
        String common = "4 stone, 8 soft, 2 deepslate | with Radius: 5 stone, 10 soft, 3 deepslate; ";
        String gold = "3 stone, 6 soft, 2 deepslate | with Radius: 4 stone, 8 soft, 2 deepslate; ";
        String rare = "2 stone, 4 soft, 1 deepslate | with Radius: 4 stone, 8 soft, 2 deepslate; ";
        String veryRare = "1 stone, 2 soft, 0 deepslate | with Radius: 2 stone, 4 soft, 1 deepslate; ";
        String[] rows = {common, common, common, gold, rare, rare, veryRare, veryRare, veryRare};
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < ores.length; i++) {
            expected.append(ores[i].getName().getString()).append(": ").append(rows[i]);
        }
        Assertions.valueEqual(helper, actual.toString(), expected.toString(),
                "the walls each ore is seen through in the all-ores mode");

        // A calibrated detector searches with its target's class.
        ItemStack onDiamond = calibratedOn(Blocks.DIAMOND_ORE);
        Assertions.valueEqual(helper, walls(helper, onDiamond, Blocks.DIAMOND_ORE, Blocks.NETHERRACK),
                "2 stone, 4 soft, 1 deepslate", "the walls a detector calibrated on diamond ore sees it through");

        TestCleanup.succeed(helper);
    }

    /**
     * Through open air the signal fades only a little (0.125 per block), and the range of each
     * class ends the search: common ores are found up to 24 blocks away, gold 20, diamond,
     * emerald, debris and the End ores 16; with Radius 28, 24, 24 and 20. Measured on a straight
     * line of air in a {@link LineWorld} - the gametest rooms are 8 blocks wide - with the ore
     * exactly at the range and one block beyond it.
     *
     * <p>That air is not free is pinned where it matters most, on debris behind two netherrack
     * (loss 4 of 5): with eight air blocks behind the wall it is still found (5.0), with nine it
     * is not (5.125). That brackets the air loss between 0.111 and 0.125.
     *
     * <p>What breaks this: a scan radius smaller than the range (the far ore is never visited), a
     * range check dropped or taken from the wrong class, Radius not stretching the range, air
     * losing its cost or costing like rock.
     */
    public static void openAirReachEndsAtTheRangeOfEachOreClass(GameTestHelper helper) {
        ItemStack plain = detectorInMode(MODE_ALL);
        ItemStack stretched = detectorInMode(MODE_ALL);
        stretched.enchant(enchantment(helper, ModEnchantments.RADIUS), 1);

        Block[] ores = {Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE, Blocks.ANCIENT_DEBRIS, ModBlocks.NIHILITH_ORE};
        StringBuilder actual = new StringBuilder();
        for (Block ore : ores) {
            actual.append(ore.getName().getString()).append(' ')
                    .append(airReach(helper, plain, ore)).append('/').append(airReach(helper, stretched, ore)).append(", ");
        }
        String expected = Blocks.IRON_ORE.getName().getString() + " 24/28, "
                + Blocks.GOLD_ORE.getName().getString() + " 20/24, "
                + Blocks.DIAMOND_ORE.getName().getString() + " 16/24, "
                + Blocks.ANCIENT_DEBRIS.getName().getString() + " 16/20, "
                + ModBlocks.NIHILITH_ORE.getName().getString() + " 16/20, ";
        Assertions.valueEqual(helper, actual.toString(), expected,
                "the farthest block through open air each ore is found at, plain/with Radius");

        ItemStack netherite = detectorInMode(MODE_NETHERITE);
        String fade = "8 air " + findsBehindWall(helper, netherite, Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK, 2, 8)
                + ", 9 air " + findsBehindWall(helper, netherite, Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK, 2, 9);
        Assertions.valueEqual(helper, fade, "8 air found, 9 air lost",
                "ancient debris behind two netherrack and then air");

        TestCleanup.succeed(helper);
    }

    /**
     * A calibrated detector marks itself in the inventory with a small glimmer in the colour of
     * its target block - {@code OreDetectorItem#targetColor}, which the client decoration draws
     * ({@code OreDetectorGlint}, asserted on screen by the client tests). The colour is decided
     * server side from the item alone, so it is pinned here: ores glow in the colour of their
     * mineral, any other block in its map colour, and a detector that has no block selected -
     * every fixed mode, and the custom mode before its first calibration - shows nothing.
     *
     * <p>What breaks this: the glimmer showing on detectors that have not selected a block, the
     * target read from the wrong place, or an ore losing its mineral colour.
     */
    public static void calibratedDetectorGlimmersInTheColourOfItsTarget(GameTestHelper helper) {
        String actual = "diamond " + hex(OreDetectorItem.targetColor(calibratedOn(Blocks.DEEPSLATE_DIAMOND_ORE)))
                + ", astralit " + hex(OreDetectorItem.targetColor(calibratedOn(ModBlocks.ASTRALIT_ORE)))
                + ", ancient debris " + hex(OreDetectorItem.targetColor(calibratedOn(Blocks.ANCIENT_DEBRIS)))
                + ", oak log " + hex(OreDetectorItem.targetColor(calibratedOn(Blocks.OAK_LOG)))
                + ", uncalibrated " + hex(OreDetectorItem.targetColor(detectorInMode(MODE_CUSTOM)))
                + ", iron mode " + hex(OreDetectorItem.targetColor(detectorInMode(MODE_IRON)))
                + ", all ores " + hex(OreDetectorItem.targetColor(detectorInMode(MODE_ALL)));
        String expected = "diamond 5decf5, astralit e49dd6, ancient debris 9a6a58, oak log "
                + hex(Blocks.OAK_LOG.defaultMapColor().col)
                + ", uncalibrated none, iron mode none, all ores none";
        Assertions.valueEqual(helper, actual, expected, "the glimmer colour of each detector");

        // A detector in a fixed mode keeps no glimmer even if it still carries an old target.
        ItemStack switchedAway = calibratedOn(Blocks.DIAMOND_ORE);
        CompoundTag nbt = customData(switchedAway);
        nbt.putInt("Mode", MODE_GOLD);
        switchedAway.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        Assertions.valueEqual(helper, hex(OreDetectorItem.targetColor(switchedAway)), "none",
                "the glimmer of a detector switched from custom to gold that still stores a target");
        TestCleanup.succeed(helper);
    }

    /** Height of the straight sight line in a {@link LineWorld}; the eye sits in block (0, 64, 0). */
    private static final int LINE_Y = 64;
    private static final Vec3 LINE_EYE = new Vec3(0.5, LINE_Y + 0.5, 0.5);

    /** Air between the wall and the ore in {@link #penetration}. */
    private static final int AIR_BEHIND_WALL = 8;

    /**
     * The thickest wall of {@code wall} blocks that {@code stack} still finds {@code ore} behind,
     * with {@link #AIR_BEHIND_WALL} blocks of air between wall and ore; -1 if not even the bare
     * air line is found. Counted upwards and stopped at the first miss.
     */
    private static int penetration(GameTestHelper helper, ItemStack stack, Block ore, Block wall) {
        int thickest = -1;
        for (int k = 0; k <= 12; k++) {
            if (!findsBehind(helper, stack, ore, wall, k, AIR_BEHIND_WALL)) break;
            thickest = k;
        }
        return thickest;
    }

    /** {@link #penetration} for stone, for {@code soft} and for deepslate, e.g. {@code "4 stone, 8 soft, 2 deepslate"}. */
    private static String walls(GameTestHelper helper, ItemStack stack, Block ore, Block soft) {
        return penetration(helper, stack, ore, Blocks.STONE) + " stone, "
                + penetration(helper, stack, ore, soft) + " soft, "
                + penetration(helper, stack, ore, Blocks.DEEPSLATE) + " deepslate";
    }

    /** "found" or "lost": {@code ore} behind {@code thickness} blocks of {@code wall} and then {@code air} blocks of air. */
    private static String findsBehindWall(GameTestHelper helper, ItemStack stack, Block ore, Block wall, int thickness, int air) {
        return findsBehind(helper, stack, ore, wall, thickness, air) ? "found" : "lost";
    }

    private static boolean findsBehind(GameTestHelper helper, ItemStack stack, Block ore, Block wall, int thickness, int air) {
        LineWorld world = new LineWorld();
        for (int x = 1; x <= thickness; x++) {
            world.put(x, wall);
        }
        int oreX = thickness + air + 1;
        world.put(oreX, ore);
        return world.pos(oreX).equals(world.scan(helper, stack));
    }

    /** The farthest distance along an empty line at which {@code stack} still finds {@code ore}, counted upwards. */
    private static int airReach(GameTestHelper helper, ItemStack stack, Block ore) {
        int farthest = 0;
        for (int d = 1; d <= 40; d++) {
            LineWorld world = new LineWorld();
            world.put(d, ore);
            if (!world.pos(d).equals(world.scan(helper, stack))) break;
            farthest = d;
        }
        return farthest;
    }

    /** {@code "<name>=<label> "} for {@code blocks[from..to)}. */
    private static String names(Block[] blocks, int from, int to, String label) {
        StringBuilder out = new StringBuilder();
        for (int i = from; i < to; i++) {
            out.append(blocks[i].getName().getString()).append('=').append(label).append(' ');
        }
        return out.toString();
    }

    /** The loss of every material step, e.g. {@code "AIR 0.125, SOFT 2.0, ..."}. */
    private static String lossTable() {
        List<String> steps = new ArrayList<>();
        for (OreDetectorItem.Material material : OreDetectorItem.Material.values()) {
            steps.add(material.name() + " " + material.loss);
        }
        return String.join(", ", steps);
    }

    /**
     * A block source of its own for the long sight lines: air everywhere except the blocks put on
     * the straight line {@code y = 64, z = 0} along +X. The gametest rooms are 8 blocks wide and
     * stand a few blocks apart, so a line of 20 and more blocks through a real level would run
     * through the neighbours' structures. The detector reads blocks from here and enchantments and
     * the registry from the test level - {@code OreDetectorItem#findTarget(BlockGetter, Level, ...)}
     * is the same search the tick runs, the tick merely passes the level for both.
     */
    private static final class LineWorld implements BlockGetter {
        private final Map<Integer, BlockState> line = new HashMap<>();

        void put(int x, Block block) {
            line.put(x, block.defaultBlockState());
        }

        BlockPos pos(int x) {
            return new BlockPos(x, LINE_Y, 0);
        }

        BlockPos scan(GameTestHelper helper, ItemStack stack) {
            return ((OreDetectorItem) ModItems.ORE_DETECTOR).findTarget(this, helper.getLevel(), stack, LINE_EYE);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (pos.getY() != LINE_Y || pos.getZ() != 0) return Blocks.AIR.defaultBlockState();
            return line.getOrDefault(pos.getX(), Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }

    /** A detector in the custom mode, calibrated on {@code block} the way a sneak click stores it. */
    private static ItemStack calibratedOn(Block block) {
        ItemStack stack = detectorInMode(MODE_CUSTOM);
        CompoundTag nbt = customData(stack);
        nbt.put("CustomBlock", NbtUtils.writeBlockState(block.defaultBlockState()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return stack;
    }

    private static String hex(int rgb) {
        return rgb < 0 ? "none" : String.format("%06x", rgb);
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
        Assertions.valueEqual(helper, modeName(helper, detector), "Iron", "mode after two plain clicks");

        // --- the sneak click calibrates and switches to the custom mode ---
        player.setShiftKeyDown(true);
        InteractionResult calibrate = useOn(helper, player, detector, SAMPLE_LOG);
        helper.assertTrue(calibrate == InteractionResult.SUCCESS,
                "the calibration click did not consume the interaction, it returned " + calibrate);
        helper.assertTrue(customData(detector).contains("CustomBlock"),
                "the calibration click stored no target block, the item data is " + customData(detector));
        Assertions.valueEqual(helper, modeName(helper, detector), "Custom",
                "the calibration click has to switch to the custom mode as well, or the stored block "
                        + "is never searched for");

        // --- the search now follows the block, whatever state it stands in ---
        clearCorridor(helper);
        helper.setBlock(rowPos(2), Blocks.STONE);
        setLogAxis(helper, rowPos(3), Direction.Axis.X);
        assertFinds(helper, detector, rowPos(3),
                "an oak log lying on its side after calibrating on an upright one");

        player.setShiftKeyDown(false);
        TestCleanup.succeed(helper);
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

        Assertions.valueEqual(helper, detector.getMaxStackSize(), 1,
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
            Assertions.valueEqual(helper, modeName(helper, detector), cycle[click],
                    "the mode after sneak-use number " + (click + 1));
        }
        Assertions.valueEqual(helper, detector.getDamageValue(), 0,
                "a creative player was billed for switching modes");

        // The control: this player is creative but not instabuild, so vanilla would have let the
        // damage through. If this line ever fails, the assertion above proves nothing.
        helper.assertTrue(player.isCreative(), "the in-level mock player stopped reporting creative");
        helper.assertFalse(player.getAbilities().instabuild,
                "the mock player builds for free again, so vanilla - not the mod - would be refusing the damage");
        detector.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        Assertions.valueEqual(helper, detector.getDamageValue(), 1,
                "vanilla refused to damage this player's tool, so the zero above says nothing about "
                        + "the mod's own creative guard");

        player.setShiftKeyDown(false);
        TestCleanup.succeed(helper);
    }

    /**
     * The tooltip is the only place a player can read which mode is active, what it hunts and
     * how much search power it has, so it is asserted line by line for all six modes.
     *
     * <p>The signal line shows the two numbers the search itself runs on - {@code OreClass#signal}
     * and {@code OreClass#range} of the mode's class. They are measured against real walls and
     * distances in {@link #allOresReachFollowsTheOreRarityAndRadiusStretchesTheRareOnes} and
     * {@link #openAirReachEndsAtTheRangeOfEachOreClass}, so this line is a consistency check
     * between what the item does and what it tells the player. "All ores" and an uncalibrated
     * "custom" advertise the common class, their widest reach, and "all ores" lists the rarer
     * classes on a line of their own, with Radius too.
     *
     * <p>The last two lines cover an index that is not a mode at all: a stack that carries
     * {@code Mode=99} (a hand-edited item, or an old save after a mode was removed) has to clamp
     * to the last mode and a negative one to the first, instead of throwing an
     * {@code ArrayIndexOutOfBoundsException} out of the tooltip and the search.
     *
     * <p>What breaks this: reordering or renaming the modes without moving the indices; changing
     * a signal or range; the custom mode no longer telling a player how to set a target, or naming the
     * wrong block; and losing the clamp in {@code getMode}, which turns a strange item into a
     * crash every time it is hovered.
     */
    public static void tooltipNamesEveryModeWithItsPowerAndTarget(GameTestHelper helper) {
        String[] names = {"Iron", "Gold", "Diamond", "Netherite", "All Ores", "Custom"};
        String[] powers = {"18, Range: 24", "13, Range: 20", "9, Range: 16", "5, Range: 16",
                "18, Range: 24", "18, Range: 24"};

        for (int mode = 0; mode < names.length; mode++) {
            List<String> lines = tooltip(helper, detectorInMode(mode));
            Assertions.valueEqual(helper, lines.getFirst(), MODE_PREFIX + names[mode],
                    "the first tooltip line of mode index " + mode);
            helper.assertTrue(lines.contains("Signal: " + powers[mode]),
                    "mode " + names[mode] + " does not advertise \"Signal: " + powers[mode]
                            + "\", its tooltip is " + lines);
        }

        // "All ores" lists the rarer classes as signal/range, and Radius shows in both lines.
        List<String> allLines = tooltip(helper, detectorInMode(MODE_ALL));
        helper.assertTrue(allLines.contains("Gold 13/20, Diamond/Emerald 9/16, Debris/End ores 5/16"),
                "the all-ores tooltip does not list the rarer classes: " + allLines);
        ItemStack stretched = detectorInMode(MODE_ALL);
        stretched.enchant(enchantment(helper, ModEnchantments.RADIUS), 1);
        List<String> stretchedLines = tooltip(helper, stretched);
        helper.assertTrue(stretchedLines.contains("Signal: 22, Range: 28")
                        && stretchedLines.contains("Gold 17/24, Diamond/Emerald 17/24, Debris/End ores 9/20"),
                "an all-ores detector with Radius does not advertise the stretched table: " + stretchedLines);

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
        Assertions.valueEqual(helper, tooltip(helper, detectorInMode(99)).getFirst(), MODE_PREFIX + "Custom",
                "a Mode of 99 has to clamp to the last mode");
        Assertions.valueEqual(helper, tooltip(helper, detectorInMode(-4)).getFirst(), MODE_PREFIX + "Iron",
                "a negative Mode has to clamp to the first mode");

        TestCleanup.succeed(helper);
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

    /**
     * A position for a failure message, as an offset from the structure origin.
     *
     * <p>Not {@code helper.relativePos}: that is not the inverse of {@code absolutePos}. It builds
     * the counter rotation as {@code rotation.getRotated(CLOCKWISE_180)}, which for
     * {@code Rotation.NONE} turns the position by 180 degrees instead of leaving it alone, so it
     * reports a mirrored x and z. Harmless in a message, but a mirrored coordinate is exactly the
     * wrong thing to hand someone who is already looking for a bug.
     */
    private static String describe(GameTestHelper helper, BlockPos absolute) {
        if (absolute == null) {
            return "nothing at all";
        }
        return absolute.subtract(helper.absolutePos(BlockPos.ZERO)) + " (relative to the room)";
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
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
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
