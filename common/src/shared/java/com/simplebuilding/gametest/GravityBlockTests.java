package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.entity.LevitatingBlockEntity;
import com.simplebuilding.items.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The mod's two pistons and its two pairs of gravity blocks, pinned at the numbers that decide
 * their behaviour: the push limit, the breaking threshold, the two ticks of lead time and the
 * rise curve.
 *
 * <p>{@code BlockBehaviourTests} already proves the coarse claims - a reinforced piston moves a
 * 13 block column a vanilla one refuses, a netherite piston at full signal breaks the stone in
 * front instead of pushing it, suspended sand does not fall, levitating sand rises as an
 * accelerating entity and lands under a ceiling. None of that is repeated here. What is missing
 * there is every <em>boundary</em>: 13 is only "more than 12", "breaks stone at signal 15" fits
 * any threshold above 1.5, and "it accelerates" fits any gravity at all. This class measures the
 * boundaries.
 *
 * <h2>Two rules the tests here follow</h2>
 * <ul>
 *   <li><b>The redstone rig checks itself.</b> Two tests need a signal weaker than 15, which is
 *       built from a redstone block and two dust steps. Before any behaviour is asserted, the
 *       test reads {@code Level#getBestNeighborSignal} - the very call
 *       {@code NetheriteBreakerPistonBlock} makes - and asserts the number it wanted. A failure
 *       there says the rig broke, not the mod.</li>
 *   <li><b>Vanilla is the yardstick, not the mod source.</b> The lead time is compared against a
 *       vanilla sand block placed in the same tick, and the rise velocity against a vanilla sand
 *       block falling in the same ticks, rather than against the constants in
 *       {@code LevitatingBlock} / {@code LevitatingBlockEntity}.</li>
 * </ul>
 *
 * <h2>Known defects</h2>
 *
 * <p><b>1. The breaker can never reach obsidian, and with it the whole top end of its own
 * formula.</b> {@code NetheriteBreakerPistonBlock#triggerEvent} only runs for block event type 0,
 * and vanilla only queues that event after {@code PistonStructureResolver#resolve} succeeded
 * ({@code PistonBaseBlock#checkIfExtend}). {@code resolve} bails out on anything
 * {@code PistonBaseBlock#isPushable} refuses, and that method hard codes
 * {@code OBSIDIAN / CRYING_OBSIDIAN / RESPAWN_ANCHOR / REINFORCED_DEEPSLATE} plus everything with
 * a destroy speed of -1. So the piston never even gets asked about them. The threshold at signal
 * 15 is exactly {@code (15/15)*50 = 50}, which is exactly obsidian's hardness - the one number the
 * formula was evidently written for is the one it can never be applied to. The reachable maximum
 * is a netherite block, which shares the hardness of 50 and <em>is</em> pushable;
 * {@link #netheritePistonBreaksOnlyWhatTheSignalStrengthCanAfford} uses it for that reason.
 *
 * <p><b>2. Two guards in {@code NetheriteBreakerPistonBlock} are dead code.</b> Both follow from
 * defect 1: {@code targetState.getDestroySpeed(...) >= 0} (line 35) and
 * {@code getPistonPushReaction() != PushReaction.BLOCK} (line 43) can only ever be false for a
 * block the resolver already refused, so deleting either changes nothing that can be observed.
 * The {@code !state.getValue(EXTENDED)} guard on line 31 is redundant for a third reason:
 * retraction is signalled with block event type 1 or 2, never 0, so the surrounding
 * {@code if (type == 0)} has already excluded it.
 *
 * <p><b>3. The breaker fires before vanilla's own signal re-check.</b> The mod destroys the block
 * in front and only then calls {@code super.triggerEvent}, which starts with
 * {@code if (!getNeighborSignal(...) && type == 0) return false}. A signal that disappears between
 * the block event being queued and being executed therefore costs the block in front without the
 * piston ever extending. Not tested: the window is one tick wide inside vanilla's block event
 * queue and cannot be hit deterministically from a game test.
 *
 * <p><b>4. {@code NetheritePistonHeadBlock} is dead code.</b> Both mod pistons inherit
 * {@code PistonBaseBlock#moveBlocks}, which places {@code Blocks.PISTON_HEAD}; nothing anywhere
 * constructs the mod's own head. Pinned as a marker in
 * {@link #modPistonsAreNotStickyAndUseTheVanillaHead} rather than as a claim that this is right.
 *
 * <p><b>5. The waterlogging code in {@code LevitatingBlockEntity} is dead code.</b>
 * {@code rise()} clears {@code WATERLOGGED} on the spawn state and puts
 * {@code state.getFluidState().createLegacyBlock()} back into the world; the landing branch sets
 * {@code WATERLOGGED} again. Neither levitating block has that property - both are
 * {@code Properties.ofFullCopy(Blocks.SAND / GRAVEL)} - so the fluid state is always empty and the
 * source position always becomes air, water or not. Pinned as a marker in
 * {@link #blockedLandingSpotsDropTheBlockOrKeepItFlying}.
 *
 * <p><b>6. No gravity block is in any {@code mineable} tag.</b> {@code ModBlockTagProvider} lists
 * the three piston blocks under {@code mineable/pickaxe} and generates no shovel tag at all, and
 * {@code Properties.ofFullCopy} copies behaviour, not tags. Suspended and levitating sand and
 * gravel therefore lose the shovel speed bonus their vanilla originals have. Pinned as a marker in
 * {@link #gravityBlocksAndPistonsCarryTheirRegisteredStrengthAndTags}, in the style of defects 4
 * and 5: not as a claim that the four blocks belong outside every tool tag, but so that adding one
 * of them to {@code mineable/pickaxe} - or generating a {@code mineable/shovel} file at all - has
 * to go past a red test and rewrite this paragraph, instead of leaving it quietly stale.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The breaking sound</b> ({@code SoundEvents.ZOMBIE_ATTACK_IRON_DOOR}).
 *       {@code world.playSound(null, ...)} only queues a packet for nearby players; there is
 *       nothing server side to observe.</li>
 *   <li><b>The {@code minY} clause and the {@code time > 600} emergency brake in
 *       {@code LevitatingBlockEntity#tick}.</b> The first needs an entity below the world floor,
 *       far outside the 8 block room and with its drop landing in the void; the second needs a 600
 *       tick budget for one branch. The {@code maxY} twin of the same {@code else if} is already
 *       covered by {@code BlockBehaviourTests#levitatingSandDropsAsAnItemAtTheBuildLimit}.</li>
 *   <li><b>{@code NetheritePistonHeadBlock#canSurvive / updateShape / playerWillDestroy /
 *       getCloneItemStack}.</b> Testing them would mean placing the block by hand and thereby
 *       describing behaviour no player can reach; see defect 4 for what is pinned instead.</li>
 *   <li><b>{@code PistonHeadBlockMixin}.</b> It lets a vanilla piston head survive behind a mod
 *       piston, which is what keeps the extended mod pistons in this file intact - every
 *       extend/retract test here would fail without it - but it has no observable effect of its
 *       own beyond that.</li>
 * </ul>
 */
public final class GravityBlockTests {

    /** Tick budget for {@link #reinforcedPistonMovesEighteenBlocksWhileTheNetheriteOneKeepsVanillasTwelve}. */
    public static final int PUSH_LIMIT_MAX_TICKS = 60;

    /** Tick budget for {@link #netheritePistonBreaksOnlyWhatTheSignalStrengthCanAfford}. */
    public static final int BREAK_THRESHOLD_MAX_TICKS = 60;

    /** Tick budget for {@link #modPistonsAreNotStickyAndUseTheVanillaHead}. */
    public static final int RETRACTION_MAX_TICKS = 80;

    /** Tick budget for {@link #extendedModPistonsCannotBeShovedByOtherPistons}. */
    public static final int PISTON_VERSUS_PISTON_MAX_TICKS = 80;

    /** Tick budget for {@link #levitatingSandLeavesOnVanillasScheduleAndRisesOnItsCurve}. */
    public static final int RISE_CURVE_MAX_TICKS = 60;

    /** Tick budget for {@link #levitatingSandWaitsUnderTheCeilingUntilTheWayUpIsFree}. */
    public static final int CEILING_WAIT_MAX_TICKS = 100;

    /** Tick budget for {@link #blockedLandingSpotsDropTheBlockOrKeepItFlying}. */
    public static final int BLOCKED_LANDING_MAX_TICKS = 80;

    /** Tick budget for {@link #suspendedSandLetsItemsThroughWhileSuspendedGravelHoldsThem}. */
    public static final int COLLISION_MAX_TICKS = 80;

    /**
     * The reinforced piston's raised limit, {@code PistonHandlerMixin} line 26. Not read from the
     * mixin: {@link #reinforcedPistonMovesEighteenBlocksWhileTheNetheriteOneKeepsVanillasTwelve}
     * drives one column of this height and one of this height plus one, so the number is the
     * boundary that is measured rather than a value copied across.
     */
    private static final int REINFORCED_LIMIT = 18;

    /** Vanilla's limit, {@code PistonStructureResolver#MAX_PUSH_DEPTH}. */
    private static final int VANILLA_LIMIT = 12;

    /**
     * Ticks between the two velocity samples in
     * {@link #levitatingSandLeavesOnVanillasScheduleAndRisesOnItsCurve}. Four ticks is enough for
     * a wrong drag or a wrong gravity to show up far above the tolerance, and short enough that
     * the entity is nowhere near the ceiling at either sample.
     */
    private static final int VELOCITY_SAMPLE_GAP = 4;

    /**
     * Vanilla's air drag, {@code Entity#getAirDrag}, and vanilla's falling gravity,
     * {@code FallingBlockEntity}'s {@code getDefaultGravity} of 0.04. They appear here as the
     * curve the mod claims to mirror, not as a copy of {@code LevitatingBlockEntity}'s own two
     * constants - and the same test measures them a second time against a real falling sand block.
     */
    private static final double VANILLA_AIR_DRAG = 0.98D;

    private static final double VANILLA_GRAVITY_STEP = 0.04D;

    /**
     * Slack for the velocity comparisons. The mod stores its drag as {@code 0.98D} while vanilla
     * widens {@code 0.98F} to {@code 0.9800000190734863}, so the two curves differ by roughly
     * 1e-8 after a handful of ticks; a drag of 0.9 or a gravity of 0.05 differs by more than 1e-2.
     */
    private static final double VELOCITY_TOLERANCE = 1.0E-6D;

    /**
     * Slack for the two distance comparisons. What has to fit inside it is double rounding and the
     * same 1e-8 wide drag gap as above; what must not is the difference the comparison is for. The
     * two possible orders of movement and drag differ by the drag factor on every step, which over
     * the four ticks measured here is about 0.02 blocks - two hundred times this tolerance.
     */
    private static final double TRAVEL_TOLERANCE = 1.0E-4D;

    private GravityBlockTests() {
    }

    // =====================================================================================
    // PISTONS: HOW MUCH THEY PUSH
    // =====================================================================================

    /**
     * {@code PistonHandlerMixin} replaces vanilla's 12 with 18 for the reinforced piston, and for
     * that piston only. Three columns run at once: 18 blocks on a reinforced piston must move, 19
     * must not, and 13 on a netherite piston must not either.
     *
     * <p>The three together are what pins the number. A single "more than 12" column - which is
     * what {@code BlockBehaviourTests#reinforcedPistonPushesThirteenBlocksWhereVanillaPistonRefuses}
     * drives - stays green for any limit from 13 upwards, so the mixin could return 13, 40 or
     * {@code Integer.MAX_VALUE} unnoticed. Here the pair 18/19 brackets it from both sides.
     *
     * <p>The netherite column is the other half of the mixin's condition,
     * {@code state.is(ModBlocks.REINFORCED_PISTON)}. Widening it to "any mod piston" would let the
     * netherite piston fire, and the first thing it would do is destroy the stone at the bottom of
     * its own column, so the assertion checks both that it did not extend and that its column is
     * untouched.
     *
     * <p>What breaks this test: any other value in {@code PistonHandlerMixin} line 26, dropping the
     * {@code @ModifyConstant} altogether, and broadening or narrowing the block check on line 25.
     *
     * <p>Wants {@code skyAccess(true)}: the tallest column reaches 21 blocks above the floor of the
     * 8 block room. The travel path is cleared with air first, so a barrier cage would not change
     * the outcome, only the tidiness.
     */
    public static void reinforcedPistonMovesEighteenBlocksWhileTheNetheriteOneKeepsVanillasTwelve(
            GameTestHelper helper) {
        BlockPos atLimit = new BlockPos(1, 1, 1);
        BlockPos overLimit = new BlockPos(5, 1, 1);
        BlockPos netherite = new BlockPos(1, 1, 5);

        int overLimitHeight = REINFORCED_LIMIT + 1;
        int netheriteHeight = VANILLA_LIMIT + 1;

        buildColumnOnPiston(helper, atLimit, ModBlocks.REINFORCED_PISTON, REINFORCED_LIMIT);
        buildColumnOnPiston(helper, overLimit, ModBlocks.REINFORCED_PISTON, overLimitHeight);
        buildColumnOnPiston(helper, netherite, ModBlocks.NETHERITE_PISTON, netheriteHeight);

        // All three in the same tick, so no column can claim it simply had more time.
        helper.setBlock(atLimit.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(overLimit.east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(netherite.west(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // 18 blocks: moved, so the column now starts one higher and ends one higher.
                    helper.assertBlockProperty(atLimit, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, atLimit.above());
                    helper.assertBlockPresent(Blocks.STONE, atLimit.above(REINFORCED_LIMIT + 1));
                    helper.assertBlockNotPresent(Blocks.STONE, atLimit.above());

                    // 19 blocks: one over the limit, so nothing moved at all.
                    helper.assertBlockProperty(overLimit, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockPresent(Blocks.STONE, overLimit.above());
                    helper.assertBlockNotPresent(Blocks.STONE, overLimit.above(overLimitHeight + 1));

                    // Netherite piston, 13 blocks: still vanilla's limit, and nothing broken either.
                    helper.assertBlockProperty(netherite, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockPresent(Blocks.STONE, netherite.above());
                    helper.assertBlockPresent(Blocks.STONE, netherite.above(netheriteHeight));
                    helper.assertBlockNotPresent(Blocks.STONE, netherite.above(netheriteHeight + 1));
                })
                .thenSucceed();
    }

    // =====================================================================================
    // PISTONS: WHAT THE NETHERITE ONE BREAKS
    // =====================================================================================

    /**
     * The netherite piston's threshold is {@code (signal / 15) * 50} compared against the block's
     * hardness, so both halves of that formula have to be measured: the factor 50 and the
     * dependency on the signal strength.
     *
     * <p>Six pistons, four signal strengths, five targets:
     * <ul>
     *   <li><b>Signal 15 on a netherite block</b> (hardness 50, and pushable, unlike obsidian):
     *       threshold 50.0, hardness 50.0, so it breaks - just. This is the upper end of the
     *       formula, and the only pushable block in the game that sits on it.</li>
     *   <li><b>Signal 14 on a netherite block</b>: threshold 46.67, so it must survive <em>and be
     *       pushed one block instead</em> - the "too hard for this signal, so treat it like an
     *       ordinary piston" branch, which no test entered before.</li>
     *   <li><b>Signal 14 on stone</b> (hardness 1.5): threshold 46.67, so it still breaks. Without
     *       this third case the pair above would also be satisfied by "breaking only happens at
     *       full signal", which is a different rule.</li>
     *   <li><b>Signal 1 on a stonecutter</b> (hardness 3.5): threshold 3.33, so it survives and is
     *       pushed. This case is what holds the factor down from above, and it has to sit at the
     *       weak end of the signal range because nothing else can. The two netherite block cases
     *       alone leave the factor anywhere in {@code [50, 53.57)} - at signal 14 a factor of 53
     *       still keeps the threshold under 50 - and no pushable block in the game has a hardness
     *       inside that gap: obsidian, crying obsidian, the respawn anchor and reinforced deepslate
     *       are the only candidates and {@code isPushable} refuses all four by name. Weakening the
     *       signal instead scales the whole window down, and 3.5 against a signal of 1 is the
     *       tightest ratio vanilla offers: it says the factor is under 52.5.</li>
     *   <li><b>Signal 15 on obsidian and on bedrock</b>: neither may be touched, and the piston
     *       may not even extend. See defect 1 in the class javadoc - vanilla's structure resolver
     *       refuses first, so this pins the outcome a player sees, not mod code. The premise that
     *       vanilla still refuses is asserted directly rather than assumed.</li>
     * </ul>
     *
     * <p>Taken together the three measured cases bracket the factor into {@code [50, 52.5)}: the
     * threshold has to reach 50 at signal 15, stay under 50 at signal 14 and stay under 3.5 at
     * signal 1. Vanilla's stonecutter hardness is asserted alongside the signals for that reason -
     * the upper bound is only as good as that 3.5. The drops are checked as well, because
     * {@code world.destroyBlock(targetPos, true)} is what makes the difference between mining a
     * block and deleting it; the stone case asks for cobblestone specifically, which only appears
     * if the loot table actually ran.
     *
     * <p>What breaks this test: any factor outside {@code [50, 52.5)}, dropping the
     * {@code power / 15.0f} term,
     * inverting the {@code blockHardness <= breakThreshold} comparison, replacing
     * {@code getBestNeighborSignal} with a fixed strength, and passing {@code false} to
     * {@code destroyBlock} so the block vanishes without drops.
     */
    public static void netheritePistonBreaksOnlyWhatTheSignalStrengthCanAfford(GameTestHelper helper) {
        fillFloor(helper);

        BlockPos fullOnNetherite = new BlockPos(1, 1, 1);
        BlockPos fullOnObsidian = new BlockPos(4, 1, 1);
        BlockPos fullOnBedrock = new BlockPos(7, 1, 1);
        BlockPos weakOnNetherite = new BlockPos(3, 1, 4);
        BlockPos weakOnStone = new BlockPos(3, 1, 6);
        BlockPos faintOnStonecutter = new BlockPos(7, 1, 4);

        placeBreakerWithTarget(helper, fullOnNetherite, Blocks.NETHERITE_BLOCK);
        placeBreakerWithTarget(helper, fullOnObsidian, Blocks.OBSIDIAN);
        placeBreakerWithTarget(helper, fullOnBedrock, Blocks.BEDROCK);
        placeBreakerWithTarget(helper, weakOnNetherite, Blocks.NETHERITE_BLOCK);
        placeBreakerWithTarget(helper, weakOnStone, Blocks.STONE);
        placeBreakerWithTarget(helper, faintOnStonecutter, Blocks.STONECUTTER);

        // Signal 15: a redstone block right next to the piston.
        helper.setBlock(fullOnNetherite.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(fullOnObsidian.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(fullOnBedrock.west(), Blocks.REDSTONE_BLOCK);

        // Signal 14: redstone block, then two dust steps. The first dust carries 15, the second 14.
        buildWeakSignalLine(helper, weakOnNetherite);
        buildWeakSignalLine(helper, weakOnStone);

        // Signal 1: a comparator reading a composter that is filled one level.
        buildFaintSignalLine(helper, faintOnStonecutter);

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    // --- the rig, checked with the very call the mod makes ---
                    assertSignalStrength(helper, fullOnNetherite, 15);
                    assertSignalStrength(helper, fullOnObsidian, 15);
                    assertSignalStrength(helper, fullOnBedrock, 15);
                    assertSignalStrength(helper, weakOnNetherite, 14);
                    assertSignalStrength(helper, weakOnStone, 14);
                    assertSignalStrength(helper, faintOnStonecutter, 1);

                    // --- the premise behind the obsidian and bedrock cases ---
                    ServerLevel level = helper.getLevel();
                    BlockPos probe = helper.absolutePos(fullOnObsidian.above());
                    helper.assertFalse(
                            PistonBaseBlock.isPushable(Blocks.OBSIDIAN.defaultBlockState(), level, probe,
                                    Direction.UP, false, Direction.UP),
                            "vanilla now lets pistons move obsidian, so PistonStructureResolver no longer "
                                    + "shields the breaker from it and the obsidian case below has to be "
                                    + "rewritten as real mod coverage");
                    helper.assertFalse(
                            PistonBaseBlock.isPushable(Blocks.BEDROCK.defaultBlockState(), level, probe,
                                    Direction.UP, false, Direction.UP),
                            "vanilla now lets pistons move bedrock; the same applies to the bedrock case");

                    // --- the premise behind the stonecutter case ---
                    // The upper bound on the factor is 15 * hardness / signal, so it is worth
                    // exactly as much as this hardness is. Soften the stonecutter and the window
                    // this test claims to measure has silently moved.
                    helper.assertValueEqual(
                            Blocks.STONECUTTER.defaultBlockState().getDestroySpeed(level, probe),
                            3.5F,
                            "vanilla's stonecutter hardness, which is what caps the breaking factor "
                                    + "at 15 * 3.5 / 1 = 52.5");
                })
                .thenExecuteAfter(20, () -> {
                    // --- signal 15, hardness 50: broken, with its drop ---
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, fullOnNetherite.above());
                    helper.assertBlockNotPresent(Blocks.NETHERITE_BLOCK, fullOnNetherite.above(2));
                    helper.assertItemEntityPresent(Blocks.NETHERITE_BLOCK.asItem(),
                            fullOnNetherite.above(), 2.0D);

                    // --- signal 14, hardness 50: too hard, so pushed like an ordinary piston ---
                    helper.assertBlockProperty(weakOnNetherite, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, weakOnNetherite.above());
                    helper.assertBlockPresent(Blocks.NETHERITE_BLOCK, weakOnNetherite.above(2));

                    // --- signal 14, hardness 1.5: still well over the threshold, so broken ---
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, weakOnStone.above());
                    helper.assertBlockNotPresent(Blocks.STONE, weakOnStone.above(2));
                    helper.assertItemEntityPresent(Items.COBBLESTONE, weakOnStone.above(), 2.0D);

                    // --- signal 1, hardness 3.5: just above the threshold of 3.33, so pushed ---
                    // The factor would have to be 52.5 or more for this one to break, which is what
                    // stops the whole formula from drifting upwards behind the two netherite cases.
                    helper.assertBlockProperty(faintOnStonecutter, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, faintOnStonecutter.above());
                    helper.assertBlockPresent(Blocks.STONECUTTER, faintOnStonecutter.above(2));

                    // --- what the breaker never gets to see ---
                    helper.assertBlockPresent(Blocks.OBSIDIAN, fullOnObsidian.above());
                    helper.assertBlockProperty(fullOnObsidian, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockPresent(Blocks.BEDROCK, fullOnBedrock.above());
                    helper.assertBlockProperty(fullOnBedrock, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                })
                .thenSucceed();
    }

    // =====================================================================================
    // PISTONS: RETRACTING, AND WHAT THE HEAD IS
    // =====================================================================================

    /**
     * Both mod pistons are built with {@code super(false, ...)}, so neither may drag anything back
     * when it retracts. Nothing in the suite ever retracted one of them before, which means the
     * sticky flag could be flipped in {@code ModBlocks} or in
     * {@code NetheriteBreakerPistonBlock}'s constructor without a single test noticing.
     *
     * <p>Both halves put a stone block exactly where a sticky piston would grab it - directly in
     * front of the extended head:
     * <ul>
     *   <li>The reinforced piston pushes a stone from directly in front of it to one block
     *       further; after the retraction that stone has to stay at its new place.</li>
     *   <li>The netherite piston gets air in front and a stone one block beyond, so it extends
     *       without breaking anything and its head ends up next to that stone. If it were sticky,
     *       retracting would pull the stone into the freed cell.</li>
     * </ul>
     *
     * <p>The netherite half is also the closest the suite can get to "only extending breaks".
     * Retraction is signalled with block event type 1 or 2 rather than 0 <em>and</em> arrives with
     * the signal already gone, which makes the threshold 0 - two independent reasons why nothing
     * can break, so the assertion pins the outcome and not the guard; see defect 2.
     *
     * <p>Both halves additionally pin that the head placed in front is vanilla's
     * {@code minecraft:piston_head} and not {@code ModBlocks.NETHERITE_PISTON_HEAD}. That is a
     * marker for defect 4, in the style of
     * {@code OreGenAndItemFrameTests#brushRevealIsWiredToAnInterfaceNothingImplements}: it is not a
     * claim that the mod's own head block should stay unused, it fails the day somebody wires it
     * up, and that is exactly when {@code NetheritePistonHeadBlock} needs real tests.
     *
     * <p>What breaks this test: {@code super(true, ...)} in either piston, a retraction that
     * destroys the block in front of the head, and a {@code moveBlocks} override that places a
     * different head block.
     */
    public static void modPistonsAreNotStickyAndUseTheVanillaHead(GameTestHelper helper) {
        BlockPos reinforced = new BlockPos(1, 1, 1);
        BlockPos netherite = new BlockPos(5, 1, 1);

        helper.setBlock(reinforced, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(reinforced.above(), Blocks.STONE);

        helper.setBlock(netherite, upright(ModBlocks.NETHERITE_PISTON));
        helper.setBlock(netherite.above(), Blocks.AIR);
        helper.setBlock(netherite.above(2), Blocks.STONE);

        helper.setBlock(reinforced.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(netherite.east(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // Both extended, both carrying a vanilla head.
                    helper.assertBlockProperty(reinforced, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockProperty(netherite, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, reinforced.above());
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, netherite.above());
                    helper.assertBlockNotPresent(ModBlocks.NETHERITE_PISTON_HEAD, netherite.above());
                    helper.assertBlockNotPresent(ModBlocks.NETHERITE_PISTON_HEAD, reinforced.above());

                    // The reinforced piston moved its stone; the netherite one had air in front and
                    // therefore had nothing to break.
                    helper.assertBlockPresent(Blocks.STONE, reinforced.above(2));
                    helper.assertBlockPresent(Blocks.STONE, netherite.above(2));

                    // Cut the power.
                    helper.setBlock(reinforced.west(), Blocks.AIR);
                    helper.setBlock(netherite.east(), Blocks.AIR);
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertBlockProperty(reinforced, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockProperty(netherite, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockNotPresent(Blocks.PISTON_HEAD, reinforced.above());
                    helper.assertBlockNotPresent(Blocks.PISTON_HEAD, netherite.above());

                    // Not sticky: both stones stay where they were, neither is dragged into the
                    // cell the head has just left.
                    helper.assertBlockPresent(Blocks.STONE, reinforced.above(2));
                    helper.assertBlockNotPresent(Blocks.STONE, reinforced.above());
                    helper.assertBlockPresent(Blocks.STONE, netherite.above(2));
                    helper.assertBlockNotPresent(Blocks.STONE, netherite.above());
                })
                .thenSucceed();
    }

    /**
     * {@code PistonBlockMixin} makes an extended mod piston immovable. Without it the mixin's job
     * would fall to vanilla, and vanilla does not do it: {@code PistonBaseBlock#isPushable} only
     * knows the {@code EXTENDED} rule for {@code Blocks.PISTON} and {@code Blocks.STICKY_PISTON}
     * by name, and everything else is judged by its push reaction - which for the mod pistons is
     * {@code NORMAL}, because {@code ModBlocks#registerBlock} starts every block from
     * {@code Properties.ofFullCopy(Blocks.GLASS)} rather than from vanilla's piston properties.
     * An extended mod piston is therefore a perfectly ordinary pushable block unless the mixin
     * says otherwise.
     *
     * <p>Three rows, all driven by a vanilla piston pushing sideways:
     * <ul>
     *   <li>an extended reinforced piston - the vanilla piston must not even extend;</li>
     *   <li>an extended netherite piston - same, since the mixin covers both;</li>
     *   <li>a retracted reinforced piston - the control. It has to be shoved one block along,
     *       which is what proves the two refusals above come from the {@code EXTENDED} check and
     *       not from the mod pistons being immovable in general.</li>
     * </ul>
     *
     * <p>Each mod piston is extended by its own redstone block <em>underneath</em> it rather than
     * beside it, so no power source is ever adjacent to the vanilla piston or to the cells vanilla
     * checks for quasi connectivity. The mod pistons are extended first and only then is the
     * vanilla piston powered, so an extended state that was set by hand cannot be retracted again
     * by the piston's own {@code checkIfExtend}.
     *
     * <p>What breaks this test: deleting {@code PistonBlockMixin}, dropping either block from its
     * condition, or removing the {@code EXTENDED} check inside it - the last one turns the control
     * row red instead.
     */
    public static void extendedModPistonsCannotBeShovedByOtherPistons(GameTestHelper helper) {
        BlockPos reinforcedVictim = new BlockPos(2, 1, 1);
        BlockPos netheriteVictim = new BlockPos(2, 1, 3);
        BlockPos retractedVictim = new BlockPos(2, 1, 5);

        BlockPos pusherAtReinforced = new BlockPos(1, 1, 1);
        BlockPos pusherAtNetherite = new BlockPos(1, 1, 3);
        BlockPos pusherAtRetracted = new BlockPos(1, 1, 5);

        helper.setBlock(reinforcedVictim, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(netheriteVictim, upright(ModBlocks.NETHERITE_PISTON));
        helper.setBlock(retractedVictim, upright(ModBlocks.REINFORCED_PISTON));

        helper.setBlock(pusherAtReinforced, facing(Blocks.PISTON, Direction.EAST));
        helper.setBlock(pusherAtNetherite, facing(Blocks.PISTON, Direction.EAST));
        helper.setBlock(pusherAtRetracted, facing(Blocks.PISTON, Direction.EAST));

        // Power from below, so nothing reaches the vanilla pistons or the cells above them.
        helper.setBlock(reinforcedVictim.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(netheriteVictim.below(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertBlockProperty(reinforcedVictim, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockProperty(netheriteVictim, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockProperty(retractedVictim, PistonBaseBlock.EXTENDED, Boolean.FALSE);

                    // Only now switch the three vanilla pistons on.
                    helper.setBlock(pusherAtReinforced.west(), Blocks.REDSTONE_BLOCK);
                    helper.setBlock(pusherAtNetherite.west(), Blocks.REDSTONE_BLOCK);
                    helper.setBlock(pusherAtRetracted.west(), Blocks.REDSTONE_BLOCK);
                })
                .thenExecuteAfter(20, () -> {
                    // The two extended mod pistons stayed put, and the vanilla pistons in front of
                    // them could not even start.
                    helper.assertBlockPresent(ModBlocks.REINFORCED_PISTON, reinforcedVictim);
                    helper.assertBlockPresent(ModBlocks.NETHERITE_PISTON, netheriteVictim);
                    helper.assertBlockProperty(pusherAtReinforced, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockProperty(pusherAtNetherite, PistonBaseBlock.EXTENDED, Boolean.FALSE);

                    // Control: the retracted one is an ordinary block and gets shoved.
                    helper.assertBlockProperty(pusherAtRetracted, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, retractedVictim);
                    helper.assertBlockPresent(ModBlocks.REINFORCED_PISTON, retractedVictim.east());
                })
                .thenSucceed();
    }

    // =====================================================================================
    // LEVITATING BLOCKS: WHEN THEY LEAVE, AND HOW FAST
    // =====================================================================================

    /**
     * Two numbers of {@code LevitatingBlock} / {@code LevitatingBlockEntity} that no test pinned:
     * the two ticks of lead time before a freshly placed block lifts off, and the curve it rises
     * on afterwards. Both are measured against a vanilla sand block instead of against the mod's
     * own constants, because vanilla is what the mod claims to mirror.
     *
     * <p><b>Lead time.</b> A levitating sand block and a vanilla sand block are placed in the same
     * tick, one with free space above and one with free space below, and the tick each of them
     * stops being a block is recorded. {@code FallingBlock#onPlace} schedules its tick two ticks
     * out; {@code LevitatingBlock} has to do the same, so the two ticks must be equal. Values from
     * 1 to 5 are all indistinguishable if you only wait for the block to be gone, which is what
     * the existing tests do.
     *
     * <p><b>Curve.</b> The rising entity's vertical speed is sampled twice, four ticks apart, and
     * checked two ways. Against vanilla: the falling sand's speed at the same two instants must be
     * the exact mirror, which pins gravity and drag together without depending on which tick the
     * entities happened to spawn on. Against the arithmetic: the second sample has to be the first
     * one put four times through {@code v = 0.98 * (v + 0.04)}. Because the second check only
     * relates the two samples to each other, it is immune to tick alignment as well - it would
     * hold at any point on the curve, and it fails for any other drag or any other gravity.
     *
     * <p><b>Distance, not only speed.</b> The height of both entities is read at the same two
     * instants, and the two are checked the same two ways: the rise has to be the mirror of the
     * fall, and it has to match the sum of the four steps the sampled speed prescribes. Speed
     * alone cannot see the order of {@code applyGravity} / {@code move} / drag, because both
     * orders store the identical sequence {@code v = 0.98 * (v + 0.04)}; they differ only in what
     * a tick covers - {@code v + 0.04} when the drag comes after the movement, 2% less when it
     * comes before. That 2% is the difference between the terminal 2.0 blocks per tick the class
     * javadoc of {@code LevitatingBlockEntity} claims and 1.96, and it is the only thing standing
     * between this test and a mod that never calls {@code move} at all: with the distance half
     * gone, deleting that call outright leaves all four speed assertions above intact.
     *
     * <p>The distance half leans on vanilla twice over: {@code FallingBlockEntity#tick} is where
     * the "drag after the movement" order comes from in the first place, and the falling sand in
     * this room is measured against the rising block rather than against a number.
     *
     * <p>Neither sample is taken anywhere near an obstacle: after eight entity ticks each block
     * has travelled about 1.25 blocks, well inside the room.
     *
     * <p>What breaks this test: another {@code DELAY_AFTER_PLACE}, another {@code RISE_GRAVITY} or
     * {@code AIR_DRAG}, applying the drag before the movement instead of after, and dropping the
     * {@code move(MoverType.SELF, ...)} call.
     */
    public static void levitatingSandLeavesOnVanillasScheduleAndRisesOnItsCurve(GameTestHelper helper) {
        BlockPos rising = new BlockPos(1, 1, 1);
        BlockPos falling = new BlockPos(5, 5, 1);

        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 0, 1), Blocks.STONE);

        helper.setBlock(rising, ModBlocks.LEVITATING_SAND);
        helper.setBlock(falling, Blocks.SAND);

        long[] leftAt = {-1L, -1L};
        helper.onEachTick(() -> {
            if (leftAt[0] < 0 && !helper.getBlockState(rising).is(ModBlocks.LEVITATING_SAND)) {
                leftAt[0] = helper.getTick();
            }
            if (leftAt[1] < 0 && !helper.getBlockState(falling).is(Blocks.SAND)) {
                leftAt[1] = helper.getTick();
            }
        });

        double[] firstSample = {Double.NaN, Double.NaN};
        double[] firstHeight = {Double.NaN, Double.NaN};

        helper.startSequence()
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(leftAt[0] >= 0 && leftAt[1] >= 0,
                            "both blocks should have turned into entities by now, left at "
                                    + leftAt[0] + " (levitating) and " + leftAt[1] + " (vanilla sand)");
                    helper.assertValueEqual(leftAt[0], leftAt[1],
                            "the tick levitating sand lifts off, against the tick vanilla sand starts "
                                    + "falling - both are placed in the same tick, so a different lead "
                                    + "time in LevitatingBlock shows up as a different tick here");

                    LevitatingBlockEntity flyer = riser(helper);
                    FallingBlockEntity sand = faller(helper);
                    firstSample[0] = flyer.getDeltaMovement().y;
                    firstSample[1] = sand.getDeltaMovement().y;
                    firstHeight[0] = flyer.getY();
                    firstHeight[1] = sand.getY();
                    helper.assertTrue(firstSample[0] > 0.0D,
                            "the levitating entity should be moving upwards, speed " + firstSample[0]);
                    helper.assertTrue(firstSample[1] < 0.0D,
                            "the vanilla sand should be moving downwards, speed " + firstSample[1]);
                })
                .thenExecuteAfter(VELOCITY_SAMPLE_GAP, () -> {
                    LevitatingBlockEntity flyer = riser(helper);
                    FallingBlockEntity sand = faller(helper);
                    double rise = flyer.getDeltaMovement().y;
                    double fall = sand.getDeltaMovement().y;
                    double rose = flyer.getY() - firstHeight[0];
                    double fell = sand.getY() - firstHeight[1];

                    // 1. mirrored against vanilla, at both samples
                    assertMirrors(helper, firstSample[0], firstSample[1], "first sample");
                    assertMirrors(helper, rise, fall, "second sample");

                    // 2. the curve between the two samples, on its own terms. Both the speed after
                    // four ticks and the ground covered in them follow from the first sample, and
                    // the second is where the order of movement and drag becomes visible: a tick
                    // covers v + 0.04 and only then stores 0.98 of it.
                    double predicted = firstSample[0];
                    double predictedTravel = 0.0D;
                    for (int step = 0; step < VELOCITY_SAMPLE_GAP; step++) {
                        double thisTick = predicted + VANILLA_GRAVITY_STEP;
                        predictedTravel += thisTick;
                        predicted = VANILLA_AIR_DRAG * thisTick;
                    }
                    helper.assertTrue(Math.abs(rise - predicted) < VELOCITY_TOLERANCE,
                            "after " + VELOCITY_SAMPLE_GAP + " ticks the rise speed should have gone "
                                    + "from " + firstSample[0] + " to " + predicted
                                    + " on v = 0.98 * (v + 0.04), but it is " + rise);
                    helper.assertTrue(Math.abs(rose - predictedTravel) < TRAVEL_TOLERANCE,
                            "in those " + VELOCITY_SAMPLE_GAP + " ticks the block should have risen "
                                    + predictedTravel + " blocks, which is what the sampled speeds add "
                                    + "up to when the drag is applied after the movement, but it rose "
                                    + rose + " (" + (VANILLA_AIR_DRAG * predictedTravel) + " would be "
                                    + "the drag applied first, 0.0 a missing move() call)");

                    // 3. and the same distance against vanilla, which is where the order comes from
                    helper.assertTrue(Math.abs(rose + fell) < TRAVEL_TOLERANCE,
                            "the distance covered should be the exact mirror of the vanilla sand's in "
                                    + "the same four ticks, but the block rose " + rose
                                    + " while the sand fell " + fell);
                })
                .thenExecute(() -> risingEntities(helper).forEach(LevitatingBlockEntity::discard))
                .thenSucceed();
    }

    /**
     * A levitating block only lifts off when the cell above it is free, and it has to try again
     * when that changes. {@code BlockBehaviourTests#levitatingSandTurnsBackIntoABlockUnderACeiling}
     * only ever watched a block that had already flown; a block that never got to fly, and a
     * ceiling that is taken away again, are both untested.
     *
     * <p>Two phases in one room:
     * <ul>
     *   <li>With stone directly above it the sand has to stay a block for a full 20 ticks, no
     *       rising entity may exist, and the wall torch hanging on the block has to survive. The
     *       torch is the assertion that carries this phase; see below.</li>
     *   <li>Then that stone is removed. The only thing that can wake the block now is
     *       {@code LevitatingBlock#updateShape}, which reschedules a tick on any neighbour change;
     *       {@code onPlace}'s original schedule fired long ago and nothing else is watching. The
     *       block has to lift off and land under the higher ceiling that has been sitting three
     *       cells above it all along - and the torch has to be gone afterwards, which is what
     *       proves it was a live detector in phase one and not decoration.</li>
     * </ul>
     *
     * <p><b>A second column, for what "free" means.</b> Stone and air answer
     * {@code FallingBlock.isFree} and a plain {@code isAir()} the same way, so a room that only
     * ever puts one of those two above a levitating block cannot tell the two apart: the guard
     * could be narrowed to {@code isAir()} and every column above would still behave. The second
     * column therefore starts under a structure void - not air, but replaceable, so vanilla counts
     * it as free - and that block has to lift off all the same. Both halves of the distinction are
     * asserted on the lid itself before its column is judged, so a vanilla change to either answer
     * shows up as a rig failure rather than as a silent pass. A structure void is used because it
     * is the only "free" state that neither collides (a snow layer would stop the entity dead
     * against the cell it is trying to leave), nor needs something to sit on (short grass wants
     * dirt below it), nor spreads or burns out (water and fire do both).
     *
     * <p>The column pins the {@code isFree} in {@code LevitatingBlock#tick} only. The second call,
     * {@code wouldContinueRising} in {@code LevitatingBlockEntity}, would need a lid that is free
     * <em>and</em> collides - a snow layer - and the entity would then have to be watched for not
     * settling; that one stays uncovered.
     *
     * <p><b>Why a torch and not just "the block is still there".</b> Deleting the
     * {@code FallingBlock.isFree(above)} guard in {@code LevitatingBlock#tick} leaves no trace at
     * the end of a tick. Scheduled block ticks run before the entity list - {@code ServerLevel#tick}
     * does {@code tickPending} first and {@code entities} afterwards - and an entity added during
     * the block tick phase is already in {@code EntityTickList} for that same tick. So without the
     * guard the block turns into an entity at y=1.0, rises the 0.02 blocks its 0.98 tall hitbox has
     * left under the lid at y=2, collides upwards and is put straight back at (1,1,1), all inside
     * one game tick. Every assertion a game test can make runs after the level tick, because the
     * test ticker is a server tickable, so the cell would look untouched at every single sample:
     * the block would leave and come back once every two ticks and nothing above would notice. The
     * torch does, because {@code LevitatingBlockEntity#rise} sets the cell to air with flag 3 and
     * the shape update that follows breaks a torch whose support has vanished - once, for good.
     *
     * <p>What breaks this test: removing the free-space check (phase one loses its torch within
     * two ticks), narrowing it from {@code FallingBlock.isFree} to a bare {@code isAir()} (the
     * second column never leaves the ground), and removing the {@code tickView.scheduleTick} from
     * {@code updateShape} (phase two never lifts off and runs into the tick budget).
     */
    public static void levitatingSandWaitsUnderTheCeilingUntilTheWayUpIsFree(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos lid = new BlockPos(1, 2, 1);
        BlockPos tell = start.east();
        BlockPos roof = new BlockPos(1, 6, 1);
        BlockPos landing = roof.below();

        BlockPos porousStart = new BlockPos(5, 1, 1);
        BlockPos porousLid = porousStart.above();
        BlockPos porousRoof = new BlockPos(5, 4, 1);
        BlockPos porousLanding = porousRoof.below();

        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(roof, Blocks.STONE);
        helper.setBlock(lid, Blocks.STONE);
        helper.setBlock(start, ModBlocks.LEVITATING_SAND);

        // The second column: a lid that is not air but that vanilla still calls free.
        helper.setBlock(new BlockPos(5, 0, 1), Blocks.STONE);
        helper.setBlock(porousRoof, Blocks.STONE);
        helper.setBlock(porousLid, Blocks.STRUCTURE_VOID);
        helper.setBlock(porousStart, ModBlocks.LEVITATING_SAND);

        // Hangs on the levitating block itself: FACING=EAST means the cell to its west carries it.
        helper.setBlock(tell, Blocks.WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(helper.getBlockState(tell).is(Blocks.WALL_TORCH),
                            "the torch that hangs on the levitating block is gone, so that cell was "
                                    + "empty at least once in these 20 ticks - the block must not "
                                    + "leave at all while something solid sits directly above it");
                    helper.assertBlockPresent(ModBlocks.LEVITATING_SAND, start);
                    List<LevitatingBlockEntity> flying = risingEntities(helper);
                    helper.assertTrue(flying.isEmpty(),
                            "a levitating block with a solid block directly above it must not lift "
                                    + "off, but " + flying.size() + " entity/entities exist");

                    // The second column, and first the two halves that make it mean anything: the
                    // lid has to be a state that answers isFree and isAir DIFFERENTLY, or it cannot
                    // separate the guard the mod uses from the narrower one it could be shrunk to.
                    BlockState porous = helper.getBlockState(porousLid);
                    helper.assertFalse(porous.isAir(),
                            "the second column's lid counts as air, so it can no longer tell "
                                    + "FallingBlock.isFree apart from a bare isAir() check");
                    helper.assertTrue(FallingBlock.isFree(porous),
                            "vanilla no longer counts the second column's lid as free, so that "
                                    + "column is not expected to rise and proves nothing");
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, porousStart);
                    helper.assertBlockPresent(ModBlocks.LEVITATING_SAND, porousLanding);

                    // Take the lid away; nothing but updateShape can notice.
                    helper.setBlock(lid, Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertBlockPresent(ModBlocks.LEVITATING_SAND, landing))
                .thenExecute(() -> {
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, start);
                    helper.assertTrue(risingEntities(helper).isEmpty(),
                            "the entity should be gone once it turned back into a block");
                    // The rig checks itself: the same torch that had to survive phase one has to be
                    // gone now that the block really did leave.
                    helper.assertFalse(helper.getBlockState(tell).is(Blocks.WALL_TORCH),
                            "the torch is still hanging where the levitating block used to be, so it "
                                    + "does not pop when that cell empties and phase one's 'the torch "
                                    + "survived' assertion cannot fail either way");
                })
                .thenExecute(() -> helper.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, helper.getBounds())
                        .forEach(ItemEntity::discard))
                .thenSucceed();
    }

    /**
     * The two landing branches that no test entered: the cell the block ends up in cannot hold it,
     * and the cell is a moving piston. Both are driven in the same room, three cells apart.
     *
     * <ul>
     *   <li><b>Cannot hold it.</b> A wall torch sits under the ceiling. It has no collision box, so
     *       the rising block passes into its cell and is stopped by the stone above; but a wall
     *       torch is not replaceable, so {@code mayReplace} is false and the entity has to break
     *       and drop a levitating sand item. Every other test in the suite takes the successful
     *       half of that {@code if}, which means the whole {@code else} - discard, drop, gone -
     *       could be deleted and nothing would notice until a player watched a block disappear.</li>
     *   <li><b>Moving piston.</b> A {@code minecraft:moving_piston} without its block entity has an
     *       empty collision shape and is not replaceable either, so without the explicit
     *       {@code !currentState.is(Blocks.MOVING_PISTON)} guard the entity would take exactly the
     *       branch above and break. With the guard it keeps flying: it bounces under the ceiling
     *       and is still alive at the end. That is the whole point of the guard - a block caught by
     *       a piston mid-flight must not be destroyed by it.</li>
     * </ul>
     *
     * <p>The moving piston is placed with {@code UPDATE_CLIENTS} only, so no neighbour logic runs
     * over a piston block that has no block entity behind it; the test asserts it is still there
     * before it draws any conclusion from the entity.
     *
     * <p><b>The drop is identified where it is born, not where it is found.</b> Counting the items
     * at the end is stable, but asking which column one belongs to by its position is not:
     * {@code Entity#spawnAtLocation} builds the {@code ItemEntity} with the constructor that gives
     * it a random horizontal speed of up to 0.1 blocks per tick, damped by 0.98 a tick while it is
     * in the air, which adds up to more than a block of drift over the fifteen odd ticks between
     * the break under the torch and the assertions here - far more than the half block that would
     * keep it over its own column. So the drop is caught on the tick it appears, when it still sits
     * at the centre of the column it came from, and the column is read off that. The two columns
     * are four blocks apart, which no drift of a single tick can bridge.
     *
     * <p>Also pinned here, as a marker for defect 5: neither levitating block has the
     * {@code WATERLOGGED} property, which is what makes the fluid handling in {@code rise()} and in
     * the landing branch unreachable. If that ever changes, "sand rises and leaves water behind"
     * becomes a real claim and needs a real test.
     *
     * <p>What breaks this test: deleting the {@code else} branch or its
     * {@code spawnAtLocation}, dropping the {@code MOVING_PISTON} check, and inverting
     * {@code mayReplace}.
     */
    public static void blockedLandingSpotsDropTheBlockOrKeepItFlying(GameTestHelper helper) {
        BlockPos unlandableStart = new BlockPos(1, 1, 1);
        BlockPos torchCell = new BlockPos(1, 4, 1);
        BlockPos pistonStart = new BlockPos(5, 1, 1);
        BlockPos movingPistonCell = new BlockPos(5, 4, 1);

        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 5, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 5, 1), Blocks.STONE);

        // The torch hangs on a stone block to its west, so nothing blocks the column itself.
        helper.setBlock(new BlockPos(0, 4, 1), Blocks.STONE);
        helper.setBlock(torchCell, Blocks.WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));

        helper.getLevel().setBlock(helper.absolutePos(movingPistonCell),
                Blocks.MOVING_PISTON.defaultBlockState()
                        .setValue(MovingPistonBlock.FACING, Direction.UP),
                Block.UPDATE_CLIENTS);

        helper.setBlock(unlandableStart, ModBlocks.LEVITATING_SAND);
        helper.setBlock(pistonStart, ModBlocks.LEVITATING_SAND);

        Item levitatingSand = ModBlocks.LEVITATING_SAND.asItem();

        // The x the first dropped item is seen at. An entity spawned during the entity tick phase
        // is not part of that same pass, and every game test assertion runs after the level tick,
        // so the first sighting is the item still standing at the centre of its column.
        double[] dropBornAt = {Double.NaN};
        helper.onEachTick(() -> {
            if (Double.isNaN(dropBornAt[0])) {
                List<ItemEntity> born = levitatingSandDrops(helper, levitatingSand);
                if (!born.isEmpty()) {
                    dropBornAt[0] = helper.relativeVec(born.get(0).position()).x;
                }
            }
        });

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    // The marker for defect 5, checked while the room is still intact.
                    helper.assertFalse(
                            ModBlocks.LEVITATING_SAND.defaultBlockState()
                                    .hasProperty(BlockStateProperties.WATERLOGGED),
                            "levitating sand can be waterlogged now, so the fluid handling in "
                                    + "LevitatingBlockEntity#rise and in its landing branch is finally "
                                    + "reachable and needs a test of its own");
                    helper.assertFalse(
                            ModBlocks.LEVITATING_GRAVEL.defaultBlockState()
                                    .hasProperty(BlockStateProperties.WATERLOGGED),
                            "levitating gravel can be waterlogged now - same as above");
                })
                .thenExecuteAfter(30, () -> {
                    // The rig has to still be standing, or nothing below means anything.
                    helper.assertBlockPresent(Blocks.WALL_TORCH, torchCell);
                    helper.assertBlockPresent(Blocks.MOVING_PISTON, movingPistonCell);

                    // Torch column: broken and dropped, not placed.
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, torchCell);
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, torchCell.below());
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, unlandableStart);

                    // Moving piston column: not placed, still in the air.
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, movingPistonCell);
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, movingPistonCell.below());

                    // Exactly one drop, and it belongs to the torch column. Counting instead of
                    // asking within a radius: the dropped item is already most of the way to the
                    // floor by now, so any radius around the landing cell would be a guess.
                    List<ItemEntity> drops = levitatingSandDrops(helper, levitatingSand);
                    helper.assertValueEqual(drops.size(), 1,
                            "exactly one levitating sand item should have dropped: the block that could "
                                    + "not replace the wall torch. The moving piston column must not have "
                                    + "dropped anything");

                    // With exactly one drop in the room, the first one seen is that one.
                    double bornInColumn = unlandableStart.getX() + 0.5D;
                    helper.assertFalse(Double.isNaN(dropBornAt[0]),
                            "one levitating sand item is lying in the room but none was ever seen "
                                    + "appearing, so the tick by tick watch above stopped working");
                    helper.assertTrue(Math.abs(dropBornAt[0] - bornInColumn) < 0.5D,
                            "the dropped item should have appeared in the wall torch column, at x="
                                    + bornInColumn + ", but it appeared at x=" + dropBornAt[0]
                                    + "; the moving piston column is at x="
                                    + (movingPistonCell.getX() + 0.5D));

                    List<LevitatingBlockEntity> flying = risingEntities(helper);
                    helper.assertValueEqual(flying.size(), 1,
                            "exactly one rising entity should be left: the one over the moving piston. "
                                    + "The one under the wall torch has to be gone, and the moving piston "
                                    + "one must not have landed");
                    // Compared in ABSOLUTE space on purpose. GameTestHelper#relativePos is not the
                    // inverse of #absolutePos: it builds the counter rotation as
                    // rotation.getRotated(CLOCKWISE_180), which is only correct for NONE and 180
                    // degrees - and for NONE it applies a 180 degree turn instead of nothing, so a
                    // relative x of 5 comes back as -5. Converting the expectation outwards avoids
                    // the asymmetry and holds under any rotation the catalogue may ask for.
                    BlockPos where = flying.get(0).blockPosition();
                    helper.assertValueEqual(where.getX(), helper.absolutePos(movingPistonCell).getX(),
                            "the surviving entity should be the one in the moving piston column");
                })
                .thenExecute(() -> {
                    // The survivor would otherwise keep bouncing under the ceiling for the rest of
                    // the run, and the drop would still be lying there when the room is reused.
                    risingEntities(helper).forEach(LevitatingBlockEntity::discard);
                    levitatingSandDrops(helper, levitatingSand).forEach(ItemEntity::discard);
                })
                .thenSucceed();
    }

    // =====================================================================================
    // SUSPENDED BLOCKS: THE ONE DIFFERENCE BETWEEN THE TWO
    // =====================================================================================

    /**
     * Suspended sand is registered with {@code noCollision()} and suspended gravel is not, and that
     * single call is the only thing separating the two blocks. {@code BlockBehaviourTests} proves
     * both of them stay in the air; neither it nor anything else ever touches anything.
     *
     * <p>The shapes are asserted and then driven: an item is dropped down each column. Over the
     * sand it has to fall straight through and land on the floor; over the gravel it has to come to
     * rest on top. Without the behavioural half the test would only be reading the same field back
     * out of the registry, and would still pass if the shape were correct but never consulted.
     *
     * <p>What breaks this test: dropping {@code noCollision()} from suspended sand, or adding it to
     * suspended gravel.
     */
    public static void suspendedSandLetsItemsThroughWhileSuspendedGravelHoldsThem(GameTestHelper helper) {
        BlockPos sand = new BlockPos(1, 3, 1);
        BlockPos gravel = new BlockPos(5, 3, 1);

        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 0, 1), Blocks.STONE);
        helper.setBlock(sand, ModBlocks.SUSPENDED_SAND);
        helper.setBlock(gravel, ModBlocks.SUSPENDED_GRAVEL);

        ServerLevel level = helper.getLevel();
        BlockState sandState = helper.getBlockState(sand);
        BlockState gravelState = helper.getBlockState(gravel);
        helper.assertTrue(
                sandState.getCollisionShape(level, helper.absolutePos(sand), CollisionContext.empty()).isEmpty(),
                "suspended sand should have no collision box at all");
        helper.assertFalse(
                gravelState.getCollisionShape(level, helper.absolutePos(gravel), CollisionContext.empty()).isEmpty(),
                "suspended gravel should keep its collision box - it is the one block of the pair that "
                        + "you can still stand on");

        ItemEntity throughSand = helper.spawnItem(Items.STONE, new Vec3(1.5D, 6.0D, 1.5D));
        ItemEntity ontoGravel = helper.spawnItem(Items.STONE, new Vec3(5.5D, 6.0D, 1.5D));

        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    double throughY = helper.relativeVec(throughSand.position()).y;
                    double ontoY = helper.relativeVec(ontoGravel.position()).y;
                    helper.assertTrue(throughY < 2.0D,
                            "the item should have fallen through the suspended sand at y=3 down onto the "
                                    + "floor, but it rests at y=" + throughY);
                    helper.assertTrue(ontoY > 3.5D,
                            "the item should be lying on top of the suspended gravel at y=3, but it is "
                                    + "at y=" + ontoY);
                })
                .thenExecute(() -> {
                    throughSand.discard();
                    ontoGravel.discard();
                })
                .thenSucceed();
    }

    // =====================================================================================
    // REGISTRY DATA
    // =====================================================================================

    /**
     * The registration lines behind the pistons: the mining tag they are listed under, the strength
     * they are built with, and the fire resistance of the netherite piston's item. All three are
     * generated or declared data that a port can drop without any behaviour test noticing.
     *
     * <p>The tag half is a real datapack lookup, so it also proves the generated
     * {@code mineable/pickaxe} JSON is inside the jar and loaded. Levitating sand is the negative
     * control: the lookup has to be able to say no, and sand has no business being pickaxe
     * mineable.
     *
     * <p>All four gravity blocks are then read out of both tool tags, which is the marker for
     * defect 6 - the shovel bonus their vanilla originals have is gone, and nothing but this pair
     * of lines would notice it coming back or the pickaxe list growing a gravel entry. Vanilla sand
     * and gravel are asserted to <em>be</em> shovel mineable first, because a
     * {@code mineable/shovel} lookup that answered no to everything would otherwise make the four
     * negatives below pass without meaning anything.
     *
     * <p>The strength half reads the same {@code getDestroySpeed} the breaker test compares its
     * threshold against, and the explosion resistance next to it. A vanilla piston is measured in
     * the same breath, so the netherite piston's 1200 is stated as a difference from vanilla rather
     * than as a number copied out of {@code ModBlocks}.
     *
     * <p>The fire resistance half compares components rather than asserting a shape: the netherite
     * piston's item must carry the very same {@code DAMAGE_RESISTANT} value a netherite ingot does,
     * and the reinforced piston must carry none.
     *
     * <p>What breaks this test: removing a block from the tag provider or failing to regenerate the
     * data, adding one of the four gravity blocks to {@code mineable/pickaxe} or to a
     * {@code mineable/shovel} tag, changing either {@code strength(...)} call, and dropping
     * {@code fireResistant()} from the netherite piston item.
     */
    public static void gravityBlocksAndPistonsCarryTheirRegisteredStrengthAndTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos probe = new BlockPos(1, 1, 1);
        BlockPos absoluteProbe = helper.absolutePos(probe);

        // --- mining tag ---
        assertPickaxeMineable(helper, ModBlocks.REINFORCED_PISTON, true);
        assertPickaxeMineable(helper, ModBlocks.NETHERITE_PISTON, true);
        assertPickaxeMineable(helper, ModBlocks.NETHERITE_PISTON_HEAD, true);
        assertPickaxeMineable(helper, ModBlocks.LEVITATING_SAND, false);
        assertPickaxeMineable(helper, ModBlocks.SUSPENDED_SAND, false);
        assertPickaxeMineable(helper, ModBlocks.LEVITATING_GRAVEL, false);
        assertPickaxeMineable(helper, ModBlocks.SUSPENDED_GRAVEL, false);

        // --- the shovel tag the four gravity blocks are missing from; see defect 6 ---
        // The two vanilla lines are the proof that the lookup can say yes at all.
        assertShovelMineable(helper, Blocks.SAND, true);
        assertShovelMineable(helper, Blocks.GRAVEL, true);
        assertShovelMineable(helper, ModBlocks.LEVITATING_SAND, false);
        assertShovelMineable(helper, ModBlocks.SUSPENDED_SAND, false);
        assertShovelMineable(helper, ModBlocks.LEVITATING_GRAVEL, false);
        assertShovelMineable(helper, ModBlocks.SUSPENDED_GRAVEL, false);

        // --- strength, measured through the state, against vanilla's piston ---
        float vanillaSpeed = Blocks.PISTON.defaultBlockState().getDestroySpeed(level, absoluteProbe);
        float reinforcedSpeed = ModBlocks.REINFORCED_PISTON.defaultBlockState()
                .getDestroySpeed(level, absoluteProbe);
        float netheriteSpeed = ModBlocks.NETHERITE_PISTON.defaultBlockState()
                .getDestroySpeed(level, absoluteProbe);

        helper.assertValueEqual(reinforcedSpeed, vanillaSpeed,
                "the reinforced piston's hardness, against a vanilla piston's - both are 1.5");
        helper.assertValueEqual(netheriteSpeed, 5.0F, "the netherite piston's hardness");
        helper.assertTrue(netheriteSpeed > reinforcedSpeed,
                "the netherite piston should be harder to mine than the reinforced one, "
                        + netheriteSpeed + " against " + reinforcedSpeed);

        helper.assertValueEqual(ModBlocks.NETHERITE_PISTON.getExplosionResistance(), 1200.0F,
                "the netherite piston's blast resistance");
        helper.assertValueEqual(ModBlocks.REINFORCED_PISTON.getExplosionResistance(),
                Blocks.PISTON.getExplosionResistance(),
                "the reinforced piston's blast resistance, against a vanilla piston's");
        helper.assertTrue(
                ModBlocks.NETHERITE_PISTON.getExplosionResistance() > Blocks.PISTON.getExplosionResistance() * 100.0F,
                "the netherite piston is supposed to be the blast proof one, but its resistance of "
                        + ModBlocks.NETHERITE_PISTON.getExplosionResistance() + " is no better than "
                        + "vanilla's " + Blocks.PISTON.getExplosionResistance());

        // --- fire resistance of the item ---
        DamageResistant netheritePiston = new ItemStack(ModItems.NETHERITE_PISTON).get(DataComponents.DAMAGE_RESISTANT);
        DamageResistant netheriteIngot = new ItemStack(Items.NETHERITE_INGOT).get(DataComponents.DAMAGE_RESISTANT);
        helper.assertTrue(netheriteIngot != null,
                "a netherite ingot no longer carries a DAMAGE_RESISTANT component, so this test has "
                        + "lost its yardstick");
        helper.assertTrue(netheritePiston != null,
                "the netherite piston item carries no DAMAGE_RESISTANT component at all, so "
                        + "fireResistant() is gone from its registration and a dropped one burns");
        // Compared by tag key rather than by component identity: the two HolderSets behind the
        // component are Named sets, and those do not implement equals.
        helper.assertValueEqual(netheritePiston.types().unwrapKey(), netheriteIngot.types().unwrapKey(),
                "the damage types the netherite piston resists, against a netherite ingot's");
        // And driven, so the assertion is about behaviour and not only about a stored tag name.
        helper.assertTrue(netheritePiston.isResistantTo(level.damageSources().lava()),
                "a dropped netherite piston should survive lava");
        helper.assertFalse(netheritePiston.isResistantTo(level.damageSources().drown()),
                "the netherite piston resists drowning as well, so its resistance is not the fire "
                        + "tag any more and the lava assertion above proves nothing");
        helper.assertTrue(
                new ItemStack(ModItems.REINFORCED_PISTON).get(DataComponents.DAMAGE_RESISTANT) == null,
                "the reinforced piston item is fire resistant too, which makes the assertions above "
                        + "meaningless - it is the netherite tier that is supposed to survive lava");

        helper.succeed();
    }

    /**
     * The yields, which {@code DataIntegrityTests#modRecipesOnlyReferenceRegisteredItems} does not
     * look at: it resolves every ingredient and result of every mod recipe but never reads a
     * pattern or a count. A recipe that produced one reinforced piston instead of two, or eight
     * suspended sand from a pattern that no longer needs the shard in the middle, would pass there.
     *
     * <p>Every recipe here is driven through the live {@code RecipeManager} with a real crafting
     * grid, so the assertion covers the pattern, the ingredients, the result and the count in one
     * go. The reinforced piston recipe is additionally offered turned by 180 degrees: same nine
     * items, and neither the pattern nor the x mirror vanilla accepts alongside it, so a match
     * there would mean the shape is not being checked at all.
     *
     * <p><b>Every recipe gets a rearranged grid as well</b>, because driving only the documented
     * layout says nothing about the recipe still being shaped: switching a JSON to
     * {@code crafting_shapeless} keeps the id, the result and the count, and the documented grid
     * goes on matching. What the counter grid has to avoid is the x mirror, which
     * {@code ShapedRecipePattern#matches} accepts alongside the pattern itself - so the netherite
     * piston's nugget moves to the <em>bottom</em> left rather than to the top right. The coating
     * ring is invariant under every rotation and mirror there is, so for those four the only grid
     * that can separate shaped from shapeless is the one with the material out of the middle.
     *
     * <p><b>And one grid of red sand</b>, which is what a widened ingredient looks like: turning
     * {@code "B": "minecraft:sand"} into {@code "#minecraft:sand"} lets red sand craft suspended
     * and levitating sand, and every assertion above stays green because none of them ever offers
     * an ingredient the recipe is supposed to refuse.
     *
     * <p>What breaks this test: any edit to the six generated recipe JSONs - a different pattern, a
     * swapped or widened ingredient, another count, another recipe type - and any of them failing
     * to load.
     */
    public static void gravityBlockRecipesCraftFromTheirDocumentedPatterns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // "DDD" / "PIP" / "III"
        CraftingInput reinforced = grid3x3(
                ModItems.CRACKED_DIAMOND, ModItems.CRACKED_DIAMOND, ModItems.CRACKED_DIAMOND,
                Items.PISTON, Items.IRON_INGOT, Items.PISTON,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT);
        assertCrafts(helper, level, reinforced, "simplebuilding:reinforced_piston",
                ModItems.REINFORCED_PISTON, 2);

        // The same nine items, turned by 180 degrees.
        CraftingInput upsideDown = grid3x3(
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.PISTON, Items.IRON_INGOT, Items.PISTON,
                ModItems.CRACKED_DIAMOND, ModItems.CRACKED_DIAMOND, ModItems.CRACKED_DIAMOND);
        helper.assertTrue(
                level.getServer().getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, upsideDown, level).isEmpty(),
                "the reinforced piston pattern turned upside down crafts something as well, so the "
                        + "recipe is not shaped the way the data says it is");

        // "NR" / "RR"
        CraftingInput bulk = CraftingInput.of(2, 2, List.of(
                new ItemStack(ModItems.NETHERITE_NUGGET), new ItemStack(ModItems.REINFORCED_PISTON),
                new ItemStack(ModItems.REINFORCED_PISTON), new ItemStack(ModItems.REINFORCED_PISTON)));
        assertCrafts(helper, level, bulk, "simplebuilding:netherite_piston_bulk",
                ModItems.NETHERITE_PISTON, 3);

        // The same four items with the nugget in the BOTTOM left. "RN"/"RR" would still match,
        // because a shaped recipe is tried against its x mirror too; this one is neither the
        // pattern nor its mirror, and only a shapeless copy of the recipe would take it.
        CraftingInput bulkShuffled = CraftingInput.of(2, 2, List.of(
                new ItemStack(ModItems.REINFORCED_PISTON), new ItemStack(ModItems.REINFORCED_PISTON),
                new ItemStack(ModItems.NETHERITE_NUGGET), new ItemStack(ModItems.REINFORCED_PISTON)));
        assertMatchesNothing(helper, level, bulkShuffled,
                "the netherite piston pattern with the nugget moved to the bottom left crafts "
                        + "something too, so netherite_piston_bulk is not the shaped NR/RR the "
                        + "data declares");

        // "BBB" / "BMB" / "BBB", eight of the coated block per craft.
        assertCoating(helper, level, Items.SAND, ModItems.NIHILITH_SHARD,
                "simplebuilding:suspended_sand", ModBlocks.SUSPENDED_SAND.asItem());
        assertCoating(helper, level, Items.GRAVEL, ModItems.NIHILITH_SHARD,
                "simplebuilding:suspended_gravel", ModBlocks.SUSPENDED_GRAVEL.asItem());
        assertCoating(helper, level, Items.SAND, ModItems.ASTRALIT_DUST,
                "simplebuilding:levitating_sand", ModBlocks.LEVITATING_SAND.asItem());
        assertCoating(helper, level, Items.GRAVEL, ModItems.ASTRALIT_DUST,
                "simplebuilding:levitating_gravel", ModBlocks.LEVITATING_GRAVEL.asItem());

        // Red sand stands in for the whole #minecraft:sand tag. It is the one ingredient that
        // separates "the key names the block" from "the key names the tag the block is in", and
        // nothing above can see the difference because nothing above offers a wrong ingredient.
        assertMatchesNothing(helper, level, coatingGrid(Items.RED_SAND, ModItems.NIHILITH_SHARD),
                "red sand crafts suspended sand, so the recipe's B key has been widened from "
                        + "minecraft:sand to a tag");
        assertMatchesNothing(helper, level, coatingGrid(Items.RED_SAND, ModItems.ASTRALIT_DUST),
                "red sand crafts levitating sand, so the recipe's B key has been widened from "
                        + "minecraft:sand to a tag");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS: PISTONS
    // =====================================================================================

    private static BlockState upright(Block piston) {
        return facing(piston, Direction.UP);
    }

    private static BlockState facing(Block piston, Direction direction) {
        return piston.defaultBlockState().setValue(DirectionalBlock.FACING, direction);
    }

    /**
     * An upward facing piston with a solid column of the given height on top of it. The travel path
     * is cleared first: it reaches well above the 8 block room, and whatever the runner put there -
     * a barrier cage, or nothing at all - must not decide the outcome.
     */
    private static void buildColumnOnPiston(GameTestHelper helper, BlockPos pistonPos, Block piston, int height) {
        for (int y = 1; y <= height + 3; y++) {
            helper.setBlock(pistonPos.above(y), Blocks.AIR);
        }
        helper.setBlock(pistonPos, upright(piston));
        for (int y = 1; y <= height; y++) {
            helper.setBlock(pistonPos.above(y), Blocks.STONE);
        }
    }

    /** An upward facing netherite piston with one block to work on directly above it. */
    private static void placeBreakerWithTarget(GameTestHelper helper, BlockPos pistonPos, Block target) {
        helper.setBlock(pistonPos, upright(ModBlocks.NETHERITE_PISTON));
        helper.setBlock(pistonPos.above(), target);
        helper.setBlock(pistonPos.above(2), Blocks.AIR);
    }

    /**
     * A redstone block and two dust steps west of the piston, which leaves exactly 14 at the
     * piston: the dust next to the source carries 15, the one next to the piston 14. The dust has
     * no north/south neighbour, so vanilla treats it as a straight line and it powers the block at
     * the end of that line.
     */
    private static void buildWeakSignalLine(GameTestHelper helper, BlockPos pistonPos) {
        helper.setBlock(pistonPos.west(3), Blocks.REDSTONE_BLOCK);
        helper.setBlock(pistonPos.west(2), Blocks.REDSTONE_WIRE);
        helper.setBlock(pistonPos.west(), Blocks.REDSTONE_WIRE);
    }

    /**
     * A signal of exactly 1 at the piston: a comparator in compare mode reading a composter that
     * is filled one level, since {@code ComposterBlock#getAnalogOutputSignal} hands out the fill
     * level unchanged.
     *
     * <p>A dust line would need fifteen steps to walk 15 down to 1, which does not fit into this
     * room next to the other four rigs without running dust past a piston that must stay unpowered.
     * Two blocks do.
     *
     * <p>{@code FACING} on a comparator points at its <em>input</em> - {@code DiodeBlock} reads
     * {@code pos.relative(FACING)} and answers {@code getSignal} only for that same direction, and
     * a west neighbour is asked for its signal with {@code WEST} - so a comparator west of the
     * piston with the composter behind it faces west.
     *
     * <p>The comparator goes down first and the composter second on purpose: a comparator does not
     * schedule its own tick when it is placed, it only reacts to a neighbour changing, so the
     * composter has to be the later of the two or the comparator never turns on.
     */
    private static void buildFaintSignalLine(GameTestHelper helper, BlockPos pistonPos) {
        helper.setBlock(pistonPos.west(), Blocks.COMPARATOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        helper.setBlock(pistonPos.west(2), Blocks.COMPOSTER.defaultBlockState()
                .setValue(BlockStateProperties.LEVEL_COMPOSTER, 1));
    }

    /**
     * Reads the signal with {@code Level#getBestNeighborSignal}, which is the call
     * {@code NetheriteBreakerPistonBlock} makes. A failure here is a broken test rig, not a broken
     * mod, and the message says so.
     */
    private static void assertSignalStrength(GameTestHelper helper, BlockPos pistonPos, int expected) {
        int actual = helper.getLevel().getBestNeighborSignal(helper.absolutePos(pistonPos));
        helper.assertValueEqual(actual, expected,
                "the redstone rig at " + pistonPos + " should deliver this signal strength; if it "
                        + "does not, the wiring in this test broke, not the piston");
    }

    private static void fillFloor(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    // =====================================================================================
    // HELPERS: LEVITATING ENTITIES
    // =====================================================================================

    /**
     * The rising entities inside THIS structure. The bounds are deliberately not inflated: test
     * structures stand a few blocks apart, and any margin would also collect the entities of the
     * neighbouring test and make the result depend on the order the tests happen to run in.
     */
    private static List<LevitatingBlockEntity> risingEntities(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(LevitatingBlockEntity.class, helper.getBounds());
    }

    /** The dropped levitating sand items inside THIS structure; the bounds stay uninflated too. */
    private static List<ItemEntity> levitatingSandDrops(GameTestHelper helper, Item levitatingSand) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(),
                item -> item.getItem().is(levitatingSand));
    }

    private static LevitatingBlockEntity riser(GameTestHelper helper) {
        List<LevitatingBlockEntity> found = risingEntities(helper);
        helper.assertValueEqual(found.size(), 1, "rising entities in the room");
        return found.get(0);
    }

    /** The vanilla falling sand in the room - every {@link LevitatingBlockEntity} filtered out. */
    private static FallingBlockEntity faller(GameTestHelper helper) {
        List<FallingBlockEntity> found = helper.getLevel()
                .getEntitiesOfClass(FallingBlockEntity.class, helper.getBounds()).stream()
                .filter(entity -> !(entity instanceof LevitatingBlockEntity))
                .toList();
        helper.assertValueEqual(found.size(), 1, "vanilla falling block entities in the room");
        return found.get(0);
    }

    private static void assertMirrors(GameTestHelper helper, double rise, double fall, String which) {
        helper.assertTrue(Math.abs(rise + fall) < VELOCITY_TOLERANCE,
                which + ": the rise should be the exact mirror of vanilla sand falling in the same "
                        + "ticks, but the rising block moves at " + rise + " while the falling one "
                        + "moves at " + fall);
    }

    // =====================================================================================
    // HELPERS: TAGS AND RECIPES
    // =====================================================================================

    private static void assertPickaxeMineable(GameTestHelper helper, Block block, boolean expected) {
        boolean actual = block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE);
        helper.assertValueEqual(actual, expected,
                block.getName().getString() + " in minecraft:mineable/pickaxe");
    }

    private static void assertShovelMineable(GameTestHelper helper, Block block, boolean expected) {
        boolean actual = block.defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL);
        helper.assertValueEqual(actual, expected,
                block.getName().getString() + " in minecraft:mineable/shovel");
    }

    /** A 3x3 crafting grid, row by row; {@code null} stands for an empty slot. */
    private static CraftingInput grid3x3(Item... items) {
        return CraftingInput.of(3, 3, List.of(
                stack(items[0]), stack(items[1]), stack(items[2]),
                stack(items[3]), stack(items[4]), stack(items[5]),
                stack(items[6]), stack(items[7]), stack(items[8])));
    }

    private static ItemStack stack(Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The documented {@code BBB / BMB / BBB} ring: eight of the base around one of the material. */
    private static CraftingInput coatingGrid(Item base, Item material) {
        return grid3x3(
                base, base, base,
                base, material, base,
                base, base, base);
    }

    private static void assertCoating(GameTestHelper helper, ServerLevel level, Item base, Item material,
                                      String recipeId, Item expected) {
        assertCrafts(helper, level, coatingGrid(base, material), recipeId, expected, 8);

        // The ring survives every rotation and every mirror unchanged, so a turned grid could never
        // tell this recipe apart from a shapeless one with the same nine items. Moving the material
        // out of the middle can: a shaped recipe refuses it, a shapeless one does not.
        CraftingInput cornered = grid3x3(
                material, base, base,
                base, base, base,
                base, base, base);
        assertMatchesNothing(helper, level, cornered,
                recipeId + " crafts with the material in a corner as well, so it is not the shaped "
                        + "BBB/BMB/BBB the data declares");
    }

    /**
     * The counterpart to {@link #assertCrafts}: this grid must reach no recipe at all. Used for the
     * rearranged patterns and for the ingredient a recipe is supposed to refuse.
     */
    private static void assertMatchesNothing(GameTestHelper helper, ServerLevel level,
                                             CraftingInput grid, String message) {
        helper.assertTrue(
                level.getServer().getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(),
                message);
    }

    private static void assertCrafts(GameTestHelper helper, ServerLevel level, CraftingInput grid,
                                     String recipeId, Item expected, int count) {
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(),
                "the documented pattern for " + recipeId + " matches no crafting recipe at all, so "
                        + "the block cannot be crafted in game");
        RecipeHolder<CraftingRecipe> holder = match.get();
        helper.assertValueEqual(holder.id().identifier().toString(), recipeId,
                "recipe matched by the documented pattern");

        ItemStack result = holder.value().assemble(grid);
        helper.assertTrue(result.is(expected),
                recipeId + " produced " + result + " instead of the expected item");
        helper.assertValueEqual(result.getCount(), count, recipeId + ": items produced per craft");
    }
}
