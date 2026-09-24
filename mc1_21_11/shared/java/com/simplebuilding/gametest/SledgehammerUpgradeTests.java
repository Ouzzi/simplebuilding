package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.util.SledgehammerProgress;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The in-world machine upgrade: a sledgehammer in the main hand, a nugget in the off hand, right
 * click held on a placed machine for five seconds ({@code SledgehammerUpgrades}).
 *
 * <p>Reinforced to netherite takes a netherite nugget and at least a diamond sledgehammer, netherite
 * to enderite an enderite nugget and at least a netherite one; the hopper, the furnace, the smoker,
 * the blast furnace and the (plain) piston all climb both steps. Every test drives the real path a
 * click takes: {@code ServerPlayerGameMode#useItemOn} - which asks the block first, so a mod furnace
 * or hopper that opened its menu instead of passing the click on would stop the upgrade before it
 * starts - and then vanilla's own player tick through {@code player.connection.tick()}, the only
 * place that counts an item use down. The gametest server never pumps a mock player's connection by
 * itself, so each test does it, one player tick at a time, and looks at the rig after every single
 * one. That is what pins the timing: four blows on ticks 20, 40, 60 and 80, the fifth - the upgrade
 * - on tick 100, and not a durability point spent in between.
 *
 * <p><b>The numbers are written out here, not read from the mod.</b> 100 ticks, a blow every 20,
 * four durability per blow on the netherite step and ten on the enderite step are this file's own
 * constants: a test that read {@code SledgehammerUpgrades.NETHERITE_DAMAGE_PER_HIT} would follow any
 * edit of it and prove nothing.
 *
 * <p><b>Who pays.</b> The player is the in-level mock (it has a connection, which the swing, the
 * sounds and the hammer cooldown all send packets through). Its {@code gameMode()} is hard wired to
 * creative, but what decides whether a nugget is consumed and durability is spent is
 * {@code hasInfiniteMaterials()}, i.e. the {@code instabuild} ability - so clearing that turns it into
 * a paying player for every test but the creative one.
 *
 * <p><b>One claim, one assertion.</b> Where several values make up one claim - what a furnace holds,
 * where an interrupted attempt leaves the rig - they are compared as one summary line, so the
 * failure message shows all of them and no assertion hides behind another.
 *
 * <p><b>Not covered:</b> the sounds, particles and arm swings of the blows (their packets go out, but
 * nothing here listens), the hint in the action bar on a refusal or on the progress, the cracks the
 * saved progress shows, the client half of the job map and what the first and third person
 * animation draw (their timing is pinned here, the picture in {@code ItemRenderingClientTest}).
 */
public final class SledgehammerUpgradeTests {

    private SledgehammerUpgradeTests() {
    }

    /** Tick budget for {@link #upgradedFurnaceKeepsCookingTheSameItemAtTheNewPace}. */
    public static final int FURNACE_PACE_MAX_TICKS = 60;

    /** Tick budget for {@link #refusedUpgradesNeverStartTheHammer}. */
    public static final int REFUSAL_MAX_TICKS = 40;

    /** Tick budget for {@link #upgradeProgressLastsUntilTheBlockChanges}. */
    public static final int PROGRESS_MAX_TICKS = 60;

    /** A little more than the one second between two checks of the saved progress. */
    private static final int PROGRESS_CHECK_TICKS = 25;

    /** The hammer draws back through 16 of the 20 ticks of every blow and swings through the rest. */
    private static final int DRAW_TICKS = 16;

    /** Length of one upgrade: five seconds. */
    private static final int UPGRADE_TICKS = 100;

    /** One blow per second. */
    private static final int HIT_EVERY = 20;

    /** Five blows in all: four on the way, the fifth is the upgrade itself. */
    private static final int BLOWS = 5;

    /** Durability every blow costs on the step to netherite, and on the step to enderite. */
    private static final int NETHERITE_STEP_WEAR = 4;
    private static final int ENDERITE_STEP_WEAR = 10;

    /** How many ticks the server lets the aim wander off the block before it gives up. */
    private static final int SERVER_AIM_GRACE = 2;

    /** Nuggets in the off hand at the start of every case, so "exactly one" is measurable. */
    private static final int NUGGETS = 2;

    /** This mod's namespace, for the recipe and advancement lookups by id. */
    private static final String MOD_ID = "simplebuilding";

    /** A machine family with its three tiers and the reinforced state it is placed in. */
    private record Family(String label, Block reinforced, Block netherite, Block enderite, BlockState placed) {
    }

    /**
     * The five families. Every one is placed facing a direction other than its default, so "the
     * facing survived" cannot pass by the upgraded block simply falling back to its default state.
     */
    private static final List<Family> FAMILIES = List.of(
            new Family("hopper", ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER,
                    ModBlocks.REINFORCED_HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST)),
            new Family("furnace", ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE,
                    ModBlocks.REINFORCED_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST)),
            new Family("smoker", ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER,
                    ModBlocks.REINFORCED_SMOKER.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST)),
            new Family("blast furnace", ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE,
                    ModBlocks.ENDERITE_BLAST_FURNACE,
                    ModBlocks.REINFORCED_BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST)),
            new Family("piston", ModBlocks.REINFORCED_PISTON, ModBlocks.NETHERITE_PISTON, ModBlocks.ENDERITE_PISTON,
                    ModBlocks.REINFORCED_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST)));

    /** Five spots, two blocks apart; the hopper points east into the empty cell beside it. */
    private static final List<BlockPos> FAMILY_SPOTS = List.of(
            new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1),
            new BlockPos(1, 1, 4), new BlockPos(3, 1, 4));

    /** Seven spots for the interruption cases, two blocks apart. */
    private static final List<BlockPos> CASE_SPOTS = List.of(
            new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1),
            new BlockPos(1, 1, 4), new BlockPos(3, 1, 4), new BlockPos(5, 1, 4),
            new BlockPos(1, 1, 7));

    /** What an interruption case does to the rig right before a given player tick. */
    private interface Interference {
        void before(int tick);
    }

    // =====================================================================================
    // THE UPGRADE ITSELF
    // =====================================================================================

    /**
     * Each of the five families is hammered from reinforced to netherite with a diamond
     * sledgehammer and a netherite nugget, and then from netherite to enderite with a netherite
     * sledgehammer and an enderite nugget - the weakest hammer each step accepts, so the gate is met
     * exactly and not by a margin.
     *
     * <p>Per step: the click reaches the hammer (the machine's menu does not open) and starts a job of
     * exactly 100 ticks; after every player tick the hammer has paid {@code wear x (ticks / 20)}
     * durability, 4 per blow on the first step and 10 on the second, and is still at it; after tick
     * 100 the block is the next tier with every block state property carried over (the non-default
     * facing above all, and LIT, ENABLED or EXTENDED), the hammer has paid five blows, exactly one of
     * the two nuggets is gone, the job is closed and the hammer cools down - so the right click that
     * is still held does not open the fresh machine's menu. The three bricks put into each machine's
     * first slot come out of both steps: the block entity was kept, not rebuilt.
     *
     * <p>What breaks this test: a blow on another tick or with another durability cost, a different
     * upgrade length, a missing or wrong entry in the upgrade table, a hammer gate above diamond or
     * netherite, the nugget not consumed or consumed twice, {@code withPropertiesOf} dropped from
     * the swap, {@code shouldChangedStateKeepBlockEntity} dropped from one of the four block classes
     * (vanilla then removes the block entity and throws its contents out), a block entity type that
     * does not accept the next tier (the swap then throws), a {@code useItemOn} that no longer skips
     * the menu, a job that outlives the upgrade, and the finishing cooldown dropped.
     */
    public static void everyMachineClimbsFromReinforcedToNetheriteToEnderite(GameTestHelper helper) {
        ServerPlayer player = smith(helper, false);

        for (int index = 0; index < FAMILIES.size(); index++) {
            Family family = FAMILIES.get(index);
            BlockPos pos = FAMILY_SPOTS.get(index);
            helper.setBlock(pos, family.placed());
            helper.assertTrue(!family.placed().equals(family.reinforced().defaultBlockState()),
                    "test setup broken: the " + family.label() + " is placed in its default state, so a lost "
                            + "facing would go unnoticed");
            BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (entity instanceof Container container) {
                container.setItem(0, new ItemStack(Items.BRICK, 3));
            }

            hammerThrough(helper, player, pos, family.reinforced(), family.netherite(),
                    ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, NETHERITE_STEP_WEAR,
                    "the reinforced " + family.label());
            assertBricksKept(helper, pos, "the " + family.label() + " after the netherite step");

            hammerThrough(helper, player, pos, family.netherite(), family.enderite(),
                    ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, ENDERITE_STEP_WEAR,
                    "the netherite " + family.label());
            assertBricksKept(helper, pos, "the " + family.label() + " after the enderite step");
        }

        TestCleanup.succeed(helper);
    }

    /**
     * A reinforced furnace is lit and ten ticks into a smelt when it is hammered to netherite.
     * Nothing the furnace was doing may be lost: cooking progress, input, fuel and output read the
     * same right after the swap as right before it. Five ticks later the kept block entity cooks on at
     * the netherite pace, four points of progress per tick instead of the reinforced two - the ticker
     * belongs to the new block now.
     *
     * <p>What breaks this test: {@code shouldChangedStateKeepBlockEntity} dropped from
     * {@code ModFurnaceBlock} (a fresh furnace entity starts at zero with an empty inventory, and the
     * old one's contents land on the floor), and a netherite furnace boost other than three.
     */
    public static void upgradedFurnaceKeepsCookingTheSameItemAtTheNewPace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.REINFORCED_FURNACE.defaultBlockState()
                .setValue(AbstractFurnaceBlock.FACING, Direction.EAST));
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        furnace.setItem(1, new ItemStack(Items.COAL, 8));
        ServerPlayer player = smith(helper, false);
        int[] progressAtUpgrade = {-1};

        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertBlockProperty(pos, AbstractFurnaceBlock.LIT, Boolean.TRUE);
                    progressAtUpgrade[0] = persistedInt(helper, pos, "cooking_time_spent");
                    helper.assertTrue(progressAtUpgrade[0] > 0,
                            "test setup broken: the reinforced furnace has not started cooking after 10 ticks");
                    String before = furnaceSummary(helper, pos);

                    hammerThrough(helper, player, pos, ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE,
                            ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, NETHERITE_STEP_WEAR,
                            "the lit reinforced furnace");

                    Assertions.valueEqual(helper, furnaceSummary(helper, pos), before,
                            "what the furnace holds and how far it has cooked, right after the upgrade");
                })
                .thenExecuteAfter(5, () -> Assertions.valueEqual(helper, 
                        persistedInt(helper, pos, "cooking_time_spent") - progressAtUpgrade[0], 5 * 4,
                        "cooking progress the upgraded furnace made in its first 5 ticks as a netherite furnace "
                                + "(1 from vanilla plus 3 from the netherite boost per tick)"))
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * A reinforced hopper with stock and an Exact Match filter is hammered to netherite and on to
     * enderite. It comes out of both steps with the same filter mode, filter item and stock.
     *
     * <p>What breaks this test: {@code shouldChangedStateKeepBlockEntity} dropped from
     * {@code ModHopperBlock}, and a hopper block entity type that does not list the next tier.
     */
    public static void upgradedHopperKeepsItsItemsFilterAndMode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.REINFORCED_HOPPER.defaultBlockState()
                .setValue(HopperBlock.FACING, Direction.EAST)
                .setValue(HopperBlock.ENABLED, Boolean.TRUE));
        ModHopperBlockEntity hopper = helper.getBlockEntity(pos, ModHopperBlockEntity.class);
        // Stock first, while the filter is still off, so the stack cannot teach slot 1 a filter item.
        hopper.setItem(1, new ItemStack(Items.COBBLESTONE, 7));
        hopper.toggleFilterMode();
        hopper.setGhostItem(0, new ItemStack(Items.DIAMOND));
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "test setup broken: one toggle should reach Exact Match, the hopper is in " + hopper.getFilterMode());
        String before = hopperSummary(helper, pos);
        ServerPlayer player = smith(helper, false);

        hammerThrough(helper, player, pos, ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER,
                ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, NETHERITE_STEP_WEAR, "the reinforced hopper");
        Assertions.valueEqual(helper, hopperSummary(helper, pos), before, "the hopper's filter and stock after the netherite step");

        hammerThrough(helper, player, pos, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER,
                ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, ENDERITE_STEP_WEAR, "the netherite hopper");
        Assertions.valueEqual(helper, hopperSummary(helper, pos), before, "the hopper's filter and stock after the enderite step");

        TestCleanup.succeed(helper);
    }

    /**
     * A creative player needs the nugget as the tier selector, but keeps it, and the hammer takes no
     * wear: after both steps on a blast furnace both nuggets are still in the off hand and the hammer
     * is undamaged after every single tick.
     *
     * <p>What breaks this test: consuming the nugget with {@code shrink} instead of
     * {@code consume(1, player)}, or charging durability past {@code hurtAndBreak}'s own creative
     * check.
     */
    public static void creativeUpgradeKeepsTheNuggetAndTheHammer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.REINFORCED_BLAST_FURNACE);
        ServerPlayer player = smith(helper, true);

        hammerThrough(helper, player, pos, ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE,
                ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, NETHERITE_STEP_WEAR,
                "the reinforced blast furnace, hammered in creative");
        hammerThrough(helper, player, pos, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE,
                ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, ENDERITE_STEP_WEAR,
                "the netherite blast furnace, hammered in creative");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // WHAT DOES NOT START
    // =====================================================================================

    /**
     * Eight clicks that must not start an upgrade, and two that must - so the eight cannot pass
     * against a rig that starts nothing at all.
     *
     * <ul>
     *   <li>a hammer below the gate: an iron sledgehammer on a reinforced furnace, a diamond one with
     *       an enderite nugget on a netherite smoker;</li>
     *   <li>the wrong nugget: an enderite nugget on a reinforced blast furnace, a netherite nugget on
     *       a netherite furnace;</li>
     *   <li>nothing to upgrade to: an enderite furnace, the top tier, and the reinforced
     *       <em>sticky</em> piston, which is not in the upgrade table;</li>
     *   <li>a busy piston: an extended reinforced piston in the tick its power is cut (the retraction
     *       is still queued, so only its EXTENDED state refuses it), and a retracted one that is
     *       powered but blocked by a chest in front of it - upgraded, it would come back as a breaker
     *       and fire at once.</li>
     * </ul>
     *
     * <p>Each refused click leaves the player idle and without a job. The five furnaces among them
     * open their own menu as usual instead of swallowing the click.
     *
     * <p>A diamond block rounds it off: with a nugget in the off hand the hammer still charges its
     * ordinary crush there - it is in use, but its charge is shorter than the 100 ticks of an upgrade.
     * The stance only hijacks clicks on something it can upgrade. Releasing that charge leaves no job
     * behind either.
     *
     * <p>What breaks this test: either hammer gate dropped or loosened, the nugget check dropped,
     * the sticky piston or an enderite machine gaining a table entry, {@code isBusyPiston} losing
     * its EXTENDED or its signal check, a job stored before the refusals are checked,
     * {@code shouldSkipBlockUse} skipping the menu although the upgrade cannot begin, {@code tryBegin}
     * answering for blocks outside the table, the use duration keyed on the stance instead of the
     * job, and {@code releaseUsing} no longer clearing the job.
     */
    public static void refusedUpgradesNeverStartTheHammer(GameTestHelper helper) {
        BlockPos weakHammer = new BlockPos(1, 1, 1);
        BlockPos weakForEnderite = new BlockPos(3, 1, 1);
        BlockPos wrongNuggetReinforced = new BlockPos(5, 1, 1);
        BlockPos wrongNuggetNetherite = new BlockPos(1, 1, 3);
        BlockPos topTier = new BlockPos(3, 1, 3);
        BlockPos stickyPiston = new BlockPos(1, 1, 5);
        BlockPos extendedPiston = new BlockPos(3, 1, 5);
        BlockPos blockedPiston = new BlockPos(5, 1, 5);
        BlockPos diamondBlock = new BlockPos(1, 1, 7);
        BlockPos furnaceControl = new BlockPos(3, 1, 7);
        BlockPos pistonControl = new BlockPos(5, 1, 7);

        helper.setBlock(weakHammer, ModBlocks.REINFORCED_FURNACE);
        helper.setBlock(weakForEnderite, ModBlocks.NETHERITE_SMOKER);
        helper.setBlock(wrongNuggetReinforced, ModBlocks.REINFORCED_BLAST_FURNACE);
        helper.setBlock(wrongNuggetNetherite, ModBlocks.NETHERITE_FURNACE);
        helper.setBlock(topTier, ModBlocks.ENDERITE_FURNACE);
        helper.setBlock(stickyPiston, eastFacing(ModBlocks.REINFORCED_STICKY_PISTON));
        helper.setBlock(diamondBlock, Blocks.DIAMOND_BLOCK);
        helper.setBlock(furnaceControl, ModBlocks.REINFORCED_FURNACE);
        helper.setBlock(pistonControl, eastFacing(ModBlocks.REINFORCED_PISTON));

        // Powered from below, so it extends; the head goes into the empty cell east of it.
        helper.setBlock(extendedPiston, eastFacing(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(extendedPiston.below(), Blocks.REDSTONE_BLOCK);
        // Powered from below as well, but a chest - a block entity no piston moves or breaches - is
        // in front of it, so it stays retracted with the signal on.
        helper.setBlock(blockedPiston.east(), Blocks.CHEST);
        helper.setBlock(blockedPiston, eastFacing(ModBlocks.REINFORCED_PISTON));
        helper.setBlock(blockedPiston.below(), Blocks.REDSTONE_BLOCK);

        ServerPlayer player = smith(helper, false);

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertBlockProperty(extendedPiston, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockProperty(blockedPiston, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(blockedPiston)),
                            "test setup broken: the blocked piston has no signal, so it is not busy at all");

                    assertRefused(helper, player, weakHammer, ModItems.IRON_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, true,
                            "an iron sledgehammer on a reinforced furnace");
                    assertRefused(helper, player, weakForEnderite, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, true,
                            "a diamond sledgehammer with an enderite nugget on a netherite smoker");
                    assertRefused(helper, player, wrongNuggetReinforced, ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, true,
                            "an enderite nugget on a reinforced blast furnace");
                    assertRefused(helper, player, wrongNuggetNetherite, ModItems.ENDERITE_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, true,
                            "a netherite nugget on a netherite furnace");
                    assertRefused(helper, player, topTier, ModItems.ENDERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, true,
                            "an enderite nugget on an enderite furnace, the top tier");
                    assertRefused(helper, player, stickyPiston, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, false,
                            "a netherite nugget on a reinforced sticky piston");
                    // Power off: the retraction is only queued as a block event, so for the rest of this
                    // tick the piston is extended without a signal - only EXTENDED can refuse it now.
                    helper.setBlock(extendedPiston.below(), Blocks.AIR);
                    helper.assertBlockProperty(extendedPiston, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertFalse(helper.getLevel().hasNeighborSignal(helper.absolutePos(extendedPiston)),
                            "test setup broken: the extended piston is still powered");
                    assertRefused(helper, player, extendedPiston, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, false,
                            "a netherite nugget on an extended, unpowered reinforced piston");
                    assertRefused(helper, player, blockedPiston, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, false,
                            "a netherite nugget on a powered but blocked reinforced piston");

                    // --- no upgrade target: the hammer does what it always does there ---
                    arm(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), new ItemStack(ModItems.NETHERITE_NUGGET, NUGGETS));
                    standOn(helper, player, diamondBlock);
                    click(helper, player, diamondBlock);
                    helper.assertTrue(player.isUsingItem(),
                            "with a nugget in the off hand the hammer no longer charges its crush on a diamond block");
                    helper.assertTrue(player.getUseItemRemainingTicks() < UPGRADE_TICKS,
                            "the crush charge on a diamond block is set to " + player.getUseItemRemainingTicks()
                                    + " ticks, the length of an upgrade - the stance hijacked a block it cannot upgrade");
                    player.releaseUsingItem();

                    // --- the two controls: the same rig does start where it should ---
                    arm(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), new ItemStack(ModItems.NETHERITE_NUGGET, NUGGETS));
                    standOn(helper, player, furnaceControl);
                    startHammering(helper, player, furnaceControl, "control: the reinforced furnace");
                    player.releaseUsingItem();
                    arm(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), new ItemStack(ModItems.NETHERITE_NUGGET, NUGGETS));
                    standOn(helper, player, pistonControl);
                    startHammering(helper, player, pistonControl, "control: the reinforced piston, retracted and unpowered");
                    player.releaseUsingItem();
                    helper.assertFalse(SledgehammerUpgrades.hasJob(player),
                            "releasing the right click left the upgrade job behind");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * Seven reinforced furnaces, one interruption each, and where it leaves the rig - checked as one
     * line: what stands there, the nuggets left, the durability spent, whether the hammer is still in
     * use and whether a job is left.
     *
     * <ul>
     *   <li><b>Released</b> before tick 50: two blows paid, nothing else.</li>
     *   <li><b>Nugget taken out of the off hand</b> before tick 50 and put back before tick 60:
     *       aborted on tick 50, the upgrade does not resume.</li>
     *   <li><b>Target changed</b> before tick 50 (the furnace is replaced by a reinforced smoker):
     *       aborted, and the smoker is still a smoker at tick 100 - not the netherite furnace the job
     *       was heading for.</li>
     *   <li><b>Looking away</b> (straight up) <b>for exactly two ticks</b>: forgiven, the upgrade
     *       completes on time.</li>
     *   <li><b>Looking away for good</b> from tick 50 on: the server tolerates
     *       {@value #SERVER_AIM_GRACE} missed ticks and gives up on the third, tick 52.</li>
     *   <li><b>The hammer breaks</b> on the second blow: aborted, the nugget is kept, and the player
     *       is no longer using an item. The last part needs {@code strike} to stop the use itself:
     *       vanilla does not, because the emptied stack in the hand and the emptied stack in use are
     *       the same one, and both answer "air" to {@code ItemStack#isSameItem}.</li>
     *   <li><b>The hammer breaks on the fifth blow</b>: that blow still completes the upgrade and
     *       takes the nugget - the swap comes before the last durability.</li>
     * </ul>
     *
     * <p>What breaks this test: the per-tick check in {@code tick} losing its nugget, its block or
     * its aim condition, a different aim grace, a release that finishes the upgrade, a hammer that
     * breaks mid-job and leaves the job or the use behind, and {@code finish} charging the last
     * durability before it swaps the block.
     */
    public static void interruptedUpgradesConsumeNoNugget(GameTestHelper helper) {
        ServerPlayer player = smith(helper, false);
        for (BlockPos spot : CASE_SPOTS) {
            helper.setBlock(spot, ModBlocks.REINFORCED_FURNACE);
        }

        // --- released ---
        BlockPos released = CASE_SPOTS.get(0);
        ItemStack releasedHammer = prepareCase(helper, player, released);
        startHammering(helper, player, released, "the furnace that is released");
        holdClick(player, UPGRADE_TICKS + 5, tick -> {
            if (tick == 50) {
                player.releaseUsingItem();
            }
        });
        Assertions.valueEqual(helper, rig(helper, player, released, player.getOffhandItem(), releasedHammer),
                rigLine(ModBlocks.REINFORCED_FURNACE, NUGGETS, 2 * NETHERITE_STEP_WEAR, false, false),
                "the rig after a release before tick 50");

        // --- the nugget leaves the off hand ---
        BlockPos nuggetGone = CASE_SPOTS.get(1);
        ItemStack nuggetGoneHammer = prepareCase(helper, player, nuggetGone);
        ItemStack takenNuggets = player.getOffhandItem();
        startHammering(helper, player, nuggetGone, "the furnace whose nugget is taken away");
        holdClick(player, UPGRADE_TICKS + 5, tick -> {
            if (tick == 50) {
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            if (tick == 60) {
                player.setItemInHand(InteractionHand.OFF_HAND, takenNuggets);
            }
        });
        Assertions.valueEqual(helper, rig(helper, player, nuggetGone, takenNuggets, nuggetGoneHammer),
                rigLine(ModBlocks.REINFORCED_FURNACE, NUGGETS, 2 * NETHERITE_STEP_WEAR, false, false),
                "the rig after the nugget was taken out of the off hand before tick 50");

        // --- the target changes under the hammer ---
        BlockPos changed = CASE_SPOTS.get(2);
        ItemStack changedHammer = prepareCase(helper, player, changed);
        startHammering(helper, player, changed, "the furnace that changes under the hammer");
        holdClick(player, UPGRADE_TICKS + 5, tick -> {
            if (tick == 50) {
                helper.setBlock(changed, ModBlocks.REINFORCED_SMOKER);
            }
        });
        Assertions.valueEqual(helper, rig(helper, player, changed, player.getOffhandItem(), changedHammer),
                rigLine(ModBlocks.REINFORCED_SMOKER, NUGGETS, 2 * NETHERITE_STEP_WEAR, false, false),
                "the rig after the furnace was replaced by a smoker before tick 50");

        // --- looking away for exactly the grace ---
        BlockPos glanced = CASE_SPOTS.get(3);
        ItemStack glancedHammer = prepareCase(helper, player, glanced);
        startHammering(helper, player, glanced, "the furnace the player glances away from");
        holdClick(player, UPGRADE_TICKS, tick -> {
            if (tick == 50) {
                lookUp(player);
            }
            if (tick == 50 + SERVER_AIM_GRACE) {
                standOn(helper, player, glanced);
            }
        });
        Assertions.valueEqual(helper, rig(helper, player, glanced, player.getOffhandItem(), glancedHammer),
                rigLine(ModBlocks.NETHERITE_FURNACE, NUGGETS - 1, BLOWS * NETHERITE_STEP_WEAR, false, false),
                "the rig after an upgrade with a glance away of " + SERVER_AIM_GRACE + " ticks");
        clearCooldown(player, glancedHammer);

        // --- looking away for good ---
        BlockPos lookedAway = CASE_SPOTS.get(4);
        prepareCase(helper, player, lookedAway);
        startHammering(helper, player, lookedAway, "the furnace the player looks away from");
        int[] stoppedAt = {-1};
        holdClick(player, UPGRADE_TICKS + 5, tick -> {
            if (tick == 50) {
                lookUp(player);
            }
            if (stoppedAt[0] < 0 && !player.isUsingItem()) {
                stoppedAt[0] = tick - 1;
            }
        });
        Assertions.valueEqual(helper, stoppedAt[0], 50 + SERVER_AIM_GRACE,
                "the player tick on which the server gave up after the player looked away before tick 50");

        // --- the hammer breaks on the second blow ---
        BlockPos brokenEarly = CASE_SPOTS.get(5);
        ItemStack brokenEarlyHammer = prepareCase(helper, player, brokenEarly);
        brokenEarlyHammer.setDamageValue(brokenEarlyHammer.getMaxDamage() - (NETHERITE_STEP_WEAR + 1));
        startHammering(helper, player, brokenEarly, "the furnace whose hammer breaks early");
        holdClick(player, UPGRADE_TICKS + 5, tick -> {
        });
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "test setup broken: the hammer with one blow and a point left did not break on the second blow");
        Assertions.valueEqual(helper, rig(helper, player, brokenEarly, player.getOffhandItem(), null),
                rigLine(ModBlocks.REINFORCED_FURNACE, NUGGETS, -1, false, false),
                "the rig after the hammer broke on the second blow");

        // --- the hammer breaks on the fifth blow ---
        BlockPos brokenLast = CASE_SPOTS.get(6);
        ItemStack brokenLastHammer = prepareCase(helper, player, brokenLast);
        brokenLastHammer.setDamageValue(brokenLastHammer.getMaxDamage() - BLOWS * NETHERITE_STEP_WEAR);
        startHammering(helper, player, brokenLast, "the furnace whose hammer breaks on the last blow");
        holdClick(player, UPGRADE_TICKS, tick -> {
        });
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "test setup broken: the hammer with exactly five blows left survived the fifth");
        Assertions.valueEqual(helper, rig(helper, player, brokenLast, player.getOffhandItem(), null),
                rigLine(ModBlocks.NETHERITE_FURNACE, NUGGETS - 1, -1, false, false),
                "the rig after the blow that broke the hammer (it has to complete the upgrade)");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // PROGRESS KEPT ON THE BLOCK
    // =====================================================================================

    /**
     * An upgrade that is let go after two blows leaves those two blows on the furnace, and the next
     * attempt picks up from there.
     *
     * <p>After the release before tick 50: the furnace is still reinforced, both nuggets are there,
     * the hammer has paid two blows, and the level's saved progress holds exactly "this furnace, two
     * blows" - marked dirty, so it is written with the world, and the same after a trip through its
     * own codec (what a restart reads back). The next click on the furnace starts a use of only the
     * 60 ticks the three missing blows take; the blows land on the same second boundaries as before
     * (wear after every tick is two paid blows plus one per 20 ticks), and after tick 60 the furnace
     * is netherite, the hammer has paid five blows in all, one nugget is gone, and the saved progress
     * is empty again.
     *
     * <p>What breaks this test: a blow that is not recorded, a resume that starts from zero (100 ticks,
     * and three blows too many), a resume that skips further than recorded, a finished upgrade that
     * leaves its entry behind, and an entry the codec does not carry.
     */
    public static void abortedUpgradeKeepsItsBlowsAndResumesThere(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.REINFORCED_FURNACE);
        ServerPlayer player = smith(helper, false);
        ItemStack hammer = prepareCase(helper, player, pos);

        startHammering(helper, player, pos, "the furnace that is let go after two blows");
        holdClick(player, 49, tick -> {
        });
        player.releaseUsingItem();
        Assertions.valueEqual(helper, rig(helper, player, pos, player.getOffhandItem(), hammer),
                rigLine(ModBlocks.REINFORCED_FURNACE, NUGGETS, 2 * NETHERITE_STEP_WEAR, false, false),
                "the rig after letting go before tick 50");
        Assertions.valueEqual(helper, storedProgress(helper, pos), progressLine(ModBlocks.REINFORCED_FURNACE, 2),
                "the progress saved on the furnace after two blows");

        SledgehammerProgress data = helper.getLevel().getDataStorage().get(SledgehammerProgress.TYPE);
        helper.assertTrue(data != null && data.isDirty(),
                "the saved progress is not marked dirty, so the world save would not write it");
        Tag saved = SledgehammerProgress.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        SledgehammerProgress reloaded = SledgehammerProgress.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(reloaded.entries().contains(
                        new SledgehammerProgress.Entry(helper.absolutePos(pos), ModBlocks.REINFORCED_FURNACE, 2)),
                "the saved progress does not survive its own codec: " + saved);

        // --- the second attempt ---
        standOn(helper, player, pos);
        player.containerMenu = player.inventoryMenu;
        click(helper, player, pos);
        helper.assertTrue(player.isUsingItem() && SledgehammerUpgrades.hasJob(player),
                "the second click on the half hammered furnace did not start the hammer");
        int missing = UPGRADE_TICKS - 2 * HIT_EVERY;
        Assertions.valueEqual(helper, player.getUseItemRemainingTicks(), missing,
                "ticks the resumed hammering is set to last (three blows left)");
        for (int tick = 1; tick < missing; tick++) {
            player.connection.tick();
            int blows = 2 + tick / HIT_EVERY;
            Assertions.valueEqual(helper, hammer.getDamageValue(), blows * NETHERITE_STEP_WEAR,
                    "durability spent after " + tick + " ticks of resumed hammering (" + blows + " blows)");
        }
        player.connection.tick();
        Assertions.valueEqual(helper, rig(helper, player, pos, player.getOffhandItem(), hammer),
                rigLine(ModBlocks.NETHERITE_FURNACE, NUGGETS - 1, BLOWS * NETHERITE_STEP_WEAR, false, false),
                "the rig after the resumed hammering");
        Assertions.valueEqual(helper, storedProgress(helper, pos), "nothing",
                "the progress saved on the furnace after it was upgraded");

        TestCleanup.succeed(helper);
    }

    /**
     * Saved progress stays as long as the block stays, and goes as soon as it is another block.
     *
     * <p>Three furnaces carry three blows each. One is lit (a state change of the same block), one is
     * replaced by a smoker, one is broken. A little more than one check interval later the lit one
     * still carries its three blows, the other two carry nothing - and a reinforced furnace put back
     * where the smoker stood starts from zero, so the old blows did not wait for it.
     *
     * <p>This goes through the server tick every loader wires up, so it also fails where that hook is
     * missing.
     *
     * <p>What breaks this test: progress that decays on any state change, progress that survives a
     * different block or an empty spot, and a loader without the tick hook.
     */
    public static void upgradeProgressLastsUntilTheBlockChanges(GameTestHelper helper) {
        BlockPos lit = new BlockPos(1, 1, 1);
        BlockPos replaced = new BlockPos(3, 1, 1);
        BlockPos broken = new BlockPos(5, 1, 1);
        for (BlockPos pos : List.of(lit, replaced, broken)) {
            helper.setBlock(pos, ModBlocks.REINFORCED_FURNACE);
            SledgehammerProgress.record(helper.getLevel(), helper.absolutePos(pos), ModBlocks.REINFORCED_FURNACE, 3);
        }
        helper.setBlock(lit, ModBlocks.REINFORCED_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true));
        helper.setBlock(replaced, ModBlocks.REINFORCED_SMOKER);
        helper.getLevel().destroyBlock(helper.absolutePos(broken), false);

        helper.startSequence()
                .thenIdle(PROGRESS_CHECK_TICKS)
                .thenExecute(() -> {
                    Assertions.valueEqual(helper, storedProgress(helper, lit), progressLine(ModBlocks.REINFORCED_FURNACE, 3),
                            "the progress on the furnace that was only lit");
                    Assertions.valueEqual(helper, storedProgress(helper, replaced), "nothing",
                            "the progress where the furnace was replaced by a smoker");
                    Assertions.valueEqual(helper, storedProgress(helper, broken), "nothing",
                            "the progress where the furnace was broken");
                    helper.setBlock(replaced, ModBlocks.REINFORCED_FURNACE);
                    Assertions.valueEqual(helper, SledgehammerProgress.hits(helper.getLevel(), helper.absolutePos(replaced),
                                    ModBlocks.REINFORCED_FURNACE), 0,
                            "blows found on a new furnace placed where the old one was replaced");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The pose the render mixins read, pinned on the server's own view of a hammering player (it is
     * not the local player, so it goes by what is in the hands - the path every other player on a
     * client takes).
     *
     * <p>Before the click the hammer shows the upgrade hint on the furnace with the netherite nugget,
     * and not with an enderite nugget. While hammering: no hint, and the blow phase after every tick
     * is that tick's place in its second; the hammer draws back through the first 80 % of every second
     * (rising, the third person arm raised) and swings through the last 20 % (falling to nothing, the
     * arm down), the blow itself landing on the second boundary.
     *
     * <p>What breaks this test: a hint that ignores the nugget or stays on while hammering, a phase
     * that is not tied to the blows, and a draw back that does not rise and fall around the strike.
     */
    public static void hammerDrawsBackBetweenBlowsAndHintsBeforehand(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.REINFORCED_FURNACE);
        ServerPlayer player = smith(helper, false);
        BlockPos absolute = helper.absolutePos(pos);

        arm(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), new ItemStack(ModItems.ENDERITE_NUGGET, NUGGETS));
        standOn(helper, player, pos);
        helper.assertFalse(SledgehammerUpgrades.showsUpgradeHint(helper.getLevel(), absolute, player),
                "the hammer hints at an upgrade with the wrong nugget in the off hand");
        ItemStack hammer = prepareCase(helper, player, pos);
        helper.assertTrue(SledgehammerUpgrades.showsUpgradeHint(helper.getLevel(), absolute, player),
                "the hammer does not hint at the upgrade it could start");
        Assertions.valueEqual(helper, SledgehammerUpgrades.blowPhase(player, 0.0F), -1.0F,
                "the blow phase before hammering");

        startHammering(helper, player, pos, "the furnace whose pose is watched");
        helper.assertFalse(SledgehammerUpgrades.showsUpgradeHint(helper.getLevel(), absolute, player),
                "the hammer still hints at the upgrade while it is hammering");
        float previous = 0.0F;
        for (int tick = 1; tick < UPGRADE_TICKS; tick++) {
            player.connection.tick();
            int inSecond = tick % HIT_EVERY;
            float phase = SledgehammerUpgrades.blowPhase(player, 0.0F);
            Assertions.valueEqual(helper, phase, inSecond / (float) HIT_EVERY, "the blow phase after " + tick + " ticks");
            float drawBack = SledgehammerUpgrades.drawBack(phase);
            boolean drawing = inSecond < DRAW_TICKS;
            Assertions.valueEqual(helper, SledgehammerUpgrades.isDrawingBack(player, 0.0F), drawing,
                    "drawing back (third person arm raised) after " + tick + " ticks");
            if (inSecond == 0) {
                Assertions.valueEqual(helper, drawBack, 0.0F, "the draw back right after the blow of tick " + tick);
            } else if (inSecond <= DRAW_TICKS) {
                helper.assertTrue(drawBack > previous,
                        "the hammer does not keep drawing back at tick " + tick + ": " + previous + " -> " + drawBack);
            } else {
                helper.assertTrue(drawBack < previous,
                        "the hammer does not swing forward at tick " + tick + ": " + previous + " -> " + drawBack);
            }
            previous = drawBack;
        }
        helper.assertTrue(SledgehammerUpgrades.drawBack(0.999F) < 0.05F,
                "the swing has not come down by the end of the second: " + SledgehammerUpgrades.drawBack(0.999F));
        player.connection.tick();
        helper.assertTrue(helper.getBlockState(pos).is(ModBlocks.NETHERITE_FURNACE),
                "test setup broken: the watched hammering did not upgrade the furnace");
        clearCooldown(player, hammer);

        TestCleanup.succeed(helper);
    }

    /** One entry of the saved progress at {@code pos}, as one line, or "nothing". */
    private static String storedProgress(GameTestHelper helper, BlockPos pos) {
        SledgehammerProgress data = helper.getLevel().getDataStorage().get(SledgehammerProgress.TYPE);
        if (data == null) {
            return "nothing";
        }
        BlockPos absolute = helper.absolutePos(pos);
        for (SledgehammerProgress.Entry entry : data.entries()) {
            if (entry.pos().equals(absolute)) {
                return progressLine(entry.block(), entry.hits());
            }
        }
        return "nothing";
    }

    private static String progressLine(Block block, int hits) {
        return BuiltInRegistries.BLOCK.getKey(block) + " x" + hits;
    }

    /**
     * The five crafting recipes the netherite machines used to have - the hopper column and the four
     * {@code *_bulk} 2x2 grids - are gone from the live recipe manager by id, and so are the five
     * recipe book advancements that unlocked them. A recipe file that came back, or an advancement
     * left behind by a datagen run that did not purge its output, would show up here even where no
     * grid test looks. The reinforced furnace recipe and its unlock are the controls that the lookups
     * can answer "yes".
     *
     * <p>What breaks this test: any of the ten files reappearing under {@code data/simplebuilding}.
     */
    public static void netheriteMachineRecipesAndTheirUnlocksAreGone(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();

        helper.assertTrue(server.getRecipeManager().byKey(recipeKey("reinforced_furnace")).isPresent(),
                "control broken: the reinforced furnace recipe is not loaded, so an absent recipe proves nothing");
        helper.assertTrue(server.getAdvancements().get(id("recipes/redstone/reinforced_furnace")) != null,
                "control broken: the reinforced furnace unlock is not loaded, so an absent unlock proves nothing");

        StringBuilder loaded = new StringBuilder();
        for (String recipe : List.of("netherite_hopper_from_crafting", "netherite_piston_bulk",
                "netherite_furnace_bulk", "netherite_smoker_bulk", "netherite_blast_furnace_bulk")) {
            if (server.getRecipeManager().byKey(recipeKey(recipe)).isPresent()) {
                loaded.append(" recipe ").append(recipe);
            }
        }
        for (String unlock : List.of("recipes/redstone/netherite_hopper_from_crafting",
                "recipes/redstone/netherite_piston_bulk", "recipes/decorations/netherite_furnace_bulk",
                "recipes/decorations/netherite_smoker_bulk", "recipes/decorations/netherite_blast_furnace_bulk")) {
            if (server.getAdvancements().get(id(unlock)) != null) {
                loaded.append(" advancement ").append(unlock);
            }
        }
        Assertions.valueEqual(helper, loaded.toString(), "",
                "removed netherite machine recipes and unlocks that are loaded again (the netherite machines are "
                        + "meant to be hammered in the world only)");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The in-level mock player with {@code instabuild} set as asked: cleared, it pays nuggets and
     * durability like a survival player; set, it builds for free. Handed back however the test ends -
     * a leaked mock player keeps the player list non-empty and the gametest server then stalls on
     * shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer smith(GameTestHelper helper, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = instabuild;
        player.setShiftKeyDown(false);
        TestCleanup.before(helper, () -> {
            player.containerMenu = player.inventoryMenu;
            MockPlayers.remove(helper, player);
        });
        helper.assertTrue(player.hasInfiniteMaterials() == instabuild,
                "test setup broken: the mock player's infinite materials do not follow instabuild");
        return player;
    }

    private static void arm(ServerPlayer player, ItemStack hammer, ItemStack nuggets) {
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, nuggets);
    }

    /**
     * Puts the player on top of the machine, looking straight down at it. Straight down is the one
     * aim that does not depend on the head yaw, which {@code snapTo} does not set and which
     * {@code LivingEntity#getViewYRot} reads instead of the body yaw.
     */
    private static void standOn(GameTestHelper helper, ServerPlayer player, BlockPos machine) {
        Vec3 feet = helper.absoluteVec(new Vec3(machine.getX() + 0.5, machine.getY() + 1.0, machine.getZ() + 0.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);
    }

    /** Turns the player to look straight up, off the machine it stands on. */
    private static void lookUp(ServerPlayer player) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, -90.0F);
    }

    /** A right click on the machine's top face, through vanilla's own server side use path. */
    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos machine) {
        BlockPos absolute = helper.absolutePos(machine);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** Holds the right click for {@code ticks} vanilla player ticks, letting a case interfere before each. */
    private static void holdClick(ServerPlayer player, int ticks, Interference interference) {
        for (int tick = 1; tick <= ticks; tick++) {
            interference.before(tick);
            player.connection.tick();
        }
    }

    /** The click has to reach the hammer and start a job of exactly one upgrade length. */
    private static void startHammering(GameTestHelper helper, ServerPlayer player, BlockPos machine, String what) {
        player.containerMenu = player.inventoryMenu;
        click(helper, player, machine);
        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                what + ": the click opened the machine's menu instead of passing on to the hammer");
        helper.assertTrue(player.isUsingItem(), what + ": the click did not start the hammer");
        helper.assertTrue(SledgehammerUpgrades.hasJob(player), what + ": the hammer is in use, but without an upgrade job");
        Assertions.valueEqual(helper, player.getUseItemRemainingTicks(), UPGRADE_TICKS,
                what + ": ticks the hammering is set to last");
    }

    /**
     * One upgrade step from click to swap, checked after every player tick. The expected wear
     * follows from this file's constants alone; whether anything is paid at all follows from the
     * player's {@code instabuild}.
     */
    private static void hammerThrough(GameTestHelper helper, ServerPlayer player, BlockPos machine, Block from, Block to,
                                      Item hammerItem, Item nuggetItem, int wear, String what) {
        ItemStack hammer = new ItemStack(hammerItem);
        arm(player, hammer, new ItemStack(nuggetItem, NUGGETS));
        standOn(helper, player, machine);
        boolean pays = !player.hasInfiniteMaterials();
        BlockState before = helper.getBlockState(machine);
        helper.assertTrue(before.is(from), "test setup broken: " + what + " is " + before);

        startHammering(helper, player, machine, what);
        for (int tick = 1; tick < UPGRADE_TICKS; tick++) {
            player.connection.tick();
            int blows = tick / HIT_EVERY;
            Assertions.valueEqual(helper, hammer.getDamageValue(), pays ? blows * wear : 0,
                    what + ": durability spent after " + tick + " ticks of hammering (" + blows + " blows)");
            if (!player.isUsingItem()) {
                helper.fail(what + ": the hammering stopped by itself after " + tick + " ticks");
            }
        }
        player.connection.tick();

        BlockState after = helper.getBlockState(machine);
        helper.assertTrue(after.is(to), what + " is " + after + " after " + UPGRADE_TICKS + " ticks of hammering, "
                + "not the next tier");
        for (Property<?> property : before.getProperties()) {
            Assertions.valueEqual(helper, after.getValue(property), before.getValue(property),
                    what + ": block state property '" + property.getName() + "' after the upgrade");
        }
        Assertions.valueEqual(helper, hammer.getDamageValue(), pays ? BLOWS * wear : 0,
                what + ": durability the whole upgrade cost");
        Assertions.valueEqual(helper, player.getOffhandItem().getCount(), pays ? NUGGETS - 1 : NUGGETS,
                what + ": nuggets left in the off hand after the upgrade");
        helper.assertFalse(SledgehammerUpgrades.hasJob(player), what + ": the job outlived the upgrade");
        helper.assertTrue(player.getCooldowns().isOnCooldown(hammer),
                what + ": the hammer is not cooling down after the upgrade, so the right click that is still held "
                        + "would open the freshly upgraded machine's menu");
        clearCooldown(player, hammer);
    }

    /** A refused click: the hammer does not start, no job is stored, and a mod furnace opens its menu. */
    private static void assertRefused(GameTestHelper helper, ServerPlayer player, BlockPos machine, Item hammerItem,
                                      Item nuggetItem, boolean opensMenu, String what) {
        arm(player, new ItemStack(hammerItem), new ItemStack(nuggetItem, NUGGETS));
        standOn(helper, player, machine);
        player.containerMenu = player.inventoryMenu;

        click(helper, player, machine);
        helper.assertFalse(player.isUsingItem(), what + " started the hammer");
        helper.assertFalse(SledgehammerUpgrades.hasJob(player), what + " stored an upgrade job");
        if (opensMenu) {
            helper.assertTrue(player.containerMenu != player.inventoryMenu,
                    what + " opened no menu - a machine that cannot be upgraded has to open as usual");
        }
        player.containerMenu = player.inventoryMenu;
    }

    /** A fresh diamond hammer and two netherite nuggets, and the player on top of the case's furnace. */
    private static ItemStack prepareCase(GameTestHelper helper, ServerPlayer player, BlockPos machine) {
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        arm(player, hammer, new ItemStack(ModItems.NETHERITE_NUGGET, NUGGETS));
        standOn(helper, player, machine);
        return hammer;
    }

    /**
     * Where a case left the rig, as one line. {@code hammer} null (a broken hammer) leaves the wear
     * out as -1.
     */
    private static String rig(GameTestHelper helper, ServerPlayer player, BlockPos machine, ItemStack nuggets,
                              ItemStack hammer) {
        return rigLine(helper.getBlockState(machine).getBlock(), nuggets.getCount(),
                hammer == null ? -1 : hammer.getDamageValue(), player.isUsingItem(), SledgehammerUpgrades.hasJob(player));
    }

    private static String rigLine(Block block, int nuggets, int wear, boolean using, boolean job) {
        return BuiltInRegistries.BLOCK.getKey(block) + ", " + nuggets + " nuggets, wear " + wear
                + ", in use " + using + ", job " + job;
    }

    /** The three bricks {@link #everyMachineClimbsFromReinforcedToNetheriteToEnderite} puts in slot 0. */
    private static void assertBricksKept(GameTestHelper helper, BlockPos pos, String what) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof Container container) {
            Assertions.valueEqual(helper, container.getItem(0).getCount() + " " + BuiltInRegistries.ITEM.getKey(container.getItem(0).getItem()),
                    "3 minecraft:brick", what + ": the stack in slot 0");
        }
    }

    /** Progress, input, fuel and output of the furnace at {@code pos}, as one line. */
    private static String furnaceSummary(GameTestHelper helper, BlockPos pos) {
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        return "progress " + persistedInt(helper, pos, "cooking_time_spent")
                + ", input " + furnace.getItem(0) + ", fuel " + furnace.getItem(1) + ", output " + furnace.getItem(2);
    }

    /** Filter mode, filter item and stock of the hopper at {@code pos}, as one line. */
    private static String hopperSummary(GameTestHelper helper, BlockPos pos) {
        ModHopperBlockEntity hopper = helper.getBlockEntity(pos, ModHopperBlockEntity.class);
        return "mode " + hopper.getFilterMode() + ", filter " + hopper.getGhostItem(0) + ", stock " + hopper.getItem(1);
    }

    private static void clearCooldown(ServerPlayer player, ItemStack hammer) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
    }

    private static BlockState eastFacing(Block piston) {
        return piston.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST);
    }

    /**
     * Reads one of the block entity's persisted ints. A missing key fails instead of answering with a
     * default that reads like a plausible zero.
     */
    private static int persistedInt(GameTestHelper helper, BlockPos pos, String key) {
        CompoundTag tag = helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                .saveWithoutMetadata(helper.getLevel().registryAccess());
        int value = tag.getIntOr(key, Integer.MIN_VALUE);
        helper.assertTrue(value != Integer.MIN_VALUE, "the block entity at " + pos + " no longer persists '" + key + "'");
        return value;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static ResourceKey<Recipe<?>> recipeKey(String path) {
        return ResourceKey.create(Registries.RECIPE, id(path));
    }
}
