package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModSounds;
import com.simplebuilding.util.PistonBoreEffects;
import com.simplebuilding.util.PistonBreach;
import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.items.custom.NetheritePistonItem;
import com.simplebuilding.networking.PistonConfigPayload;
import com.simplebuilding.platform.PistonEventProbe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The pistons that deal with "unbreakable" blocks ({@code PistonBreach}): the reinforced piston and
 * its sticky twin push exactly one of them, the netherite piston sacrifices itself to delete one, the
 * enderite piston breaches three cells deep. All three are paid for by a redstone block directly
 * beside the piston, and all three have to leave vanilla pistons and everything the immune tag lists
 * alone.
 *
 * <p>{@code GravityBlockTests} pins the older piston numbers (the push limit of 18, the breaking
 * threshold) and since 2026-09 the bedrock row of the netherite breaker; this class pins the breach
 * itself, row by row, each row with the control that tells it apart from a rig that simply did
 * nothing. The rows share one convention: an upright piston at {@code y = 1}, the target directly
 * above it, and the paying redstone block directly <em>below</em> it - "behind" for an upward piston,
 * which is the cell {@code PistonBreach#findFuel} asks first. Rows stand two blocks apart, so no power
 * source and no quasi-connectivity cell of one row touches another.
 *
 * <p>The redstone block is placed last in every row: its neighbour update is what makes the piston
 * look at its front, and the target has to be there by then.
 *
 * <h2>Known defects</h2>
 *
 * <p><b>1. The block-entity rule in {@code PistonBreach#isBreachable} is dead in vanilla.</b> Every
 * vanilla block with a destroy speed below 0 that carries a block entity (command blocks, structure
 * and jigsaw blocks, test blocks, the moving piston, the portals) is also listed in
 * {@code simplebuilding:piston_breach_immune}, so deleting the {@code hasBlockEntity()} check changes
 * nothing a vanilla world can show. It only guards modded unbreakables with a block entity. Pinned
 * by {@link #immuneBlocksNeverMoveOrBreak} as an outcome (the command block survives), not as proof
 * of that line.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The fuel guard against slime structures</b> ({@code PistonHandlerMixin#keepTheFuelInPlace}).
 *       It needs a slime rig that drags the redstone block behind the piston into the push list; the
 *       rows here never build one.</li>
 *   <li><b>World limits</b> (bedrock at {@code getMinY()} / {@code getMaxY()}). They need absolute
 *       coordinates far outside the test room.</li>
 *   <li><b>The client side</b> of the breach: the block event the client replays arms its own
 *       resolver from its own world. Only a client test can see that.</li>
 * </ul>
 */
public final class PistonBreachTests {

    /** Tick budget for {@link #reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays}. */
    public static final int PAID_PUSH_MAX_TICKS = 60;

    /** Tick budget for {@link #reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock}. */
    public static final int LIMIT_MAX_TICKS = 60;

    /** Tick budget for {@link #stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack}. */
    public static final int STICKY_MAX_TICKS = 100;

    /** Tick budget for {@link #netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock}. */
    public static final int SACRIFICE_MAX_TICKS = 60;

    /** Tick budget for {@link #enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks}. */
    public static final int ENDERITE_MAX_TICKS = 60;

    /** Tick budget for {@link #breakingTheHeadOfModPistonsBreaksThePistonToo}. */
    public static final int HEAD_MAX_TICKS = 80;

    /** Tick budget for {@link #everyModPistonCarriesTheHeadOfItsTierAndTakesItBack}. */
    public static final int OWN_HEAD_MAX_TICKS = 80;

    /** Tick budget for {@link #modPistonBreaksReportEveryDestroyedBlockForParticlesAndSound}. */
    public static final int BORE_EFFECTS_MAX_TICKS = 60;

    /** Tick budget for {@link #immuneBlocksNeverMoveOrBreak}. */
    public static final int IMMUNE_MAX_TICKS = 60;

    /** Tick budget for {@link #explosionsCannotDeleteUnbreakableBlocksWhileTheyMove}. */
    public static final int EXPLOSION_MAX_TICKS = 60;

    /** The reinforced piston's push limit, {@code PistonHandlerMixin#modifyPistonLimit}. */
    private static final int REINFORCED_LIMIT = 18;

    /** The explosion of {@link #explosionsCannotDeleteUnbreakableBlocksWhileTheyMove}: small, inside the room. */
    private static final float BLAST_POWER = 2.0F;

    private PistonBreachTests() {
    }

    // =====================================================================================
    // REINFORCED PISTON: ONE UNBREAKABLE, PAID FOR
    // =====================================================================================

    /**
     * The reinforced piston pushes the one unbreakable block directly in front of it, but only when a
     * redstone block beside it pays - and it pays only for that.
     *
     * <p>Eight rows:
     * <ul>
     *   <li><b>Bedrock and reinforced deepslate</b> on a reinforced piston with a
     *       redstone block behind it: each block has moved one cell, the redstone block is gone, and
     *       because that redstone block was the only power the piston retracted in the same tick - no
     *       head is left. Bedrock is unbreakable by hardness, reinforced deepslate only
     *       through {@code simplebuilding:piston_breachable_extra}, so the two rows pin both halves
     *       of {@code PistonBreach#isUnbreakableClass}.</li>
     *   <li><b>An end portal frame</b> in the same rig stays where it is and so does the redstone
     *       block: {@code pistonsBreachEndPortalFrames} is off by default (owner decision
     *       2026-09-26).</li>
     *   <li><b>A vanilla piston and a vanilla sticky piston</b> with the same bedrock and redstone
     *       block: nothing moves and the redstone block stays. The breach lives in the resolver of the
     *       mod's own pistons and nowhere else.</li>
     *   <li><b>Stone on a reinforced piston with a redstone block behind it:</b> an ordinary push, so
     *       the stone moves and the redstone block stays. Paying is for the breach only.</li>
     *   <li><b>Bedrock on a reinforced piston powered by a lever:</b> a lever powers the piston but
     *       pays for nothing, so the bedrock stays and the piston does not even extend.</li>
     *   <li><b>Stone on a reinforced piston powered by a lever:</b> the control for the row above -
     *       lever power still works the vanilla way and pushes an ordinary block.</li>
     * </ul>
     *
     * <p>What breaks this test: a breach that does not fire, one that moves the block without using
     * up the redstone block, one that uses up the redstone block on an ordinary push, dropping
     * {@code piston_breachable_extra} from the unbreakable class, a breach that also arms vanilla
     * pistons, and a lever that counts as payment.
     */
    public static void reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays(GameTestHelper helper) {
        BlockPos onBedrock = new BlockPos(1, 1, 1);
        BlockPos onDeepslate = new BlockPos(3, 1, 1);
        BlockPos onFrame = new BlockPos(5, 1, 1);
        BlockPos vanilla = new BlockPos(1, 1, 3);
        BlockPos vanillaSticky = new BlockPos(3, 1, 3);
        BlockPos onStone = new BlockPos(5, 1, 3);
        BlockPos leverOnBedrock = new BlockPos(1, 1, 6);
        BlockPos leverOnStone = new BlockPos(5, 1, 6);

        // The default (off) is pinned by ConfigOptionTests from a fresh config object; the live one
        // is read from the run's config file, which may still say otherwise - so it is set here.
        boolean framesBefore = Simplebuilding.getConfig().pistonsBreachEndPortalFrames;
        Simplebuilding.getConfig().pistonsBreachEndPortalFrames = false;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().pistonsBreachEndPortalFrames = framesBefore);

        paidRow(helper, onBedrock, ModBlocks.REINFORCED_PISTON, Blocks.BEDROCK);
        paidRow(helper, onDeepslate, ModBlocks.REINFORCED_PISTON, Blocks.REINFORCED_DEEPSLATE);
        paidRow(helper, onFrame, ModBlocks.REINFORCED_PISTON, Blocks.END_PORTAL_FRAME);
        paidRow(helper, vanilla, Blocks.PISTON, Blocks.BEDROCK);
        paidRow(helper, vanillaSticky, Blocks.STICKY_PISTON, Blocks.BEDROCK);
        paidRow(helper, onStone, ModBlocks.REINFORCED_PISTON, Blocks.STONE);
        leverRow(helper, leverOnBedrock, ModBlocks.REINFORCED_PISTON, Blocks.BEDROCK, Direction.WEST);
        leverRow(helper, leverOnStone, ModBlocks.REINFORCED_PISTON, Blocks.STONE, Direction.EAST);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // --- bedrock: moved one cell, paid for, and the piston is back down ---
                    helper.assertTrue(helper.getBlockState(onBedrock.above(2)).is(Blocks.BEDROCK),
                            "the reinforced piston did not push the bedrock in front of it although a "
                                    + "redstone block sat directly behind it");
                    helper.assertTrue(!helper.getBlockState(onBedrock.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block that paid for pushing the bedrock is still there - the "
                                    + "breach was free");
                    helper.assertBlockNotPresent(Blocks.BEDROCK, onBedrock.above());
                    helper.assertBlockProperty(onBedrock, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockNotPresent(ModBlocks.REINFORCED_PISTON_HEAD, onBedrock.above());

                    // --- reinforced deepslate: unbreakable only through the extra tag ---
                    helper.assertTrue(helper.getBlockState(onDeepslate.above(2)).is(Blocks.REINFORCED_DEEPSLATE),
                            "the reinforced piston did not push the reinforced deepslate; it counts as "
                                    + "unbreakable only through simplebuilding:piston_breachable_extra");
                    helper.assertTrue(!helper.getBlockState(onDeepslate.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block that paid for the reinforced deepslate is still there");

                    // --- end portal frame: immune while pistonsBreachEndPortalFrames is off (default) ---
                    helper.assertTrue(helper.getBlockState(onFrame.above()).is(Blocks.END_PORTAL_FRAME),
                            "the reinforced piston pushed the end portal frame although "
                                    + "pistonsBreachEndPortalFrames is off");
                    helper.assertTrue(helper.getBlockState(onFrame.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block beside the end portal frame was used up although the frame "
                                    + "may not be breached by default");

                    // --- vanilla pistons: untouched by all of it ---
                    helper.assertTrue(helper.getBlockState(vanilla.above()).is(Blocks.BEDROCK),
                            "a vanilla piston moved bedrock - the breach leaked out of the mod's pistons");
                    helper.assertBlockProperty(vanilla, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertTrue(helper.getBlockState(vanilla.below()).is(Blocks.REDSTONE_BLOCK),
                            "a vanilla piston used up the redstone block behind it");
                    helper.assertTrue(helper.getBlockState(vanillaSticky.above()).is(Blocks.BEDROCK),
                            "a vanilla sticky piston moved bedrock - the breach leaked out of the mod's pistons");
                    helper.assertBlockProperty(vanillaSticky, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertTrue(helper.getBlockState(vanillaSticky.below()).is(Blocks.REDSTONE_BLOCK),
                            "a vanilla sticky piston used up the redstone block behind it");

                    // --- stone: an ordinary push, which costs nothing ---
                    helper.assertTrue(helper.getBlockState(onStone.above(2)).is(Blocks.STONE),
                            "the reinforced piston did not push an ordinary stone block");
                    helper.assertTrue(helper.getBlockState(onStone.below()).is(Blocks.REDSTONE_BLOCK),
                            "an ordinary push used up the redstone block behind the reinforced piston; "
                                    + "only a breach may cost it");
                    helper.assertBlockProperty(onStone, PistonBaseBlock.EXTENDED, Boolean.TRUE);

                    // --- a lever powers, but it does not pay ---
                    helper.assertTrue(helper.getBlockState(leverOnBedrock.above()).is(Blocks.BEDROCK),
                            "a lever alone let the reinforced piston push bedrock; only a redstone block "
                                    + "beside the piston pays for a breach");
                    helper.assertBlockProperty(leverOnBedrock, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertTrue(helper.getBlockState(leverOnStone.above(2)).is(Blocks.STONE),
                            "a lever no longer powers the reinforced piston the vanilla way - it did not "
                                    + "push an ordinary stone");
                    helper.assertBlockProperty(leverOnStone, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The breached block is a block like any other in the push line: it counts towards the
     * reinforced piston's limit of 18, and it has to be the block directly in front.
     *
     * <ul>
     *   <li><b>Bedrock plus 17 stones</b> (18 blocks) moves, and the redstone block is used up.</li>
     *   <li><b>Bedrock plus 18 stones</b> (19 blocks) does not move, and because nothing moved the
     *       redstone block stays - consumption happens only after a push that succeeded.</li>
     *   <li><b>Two bedrock blocks in line</b> do not move either: only the front one may be breached,
     *       the second one is refused by vanilla's own rule, and again nothing is paid.</li>
     *   <li><b>Stone, then bedrock:</b> the unbreakable is not the front block, so there is no breach
     *       at all.</li>
     * </ul>
     *
     * <p>Wants {@code skyAccess(true)}: the tall columns reach 20 blocks above the floor. Their travel
     * path is cleared first.
     *
     * <p>What breaks this test: a push limit other than 18, a breach that does not count the breached
     * block, a breach that reaches past {@code startPos}, and a redstone block that is used up although
     * the push was refused.
     */
    public static void reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock(GameTestHelper helper) {
        BlockPos atLimit = new BlockPos(1, 1, 1);
        BlockPos overLimit = new BlockPos(5, 1, 1);
        BlockPos twoBedrock = new BlockPos(1, 1, 5);
        BlockPos bedrockSecond = new BlockPos(5, 1, 5);

        buildBreachColumn(helper, atLimit, REINFORCED_LIMIT - 1);
        buildBreachColumn(helper, overLimit, REINFORCED_LIMIT);

        helper.setBlock(twoBedrock, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(twoBedrock.above(), Blocks.BEDROCK);
        helper.setBlock(twoBedrock.above(2), Blocks.BEDROCK);
        helper.setBlock(bedrockSecond, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(bedrockSecond.above(), Blocks.STONE);
        helper.setBlock(bedrockSecond.above(2), Blocks.BEDROCK);

        // All four paid in the same tick.
        helper.setBlock(atLimit.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(overLimit.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(twoBedrock.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(bedrockSecond.below(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // --- 18 blocks with the bedrock in front: moved, paid ---
                    helper.assertTrue(helper.getBlockState(atLimit.above(2)).is(Blocks.BEDROCK),
                            "bedrock plus seventeen stones did not move; with the bedrock counted that is "
                                    + "exactly the reinforced piston's limit of 18");
                    helper.assertBlockPresent(Blocks.STONE, atLimit.above(REINFORCED_LIMIT + 1));
                    helper.assertTrue(!helper.getBlockState(atLimit.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block under the 18 block column is still there after it moved");

                    // --- 19 blocks: over the limit, nothing moved, nothing paid ---
                    helper.assertTrue(helper.getBlockState(overLimit.above()).is(Blocks.BEDROCK),
                            "bedrock plus eighteen stones moved - the breached block no longer counts "
                                    + "towards the limit of 18, or the limit is higher");
                    helper.assertBlockNotPresent(Blocks.STONE, overLimit.above(REINFORCED_LIMIT + 2));
                    helper.assertTrue(helper.getBlockState(overLimit.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block under the 19 block column was used up although the "
                                    + "column never moved");

                    // --- a second unbreakable in line: refused, not paid ---
                    helper.assertTrue(helper.getBlockState(twoBedrock.above()).is(Blocks.BEDROCK)
                                    && helper.getBlockState(twoBedrock.above(2)).is(Blocks.BEDROCK)
                                    && !helper.getBlockState(twoBedrock.above(3)).is(Blocks.BEDROCK),
                            "the reinforced piston pushed a second unbreakable block behind the first - "
                                    + "the breach reaches further than the block in front");
                    helper.assertTrue(helper.getBlockState(twoBedrock.below()).is(Blocks.REDSTONE_BLOCK),
                            "two bedrock blocks in line used up the redstone block without moving");

                    // --- the unbreakable is not the front block: no breach at all ---
                    helper.assertTrue(helper.getBlockState(bedrockSecond.above()).is(Blocks.STONE)
                                    && helper.getBlockState(bedrockSecond.above(2)).is(Blocks.BEDROCK),
                            "stone followed by bedrock moved - only the block directly in front may be "
                                    + "an unbreakable one");
                    helper.assertTrue(helper.getBlockState(bedrockSecond.below()).is(Blocks.REDSTONE_BLOCK),
                            "stone followed by bedrock used up the redstone block");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The reinforced sticky piston breaches like the plain one, but retracting never pulls the
     * unbreakable back: the retracting resolver is never armed, and vanilla's pull check refuses a
     * block with a destroy speed of -1.
     *
     * <ul>
     *   <li><b>Paid by its redstone block alone:</b> the bedrock is pushed, the redstone block is used
     *       up, the piston loses its only power in the same tick and retracts at once - vanilla then
     *       drops ("spits") the moving bedrock at its new place instead of pulling it.</li>
     *   <li><b>Paid, and held out by a lever:</b> it stays extended with a <em>sticky</em> head; when
     *       the lever is switched off it retracts and the bedrock stays where it was pushed.</li>
     *   <li><b>Control:</b> the same piston on a lever pulls an ordinary stone block back, so it really
     *       is sticky and the two rows above prove the refusal, not a non-sticky piston.</li>
     * </ul>
     *
     * <p>What breaks this test: arming the breach during a retraction, a sticky head that does not fit
     * the sticky reinforced piston ({@code PistonHeadBlockMixin}), or building the sticky piston
     * non-sticky.
     */
    public static void stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack(GameTestHelper helper) {
        BlockPos fuelOnly = new BlockPos(1, 1, 1);
        BlockPos heldOut = new BlockPos(4, 1, 1);
        BlockPos control = new BlockPos(2, 1, 5);
        BlockPos heldOutLever = heldOut.east();
        BlockPos controlLever = control.west();

        paidRow(helper, fuelOnly, ModBlocks.REINFORCED_STICKY_PISTON, Blocks.BEDROCK);
        paidRow(helper, heldOut, ModBlocks.REINFORCED_STICKY_PISTON, Blocks.BEDROCK);
        placeLever(helper, heldOutLever);
        leverRow(helper, control, ModBlocks.REINFORCED_STICKY_PISTON, Blocks.STONE, Direction.WEST);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // --- paid by its redstone block alone: pushed, spat out, no head ---
                    helper.assertTrue(helper.getBlockState(fuelOnly.above(2)).is(Blocks.BEDROCK),
                            "the reinforced sticky piston did not push the bedrock although a redstone "
                                    + "block sat directly behind it");
                    helper.assertBlockNotPresent(Blocks.BEDROCK, fuelOnly.above());
                    helper.assertTrue(!helper.getBlockState(fuelOnly.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block that paid the sticky piston's breach is still there");
                    helper.assertBlockProperty(fuelOnly, PistonBaseBlock.EXTENDED, Boolean.FALSE);

                    // --- held out by the lever: extended, sticky head, bedrock pushed ---
                    helper.assertBlockProperty(heldOut, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertTrue(helper.getBlockState(heldOut.above()).is(ModBlocks.REINFORCED_PISTON_HEAD)
                                    && helper.getBlockState(heldOut.above()).getValue(PistonHeadBlock.TYPE) == PistonType.STICKY,
                            "the extended reinforced sticky piston carries no sticky head, found "
                                    + helper.getBlockState(heldOut.above()));
                    helper.assertBlockPresent(Blocks.BEDROCK, heldOut.above(2));

                    // --- control: extended on stone, sticky head ---
                    helper.assertBlockProperty(control, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.STONE, control.above(2));

                    helper.pullLever(heldOutLever);
                    helper.pullLever(controlLever);
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertBlockProperty(heldOut, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertTrue(helper.getBlockState(heldOut.above(2)).is(Blocks.BEDROCK)
                                    && !helper.getBlockState(heldOut.above()).is(Blocks.BEDROCK),
                            "the reinforced sticky piston pulled the bedrock back when it retracted");
                    helper.assertBlockNotPresent(ModBlocks.REINFORCED_PISTON_HEAD, heldOut.above());

                    // The control proves the piston is sticky at all.
                    helper.assertBlockProperty(control, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertTrue(helper.getBlockState(control.above()).is(Blocks.STONE),
                            "the reinforced sticky piston did not pull an ordinary stone back, so it is "
                                    + "not sticky and the bedrock row above proves nothing");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // NETHERITE AND ENDERITE: THE SACRIFICE
    // =====================================================================================

    /**
     * The netherite piston deletes the unbreakable block in front of it and pays with the redstone
     * block <em>and itself</em>, leaving nothing behind: no head, no moving block, no drop.
     *
     * <ul>
     *   <li><b>Facing up</b> with the redstone block behind (below) and <b>facing down</b> with the
     *       redstone block behind (above): bedrock, redstone block and piston are gone, no
     *       {@code piston_head} or {@code moving_piston} is left in front.</li>
     *   <li><b>A redstone block behind and one beside:</b> only the one directly behind is used up -
     *       {@code PistonBreach#findFuel} asks behind first.</li>
     *   <li><b>A lever only:</b> nothing breaks, the bedrock and the piston stay. A lever powers the
     *       piston but pays for nothing.</li>
     * </ul>
     * Afterwards not a single item entity may lie in the room: the unbreakable, the redstone block
     * and the piston are all destroyed without drops.
     *
     * <p>What breaks this test: a sacrifice that keeps the redstone block or the piston, one that drops
     * anything, a breach that runs {@code moveBlocks} (a head or moving block is left), taking a side
     * redstone block before the one behind, and a lever that pays.
     */
    public static void netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock(GameTestHelper helper) {
        fillFloor(helper);
        BlockPos up = new BlockPos(1, 1, 1);
        BlockPos down = new BlockPos(4, 3, 1);
        BlockPos twoPayers = new BlockPos(1, 1, 5);
        BlockPos leverOnly = new BlockPos(5, 1, 5);

        paidRow(helper, up, ModBlocks.NETHERITE_PISTON, Blocks.BEDROCK);

        helper.setBlock(down, facing(ModBlocks.NETHERITE_PISTON, Direction.DOWN));
        helper.setBlock(down.below(), Blocks.BEDROCK);
        helper.setBlock(down.above(), Blocks.REDSTONE_BLOCK);

        BlockPos sidePayer = twoPayers.east();
        helper.setBlock(twoPayers, upright(ModBlocks.NETHERITE_PISTON));
        helper.setBlock(twoPayers.above(), Blocks.BEDROCK);
        helper.setBlock(sidePayer, Blocks.REDSTONE_BLOCK);
        helper.setBlock(twoPayers.below(), Blocks.REDSTONE_BLOCK);

        leverRow(helper, leverOnly, ModBlocks.NETHERITE_PISTON, Blocks.BEDROCK, Direction.EAST);

        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    // --- facing up ---
                    helper.assertTrue(!helper.getBlockState(up.above()).is(Blocks.BEDROCK),
                            "the netherite piston did not destroy the bedrock in front of it although a "
                                    + "redstone block sat directly behind it");
                    helper.assertTrue(!helper.getBlockState(up.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block that paid the netherite piston's breach is still there");
                    helper.assertTrue(!helper.getBlockState(up).is(ModBlocks.NETHERITE_PISTON),
                            "the netherite piston survived its own breach; it is supposed to be used up");
                    assertNothingMovedIn(helper, up.above());

                    // --- facing down: the same with the redstone block above ---
                    helper.assertTrue(!helper.getBlockState(down.below()).is(Blocks.BEDROCK),
                            "the downward netherite piston did not destroy the bedrock below it");
                    helper.assertTrue(!helper.getBlockState(down.above()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block above the downward netherite piston is still there");
                    helper.assertTrue(!helper.getBlockState(down).is(ModBlocks.NETHERITE_PISTON),
                            "the downward netherite piston survived its own breach");
                    assertNothingMovedIn(helper, down.below());

                    // --- two payers: the one behind pays, the one beside stays ---
                    helper.assertTrue(!helper.getBlockState(twoPayers.above()).is(Blocks.BEDROCK),
                            "the netherite piston with two redstone blocks did not breach");
                    helper.assertTrue(helper.getBlockState(sidePayer).is(Blocks.REDSTONE_BLOCK)
                                    && !helper.getBlockState(twoPayers.below()).is(Blocks.REDSTONE_BLOCK),
                            "the redstone block beside the netherite piston paid although one sat directly "
                                    + "behind it - the one behind has to be taken first");

                    // --- a lever only: nothing breaks ---
                    helper.assertTrue(helper.getBlockState(leverOnly.above()).is(Blocks.BEDROCK)
                                    && helper.getBlockState(leverOnly).is(ModBlocks.NETHERITE_PISTON),
                            "a netherite piston powered by a lever alone destroyed the bedrock or itself");

                    // --- and not one drop in the whole room ---
                    List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
                    helper.assertTrue(drops.isEmpty(),
                            "the sacrifice dropped items, but the unbreakable, the redstone block and the "
                                    + "piston are destroyed without drops: " + drops);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The enderite piston breaches up to three cells deep: air is skipped, every unbreakable block on
     * the way is removed without a drop, an ordinary block the breaker may break is destroyed with its
     * drop, and a block from {@code simplebuilding:piston_breach_immune} ends the breach. The fourth
     * cell is out of reach. Afterwards the redstone block and the piston are gone, as with the
     * netherite piston.
     *
     * <ul>
     *   <li><b>Bedrock, air, stone, bedrock:</b> the first bedrock goes, the air gap is skipped, the
     *       stone breaks and drops cobblestone, the bedrock in the fourth cell stays.</li>
     *   <li><b>Bedrock, light, bedrock:</b> the first bedrock goes, the light block (immune) stops the
     *       breach, so both it and the bedrock behind it stay.</li>
     * </ul>
     *
     * <p>Both pistons face east with the redstone block behind them in the west wall cell, and the
     * floor is filled so the cobblestone has something to land on.
     *
     * <p>What breaks this test: a depth other than three, air that ends the breach instead of being
     * skipped, an immune block that is skipped instead of ending it, ordinary blocks destroyed without
     * their drop, and a sacrifice that keeps the redstone block or the piston.
     */
    public static void enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks(GameTestHelper helper) {
        fillFloor(helper);
        BlockPos gapped = new BlockPos(1, 1, 1);
        BlockPos immune = new BlockPos(1, 1, 5);

        helper.setBlock(gapped, facing(ModBlocks.ENDERITE_PISTON, Direction.EAST));
        helper.setBlock(gapped.east(1), Blocks.BEDROCK);
        helper.setBlock(gapped.east(2), Blocks.AIR);
        helper.setBlock(gapped.east(3), Blocks.STONE);
        helper.setBlock(gapped.east(4), Blocks.BEDROCK);

        helper.setBlock(immune, facing(ModBlocks.ENDERITE_PISTON, Direction.EAST));
        helper.setBlock(immune.east(1), Blocks.BEDROCK);
        helper.setBlock(immune.east(2), Blocks.LIGHT);
        helper.setBlock(immune.east(3), Blocks.BEDROCK);

        helper.setBlock(gapped.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(immune.west(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    // --- bedrock, air, stone, bedrock ---
                    helper.assertTrue(!helper.getBlockState(gapped.east(1)).is(Blocks.BEDROCK),
                            "the enderite piston did not breach the bedrock directly in front of it");
                    helper.assertTrue(!helper.getBlockState(gapped.east(3)).is(Blocks.STONE),
                            "the enderite breach stopped at the air gap instead of skipping it - the stone "
                                    + "in the third cell is still there");
                    helper.assertTrue(helper.getBlockState(gapped.east(4)).is(Blocks.BEDROCK),
                            "the enderite piston reached a fourth cell; its breach is three cells deep");
                    helper.assertTrue(countDropped(helper, Items.COBBLESTONE) == 1,
                            "the stone the enderite breach broke did not drop its cobblestone - ordinary "
                                    + "blocks on the way are mined, not deleted");
                    helper.assertTrue(!helper.getBlockState(gapped.west()).is(Blocks.REDSTONE_BLOCK)
                                    && !helper.getBlockState(gapped).is(ModBlocks.ENDERITE_PISTON),
                            "the enderite piston or its redstone block survived the breach");

                    // --- bedrock, light, bedrock: the immune light block ends it ---
                    helper.assertTrue(!helper.getBlockState(immune.east(1)).is(Blocks.BEDROCK),
                            "the enderite piston did not breach the bedrock in front of the light block");
                    helper.assertTrue(helper.getBlockState(immune.east(2)).is(Blocks.LIGHT)
                                    && helper.getBlockState(immune.east(3)).is(Blocks.BEDROCK),
                            "the enderite breach went on past the immune light block; an immune block has "
                                    + "to end it");
                    helper.assertTrue(!helper.getBlockState(immune).is(ModBlocks.ENDERITE_PISTON),
                            "the second enderite piston survived its breach");

                    // Nothing unbreakable and no piston ever drops.
                    helper.assertTrue(countDropped(helper, Items.BEDROCK) == 0
                                    && countDropped(helper, ModItems.ENDERITE_PISTON) == 0,
                            "the enderite breach dropped bedrock or the enderite piston itself");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // THE HEAD, THE IMMUNE LIST, EXPLOSIONS
    // =====================================================================================

    /**
     * Breaking the head of an extended mod piston breaks the piston with it and drops it, exactly as
     * vanilla does for its own pistons ({@code PistonHeadBlockMixin} accepts the mod pistons in
     * {@code PistonHeadBlock#isFittingBase}). Before 2026-09 the head's removal left a headless extended
     * base, whose later retraction deleted whatever had been put into the head cell.
     *
     * <p>Five rows: the reinforced piston, the reinforced sticky piston (whose head has to be the
     * sticky one), the netherite piston, the enderite piston, and a vanilla piston as the yardstick. Each is extended by a
     * redstone block below it with air in front; then each head is removed, and the base has to be gone
     * with its item lying next to it.
     *
     * <p>What breaks this test: dropping a mod piston from the mixin, a head type the mixin maps to the
     * wrong piston, and a base that survives the loss of its head.
     */
    public static void breakingTheHeadOfModPistonsBreaksThePistonToo(GameTestHelper helper) {
        fillFloor(helper);
        BlockPos reinforced = new BlockPos(1, 1, 1);
        BlockPos sticky = new BlockPos(3, 1, 1);
        BlockPos netherite = new BlockPos(5, 1, 1);
        BlockPos vanilla = new BlockPos(3, 1, 5);
        BlockPos enderite = new BlockPos(5, 1, 5);

        for (BlockPos piston : List.of(reinforced, sticky, netherite, enderite, vanilla)) {
            helper.setBlock(piston.above(), Blocks.AIR);
        }
        helper.setBlock(reinforced, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(sticky, upright(ModBlocks.REINFORCED_STICKY_PISTON));
        helper.setBlock(netherite, upright(ModBlocks.NETHERITE_PISTON));
        helper.setBlock(enderite, upright(ModBlocks.ENDERITE_PISTON));
        helper.setBlock(vanilla, upright(Blocks.PISTON));
        for (BlockPos piston : List.of(reinforced, sticky, netherite, enderite, vanilla)) {
            helper.setBlock(piston.below(), Blocks.REDSTONE_BLOCK);
        }

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    assertExtendedWithHead(helper, vanilla, Blocks.PISTON_HEAD, PistonType.DEFAULT, "the vanilla piston");
                    assertExtendedWithHead(helper, reinforced, ModBlocks.REINFORCED_PISTON_HEAD, PistonType.DEFAULT,
                            "the reinforced piston");
                    assertExtendedWithHead(helper, sticky, ModBlocks.REINFORCED_PISTON_HEAD, PistonType.STICKY,
                            "the reinforced sticky piston");
                    assertExtendedWithHead(helper, netherite, ModBlocks.NETHERITE_PISTON_HEAD, PistonType.DEFAULT,
                            "the netherite piston");
                    assertExtendedWithHead(helper, enderite, ModBlocks.ENDERITE_PISTON_HEAD, PistonType.DEFAULT,
                            "the enderite piston");

                    for (BlockPos piston : List.of(reinforced, sticky, netherite, enderite, vanilla)) {
                        helper.destroyBlock(piston.above());
                    }
                })
                .thenExecuteAfter(5, () -> {
                    assertBrokeWithItsHead(helper, vanilla, Blocks.PISTON, Items.PISTON, "the vanilla piston");
                    assertBrokeWithItsHead(helper, reinforced, ModBlocks.REINFORCED_PISTON, ModItems.REINFORCED_PISTON,
                            "the reinforced piston");
                    assertBrokeWithItsHead(helper, sticky, ModBlocks.REINFORCED_STICKY_PISTON,
                            ModItems.REINFORCED_STICKY_PISTON, "the reinforced sticky piston");
                    assertBrokeWithItsHead(helper, netherite, ModBlocks.NETHERITE_PISTON, ModItems.NETHERITE_PISTON,
                            "the netherite piston");
                    assertBrokeWithItsHead(helper, enderite, ModBlocks.ENDERITE_PISTON, ModItems.ENDERITE_PISTON,
                            "the enderite piston");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * Every mod piston extends with the head of its own tier and takes it back when it retracts:
     * {@code reinforced_piston_head} (normal type on the reinforced piston, sticky type on the
     * reinforced sticky piston), {@code netherite_piston_head}, {@code enderite_piston_head}; a vanilla
     * piston in the same room keeps {@code minecraft:piston_head}. Picking the head with the middle
     * mouse button gives the piston it belongs to. After the power is cut every base is retracted and
     * every head cell is empty again.
     *
     * <p>What breaks this test: losing the head swap in {@code PistonBlockMixin} (the heads turn
     * vanilla), a wrong entry in {@code ModPistonHeadBlock#headFor} or {@code #typeFor}, a head that
     * does not fit its base in {@code PistonHeadBlockMixin} (it vanishes as soon as it lands), and a
     * retraction that leaves the head behind.
     */
    public static void everyModPistonCarriesTheHeadOfItsTierAndTakesItBack(GameTestHelper helper) {
        fillFloor(helper);
        BlockPos reinforced = new BlockPos(1, 1, 1);
        BlockPos sticky = new BlockPos(3, 1, 1);
        BlockPos netherite = new BlockPos(5, 1, 1);
        BlockPos enderite = new BlockPos(1, 1, 4);
        BlockPos vanilla = new BlockPos(4, 1, 4);
        List<BlockPos> all = List.of(reinforced, sticky, netherite, enderite, vanilla);

        for (BlockPos piston : all) {
            helper.setBlock(piston.above(), Blocks.AIR);
        }
        helper.setBlock(reinforced, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(sticky, upright(ModBlocks.REINFORCED_STICKY_PISTON));
        helper.setBlock(netherite, upright(ModBlocks.NETHERITE_PISTON));
        helper.setBlock(enderite, upright(ModBlocks.ENDERITE_PISTON));
        helper.setBlock(vanilla, upright(Blocks.PISTON));
        for (BlockPos piston : all) {
            helper.setBlock(piston.below(), Blocks.REDSTONE_BLOCK);
        }

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    assertExtendedWithHead(helper, reinforced, ModBlocks.REINFORCED_PISTON_HEAD, PistonType.DEFAULT,
                            "the reinforced piston");
                    assertExtendedWithHead(helper, sticky, ModBlocks.REINFORCED_PISTON_HEAD, PistonType.STICKY,
                            "the reinforced sticky piston");
                    assertExtendedWithHead(helper, netherite, ModBlocks.NETHERITE_PISTON_HEAD, PistonType.DEFAULT,
                            "the netherite piston");
                    assertExtendedWithHead(helper, enderite, ModBlocks.ENDERITE_PISTON_HEAD, PistonType.DEFAULT,
                            "the enderite piston");
                    assertExtendedWithHead(helper, vanilla, Blocks.PISTON_HEAD, PistonType.DEFAULT, "the vanilla piston");

                    assertPicksItsPiston(helper, reinforced, ModItems.REINFORCED_PISTON);
                    assertPicksItsPiston(helper, sticky, ModItems.REINFORCED_STICKY_PISTON);
                    assertPicksItsPiston(helper, netherite, ModItems.NETHERITE_PISTON);
                    assertPicksItsPiston(helper, enderite, ModItems.ENDERITE_PISTON);

                    for (BlockPos piston : all) {
                        helper.setBlock(piston.below(), Blocks.STONE);
                    }
                })
                .thenExecuteAfter(20, () -> {
                    for (BlockPos piston : all) {
                        helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                        helper.assertTrue(helper.getBlockState(piston.above()).isAir(),
                                helper.getBlockState(piston) + " retracted but left "
                                        + helper.getBlockState(piston.above()) + " in its head cell");
                    }
                })
                .thenSucceed();
    }

    /**
     * Every block a mod piston destroys goes through {@code PistonBoreEffects#destroy}: vanilla's
     * {@code Level#destroyBlock} (world event 2001 - the block's break particles and its break sound
     * for every player nearby) plus the mod's own sound {@code simplebuilding:block.piston.bore}. The
     * effects themselves are packets; the observer hook is what the server can show, and it reports
     * only after {@code destroyBlock} succeeded.
     *
     * <ul>
     *   <li><b>Netherite piston on stone:</b> the ordinary break, with drop - one report.</li>
     *   <li><b>Enderite piston, bedrock then stone, redstone block below:</b> the paid breach - bedrock
     *       without drop, then the stone with drop, two reports. The paying redstone block and the
     *       piston itself vanish too, but they are not bored through and report nothing.</li>
     *   <li><b>Control, a vanilla piston on stone:</b> pushed, no report.</li>
     * </ul>
     * The sound event is registered under its id.
     *
     * <p>What breaks this test: either piston destroying its target without {@code PistonBoreEffects},
     * a report for the payer or the piston, a missing sound registration.
     */
    public static void modPistonBreaksReportEveryDestroyedBlockForParticlesAndSound(GameTestHelper helper) {
        BlockPos netherite = new BlockPos(1, 1, 1);
        BlockPos enderite = new BlockPos(4, 1, 1);
        BlockPos vanilla = new BlockPos(2, 1, 4);
        ServerLevel level = helper.getLevel();
        AABB room = helper.getBounds();
        List<String> reports = new java.util.concurrent.CopyOnWriteArrayList<>();
        Runnable stop = PistonBoreEffects.observe((where, pos, broken, dropped) -> {
            if (where == level && room.contains(Vec3.atCenterOf(pos))) {
                reports.add(pos.toShortString() + " " + BuiltInRegistries.BLOCK.getKey(broken.getBlock())
                        + (dropped ? " drop" : " nodrop"));
            }
        });

        helper.setBlock(enderite.above(3), Blocks.AIR);
        helper.setBlock(enderite.above(2), Blocks.STONE);
        paidRow(helper, netherite, ModBlocks.NETHERITE_PISTON, Blocks.STONE);
        helper.setBlock(enderite, upright(ModBlocks.ENDERITE_PISTON));
        helper.setBlock(enderite.above(), Blocks.BEDROCK);
        helper.setBlock(enderite.below(), Blocks.REDSTONE_BLOCK);
        paidRow(helper, vanilla, Blocks.PISTON, Blocks.STONE);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    stop.run();
                    List<String> expected = List.of(
                            helper.absolutePos(netherite.above()).toShortString() + " minecraft:stone drop",
                            helper.absolutePos(enderite.above()).toShortString() + " minecraft:bedrock nodrop",
                            helper.absolutePos(enderite.above(2)).toShortString() + " minecraft:stone drop");
                    helper.assertTrue(reports.size() == expected.size() && reports.containsAll(expected),
                            "the bore effects reported " + reports + ", expected exactly " + expected);
                    helper.assertBlockPresent(Blocks.STONE, vanilla.above(2));
                    Assertions.valueEqual(helper, BuiltInRegistries.SOUND_EVENT.getValue(ModSounds.PISTON_BORE_ID), ModSounds.PISTON_BORE,
                            "the sound event registered as simplebuilding:block.piston.bore");
                })
                .thenSucceed();
    }

    /** Tick budget for {@link #modPistonsAskThePlatformGuardBeforeEveryBreak}. */
    public static final int GUARD_MAX_TICKS = 60;

    /**
     * Every block a mod piston destroys is first put to the platform guard
     * ({@code PlatformServices#mayPistonBreak}, which fires the loader's piston and block break
     * events for protection mods), and a refusal keeps the block. Before 2026-09-26 the breaker broke
     * before any event and the enderite breach fired none at all (audit #15).
     *
     * <ul>
     *   <li><b>Netherite piston on stone, refused:</b> nothing is destroyed; the piston then extends
     *       like a vanilla one and pushes the stone.</li>
     *   <li><b>Netherite breach on bedrock, refused:</b> the bedrock stays, and neither the paying
     *       redstone block nor the piston is used up.</li>
     *   <li><b>Enderite breach, second cell refused:</b> the bedrock in front goes, the refused stone
     *       and the stone behind it stay; the breach was paid, so redstone block and piston are gone.</li>
     *   <li><b>Control, netherite piston on stone, allowed:</b> broken with its drop.</li>
     * </ul>
     * The guard has to have been asked for each of these five cells.
     */
    public static void modPistonsAskThePlatformGuardBeforeEveryBreak(GameTestHelper helper) {
        BlockPos refusedBreak = new BlockPos(1, 1, 1);
        BlockPos refusedBreach = new BlockPos(4, 1, 1);
        BlockPos enderite = new BlockPos(1, 1, 5);
        BlockPos allowed = new BlockPos(5, 1, 5);
        ServerLevel level = helper.getLevel();
        AABB room = helper.getBounds();

        java.util.Set<BlockPos> refused = java.util.Set.of(
                helper.absolutePos(refusedBreak.above()),
                helper.absolutePos(refusedBreach.above()),
                helper.absolutePos(enderite.above(2)));
        java.util.Set<BlockPos> asked = java.util.concurrent.ConcurrentHashMap.newKeySet();
        com.simplebuilding.platform.PistonBreakGuard installed = com.simplebuilding.platform.PlatformServices.pistonBreakGuard();
        java.util.Map<BlockPos, java.util.concurrent.atomic.AtomicInteger> moves = new java.util.concurrent.ConcurrentHashMap<>();
        com.simplebuilding.platform.PlatformServices.setPistonBreakGuard(new com.simplebuilding.platform.PistonBreakGuard() {
            @Override
            public boolean mayBreak(ServerLevel where, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
                if (where == level && room.contains(Vec3.atCenterOf(target))) {
                    asked.add(target.immutable());
                    if (refused.contains(target)) {
                        return false;
                    }
                }
                return installed.mayBreak(where, piston, facing, target, state);
            }

            // Forwarded, so the loader's piston event still fires for every other test meanwhile.
            @Override
            public boolean mayMove(ServerLevel where, BlockPos piston, Direction facing) {
                if (where == level && room.contains(Vec3.atCenterOf(piston))) {
                    moves.computeIfAbsent(piston.immutable(), key -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
                }
                return installed.mayMove(where, piston, facing);
            }
        });
        TestCleanup.before(helper, () -> com.simplebuilding.platform.PlatformServices.setPistonBreakGuard(installed));

        List<String> reports = new java.util.concurrent.CopyOnWriteArrayList<>();
        Runnable stop = PistonBoreEffects.observe((where, pos, broken, dropped) -> {
            if (where == level && room.contains(Vec3.atCenterOf(pos))) {
                reports.add(pos.toShortString());
            }
        });
        TestCleanup.before(helper, stop);

        paidRow(helper, refusedBreak, ModBlocks.NETHERITE_PISTON, Blocks.STONE);
        helper.setBlock(refusedBreak.above(3), Blocks.AIR);
        paidRow(helper, refusedBreach, ModBlocks.NETHERITE_PISTON, Blocks.BEDROCK);
        // paidRow clears above(2), so the stones behind the bedrock go in afterwards.
        paidRow(helper, enderite, ModBlocks.ENDERITE_PISTON, Blocks.BEDROCK);
        helper.setBlock(enderite.above(4), Blocks.AIR);
        helper.setBlock(enderite.above(3), Blocks.STONE);
        helper.setBlock(enderite.above(2), Blocks.STONE);
        paidRow(helper, allowed, ModBlocks.NETHERITE_PISTON, Blocks.STONE);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    List<BlockPos> mustBeAsked = List.of(
                            helper.absolutePos(refusedBreak.above()),
                            helper.absolutePos(refusedBreach.above()),
                            helper.absolutePos(enderite.above()),
                            helper.absolutePos(enderite.above(2)),
                            helper.absolutePos(allowed.above()));
                    helper.assertTrue(asked.containsAll(mustBeAsked),
                            "the platform guard was asked for " + asked + " but not for all of " + mustBeAsked
                                    + " - a mod piston destroys blocks without asking protection mods");

                    // --- refused break: nothing destroyed, the stone was pushed instead ---
                    helper.assertTrue(!reports.contains(helper.absolutePos(refusedBreak.above()).toShortString()),
                            "the netherite piston destroyed the stone the guard refused");
                    helper.assertTrue(helper.getBlockState(refusedBreak.above(2)).is(Blocks.STONE),
                            "the refused stone was not pushed like by a vanilla piston, found "
                                    + helper.getBlockState(refusedBreak.above(2)));

                    // --- refused breach: nothing consumed ---
                    helper.assertTrue(helper.getBlockState(refusedBreach.above()).is(Blocks.BEDROCK)
                                    && helper.getBlockState(refusedBreach).is(ModBlocks.NETHERITE_PISTON)
                                    && helper.getBlockState(refusedBreach.below()).is(Blocks.REDSTONE_BLOCK),
                            "a refused netherite breach still destroyed the bedrock, the piston or its redstone block");

                    // --- enderite: first cell allowed, second refused ---
                    helper.assertTrue(!helper.getBlockState(enderite.above()).is(Blocks.BEDROCK),
                            "the enderite breach did not remove the allowed bedrock in front");
                    helper.assertTrue(helper.getBlockState(enderite.above(2)).is(Blocks.STONE)
                                    && helper.getBlockState(enderite.above(3)).is(Blocks.STONE),
                            "the enderite breach went through the refused stone");
                    helper.assertTrue(!helper.getBlockState(enderite).is(ModBlocks.ENDERITE_PISTON)
                                    && !helper.getBlockState(enderite.below()).is(Blocks.REDSTONE_BLOCK),
                            "the enderite breach broke the front bedrock but kept its piston or redstone block");

                    // --- control ---
                    helper.assertTrue(reports.contains(helper.absolutePos(allowed.above()).toShortString()),
                            "the netherite piston did not break the allowed stone - the rig itself is broken");

                    // --- the piston event goes out once per action, not once per block (audit N16) ---
                    java.util.concurrent.atomic.AtomicInteger enderiteMoves = moves.get(helper.absolutePos(enderite));
                    java.util.concurrent.atomic.AtomicInteger allowedMoves = moves.get(helper.absolutePos(allowed));
                    helper.assertTrue(enderiteMoves != null && enderiteMoves.get() == 1,
                            "the enderite breach asked the piston event " + enderiteMoves + " times instead of once "
                                    + "for the whole breach");
                    helper.assertTrue(allowedMoves != null && allowedMoves.get() == 1,
                            "the breaker asked the piston event " + allowedMoves + " times instead of once for its break");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // AUDIT 2026-09-26 #23 / #24, NACH-AUDIT N4 / N5 / N16
    // =====================================================================================

    /** Tick budget for {@link #netheriteBreakerLosesDurabilityAndCrumblesToReinforcedPiston}. */
    public static final int DURABILITY_MAX_TICKS = 80;

    /**
     * The netherite breaker has a real durability (owner's choice 2026-09-28, replacing the wear
     * budget of 2026-09-27): 1/9 of the netherite pickaxe, because the upgrade costs one nugget.
     *
     * <ul>
     *   <li><b>The numbers:</b> {@code 2031 / 9 = 226}; every damage from 0 to 225 survives the trip
     *       through the two block state properties, and the crack stage is {@code damage * 8 / 226}.
     *       An old {@code wear=n} state (step 0) reads as the first damage of stage n - the same
     *       fraction n/8.</li>
     *   <li><b>Only breaking costs, one per block:</b> stone and deepslate (hardness 1.5 and 3) both
     *       cost exactly 1; extending into air, and retracting, cost nothing.</li>
     *   <li><b>Switched off</b> ({@code freezeDurabilityAt}, the config switch without touching the
     *       shared config): the stone still breaks, the damage stays.</li>
     *   <li><b>Running out:</b> at 225 damage one more stone turns the breaker into a reinforced
     *       piston (same facing) that extends with its own head.</li>
     *   <li><b>The enderite piston</b> pays the same 1 for the same stone.</li>
     *   <li><b>Repair:</b> a netherite nugget restores full durability and is used up outside
     *       creative.</li>
     * </ul>
     */
    public static void netheriteBreakerLosesDurabilityAndCrumblesToReinforcedPiston(GameTestHelper helper) {
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos crumble = new BlockPos(4, 1, 1);
        BlockPos frozen = new BlockPos(1, 1, 5);
        BlockPos enderite = new BlockPos(4, 1, 5);
        BlockPos deepslate = new BlockPos(6, 1, 1);
        BlockPos air = new BlockPos(6, 1, 5);
        BlockPos repair = new BlockPos(3, 1, 3);
        ServerLevel level = helper.getLevel();

        // --- the numbers, without a world ---
        Assertions.valueEqual(helper, net.minecraft.world.item.ToolMaterial.NETHERITE.durability(), 2031,
                "durability of the netherite pickaxe the breaker's durability is derived from");
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.NETHERITE_MAX_DURABILITY, 226,
                "durability of the netherite piston (1/9 of the netherite pickaxe)");
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.maxDurabilityOf(ModBlocks.NETHERITE_PISTON.defaultBlockState()), 226,
                "durability the netherite piston block reports");
        assertEveryDamageRoundTrips(helper, ModBlocks.NETHERITE_PISTON, 226);
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(upright(ModBlocks.NETHERITE_PISTON)
                        .setValue(NetheriteBreakerPistonBlock.WEAR, 4)), 113,
                "damage of an old-world netherite piston at wear stage 4 (half of 226)");
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.durabilityOf(upright(ModBlocks.NETHERITE_PISTON)), 226,
                "durability left on a fresh netherite piston");

        TestCleanup.before(helper, NetheriteBreakerPistonBlock.freezeDurabilityAt(helper.absolutePos(frozen)));

        paidRow(helper, stone, ModBlocks.NETHERITE_PISTON, Blocks.STONE);
        paidRow(helper, deepslate, ModBlocks.NETHERITE_PISTON, Blocks.DEEPSLATE);
        paidRow(helper, enderite, ModBlocks.ENDERITE_PISTON, Blocks.STONE);
        helper.setBlock(frozen.above(2), Blocks.AIR);
        helper.setBlock(frozen, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.NETHERITE_PISTON), 40));
        helper.setBlock(frozen.above(), Blocks.STONE);
        helper.setBlock(frozen.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(air.above(2), Blocks.AIR);
        helper.setBlock(air, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.NETHERITE_PISTON), 100));
        helper.setBlock(air.above(), Blocks.AIR);
        helper.setBlock(air.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(crumble.above(2), Blocks.AIR);
        helper.setBlock(crumble, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.NETHERITE_PISTON), 225));
        helper.setBlock(crumble.above(), Blocks.STONE);
        helper.setBlock(crumble.below(), Blocks.REDSTONE_BLOCK);

        // --- repair with a netherite nugget ---
        helper.setBlock(repair, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.NETHERITE_PISTON), 150));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TestCleanup.before(helper, () -> level.getServer().getPlayerList().remove(player));
        player.getAbilities().instabuild = false;
        ItemStack nugget = new ItemStack(ModItems.NETHERITE_NUGGET, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, nugget);
        BlockPos repairAbsolute = helper.absolutePos(repair);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(repairAbsolute), Direction.NORTH, repairAbsolute, false);
        InteractionResult repaired = level.getBlockState(repairAbsolute)
                .useItemOn(nugget, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(repaired.consumesAction(), "using a netherite nugget on a damaged breaker did nothing: " + repaired);
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(repair)), 0,
                "damage after the repair with a netherite nugget (full durability)");
        Assertions.valueEqual(helper, nugget.getCount(), 1, "netherite nuggets left after one repair outside creative");

        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    BlockState stoneBreaker = helper.getBlockState(stone);
                    helper.assertTrue(stoneBreaker.is(ModBlocks.NETHERITE_PISTON) && stoneBreaker.getValue(PistonBaseBlock.EXTENDED),
                            "the netherite piston on stone did not extend, found " + stoneBreaker);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(stoneBreaker), 1,
                            "damage after breaking one stone");
                    BlockState deepslateBreaker = helper.getBlockState(deepslate);
                    helper.assertTrue(deepslateBreaker.getValue(PistonBaseBlock.EXTENDED)
                                    && helper.getBlockState(deepslate.above()).is(ModBlocks.NETHERITE_PISTON_HEAD),
                            "the netherite piston did not break the deepslate, found " + helper.getBlockState(deepslate.above()));
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(deepslateBreaker), 1,
                            "damage after breaking one deepslate - one per block, not per hardness point");

                    BlockState airBreaker = helper.getBlockState(air);
                    helper.assertTrue(airBreaker.getValue(PistonBaseBlock.EXTENDED),
                            "the netherite piston in front of air did not extend, found " + airBreaker);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(airBreaker), 100,
                            "damage after extending into air - only breaking costs durability");

                    helper.assertTrue(helper.getBlockState(frozen.above()).is(ModBlocks.NETHERITE_PISTON_HEAD),
                            "with the durability switched off the breaker has to break the stone as before, found "
                                    + helper.getBlockState(frozen.above()));
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(frozen)), 40,
                            "damage with the durability switched off");

                    helper.assertTrue(helper.getBlockState(crumble).is(ModBlocks.REINFORCED_PISTON),
                            "a netherite piston with 1 durability left did not crumble into a reinforced piston, found "
                                    + helper.getBlockState(crumble));
                    assertExtendedWithHead(helper, crumble, ModBlocks.REINFORCED_PISTON_HEAD, PistonType.DEFAULT,
                            "the crumbled breaker");
                    Assertions.valueEqual(helper, helper.getBlockState(crumble).getValue(DirectionalBlock.FACING), Direction.UP,
                            "facing of the reinforced piston the breaker crumbled into");

                    helper.assertTrue(helper.getBlockState(enderite).is(ModBlocks.ENDERITE_PISTON),
                            "the enderite piston is gone, found " + helper.getBlockState(enderite));
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(enderite)), 1,
                            "enderite piston damage after breaking one stone");

                    // Retract the stone and air rows: take their redstone blocks away.
                    helper.setBlock(stone.below(), Blocks.AIR);
                    helper.setBlock(air.below(), Blocks.AIR);
                })
                .thenExecuteAfter(6, () -> {
                    BlockState retracted = helper.getBlockState(stone);
                    helper.assertTrue(retracted.is(ModBlocks.NETHERITE_PISTON) && !retracted.getValue(PistonBaseBlock.EXTENDED),
                            "the netherite piston did not retract, found " + retracted);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(retracted), 1,
                            "damage after retracting - retracting neither costs nor repairs");
                    BlockState airRetracted = helper.getBlockState(air);
                    helper.assertTrue(!airRetracted.getValue(PistonBaseBlock.EXTENDED),
                            "the netherite piston in front of air did not retract, found " + airRetracted);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(airRetracted), 100,
                            "damage after extending into air and retracting");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /** Every damage from 0 to {@code max - 1} survives the trip through the block state, with the right crack stage. */
    private static void assertEveryDamageRoundTrips(GameTestHelper helper, Block piston, int max) {
        BlockState base = upright(piston);
        for (int damage = 0; damage < max; damage++) {
            BlockState state = NetheriteBreakerPistonBlock.withDamage(base, damage);
            if (NetheriteBreakerPistonBlock.damageOf(state) != damage) {
                helper.fail(piston + ": damage " + damage + " reads back as " + NetheriteBreakerPistonBlock.damageOf(state)
                        + " from " + state);
            }
            if (NetheriteBreakerPistonBlock.wearOf(state) != damage * NetheriteBreakerPistonBlock.WEAR_STAGES / max) {
                helper.fail(piston + ": damage " + damage + " shows crack stage " + NetheriteBreakerPistonBlock.wearOf(state)
                        + " instead of " + damage * NetheriteBreakerPistonBlock.WEAR_STAGES / max);
            }
        }
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(NetheriteBreakerPistonBlock.withDamage(base, max + 50)), max - 1,
                piston + ": damage past the maximum is capped at max - 1 (the next break turns it into the tier below)");
    }

    /** Tick budget for {@link #enderitePistonLosesDurabilityAndCrumblesToNetheriteBreaker}. */
    public static final int ENDERITE_DURABILITY_MAX_TICKS = 80;

    /**
     * The enderite piston's durability: 1/9 of the enderite pickaxe ({@code 2530 / 9 = 281}).
     *
     * <ul>
     *   <li><b>One per broken block:</b> stone and deepslate both cost 1, and the damage survives
     *       retracting.</li>
     *   <li><b>Running out:</b> at 280 damage one more stone turns it one tier down into a netherite
     *       breaker - same facing, <em>full</em> netherite durability (only the enderite layer is used
     *       up) - that extends with its netherite head.</li>
     *   <li><b>Repair:</b> an enderite nugget restores full durability and is used up outside
     *       creative; a netherite nugget does not repair an enderite piston.</li>
     *   <li><b>Old worlds:</b> the default state (what an enderite piston from before the wear loads
     *       as) has full durability.</li>
     * </ul>
     */
    public static void enderitePistonLosesDurabilityAndCrumblesToNetheriteBreaker(GameTestHelper helper) {
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos crumble = new BlockPos(4, 1, 1);
        BlockPos deepslate = new BlockPos(1, 1, 5);
        BlockPos repair = new BlockPos(6, 1, 3);
        BlockPos wrongNugget = new BlockPos(6, 1, 5);
        ServerLevel level = helper.getLevel();

        // --- the numbers, without a world ---
        Assertions.valueEqual(helper, com.simplebuilding.items.ModToolMaterials.ENDERITE.durability(), 2530,
                "durability of the enderite pickaxe the piston's durability is derived from");
        Assertions.valueEqual(helper, com.simplebuilding.blocks.custom.EnderitePistonBlock.ENDERITE_MAX_DURABILITY, 281,
                "durability of the enderite piston (1/9 of the enderite pickaxe)");
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.maxDurabilityOf(ModBlocks.ENDERITE_PISTON.defaultBlockState()), 281,
                "durability the enderite piston block reports");
        assertEveryDamageRoundTrips(helper, ModBlocks.ENDERITE_PISTON, 281);
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(ModBlocks.ENDERITE_PISTON.defaultBlockState()), 0,
                "damage of the default state an enderite piston from an old world loads with");

        paidRow(helper, stone, ModBlocks.ENDERITE_PISTON, Blocks.STONE);
        paidRow(helper, deepslate, ModBlocks.ENDERITE_PISTON, Blocks.DEEPSLATE);
        helper.setBlock(crumble.above(2), Blocks.AIR);
        helper.setBlock(crumble, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.ENDERITE_PISTON), 280));
        helper.setBlock(crumble.above(), Blocks.STONE);
        helper.setBlock(crumble.below(), Blocks.REDSTONE_BLOCK);

        // --- repair with an enderite nugget, not with a netherite one ---
        helper.setBlock(repair, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.ENDERITE_PISTON), 200));
        helper.setBlock(wrongNugget, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.ENDERITE_PISTON), 200));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TestCleanup.before(helper, () -> level.getServer().getPlayerList().remove(player));
        player.getAbilities().instabuild = false;
        ItemStack netheriteNugget = new ItemStack(ModItems.NETHERITE_NUGGET, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, netheriteNugget);
        BlockPos wrongAbsolute = helper.absolutePos(wrongNugget);
        level.getBlockState(wrongAbsolute).useItemOn(netheriteNugget, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(wrongAbsolute), Direction.NORTH, wrongAbsolute, false));
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(wrongNugget)), 200,
                "damage of an enderite piston after using a netherite nugget on it");
        Assertions.valueEqual(helper, netheriteNugget.getCount(), 2, "netherite nuggets left after trying them on an enderite piston");

        ItemStack nugget = new ItemStack(ModItems.ENDERITE_NUGGET, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, nugget);
        BlockPos repairAbsolute = helper.absolutePos(repair);
        InteractionResult repaired = level.getBlockState(repairAbsolute).useItemOn(nugget, level, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(repairAbsolute), Direction.NORTH, repairAbsolute, false));
        helper.assertTrue(repaired.consumesAction(), "using an enderite nugget on a damaged enderite piston did nothing: " + repaired);
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(repair)), 0,
                "damage after the repair with an enderite nugget (full durability)");
        Assertions.valueEqual(helper, nugget.getCount(), 1, "enderite nuggets left after one repair outside creative");

        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    BlockState stonePiston = helper.getBlockState(stone);
                    helper.assertTrue(stonePiston.is(ModBlocks.ENDERITE_PISTON) && stonePiston.getValue(PistonBaseBlock.EXTENDED),
                            "the enderite piston on stone did not extend, found " + stonePiston);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(stonePiston), 1,
                            "enderite damage after breaking one stone");
                    BlockState deepslatePiston = helper.getBlockState(deepslate);
                    helper.assertTrue(deepslatePiston.is(ModBlocks.ENDERITE_PISTON) && deepslatePiston.getValue(PistonBaseBlock.EXTENDED),
                            "the enderite piston on deepslate did not extend, found " + deepslatePiston);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(deepslatePiston), 1,
                            "enderite damage after breaking one deepslate - one per block, not per hardness point");

                    BlockState crumbled = helper.getBlockState(crumble);
                    helper.assertTrue(crumbled.is(ModBlocks.NETHERITE_PISTON),
                            "an enderite piston with 1 durability left did not crumble into a netherite piston, found " + crumbled);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.durabilityOf(crumbled), 226,
                            "durability of the netherite piston the enderite piston crumbled into (full)");
                    Assertions.valueEqual(helper, crumbled.getValue(DirectionalBlock.FACING), Direction.UP,
                            "facing of the netherite piston the enderite piston crumbled into");
                    assertExtendedWithHead(helper, crumble, ModBlocks.NETHERITE_PISTON_HEAD, PistonType.DEFAULT,
                            "the crumbled enderite piston");

                    helper.setBlock(stone.below(), Blocks.AIR);
                })
                .thenExecuteAfter(6, () -> {
                    BlockState retracted = helper.getBlockState(stone);
                    helper.assertTrue(retracted.is(ModBlocks.ENDERITE_PISTON) && !retracted.getValue(PistonBaseBlock.EXTENDED),
                            "the enderite piston did not retract, found " + retracted);
                    Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(retracted), 1,
                            "enderite damage after retracting");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The durability travels with the item, as a durability bar:
     *
     * <ul>
     *   <li><b>Pick-up:</b> a damaged netherite piston broken by a player drops itself with
     *       {@code max_damage 226}, its {@code damage} and a stack size of 1 - vanilla then shows the
     *       bar ({@code isBarVisible}) in the hand and the inventory. A fresh one drops without them
     *       and still stacks to 64. The enderite piston does the same with 281.</li>
     *   <li><b>Placing</b> the damaged item puts the same damage back onto the block.</li>
     *   <li><b>Migration:</b> a legacy item ({@code block_state {wear:4}}) placed as it is gives the
     *       first damage of stage 4 (113 of 226, the same fraction); a single legacy item moves over
     *       to the durability components when it ticks in an inventory; a legacy stack of two keeps
     *       its component (it could not carry durability) but still reports the damage.</li>
     * </ul>
     */
    public static void breakerPistonDurabilityTravelsWithTheItemAsItsDurabilityBar(GameTestHelper helper) {
        BlockPos mined = new BlockPos(1, 1, 1);
        BlockPos placed = new BlockPos(3, 1, 1);
        BlockPos legacyPlaced = new BlockPos(5, 1, 1);
        ServerLevel level = helper.getLevel();

        // --- pick-up: broken by a player, with drops ---
        helper.setBlock(mined, NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.NETHERITE_PISTON), 100));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TestCleanup.before(helper, () -> level.getServer().getPlayerList().remove(player));
        player.setPos(helper.absoluteVec(new Vec3(6.5, 1.0, 6.5)));
        BlockPos minedAbsolute = helper.absolutePos(mined);
        level.destroyBlock(minedAbsolute, true, player);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(minedAbsolute).inflate(1.5));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModItems.NETHERITE_PISTON),
                "a damaged netherite piston broken by a player dropped " + drops + " instead of itself");
        ItemStack picked = drops.get(0).getItem().copy();
        drops.forEach(ItemEntity::discard);
        Assertions.valueEqual(helper, picked.get(DataComponents.MAX_DAMAGE), 226, "max_damage of the dropped netherite piston");
        Assertions.valueEqual(helper, picked.getDamageValue(), 100, "damage the dropped netherite piston carries");
        Assertions.valueEqual(helper, picked.getMaxStackSize(), 1, "stack size of a damaged netherite piston");
        helper.assertTrue(picked.isBarVisible(), "a damaged netherite piston shows no durability bar");
        Assertions.valueEqual(helper, picked.getBarWidth(), Math.round(13.0F - 100 * 13.0F / 226),
                "width of the durability bar at 126 of 226");

        List<ItemStack> freshDrops = Block.getDrops(upright(ModBlocks.NETHERITE_PISTON), level, minedAbsolute, null, null, ItemStack.EMPTY);
        helper.assertTrue(freshDrops.size() == 1 && !freshDrops.get(0).has(DataComponents.MAX_DAMAGE)
                        && !freshDrops.get(0).isBarVisible() && freshDrops.get(0).getMaxStackSize() == 64,
                "a fresh netherite piston dropped " + freshDrops + " with durability, so it no longer stacks with fresh ones");
        List<ItemStack> enderiteDrops = Block.getDrops(NetheriteBreakerPistonBlock.withDamage(upright(ModBlocks.ENDERITE_PISTON), 7),
                level, minedAbsolute, null, null, ItemStack.EMPTY);
        helper.assertTrue(enderiteDrops.size() == 1 && enderiteDrops.get(0).is(ModItems.ENDERITE_PISTON)
                        && enderiteDrops.get(0).isBarVisible(),
                "a damaged enderite piston dropped " + enderiteDrops + " without a durability bar");
        Assertions.valueEqual(helper, enderiteDrops.get(0).get(DataComponents.MAX_DAMAGE), 281, "max_damage of the dropped enderite piston");
        Assertions.valueEqual(helper, enderiteDrops.get(0).getDamageValue(), 7, "damage the dropped enderite piston carries");

        // --- placing the damaged item ---
        helper.setBlock(placed.below(), Blocks.STONE);
        helper.setBlock(placed, Blocks.AIR);
        placeFromHand(helper, player, picked, placed);
        BlockState placedState = helper.getBlockState(placed);
        helper.assertTrue(placedState.is(ModBlocks.NETHERITE_PISTON), "the damaged netherite piston item placed " + placedState);
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(placedState), 100,
                "damage of the netherite piston placed from an item with 100 damage - placing must not repair it");

        // --- migration of items from before the durability ---
        ItemStack legacy = new ItemStack(ModItems.NETHERITE_PISTON);
        legacy.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(NetheriteBreakerPistonBlock.WEAR, 4));
        Assertions.valueEqual(helper, NetheritePistonItem.damageOf(legacy), 113, "damage a legacy item at wear stage 4 reports");
        helper.setBlock(legacyPlaced.below(), Blocks.STONE);
        helper.setBlock(legacyPlaced, Blocks.AIR);
        placeFromHand(helper, player, legacy.copy(), legacyPlaced);
        Assertions.valueEqual(helper, NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(legacyPlaced)), 113,
                "damage of a netherite piston placed from a legacy item at wear stage 4");

        ItemStack ticked = legacy.copy();
        ticked.getItem().inventoryTick(ticked, level, player, null);
        helper.assertTrue(!ticked.has(DataComponents.BLOCK_STATE) && ticked.isBarVisible(),
                "a single legacy item did not move over to the durability bar in the inventory: " + ticked);
        Assertions.valueEqual(helper, ticked.getDamageValue(), 113, "damage of the migrated legacy item");
        Assertions.valueEqual(helper, ticked.get(DataComponents.MAX_DAMAGE), 226, "max_damage of the migrated legacy item");

        ItemStack legacyStack = legacy.copyWithCount(2);
        helper.assertTrue(!NetheritePistonItem.migrateLegacyWear(legacyStack) && legacyStack.getCount() == 2
                        && legacyStack.has(DataComponents.BLOCK_STATE) && !legacyStack.has(DataComponents.MAX_DAMAGE),
                "a legacy stack of two was changed although it cannot carry durability: " + legacyStack);
        Assertions.valueEqual(helper, NetheritePistonItem.damageOf(legacyStack), 113, "damage a legacy stack of two reports");

        TestCleanup.run(helper);
        helper.succeed();
    }

    /** Places {@code stack} from the player's main hand on top of the block below {@code target}. */
    private static void placeFromHand(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos target) {
        BlockPos below = helper.absolutePos(target.below());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
        helper.assertTrue(result.consumesAction(), "placing " + stack + " at " + target + " failed: " + result);
    }

    /** Tick budget for {@link #modPistonsFireTheRealLoaderEventsAndHonourTheirConfigSwitch}. */
    public static final int LOADER_EVENTS_MAX_TICKS = 60;

    /**
     * The platform guard fires the loader's <em>real</em> events, and a listener that cancels them
     * keeps the block (audit N5); the config switch {@code pistonsFireBreakEvents} turns the block
     * break event with the fake player off (audit N4). The listeners are ordinary ones each loader's
     * guard installer registers ({@code PistonEventProbe}), cancelling only the positions a test
     * names - the same way a protection mod would.
     *
     * <p>Which refusal has to keep the block depends on the loader, and the installer says which
     * events its guard fires: Fabric only the block break event ({@code PlayerBlockBreakEvents.BEFORE},
     * no piston event exists), Forge only {@code PistonEvent.Pre} (Forge 26.2 has no fake player),
     * NeoForge both ({@code PistonEvent.Pre}, {@code BreakBlockEvent}). Rows, each a netherite piston
     * on stone with a redstone block below:
     * <ul>
     *   <li><b>Break event cancelled at the stone:</b> with a break event the stone survives and is
     *       pushed; without (Forge) it is destroyed.</li>
     *   <li><b>Piston event cancelled at the piston:</b> with a piston event the stone survives and the
     *       piston does not even extend (vanilla's own {@code PistonEvent.Pre} is cancelled too);
     *       without (Fabric) it is destroyed.</li>
     *   <li><b>Break event cancelled, {@code pistonsFireBreakEvents} off:</b> destroyed on every loader.</li>
     *   <li><b>Control, nothing cancelled:</b> destroyed.</li>
     * </ul>
     */
    public static void modPistonsFireTheRealLoaderEventsAndHonourTheirConfigSwitch(GameTestHelper helper) {
        BlockPos breakRow = new BlockPos(1, 1, 1);
        BlockPos pistonRow = new BlockPos(4, 1, 1);
        BlockPos switchedOff = new BlockPos(1, 1, 5);
        BlockPos control = new BlockPos(4, 1, 5);
        ServerLevel level = helper.getLevel();
        AABB room = helper.getBounds();
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null, "the mod has no live config, so the switch cannot be flipped");
        boolean fireBefore = config.pistonsFireBreakEvents;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().pistonsFireBreakEvents = fireBefore);
        boolean pistonEvent = PistonEventProbe.firesPistonEvent();
        boolean breakEvent = PistonEventProbe.firesBreakEvent();
        helper.assertTrue(pistonEvent || breakEvent,
                "the loader's piston break guard declared no event at all - no guard is installed, so protection "
                        + "mods are never asked");

        List<String> reports = new java.util.concurrent.CopyOnWriteArrayList<>();
        Runnable stop = PistonBoreEffects.observe((where, pos, broken, dropped) -> {
            if (where == level && room.contains(Vec3.atCenterOf(pos))) {
                reports.add(pos.toShortString());
            }
        });
        TestCleanup.before(helper, stop);
        TestCleanup.before(helper, PistonEventProbe.refuseBreakAt(helper.absolutePos(breakRow.above())));
        TestCleanup.before(helper, PistonEventProbe.refusePistonAt(helper.absolutePos(pistonRow)));
        TestCleanup.before(helper, PistonEventProbe.refuseBreakAt(helper.absolutePos(switchedOff.above())));

        config.pistonsFireBreakEvents = true;
        paidRow(helper, breakRow, ModBlocks.NETHERITE_PISTON, Blocks.STONE);
        helper.setBlock(breakRow.above(3), Blocks.AIR);
        paidRow(helper, pistonRow, ModBlocks.NETHERITE_PISTON, Blocks.STONE);
        paidRow(helper, control, ModBlocks.NETHERITE_PISTON, Blocks.STONE);

        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    String breakTarget = helper.absolutePos(breakRow.above()).toShortString();
                    if (breakEvent) {
                        helper.assertTrue(!reports.contains(breakTarget),
                                "a cancelled block break event did not keep the stone - the guard ignores the loader's "
                                        + "break listeners");
                        helper.assertTrue(helper.getBlockState(breakRow.above(2)).is(Blocks.STONE),
                                "the stone whose break was cancelled was not pushed like by a vanilla piston, found "
                                        + helper.getBlockState(breakRow.above(2)));
                    } else {
                        helper.assertTrue(reports.contains(breakTarget),
                                "this loader declares no block break event, yet the stone survived");
                    }

                    String pistonTarget = helper.absolutePos(pistonRow.above()).toShortString();
                    if (pistonEvent) {
                        helper.assertTrue(!reports.contains(pistonTarget)
                                        && helper.getBlockState(pistonRow.above()).is(Blocks.STONE)
                                        && !helper.getBlockState(pistonRow).getValue(PistonBaseBlock.EXTENDED),
                                "a cancelled PistonEvent.Pre did not keep the stone in place and the piston retracted: "
                                        + helper.getBlockState(pistonRow.above()) + " / " + helper.getBlockState(pistonRow));
                    } else {
                        helper.assertTrue(reports.contains(pistonTarget),
                                "this loader declares no piston event, yet the stone survived");
                    }

                    helper.assertTrue(reports.contains(helper.absolutePos(control.above()).toShortString()),
                            "the control breaker did not break its stone - the rig itself is broken");

                    // --- the switch: no block break event with the fake player any more ---
                    Simplebuilding.getConfig().pistonsFireBreakEvents = false;
                    paidRow(helper, switchedOff, ModBlocks.NETHERITE_PISTON, Blocks.STONE);
                })
                .thenExecuteAfter(4, () -> {
                    Simplebuilding.getConfig().pistonsFireBreakEvents = fireBefore;
                    helper.assertTrue(reports.contains(helper.absolutePos(switchedOff.above()).toShortString()),
                            "with pistonsFireBreakEvents off a cancelling break listener still stopped the breaker - "
                                    + "the switch does not reach the loader's guard");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * "Unbreakable" means vanilla's unbreakables (audit #24): a destroy speed below 0 counts only in
     * the {@code minecraft} namespace (or through the tag {@code piston_breachable_extra}); another
     * mod's hardness -1 block is left alone unless {@code pistonsBreachModdedUnbreakables} is on.
     * No modded unbreakable is registered in the test world, so the namespace rule is asked for a
     * mod block directly ({@code PistonBreach#unbreakableCounts}); bedrock is the live control.
     *
     * <p>And the client half of the piston options (audit N16): the server sends both options at
     * login ({@code PistonConfigPayload}), a client level reads only those; a server level never
     * does, whatever a client stored.
     */
    public static void onlyVanillaUnbreakablesAreBreachedUnlessTheConfigSaysOtherwise(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null, "the mod has no live config, so the option cannot be switched");
        boolean moddedBefore = config.pistonsBreachModdedUnbreakables;
        TestCleanup.before(helper, () -> {
            Simplebuilding.getConfig().pistonsBreachModdedUnbreakables = moddedBefore;
            PistonBreach.setClientRules(null);
        });

        config.pistonsBreachModdedUnbreakables = false;
        helper.assertTrue(PistonBreach.unbreakableCounts(Blocks.BEDROCK, level),
                "bedrock (minecraft namespace) no longer counts as a breachable unbreakable");
        helper.assertTrue(!PistonBreach.unbreakableCounts(ModBlocks.REINFORCED_PISTON, level),
                "a block outside the minecraft namespace counts as a breachable unbreakable with "
                        + "pistonsBreachModdedUnbreakables off");
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.BEDROCK);
        helper.assertTrue(PistonBreach.isBreachable(level, helper.absolutePos(new BlockPos(1, 1, 1))),
                "bedrock in the world is no longer breachable");

        PistonConfigPayload sent = PistonConfigPayload.fromServer();
        helper.assertTrue(!sent.moddedUnbreakables() && sent.endPortalFrames() == config.pistonsBreachEndPortalFrames,
                "the login payload does not carry the server's piston options: " + sent);

        config.pistonsBreachModdedUnbreakables = true;
        helper.assertTrue(PistonBreach.unbreakableCounts(ModBlocks.REINFORCED_PISTON, level),
                "with pistonsBreachModdedUnbreakables on, another namespace's unbreakables still do not count");
        helper.assertTrue(PistonConfigPayload.fromServer().moddedUnbreakables(),
                "the login payload does not follow the switched option");

        // A client's stored values never reach a server level.
        new PistonConfigPayload(false, false).apply();
        helper.assertTrue(PistonBreach.unbreakableCounts(ModBlocks.REINFORCED_PISTON, level),
                "the values a client received decided a question on the server level");
        config.pistonsBreachModdedUnbreakables = false;
        new PistonConfigPayload(true, true).apply();
        helper.assertTrue(!PistonBreach.unbreakableCounts(ModBlocks.REINFORCED_PISTON, level),
                "the values a client received decided a question on the server level");
        PistonBreach.setClientRules(null);
        TestCleanup.succeed(helper);
    }

    /**
     * {@code simplebuilding:piston_breach_immune} holds what no piston may ever breach, paid or not:
     * every row here has a redstone block behind the piston, and every target has to stay.
     *
     * <ul>
     *   <li><b>A light block</b> (destroy speed -1, no block entity, ordinary push reaction - so only
     *       the tag keeps it out) in front of a reinforced piston and in front of a netherite piston.</li>
     *   <li><b>A command block</b> in front of a netherite piston (tag and block entity).</li>
     *   <li><b>A barrier</b> in front of a reinforced piston and in front of an enderite piston.</li>
     *   <li><b>Control:</b> bedrock in front of a reinforced piston in the same room is pushed, so the
     *       paying rig works and the refusals above are the tag's.</li>
     * </ul>
     * The redstone blocks of the refused rows stay, and so do the netherite and enderite pistons.
     *
     * <p>What breaks this test: dropping the immune tag check from {@code PistonBreach#isBreachable},
     * removing light or barrier from the tag, or a breach that ignores both.
     */
    public static void immuneBlocksNeverMoveOrBreak(GameTestHelper helper) {
        BlockPos reinforcedLight = new BlockPos(1, 1, 1);
        BlockPos netheriteLight = new BlockPos(3, 1, 1);
        BlockPos netheriteCommand = new BlockPos(5, 1, 1);
        BlockPos reinforcedBarrier = new BlockPos(1, 1, 4);
        BlockPos enderiteBarrier = new BlockPos(3, 1, 4);
        BlockPos control = new BlockPos(5, 1, 4);

        paidRow(helper, reinforcedLight, ModBlocks.REINFORCED_PISTON, Blocks.LIGHT);
        paidRow(helper, netheriteLight, ModBlocks.NETHERITE_PISTON, Blocks.LIGHT);
        paidRow(helper, netheriteCommand, ModBlocks.NETHERITE_PISTON, Blocks.COMMAND_BLOCK);
        paidRow(helper, reinforcedBarrier, ModBlocks.REINFORCED_PISTON, Blocks.BARRIER);
        paidRow(helper, enderiteBarrier, ModBlocks.ENDERITE_PISTON, Blocks.BARRIER);
        paidRow(helper, control, ModBlocks.REINFORCED_PISTON, Blocks.BEDROCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(helper.getBlockState(control.above(2)).is(Blocks.BEDROCK),
                            "the control row did not push its bedrock, so the paying rig in this room is "
                                    + "broken and the refusals below prove nothing");

                    assertImmuneRowUntouched(helper, reinforcedLight, ModBlocks.REINFORCED_PISTON, Blocks.LIGHT,
                            "a reinforced piston moved the light block, which piston_breach_immune lists");
                    assertImmuneRowUntouched(helper, netheriteLight, ModBlocks.NETHERITE_PISTON, Blocks.LIGHT,
                            "a netherite piston breached the light block, which piston_breach_immune lists");
                    assertImmuneRowUntouched(helper, netheriteCommand, ModBlocks.NETHERITE_PISTON, Blocks.COMMAND_BLOCK,
                            "a netherite piston breached a command block");
                    assertImmuneRowUntouched(helper, reinforcedBarrier, ModBlocks.REINFORCED_PISTON, Blocks.BARRIER,
                            "a reinforced piston moved a barrier");
                    assertImmuneRowUntouched(helper, enderiteBarrier, ModBlocks.ENDERITE_PISTON, Blocks.BARRIER,
                            "an enderite piston breached a barrier");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * A breached block spends two ticks inside a {@code minecraft:moving_piston}, and that block has an
     * explosion resistance of 0. {@code MovingPistonExplosionMixin} makes the moving piston ignore an
     * explosion while it carries an unbreakable block - otherwise a TNT blast during those two ticks
     * would delete bedrock for the price of one redstone block.
     *
     * <p>Two moving pistons are built by hand side by side, one carrying bedrock, one carrying stone,
     * and a small explosion goes off exactly between them. The stone one is the control: it has to be
     * gone, which proves the blast reached both. The bedrock one has to survive the blast and, a few
     * ticks later, arrive as bedrock.
     *
     * <p>What breaks this test: removing the {@code ci.cancel()} of the mixin, or a guard that asks the
     * moving piston's own hardness instead of the hardness of the block it carries.
     */
    public static void explosionsCannotDeleteUnbreakableBlocksWhileTheyMove(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bedrock = new BlockPos(3, 2, 3);
        BlockPos stone = new BlockPos(5, 2, 3);
        placeMovingBlock(helper, bedrock, Blocks.BEDROCK.defaultBlockState());
        placeMovingBlock(helper, stone, Blocks.STONE.defaultBlockState());

        BlockPos centre = helper.absolutePos(new BlockPos(4, 2, 3));
        level.explode(null, centre.getX() + 0.5D, centre.getY() + 0.5D, centre.getZ() + 0.5D,
                BLAST_POWER, Level.ExplosionInteraction.BLOCK);

        helper.assertTrue(!helper.getBlockState(stone).is(Blocks.MOVING_PISTON),
                "the moving stone survived the blast next to it, so the blast is too weak to say "
                        + "anything about the moving bedrock");
        helper.assertTrue(helper.getBlockState(bedrock).is(Blocks.MOVING_PISTON)
                        || helper.getBlockState(bedrock).is(Blocks.BEDROCK),
                "the explosion deleted the bedrock while it was moving, found "
                        + helper.getBlockState(bedrock));

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(helper.getBlockState(bedrock).is(Blocks.BEDROCK),
                            "the moving bedrock never arrived after the blast, found "
                                    + helper.getBlockState(bedrock));
                    helper.assertBlockNotPresent(Blocks.STONE, stone);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // THE CONFIG OPTION
    // =====================================================================================

    /**
     * {@code pistonsBreachEndPortalFrames} decides whether end portal frames count as breachable. Both
     * of its consumers are driven with the option off and on:
     * <ul>
     *   <li><b>The reinforced piston's resolver</b>, asked directly: with the option off it refuses the
     *       frame, with the option on it accepts it.</li>
     *   <li><b>The netherite piston's block event</b>, driven synchronously: with the option off nothing
     *       happens - frame, redstone block and piston stay; with the option on the frame, the redstone
     *       block and the piston are gone.</li>
     * </ul>
     *
     * <p>The option is global and the suite runs in one world, so everything happens inside this one
     * call - write, check, restore - and the previous value is put back in a {@code finally}. The
     * redstone blocks and frames are placed without neighbour updates, so neither piston acts on its own
     * before the test asks it to.
     *
     * <p>What breaks this test: {@code PistonBreach} ignoring the option, or reading it inverted.
     */
    public static void endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null, "the mod has no live config, so the option cannot be switched");
        boolean original = config.pistonsBreachEndPortalFrames;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().pistonsBreachEndPortalFrames = original);

        BlockPos reinforced = new BlockPos(1, 1, 1);
        BlockPos netherite = new BlockPos(4, 1, 1);
        helper.setBlock(reinforced, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(netherite, upright(ModBlocks.NETHERITE_PISTON));
        placeQuietly(helper, reinforced.above(), Blocks.END_PORTAL_FRAME.defaultBlockState());
        placeQuietly(helper, netherite.above(), Blocks.END_PORTAL_FRAME.defaultBlockState());
        placeQuietly(helper, reinforced.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        placeQuietly(helper, netherite.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());

        BlockPos reinforcedAbsolute = helper.absolutePos(reinforced);
        BlockPos netheriteAbsolute = helper.absolutePos(netherite);
        try {
            // --- switched off ---
            config.pistonsBreachEndPortalFrames = false;
            helper.assertFalse(PistonBreach.isBreachable(level, helper.absolutePos(reinforced.above())),
                    "with pistonsBreachEndPortalFrames switched off the end portal frame still counts as "
                            + "breachable");
            helper.assertFalse(new PistonStructureResolver(level, reinforcedAbsolute, Direction.UP, true).resolve(),
                    "with pistonsBreachEndPortalFrames switched off the reinforced piston would still push "
                            + "the end portal frame");
            BlockState netheriteState = level.getBlockState(netheriteAbsolute);
            netheriteState.triggerEvent(level, netheriteAbsolute, 0, Direction.UP.get3DDataValue());
            helper.assertTrue(helper.getBlockState(netherite.above()).is(Blocks.END_PORTAL_FRAME)
                            && helper.getBlockState(netherite).is(ModBlocks.NETHERITE_PISTON)
                            && helper.getBlockState(netherite.below()).is(Blocks.REDSTONE_BLOCK),
                    "with pistonsBreachEndPortalFrames switched off the netherite piston still breached "
                            + "the end portal frame");

            // --- switched on ---
            config.pistonsBreachEndPortalFrames = true;
            helper.assertTrue(new PistonStructureResolver(level, reinforcedAbsolute, Direction.UP, true).resolve(),
                    "with pistonsBreachEndPortalFrames switched on the reinforced piston refuses the end "
                            + "portal frame");
            netheriteState = level.getBlockState(netheriteAbsolute);
            netheriteState.triggerEvent(level, netheriteAbsolute, 0, Direction.UP.get3DDataValue());
            helper.assertTrue(!helper.getBlockState(netherite.above()).is(Blocks.END_PORTAL_FRAME)
                            && !helper.getBlockState(netherite).is(ModBlocks.NETHERITE_PISTON)
                            && !helper.getBlockState(netherite.below()).is(Blocks.REDSTONE_BLOCK),
                    "with pistonsBreachEndPortalFrames switched on the netherite piston did not breach the "
                            + "end portal frame");
        } finally {
            config.pistonsBreachEndPortalFrames = original;
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // THE REINFORCED STICKY PISTON AS AN ITEM
    // =====================================================================================

    /**
     * The reinforced sticky piston is made the way vanilla makes its sticky piston - a slime ball on
     * top of the piston - and it drops itself when it is broken.
     *
     * <ul>
     *   <li><b>Slime ball over reinforced piston</b> crafts exactly one reinforced sticky piston.</li>
     *   <li><b>The same two items upside down</b> craft nothing - the recipe is shaped.</li>
     *   <li><b>Slime ball over a vanilla piston</b> still crafts vanilla's sticky piston, so the mod's
     *       recipe has not shadowed vanilla's.</li>
     *   <li><b>Loot:</b> a placed reinforced sticky piston broken with drops leaves its own item, and it
     *       is mineable with a pickaxe.</li>
     * </ul>
     *
     * <p>What breaks this test: a changed key, pattern or count in
     * {@code recipe/reinforced_sticky_piston.json}, a loot table that drops something else, and the
     * block falling out of {@code mineable/pickaxe}.
     */
    public static void reinforcedStickyPistonCraftsFromSlimeAndDropsItself(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        CraftingInput asShipped = CraftingInput.of(1, 2, List.of(
                new ItemStack(Items.SLIME_BALL), new ItemStack(ModItems.REINFORCED_PISTON)));
        ItemStack crafted = craft(helper, level, asShipped, "slime ball over a reinforced piston");
        helper.assertTrue(crafted.is(ModItems.REINFORCED_STICKY_PISTON),
                "slime ball over a reinforced piston crafts " + crafted + " instead of a reinforced "
                        + "sticky piston");
        Assertions.valueEqual(helper, crafted.getCount(), 1, "reinforced sticky pistons per craft");

        CraftingInput upsideDown = CraftingInput.of(1, 2, List.of(
                new ItemStack(ModItems.REINFORCED_PISTON), new ItemStack(Items.SLIME_BALL)));
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, upsideDown, level).isEmpty(),
                "the reinforced piston under a slime ball crafts something as well, so the sticky "
                        + "recipe is not shaped");

        CraftingInput vanilla = CraftingInput.of(1, 2, List.of(
                new ItemStack(Items.SLIME_BALL), new ItemStack(Items.PISTON)));
        ItemStack vanillaSticky = craft(helper, level, vanilla, "slime ball over a vanilla piston");
        helper.assertTrue(vanillaSticky.is(Items.STICKY_PISTON),
                "slime ball over a vanilla piston no longer crafts vanilla's sticky piston but " + vanillaSticky);

        // --- loot: it drops itself ---
        BlockPos placed = new BlockPos(2, 1, 2);
        helper.setBlock(placed, upright(ModBlocks.REINFORCED_STICKY_PISTON));
        level.destroyBlock(helper.absolutePos(placed), true);
        helper.assertTrue(countDropped(helper, ModItems.REINFORCED_STICKY_PISTON) == 1,
                "the broken reinforced sticky piston did not drop itself");
        helper.assertTrue(ModBlocks.REINFORCED_STICKY_PISTON.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "the reinforced sticky piston is not in minecraft:mineable/pickaxe");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static BlockState upright(Block piston) {
        return facing(piston, Direction.UP);
    }

    private static BlockState facing(Block piston, Direction direction) {
        return piston.defaultBlockState().setValue(DirectionalBlock.FACING, direction);
    }

    /** An upright piston, its target above, then the paying redstone block below it. */
    private static void paidRow(GameTestHelper helper, BlockPos piston, Block pistonBlock, Block target) {
        helper.setBlock(piston.above(2), Blocks.AIR);
        helper.setBlock(piston, upright(pistonBlock));
        helper.setBlock(piston.above(), target);
        helper.setBlock(piston.below(), Blocks.REDSTONE_BLOCK);
    }

    /** An upright piston, its target above, and a switched-on lever beside it instead of a redstone block. */
    private static void leverRow(GameTestHelper helper, BlockPos piston, Block pistonBlock, Block target, Direction side) {
        helper.setBlock(piston.above(2), Blocks.AIR);
        helper.setBlock(piston, upright(pistonBlock));
        helper.setBlock(piston.above(), target);
        placeLever(helper, piston.relative(side));
    }

    /** A switched-on lever standing on a stone block. */
    private static void placeLever(GameTestHelper helper, BlockPos lever) {
        helper.setBlock(lever.below(), Blocks.STONE);
        helper.setBlock(lever, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, Boolean.TRUE));
    }

    /** A reinforced piston with a breachable bedrock in front and a stone column of the given height behind it. */
    private static void buildBreachColumn(GameTestHelper helper, BlockPos piston, int stones) {
        for (int y = 1; y <= stones + 4; y++) {
            helper.setBlock(piston.above(y), Blocks.AIR);
        }
        helper.setBlock(piston, upright(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(piston.above(), Blocks.BEDROCK);
        for (int y = 2; y <= stones + 1; y++) {
            helper.setBlock(piston.above(y), Blocks.STONE);
        }
    }

    /** Sets a block without telling its neighbours, so no piston next to it wakes up. */
    private static void placeQuietly(GameTestHelper helper, BlockPos pos, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(pos), state, Block.UPDATE_CLIENTS);
    }

    /** A moving piston at {@code pos} that is carrying {@code carried} upwards into that very cell. */
    private static void placeMovingBlock(GameTestHelper helper, BlockPos pos, BlockState carried) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState moving = Blocks.MOVING_PISTON.defaultBlockState()
                .setValue(MovingPistonBlock.FACING, Direction.UP)
                .setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);
        helper.getLevel().setBlock(absolute, moving, Block.UPDATE_ALL);
        helper.getLevel().setBlockEntity(MovingPistonBlock.newMovingBlockEntity(absolute, moving, carried,
                Direction.UP, true, false));
    }

    /** Neither a head nor a moving block may be left in front of a sacrificed piston. */
    private static void assertNothingMovedIn(GameTestHelper helper, BlockPos front) {
        helper.assertTrue(!(helper.getBlockState(front).getBlock() instanceof PistonHeadBlock)
                        && !helper.getBlockState(front).is(Blocks.MOVING_PISTON),
                "the sacrifice left " + helper.getBlockState(front) + " in front of the piston - it ran "
                        + "moveBlocks instead of deleting the target");
    }

    private static void assertImmuneRowUntouched(GameTestHelper helper, BlockPos piston, Block pistonBlock,
                                                 Block target, String message) {
        helper.assertTrue(helper.getBlockState(piston.above()).is(target)
                        && helper.getBlockState(piston).is(pistonBlock)
                        && helper.getBlockState(piston.below()).is(Blocks.REDSTONE_BLOCK),
                message + " (target " + helper.getBlockState(piston.above()) + ", piston "
                        + helper.getBlockState(piston) + ", payer " + helper.getBlockState(piston.below()) + ")");
    }

    private static void assertExtendedWithHead(GameTestHelper helper, BlockPos piston, Block headBlock, PistonType type,
                                               String label) {
        BlockState head = helper.getBlockState(piston.above());
        helper.assertTrue(helper.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED)
                        && head.is(headBlock) && head.getValue(PistonHeadBlock.TYPE) == type
                        && head.getValue(DirectionalBlock.FACING) == Direction.UP,
                label + " did not extend with a " + type.getSerializedName() + " " + headBlock
                        + " facing up in front of it, found " + head);
    }

    private static void assertPicksItsPiston(GameTestHelper helper, BlockPos piston, Item item) {
        BlockPos head = helper.absolutePos(piston.above());
        ItemStack picked = helper.getLevel().getBlockState(head).getCloneItemStack(helper.getLevel(), head, false);
        helper.assertTrue(picked.is(item), "picking the head of " + helper.getBlockState(piston) + " gave " + picked
                + " instead of " + item);
    }

    private static void assertBrokeWithItsHead(GameTestHelper helper, BlockPos piston, Block pistonBlock, Item item,
                                               String label) {
        helper.assertTrue(!helper.getBlockState(piston).is(pistonBlock),
                label + " survived the loss of its head - a headless extended base is exactly what the "
                        + "head fix is there to prevent");
        helper.assertTrue(countDropped(helper, item) == 1,
                label + " broke with its head but did not drop its item");
    }

    /** How many of {@code item} lie in the room as item entities; the bounds stay uninflated. */
    private static int countDropped(GameTestHelper helper, Item item) {
        int total = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }

    private static void fillFloor(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    /** The one place a crafting result is assembled; the assemble signature differs between the lines. */
    private static ItemStack craft(GameTestHelper helper, ServerLevel level, CraftingInput grid, String what) {
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(), what + " crafts nothing at all");
        return match.get().value().assemble(grid, level.registryAccess());
    }
}
