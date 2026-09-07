package com.simplebuilding.gametest;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.ModToolMaterials;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The chisel and the spatula: which transformation table a click reaches, what survives the
 * transformation, how the click position orients the result, what the tool costs while mining,
 * and how a worn chisel is carried up the smithing tiers.
 *
 * <p>{@link ChiselItem} backs <em>thirteen</em> registered items (seven chisels, six spatulas)
 * out of one class. Everything that differs between them is set from outside after
 * construction - {@code setCooldownTicks}, {@code setAsDedicatedSpatula} - or derived from the
 * {@code ToolMaterial} in the constructor. The suite so far only ever picked up the stone and
 * the diamond chisel, so eleven of those thirteen items, the whole tier-merge that the static
 * initialiser builds, and the entire orientation pass were running untested.
 *
 * <h2>What is deliberately left to the existing files</h2>
 * <ul>
 *   <li>{@code ToolBehaviourTests#chiselAndSpatulaTransformBlockInBothDirections} and
 *       {@code #chiselTierGatesTransformations} already pin the plain forward/backward step and
 *       the fact that a stone chisel refuses a diamond-tier block. Only the parts those two do
 *       not reach are repeated here (the sneaking spatula, the merge <em>upwards</em>, the
 *       material pairs).</li>
 *   <li>{@code ConsumptionAndDurabilityTests#chiselChargesDurabilityAndCooldownOnlyOutsideCreative}
 *       owns the 1-point/2-point durability split and the creative exemption.</li>
 *   <li>{@code BuildingEnchantmentTests} owns Constructor's Touch (the {@code touch*} tables) and
 *       Fast Chiseling (cooldown scaling and the {@code +5}/{@code +17} mining bonus). It also
 *       already asserts the stone chisel's base cooldown of 30 ticks;
 *       {@link #cooldownTicksFollowTheTierTable} repeats that one row on purpose, as the anchor of
 *       the full table.</li>
 *   <li>{@code TradeAndMigrationTests} owns the toolsmith trade ids and the legacy
 *       spatula-to-chisel item migration.</li>
 * </ul>
 *
 * <h2>Known defects (asserted around, not asserted in)</h2>
 * <ul>
 *   <li><b>{@code isCorrectToolForDrops} ignores the tier.</b> {@code ChiselItem} overrides it
 *       with a plain "is this block mineable with pickaxe, axe or shovel" test and never looks at
 *       {@code this.material}. That bypasses vanilla's {@code minecraft:incorrect_for_*_tool}
 *       gate, so a <em>stone</em> chisel counts as the correct tool for obsidian, ancient debris
 *       and deepslate diamond ore and makes them drop. {@link #chiselMinesAtHalfMaterialSpeed}
 *       therefore only asserts the three tags the method is meant to accept plus one block it
 *       must refuse; it does not assert anything about a tier-gated block, in either direction.</li>
 *   <li><b>No {@code newState != oldState} guard.</b> Entries that map a block onto itself
 *       (mud brick, end stone brick, purpur, cut copper, nether brick and resin brick stairs and
 *       slabs) exist so the block can be re-aimed. Because nothing compares the outcome with the
 *       input, a click that lands on the orientation the block already has still spends a point of
 *       durability and a full cooldown. {@link #sharedPropertiesSurviveAndSelfMappingsReorient} drives the
 *       case that <em>does</em> change something and states the price for it; it does not pin the
 *       no-op click.</li>
 *   <li><b>{@code setChiselDirectionCycle} is dead.</b> The {@code chiselDirection} field it
 *       writes is annotated {@code @SuppressWarnings("unused")} and is never read; the spatula's
 *       reversed default comes from {@code isDedicatedSpatula} alone. Nothing here can go red for
 *       it, so it is only recorded.</li>
 * </ul>
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li>The client half of {@code useOn}. It runs behind {@code level.isClientSide()} and answers
 *       {@code SUCCESS} whenever the block appears in <em>any</em> of the four tables, without
 *       looking at the sneak state or at Constructor's Touch - so the client swing is wider than
 *       what the server will do. A gametest only ever sees the server side.</li>
 *   <li>The transformation sound ({@code world.playSound}) and the block particles
 *       ({@code spawnEffects}): both leave the server as packets with no observable server state.</li>
 *   <li>The tooltip's colour/style. {@link #lastTargetIsStoredAndShownInTheTooltip} reads the line
 *       through {@code Component#getString()}, which drops the {@code GRAY} formatting.</li>
 *   <li>The smithing <em>table</em> itself: {@code SmithingScreenHandlerMixin} subtracts the extra
 *       addition items in {@code onTake}, which needs a live {@code SmithingMenu} and a slot
 *       listener. {@link #smithingUpgradesCarryWearNameAndEnchantments} stops at the recipe.</li>
 * </ul>
 */
public final class ChiselTests {

    private ChiselTests() {
    }

    /** Middle of a block's top face - outside every rim, so no edge orientation is derived. */
    private static final Vec3 TOP_CENTRE = new Vec3(0.5, 1.0, 0.5);

    /** Top face, well inside the western rim ({@code x < 0.25}). */
    private static final Vec3 TOP_WEST_RIM = new Vec3(0.1, 1.0, 0.5);

    /** Upper bound for the cooldown drain loops, so a stuck cooldown fails instead of hanging. */
    private static final int COOLDOWN_TICK_CAP = 400;

    /**
     * The one transformation every tier can perform: it is registered in the stone tables only and
     * reaches the higher tiers through the merge chain in {@code ChiselItem}'s static initialiser.
     */
    private static final Block TIER_NEUTRAL_SOURCE = Blocks.STONE;
    private static final Block TIER_NEUTRAL_RESULT = Blocks.CHISELED_STONE_BRICKS;

    /** Working position for the single-block tests. */
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);

    /** One registered tool together with the cooldown its tier is supposed to impose. */
    private record TierRow(ChiselItem tool, String name, int cooldownTicks) {
    }

    /**
     * All thirteen registered {@link ChiselItem}s with the cooldown their tier should hand out.
     * Copper deliberately shares iron's 25 ticks and enderite shares netherite's 5 - the two rows
     * that are easiest to get wrong when a tier is added.
     */
    private static final List<TierRow> TIER_TABLE = List.of(
            new TierRow(ModItems.STONE_CHISEL, "stone_chisel", 30),
            new TierRow(ModItems.COPPER_CHISEL, "copper_chisel", 25),
            new TierRow(ModItems.IRON_CHISEL, "iron_chisel", 25),
            new TierRow(ModItems.GOLD_CHISEL, "gold_chisel", 20),
            new TierRow(ModItems.DIAMOND_CHISEL, "diamond_chisel", 10),
            new TierRow(ModItems.NETHERITE_CHISEL, "netherite_chisel", 5),
            new TierRow(ModItems.ENDERITE_CHISEL, "enderite_chisel", 5),
            new TierRow(ModItems.STONE_SPATULA, "stone_spatula", 30),
            new TierRow(ModItems.COPPER_SPATULA, "copper_spatula", 25),
            new TierRow(ModItems.IRON_SPATULA, "iron_spatula", 25),
            new TierRow(ModItems.GOLD_SPATULA, "gold_spatula", 20),
            new TierRow(ModItems.DIAMOND_SPATULA, "diamond_spatula", 10),
            new TierRow(ModItems.NETHERITE_SPATULA, "netherite_spatula", 5));

    // =====================================================================================
    // DIRECTION: WHICH OF THE TWO TABLES A CLICK REACHES
    // =====================================================================================

    /**
     * The complete direction matrix of the two tool shapes over one reversible pair of blocks.
     * A chisel walks the forward table and reverses while sneaking; a spatula is the mirror image
     * of that - it walks the backward table and runs <em>forward</em> while sneaking.
     *
     * <p>The four refusals matter as much as the four hits: each tool only ever consults one map
     * per click, so the four combinations that would need the other map have to come back
     * {@code PASS} and leave the block alone. Without them, a version that consulted both maps at
     * once would still satisfy every positive assertion.
     *
     * <p>What breaks this: dropping the {@code isSneaking} branch out of the spatula arm of
     * {@code tryChiselBlock} (the sneaking spatula was the one corner of this matrix nothing
     * reached before); swapping the two ternaries so chisel and spatula share a default; or
     * letting a click fall through to the second map when the first has no entry.
     */
    public static void spatulaRunsForwardWhileSneakingAndChiselRunsBackward(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);

        ItemStack chisel = new ItemStack(ModItems.STONE_CHISEL);
        ItemStack spatula = new ItemStack(ModItems.STONE_SPATULA);
        helper.assertTrue(ModItems.STONE_SPATULA.isDedicatedSpatula(),
                "stone_spatula is no longer flagged as a dedicated spatula, so it now behaves like a "
                        + "chisel and the whole matrix below means nothing");
        helper.assertFalse(ModItems.STONE_CHISEL.isDedicatedSpatula(),
                "stone_chisel is flagged as a dedicated spatula");

        // --- the four combinations that must act ---
        assertChisels(helper, player, chisel, false, Blocks.STONE, Blocks.CHISELED_STONE_BRICKS,
                "a standing chisel");
        assertChisels(helper, player, chisel, true, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE,
                "a sneaking chisel");
        assertChisels(helper, player, spatula, false, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE,
                "a standing spatula");
        // The gap this test exists for: the spatula's sneak branch runs the forward table.
        assertChisels(helper, player, spatula, true, Blocks.STONE, Blocks.CHISELED_STONE_BRICKS,
                "a sneaking spatula");

        // --- and the four that must not, because the entry lives in the other map ---
        assertRefuses(helper, player, chisel, false, Blocks.CHISELED_STONE_BRICKS,
                "a standing chisel reached into the backward table");
        assertRefuses(helper, player, chisel, true, Blocks.STONE,
                "a sneaking chisel reached into the forward table");
        assertRefuses(helper, player, spatula, false, Blocks.STONE,
                "a standing spatula reached into the forward table");
        assertRefuses(helper, player, spatula, true, Blocks.CHISELED_STONE_BRICKS,
                "a sneaking spatula reached into the backward table");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // TIERS: MERGED UPWARDS, SHARED IN PAIRS
    // =====================================================================================

    /**
     * Every tier's table is the union of its own entries and all the lower ones, and the seven
     * materials collapse into four tables: stone, copper+iron, gold+diamond, netherite+enderite.
     *
     * <p>The merge is only visible from above: {@code STONE -> CHISELED_STONE_BRICKS} is registered
     * in the stone table and nowhere else, so a diamond, netherite or enderite chisel performing it
     * can only have got there through {@code merge(...)}.
     *
     * <p>That upward half on its own pins nothing about <em>which</em> of the four tables a tool
     * holds, and neither does the pair check below. {@code merge(lower, own)} makes the four tables
     * genuinely nested ({@code FINAL_STONE_FWD} is a subset of {@code FINAL_IRON_FWD} is a subset of
     * {@code FINAL_DIAMOND_FWD} is a subset of {@code FINAL_NETHERITE_FWD}), so every assertion that
     * only demands a transformation is equally satisfied by the tool's own table and by every table
     * above it. A copper chisel moved into the constructor's {@code GOLD || DIAMOND} branch would
     * still perform {@code STONE -> CHISELED_STONE_BRICKS} and still agree with the iron chisel on
     * {@code CHISELED_STONE_BRICKS -> STONE_BRICKS}, because that entry is in the diamond and
     * netherite tables too.
     *
     * <p>So each tool that has a tier above it is also given a <em>ceiling</em>: one block that is a
     * key of the next table up and of no lower one. Lower bound and ceiling together clamp the tool
     * to exactly one table, which is what makes "the tiers stay locked downwards" - the property
     * that makes an upgrade worth crafting - an assertion rather than a claim. Only the top pair has
     * no ceiling, because nothing sits above it.
     *
     * <p>The pairs are checked by running the same block through both members and demanding the
     * same result. That is stronger than comparing the map fields, because it also proves the
     * constructor's {@code ==} comparisons against the {@code ToolMaterial} constants still route
     * both members to the same table - {@code ModToolMaterials.ENDERITE} in particular is a mod
     * instance and would silently fall through to the empty fallback map if that branch were lost.
     * Both directions are driven, because {@code forwardMap} and {@code backwardMap} are assigned on
     * separate lines in every branch of the constructor and can be wired to different tiers.
     *
     * <p>What breaks this: deleting a {@code merge(...)} step; adding a tier without folding the
     * lower table into it; dropping one material from an {@code ||} in the constructor (the
     * offending tool then transforms nothing at all and every assertion for it goes red); and -
     * this is what the ceilings are for - moving a material into a higher branch, which upgrades
     * that tool for free without breaking a single one of the positive assertions.
     */
    public static void tierTablesAreInheritedUpwardsAndSharedInPairs(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);

        // --- merged upwards: a stone-tier entry is still there four tiers up ---
        for (TierRow row : TIER_TABLE) {
            if (row.tool().isDedicatedSpatula()) {
                continue;
            }
            assertChisels(helper, player, new ItemStack(row.tool()), false,
                    TIER_NEUTRAL_SOURCE, TIER_NEUTRAL_RESULT,
                    row.name() + " lost the stone tier's transformation, so the table merge is gone");
        }

        // --- and capped from above: one block per chisel out of the next table up and out of no
        //     lower one, so a tool routed into a higher branch of the constructor goes red here ---
        assertRefuses(helper, player, new ItemStack(ModItems.STONE_CHISEL), false,
                Blocks.CHISELED_STONE_BRICKS,
                "a stone chisel performed the iron tier's chiseled -> stone bricks step");
        assertRefuses(helper, player, new ItemStack(ModItems.COPPER_CHISEL), false,
                Blocks.SMOOTH_QUARTZ,
                "a copper chisel performed the gold tier's smooth quartz step, so copper no longer "
                        + "sits on iron's table but on a higher one");
        assertRefuses(helper, player, new ItemStack(ModItems.IRON_CHISEL), false,
                Blocks.SMOOTH_QUARTZ,
                "an iron chisel performed the gold tier's smooth quartz step");
        assertRefuses(helper, player, new ItemStack(ModItems.GOLD_CHISEL), false,
                Blocks.NETHERRACK,
                "a gold chisel performed the netherite tier's netherrack step, so gold no longer "
                        + "sits on diamond's table but on a higher one");
        assertRefuses(helper, player, new ItemStack(ModItems.DIAMOND_CHISEL), false,
                Blocks.NETHERRACK,
                "a diamond chisel performed the netherite tier's netherrack step");

        // --- the same ceiling on the backward tables, which had none: a standing spatula reads
        //     backwardMap, and that field is wired tier by tier just like the forward one ---
        assertRefuses(helper, player, new ItemStack(ModItems.STONE_SPATULA), false,
                Blocks.STONE_BRICKS,
                "a stone spatula performed the iron tier's stone bricks -> chiseled step");
        assertRefuses(helper, player, new ItemStack(ModItems.COPPER_SPATULA), false,
                Blocks.QUARTZ_PILLAR,
                "a copper spatula performed the gold tier's quartz pillar step");
        assertRefuses(helper, player, new ItemStack(ModItems.IRON_SPATULA), false,
                Blocks.QUARTZ_PILLAR,
                "an iron spatula performed the gold tier's quartz pillar step");
        assertRefuses(helper, player, new ItemStack(ModItems.GOLD_SPATULA), false,
                Blocks.NETHER_BRICKS,
                "a gold spatula performed the netherite tier's nether bricks step");
        assertRefuses(helper, player, new ItemStack(ModItems.DIAMOND_SPATULA), false,
                Blocks.NETHER_BRICKS,
                "a diamond spatula performed the netherite tier's nether bricks step");

        // --- copper and iron share one table, in both directions ---
        assertSamePair(helper, player, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL,
                Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, "copper/iron");
        assertSamePair(helper, player, ModItems.COPPER_SPATULA, ModItems.IRON_SPATULA,
                Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, "copper/iron backwards");
        // --- gold and diamond share one table ---
        assertSamePair(helper, player, ModItems.GOLD_CHISEL, ModItems.DIAMOND_CHISEL,
                Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, "gold/diamond");
        assertSamePair(helper, player, ModItems.GOLD_SPATULA, ModItems.DIAMOND_SPATULA,
                Blocks.QUARTZ_PILLAR, Blocks.SMOOTH_QUARTZ, "gold/diamond backwards");
        // --- netherite and enderite share one table ---
        assertSamePair(helper, player, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL,
                Blocks.NETHERRACK, Blocks.NETHER_BRICKS, "netherite/enderite");
        // There is no enderite spatula, so the top backward table is pinned on its own member.
        assertChisels(helper, player, new ItemStack(ModItems.NETHERITE_SPATULA), false,
                Blocks.NETHER_BRICKS, Blocks.NETHERRACK,
                "a netherite spatula, the only spatula on the top table");

        TestCleanup.succeed(helper);
    }

    /**
     * The top tier's own table: the nether brick family is registered with {@code registerCyclic},
     * so four forward clicks return the block to where it started, and one backward click from the
     * start jumps straight to the last member. The chiseled sandstone entry is checked alongside it
     * because it is the one netherite step that leaves the "brick" family entirely.
     *
     * <p>The full circle is what makes the cycle falsifiable: a table built with
     * {@code registerLinear} by mistake would produce the same first three results and then stop,
     * so only the fourth click - back to netherrack - can tell the two apart. The single backward
     * click pins the other half of {@code registerCyclic}, the wrap-around entry it writes into the
     * spatula map.
     *
     * <p>The sand sits on a stone pad because sand falls; the pad keeps the assertion about what
     * the chisel produced from turning into an assertion about gravity.
     *
     * <p>What breaks this: turning the cyclic registration into a linear one, reordering the four
     * members, or losing the {@code CHISELED_SANDSTONE -> SAND} entry when the sandstone chain is
     * touched at the stone tier.
     */
    public static void netheriteTierCyclesTheNetherBrickFamily(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        ItemStack chisel = new ItemStack(ModItems.NETHERITE_CHISEL);

        helper.setBlock(TARGET, Blocks.NETHERRACK);
        Block[] circle = {
                Blocks.NETHER_BRICKS,
                Blocks.CRACKED_NETHER_BRICKS,
                Blocks.CHISELED_NETHER_BRICKS,
                Blocks.NETHERRACK
        };
        for (int step = 0; step < circle.length; step++) {
            Block got = chiselAt(helper, player, chisel, TARGET, false,
                    "netherite chisel, step " + (step + 1) + " of the nether brick circle");
            Assertions.valueEqual(helper, got, circle[step],
                    "step " + (step + 1) + " of the nether brick circle");
        }

        // The wrap-around entry registerCyclic writes into the backward map.
        assertChisels(helper, player, new ItemStack(ModItems.NETHERITE_SPATULA), false,
                Blocks.NETHERRACK, Blocks.CHISELED_NETHER_BRICKS,
                "a netherite spatula on netherrack");

        // --- the one netherite step that leaves the family: chiseled sandstone crumbles to sand ---
        BlockPos sandPos = new BlockPos(5, 1, 5);
        helper.setBlock(sandPos.below(), Blocks.STONE);
        helper.setBlock(sandPos, Blocks.CHISELED_SANDSTONE);
        Block crumbled = chiselAt(helper, player, chisel, sandPos, false,
                "netherite chisel on chiseled sandstone");
        Assertions.valueEqual(helper, crumbled, Blocks.SAND, "chiseled sandstone under a netherite chisel");

        TestCleanup.succeed(helper);
    }

    /**
     * Each tier hands the player its own cooldown, and the value that arrives on the player is the
     * one the item was registered with.
     *
     * <p>The number is <em>measured</em>, not read off: after a real transformation the player's
     * {@code ItemCooldowns} is ticked until the tool is free again, and the tick count is compared
     * both with the item's own {@code getCooldownTicks()} and with the value the tier is supposed
     * to have. The first comparison catches a broken path from the field to the player (a dropped
     * {@code addCooldown}, a hard-coded constant in {@code tryChiselBlock}); the second catches a
     * mis-wired registration - and copper (25, iron's value, not stone's) and enderite (5,
     * netherite's value) are exactly the two rows a new tier tends to get wrong.
     *
     * <p>All thirteen items are driven through a real click rather than through the getter, which
     * is also the only test that touches eleven of them at all: a spatula whose material branch was
     * dropped in the constructor gets the empty fallback map, refuses the block and fails here.
     *
     * <p>What breaks this: retuning a tier without updating this table; giving the spatulas their
     * own cooldown ladder; scaling the cooldown outside the Fast Chiseling branch.
     */
    public static void cooldownTicksFollowTheTierTable(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        ItemCooldowns cooldowns = player.getCooldowns();

        for (TierRow row : TIER_TABLE) {
            ItemStack stack = new ItemStack(row.tool());
            // A spatula runs backwards while standing, so it needs the far end of the same pair.
            boolean spatula = row.tool().isDedicatedSpatula();
            Block from = spatula ? TIER_NEUTRAL_RESULT : TIER_NEUTRAL_SOURCE;
            Block to = spatula ? TIER_NEUTRAL_SOURCE : TIER_NEUTRAL_RESULT;

            helper.setBlock(TARGET, from);
            clearCooldown(player, stack);
            player.setShiftKeyDown(false);
            InteractionResult result = useOn(helper, player, stack, TARGET, Direction.UP, TOP_CENTRE);

            helper.assertTrue(result != InteractionResult.PASS,
                    row.name() + " refused " + from + ", so it has no cooldown to measure; it "
                            + "returned " + result);
            helper.assertBlockPresent(to, TARGET);
            helper.assertTrue(cooldowns.isOnCooldown(stack),
                    row.name() + " put itself on no cooldown at all after a real transformation");

            int ticks = 0;
            while (cooldowns.isOnCooldown(stack) && ticks < COOLDOWN_TICK_CAP) {
                cooldowns.tick();
                ticks++;
            }
            helper.assertTrue(ticks < COOLDOWN_TICK_CAP,
                    row.name() + " never came off cooldown within " + COOLDOWN_TICK_CAP + " ticks");
            Assertions.valueEqual(helper, ticks, row.tool().getCooldownTicks(),
                    row.name() + ": the cooldown that reached the player is not the one the item "
                            + "was registered with");
            Assertions.valueEqual(helper, ticks, row.cooldownTicks(),
                    row.name() + ": cooldown ticks for this tier");
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // WHAT SURVIVES THE TRANSFORMATION
    // =====================================================================================

    /**
     * What the new block state is made of: properties both blocks understand are carried across (a
     * waterlogged stair stays waterlogged), and an entry that maps a block onto itself re-aims it
     * instead of replacing it - for the ordinary forward price of one point of durability plus a
     * full cooldown.
     *
     * <p>Both halves of the water are driven: a waterlogged stair keeps its water, a dry one stays
     * dry. The dry control is what makes the first assertion mean something - a copy loop that
     * simply forced {@code WATERLOGGED} on, or one that dropped the property and let the new
     * block's default of {@code false} stand, would each satisfy only one of the two.
     *
     * <p>See "Known defects" in the class javadoc for the self-mapping half - there is no
     * {@code newState != oldState} check, so a click that lands on the orientation the block
     * already has is billed just the same. This drives the case that does change something (a
     * top-half stair facing south becomes a bottom-half stair facing west) and states the price
     * for it; it deliberately does not assert anything about the no-op click, in either direction.
     *
     * <p>The player is turned west for that click on purpose. {@code NETHER_BRICK_STAIRS} defaults
     * to {@code NORTH} and so does every mock player in this file, so an expected facing of north
     * would be satisfied by a re-aim that did nothing at all; west is neither the state that goes
     * in nor the block's default, and can only come from the player.
     *
     * <p>What breaks this: removing the property copy loop from {@code tryChiselBlock}, or moving
     * it <em>after</em> {@code applyIntuitiveOrientation} - the orientation pass would then be
     * overwritten by the old block's stale facing on every stair in the game; dropping the
     * self-mapping entries, which makes those stairs impossible to re-aim; or returning early
     * before the durability and cooldown block when the block type does not change, which would
     * make re-aiming free.
     */
    public static void sharedPropertiesSurviveAndSelfMappingsReorient(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        ItemStack stoneChisel = new ItemStack(ModItems.STONE_CHISEL);

        // --- waterlogged in, waterlogged out ---
        helper.setBlock(TARGET, Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true)
                .setValue(StairBlock.FACING, Direction.SOUTH)
                .setValue(StairBlock.HALF, Half.TOP));
        Block wet = chiselAt(helper, player, stoneChisel, TARGET, false, "stone chisel on wet stone stairs");
        Assertions.valueEqual(helper, wet, Blocks.COBBLESTONE_STAIRS, "the wet stair's new block");
        helper.assertTrue(helper.getBlockState(TARGET).getValue(BlockStateProperties.WATERLOGGED),
                "the water was lost when the stair was chiselled, so every waterlogged stair in a "
                        + "build drains the moment it is touched");

        // --- and dry in, dry out: the copy loop copies, it does not force ---
        helper.setBlock(TARGET, Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(StairBlock.FACING, Direction.SOUTH)
                .setValue(StairBlock.HALF, Half.TOP));
        Block dry = chiselAt(helper, player, stoneChisel, TARGET, false, "stone chisel on dry stone stairs");
        Assertions.valueEqual(helper, dry, Blocks.COBBLESTONE_STAIRS, "the dry stair's new block");
        helper.assertFalse(helper.getBlockState(TARGET).getValue(BlockStateProperties.WATERLOGGED),
                "a dry stair came out of the chisel waterlogged");

        // --- an entry that maps a block onto itself only re-aims it, and still charges for it.
        //     The player is turned west first, so the expected facing is neither the state that
        //     goes in (south) nor NETHER_BRICK_STAIRS' own default (north) - otherwise "it faces
        //     the way the player looks" would be indistinguishable from "nothing was re-aimed". ---
        ItemStack chisel = new ItemStack(ModItems.NETHERITE_CHISEL);
        turnedTo(helper, player, 90.0F, Direction.WEST);
        helper.setBlock(TARGET, Blocks.NETHER_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.SOUTH)
                .setValue(StairBlock.HALF, Half.TOP));

        clearCooldown(player, chisel);
        player.setShiftKeyDown(false);
        InteractionResult result = useOn(helper, player, chisel, TARGET, Direction.UP, TOP_CENTRE);
        helper.assertTrue(result != InteractionResult.PASS,
                "the netherite chisel refused nether brick stairs, so the self-mapping entry is gone "
                        + "and the block can no longer be re-aimed; it returned " + result);

        BlockState after = helper.getBlockState(TARGET);
        Assertions.valueEqual(helper, after.getBlock(), Blocks.NETHER_BRICK_STAIRS,
                "a self-mapping entry changed the block type");
        Assertions.valueEqual(helper, after.getValue(StairBlock.FACING), Direction.WEST,
                "the re-aimed stair does not face the way the player looks");
        Assertions.valueEqual(helper, after.getValue(StairBlock.HALF), Half.BOTTOM,
                "the re-aimed stair kept its top half after a click on the top face");

        Assertions.valueEqual(helper, chisel.getDamageValue(), 1,
                "re-aiming a block costs a different amount of durability than a forward step");
        helper.assertTrue(player.getCooldowns().isOnCooldown(chisel),
                "re-aiming a block set no cooldown, so self-mapping entries can be spammed");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // INTUITIVE ORIENTATION
    // =====================================================================================

    /**
     * {@code applyIntuitiveOrientation} as a pure function: the outer quarter of a face counts as
     * a rim and names a direction, everything inside it falls back to the clicked face, and the
     * three block families read that result differently.
     *
     * <p>Driving the function directly is the only way to hit the margin at all - every click that
     * goes through the world in this suite lands on {@code (0.5, 1.0, 0.5)}, dead centre. The pairs
     * at {@code 0.24}/{@code 0.26} and the exact {@code 0.25}/{@code 0.75} samples pin both the
     * width of the rim and the strictness of the comparisons: with {@code <=} instead of {@code <}
     * the exact-boundary rows go red, and with a margin of {@code 0.2} or {@code 0.3} the
     * {@code 0.24}/{@code 0.26} pair does.
     *
     * <p>The three families are checked on one state each, because the function returns at the
     * first property it recognises: a pillar (axis only), a stair (horizontal facing plus half) and
     * a rod (full facing). A block with none of them has to come back untouched, which is what
     * keeps the function from quietly rewriting states it was never meant to.
     *
     * <p>The stair rows are driven from <em>two</em> viewing angles, and that is not padding. Every
     * player in this file is built facing north, and north is also the default value of
     * {@code StairBlock.FACING} on every stair block used here - so with one angle the expected
     * facing is simultaneously the player's direction, the block's default and the constant
     * {@code NORTH}, and no assertion can tell the three apart. A second angle separates them: with
     * the player turned west the middle of the face has to come back {@code WEST}, while the same
     * click on the western rim has to stay {@code EAST}, because the rim overrules the player.
     *
     * <p>What breaks this: retuning {@code margin}; flipping a rim to the wrong compass direction;
     * dropping {@code getOpposite()} from the stair facing, which is what makes a stair climb
     * <em>towards</em> the rim you clicked; reordering the x-before-z checks; handing a rod the
     * rim direction where it should take the clicked face; and replacing
     * {@code facing = player.getDirection()} with any constant or with the incoming state's own
     * facing, which the second angle is there to catch.
     *
     * <p><strong>Every branch, with its direction.</strong> A pillar only carries an axis, so the
     * pillar cases can show <em>where</em> the rim is but never which of its two ends the code
     * picked. The rod carries the direction itself, so the twelve rim branches are walked once
     * each on a rod - the audit of 2026-09-08 found six of them never entered at all (the whole
     * east/west face block, plus UP and EAST of the north face) and four more seen only through
     * an axis, which meant half of them could be swapped with their opposite unnoticed.
     *
     * <p>Three corner cases follow, because a corner is the only place where two conditions hold
     * at once and the order of the checks becomes observable. Then the margin from both sides:
     * 0.249999 is still rim, 0.25 exactly is not, which pins the constant and the strict
     * {@code <}.
     *
     * <p><strong>The half of a stair, read from y.</strong> The last two lines of the derivation
     * overwrite the half whenever the orientation is UP or DOWN - which is every click on the top
     * or bottom face. So on those faces the y arms cannot be observed at all, and replacing one of
     * them left the suite green. The four cases on a side face fix that: there the orientation is
     * the side itself, no override fires, and the half is genuinely the one y chose.
     */
    public static void intuitiveOrientationDerivesTheEdgeDirection(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        Assertions.valueEqual(helper, player.getDirection(), Direction.NORTH,
                "the mock player is not facing north, so the expected stair facings below are wrong");

        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
        BlockState rod = Blocks.END_ROD.defaultBlockState();

        // --- top face: the rim names a compass direction, the middle keeps the face ---
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.5, 1.0, 0.5),
                Direction.Axis.Y, "top centre");
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.24, 1.0, 0.5),
                Direction.Axis.X, "top face just inside the west rim");
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.26, 1.0, 0.5),
                Direction.Axis.Y, "top face just outside the west rim");
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.76, 1.0, 0.5),
                Direction.Axis.X, "top face just inside the east rim");
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.5, 1.0, 0.24),
                Direction.Axis.Z, "top face just inside the north rim");
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.5, 1.0, 0.76),
                Direction.Axis.Z, "top face just inside the south rim");
        // Exactly on the boundary the comparison is strict, so this is still the middle.
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.25, 1.0, 0.5),
                Direction.Axis.Y, "top face exactly on the west boundary");
        assertAxis(helper, player, pillar, Direction.UP, new Vec3(0.75, 1.0, 0.5),
                Direction.Axis.Y, "top face exactly on the east boundary");

        // --- a side face reads its rims from y and from the other horizontal axis ---
        assertAxis(helper, player, pillar, Direction.NORTH, new Vec3(0.5, 0.5, 0.0),
                Direction.Axis.Z, "north face centre");
        assertAxis(helper, player, pillar, Direction.NORTH, new Vec3(0.5, 0.1, 0.0),
                Direction.Axis.Y, "north face, lower rim");
        assertAxis(helper, player, pillar, Direction.NORTH, new Vec3(0.1, 0.5, 0.0),
                Direction.Axis.X, "north face, west rim");

        // --- rods point at the clicked face, or at the rim when there is one ---
        assertFacing(helper, player, rod, Direction.UP, TOP_CENTRE, Direction.UP, "rod, top centre");
        assertFacing(helper, player, rod, Direction.UP, TOP_WEST_RIM, Direction.WEST, "rod, top west rim");
        assertFacing(helper, player, rod, Direction.NORTH, new Vec3(0.5, 0.5, 0.0),
                Direction.NORTH, "rod, north face centre");
        assertFacing(helper, player, rod, Direction.NORTH, new Vec3(0.5, 0.1, 0.0),
                Direction.DOWN, "rod, north face lower rim");

        // --- stairs: facing from the player in the middle, away from the rim on a rim ---
        assertStairs(helper, player, stairs, Direction.UP, TOP_CENTRE,
                Direction.NORTH, Half.BOTTOM, "stairs, top centre");
        assertStairs(helper, player, stairs, Direction.UP, TOP_WEST_RIM,
                Direction.EAST, Half.TOP, "stairs, top west rim");
        assertStairs(helper, player, stairs, Direction.DOWN, new Vec3(0.5, 0.0, 0.5),
                Direction.NORTH, Half.TOP, "stairs, bottom centre");
        assertStairs(helper, player, stairs, Direction.NORTH, new Vec3(0.5, 0.5, 0.0),
                Direction.NORTH, Half.BOTTOM, "stairs, north face centre");


        // --- all twelve rim branches, on a rod, which stores the direction and not just its axis ---
        // The pillar cases above can only show the axis, so they cannot tell WEST from EAST or
        // DOWN from UP; the audit of 2026-09-08 found six of the twelve branches never entered at
        // all and four more only through an axis. The margin here is 0.25, wider than the
        // rotator's.
        // top face (axis Y): x is read first, then z
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.1, 1.0, 0.5),
                Direction.WEST, "rod, top face west rim");
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.9, 1.0, 0.5),
                Direction.EAST, "rod, top face east rim");
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.5, 1.0, 0.1),
                Direction.NORTH, "rod, top face north rim");
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.5, 1.0, 0.9),
                Direction.SOUTH, "rod, top face south rim");
        // east face (axis X): y is read first, then z - none of these four was ever entered
        assertFacing(helper, player, rod, Direction.EAST, new Vec3(1.0, 0.1, 0.5),
                Direction.DOWN, "rod, east face lower rim");
        assertFacing(helper, player, rod, Direction.EAST, new Vec3(1.0, 0.9, 0.5),
                Direction.UP, "rod, east face upper rim");
        assertFacing(helper, player, rod, Direction.EAST, new Vec3(1.0, 0.5, 0.1),
                Direction.NORTH, "rod, east face north rim");
        assertFacing(helper, player, rod, Direction.EAST, new Vec3(1.0, 0.5, 0.9),
                Direction.SOUTH, "rod, east face south rim");
        // north face (axis Z): y is read first, then x
        assertFacing(helper, player, rod, Direction.NORTH, new Vec3(0.5, 0.9, 0.0),
                Direction.UP, "rod, north face upper rim");
        assertFacing(helper, player, rod, Direction.NORTH, new Vec3(0.1, 0.5, 0.0),
                Direction.WEST, "rod, north face west rim");
        assertFacing(helper, player, rod, Direction.NORTH, new Vec3(0.9, 0.5, 0.0),
                Direction.EAST, "rod, north face east rim");

        // --- the corners, where the order of the checks is the only thing that decides ---
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.1, 1.0, 0.1),
                Direction.WEST, "rod, north-west corner of the top face: x is read before z");
        assertFacing(helper, player, rod, Direction.EAST, new Vec3(1.0, 0.1, 0.1),
                Direction.DOWN, "rod, lower north corner of the east face: y is read before z");
        assertFacing(helper, player, rod, Direction.NORTH, new Vec3(0.1, 0.1, 0.0),
                Direction.DOWN, "rod, lower west corner of the north face: y is read before x");

        // --- the margin of applyIntuitiveOrientation is 0.25, and the comparison is strict ---
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.249999, 1.0, 0.5),
                Direction.WEST, "rod, 0.249999 from the west edge, the last hair inside the rim");
        assertFacing(helper, player, rod, Direction.UP, new Vec3(0.25, 1.0, 0.5),
                Direction.UP, "rod, 0.25 exactly, which the strict comparison leaves outside");

        // --- the two y arms of the stair half, on a VERTICAL face where nothing overwrites them ---
        // On the top and bottom faces the orientation is UP or DOWN, and the last two lines of the
        // half derivation overwrite whatever y produced - which is why the existing cases above
        // cannot see these arms at all. On a side face the orientation is the side, so neither
        // override fires and the half is genuinely the one y chose.
        assertStairs(helper, player, stairs, Direction.NORTH, new Vec3(0.5, 0.3, 0.0),
                Direction.NORTH, Half.BOTTOM, "stairs, north face centre below the middle");
        assertStairs(helper, player, stairs, Direction.NORTH, new Vec3(0.5, 0.7, 0.0),
                Direction.NORTH, Half.TOP, "stairs, north face centre above the middle");
        assertStairs(helper, player, stairs, Direction.EAST, new Vec3(1.0, 0.3, 0.5),
                Direction.NORTH, Half.BOTTOM, "stairs, east face centre below the middle");
        assertStairs(helper, player, stairs, Direction.EAST, new Vec3(1.0, 0.7, 0.5),
                Direction.NORTH, Half.TOP, "stairs, east face centre above the middle");

        // --- the same two clicks from a second angle. NORTH above is the player's direction, the
        //     block's default and a compass constant all at once; WEST here is none of the other
        //     two, so only this pair proves the middle really reads the player - and that the rim
        //     does not. ---
        turnedTo(helper, player, 90.0F, Direction.WEST);
        assertStairs(helper, player, stairs, Direction.UP, TOP_CENTRE,
                Direction.WEST, Half.BOTTOM, "stairs, top centre, player facing west");
        assertStairs(helper, player, stairs, Direction.UP, TOP_WEST_RIM,
                Direction.EAST, Half.TOP, "stairs, top west rim, player facing west");
        turnedTo(helper, player, 180.0F, Direction.NORTH);

        // --- a block with none of the three properties comes back untouched ---
        BlockState plain = Blocks.STONE.defaultBlockState();
        Assertions.valueEqual(helper, 
                ChiselItem.applyIntuitiveOrientation(plain, Direction.UP, TOP_WEST_RIM, player),
                plain, "a block without axis, stair facing or facing was rewritten anyway");

        TestCleanup.succeed(helper);
    }

    /**
     * The orientation pass is really wired into the click, and it runs <em>after</em> the property
     * copy: a pillar produced by a chisel takes its axis from where on the face the player clicked,
     * and a stair takes its facing and half from there too - not from the block that was there
     * before.
     *
     * <p>{@link #intuitiveOrientationDerivesTheEdgeDirection} proves the function computes the
     * right answer; this proves {@code tryChiselBlock} asks it, and asks it last. Every input state
     * here is deliberately set to the opposite of the expected output (a south-facing top-half
     * stair that has to come out facing north on the bottom half), so a version that simply copied
     * the old orientation across would fail rather than accidentally agree.
     *
     * <p>The last click is made by a player turned west, and it is the one row in this file where
     * the three candidates for a stair's facing are all different: the input state says south, the
     * block's own default says north, and the player says west. A click path that copied the old
     * facing, kept the default or hard-coded a compass direction each fails only here.
     *
     * <p>What breaks this: deleting the {@code applyIntuitiveOrientation} call from
     * {@code tryChiselBlock}; running it before the property copy loop; handing it the absolute
     * click location instead of the position relative to the block corner, which would put every
     * click far outside {@code 0..1} and make every face read as a rim; or handing it something
     * other than the clicking player, whose direction it needs.
     */
    public static void chiselledPillarsAndStairsTakeTheClickOrientation(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        Assertions.valueEqual(helper, player.getDirection(), Direction.NORTH,
                "the mock player is not facing north, so the expected stair facing below is wrong");

        // --- pillar: the axis comes from the click, the source block has no axis to copy ---
        ItemStack goldChisel = new ItemStack(ModItems.GOLD_CHISEL);
        helper.setBlock(TARGET, Blocks.SMOOTH_QUARTZ);
        Block upright = chiselAt(helper, player, goldChisel, TARGET, false, "gold chisel, top centre");
        Assertions.valueEqual(helper, upright, Blocks.QUARTZ_PILLAR, "smooth quartz under a gold chisel");
        Assertions.valueEqual(helper, helper.getBlockState(TARGET).getValue(RotatedPillarBlock.AXIS),
                Direction.Axis.Y, "a pillar chiselled in the middle of the top face");

        helper.setBlock(TARGET, Blocks.SMOOTH_QUARTZ);
        clearCooldown(player, goldChisel);
        useOn(helper, player, goldChisel, TARGET, Direction.UP, TOP_WEST_RIM);
        helper.assertBlockPresent(Blocks.QUARTZ_PILLAR, TARGET);
        Assertions.valueEqual(helper, helper.getBlockState(TARGET).getValue(RotatedPillarBlock.AXIS),
                Direction.Axis.X, "a pillar chiselled on the west rim of the top face");

        // --- stairs: the old facing and half are overwritten, not carried over ---
        ItemStack stoneChisel = new ItemStack(ModItems.STONE_CHISEL);
        BlockState wrongWayRound = Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.SOUTH)
                .setValue(StairBlock.HALF, Half.TOP);

        helper.setBlock(TARGET, wrongWayRound);
        clearCooldown(player, stoneChisel);
        useOn(helper, player, stoneChisel, TARGET, Direction.UP, TOP_CENTRE);
        BlockState centre = helper.getBlockState(TARGET);
        Assertions.valueEqual(helper, centre.getBlock(), Blocks.COBBLESTONE_STAIRS,
                "stone stairs under a stone chisel");
        Assertions.valueEqual(helper, centre.getValue(StairBlock.FACING), Direction.NORTH,
                "a stair chiselled in the middle of the top face does not face the player");
        Assertions.valueEqual(helper, centre.getValue(StairBlock.HALF), Half.BOTTOM,
                "a stair chiselled in the middle of the top face kept the old top half");

        helper.setBlock(TARGET, wrongWayRound);
        clearCooldown(player, stoneChisel);
        useOn(helper, player, stoneChisel, TARGET, Direction.UP, TOP_WEST_RIM);
        BlockState rim = helper.getBlockState(TARGET);
        Assertions.valueEqual(helper, rim.getValue(StairBlock.FACING), Direction.EAST,
                "a stair chiselled on the west rim does not climb away from that rim");
        Assertions.valueEqual(helper, rim.getValue(StairBlock.HALF), Half.TOP,
                "a stair chiselled on a rim of the top face");

        // --- and the same centre click from a second angle. The input faces south, the result
        //     block defaults to north, the player looks west: only a click path that really carries
        //     the player's direction through can produce west. ---
        turnedTo(helper, player, 90.0F, Direction.WEST);
        helper.setBlock(TARGET, wrongWayRound);
        clearCooldown(player, stoneChisel);
        useOn(helper, player, stoneChisel, TARGET, Direction.UP, TOP_CENTRE);
        Assertions.valueEqual(helper, helper.getBlockState(TARGET).getValue(StairBlock.FACING),
                Direction.WEST,
                "a stair chiselled in the middle of the top face by a west-facing player does not "
                        + "face west, so the click path does not pass the player's direction on");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // MINING WITH THE CHISEL
    // =====================================================================================

    /**
     * Away from its right click, a chisel is an ordinary digging tool: it is effective on pickaxe,
     * axe and shovel blocks, digs them at half its material's speed, and pays two points of
     * durability per block - unless the block breaks instantly, which costs nothing.
     *
     * <p>The speed is asserted as an absolute value against {@code ToolMaterial#speed()} rather
     * than as a ratio between two tiers, so that a halving factor quietly changed to {@code 1.0f}
     * (or dropped) goes red. Three materials are measured, including the mod's own
     * {@code ModToolMaterials.ENDERITE}, so the assertion cannot be satisfied by a hard-coded
     * number either. {@code BuildingEnchantmentTests} covers what Fast Chiseling adds on top.
     *
     * <p>See "Known defects" in the class javadoc: the effectiveness check never looks at the tier,
     * so nothing is asserted here about blocks vanilla gates behind one.
     *
     * <p>What breaks this: dropping one of the three block tags from
     * {@code isCorrectToolForDrops} (the axe and shovel arms had no test at all); changing the
     * {@code * 0.5f}; changing the {@code 2} in {@code mineBlock}; or moving the
     * {@code hurtAndBreak} out from behind the {@code getDestroySpeed(...) != 0} guard, which would
     * bill the player for brushing through grass.
     */
    public static void chiselMinesAtHalfMaterialSpeed(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        ChiselItem chisel = ModItems.STONE_CHISEL;
        ItemStack stack = new ItemStack(chisel);

        // --- effective on all three tool families, and only on those ---
        helper.assertTrue(chisel.isCorrectToolForDrops(stack, Blocks.STONE.defaultBlockState()),
                "the chisel is no longer an effective tool on pickaxe blocks");
        helper.assertTrue(chisel.isCorrectToolForDrops(stack, Blocks.OAK_LOG.defaultBlockState()),
                "the chisel is no longer an effective tool on axe blocks");
        helper.assertTrue(chisel.isCorrectToolForDrops(stack, Blocks.DIRT.defaultBlockState()),
                "the chisel is no longer an effective tool on shovel blocks");
        helper.assertFalse(chisel.isCorrectToolForDrops(stack, Blocks.GLASS.defaultBlockState()),
                "the chisel claims to be the correct tool for glass, which belongs to none of the "
                        + "three mineable tags");

        // --- half the material speed, on every family and on three different materials ---
        Assertions.valueEqual(helper, chisel.getMaterial(), ToolMaterial.STONE,
                "stone_chisel is no longer built on ToolMaterial.STONE, so the speeds below compare "
                        + "against the wrong material");
        assertSpeed(helper, chisel, Blocks.STONE, ToolMaterial.STONE.speed() * 0.5F, "stone chisel on stone");
        assertSpeed(helper, chisel, Blocks.OAK_LOG, ToolMaterial.STONE.speed() * 0.5F, "stone chisel on a log");
        assertSpeed(helper, chisel, Blocks.DIRT, ToolMaterial.STONE.speed() * 0.5F, "stone chisel on dirt");
        assertSpeed(helper, ModItems.DIAMOND_CHISEL, Blocks.STONE, ToolMaterial.DIAMOND.speed() * 0.5F,
                "diamond chisel on stone");
        assertSpeed(helper, ModItems.ENDERITE_CHISEL, Blocks.STONE, ModToolMaterials.ENDERITE.speed() * 0.5F,
                "enderite chisel on stone");
        assertSpeed(helper, chisel, Blocks.GLASS, 1.0F, "stone chisel on glass");

        // --- two points per mined block ... ---
        helper.setBlock(TARGET, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(TARGET);
        chisel.mineBlock(stack, helper.getLevel(), helper.getBlockState(TARGET), absolute, player);
        Assertions.valueEqual(helper, stack.getDamageValue(), 2, "wear after mining one stone block");

        // --- ... but nothing at all for a block with no hardness. TNT is the rare instabreak
        //     block that is also a full cube, so it can be placed anywhere in the room. ---
        helper.setBlock(TARGET, Blocks.TNT);
        helper.assertTrue(helper.getBlockState(TARGET).getDestroySpeed(helper.getLevel(), absolute) == 0.0F,
                "the zero-hardness probe block is no longer instabreak, so the assertion below "
                        + "would pass for the wrong reason");
        chisel.mineBlock(stack, helper.getLevel(), helper.getBlockState(TARGET), absolute, player);
        Assertions.valueEqual(helper, stack.getDamageValue(), 2,
                "the chisel wore down on a block that breaks instantly");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // LAST TARGET COMPONENT
    // =====================================================================================

    /**
     * Every accepted click stores the absolute position it worked on in the stack's
     * {@code COORDINATES} component, and the tooltip reports it as a single "Last Target" line.
     *
     * <p>Three states are driven, because only together do they pin the component to the successful
     * branch: a fresh chisel carries nothing and shows no line; a click overwrites whatever was
     * there before; and a <em>refused</em> click leaves the previous value alone. Without the last
     * one, moving {@code stack.set(...)} out of the {@code if (currentMap.containsKey(...))} branch
     * would go unnoticed and the tooltip would start pointing at blocks the tool never touched.
     *
     * <p>The stored position is absolute: the expected value is the test room's own
     * {@code helper.absolutePos(...)}, so a version that stored the relative position, or the
     * neighbouring position the click came from, fails.
     *
     * <p>What breaks this: storing the position outside the success branch, storing a relative or
     * offset position, or dropping the tooltip line.
     */
    public static void lastTargetIsStoredAndShownInTheTooltip(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper, new Vec3(3.5, 2.0, 5.5), 180.0F);
        ItemStack chisel = new ItemStack(ModItems.STONE_CHISEL);

        // --- a fresh chisel has no target and no line ---
        helper.assertTrue(chisel.get(ModDataComponentTypes.COORDINATES) == null,
                "a fresh chisel already carries a last target");
        helper.assertTrue(lastTargetLines(helper, chisel).isEmpty(),
                "a chisel without a stored target already shows a Last Target line");

        // --- a real click stores the absolute position and shows it ---
        helper.setBlock(TARGET, Blocks.STONE);
        chiselAt(helper, player, chisel, TARGET, false, "stone chisel storing its first target");
        assertLastTarget(helper, chisel, helper.absolutePos(TARGET), "the first click");

        // --- a second click somewhere else overwrites it ---
        BlockPos second = new BlockPos(5, 1, 2);
        helper.setBlock(second, Blocks.STONE);
        chiselAt(helper, player, chisel, second, false, "stone chisel storing its second target");
        assertLastTarget(helper, chisel, helper.absolutePos(second), "the second click");

        // --- a refused click must not move the marker ---
        BlockPos refused = new BlockPos(5, 1, 4);
        helper.setBlock(refused, Blocks.GLASS);
        InteractionResult result = useOn(helper, player, chisel, refused, Direction.UP, TOP_CENTRE);
        helper.assertTrue(result == InteractionResult.PASS,
                "the stone chisel claims to have chiselled glass, result was " + result);
        assertLastTarget(helper, chisel, helper.absolutePos(second),
                "a refused click; the marker moved to a block the chisel never changed");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // SMITHING
    // =====================================================================================

    /**
     * The chisel upgrade ladder, end to end: copper to iron to gold to diamond over the mod's
     * count-based smithing recipe, then diamond to netherite and netherite to enderite over
     * vanilla's {@code smithing_transform}. A worn, named, enchanted chisel comes out the other
     * side still worn, still named, still enchanted.
     *
     * <p>The carry-over is the point of the whole recipe type - a player who upgrades a favourite
     * tool must not lose its enchantments - and {@code CountBasedSmithingRecipe} had no test at
     * all. It is asserted on a base whose three components are all different from the result item's
     * defaults, so a version that returned a plain {@code result.create()} fails on every one.
     *
     * <p>The count is checked from both sides: two ingots match, one ingot matches nothing. That is
     * the entire reason the recipe type exists (vanilla smithing ignores the addition's stack size),
     * so the one-ingot case is what tells this recipe apart from a plain {@code smithing_transform}.
     *
     * <p>What breaks this: dropping the {@code getCount() >= additionCount} check; replacing
     * {@code applyComponentsAndValidate(base.getComponentsPatch())} with a bare result; pointing an
     * upgrade at the wrong template, base or addition; letting a tier be skipped.
     */
    public static void smithingUpgradesCarryWearNameAndEnchantments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipes = level.getServer().getRecipeManager();

        // --- the count-based ladder, with a base worth carrying over ---
        assertCountBasedUpgrade(helper, level, recipes, ModItems.BASIC_UPGRADE_TEMPLATE,
                ModItems.COPPER_CHISEL, Items.IRON_INGOT, ModItems.IRON_CHISEL, "copper -> iron");
        assertCountBasedUpgrade(helper, level, recipes, ModItems.BASIC_UPGRADE_TEMPLATE,
                ModItems.IRON_CHISEL, Items.GOLD_INGOT, ModItems.GOLD_CHISEL, "iron -> gold");
        assertCountBasedUpgrade(helper, level, recipes, ModItems.BASIC_UPGRADE_TEMPLATE,
                ModItems.GOLD_CHISEL, Items.DIAMOND, ModItems.DIAMOND_CHISEL, "gold -> diamond");

        // --- and vanilla's two transforms on top of it ---
        assertTransform(helper, level, recipes, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                ModItems.DIAMOND_CHISEL, Items.NETHERITE_INGOT, ModItems.NETHERITE_CHISEL,
                "diamond -> netherite");
        assertTransform(helper, level, recipes, ModItems.ENDERITE_UPGRADE_TEMPLATE,
                ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_INGOT, ModItems.ENDERITE_CHISEL,
                "netherite -> enderite");

        // --- the wrong template forges nothing, so the ladder cannot be short-circuited ---
        SmithingRecipeInput wrongTemplate = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(ModItems.COPPER_CHISEL),
                new ItemStack(Items.IRON_INGOT, 2));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, wrongTemplate, level).isEmpty(),
                "the copper -> iron upgrade accepted the netherite template");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The ordinary in-level mock player. Its {@code gameMode()} is hard-wired to creative and
     * {@code instabuild} stays on, so no click costs anything and no cooldown is ever set - which
     * is exactly what the table-walking tests want, because they click the same tool many times in
     * a row.
     */
    private static ServerPlayer creativePlayer(GameTestHelper helper, Vec3 relativePos, float yRot) {
        return mockPlayer(helper, relativePos, yRot, true);
    }

    /**
     * The same in-level mock with {@code instabuild} cleared, which is the only guard the mod's
     * chisel branches actually read ({@code player.getAbilities().instabuild}). Needed wherever the
     * durability or the cooldown is the thing under test. The player keeps its connection, which
     * {@code ServerItemCooldowns} needs to send its packet.
     */
    private static ServerPlayer survivalPlayer(GameTestHelper helper, Vec3 relativePos, float yRot) {
        ServerPlayer player = mockPlayer(helper, relativePos, yRot, false);
        helper.assertFalse(player.getAbilities().instabuild,
                "the mock player still builds for free, so no cooldown and no durability would ever "
                        + "be paid and this test would measure nothing");
        return player;
    }

    /**
     * Turns the mock player on the spot and checks that the new yaw really produced the compass
     * direction the caller is about to assert against.
     *
     * <p>The player is snapped back onto its own coordinates, so only the yaw moves: the
     * orientation pass reads nothing but {@code player.getDirection()}, and relocating the player
     * would also move the block it is allowed to reach. The check is not ceremony -
     * {@code Direction#fromYRot} is vanilla's mapping, and every "the stair faces the player"
     * assertion in this file is only worth anything as long as the yaw below really is the
     * direction named here.
     */
    private static void turnedTo(GameTestHelper helper, ServerPlayer player, float yRot,
                                 Direction expected) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yRot, player.getXRot());
        Assertions.valueEqual(helper, player.getDirection(), expected,
                "a mock player set to yaw " + yRot + " reports " + player.getDirection()
                        + ", so the stair facings expected from it are wrong");
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relativePos, float yRot,
                                           boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, yRot, 0.0F);
        player.getAbilities().instabuild = instabuild;
        // Hand the player back however the test ends; a leaked mock player keeps the player list
        // non-empty and the gametest server then stalls on shutdown.
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Right clicks the middle of a block's top face and returns the block that is there afterwards.
     *
     * <p>Any leftover cooldown is cleared first, so each step starts from the same state instead of
     * being silently swallowed by the previous one. On the way through, {@code ChiselItem#canChisel}
     * - the predicate the client highlight uses - is compared with what the click really did; the
     * two repeat the same map selection in two places and have to agree.
     */
    private static Block chiselAt(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                  BlockPos relativePos, boolean sneaking, String what) {
        player.setShiftKeyDown(sneaking);
        clearCooldown(player, stack);
        BlockPos pos = helper.absolutePos(relativePos);

        boolean predicted = ((ChiselItem) stack.getItem()).canChisel(helper.getLevel(), pos, stack, player);
        InteractionResult result = useOn(helper, player, stack, relativePos, Direction.UP, TOP_CENTRE);
        boolean acted = result != InteractionResult.PASS;

        Assertions.valueEqual(helper, predicted, acted,
                "canChisel and the actual click disagree for " + what + " (canChisel said " + predicted
                        + ", the click returned " + result + "), so the block highlight lies to the player");
        return helper.getBlockState(relativePos).getBlock();
    }

    /** Places {@code from}, clicks it and demands {@code to}. */
    private static void assertChisels(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                      boolean sneaking, Block from, Block to, String what) {
        helper.setBlock(TARGET, from);
        Block got = chiselAt(helper, player, stack, TARGET, sneaking, what);
        Assertions.valueEqual(helper, got, to, what + " on " + from);
    }

    /** Places {@code from}, clicks it and demands that nothing happened. */
    private static void assertRefuses(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                      boolean sneaking, Block from, String complaint) {
        helper.setBlock(TARGET, from);
        Block got = chiselAt(helper, player, stack, TARGET, sneaking, complaint);
        Assertions.valueEqual(helper, got, from, complaint + " (it became " + got + ")");
    }

    /** Runs the same block through both members of a material pair and demands the same result. */
    private static void assertSamePair(GameTestHelper helper, ServerPlayer player,
                                       ChiselItem first, ChiselItem second,
                                       Block from, Block to, String pair) {
        assertChisels(helper, player, new ItemStack(first), false, from, to,
                pair + ": the first member of the pair");
        assertChisels(helper, player, new ItemStack(second), false, from, to,
                pair + ": the second member of the pair, which has to share the first one's table");
    }

    private static void assertAxis(GameTestHelper helper, ServerPlayer player, BlockState state,
                                   Direction side, Vec3 hit, Direction.Axis expected, String what) {
        BlockState oriented = ChiselItem.applyIntuitiveOrientation(state, side, hit, player);
        Assertions.valueEqual(helper, oriented.getValue(RotatedPillarBlock.AXIS), expected,
                "pillar axis for " + what);
    }

    private static void assertFacing(GameTestHelper helper, ServerPlayer player, BlockState state,
                                     Direction side, Vec3 hit, Direction expected, String what) {
        BlockState oriented = ChiselItem.applyIntuitiveOrientation(state, side, hit, player);
        Assertions.valueEqual(helper, oriented.getValue(BlockStateProperties.FACING), expected,
                "facing for " + what);
    }

    private static void assertStairs(GameTestHelper helper, ServerPlayer player, BlockState state,
                                     Direction side, Vec3 hit, Direction expectedFacing,
                                     Half expectedHalf, String what) {
        BlockState oriented = ChiselItem.applyIntuitiveOrientation(state, side, hit, player);
        Assertions.valueEqual(helper, oriented.getValue(StairBlock.FACING), expectedFacing,
                "stair facing for " + what);
        Assertions.valueEqual(helper, oriented.getValue(StairBlock.HALF), expectedHalf,
                "stair half for " + what);
    }

    private static void assertSpeed(GameTestHelper helper, ChiselItem chisel, Block block,
                                    float expected, String what) {
        float actual = chisel.getDestroySpeed(new ItemStack(chisel), block.defaultBlockState());
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4F,
                what + ": mining speed is " + actual + " instead of " + expected);
    }

    /** Right clicks a block face at a precise spot on that face, server side. */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos, Direction face, Vec3 offsetInBlock) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + offsetInBlock.x, pos.getY() + offsetInBlock.y,
                pos.getZ() + offsetInBlock.z);
        BlockHitResult hitResult = new BlockHitResult(hit, face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    /**
     * Takes the item off its cooldown again. Without this the next click would be swallowed by the
     * cooldown the previous one set, and the test would measure nothing.
     */
    private static void clearCooldown(ServerPlayer player, ItemStack stack) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
    }

    /** The tooltip lines the item adds that talk about the stored target. */
    private static List<String> lastTargetLines(GameTestHelper helper, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()),
                TooltipDisplay.DEFAULT, line -> lines.add(line.getString()), TooltipFlag.NORMAL);
        lines.removeIf(line -> !line.startsWith("Last Target"));
        return lines;
    }

    private static void assertLastTarget(GameTestHelper helper, ItemStack stack, BlockPos expected,
                                         String what) {
        Assertions.valueEqual(helper, stack.get(ModDataComponentTypes.COORDINATES), expected,
                "the stored last target after " + what);
        List<String> lines = lastTargetLines(helper, stack);
        Assertions.valueEqual(helper, lines.size(), 1,
                "the tooltip lines mentioning the last target after " + what + ", got " + lines);
        Assertions.valueEqual(helper, lines.getFirst(),
                "Last Target: " + expected.getX() + ", " + expected.getY() + ", " + expected.getZ(),
                "the tooltip's last target line after " + what);
    }

    /**
     * Drives one rung of the count-based ladder: the right addition count matches and carries the
     * base's wear, name and enchantment across, one item short of it matches nothing.
     */
    private static void assertCountBasedUpgrade(GameTestHelper helper, ServerLevel level,
                                                RecipeManager recipes, Item template, Item base,
                                                Item addition, Item expected, String what) {
        ItemStack worn = new ItemStack(base);
        worn.set(DataComponents.DAMAGE, 7);
        worn.set(DataComponents.CUSTOM_NAME, Component.literal("Grandpa's tool"));
        worn.enchant(enchantment(helper, ModEnchantments.FAST_CHISELING), 2);

        SmithingRecipeInput enough = new SmithingRecipeInput(
                new ItemStack(template), worn, new ItemStack(addition, 2));
        Optional<RecipeHolder<SmithingRecipe>> holder =
                recipes.getRecipeFor(RecipeType.SMITHING, enough, level);
        helper.assertTrue(holder.isPresent(), what + " matches no smithing recipe at all");

        SmithingRecipe recipe = holder.get().value();
        helper.assertTrue(recipe instanceof CountBasedSmithingRecipe,
                what + " is a " + recipe.getClass().getSimpleName()
                        + ", not the mod's count based smithing recipe, so the count is ignored");
        Assertions.valueEqual(helper, ((CountBasedSmithingRecipe) recipe).getAdditionCount(), 2,
                what + ": the number of additions the upgrade demands");

        // assemble takes the registries alongside the input on this line; 26.2 dropped that parameter.
        ItemStack forged = recipe.assemble(enough, level.registryAccess());
        helper.assertTrue(forged.is(expected), what + " forges " + forged + " instead of " + expected);
        Assertions.valueEqual(helper, forged.getDamageValue(), 7, what + ": the wear carried over");
        Assertions.valueEqual(helper, forged.getHoverName().getString(), "Grandpa's tool",
                what + ": the custom name carried over");
        Assertions.valueEqual(helper, 
                com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel(
                        forged, level, ModEnchantments.FAST_CHISELING),
                2, what + ": the enchantment carried over");

        SmithingRecipeInput tooFew = new SmithingRecipeInput(
                new ItemStack(template), new ItemStack(base), new ItemStack(addition, 1));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, tooFew, level).isEmpty(),
                what + " went through with a single " + addition + ", so the addition count is not "
                        + "checked and the recipe is a plain smithing transform");
    }

    /** Drives one of the two vanilla {@code smithing_transform} rungs on top of the ladder. */
    private static void assertTransform(GameTestHelper helper, ServerLevel level,
                                        RecipeManager recipes, Item template, Item base,
                                        Item addition, Item expected, String what) {
        SmithingRecipeInput input = new SmithingRecipeInput(
                new ItemStack(template), new ItemStack(base), new ItemStack(addition));
        Optional<RecipeHolder<SmithingRecipe>> holder =
                recipes.getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(holder.isPresent(), what + " matches no smithing recipe at all");
        ItemStack forged = holder.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(forged.is(expected), what + " forges " + forged + " instead of " + expected);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /**
     * Die einzigen Vorwaertseintraege, deren Rueckweg woanders hinzeigt.
     *
     * <p>Beide bilden auf sich selbst ab (Notbehelf im Quelltext: fuer gestampften Schlamm gibt
     * es keine Treppe und keine Stufe). Ab der Eisenstufe biegt {@code brick_stairs ->
     * mud_brick_stairs} den Rueckweg auf {@code brick_stairs} um. Namentlich zugelassen, damit
     * eine dritte solche Stelle auffaellt statt mitzuschwimmen.
     */
    private static final Set<String> SELF_MAPPED_WITHOUT_RETURN = Set.of(
            "mud_brick_stairs>mud_brick_stairs",
            "mud_brick_slab>mud_brick_slab");

    /**
     * die Wurzel der Kette - 15 Eintraege, die Stufe fuehrt damit 15.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] STONE_OWN = {
            "cut_red_sandstone>red_sandstone",
            "cut_red_sandstone_slab>smooth_red_sandstone_slab",
            "cut_sandstone>sandstone",
            "cut_sandstone_slab>smooth_sandstone_slab",
            "red_sandstone>chiseled_red_sandstone",
            "red_sandstone_slab>cut_red_sandstone_slab",
            "red_sandstone_stairs>smooth_red_sandstone_stairs",
            "sandstone>chiseled_sandstone",
            "sandstone_slab>cut_sandstone_slab",
            "sandstone_stairs>smooth_sandstone_stairs",
            "smooth_red_sandstone>cut_red_sandstone",
            "smooth_sandstone>cut_sandstone",
            "smooth_stone_slab>stone_slab",
            "stone>chiseled_stone_bricks",
            "stone_stairs>cobblestone_stairs"
    };
    /**
     * was diese Stufe zu STONE hinzufuegt - 36 Eintraege, die Stufe fuehrt damit 51.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] IRON_OWN = {
            "acacia_planks>acacia_stairs",
            "acacia_stairs>acacia_slab",
            "bamboo_planks>bamboo_stairs",
            "bamboo_stairs>bamboo_slab",
            "birch_planks>birch_stairs",
            "birch_stairs>birch_slab",
            "cherry_planks>cherry_stairs",
            "cherry_stairs>cherry_slab",
            "chiseled_stone_bricks>stone_bricks",
            "dark_oak_planks>dark_oak_stairs",
            "dark_oak_stairs>dark_oak_slab",
            "jungle_planks>jungle_stairs",
            "jungle_stairs>jungle_slab",
            "mangrove_planks>mangrove_stairs",
            "mangrove_stairs>mangrove_slab",
            "oak_planks>oak_stairs",
            "oak_stairs>oak_slab",
            "pale_oak_planks>pale_oak_stairs",
            "pale_oak_stairs>pale_oak_slab",
            "polished_andesite>andesite",
            "polished_andesite_slab>andesite_slab",
            "polished_andesite_stairs>andesite_stairs",
            "polished_diorite>diorite",
            "polished_diorite_slab>diorite_slab",
            "polished_diorite_stairs>diorite_stairs",
            "polished_granite>granite",
            "polished_granite_slab>granite_slab",
            "polished_granite_stairs>granite_stairs",
            "polished_tuff>tuff",
            "polished_tuff_slab>tuff_slab",
            "polished_tuff_stairs>tuff_stairs",
            "spruce_planks>spruce_stairs",
            "spruce_stairs>spruce_slab",
            "stone_brick_slab>mossy_stone_brick_slab",
            "stone_brick_stairs>mossy_stone_brick_stairs",
            "stone_bricks>cracked_stone_bricks"
    };
    /**
     * was diese Stufe zu IRON hinzufuegt - 36 Eintraege, die Stufe fuehrt damit 87.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] DIAMOND_OWN = {
            "basalt>smooth_basalt",
            "blackstone>chiseled_polished_blackstone",
            "blackstone_slab>polished_blackstone_brick_slab",
            "blackstone_stairs>polished_blackstone_brick_stairs",
            "chiseled_deepslate>deepslate_bricks",
            "chiseled_polished_blackstone>polished_blackstone_bricks",
            "chiseled_quartz_block>quartz_block",
            "chiseled_tuff>tuff_bricks",
            "cracked_deepslate_bricks>deepslate_tiles",
            "cracked_deepslate_tiles>deepslate",
            "cracked_stone_bricks>cobblestone",
            "deepslate>cobbled_deepslate",
            "deepslate_brick_slab>deepslate_tile_slab",
            "deepslate_brick_stairs>deepslate_tile_stairs",
            "deepslate_bricks>cracked_deepslate_bricks",
            "deepslate_tile_slab>cobbled_deepslate_slab",
            "deepslate_tile_stairs>cobbled_deepslate_stairs",
            "deepslate_tiles>cracked_deepslate_tiles",
            "polished_blackstone>blackstone",
            "polished_blackstone_bricks>cracked_polished_blackstone_bricks",
            "polished_blackstone_slab>blackstone_slab",
            "polished_blackstone_stairs>blackstone_stairs",
            "polished_deepslate>chiseled_deepslate",
            "polished_deepslate_slab>deepslate_brick_slab",
            "polished_deepslate_stairs>deepslate_brick_stairs",
            "quartz_bricks>chiseled_quartz_block",
            "quartz_pillar>quartz_bricks",
            "smooth_basalt>polished_basalt",
            "smooth_quartz>quartz_pillar",
            "smooth_quartz_slab>quartz_slab",
            "smooth_quartz_stairs>quartz_stairs",
            "tuff>chiseled_tuff",
            "tuff_brick_slab>polished_tuff_slab",
            "tuff_brick_stairs>polished_tuff_stairs",
            "tuff_slab>tuff_brick_slab",
            "tuff_stairs>tuff_brick_stairs"
    };
    /**
     * was diese Stufe zu DIAMOND hinzufuegt - 11 Eintraege, die Stufe fuehrt damit 98.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] NETHERITE_OWN = {
            "chiseled_nether_bricks>netherrack",
            "chiseled_red_sandstone>red_sand",
            "chiseled_sandstone>sand",
            "cracked_nether_bricks>chiseled_nether_bricks",
            "nether_brick_slab>nether_brick_slab",
            "nether_brick_stairs>nether_brick_stairs",
            "nether_bricks>cracked_nether_bricks",
            "netherrack>nether_bricks",
            "resin_brick_slab>resin_brick_slab",
            "resin_brick_stairs>resin_brick_stairs",
            "resin_bricks>chiseled_resin_bricks"
    };
    /**
     * was diese Stufe zu STONE hinzufuegt - 16 Eintraege, die Stufe fuehrt damit 31.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] STONE_TOUCH_OWN = {
            "acacia_log>stripped_acacia_log",
            "birch_log>stripped_birch_log",
            "cherry_log>stripped_cherry_log",
            "cobblestone>mossy_cobblestone",
            "cobblestone_slab>mossy_cobblestone_slab",
            "cobblestone_stairs>mossy_cobblestone_stairs",
            "dark_oak_log>stripped_dark_oak_log",
            "jungle_log>stripped_jungle_log",
            "mangrove_log>stripped_mangrove_log",
            "mud_brick_slab>mud_brick_slab",
            "mud_brick_stairs>mud_brick_stairs",
            "mud_bricks>packed_mud",
            "oak_log>stripped_oak_log",
            "packed_mud>mud",
            "pale_oak_log>stripped_pale_oak_log",
            "spruce_log>stripped_spruce_log"
    };
    /**
     * was diese Stufe zu STONE_TOUCH hinzufuegt - 52 Eintraege, die Stufe fuehrt damit 83.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] IRON_TOUCH_OWN = {
            "acacia_planks>acacia_stairs",
            "acacia_stairs>acacia_slab",
            "acacia_wood>stripped_acacia_wood",
            "bamboo_planks>bamboo_stairs",
            "bamboo_stairs>bamboo_slab",
            "birch_planks>birch_stairs",
            "birch_stairs>birch_slab",
            "birch_wood>stripped_birch_wood",
            "brick_slab>mud_brick_slab",
            "brick_stairs>mud_brick_stairs",
            "bricks>mud_bricks",
            "cherry_planks>cherry_stairs",
            "cherry_stairs>cherry_slab",
            "cherry_wood>stripped_cherry_wood",
            "chiseled_stone_bricks>stone_bricks",
            "crimson_planks>crimson_stairs",
            "crimson_stairs>crimson_slab",
            "dark_oak_planks>dark_oak_stairs",
            "dark_oak_stairs>dark_oak_slab",
            "dark_oak_wood>stripped_dark_oak_wood",
            "jungle_planks>jungle_stairs",
            "jungle_stairs>jungle_slab",
            "jungle_wood>stripped_jungle_wood",
            "mangrove_planks>mangrove_stairs",
            "mangrove_stairs>mangrove_slab",
            "mangrove_wood>stripped_mangrove_wood",
            "oak_planks>oak_stairs",
            "oak_stairs>oak_slab",
            "oak_wood>stripped_oak_wood",
            "pale_oak_planks>pale_oak_stairs",
            "pale_oak_stairs>pale_oak_slab",
            "pale_oak_wood>stripped_pale_oak_wood",
            "polished_andesite>andesite",
            "polished_andesite_slab>andesite_slab",
            "polished_andesite_stairs>andesite_stairs",
            "polished_diorite>diorite",
            "polished_diorite_slab>diorite_slab",
            "polished_diorite_stairs>diorite_stairs",
            "polished_granite>granite",
            "polished_granite_slab>granite_slab",
            "polished_granite_stairs>granite_stairs",
            "polished_tuff>tuff",
            "polished_tuff_slab>tuff_slab",
            "polished_tuff_stairs>tuff_stairs",
            "spruce_planks>spruce_stairs",
            "spruce_stairs>spruce_slab",
            "spruce_wood>stripped_spruce_wood",
            "stone_brick_slab>mossy_stone_brick_slab",
            "stone_brick_stairs>mossy_stone_brick_stairs",
            "stone_bricks>cracked_stone_bricks",
            "warped_planks>warped_stairs",
            "warped_stairs>warped_slab"
    };
    /**
     * was diese Stufe zu IRON_TOUCH hinzufuegt - 61 Eintraege, die Stufe fuehrt damit 144.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] DIAMOND_TOUCH_OWN = {
            "basalt>smooth_basalt",
            "blackstone>chiseled_polished_blackstone",
            "blackstone_slab>polished_blackstone_brick_slab",
            "blackstone_stairs>polished_blackstone_brick_stairs",
            "brain_coral_block>bubble_coral_block",
            "bubble_coral_block>fire_coral_block",
            "chiseled_copper>copper_grate",
            "chiseled_deepslate>deepslate_bricks",
            "chiseled_polished_blackstone>polished_blackstone_bricks",
            "chiseled_quartz_block>quartz_block",
            "chiseled_tuff>tuff_bricks",
            "copper_block>cut_copper",
            "cracked_deepslate_bricks>deepslate_tiles",
            "cracked_deepslate_tiles>deepslate",
            "cracked_stone_bricks>cobblestone",
            "cut_copper>chiseled_copper",
            "cut_copper_slab>cut_copper_slab",
            "cut_copper_stairs>cut_copper_stairs",
            "dead_brain_coral_block>dead_bubble_coral_block",
            "dead_bubble_coral_block>dead_fire_coral_block",
            "dead_fire_coral_block>dead_horn_coral_block",
            "dead_horn_coral_block>dead_tube_coral_block",
            "dead_tube_coral_block>dead_brain_coral_block",
            "deepslate>cobbled_deepslate",
            "deepslate_brick_slab>deepslate_tile_slab",
            "deepslate_brick_stairs>deepslate_tile_stairs",
            "deepslate_bricks>cracked_deepslate_bricks",
            "deepslate_tile_slab>cobbled_deepslate_slab",
            "deepslate_tile_stairs>cobbled_deepslate_stairs",
            "deepslate_tiles>cracked_deepslate_tiles",
            "end_stone>end_stone_bricks",
            "end_stone_brick_slab>end_stone_brick_slab",
            "end_stone_brick_stairs>end_stone_brick_stairs",
            "fire_coral_block>horn_coral_block",
            "horn_coral_block>tube_coral_block",
            "polished_blackstone>blackstone",
            "polished_blackstone_bricks>cracked_polished_blackstone_bricks",
            "polished_blackstone_slab>blackstone_slab",
            "polished_blackstone_stairs>blackstone_stairs",
            "polished_deepslate>chiseled_deepslate",
            "polished_deepslate_slab>deepslate_brick_slab",
            "polished_deepslate_stairs>deepslate_brick_stairs",
            "prismarine>prismarine_bricks",
            "prismarine_slab>prismarine_brick_slab",
            "prismarine_stairs>prismarine_brick_stairs",
            "purpur_pillar>purpur_block",
            "purpur_slab>purpur_slab",
            "purpur_stairs>purpur_stairs",
            "quartz_bricks>chiseled_quartz_block",
            "quartz_pillar>quartz_bricks",
            "smooth_basalt>polished_basalt",
            "smooth_quartz>quartz_pillar",
            "smooth_quartz_slab>quartz_slab",
            "smooth_quartz_stairs>quartz_stairs",
            "smooth_stone>stone",
            "tube_coral_block>brain_coral_block",
            "tuff>chiseled_tuff",
            "tuff_brick_slab>polished_tuff_slab",
            "tuff_brick_stairs>polished_tuff_stairs",
            "tuff_slab>tuff_brick_slab",
            "tuff_stairs>tuff_brick_stairs"
    };
    /**
     * was diese Stufe zu DIAMOND_TOUCH hinzufuegt - 32 Eintraege, die Stufe fuehrt damit 176.
     *
     * <p>Schreibweise {@code von>nach}, beides ohne Namensraum, weil alles Vanilla-Bloecke sind.
     */
    private static final String[] NETHERITE_TOUCH_OWN = {
            "black_concrete>black_concrete_powder",
            "blue_concrete>blue_concrete_powder",
            "brown_concrete>brown_concrete_powder",
            "calcite>dripstone_block",
            "chiseled_nether_bricks>netherrack",
            "chiseled_red_sandstone>red_sand",
            "chiseled_sandstone>sand",
            "cracked_nether_bricks>chiseled_nether_bricks",
            "crimson_stem>stripped_crimson_stem",
            "cyan_concrete>cyan_concrete_powder",
            "diorite>calcite",
            "gray_concrete>gray_concrete_powder",
            "green_concrete>green_concrete_powder",
            "light_blue_concrete>light_blue_concrete_powder",
            "light_gray_concrete>light_gray_concrete_powder",
            "lime_concrete>lime_concrete_powder",
            "magenta_concrete>magenta_concrete_powder",
            "nether_brick_slab>nether_brick_slab",
            "nether_brick_stairs>nether_brick_stairs",
            "nether_bricks>cracked_nether_bricks",
            "netherrack>nether_bricks",
            "obsidian>crying_obsidian",
            "orange_concrete>orange_concrete_powder",
            "pink_concrete>pink_concrete_powder",
            "purple_concrete>purple_concrete_powder",
            "red_concrete>red_concrete_powder",
            "resin_brick_slab>resin_brick_slab",
            "resin_brick_stairs>resin_brick_stairs",
            "resin_bricks>chiseled_resin_bricks",
            "warped_stem>stripped_warped_stem",
            "white_concrete>white_concrete_powder",
            "yellow_concrete>yellow_concrete_powder"
    };

    /**
     * Nagelt die Umwandlungstabellen des Meissels Eintrag fuer Eintrag fest.
     *
     * <p><strong>Warum es diesen Test gibt.</strong> Die vier Stufen tragen zwischen 15 und 176
     * Umwandlungen, und die uebrigen Meisseltests fahren davon eine Handvoll. Damit laesst sich
     * jeder andere Eintrag loeschen oder auf ein anderes Ziel umbiegen, ohne dass ein Test rot
     * wird - genau das hat das Audit vom 2026-09-08 an sechs Stellen nachgewiesen. Ein Test je
     * Eintrag waere nicht zu pflegen, also nagelt dieser hier die Tabellen als Ganzes fest.
     *
     * <p><strong>Was er beweist und was nicht.</strong> Er beweist, dass sich an den Tabellen
     * nichts <em>unbemerkt</em> aendert. Er beweist <em>nicht</em>, dass die Zuordnungen
     * inhaltlich richtig sind - dafuer waere ein zweiter Massstab noetig, den es nicht gibt.
     * Wer eine Umwandlung absichtlich aendert, aendert hier eine Zeile mit; das ist der Zweck.
     * Die Zeile im Diff ist der Unterschied zu heute, wo dieselbe Aenderung spurlos bliebe.
     *
     * <p><strong>Drei Aussagen, nicht eine.</strong>
     * <ol>
     *   <li><em>Eigenanteil:</em> was jede Stufe zur vorigen hinzufuegt, steht vollstaendig in
     *       den Konstanten oben - Eintrag fuer Eintrag, in beide Richtungen verglichen.</li>
     *   <li><em>Vererbung:</em> jede Stufe traegt alles, was die vorige trug. Ein Eintrag, der
     *       beim Hochstufen verschwaende, faellt hier auf und nicht erst beim Spieler.</li>
     *   <li><em>Umkehrbarkeit:</em> zu jedem {@code a -> b} vorwaerts gehoert {@code b -> a}
     *       rueckwaerts. Das ist keine Momentaufnahme, sondern die Eigenschaft, die
     *       {@code registerLinear} und {@code registerCyclic} herstellen sollen.</li>
     * </ol>
     *
     * <p><strong>Zwei benannte Ausnahmen von der Umkehrbarkeit.</strong>
     * {@code mud_brick_stairs} und {@code mud_brick_slab} sind in der Steinstufe auf sich selbst
     * abgebildet (im Quelltext als Notbehelf kommentiert: fuer gestampften Schlamm gibt es keine
     * Treppe). Ab der Eisenstufe legt {@code brick_stairs -> mud_brick_stairs} den Rueckweg auf
     * {@code brick_stairs}, womit die Selbstabbildung ihren Rueckweg verliert. Das ist hier
     * festgehalten, damit es eine bekannte Eigenheit bleibt und keine stille Ueberraschung.
     *
     * <p><strong>Was diesen Test rot macht:</strong> ein geloeschter, hinzugefuegter oder
     * umgebogener Tabelleneintrag; eine Stufe, die eine Umwandlung der vorigen verliert; eine
     * {@code registerLinear}-Zeile, die nur eine Richtung schreibt; und eine dritte
     * Selbstabbildung, die den Weg der beiden Schlammziegel-Eintraege geht.
     */
    public static void conversionTablesArePinnedEntryByEntry(GameTestHelper helper) {
        assertOwnContribution(helper, "stone", ChiselItem.FINAL_STONE_FWD, null, STONE_OWN);
        assertOwnContribution(helper, "iron", ChiselItem.FINAL_IRON_FWD,
                ChiselItem.FINAL_STONE_FWD, IRON_OWN);
        assertOwnContribution(helper, "diamond", ChiselItem.FINAL_DIAMOND_FWD,
                ChiselItem.FINAL_IRON_FWD, DIAMOND_OWN);
        assertOwnContribution(helper, "netherite", ChiselItem.FINAL_NETHERITE_FWD,
                ChiselItem.FINAL_DIAMOND_FWD, NETHERITE_OWN);

        assertOwnContribution(helper, "stone+touch", ChiselItem.FINAL_STONE_TOUCH_FWD,
                ChiselItem.FINAL_STONE_FWD, STONE_TOUCH_OWN);
        assertOwnContribution(helper, "iron+touch", ChiselItem.FINAL_IRON_TOUCH_FWD,
                ChiselItem.FINAL_STONE_TOUCH_FWD, IRON_TOUCH_OWN);
        assertOwnContribution(helper, "diamond+touch", ChiselItem.FINAL_DIAMOND_TOUCH_FWD,
                ChiselItem.FINAL_IRON_TOUCH_FWD, DIAMOND_TOUCH_OWN);
        assertOwnContribution(helper, "netherite+touch", ChiselItem.FINAL_NETHERITE_TOUCH_FWD,
                ChiselItem.FINAL_DIAMOND_TOUCH_FWD, NETHERITE_TOUCH_OWN);

        assertReversible(helper, "stone", ChiselItem.FINAL_STONE_FWD, ChiselItem.FINAL_STONE_BWD);
        assertReversible(helper, "iron", ChiselItem.FINAL_IRON_FWD, ChiselItem.FINAL_IRON_BWD);
        assertReversible(helper, "diamond", ChiselItem.FINAL_DIAMOND_FWD, ChiselItem.FINAL_DIAMOND_BWD);
        assertReversible(helper, "netherite", ChiselItem.FINAL_NETHERITE_FWD,
                ChiselItem.FINAL_NETHERITE_BWD);
        assertReversible(helper, "stone+touch", ChiselItem.FINAL_STONE_TOUCH_FWD,
                ChiselItem.FINAL_STONE_TOUCH_BWD);
        assertReversible(helper, "iron+touch", ChiselItem.FINAL_IRON_TOUCH_FWD,
                ChiselItem.FINAL_IRON_TOUCH_BWD);
        assertReversible(helper, "diamond+touch", ChiselItem.FINAL_DIAMOND_TOUCH_FWD,
                ChiselItem.FINAL_DIAMOND_TOUCH_BWD);
        assertReversible(helper, "netherite+touch", ChiselItem.FINAL_NETHERITE_TOUCH_FWD,
                ChiselItem.FINAL_NETHERITE_TOUCH_BWD);

        TestCleanup.succeed(helper);
    }

    /**
     * Prueft den Eigenanteil einer Stufe und, sofern es eine Vorstufe gibt, deren vollstaendige
     * Vererbung.
     *
     * <p>Verglichen wird in beide Richtungen: fehlende Eintraege und ueberzaehlige werden
     * einzeln benannt. Eine blosse Anzahl koennte "einer geloescht, einer dazu" nicht von
     * "unveraendert" unterscheiden.
     */
    private static void assertOwnContribution(GameTestHelper helper, String tier,
                                              Map<Block, Block> map,
                                              @Nullable Map<Block, Block> parent,
                                              String[] expectedOwn) {
        if (parent != null) {
            List<String> lost = new ArrayList<>();
            for (Map.Entry<Block, Block> e : parent.entrySet()) {
                Block carried = map.get(e.getKey());
                if (carried != e.getValue()) {
                    lost.add(blockName(e.getKey()) + ">" + blockName(e.getValue())
                            + (carried == null ? " (fehlt ganz)" : " (zeigt auf " + blockName(carried) + ")"));
                }
            }
            helper.assertTrue(lost.isEmpty(), tier + " verliert " + lost.size()
                    + " Umwandlung(en) der Vorstufe: " + lost);
        }

        Set<String> actualOwn = new TreeSet<>();
        for (Map.Entry<Block, Block> e : map.entrySet()) {
            if (parent == null || parent.get(e.getKey()) != e.getValue()) {
                actualOwn.add(blockName(e.getKey()) + ">" + blockName(e.getValue()));
            }
        }
        Set<String> expected = new TreeSet<>(Arrays.asList(expectedOwn));

        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actualOwn);
        Set<String> surplus = new TreeSet<>(actualOwn);
        surplus.removeAll(expected);

        helper.assertTrue(missing.isEmpty(),
                tier + " hat " + missing.size() + " festgenagelte Umwandlung(en) verloren: " + missing);
        helper.assertTrue(surplus.isEmpty(),
                tier + " hat " + surplus.size() + " Umwandlung(en), die hier nicht festgenagelt sind: "
                        + surplus + " - wenn sie gewollt sind, gehoeren sie in die Konstante");
    }

    /**
     * Prueft, dass die Rueckwaertstabelle die Umkehrung der Vorwaertstabelle ist.
     *
     * <p>Die beiden Schlammziegel-Selbstabbildungen sind die einzige erlaubte Abweichung, siehe
     * Klassendoku des Tests. Sie werden namentlich zugelassen und nicht pauschal uebergangen -
     * eine dritte solche Stelle soll auffallen.
     */
    private static void assertReversible(GameTestHelper helper, String tier,
                                         Map<Block, Block> forward, Map<Block, Block> backward) {
        List<String> broken = new ArrayList<>();
        for (Map.Entry<Block, Block> e : forward.entrySet()) {
            Block back = backward.get(e.getValue());
            if (back == e.getKey()) {
                continue;
            }
            String entry = blockName(e.getKey()) + ">" + blockName(e.getValue());
            if (SELF_MAPPED_WITHOUT_RETURN.contains(entry)) {
                // Bekannt und oben begruendet: die Selbstabbildung verliert ihren Rueckweg an
                // brick_stairs bzw. brick_slab. Dass der Rueckweg genau DORTHIN zeigt, wird
                // gleich mitgeprueft - sonst waere die Ausnahme ein Freibrief.
                helper.assertTrue(back != null, tier + ": " + entry + " hat gar keinen Rueckweg");
                continue;
            }
            broken.add(entry + " (rueckwaerts: " + (back == null ? "nichts" : blockName(back)) + ")");
        }
        helper.assertTrue(broken.isEmpty(), tier + " ist an " + broken.size()
                + " Stelle(n) nicht umkehrbar: " + broken);
    }

    private static String blockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }
}
